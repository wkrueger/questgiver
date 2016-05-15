package com.krueg.questgiver;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.krueg.questgiver.Singleton.DemSQL;
import com.krueg.questgiver.dataobject.StepDO;
import com.krueg.questgiver.views.StepView;

import java.text.DateFormat;
import java.util.Date;

public class Notification extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }
    }

    static class NotificationAdapter extends CursorAdapter {

        public NotificationAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        class ViewHolder {
            StepView sView;
            TextView desc;
            TextView date;
            
        }
        
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            ViewGroup root = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.listview_notification, null);
            ViewHolder holder = new ViewHolder();
            holder.sView = (StepView) root.findViewById(R.id.frameLayout1);
            holder.desc = (TextView)root.findViewById(R.id.textView2);
            holder.date = (TextView)root.findViewById(R.id.textView3);
            root.setTag(holder);
            return root; 
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            StepDO step = StepDO.newInstance(cursor.getInt(cursor.getColumnIndex("step")));
            holder.sView.setStep(step);
            holder.desc.setText(cursor.getString(cursor.getColumnIndex("desc")));
            holder.date.setText(DateFormat.getDateTimeInstance(
                    DateFormat.SHORT,DateFormat.SHORT).format(new Date(
                            cursor.getLong(cursor.getColumnIndex("schedule")))));
        }
        
    }
    
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_notification,
                    container, false);
            ListView list = (ListView) rootView.findViewById(R.id.listView1);
            String[] args = { "0" };
            Singleton.init(getActivity());
            Cursor c = Singleton.db_r.query(DemSQL.TABLE_NOTIFICATION, null,
                    "checked = ?", args, null, null, "schedule DESC", null);
            NotificationAdapter adapter = 
                    new Notification.NotificationAdapter(getActivity(),c,0);
            list.setAdapter(adapter);
            
            return rootView;
        }
    }

}
