package com.krueg.questgiver;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.bugsense.trace.BugSenseHandler;
import com.krueg.questgiver.Exceptions.EmptyCursorException;
import com.krueg.questgiver.Exceptions.RGException;
import com.krueg.questgiver.Exceptions.RGRuntimeException;
import com.krueg.questgiver.dataobject.ActivityDO;
import com.krueg.questgiver.dataobject.BaseDO;
import com.krueg.questgiver.dataobject.NotificationDO;
import com.krueg.questgiver.dataobject.StepDO;
import com.krueg.questgiver.dataobject.StepSingleDO;
import com.krueg.questgiver.dataobject.StepWeekDO;
import com.krueg.questgiver.dialog.SimpleIntegerDF;
import com.krueg.questgiver.gamby.RGDate;
import com.krueg.questgiver.gamby.RGMutableDateTime;
import com.krueg.questgiver.views.SwipelessViewPager;

import org.androidannotations.annotations.EActivity;
import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

@SuppressLint("Registered")
@EActivity
public class Landing extends FragmentActivity implements OnSharedPreferenceChangeListener {
	
	//debug option
	private boolean mManualProgressChangeOpt = false;
    private boolean isBugsenseEnabled = true;
    private boolean mCreateTestNotificationOpt = false;
	
	private Menu actionMenu;
	
	public static final int REQUEST_STEP = 9;
	public static final int REQUEST_SETTINGS = 8;
	
	public SwipelessViewPager mPager;
	public ProfilePagerAdapter mPagerAdapter;
	
	public ActionMode moveAct;
	
	private static final int TAB_ACT = 0;
	private static final int TAB_PERK = 1;
	
	public interface ResultTester {
		public void propagateOnResult(int requestCode,int resultCode, Intent intent);
	}
	
	public ResultTester tester;
	
	public Fragment getFrag(int tabn) {
		return getSupportFragmentManager()
			.findFragmentByTag(Singleton.makeFragmentName(R.id.profilePager, tabn));
	}
	
	public static class ProfilePagerAdapter extends FragmentPagerAdapter {

		public ProfilePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int arg0) {
			switch(arg0) {
			case TAB_ACT:	
				return Acts_.newInstance();
				//return new Fragment();
			case TAB_PERK:
				return Projects_.newInstance();
				//return new Fragment();
			}
			
			return Acts.newInstance();
		}

