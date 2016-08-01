package subscribe.diguagege.com.subscribe.base;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by hanwei on 16-7-26.
 */
public class SubscribeProviders extends SQLiteContentProvider {
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    protected static final int SUBJECT = 1;
    protected static final int SUBSCRIBE = 2;
    protected static final int REMINDER = 3;
    protected static final int SUBSCRIBE_ALERTS = 4;
    protected static final int SCHEDULE_ALARM = 5;
    protected static final int SCHEDULE_ALARM_REMOVE = 6;
    private SubscribeDatabaseHelper mHelper;
    private SubscribeAlarmManager mAlarmManager;
    private Context mContext;
    private static SubscribeProviders mInstance;

    static {
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "subject", SUBJECT);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "subscribe", SUBSCRIBE);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "reminders", REMINDER);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "subscribe_alerts", SUBSCRIBE_ALERTS);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, SubscribeAlarmManager.SCHEDULE_ALARM_PATH,
                SCHEDULE_ALARM);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY,
                SubscribeAlarmManager.SCHEDULE_ALARM_REMOVE_PATH, SCHEDULE_ALARM_REMOVE);

    }

    @Override
    public boolean onCreate() {
        super.onCreate();
        try {
            return initialize();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean initialize() {
        mInstance = this;
        mContext = getContext();
        mHelper = (SubscribeDatabaseHelper)getDatabaseHelper();
        // Register for Intent broadcasts
        IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);

        // We don't ever unregister this because this thread always wants
        // to receive notifications, even in the background.  And if this
        // thread is killed then the whole process will be killed and the
        // memory resources will be reclaimed.
        mContext.registerReceiver(mIntentReceiver, filter);
        initAlarmManager();

        return true;
    }


    private void initAlarmManager() {
        mAlarmManager = getOrCreateSubscribeAlarmManager();
    }

    public static SubscribeProviders getInstance() {
        return mInstance;
    }


    synchronized SubscribeAlarmManager getOrCreateSubscribeAlarmManager() {
        if (mAlarmManager == null) {
            mAlarmManager = new SubscribeAlarmManager(mContext);
        }
        return mAlarmManager;
    }

    @Override
    protected SQLiteOpenHelper getDatabaseHelper(Context context) {
        return SubscribeDatabaseHelper.getInstance(context);
    }

    @Override
    protected Uri insertInTransaction(Uri uri, ContentValues values, boolean callerIsSyncAdapter) {
        final int match = sUriMatcher.match(uri);
        long id = 0;
        switch (match) {
            case SUBJECT:
                id = mHelper.insertSubject(values);
                break;
            case SUBSCRIBE:
                id = mHelper.insertSubscribe(values);
                break;
            case REMINDER:
                id = mHelper.insertReminders(values);
                // TODO: This may add SubscribeAlerts
                mAlarmManager.scheduleNextAlarm(false);
                break;
            case SUBSCRIBE_ALERTS:
                id = mHelper.insertAlerts(values);
                break;
        }

        if (id < 0) {
            return null;
        }

        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    protected int updateInTransaction(Uri uri, ContentValues values, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        final int match = sUriMatcher.match(uri);
        mDb = mHelper.getWritableDatabase();
        switch (match) {
            case REMINDER:
                mAlarmManager.scheduleNextAlarm(false);
                return 0;
            // TODO: replace the SCHEDULE_ALARM private URIs with a
            // service
            case SCHEDULE_ALARM: {
                mAlarmManager.scheduleNextAlarm(false);
                return 0;
            }
            case SCHEDULE_ALARM_REMOVE: {
                mAlarmManager.scheduleNextAlarm(true);
                return 0;
            }
        }
        return 0;
    }

    @Override
    protected int deleteInTransaction(Uri uri, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SUBJECT:
                mDb.delete(SubscribeContract.Subject.TABLE_NAME, selection, selectionArgs);
                break;
            case SUBSCRIBE:
                mDb.delete(SubscribeContract.Subscribe.TABLE_NAME, selection, selectionArgs);
                mAlarmManager.scheduleNextAlarm(false /* do not remove alarms */);
                Log.d("ProviderDebug", "DeleteReminders");
                break;
            case REMINDER:
                mDb.delete(SubscribeContract.Reminders.TABLE_NAME, selection, selectionArgs);
                break;
            case SUBSCRIBE_ALERTS:
                mDb.delete(SubscribeContract.SubscribeAlerts.TABLE_NAME, selection, selectionArgs);
                break;
        }

        return 0;
    }

    @Override
    protected void notifyChange(boolean syncToNetwork) {

    }

    @Override
    protected boolean shouldSyncFor(Uri uri) {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SUBJECT:
                return mDb.query(SubscribeContract.Subject.TABLE_NAME,
                        null, selection, selectionArgs, null, null, sortOrder);
            case SUBSCRIBE:
                return mDb.query(SubscribeContract.Subscribe.TABLE_NAME,
                        null, selection, selectionArgs, null, null, sortOrder);
        }
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public void shutdown() {
        if (mHelper != null) {
            mHelper.close();
            mHelper = null;
            mDb = null;
        }
    }

    /**
     * Listens for timezone changes and disk-no-longer-full events
     */
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                mAlarmManager.scheduleNextAlarm(false /* do not remove alarms */);
            } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                // Try to clean up if things were screwy due to a full disk
                mAlarmManager.scheduleNextAlarm(false /* do not remove alarms */);
            } else if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                mAlarmManager.scheduleNextAlarm(false /* do not remove alarms */);
            }
        }
    };
}
