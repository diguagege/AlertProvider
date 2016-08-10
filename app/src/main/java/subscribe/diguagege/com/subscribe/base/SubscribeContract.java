package subscribe.diguagege.com.subscribe.base;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

/**
 * Created by hanwei on 16-7-26.
 */
public class SubscribeContract {

    public static final String AUTHORITY = "com.android.calendar.subscribe";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);


    public static final String ACTION_SUBSCRIBE_REMINDER = "com.android.calendar.subscribe.action.SUBSCRIBE_REMINDER";

    public static final class Subscribe implements BaseColumns, SubscribeColumns {
        public static final String TABLE_NAME = "Subscribe";
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/subscribe");
    }

    public static final class Subject implements BaseColumns, SubjectColumns {
        public static final String TABLE_NAME = "Subject";
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/subject");
    }

    public static final class Reminders implements BaseColumns, ReminderColumns {
        public static final String TABLE_NAME = "Reminders";
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/reminders");
    }

    public static final class Linked implements BaseColumns, LinkedColumns {
        public static final String TABLE_NAME = "Linked";
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/linked");

        public static final int delete(ContentResolver cr, String where, String[] selectionArgs) {
            return cr.delete(CONTENT_URI, where, selectionArgs);
        }

    }

    public static final class SubscribeAlerts implements BaseColumns, SubscribeAlertsColumns {
        public static final String TABLE_NAME = "SubscribeAlerts";
        /**
         * This utility class cannot be instantiated
         */
        private SubscribeAlerts() {}

        private static final String WHERE_ALARM_EXISTS = SUBSCRIBE_ID + "=?"
                + " AND " + BEGIN + "=?"
                + " AND " + ALARM_TIME + "=?";

        private static final String WHERE_FINDNEXTALARMTIME = ALARM_TIME + ">=?";
        private static final String SORT_ORDER_ALARMTIME_ASC = ALARM_TIME + " ASC";

        private static final String WHERE_RESCHEDULE_MISSED_ALARMS = STATE + "=" + STATE_SCHEDULED
                + " AND " + ALARM_TIME + "<?"
                + " AND " + ALARM_TIME + ">?"
                + " AND " + END + ">=?";


        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/subscribe_alerts");


        /**
         * Schedules an alarm intent with the system AlarmManager that will
         * notify listeners when a reminder should be fired. The provider will
         * keep scheduled reminders up to date but apps may use this to
         * implement snooze functionality without modifying the reminders table.
         * Scheduled alarms will generate an intent using
         * {@link #ACTION_SUBSCRIBE_REMINDER}. TODO Move to provider
         *
         * @param context A context for referencing system resources
         * @param manager The AlarmManager to use or null
         * @param alarmTime The time to fire the intent in UTC millis since
         *            epoch
         * @hide
         */
        public static void scheduleAlarm(Context context, AlarmManager manager, long alarmTime) {
            if (manager == null) {
                manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            }

            Intent intent = new Intent(ACTION_SUBSCRIBE_REMINDER);
            intent.setData(ContentUris.withAppendedId(SubscribeContract.CONTENT_URI, alarmTime));
            intent.putExtra(ALARM_TIME, alarmTime);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
            manager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pi);
        }

        /**
         * Finds the next alarm after (or equal to) the given time and returns
         * the time of that alarm or -1 if no such alarm exists. This is a
         * blocking call and should not be done on the UI thread. TODO move to
         * provider
         *
         * @param cr the ContentResolver
         * @param millis the time in UTC milliseconds
         * @return the next alarm time greater than or equal to "millis", or -1
         *         if no such alarm exists.
         * @hide
         */
        public static final long findNextAlarmTime(ContentResolver cr, long millis) {
            // TODO: construct an explicit SQL query so that we can add
            // "LIMIT 1" to the end and get just one result.
//            String[] projection = new String[] { ALARM_TIME };
            Cursor cursor = cr.query(CONTENT_URI, null/*projection*/, WHERE_FINDNEXTALARMTIME,
                    (new String[] {
                            Long.toString(millis)
                    }), SORT_ORDER_ALARMTIME_ASC);
            long alarmTime = -1;
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    alarmTime = cursor.getLong(4);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return alarmTime;
        }


        /**
         * Helper for inserting an alarm time associated with an event TODO move
         * to Provider
         *
         * @hide
         */
        public static final Uri insert(ContentResolver cr, long eventId,
                                       long begin, long end, long alarmTime, int minutes) {
            ContentValues values = new ContentValues();
            values.put(SUBSCRIBE_ID, eventId);
            values.put(BEGIN, begin);
            values.put(END, end);
            values.put(ALARM_TIME, alarmTime);
            long currentTime = System.currentTimeMillis();
            values.put(CREATION_TIME, currentTime);
            values.put(RECEIVED_TIME, 0);
            values.put(NOTIFY_TIME, 0);
            values.put(STATE, STATE_SCHEDULED);
            values.put(MINUTES, minutes);
            return cr.insert(CONTENT_URI, values);
        }


        /**
         * Searches for an entry in the CalendarAlerts table that matches the
         * given event id, begin time and alarm time. If one is found then this
         * alarm already exists and this method returns true. TODO Move to
         * provider
         *
         * @param cr the ContentResolver
         * @param subscribeId the event id to match
         * @param begin the start time of the event in UTC millis
         * @param alarmTime the alarm time of the event in UTC millis
         * @return true if there is already an alarm for the given event with
         *         the same start time and alarm time.
         * @hide
         */
        public static final boolean alarmExists(ContentResolver cr, long subscribeId,
                                                long begin, long alarmTime) {
            // TODO: construct an explicit SQL query so that we can add
            // "LIMIT 1" to the end and get just one result.
//            String[] projection = new String[] { ALARM_TIME };
            Cursor cursor = cr.query(CONTENT_URI, null, WHERE_ALARM_EXISTS,
                    (new String[] {
                            Long.toString(subscribeId), Long.toString(begin), Long.toString(alarmTime)
                    }), null);
            boolean found = false;
            try {
                if (cursor != null && cursor.getCount() > 0) {
                    found = true;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return found;
        }


        /**
         * Searches the CalendarAlerts table for alarms that should have fired
         * but have not and then reschedules them. This method can be called at
         * boot time to restore alarms that may have been lost due to a phone
         * reboot. TODO move to provider
         *
         * @param cr the ContentResolver
         * @param context the Context
         * @param manager the AlarmManager
         * @hide
         */
        public static final void rescheduleMissedAlarms(ContentResolver cr,
                                                        Context context, AlarmManager manager) {
            // Get all the alerts that have been scheduled but have not fired
            // and should have fired by now and are not too old.
            long now = System.currentTimeMillis();
            long ancient = now - DateUtils.DAY_IN_MILLIS;
//            String[] projection = new String[] {
//                    ALARM_TIME,
//            };

            // TODO: construct an explicit SQL query so that we can add
            // "GROUPBY" instead of doing a sort and de-dup
            Cursor cursor = cr.query(SubscribeAlerts.CONTENT_URI, null,
                    WHERE_RESCHEDULE_MISSED_ALARMS, (new String[] {
                            Long.toString(now), Long.toString(ancient), Long.toString(now)
                    }), SORT_ORDER_ALARMTIME_ASC);
            if (cursor == null) {
                return;
            }

            try {
                long alarmTime = -1;

                while (cursor.moveToNext()) {
                    long newAlarmTime = cursor.getLong(4);
                    if (alarmTime != newAlarmTime) {
                        scheduleAlarm(context, manager, newAlarmTime);
                        alarmTime = newAlarmTime;
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }


    protected interface SubscribeColumns {
        public static final String EVENT_ID = "event_id";
        public static final String TITLE = "title";
        public static final String ACTION = "action";
        public static final String DESCRIPTION = "description";
        public static final String DTSTART = "dtstart";
        public static final String DTEND = "dtend";
        public static final String ORGINZER = "orginzer";
        public static final String ICON_URL = "icon";
        public static final String EXTEND_DATA1 = "extend_data1";
        public static final String EXTEND_DATA2 = "extend_data2";
        public static final String EXTEND_DATA3 = "extend_data3";
        public static final String EXTEND_DATA4 = "extend_data4";
        public static final String EXTEND_DATA5 = "extend_data5";
        public static final String EXTEND_DATA6 = "extend_data6";
        public static final String EXTEND_DATA7 = "extend_data7";
        public static final String EXTEND_DATA8 = "extend_data8";
        public static final String EXTEND_DATA9 = "extend_data9";
    }

    protected interface SubjectColumns {
        public static final String TITLE = "title";
        public static final String ACTION = "action";
        public static final String DESCRIPTION = "description";
        public static final String ICON_URL = "icon";
        public static final String TYPE = "type";
    }

    protected interface ReminderColumns {
        public static final String SUBSCRIBE_ID = "subscribe_id";
        public static final String MINUTES = "minutes";
    }

    protected interface LinkedColumns {
        public static final String SUBJECT_ID = "subject_id";
        public static final String SUBSCRIBE_ID = "subscribe_id";
    }

    protected interface SubscribeAlertsColumns {
        public static final String SUBSCRIBE_ID = "subscribe_id";
        public static final String BEGIN = "begin";
        public static final String END = "end";
        public static final String ALARM_TIME = "alarmTime";
        public static final String CREATION_TIME = "creationTime";
        public static final String RECEIVED_TIME = "receivedTime";
        public static final String NOTIFY_TIME = "notifyTime";
        public static final String STATE = "state";
        public static final String MINUTES = "minutes";

        /**
         * An alert begins in this state when it is first created.
         */
        public static final int STATE_SCHEDULED = 0;
        /**
         * After a notification for an alert has been created it should be
         * updated to fired.
         */
        public static final int STATE_FIRED = 1;
        /**
         * Once the user has dismissed the notification the alert's state should
         * be set to dismissed so it is not fired again.
         */
        public static final int STATE_DISMISSED = 2;
    }
}
