package subscribe.diguagege.com.subscribe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by hanwei on 16-7-27.
 */
public class TestReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Time t = new Time();
        t.set(System.currentTimeMillis());
        Log.d("ProviderDebug", t.format2445());
        Toast.makeText(context, "提醒时间到", Toast.LENGTH_LONG).show();
    }
}
