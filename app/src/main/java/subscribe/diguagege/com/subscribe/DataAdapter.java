package subscribe.diguagege.com.subscribe;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import subscribe.diguagege.com.subscribe.base.SubscribeContract;

/**
 * Created by hanwei on 16-8-15.
 */
public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    private LayoutInflater mInflater;
    private Context mContext;
    public ArrayList<Event> mEvents = new ArrayList<Event>();
    private Handler mHandler = new Handler();

    public static final String SORT_BY_TIME = SubscribeContract.Subscribe.DTSTART + " ASC";

    public DataAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadEvent();
            }
        }, 1000);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.subscribe_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.tvTime.setText(mEvents.get(position).getTime());
        holder.subscribeTitle.setText(mEvents.get(position).getTitle());
    }

    @Override
    public int getItemCount() {
        return mEvents.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTime;
        private TextView subscribeTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            tvTime = (TextView) itemView.findViewById(R.id.time);
            subscribeTitle = (TextView) itemView.findViewById(R.id.subscribeTitle);
        }
    }

    public void loadEvent() {
        Cursor c = mContext.getContentResolver().query(SubscribeContract.Subscribe.CONTENT_URI
                , null, null, null, SORT_BY_TIME);
        ArrayList<Event> events = new ArrayList<Event>();
        try {
            while (c != null && c.moveToNext()) {
                Event event = new Event();
                event.setTitle(c.getString(2));
                long time = c.getLong(5);
                Time t = new Time();
                t.set(time);
                event.setTime(t.format2445());
                events.add(event);
            }
            mEvents = events;
            notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
    }
}

class Event {
    public String time;
    public String title;
    public long eventId;
    public long startTime;
    public long endTime;

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
