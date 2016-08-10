package subscribe.diguagege.com.subscribe.base;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by hanwei on 16-7-26.
 */
public class SubscribeAlarmManager {
    protected Context mContext;
    private AlarmManager mAlarmManager;
    static final String SCHEDULE_ALARM_PATH = "schedule_alarms";
    static final String SCHEDULE_ALARM_REMOVE_PATH = "schedule_alarms_remove";
    private static final String SCHEDULE_NEXT_ALARM_WAKE_LOCK = "ScheduleNextAlarmWakeLock";

    static final Uri SCHEDULE_ALARM_REMOVE_URI = Uri.withAppendedPath(
            SubscribeContract.CONTENT_URI, SCHEDULE_ALARM_REMOVE_PATH);
    static final Uri SCHEDULE_ALARM_URI = Uri.withAppendedPath(
            SubscribeContract.CONTENT_URI, SCHEDULE_ALARM_PATH);

    private static final String REMOVE_SUBSCRIBE_ALARM_VALUE = "removeSubscribeAlarms";
    protected static final String ACTION_CHECK_NEXT_ALARM =
            "com.android.calendar.subscribe.check_next_alarm";
    /**
     * We search backward in time for event reminders that we may have missed
     * and schedule them if the event has not yet expired. The amount in the
     * past to search backwards is controlled by this constant. It should be at
     * least a few minutes to allow for an event that was recently created on
     * the web to make its way to the phone. Two hours might seem like overkill,
     * but it is useful in the case where the user just crossed into a new
     * timezone and might have just missed an alarm.
     */
    private static final long SCHEDULE_ALARM_SLACK = 2 * DateUtils.HOUR_IN_MILLIS;

    /**
     * Alarms older than this threshold will be deleted from the CalendarAlerts
     * table. This should be at least a day because if the timezone is wrong and
     * the user corrects it we might delete good alarms that appear to be old
     * because the device time was incorrectly in the future. This threshold
     * must also be larger than SCHEDULE_ALARM_SLACK. We add the
     * SCHEDULE_ALARM_SLACK to ensure this. To make it easier to find and debug
     * problems with missed reminders, set this to something greater than a day.
     */
    private static final long CLEAR_OLD_ALARM_THRESHOLD = 7 * DateUtils.DAY_IN_MILLIS
            + SCHEDULE_ALARM_SLACK;

    /**
     * Used for tracking if the next alarm is already scheduled
     */
    protected AtomicBoolean mNextAlarmCheckScheduled;
    /**
     * Used to keep the process from getting killed while scheduling alarms
     */
    private final PowerManager.WakeLock mScheduleNextAlarmWakeLock;

    static final int ALARM_CHECK_DELAY_MILLIS = 5000;

