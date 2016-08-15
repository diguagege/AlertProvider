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
        if (intent.getAction().equals(Intent.ACTION_PROVIDER_CHANGED)) {
            Toast.makeText(context, "provider changed", Toast.LENGTH_LONG).show();
            return;
        }
        Time t = new Time();
        t.set(System.currentTimeMillis());
        Log.d("ProviderDebug", t.format2445());
        Uri uri = intent.getData();
        Cursor alertCursor = null;
        Cursor subscribeCursor = null;
        try {
            if (uri != null) {
                alertCursor = context.getContentResolver().query(SubscribeContract.SubscribeAlerts.CONTENT_URI
                        , null, "alarmTime=?", new String[]{uri.getLastPathSegment()}, null);
                if (alertCursor != null && alertCursor.moveToFirst()) {
                    long subscribeId = alertCursor.getLong(1);
                    subscribeCursor = context.getContentResolver()
                            .query(SubscribeContract.Subscribe.CONTENT_URI, null, "_id=?", new String[]{String.valueOf(subscribeId)}, null, null);
                    if (subscribeCursor != null && subscribeCursor.moveToFirst()) {
                        String title = subscribeCursor.getString(2);
                        Toast.makeText(context, title + "的提醒时间到", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } finally {
            if (alertCursor != null) {
                alertCursor.close();
            }
            if (subscribeCursor != null) {
                subscribeCursor.close();
            }

        }
    }
}
