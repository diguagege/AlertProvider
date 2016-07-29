package subscribe.diguagege.com.subscribe;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;

import subscribe.diguagege.com.subscribe.base.SubscribeContract;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
    }
}