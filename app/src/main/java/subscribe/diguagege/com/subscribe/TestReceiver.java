package subscribe.diguagege.com.subscribe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import subscribe.diguagege.com.subscribe.base.SubscribeContract;

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
        Uri uri = intent.getData();
        if (uri != null) {
            Cursor cursor = context.getContentResolver().query(SubscribeContract.SubscribeAlerts.CONTENT_URI
                    , null, "alarmTime=?", new String[]{uri.getLastPathSegment()}, null);
            if (cursor != null && cursor.moveToFirst()) {
                long subscribeId = cursor.getLong(1);
                Log.d("ProviderDebug", "Id : " + subscribeId);
            }
        }
        MainActivity.tv.setText(t.format2445());
    }
}
