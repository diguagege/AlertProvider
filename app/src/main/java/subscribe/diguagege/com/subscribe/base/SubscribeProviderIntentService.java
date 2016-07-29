package subscribe.diguagege.com.subscribe.base;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by hanwei on 16-7-27.
 */
public class SubscribeProviderIntentService extends IntentService {
    private static final String REMOVE_SUBSCRIBE_ALARM_VALUE = "removeSubscribeAlarms";
    public SubscribeProviderIntentService() {
        super("SubscribeProviderIntentService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (!SubscribeAlarmManager.ACTION_CHECK_NEXT_ALARM.equals(action)) {
            return;
        }
        final SubscribeProviders provider = SubscribeProviders.getInstance();
        // Schedule the next alarm
        final boolean removeAlarms = intent.getBooleanExtra(REMOVE_SUBSCRIBE_ALARM_VALUE, false);
        provider.getOrCreateSubscribeAlarmManager().runScheduleNextAlarm(removeAlarms, provider);
        // Release the wake lock that was set in the Broadcast Receiver
        provider.getOrCreateSubscribeAlarmManager().releaseScheduleNextAlarmWakeLock();
    }
}
