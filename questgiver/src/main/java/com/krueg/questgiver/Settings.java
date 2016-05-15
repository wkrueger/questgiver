package com.krueg.questgiver;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.krueg.questgiver.dataobject.NotificationDO;

import java.util.List;

public class Settings extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new FRSettings()).commit();
		}
	}

	public static class FRSettings extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			Preference clearTrash = findPreference("pref_clear_trash");
			clearTrash.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					NotificationDO dummy = new NotificationDO();
					List<NotificationDO> list = dummy.listMe();
					for ( NotificationDO l : list ) l.delete();
					return true;
				}
			});
		}
		
	}

}
