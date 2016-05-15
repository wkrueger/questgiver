package com.krueg.questgiver;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.krueg.questgiver.dataobject.NotificationDO;
import com.krueg.questgiver.dataobject.StepDO;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.joda.time.DateTime;

import java.util.List;

@EActivity
public class DbDumper extends Activity {

    @ViewById
    TableLayout dbtable;

    void addColumn(ViewGroup view,String text) {
      TextView t = new TextView(this);
      t.setText(text);
      t.setPadding(5,5,5,5);
      view.addView(t);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_dumper);

        NotificationDO dummy = new NotificationDO();
        List<NotificationDO> sList = dummy.listMe();
        for ( NotificationDO not : sList ) {
          TableRow row = new TableRow(this);

          addColumn(row,""+not.getId());
          addColumn(row,not.desc);
          addColumn(row,not.title);
          addColumn(row,new DateTime(not.schedule).toString());
          addColumn(row,""+not.type);
          addColumn(row,StepDO.newInstance(not.stepId).title);

          dbtable.addView(row);
        }
    }
}
