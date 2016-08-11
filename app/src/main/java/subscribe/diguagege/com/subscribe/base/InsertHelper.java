package subscribe.diguagege.com.subscribe.base;

/**
 * Created by hanwei on 16-8-11.
 */

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * This class allows users to do multiple inserts into a table using
 * the same statement.
 * <p>
 * This class is not thread-safe.
 * </p>
 *
 */
public  class InsertHelper {

    private static final String TAG = "InsertHelper";

    private final SQLiteDatabase mDb;
    private final String mTableName;
    private HashMap<String, Integer> mColumns;
    private String mInsertSQL = null;
    private SQLiteStatement mInsertStatement = null;
    private SQLiteStatement mIgnoreStatement = null;
    private SQLiteStatement mReplaceStatement = null;
    private SQLiteStatement mPreparedStatement = null;

    /**
     * {@hide}
     *
     * These are the columns returned by sqlite's "PRAGMA
     * table_info(...)" command that we depend on.
     */
    public static final int TABLE_INFO_PRAGMA_COLUMNNAME_INDEX = 1;

    /**
     * This field was accidentally exposed in earlier versions of the platform
     * so we can hide it but we can't remove it.
     *
     * @hide
     */
    public static final int TABLE_INFO_PRAGMA_DEFAULT_INDEX = 4;

    /**
     * @param db the SQLiteDatabase to insert into
     * @param tableName the name of the table to insert into
     */
    public InsertHelper(SQLiteDatabase db, String tableName) {
        mDb = db;
        mTableName = tableName;
    }

    public void initColumns() {
        getStatement(false);
    }

    private void buildSQL() throws SQLException {
        StringBuilder sb = new StringBuilder(128);
        sb.append("INSERT INTO ");
        sb.append(mTableName);
        sb.append(" (");

        StringBuilder sbv = new StringBuilder(128);
        sbv.append("VALUES (");

        int i = 1;
        Cursor cur = null;
        try {
            cur = mDb.rawQuery("PRAGMA table_info(" + mTableName + ")", null);
            mColumns = new HashMap<String, Integer>(cur.getCount());
            while (cur.moveToNext()) {
                String columnName = cur.getString(TABLE_INFO_PRAGMA_COLUMNNAME_INDEX);
                String defaultValue = cur.getString(TABLE_INFO_PRAGMA_DEFAULT_INDEX);

                mColumns.put(columnName, i);
                sb.append("'");
                sb.append(columnName);
                sb.append("'");

                if (defaultValue == null) {
                    sbv.append("?");
                } else {
                    sbv.append("COALESCE(?, ");
                    sbv.append(defaultValue);
                    sbv.append(")");
                }

                sb.append(i == cur.getCount() ? ") " : ", ");
                sbv.append(i == cur.getCount() ? ");" : ", ");
                ++i;
            }
        } finally {
            if (cur != null) cur.close();
        }

        sb.append(sbv);

        mInsertSQL = sb.toString();
    }

