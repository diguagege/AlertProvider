package subscribe.diguagege.com.subscribe;

import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.Handler;
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
    private Handler mHandler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendBroadcast(new Intent("com.android.calendar.subscribeTest"));
        FileUtils.readSubjectLine();
        FileUtils.readSubscribeLine();

        RecyclerView eventRecyclerView = (RecyclerView) findViewById(R.id.eventRecyclerView);
        eventRecyclerView.getItemAnimator().setSupportsChangeAnimations(false);
        eventRecyclerView.setHasFixedSize(true);
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final DataAdapter dataAdapter = new DataAdapter(this);
        eventRecyclerView.setAdapter(dataAdapter);

        RecyclerView subjectRecyclerView = (RecyclerView) findViewById(R.id.subjectRecyclerView);
        subjectRecyclerView.getItemAnimator().setSupportsChangeAnimations(false);
        subjectRecyclerView.setHasFixedSize(true);
        subjectRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final SubjectAdapter subjectAdapter = new SubjectAdapter(this, dataAdapter);
        subjectRecyclerView.setAdapter(subjectAdapter);

        subjectAdapter.setSubjects(FileUtils.getSubjects());
        subjectAdapter.setSubscribe(FileUtils.getEvents());
    }
}
