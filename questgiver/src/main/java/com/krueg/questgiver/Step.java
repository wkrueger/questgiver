package com.krueg.questgiver;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.krueg.questgiver.Exceptions.PkNotFoundException;
import com.krueg.questgiver.Exceptions.RGException;
import com.krueg.questgiver.dataobject.StepDO;
import com.krueg.questgiver.dataobject.StepParentDO;
import com.krueg.questgiver.dataobject.StepSingleDO;
import com.krueg.questgiver.dataobject.StepSubsequentDO;
import com.krueg.questgiver.dataobject.StepWeekDO;
import com.krueg.questgiver.dialog.DialogBuilder;
import com.krueg.questgiver.dialog.SimpleAlertDF;
import com.krueg.questgiver.views.DeadlinePicker;
import com.krueg.questgiver.views.IntegerWithLabelView;

public class Step extends FragmentActivity {

	private StepParentDO maybeParent;
	private IntegerWithLabelView mRepeats;
	private DeadlinePicker mDeadline;
	private IntegerWithLabelView mRelevance;
	private ViewGroup mRecOptLW;
	private ViewGroup mDeadLineLW;
	private CheckedTextView ctw;
	private IntegerWithLabelView mHourcostSingle;
	private IntegerWithLabelView mHourcostWeekly;
	
	private IntegerWithLabelView mStandardRelevance;
	private IntegerWithLabelView mGroupRelevance;
	
	enum StepTypes {
		PARENT,
		SINGLE,
		WEEKLY
	};
	
	private StepTypes mTypeOpt;
	
	static public String PARENT_SEND = "STEP_SEND";
	static public String STEP_EXTRA = "STEP_EXTRA";
	