    public SubscribeAlarmManager(Context context) {
        initializeWithContext(context);

        PowerManager powerManager = (PowerManager) context.getSystemService(
                Context.POWER_SERVICE);
        // Create a wake lock that will be used when we are actually
        // scheduling the next alarm
        mScheduleNextAlarmWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, SCHEDULE_NEXT_ALARM_WAKE_LOCK);
        // We want the Wake Lock to be reference counted (so that we dont
        // need to take care
        // about its reference counting)
        mScheduleNextAlarmWakeLock.setReferenceCounted(true);
    }

    protected void initializeWithContext(Context context) {
        mContext = context;
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mNextAlarmCheckScheduled = new AtomicBoolean(false);
    }

    void trrigerDeleteLink(int subjectId) {
        String selection = SubscribeContract.Linked.SUBJECT_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(subjectId)};
        final ContentResolver resolver = mContext.getContentResolver();
        SubscribeContract.Linked.delete(resolver, selection, selectionArgs);
    }

    void trrigerDeleteSubscribe(Cursor cursor) {
        if (cursor == null) {
            return;
        }
        try {
            final ContentResolver resolver = mContext.getContentResolver();
            final ArrayList<ContentProviderOperation> ops
                    = new ArrayList<ContentProviderOperation>();
            while (cursor != null && cursor.moveToNext()) {
                long id = cursor.getLong(2);
                ContentProviderOperation.Builder builder = ContentProviderOperation.newDelete(SubscribeContract.Subscribe.CONTENT_URI);
                String selection = "_id=? AND 0=(SELECT count(*) FROM " + SubscribeDatabaseHelper.Tables.LINKED + " WHERE " + SubscribeContract.Linked.SUBSCRIBE_ID +"=?)";
                String[] selectionArgs = new String[]{String.valueOf(id), String.valueOf(id)};
                builder.withSelection(selection, selectionArgs);
                ops.add(builder.build());
            }

            resolver.applyBatch(SubscribeContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    void scheduleNextAlarm(boolean removeAlarms) {
        if (!mNextAlarmCheckScheduled.getAndSet(true) || removeAlarms) {
            Intent intent = new Intent(ACTION_CHECK_NEXT_ALARM);
            intent.putExtra(REMOVE_SUBSCRIBE_ALARM_VALUE, removeAlarms);
            PendingIntent pending = PendingIntent.getBroadcast(mContext, 0 /* ignored */, intent,
                    PendingIntent.FLAG_NO_CREATE);
            if (pending != null) {
                // Cancel any previous Alarm check requests
                cancel(pending);
            }
            pending = PendingIntent.getBroadcast(mContext, 0 /* ignored */, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            // Trigger the check in 5s from now
            long triggerAtTime = SystemClock.elapsedRealtime() + ALARM_CHECK_DELAY_MILLIS;
            int alarmType = AlarmManager.ELAPSED_REALTIME;
            set(alarmType, triggerAtTime, pending);
        }
    }

    void runScheduleNextAlarm(boolean removeAlarms, SubscribeProviders provider) {
        SQLiteDatabase db = provider.mDb;
        if (db == null) {
            return;
        }

        // Reset so that we can accept other schedules of next alarm
        mNextAlarmCheckScheduled.set(false);
        db.beginTransaction();
        try {
            if (removeAlarms) {
                removeScheduledAlarmsLocked(db);
            }
            scheduleNextAlarmLocked(db, provider);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void releaseScheduleNextAlarmWakeLock() {
        try {
            getScheduleNextAlarmWakeLock().release();
        } catch (RuntimeException e) {

        }
    }

    public void cancel(PendingIntent operation) {
        mAlarmManager.cancel(operation);
    }

    public void set(int type, long triggerAtTime, PendingIntent operation) {
        mAlarmManager.setExact(type, triggerAtTime, operation);
    }

    PowerManager.WakeLock getScheduleNextAlarmWakeLock() {
        return mScheduleNextAlarmWakeLock;
    }

    void acquireScheduleNextAlarmWakeLock() {
        getScheduleNextAlarmWakeLock().acquire();
    }

    /**
     * Removes the entries in the SubscribeAlerts table for alarms that we have
     * scheduled but that have not fired yet. We do this to ensure that we don't
     * miss an alarm. The SubscribeAlerts table keeps track of the alarms that we
     * have scheduled but the actual alarm list is in memory and will be cleared
     * if the phone reboots. We don't need to remove entries that have already
     * fired, and in fact we should not remove them because we need to display
     * the notifications until the user dismisses them. We could remove entries
     * that have fired and been dismissed, but we leave them around for a while
     * because it makes it easier to debug problems. Entries that are old enough
     * will be cleaned up later when we schedule new alarms.
     */
    private static void removeScheduledAlarmsLocked(SQLiteDatabase db) {
        db.delete(SubscribeContract.SubscribeAlerts.TABLE_NAME, SubscribeContract.SubscribeAlerts.STATE + "="
                + SubscribeContract.SubscribeAlerts.STATE_SCHEDULED, null /* whereArgs */);
    }


    private void scheduleNextAlarmLocked(SQLiteDatabase db, SubscribeProviders providers) {
        final long currentMillis = System.currentTimeMillis();
        final long start = currentMillis - SCHEDULE_ALARM_SLACK;
        final long end = start + (24 * 60 * 60 * 1000);
        // Delete alerts where alertTime < currentMillis - 2HourMillis
        String selectArg[] = new String[] { Long.toString(
                currentMillis - CLEAR_OLD_ALARM_THRESHOLD) };

        // TODO : Need delete SubscribeAlerts table row where AlertTime < currentMillis - 2HourMillis
        int rowsDeleted = db.delete(
                SubscribeContract.SubscribeAlerts.TABLE_NAME, SubscribeContract.SubscribeAlerts.ALARM_TIME + "<?", selectArg);

        long nextAlarmTime = end;
        final ContentResolver resolver = mContext.getContentResolver();
        final long tmpAlarmTime = SubscribeContract.SubscribeAlerts.findNextAlarmTime(resolver, currentMillis);
        if (tmpAlarmTime != -1 && tmpAlarmTime < nextAlarmTime) {
            nextAlarmTime = tmpAlarmTime;
        }

        // TODO: 这里搜出来的需要连表排除无用的提醒，已经提醒过的提醒（提醒之后没被删掉的）
        String query = "SELECT s."+ SubscribeContract.Subscribe._ID +",s." + SubscribeContract.Subscribe.DTSTART + ",s." + SubscribeContract.Subscribe.DTEND +
                ",r." + SubscribeContract.Reminders.MINUTES + ",s." + SubscribeContract.Subscribe.DTSTART + "-(r." + SubscribeContract.Reminders.MINUTES + "*" + DateUtils.MINUTE_IN_MILLIS + ") AS alertTime" +
                " FROM Subscribe AS s, " +
                "Reminders AS r " +
                "WHERE s."+ SubscribeContract.Subscribe._ID + "=r." + SubscribeContract.Reminders.SUBSCRIBE_ID + " " +
                "AND alertTime>=CAST(? AS INT) " +
                "AND alertTime<=CAST(? AS INT) " +
                "AND 0=(SELECT count(*) FROM " + SubscribeDatabaseHelper.Tables.ALERTS + " AS CA" + " WHERE CA."
                + SubscribeContract.SubscribeAlerts.SUBSCRIBE_ID + "=s." + SubscribeContract.Subscribe._ID + " AND CA." + SubscribeContract.SubscribeAlerts.BEGIN + "="
                + SubscribeContract.Subscribe.DTSTART + " AND CA." + SubscribeContract.SubscribeAlerts.ALARM_TIME + "=alertTime)"
                + " ORDER BY alertTime," + SubscribeContract.Subscribe.DTSTART + "," + SubscribeContract.Subscribe.TITLE;

        String queryParams[] = new String[]{String.valueOf(start)
                                          , String.valueOf(nextAlarmTime)};

        Log.d("ProviderDebug", "start : " + start);

        Cursor cursor = null;
        try {
            // 通过Subscribe表与Reminder表连表，查出需要提醒的事件，并将记录至Subscribe_Alerts表中
            cursor = db.rawQuery(query, queryParams);
            int subscribeIdIndex = cursor.getColumnIndex(SubscribeContract.Subscribe._ID);
            int startTimeIndex = cursor.getColumnIndex(SubscribeContract.Subscribe.DTSTART);
            int endTimeIndex = cursor.getColumnIndex(SubscribeContract.Subscribe.DTEND);
            int alarmTimeIndex = cursor.getColumnIndex("alertTime");
            int minutesIndex = cursor.getColumnIndex(SubscribeContract.Reminders.MINUTES);

            while (cursor.moveToNext()) {
                final long alarmTime = cursor.getLong(alarmTimeIndex);
                final long subscribeId = cursor.getLong(subscribeIdIndex);
                final int minutes = cursor.getInt(minutesIndex);
                final long startTime = cursor.getLong(startTimeIndex);
                final long endTime = cursor.getLong(endTimeIndex);

                if (alarmTime < nextAlarmTime) {
                    nextAlarmTime = alarmTime;
                } else if (alarmTime > nextAlarmTime + DateUtils.MINUTE_IN_MILLIS) {
                    // This event alarm (and all later ones) will be scheduled
                    // later.
                    break;
                }

                if (SubscribeContract.SubscribeAlerts.alarmExists(resolver, subscribeId, startTime, alarmTime)) {
                    Log.d("ProviderDebug", "alarmExists");
                    continue;
                }

                // Insert this alarm into the CalendarAlerts table
                Uri uri = SubscribeContract.SubscribeAlerts.insert(
                        resolver, subscribeId, startTime, endTime, alarmTime, minutes);
                if (uri == null) {
                    Log.d("ProviderDebug", "uri == null");
                    continue;
                }

                scheduleAlarm(alarmTime);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Refresh notification bar
        if (rowsDeleted > 0) {
            Log.d("ProviderDebug", "Refresh notification bar");
            scheduleAlarm(currentMillis);
        }
        // If we scheduled an event alarm, then schedule the next alarm check
        // for one minute past that alarm. Otherwise, if there were no
        // event alarms scheduled, then check again in 24 hours. If a new
        // event is inserted before the next alarm check, then this method
        // will be run again when the new event is inserted.
        if (nextAlarmTime != Long.MAX_VALUE) {
            Time t = new Time();
            t.set(nextAlarmTime);
            Log.d("ProviderDebug", "NextAlarmTime : " + t.format2445());
            scheduleNextAlarmCheck(nextAlarmTime + DateUtils.MINUTE_IN_MILLIS);
        } else {
            scheduleNextAlarmCheck(currentMillis + DateUtils.DAY_IN_MILLIS);
        }

    }

    void scheduleNextAlarmCheck(long triggerTime) {
        Intent intent = new Intent(SubscribeReceiver.SCHEDULE);
        intent.setClass(mContext, SubscribeReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(
                mContext, 0, intent, PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            // Cancel any previous alarms that do the same thing.
            cancel(pending);
        }
        pending = PendingIntent.getBroadcast(
                mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Time time = new Time();
        time.set(triggerTime);
        String timeStr = time.format(" %a, %b %d, %Y %I:%M%P");
        Log.d("ProviderDebug", "scheduleNextAlarmCheck at: " + triggerTime + timeStr);


        set(AlarmManager.RTC_WAKEUP, triggerTime, pending);
    }

    public void scheduleAlarm(long alarmTime) {
        Time alarm = new Time();
        alarm.set(alarmTime);
        Log.d("ProviderDebug", "scheduleAlarm : " + alarm.format2445());
        SubscribeContract.SubscribeAlerts.scheduleAlarm(mContext, mAlarmManager, alarmTime);
    }


    void rescheduleMissedAlarms() {
        rescheduleMissedAlarms(mContext.getContentResolver());
    }

    public void rescheduleMissedAlarms(ContentResolver cr) {
        SubscribeContract.SubscribeAlerts.rescheduleMissedAlarms(cr, mContext, mAlarmManager);
    }
}