		@Override
		public int getCount() {
			return 2;
		}
		
	}
	
	void setupActionMenu(final Menu menu,final int position) {
        //i feel very lazy today
        MenuItem item1 = menu.findItem(R.id.menuProfileHideComplete);
        boolean state1 = false;
        setHideCompleteOpt(Singleton.opt_hideComplete,item1);
        MenuItem item3 = menu.findItem(R.id.menuSetProgress);
        boolean state3 = mManualProgressChangeOpt;
        MenuItem item4 = menu.findItem(R.id.menuDbDump);
        boolean state4 = mManualProgressChangeOpt;
        MenuItem item5 = menu.findItem(R.id.menuTestNotif);
        boolean state5 = mCreateTestNotificationOpt;
        switch (position) {
            case TAB_PERK:
                state1 = true;
                break;
            default:
                state1 = false;
        }
		item1.setEnabled(state1);
		item1.setVisible(state1);
		item3.setEnabled(state3);
		item3.setVisible(state3);
        item4.setEnabled(state4);
        item4.setVisible(state4);
        item5.setEnabled(state5);
        item5.setVisible(state5);
	}
	
	public void testHacks() {
		//NotificationDO.TESTMODE = true;
        mCreateTestNotificationOpt = false;
		mManualProgressChangeOpt = false;
        isBugsenseEnabled = true;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Singleton.init(this);
		testHacks();
        if (isBugsenseEnabled) BugSenseHandler.initAndStartSession(this, "5a071dcd");

		//PREFERENCES STUFF
		PreferenceManager.setDefaultValues(this, R.xml.preferences , false);
		SharedPreferences prefs2 =
				PreferenceManager.getDefaultSharedPreferences(this);
		Singleton.opt_notification60 = prefs2.getBoolean("pref_notif_60pct", true);
        Singleton.opt_notificationOnExpire = prefs2.getBoolean("pref_notif_expire", true);
		Singleton.opt_notificationWeekly = prefs2.getBoolean("pref_notif_dailyweek", true);
        Singleton.opt_hideComplete = prefs2.getBoolean("hideComplete",false);
		Float progress = prefs2.getFloat("progressStore", 0);
		if (Float.isNaN(progress)) progress = 0F;
		long lastDeduction = prefs2.getLong("lastDeductionStore", 0);
		Progress.init(progress, lastDeduction);
		Singleton.opt_threshold = Singleton.toFloat(prefs2.getString("pref_action_showThreshold", "0"));
		DateTimeFormatter fmt = DateTimeFormat.forPattern("HH:mm");
		DateTime dt = fmt.parseDateTime(
				prefs2.getString("pref_action_dailyTime", "08:00") );
		Singleton.opt_dailyNotHour = dt.getHourOfDay();
		Singleton.opt_dailyNotMinute = dt.getMinuteOfHour();
		
		setContentView(R.layout.activity_profile);
		
		
		//VIEW PAGER AND ACTION BAR STUFF
		final ActionBar actionBar = getActionBar();
		mPager = (SwipelessViewPager)findViewById(R.id.profilePager);
		FragmentManager frm = getSupportFragmentManager();
		mPagerAdapter = new ProfilePagerAdapter(frm);
		mPager.setAdapter(mPagerAdapter);
		mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                //hack to make step move mode cancel properly on tab change
                if (moveAct != null) {
                    moveAct.setTag(null);
                    moveAct.finish();
                }

                //hide icons depending on the tab
                if (actionMenu == null) return;
                setupActionMenu(actionMenu, position);
            }
        });
		actionBar.setDisplayShowTitleEnabled(false);
		
		//LOG THINGS ONTO... THE LOG SCREEN
		StepDO.setOnEventListener("actLog", new StepDO.OnEventListener() {
			private String[] text = {
				"",		//0
				"Step complete:",			//1
				"Relevance changed:",	//2
				"Hour cost changed:",	//3
				"Deadline changed:",	//4
				"Step removed:",			//5
				"Weekly reset:",			//6
				"New step:",					//7
				"Not now:",						//8
				"After update",				//9
				"Changed parent",			//10
				"Activated"						//11
			};
			
			@Override
			public boolean onEvent(StepDO step, int what) {
                String t;
                try { t = text[what] + " " + step; }
                catch ( NullPointerException | ArrayIndexOutOfBoundsException e ) {
                    t = "Undefined event text! Fill me up!";
                }
				ActivityDO.logAct(t);
				return false;
			}
		});
		
		
		//NOTIFICATION HOOKS
		StepDO.setOnEventListener("notificationSender",
		new StepDO.OnEventListener() {
			
			//alters the date or deletes an existing not.
			private void setScheduleWeekly(StepDO step,NotificationDO not) {
				StepWeekDO sweek = (StepWeekDO) step;
				if ( sweek.isComplete() ) not.delete();
				else {
					int today = new RGMutableDateTime().getDayOfWeek();
					int missing = sweek.getTotalRepeats()-sweek.getRepeats();
					//if we are "late", schedule for today
					if ( today + missing > 7 && today <= 7 ) {
						MutableDateTime cal = new RGMutableDateTime();
						cal.setDayOfWeek	(today);
						cal.setHourOfDay	(Singleton.opt_dailyNotHour);
						cal.setMinuteOfHour (Singleton.opt_dailyNotMinute);
						not.schedule = cal.getMillis();
						if (not.getId()==null) not.create(); else not.update();
					} else if ( today < 7 ) {	
						//otherwise, schedule just for when needed
						MutableDateTime cal = new RGMutableDateTime();
						cal.setDayOfWeek	(8-missing);
						cal.setHourOfDay	(Singleton.opt_dailyNotHour);
						cal.setMinuteOfHour (Singleton.opt_dailyNotMinute);
						not.schedule = cal.getMillis();
						if (not.getId()==null) not.create(); else not.update();									
					} else not.delete();	//don't schedule for next week
				}
			}

			
			NotificationDO freshWeeklyNot(StepDO step) {
				NotificationDO not2 = new NotificationDO();
				not2.title = 	    step.title;
				not2.desc = 		getString(R.string.notif_dailyweek_smalltext);
				not2.schedule =     0L;
				not2.stepId = 	    step.getId();
				not2.type = 		NotificationDO.TYPE_DAILYWEEK;
				return not2;
			}
			
			@Override
			public boolean onEvent(StepDO step, int what) {	
				//always remove (this is not the same as SQL ON CASCADE
				//bacause not.delete() shall unschedule things...
				if ( what == StepDO.EVENT_REMOVE ) { try {
					Cursor c = NotificationDO.queryMe(step.getId());
					NotificationDO not = new NotificationDO(c);
					not.delete();
					return false;
				} catch (RGRuntimeException e){}}
				
				
				//60% alert and expire alert--------------
				try{
				if ( step instanceof StepSingleDO && ((StepSingleDO)step).isActive() ) {
					
					final StepSingleDO ssingle = (StepSingleDO) step;
					
					switch ( what ) {

						//events that trigger a removal of the notification
						case StepDO.EVENT_COMPLETE: {
                            if (Singleton.opt_notification60) {
                                Cursor c =
                                        NotificationDO.queryMe(ssingle.getId(),
                                                NotificationDO.TYPE_SINGLE60PCT);
                                NotificationDO not = new NotificationDO(c);
                                not.delete();
                                break;
                            }
                            if (Singleton.opt_notificationOnExpire) {
                                Cursor c =
                                        NotificationDO.queryMe(ssingle.getId(),
                                                NotificationDO.TYPE_SINGLEEXPIRED);
                                NotificationDO not = new NotificationDO(c);
                                not.delete();
                            }
                            break;
                        }
						
						
						//events that trigger a new notification
						case StepDO.EVENT_NEW:
						{
                            if (Singleton.opt_notification60) {
                                NotificationDO not = new NotificationDO();
                                not.title = ssingle.title;
                                not.desc = getString(R.string.notif_60pct_smalltext);
                                not.schedule = (long) ((ssingle.getDeadline().getTime() -
                                        ssingle.getActivationDate().getTime()) * 0.6
                                        + ssingle.getActivationDate().getTime());
                                not.stepId = ssingle.getId();
                                not.type = NotificationDO.TYPE_SINGLE60PCT;
                                not.create();
                            }
                            if (Singleton.opt_notificationOnExpire) {
                                NotificationDO not = new NotificationDO();
                                not.title = ssingle.title;
                                not.desc = getString(R.string.notif_expire_smalltext);
                                not.schedule = ssingle.getDeadline().getTime();
                                not.stepId = ssingle.getId();
                                not.type = NotificationDO.TYPE_SINGLEEXPIRED;
                                not.create();
                            }
							
							break;
						}
						
						//events that trigger a new notification
						//(since this event is triggered before the
						//data is changed, the instruction has to be delayed
						//to the next AFTER_UPDATE event)
						case StepDO.EVENT_ALTER_DEADLINE:
						{
                            if ( Singleton.opt_notification60 ) {
                                ssingle.setLocalOnEventListener("60pctnotdeadline", new StepDO.OnEventListener() {
                                    public boolean onEvent(StepDO step, int what) {
                                        if (what != StepDO.EVENT_AFTER_UPDATE) return false;
                                        try {
                                            Cursor c =
                                                    NotificationDO.queryMe(ssingle.getId(),
                                                            NotificationDO.TYPE_SINGLE60PCT);
                                            NotificationDO not = new NotificationDO(c);
                                            not.schedule = (long) ((ssingle.getDeadline().getTime() -
                                                    ssingle.getActivationDate().getTime()) * 0.6
                                                    + ssingle.getActivationDate().getTime());
                                            not.update();
                                        } catch (RGException | RGRuntimeException e) {
                                        }
                                        return true;
                                    }
                                });
                            }
                            if ( Singleton.opt_notificationOnExpire ) {
                                ssingle.setLocalOnEventListener("expirednotdeadline", new StepDO.OnEventListener() {
                                    public boolean onEvent(StepDO step, int what) {
                                        if (what != StepDO.EVENT_AFTER_UPDATE) return false;
                                        try {
                                            Cursor c =
                                                    NotificationDO.queryMe(ssingle.getId(),
                                                            NotificationDO.TYPE_SINGLEEXPIRED);
                                            NotificationDO not = new NotificationDO(c);
                                            not.schedule = ssingle.getDeadline().getTime();
                                            not.update();
                                        } catch (RGException | RGRuntimeException e) {
                                        }
                                        return true;
                                    }
                                });
                            }
							break;
						}
					}
				}
				}catch(RGRuntimeException | RGException e){}
				// ^ RGException = inactive subsequent step. Dont sent notification.
				// ^ PkNotFound = step didnt have a notification (shouldnt happen
				// ^ but can happen for old steps). Ignore.
					
				//weekly expire-----------------------
				if ( Singleton.opt_notificationWeekly && step instanceof StepWeekDO ) {
				
					StepWeekDO sweek = (StepWeekDO) step;
					
					//always create an empty notification stub if it doesnt exist
					NotificationDO not2;
					try {
						Cursor c = NotificationDO
								.queryMe(sweek.getId(),NotificationDO.TYPE_DAILYWEEK);
						not2 = new NotificationDO(c);
					} catch ( RGRuntimeException e ) {
						not2 = freshWeeklyNot(step);
					}
					final NotificationDO not = not2;
					
					switch ( what ) {
						//these events are sent before the changes are made
						case StepDO.EVENT_COMPLETE:
						//case StepDO.EVENT_POSTPONE:
						case StepDO.EVENT_WEEKLY_RESET:
						{
							sweek.setLocalOnEventListener("weeknotcomplete",
							new StepDO.OnEventListener() {
								public boolean onEvent(StepDO step, int what) {
									if ( what != StepDO.EVENT_AFTER_UPDATE ) return false;
									setScheduleWeekly(step,not);
									return true;
								}
							});
							break;
						}
					
						//this event is on the end of the chain
						case StepDO.EVENT_NEW:
							setScheduleWeekly(step,not);
							break;
					}
				}

				return false;
			}
		});

        //DAILY ABANDON NOTIFICATION CHECK

        NotificationDO abandonNot;
        try {
            Cursor c = NotificationDO.queryMeType(NotificationDO.TYPE_ABANDONED);
            abandonNot = new NotificationDO(c);
        } catch (RGRuntimeException e) {
            abandonNot = new NotificationDO();
        }

        abandonNot.title = getString(R.string.notif_abandon_title);
        abandonNot.desc = getString(R.string.notif_abandon_text);
        abandonNot.type = NotificationDO.TYPE_ABANDONED;

        RGMutableDateTime dailySchedule = new RGMutableDateTime();
        dailySchedule.setHourOfDay(Singleton.opt_dailyNotHour);
        dailySchedule.setMinuteOfDay(Singleton.opt_dailyNotMinute);
        if ( dailySchedule.getMillis() < new RGMutableDateTime().getMillis() ) {
            dailySchedule.addDays(1);
        }
        abandonNot.schedule = dailySchedule.getMillis();
        try { abandonNot.create(); } catch(RGRuntimeException e) { abandonNot.update(); }

		
	}

	@Override
	protected void onStop() {
		Progress.doDeduction();
		SharedPreferences prefs2 = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = prefs2.edit();
		editor.putFloat("progressStore", Progress.getCurrentValue());
		editor.putLong("lastDeductionStore", Progress.getLastDeduction());
        editor.putBoolean("hideComplete",Singleton.opt_hideComplete);
		editor.commit();
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		actionMenu = menu;
		getMenuInflater().inflate(R.menu.profile, menu);
		setupActionMenu(menu, TAB_ACT);
		return true;
	}

    private void setHideCompleteOpt(boolean inp,MenuItem item) {
        item.setChecked(inp);
        //I tried to use selector drawable, but no way...
        //icon vs state has to be hardcoded because android API
        //wont propagate checked state to a selector drawable here
        int iconid = (item.isChecked()) ? R.drawable.ic_complete_step_white
                : R.drawable.ic_hide_complete_step_white;
        item.setIcon(getResources().getDrawable(iconid));

        Singleton.opt_hideComplete = item.isChecked();
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		if ( item.getItemId() == R.id.menuProfileOpt) {
			Intent intent = new Intent(this, Settings.class);
			startActivityForResult(intent,REQUEST_SETTINGS);
		}
		else if ( item.getItemId() == R.id.menuProfileHideComplete ) {
            setHideCompleteOpt(!item.isChecked(),item);
            Projects proj = (Projects) getFrag(TAB_PERK);
            if ( Singleton.opt_hideComplete ) { proj.hideComplete(); }
            else { proj.popDisabled(); }
		} else if ( item.getItemId() == R.id.menuProfileActLog ) {
			Intent intent = new Intent(this, ActivityLog.class);
			startActivity(intent);			
		} else if ( item.getItemId() == R.id.menuProfileDocs ) {
			Intent intent = new Intent(this,Docs_.class);
			startActivity(intent);
		} else if ( item.getItemId() == R.id.menuProfileNotif ) {
			Intent intent = new Intent(this,Notification.class);
			startActivity(intent);			
		} else if ( item.getItemId() == R.id.menuSetProgress ) {
			SimpleIntegerDF sdf = SimpleIntegerDF.newInstance("Set the points (0-100)",
					new SimpleIntegerDF.SimpleIntegerIF() {
						@Override
						public void onPositive(SimpleIntegerDF dialog, int value) {
							Progress.debugSetCurrentValue(value);
						}
					});
			sdf.show(getSupportFragmentManager(),"anytag");
		} else if ( item.getItemId() == R.id.menuDbDump ) {
            Intent intent = new Intent(this,DbDumper_.class);
            startActivity(intent);
        } else if ( item.getItemId() == R.id.menuTestNotif ) {
            NotificationDO not = new NotificationDO();
            not.desc = "Novo teste";
            not.schedule = new RGMutableDateTime().getMillis()+4000;
            StepDO dummy = new StepDO();
            List<BaseDO> l = dummy.listMe();
            not.stepId = l.get(0).getId();
            not.title = "Novo teste";
            not.type = NotificationDO.TYPE_SINGLE60PCT;
            not.create();
        } else if ( item.getItemId() == R.id.tabDashboard ) {
            mPager.setCurrentItem(TAB_ACT);
        } else if ( item.getItemId() == R.id.tabProjects ) {
            mPager.setCurrentItem(TAB_PERK);
        }


		return true;
	}
	
	@Override
	protected void onActivityResult(int requestcode, int responsecode, Intent intent) {
		super.onActivityResult(requestcode, responsecode, intent);
		if ( tester != null ) 
			tester.propagateOnResult(requestcode, responsecode, intent);
		if ( responsecode != RESULT_OK ) return;
		switch(requestcode) {
		case REQUEST_STEP:
			Projects projects = (Projects) getFrag(TAB_PERK);
			StepDO step = StepDO.newInstance(intent.getIntExtra(Step.STEP_EXTRA, -1));
			projects.animateOnNextRefresh(step);
			break;
		case REQUEST_SETTINGS:
			Acts acts = (Acts) getFrag(TAB_ACT);
			if ( acts != null ) acts.pop();
		default:
			return;
		}
	}
	
	public void newPerk() {
		Intent intent = new Intent(this, Step.class);
		startActivityForResult(intent, Landing.REQUEST_STEP);
	}
	
	/**
	 * Testing
	 */
	public void newPerkAuto(Bundle extras,Integer parentid) {
		Intent intent = new Intent(this, Step.class);
		extras.putBoolean("automated", true);
		if ( parentid != null ) extras.putInt(Step.PARENT_SEND, parentid);
		intent.putExtras(extras);
		startActivityForResult(intent, Landing.REQUEST_STEP);		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

	}

	/**
	 * Testing purposes
	 */
	public static void setNow(long ms) {
		RGDate.setNow(ms);
		RGMutableDateTime.setNow(ms);
	}
	
}