	//testing stuff
	DialogBuilder.ListItem itemParent;
	DialogBuilder.ListItem itemSingle;
	DialogBuilder.ListItem itemDaily;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_step);
		
		Intent intent = getIntent();
		int parentId = intent.getIntExtra(PARENT_SEND, -1);
		maybeParent = ( parentId >= 0 ) ? (StepParentDO)StepDO.newInstance(parentId) :
			null;
		
		//default margin for sub-options
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(Singleton.dpToPx(20), 0, 0, 0);
		
		//WEEKLY STEP OPTIONS
		mRecOptLW = (ViewGroup) findViewById(R.id.stepRecurrencyLW);
		mRepeats = new IntegerWithLabelView(this);
		mRepeats.setBounds(1, 5);
		mRepeats.setText(getString(R.string.step_set_repeats));
		mRecOptLW.addView(mRepeats,params);
		mHourcostWeekly = new IntegerWithLabelView(this);
		mHourcostWeekly.setText(getString(R.string.step_hourcost_desc));
		mHourcostWeekly.setValue(1);
		mRecOptLW.addView(mHourcostWeekly,params);
		
		//RELEVANCE
		mRelevance = (IntegerWithLabelView) findViewById(R.id.relevanceView);
		mStandardRelevance = mRelevance;
		mGroupRelevance = (IntegerWithLabelView) findViewById(R.id.relevanceViewForGroups);
		
		//SINGLE STEP OPTIONS
		mDeadLineLW = (ViewGroup) findViewById(R.id.stepDeadlineLW);	
		mDeadline = new DeadlinePicker(this); 
		mDeadline.setText(getString(R.string.step_set_deadline));
		mDeadline.setValue(24);
		mDeadLineLW.addView(mDeadline,params);
		mHourcostSingle = new IntegerWithLabelView(this);
		mHourcostSingle.setText(getString(R.string.step_hourcost_desc));
		mHourcostSingle.setValue(1);
		mDeadLineLW.addView(mHourcostSingle,params);
		
		ctw = (CheckedTextView) getLayoutInflater().inflate(R.layout.sub_checked_tw, null);
			ctw.setText(R.string.step_initally_disabled);
			ctw.setOnClickListener(new OnClickListener() {public void onClick(View v) {
				CheckedTextView ct = (CheckedTextView)v;
				ct.setChecked(!ct.isChecked());
			}});
		mDeadLineLW.addView(ctw,params);
		
		//TYPE SELECTOR
		final TextView typeTW = (TextView) findViewById(R.id.stepType);
		typeTW.setText(R.string.step_recurrency_single);
		
		final DialogBuilder builder = new DialogBuilder(getString(R.string.type),
				Step.this);
		
		//parent
		itemParent = builder.new ListItem();
		itemParent.title = getString(R.string.step_recurrency_parent);
		itemParent.act =  new DialogBuilder.ItemAction() {
			public void act() {
				mRecOptLW.setVisibility(View.GONE);
				mDeadLineLW.setVisibility(View.GONE);
				typeTW.setText(R.string.step_recurrency_parent);
				mTypeOpt = StepTypes.PARENT;
				mRelevance.setVisibility(View.GONE);
				mRelevance = mGroupRelevance;
				mRelevance.setVisibility(View.VISIBLE);
			}
		};
		
		if ( maybeParent != null ) {
			//single
			itemSingle = builder.new ListItem();
			itemSingle.title = getString(R.string.step_recurrency_single);
			itemSingle.act = new DialogBuilder.ItemAction() {
				public void act() {
					mRecOptLW.setVisibility(View.GONE);
					mDeadLineLW.setVisibility(View.VISIBLE);
					typeTW.setText(R.string.step_recurrency_single);
					mTypeOpt = StepTypes.SINGLE;
					mRelevance.setVisibility(View.GONE);
					mRelevance = mStandardRelevance;
					mRelevance.setVisibility(View.VISIBLE);					
				}
			};
			
			
			//weekly
			itemDaily = builder.new ListItem();
			itemDaily.title = getString(R.string.step_recurrency_daily);
			itemDaily.act = new DialogBuilder.ItemAction() {
				public void act() {
					mRecOptLW.setVisibility(View.VISIBLE);
					mDeadLineLW.setVisibility(View.GONE);
					typeTW.setText(R.string.step_recurrency_daily);
					mTypeOpt = StepTypes.WEEKLY;
					mRelevance.setVisibility(View.GONE);
					mRelevance = mStandardRelevance;
					mRelevance.setVisibility(View.VISIBLE);
				}
			};
			
			builder.listItems.add(itemSingle);
			builder.listItems.add(itemDaily);
			
			//init case
			mTypeOpt = StepTypes.SINGLE;
			itemSingle.act.act();
		} else {
			mTypeOpt = StepTypes.PARENT;
			itemParent.act.act();
		}
		builder.listItems.add(itemParent);
		
		typeTW.setOnClickListener( new OnClickListener() {
			public void onClick(View v) {
				builder.buildDialog().show();	
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		if ( intent.getBooleanExtra("automated", false) ) automatedFillUp();
	}
	
	/**
	 * "Testable"
	 */
	public void automatedFillUp() {

		new Thread(new Runnable() {
			
			@Override
			public void run() {
				Intent intent = getIntent();
				
				//INPUT
				final String text = intent.getStringExtra("title");
				final String stepType = intent.getStringExtra("type");
				final int arg0 = intent.getIntExtra("deadline", 24);		//deadline magic number
				final int arg1 = intent.getIntExtra("relevance", 4);		//relevance magic number
				final int arg2 = intent.getIntExtra("hourcost", 1);			//hourcost magic number
				final int arg3 = intent.getIntExtra("repeats", 2);			//repeats magic number
				
				try {
					Thread.sleep(1000);
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							//FILL UP
							((EditText)findViewById(R.id.stepTitle)).setText(text);
							
							if 				( stepType.equals("StepSingleDO") ) 	itemSingle.act.act();
							else if		( stepType.equals("StepSubsequentDO") ) {
								itemSingle.act.act();
								ctw.setChecked(true);
							}
							else if		( stepType.equals("StepParentDO") )		itemParent.act.act();
							else if		( stepType.equals("StepWeekDO") )			itemDaily.act.act();
						}
					});
				
					Thread.sleep(1000);
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							mRelevance.setValue(arg1);
							mDeadline.setValue(arg0);
							mHourcostSingle.setValue(arg2);
							mHourcostWeekly.setValue(arg2);
							mRepeats.setValue(arg3);	
						}
						
					});
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						submitThis();
					}
					
				});
			}
		}).start();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.new_step, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_step_ok:
			submitThis();
			return true;
		case R.id.menu_step_cancel:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	StepDO fillCommon(StepDO step,String title,
			StepParentDO maybeparent) {
		step.title = title;
		if ( maybeparent != null ) maybeparent.addChild(step);
		else ((StepParentDO)step).setAsRoot();
		step.setRelevance(mRelevance.getValue());
		return step;
	}

    /**
     * Submits the page
     */
	@SuppressWarnings("null")
	private void submitThis(){
		String stitle = ((EditText)findViewById(R.id.stepTitle)).getText().toString();
		StepDO gstep = null;	//pointerexception
		
		boolean failFlag = false;
		Singleton.db_w.beginTransaction();
		try {
			if ( stitle.length() < 1 ) throw new RGException(getString(R.string.err_step_title_min_size));

			if ( mTypeOpt == StepTypes.WEEKLY ) {
				gstep = new StepWeekDO();
				fillCommon(gstep,stitle,maybeParent);
				((StepWeekDO)gstep).setTotalRepeats((int) mRepeats.getValue());
				((StepWeekDO)gstep).setHourCost((int) mHourcostWeekly.getValue());
				gstep.create();
			}
			else if ( mTypeOpt == StepTypes.SINGLE ){
				if ( ctw.isChecked() ) {	//SUBSEQUENT (SINGLE DISABLED)
					gstep = new StepSubsequentDO();
				} else {	//SINGLE
					gstep = new StepSingleDO();
				}
				fillCommon(gstep,stitle,maybeParent);
				((StepSingleDO)gstep).setDeadline((int) mDeadline.getValue());
				((StepSingleDO)gstep).setHourCost((int) mHourcostSingle.getValue());
				gstep.create();
			}
			else {	//parent
				gstep = new StepParentDO();
				fillCommon(gstep,stitle,maybeParent);
				gstep.create();
			}
			
			Singleton.db_w.setTransactionSuccessful();
			
		} catch ( RGException|PkNotFoundException|SQLiteConstraintException e) {
			failFlag = true;
			String msg = e.getMessage();
			SimpleAlertDF df = SimpleAlertDF.newInstance(msg + " (" + e.getClass().getSimpleName() + ")");
			df.show(getSupportFragmentManager(), "error");
		} finally {
			Singleton.db_w.endTransaction();
			if (failFlag) return;
		}

		Intent resultIntent = new Intent();
		resultIntent.putExtra(STEP_EXTRA, gstep.getId());	//TODO warning: gstep may be null 
		setResult(RESULT_OK, resultIntent);
		finish();
	}
	
}
