package subscribe.diguagege.com.subscribe.base;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
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
    protected static final int LINKED = 7;
    protected static final int SUBSCRIBE_ID = 8;
    protected static final int SUBJECT_ID = 9;
    protected static final int SUBSCRIBE_ALERTS_ID = 10;
    private SubscribeDatabaseHelper mHelper;
    private SubscribeAlarmManager mAlarmManager;
    private Context mContext;
    private static SubscribeProviders mInstance;
    private Handler mBroadcastHandler;

    /**
     * Arbitrary integer that we assign to the messages that we send to this
     * thread's handler, indicating that these are requests to send an update
     * notification intent.
     */
    private static final int UPDATE_BROADCAST_MSG = 1;

    /**
     * Any requests to send a PROVIDER_CHANGED intent will be collapsed over
     * this window, to prevent spamming too many intents at once.
     */
    private static final long UPDATE_BROADCAST_TIMEOUT_MILLIS =
            DateUtils.SECOND_IN_MILLIS;

    static {
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "subject", SUBJECT);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "subject/*", SUBJECT_ID);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "subscribe", SUBSCRIBE);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "subscribe/*", SUBSCRIBE_ID);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "reminders", REMINDER);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "subscribe_alerts", SUBSCRIBE_ALERTS);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "subscribe_alerts/*", SUBSCRIBE_ALERTS_ID);
        sUriMatcher.addURI(SubscribeContract.AUTHORITY, "linked", LINKED );
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

        // Fix bug #312008
        mBroadcastHandler = new Handler(mContext.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Context context = SubscribeProviders.this.mContext;
                if (msg.what == UPDATE_BROADCAST_MSG) {
                    // Broadcast a provider changed intent
                    doSendUpdateNotification();
                    // Because the handler does not guarantee message delivery in
                    // the case that the provider is killed, we need to make sure
                    // that the provider stays alive long enough to deliver the
                    // notification. This empty service is sufficient to "wedge" the
                    // process until we stop it here.
                    context.stopService(new Intent(context, EmptyService.class));
                }
            }
        };


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
                sendUpdateNotification(id);
                break;
            case SUBSCRIBE_ID:
                id = mHelper.insertSubscribe(values);
                long subjectId = ContentUris.parseId(uri);
                long subscribeId = (long) values.get(SubscribeContract.Subscribe.EVENT_ID);
                if (subscribeId >= 0) {
                    mHelper.insertLinked(subjectId, subscribeId);
                    sendUpdateNotification(subscribeId);
                }
                break;
            case REMINDER:
                id = mHelper.insertReminders(values);
                // TODO: This may add SubscribeAlerts
                mAlarmManager.scheduleNextAlarm(false);
                break;
            case SUBSCRIBE_ALERTS:
                id = mHelper.insertAlerts(values);
                break;
            case LINKED:
                id = mHelper.insertLinked(values);
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
            case SUBSCRIBE:
                mDb.update(SubscribeDatabaseHelper.Tables.SUBSCRIBE, values, selection, selectionArgs);
                return 0;
            case SUBSCRIBE_ID:
                mDb.update(SubscribeDatabaseHelper.Tables.SUBSCRIBE, values, selection, selectionArgs);
                return 0;
            case REMINDER:
                mDb.update(SubscribeDatabaseHelper.Tables.REMINDER, values, selection, selectionArgs);
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
            case SUBSCRIBE_ALERTS_ID:
                mDb.update(SubscribeDatabaseHelper.Tables.ALERTS, values, selection, selectionArgs);
                return 0;
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
            case SUBJECT_ID:
                mDb.delete(SubscribeContract.Subject.TABLE_NAME, selection, selectionArgs);
                long subjectId = ContentUris.parseId(uri);
                mAlarmManager.trrigerDeleteLink(subjectId);
                break;
            case SUBSCRIBE:
                int deleteCount = mDb.delete(SubscribeContract.Subscribe.TABLE_NAME, selection, selectionArgs);
                Log.d("ProviderDebug", "DeleteCount : " + deleteCount);
                if (deleteCount > 0) {
                    sendUpdateNotification(deleteCount);
                    mAlarmManager.scheduleNextAlarm(false /* do not remove alarms */);
                    Log.d("ProviderDebug", "DeleteReminders");
                }
                break;
            case REMINDER:
                mDb.delete(SubscribeContract.Reminders.TABLE_NAME, selection, selectionArgs);
                break;
            case SUBSCRIBE_ALERTS:
                mDb.delete(SubscribeContract.SubscribeAlerts.TABLE_NAME, selection, selectionArgs);
                break;
            case LINKED:
                final Cursor cursor = mDb.query(SubscribeContract.Linked.TABLE_NAME, null, selection, selectionArgs, null, null, null);
                if (cursor != null) {
                    Log.d("ProviderDebug", "Count : " + cursor.getCount());
                }
                mDb.delete(SubscribeContract.Linked.TABLE_NAME, selection, selectionArgs);
                mAlarmManager.trrigerDeleteSubscribe(cursor);
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
        mDb = mHelper.getWritableDatabase();
        switch (match) {
            case SUBJECT:
                return mDb.query(SubscribeContract.Subject.TABLE_NAME,
                        null, selection, selectionArgs, null, null, sortOrder);
            case SUBSCRIBE:
                return mDb.query(SubscribeContract.Subscribe.TABLE_NAME,
                        null, selection, selectionArgs, null, null, sortOrder);
            case SUBSCRIBE_ALERTS:
                return mDb.query(SubscribeContract.SubscribeAlerts.TABLE_NAME,
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
                updateTimezoneDependentFields();
                mAlarmManager.scheduleNextAlarm(false /* do not remove alarms */);
            } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                // Try to clean up if things were screwy due to a full disk
                updateTimezoneDependentFields();
                mAlarmManager.scheduleNextAlarm(false /* do not remove alarms */);
            } else if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                updateTimezoneDependentFields();
                mAlarmManager.scheduleNextAlarm(false /* do not remove alarms */);
            }
        }
    };


    /**
     * This method use to
     */
    private void checkUpdateSubscribe() {

    }


    /**
     * This creates a background thread to check the timezone and update
     * the timezone dependent fields in the Instances table if the timezone
     * has changed.
     */
    protected void updateTimezoneDependentFields() {
        Thread thread = new TimezoneCheckerThread();
        thread.start();
    }

    private class TimezoneCheckerThread extends Thread {
        @Override
        public void run() {
            doUpdateTimezoneDependentFields();
        }
    }


    /**
     * This method runs in a background thread.  If the timezone has changed
     * then the Instances table will be regenerated.
     */
    protected void doUpdateTimezoneDependentFields() {
        mAlarmManager.rescheduleMissedAlarms();
    }


    /**
     * This method should not ever be called directly, to prevent sending too
     * many potentially expensive broadcasts.  Instead, call
     * {@link #sendUpdateNotification()} instead.
     *
     * @see #sendUpdateNotification()
     */
    private void doSendUpdateNotification() {
        Intent intent = new Intent(Intent.ACTION_PROVIDER_CHANGED,
                SubscribeContract.CONTENT_URI);
        mContext.sendBroadcast(intent, null);
    }


    /**
     * Call this to trigger a broadcast of the ACTION_PROVIDER_CHANGED intent.
     * This also provides a timeout, so any calls to this method will be batched
     * over a period of BROADCAST_TIMEOUT_MILLIS defined in this class.
     *
     */
    private void sendUpdateNotification() {
        // We use -1 to represent an update to all events
        sendUpdateNotification(-1);
    }


    /**
     * Call this to trigger a broadcast of the ACTION_PROVIDER_CHANGED intent.
     * This also provides a timeout, so any calls to this method will be batched
     * over a period of BROADCAST_TIMEOUT_MILLIS defined in this class.  The
     * actual sending of the intent is done in
     * {@link #doSendUpdateNotification()}.
     *
     * TODO add support for eventId
     *
     * @param eventId the ID of the event that changed, or -1 for no specific event
     */
    private void sendUpdateNotification(long eventId) {
        // Are there any pending broadcast requests?
        if (mBroadcastHandler.hasMessages(UPDATE_BROADCAST_MSG)) {
            // Delete any pending requests, before requeuing a fresh one
            mBroadcastHandler.removeMessages(UPDATE_BROADCAST_MSG);
        } else {
            // Because the handler does not guarantee message delivery in
            // the case that the provider is killed, we need to make sure
            // that the provider stays alive long enough to deliver the
            // notification. This empty service is sufficient to "wedge" the
            // process until we stop it here.
            mContext.startService(new Intent(mContext, EmptyService.class));
        }
        // We use a much longer delay for sync-related updates, to prevent any
        // receivers from slowing down the sync
        long delay = UPDATE_BROADCAST_TIMEOUT_MILLIS;
        // Despite the fact that we actually only ever use one message at a time
        // for now, it is really important to call obtainMessage() to get a
        // clean instance.  This avoids potentially infinite loops resulting
        // adding the same instance to the message queue twice, since the
        // message queue implements its linked list using a field from Message.
        Message msg = mBroadcastHandler.obtainMessage(UPDATE_BROADCAST_MSG);
        mBroadcastHandler.sendMessageDelayed(msg, delay);
    }
}
