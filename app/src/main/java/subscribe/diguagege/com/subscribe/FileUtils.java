package subscribe.diguagege.com.subscribe;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by hanwei on 16-8-16.
 */
public class FileUtils {
    private static ArrayList<Subject> mSubjects = new ArrayList<Subject>();
    private static ArrayList<ArrayList<Event>> mEvents = new ArrayList<ArrayList<Event>>();

    public static ArrayList<Subject> getSubjects() {
        return mSubjects;
    }

    public static void setSubjects(ArrayList<Subject> mSubjects) {
        FileUtils.mSubjects = mSubjects;
    }

    public static ArrayList<ArrayList<Event>> getEvents() {
        return mEvents;
    }

    public static void setEvents(ArrayList<ArrayList<Event>> mEvents) {
        FileUtils.mEvents = mEvents;
    }

    public static void readSubjectLine() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/sdcard/Subject.txt"));
            String line = null;
            mSubjects.clear();
            while ((line = br.readLine()) != null) {
                Subject subject = new Subject();
                String[] subjects = line.trim().split(" ");
                subject.setSubjectId(Long.valueOf(subjects[0]));
                subject.setTitle(subjects[1]);
                subject.setType(Long.valueOf(subjects[2]));
                mSubjects.add(subject);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readSubscribeLine() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/sdcard/Subscribe.txt"));
            String line = null;
            mEvents.clear();
            int i = 0;
            mEvents.add(new ArrayList<Event>());
            while ((line = br.readLine()) != null) {
                if (line.equals("==")) {
                    i++;
                    mEvents.add(new ArrayList<Event>());
                    continue;
                }
                Event event = new Event();
                String[] events = line.trim().split(" ");
                event.setEventId(Long.valueOf(events[0]));
                event.setTitle(events[1]);
                event.setStartTime(Long.valueOf(events[2]));
                event.setEndTime(Long.valueOf(events[2]));
                mEvents.get(i).add(event);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