    private SQLiteStatement getStatement(boolean allowReplace) throws SQLException {
        if (allowReplace) {
            if (mReplaceStatement == null) {
                if (mInsertSQL == null) buildSQL();
                // chop "INSERT" off the front and prepend "INSERT OR REPLACE" instead.
                String replaceSQL = "INSERT OR REPLACE" + mInsertSQL.substring(6);
                mReplaceStatement = mDb.compileStatement(replaceSQL);
            }
            return mReplaceStatement;
        } else {
            if (mInsertStatement == null) {
                if (mInsertSQL == null) buildSQL();
                mInsertStatement = mDb.compileStatement(mInsertSQL);
            }
            return mInsertStatement;
        }
    }
    public SQLiteStatement getStatementIgnore() throws SQLException {
        if (mIgnoreStatement == null) {
            if (mInsertSQL == null) buildSQL();
            // chop "INSERT" off the front and prepend "INSERT OR IGNORE" instead.
            String replaceSQL = "INSERT OR IGNORE" + mInsertSQL.substring(6);
            mIgnoreStatement = mDb.compileStatement(replaceSQL);
        }
        return mIgnoreStatement;
    }
    /**
     * Performs an insert, adding a new row with the given values.
     *
     * @param values the set of values with which  to populate the
     * new row
     * @param allowReplace if true, the statement does "INSERT OR
     *   REPLACE" instead of "INSERT", silently deleting any
     *   previously existing rows that would cause a conflict
     *
     * @return the row ID of the newly inserted row, or -1 if an
     * error occurred
     */
    private long insertInternal(ContentValues values, boolean allowReplace) {
        // Start a transaction even though we don't really need one.
        // This is to help maintain compatibility with applications that
        // access InsertHelper from multiple threads even though they never should have.
        // The original code used to lock the InsertHelper itself which was prone
        // to deadlocks.  Starting a transaction achieves the same mutual exclusion
        // effect as grabbing a lock but without the potential for deadlocks.
        mDb.beginTransactionNonExclusive();
        try {
            SQLiteStatement stmt = getStatement(allowReplace);
            stmt.clearBindings();
            for (Map.Entry<String, Object> e: values.valueSet()) {
                final String key = e.getKey();
                int i = getColumnIndex(key);
                DatabaseUtils.bindObjectToProgram(stmt, i, e.getValue());
            }
            long result = stmt.executeInsert();
            mDb.setTransactionSuccessful();
            return result;
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + values + " into table  " + mTableName, e);
            return -1;
        } finally {
            mDb.endTransaction();
        }
    }
    private long insertInternalIgnore(ContentValues values) {
        mDb.beginTransactionNonExclusive();
        try {
            SQLiteStatement stmt = getStatementIgnore();
            stmt.clearBindings();
            for (Map.Entry<String, Object> e: values.valueSet()) {
                final String key = e.getKey();
                int i = getColumnIndexForIngnore(key);
                DatabaseUtils.bindObjectToProgram(stmt, i, e.getValue());
            }
            long result = stmt.executeInsert();
            mDb.setTransactionSuccessful();
            return result;
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + values + " into table  " + mTableName, e);
            return -1;
        } finally {
            mDb.endTransaction();
        }
    }

    public long insertIgnore() {
        mDb.beginTransactionNonExclusive();
        try {
            SQLiteStatement stmt = getStatementIgnore();
            stmt.clearBindings();

            long result = stmt.executeInsert();
            mDb.setTransactionSuccessful();
            return result;
        } catch (SQLException e) {
            return -1;
        } finally {
            mDb.endTransaction();
        }
    }

    public long insertIgnore(SQLiteStatement stmt) {
        mDb.beginTransactionNonExclusive();
        try {
            long result = stmt.executeInsert();
            mDb.setTransactionSuccessful();
            return result;
        } catch (SQLException e) {
            return -1;
        } finally {
            mDb.endTransaction();
        }
    }

    public long insertIgnoreNonTransaction(SQLiteStatement stmt) {
        try {
            long result = stmt.executeInsert();
            return result;
        } catch (SQLException e) {
            return -1;
        }
    }

    public void beginTransactionNonExclusive() {
        mDb.beginTransactionNonExclusive();
    }

    public void setTransactionSuccessful() {
        mDb.setTransactionSuccessful();
    }

    public void endTransaction(){
        mDb.endTransaction();
    }

    /**
     * Returns the index of the specified column. This is index is suitagble for use
     * in calls to bind().
     * @param key the column name
     * @return the index of the column
     */
    public int getColumnIndex(String key) {
        getStatement(false);
        final Integer index = mColumns.get(key);
        if (index == null) {
            throw new IllegalArgumentException("column '" + key + "' is invalid");
        }
        return index;
    }
    public int getColumnIndexForIngnore(String key) {
        final Integer index = mColumns.get(key);
        if (index == null) {
            throw new IllegalArgumentException("column '" + key + "' is invalid");
        }
        return index;
    }
    /**
     * Bind the value to an index. A prepareForInsert() or prepareForReplace()
     * without a matching execute() must have already have been called.
     * @param index the index of the slot to which to bind
     * @param value the value to bind
     */
    public void bind(int index, double value) {
        mPreparedStatement.bindDouble(index, value);
    }

    /**
     * Bind the value to an index. A prepareForInsert() or prepareForReplace()
     * without a matching execute() must have already have been called.
     * @param index the index of the slot to which to bind
     * @param value the value to bind
     */
    public void bind(int index, float value) {
        mPreparedStatement.bindDouble(index, value);
    }

    /**
     * Bind the value to an index. A prepareForInsert() or prepareForReplace()
     * without a matching execute() must have already have been called.
     * @param index the index of the slot to which to bind
     * @param value the value to bind
     */
    public void bind(int index, long value) {
        mPreparedStatement.bindLong(index, value);
    }

    /**
     * Bind the value to an index. A prepareForInsert() or prepareForReplace()
     * without a matching execute() must have already have been called.
     * @param index the index of the slot to which to bind
     * @param value the value to bind
     */
    public void bind(int index, int value) {
        mPreparedStatement.bindLong(index, value);
    }

    /**
     * Bind the value to an index. A prepareForInsert() or prepareForReplace()
     * without a matching execute() must have already have been called.
     * @param index the index of the slot to which to bind
     * @param value the value to bind
     */
    public void bind(int index, boolean value) {
        mPreparedStatement.bindLong(index, value ? 1 : 0);
    }

    /**
     * Bind null to an index. A prepareForInsert() or prepareForReplace()
     * without a matching execute() must have already have been called.
     * @param index the index of the slot to which to bind
     */
    public void bindNull(int index) {
        mPreparedStatement.bindNull(index);
    }

    /**
     * Bind the value to an index. A prepareForInsert() or prepareForReplace()
     * without a matching execute() must have already have been called.
     * @param index the index of the slot to which to bind
     * @param value the value to bind
     */
    public void bind(int index, byte[] value) {
        if (value == null) {
            mPreparedStatement.bindNull(index);
        } else {
            mPreparedStatement.bindBlob(index, value);
        }
    }

    /**
     * Bind the value to an index. A prepareForInsert() or prepareForReplace()
     * without a matching execute() must have already have been called.
     * @param index the index of the slot to which to bind
     * @param value the value to bind
     */
    public void bind(int index, String value) {
        if (value == null) {
            mPreparedStatement.bindNull(index);
        } else {
            mPreparedStatement.bindString(index, value);
        }
    }

    /**
     * Performs an insert, adding a new row with the given values.
     * If the table contains conflicting rows, an error is
     * returned.
     *
     * @param values the set of values with which to populate the
     * new row
     *
     * @return the row ID of the newly inserted row, or -1 if an
     * error occurred
     */
    public long insert(ContentValues values) {
        return insertInternal(values, false);
    }

    public long insertIgnore(ContentValues values) {
        return insertInternalIgnore(values);
    }
    /**
     * Execute the previously prepared insert or replace using the bound values
     * since the last call to prepareForInsert or prepareForReplace.
     *
     * <p>Note that calling bind() and then execute() is not thread-safe. The only thread-safe
     * way to use this class is to call insert() or replace().
     *
     * @return the row ID of the newly inserted row, or -1 if an
     * error occurred
     */
    public long execute() {
        if (mPreparedStatement == null) {
            throw new IllegalStateException("you must prepare this inserter before calling "
                    + "execute");
        }
        try {
            return mPreparedStatement.executeInsert();
        } catch (SQLException e) {
            Log.e(TAG, "Error executing InsertHelper with table " + mTableName, e);
            return -1;
        } finally {
            // you can only call this once per prepare
            mPreparedStatement = null;
        }
    }

    /**
     * Prepare the InsertHelper for an insert. The pattern for this is:
     * <ul>
     * <li>prepareForInsert()
     * <li>bind(index, value);
     * <li>bind(index, value);
     * <li>...
     * <li>bind(index, value);
     * <li>execute();
     * </ul>
     */
    public void prepareForInsert() {
        mPreparedStatement = getStatement(false);
        mPreparedStatement.clearBindings();
    }

    /**
     * Prepare the InsertHelper for a replace. The pattern for this is:
     * <ul>
     * <li>prepareForReplace()
     * <li>bind(index, value);
     * <li>bind(index, value);
     * <li>...
     * <li>bind(index, value);
     * <li>execute();
     * </ul>
     */
    public void prepareForReplace() {
        mPreparedStatement = getStatement(true);
        mPreparedStatement.clearBindings();
    }

    public void prepareForIgnore() {
        mPreparedStatement = getStatementIgnore();
        mPreparedStatement.clearBindings();
    }

    /**
     * Performs an insert, adding a new row with the given values.
     * If the table contains conflicting rows, they are deleted
     * and replaced with the new row.
     *
     * @param values the set of values with which to populate the
     * new row
     *
     * @return the row ID of the newly inserted row, or -1 if an
     * error occurred
     */
    public long replace(ContentValues values) {
        return insertInternal(values, true);
    }


    /**
     * Close this object and release any resources associated with
     * it.  The behavior of calling <code>insert()</code> after
     * calling this method is undefined.
     */
    public void close() {
        if (mInsertStatement != null) {
            mInsertStatement.close();
            mInsertStatement = null;
        }
        if (mReplaceStatement != null) {
            mReplaceStatement.close();
            mReplaceStatement = null;
        }
        if (mIgnoreStatement != null) {
            mIgnoreStatement.close();
            mIgnoreStatement = null;
        }
        mInsertSQL = null;
        mColumns = null;
    }
}