package subscribe.diguagege.com.subscribe;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;

import subscribe.diguagege.com.subscribe.base.SubscribeContract;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText et = (EditText) findViewById(R.id.edittext);
        Button btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContentResolver().delete(SubscribeContract.Subscribe.CONTENT_URI, "_id=?", new String[]{et.getText().toString()});
            }
        });
//        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
//        for (int i = 0; i < 2; i++) {
//            ContentValues values = new ContentValues();
//            values.put(SubscribeContract.Subscribe.TITLE, "Hello World " + i);
//            values.put(SubscribeContract.Subscribe.DTSTART, System.currentTimeMillis() + (i*DateUtils.MINUTE_IN_MILLIS));
//            values.put(SubscribeContract.Subscribe.DTEND, System.currentTimeMillis() + (i*DateUtils.MINUTE_IN_MILLIS));
//            int eventIdIndex = ops.size();
//            ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(
//                    SubscribeContract.Subscribe.CONTENT_URI).withValues(values);
//            ops.add(b.build());
//            ContentValues reminderValues = new ContentValues();
//            reminderValues.put(SubscribeContract.Reminders.MINUTES, 1);
//            b = ContentProviderOperation.newInsert(SubscribeContract.Reminders.CONTENT_URI).withValues(reminderValues);
//            b.withValueBackReference(SubscribeContract.Reminders.SUBSCRIBE_ID, eventIdIndex);
//            ops.add(b.build());
//        }
//        try {
//            getContentResolver().applyBatch(SubscribeContract.AUTHORITY, ops);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        } catch (OperationApplicationException e) {
//            e.printStackTrace();
//        }



        for (int i = 0; i < 8; i++) {
            ContentValues values = new ContentValues();
            values.put(SubscribeContract.Subscribe.TITLE, "Hello World " + i);
            values.put(SubscribeContract.Subscribe.DTSTART, System.currentTimeMillis() + (i * DateUtils.MINUTE_IN_MILLIS));
            values.put(SubscribeContract.Subscribe.DTEND, System.currentTimeMillis() + (i * DateUtils.MINUTE_IN_MILLIS));
            getContentResolver().insert(SubscribeContract.Subscribe.CONTENT_URI, values);
            ContentValues reminderValues = new ContentValues();
            reminderValues.put(SubscribeContract.Reminders.MINUTES, 1);
            reminderValues.put(SubscribeContract.Reminders.SUBSCRIBE_ID, i + 1);
            getContentResolver().insert(SubscribeContract.Reminders.CONTENT_URI, reminderValues);
        }


//        Cursor c = getContentResolver().query(SubscribeContract.Subscribe.CONTENT_URI, null, null, null, null);
//        if (c != null) {
//            Log.d("ProviderDebug", "Count : " + c.getCount());
//        }

//        ContentValues v = new ContentValues();
//        v.put(SubscribeContract.SubscribeAlerts.SUBSCRIBE_ID, 1);
//        getContentResolver().insert(SubscribeContract.SubscribeAlerts.CONTENT_URI, v);
//
//        Cursor c1 = getContentResolver().query(SubscribeContract.SubscribeAlerts.CONTENT_URI, null, null, null, null);
//        if (c1 != null) {
//            Log.d("ProviderDebug", "Count alerts : " + c1.getCount());
//        }
    }
}
