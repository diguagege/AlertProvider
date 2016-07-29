package subscribe.diguagege.com.subscribe.base;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by hanwei on 16-7-27.
 */
public class SubscribeProviderBrocastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ProviderDebug", "Get message");
        Log.d("ProviderDebug", "Action : " + intent.getAction());
        String action = intent.getAction();
        if (!SubscribeAlarmManager.ACTION_CHECK_NEXT_ALARM.equals(action)) {
            setResultCode(Activity.RESULT_CANCELED);
            return;
        }
        final SubscribeProviders provider = SubscribeProviders.getInstance();
        // Acquire a wake lock that will be released when the launched Service is doing its work
        provider.getOrCreateSubscribeAlarmManager().acquireScheduleNextAlarmWakeLock();
        // Set the result code
        setResultCode(Activity.RESULT_OK);
        // Launch the Service
        intent.setClass(context, SubscribeProviderIntentService.class);
        context.startService(intent);
    }
}
