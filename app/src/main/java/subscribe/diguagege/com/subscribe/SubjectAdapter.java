package subscribe.diguagege.com.subscribe;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import subscribe.diguagege.com.subscribe.base.SubscribeContract;

/**
 * Created by hanwei on 16-8-16.
 */
public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder> {
    private LayoutInflater mInflater;
    private Context mContext;
    private DataAdapter mAdapter;
    private ArrayList<Subject> mSubjects = new ArrayList<Subject>();
    private ArrayList<ArrayList<Event>> mEvents = new ArrayList<ArrayList<Event>>();

    public SubjectAdapter(Context context, DataAdapter adapter) {
        mContext = context;
        mAdapter = adapter;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public SubjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.subject_item, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mSubjects.size();
    }

    public void setSubjects(ArrayList<Subject> subjects) {
        mSubjects = subjects;
        notifyDataSetChanged();
    }

    public void setSubjects(Subject[] subjects) {
        mSubjects.clear();
        for(Subject subject : subjects) {
            mSubjects.add(subject);
        }
        notifyDataSetChanged();
    }

    public void setSubscribe(ArrayList<ArrayList<Event>> events) {
        mEvents = events;
        mAdapter.loadEvent();
    }

    @Override
    public void onBindViewHolder(SubjectViewHolder holder, final int position) {
        holder.title.setText(mSubjects.get(position).getTitle());

        holder.subscribeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Subject subject = mSubjects.get(position);
                final long subjectId = subject.getSubjectId();
                ContentValues subjectValues = new ContentValues();
                subjectValues.put(SubscribeContract.Subject.TITLE, subject.getTitle());
                subjectValues.put(SubscribeContract.Subject.TYPE, subject.getType());
                subjectValues.put(SubscribeContract.Subject.SUBJECT_ID, subjectId);
                Uri uri = mContext.getContentResolver().insert(SubscribeContract.Subject.CONTENT_URI, subjectValues);
                if (uri == null) {
                    return;
                }

                final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                ArrayList<Event> eventArr = mEvents.get(position);
                for (int i = 0; i < eventArr.size(); i++) {
                    ops.clear();
                    ContentValues values = new ContentValues();
                    values.put(SubscribeContract.Subscribe.EVENT_ID, eventArr.get(i).getEventId());
                    values.put(SubscribeContract.Subscribe.TITLE, eventArr.get(i).getTitle());
                    values.put(SubscribeContract.Subscribe.DTSTART, eventArr.get(i).getStartTime());
                    values.put(SubscribeContract.Subscribe.DTEND, eventArr.get(i).getEndTime());
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
                        mContext.getContentResolver().applyBatch(SubscribeContract.AUTHORITY, ops);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (OperationApplicationException e) {
                        e.printStackTrace();
                    } finally {
                        mAdapter.loadEvent();
                    }
                }
            }
        });

        holder.unSubscribeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Subject subject = mSubjects.get(position);
                final long subjectId = subject.getSubjectId();
                mContext.getContentResolver().delete(ContentUris.withAppendedId(SubscribeContract.Subject.CONTENT_URI, subjectId)
                        , "subject_id=?", new String[]{String.valueOf(subjectId)});
                mAdapter.loadEvent();
            }
        });
    }


    public class SubjectViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private Button subscribeBtn;
        private Button unSubscribeBtn;

        public SubjectViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.subjectTitle);
            subscribeBtn = (Button) itemView.findViewById(R.id.subscribe);
            unSubscribeBtn = (Button) itemView.findViewById(R.id.unSubscribe);
        }
    }
}

class Subject {
    long subjectId;
    String title;
    long type;

    public long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(long subjectId) {
        this.subjectId = subjectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }
}
