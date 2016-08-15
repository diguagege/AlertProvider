package subscribe.diguagege.com.subscribe;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import subscribe.diguagege.com.subscribe.base.SubscribeContract;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final String[] subjectArr = getResources().getStringArray(R.array.subject_array);
        final String[] rockets = getResources().getStringArray(R.array.rocket);
        final String[] lakers = getResources().getStringArray(R.array.lakers);
        final String[] shelled = getResources().getStringArray(R.array.shelled);
        final String[] rocketsTimes = getResources().getStringArray(R.array.rocket_time);
        final String[] lakersTimes = getResources().getStringArray(R.array.lakers_time);
        final String[] shelledTimes = getResources().getStringArray(R.array.shelled_time);

        final String[] rocketId = getResources().getStringArray(R.array.rocket_id);
        final String[] lakersId = getResources().getStringArray(R.array.lakers_id);
        final String[] shelledId = getResources().getStringArray(R.array.shelled_id);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.dataRecyclerView);
        recyclerView.getItemAnimator().setSupportsChangeAnimations(false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final DataAdapter adapter = new DataAdapter(this);
        recyclerView.setAdapter(adapter);


        Button btn1 = (Button) findViewById(R.id.subscribeLow1);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues subjectValues = new ContentValues();
                subjectValues.put(SubscribeContract.Subject.TITLE, subjectArr[0]);
                subjectValues.put(SubscribeContract.Subject.TYPE, 1);
                subjectValues.put(SubscribeContract.Subject.SUBJECT_ID, 1);
                Uri uri = getContentResolver().insert(SubscribeContract.Subject.CONTENT_URI, subjectValues);
                if (uri == null) {
                    return;
                }
                final long subjectId = 1;
                final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                for (int i = 0; i < rockets.length; i++) {
                    ops.clear();
                    ContentValues values = new ContentValues();
                    values.put(SubscribeContract.Subscribe.EVENT_ID, Integer.valueOf(rocketId[i]));
                    values.put(SubscribeContract.Subscribe.TITLE, rockets[i]);
                    values.put(SubscribeContract.Subscribe.DTSTART, rocketsTimes[i]);
                    values.put(SubscribeContract.Subscribe.DTEND, rocketsTimes[i]);
                    int eventIdIndex = ops.size();

                    ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(
                            ContentUris.withAppendedId(SubscribeContract.Subscribe.CONTENT_URI, subjectId)).withValues(values);
                    ops.add(b.build());

                    ContentValues reminderValues = new ContentValues();
                    reminderValues.put(SubscribeContract.Reminders.MINUTES, 10);
                    b = ContentProviderOperation.newInsert(SubscribeContract.Reminders.CONTENT_URI).withValues(reminderValues);
                    b.withValueBackReference(SubscribeContract.Reminders.SUBSCRIBE_ID, eventIdIndex);
                    ops.add(b.build());

                    try {
                        getContentResolver().applyBatch(SubscribeContract.AUTHORITY, ops);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (OperationApplicationException e) {
                        e.printStackTrace();
                    } finally {
                        adapter.loadEvent();
                    }
                }
            }
        });


        Button unSubscribeBtn1 = (Button) findViewById(R.id.unSubscribeLow1);
        unSubscribeBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContentResolver().delete(ContentUris.withAppendedId(SubscribeContract.Subject.CONTENT_URI, 1)
                        , "subject_id=?", new String[]{"1"});

                adapter.loadEvent();
            }
        });


        Button btn2 = (Button) findViewById(R.id.subscribeLow2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues subjectValues = new ContentValues();
                subjectValues.put(SubscribeContract.Subject.TITLE, subjectArr[1]);
                subjectValues.put(SubscribeContract.Subject.TYPE, 2);
                subjectValues.put(SubscribeContract.Subject.SUBJECT_ID, 2);
                Uri uri = getContentResolver().insert(SubscribeContract.Subject.CONTENT_URI, subjectValues);
                if (uri == null) {
                    return;
                }
                final long subjectId = 2;
                final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                for (int i = 0; i < lakers.length; i++) {
                    ops.clear();
                    ContentValues values = new ContentValues();
                    values.put(SubscribeContract.Subscribe.EVENT_ID, Integer.valueOf(lakersId[i]));
                    values.put(SubscribeContract.Subscribe.TITLE, lakers[i]);
                    values.put(SubscribeContract.Subscribe.DTSTART, lakersTimes[i]);
                    values.put(SubscribeContract.Subscribe.DTEND, lakersTimes[i]);
                    int eventIdIndex = ops.size();

                    ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(
                            ContentUris.withAppendedId(SubscribeContract.Subscribe.CONTENT_URI, subjectId)).withValues(values);
                    ops.add(b.build());

                    ContentValues reminderValues = new ContentValues();
                    reminderValues.put(SubscribeContract.Reminders.MINUTES, 10);
                    b = ContentProviderOperation.newInsert(SubscribeContract.Reminders.CONTENT_URI).withValues(reminderValues);
                    b.withValueBackReference(SubscribeContract.Reminders.SUBSCRIBE_ID, eventIdIndex);
                    ops.add(b.build());

                    try {
                        getContentResolver().applyBatch(SubscribeContract.AUTHORITY, ops);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (OperationApplicationException e) {
                        e.printStackTrace();
                    } finally {
                        adapter.loadEvent();
                    }

                }
            }
        });

        Button unSubscribeBtn2 = (Button) findViewById(R.id.unSubscribeLow2);
        unSubscribeBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContentResolver().delete(ContentUris.withAppendedId(SubscribeContract.Subject.CONTENT_URI, 2)
                        , "subject_id=?", new String[]{"2"});
                adapter.loadEvent();
            }
        });


        Button btn3 = (Button) findViewById(R.id.subscribeLow3);
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues subjectValues = new ContentValues();
                subjectValues.put(SubscribeContract.Subject.TITLE, subjectArr[2]);
                subjectValues.put(SubscribeContract.Subject.TYPE, 3);
                subjectValues.put(SubscribeContract.Subject.SUBJECT_ID, 3);
                Uri uri = getContentResolver().insert(SubscribeContract.Subject.CONTENT_URI, subjectValues);
                if (uri == null) {
                    return;
                }
                final long subjectId = 3;
                final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                for (int i = 0; i < shelled.length; i++) {
                    ops.clear();
                    ContentValues values = new ContentValues();
                    values.put(SubscribeContract.Subscribe.EVENT_ID, Integer.valueOf(shelledId[i]));
                    values.put(SubscribeContract.Subscribe.TITLE, shelled[i]);
                    values.put(SubscribeContract.Subscribe.DTSTART, shelledTimes[i]);
                    values.put(SubscribeContract.Subscribe.DTEND, shelledTimes[i]);
                    int eventIdIndex = ops.size();

                    ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(
                            ContentUris.withAppendedId(SubscribeContract.Subscribe.CONTENT_URI, subjectId)).withValues(values);
                    ops.add(b.build());

                    ContentValues reminderValues = new ContentValues();
                    reminderValues.put(SubscribeContract.Reminders.MINUTES, 10);
                    b = ContentProviderOperation.newInsert(SubscribeContract.Reminders.CONTENT_URI).withValues(reminderValues);
                    b.withValueBackReference(SubscribeContract.Reminders.SUBSCRIBE_ID, eventIdIndex);
                    ops.add(b.build());

                    try {
                        getContentResolver().applyBatch(SubscribeContract.AUTHORITY, ops);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (OperationApplicationException e) {
                        e.printStackTrace();
                    } finally {
                        adapter.loadEvent();
                    }
                }
            }
        });

        Button unSubscribeBtn3 = (Button) findViewById(R.id.unSubscribeLow3);
        unSubscribeBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContentResolver().delete(ContentUris.withAppendedId(SubscribeContract.Subject.CONTENT_URI, 3)
                        , "subject_id=?", new String[]{"3"});
                adapter.loadEvent();
            }
        });
    }
}
