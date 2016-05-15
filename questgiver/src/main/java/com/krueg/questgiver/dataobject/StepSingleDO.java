package com.krueg.questgiver.dataobject;

import android.content.ContentValues;
import android.database.Cursor;

import com.krueg.questgiver.Exceptions.RGException;
import com.krueg.questgiver.Exceptions.RGRuntimeException;
import com.krueg.questgiver.Singleton;
import com.krueg.questgiver.gamby.HourCostIF;
import com.krueg.questgiver.gamby.RGDate;

import java.util.Date;

/**
 * A step that only occurs a single time. 
 * <b>STATES</b>
 * - Complete/non-complete
 */
public class StepSingleDO extends StepDO implements HourCostIF {
	
	protected Integer deadline;
	protected int hourCost;
	
	public StepSingleDO() {
		super();
		deadline = 48;
		hourCost = 1;
	}
	
	public StepSingleDO(Cursor c) throws RGException {
		super(c);
	}
	
	public StepSingleDO(int pk) throws RGException {
		super(pk);
	}
	
	/**
	 * @return The date in which the step has been activated
	 * @throws RGException on inactive step. The step needs to be checked against
	 * inactive before this function is called.
	 */
	public Date getActivationDate() throws RGException {
		return created;
	}
	
	@SuppressWarnings("static-method")
	public boolean isActive() {
		return !isComplete();
	}
	
	float getCompletionScore() {
		if ( isComplete() ) return 1;
		long start;
		try {
			start = getActivationDate().getTime();
		} catch (RGException e) { return 0; }	//inactive step (subsequent)
		long end = start + deadline * 1000 * 60 * 60;
		float perc = (new RGDate().getTime() - start) / (float)(end-start);
		perc = (perc > 1) ? 1 : perc;
		return (float) Math.pow(perc, 1.5);
	}
	
	/**
	 * @throws RGException On subsequent steps, the step needs to be checked against
	 * non active first
	 */
	public Date getDeadline() throws RGException {
		long r = created.getTime() + 1000 * 60 * 60 * deadline;
		return new RGDate(r);
	}
	
	public int getDeadline1() {
		return deadline;
	}
	
	/**
	 * Query steps above 60% to end, for use in notifications
	 * @return Cursor with the query result
	 */
	public Cursor queryPct() {
		String clause = "(strftime('%s','now')*1000-created)*1.0/(deadline*60*60*1000*1.0) > 0.2 AND type = 0";
		Cursor c = Singleton.db_r.query(getTable(),null,clause,null,null,null,null);
		return c;
	}
	
	@Override
	ContentValues preSaveHook() {
		ContentValues values = super.preSaveHook();
		values.put("deadline", deadline);
		values.put("repeats", hourCost);
		return values;
	}
	
	@Override
	void initialize(Cursor c) {
		super.initialize(c);
		deadline = c.getInt(c.getColumnIndex("deadline"));
		hourCost = c.getInt(c.getColumnIndex("repeats"));
	}


	@Override
	protected int getTypeId() {
		return 0;
	}
	
	public int getHourCost() {
		return hourCost;
	}
	
	public void setDeadline(int deadline) {
		this.deadline = deadline;
		sendEvents(this, EVENT_ALTER_DEADLINE);
		update();
	}
	
	public void setHourCost(int cost) {
		if ( cost < 0 ) throw new RGRuntimeException("Invalid hour cost.");
		hourCost = cost;
		sendEvents(this,EVENT_ALTER_HOURCOST);
		update();
	}
}
