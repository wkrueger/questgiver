package com.krueg.questgiver;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.krueg.questgiver.dataobject.ActivityDO;

import java.util.List;

public class ActivityLog extends FragmentActivity {
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		
		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ViewGroup.LayoutParams params =
				new ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						80);
		ll.setLayoutParams(params);
		

		ScrollView scroll = new ScrollView(this);
		scroll.setLayoutParams(params);
		scroll.addView(ll);
		
		setContentView(scroll);
		
		ActivityDO dummy = new ActivityDO();
		List <ActivityDO> list = dummy.listMe();
		for ( ActivityDO act :  list ) {
			TextView tw = new TextView(this);
			tw.setLayoutParams(params);
			tw.setText(act.getDate().toString() + " | " + act.text);
			ll.addView(tw);
		}
	}
}
