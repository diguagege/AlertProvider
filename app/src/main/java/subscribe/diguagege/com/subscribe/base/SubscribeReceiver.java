package subscribe.diguagege.com.subscribe.base;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hanwei on 16-8-1.
 */
public class SubscribeReceiver extends BroadcastReceiver {
    static final String SCHEDULE = "com.android.calendar.subscribe.action.SCHEDULE_ALARM";

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private PowerManager.WakeLock mWakeLock;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ProviderDebug", "Received BroadCast");
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SubscribeReceiver_Provider");
            mWakeLock.setReferenceCounted(true);
        }
        mWakeLock.acquire();

        final String action = intent.getAction();
        final ContentResolver cr = context.getContentResolver();
        final PendingResult result = goAsync();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                if (action.equals(SCHEDULE)) {
                    cr.update(SubscribeAlarmManager.SCHEDULE_ALARM_URI, null /* values */,
                            null /* where */, null /* selectionArgs */);
                } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                    removeScheduledAlarms(cr);
                }
                result.finish();
                mWakeLock.release();
            }
        });
    }

    /*
    * Remove alarms from the CalendarAlerts table that have been marked
    * as "scheduled" but not fired yet.  We do this because the
    * AlarmManagerService loses all information about alarms when the
    * power turns off but we store the information in a database table
    * that persists across reboots. See the documentation for
    * scheduleNextAlarmLocked() for more information.
    *
    * We don't expect this to be called more than once.  If it were, we would have to
    * worry about serializing the use of the service.
    */
    private void removeScheduledAlarms(ContentResolver resolver) {
        resolver.update(SubscribeAlarmManager.SCHEDULE_ALARM_REMOVE_URI, null /* values */,
                null /* where */, null /* selectionArgs */);
    }
}
