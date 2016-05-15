package com.krueg.questgiver.dataobject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.widget.Toast;

import com.krueg.questgiver.Exceptions.RGException;
import com.krueg.questgiver.Exceptions.RGRuntimeException;
import com.krueg.questgiver.gamby.HourCostIF;
import com.krueg.questgiver.gamby.RGDate;
import com.krueg.questgiver.gamby.RGMutableDateTime;

import org.joda.time.MutableDateTime;

import java.util.Date;

public class StepWeekDO extends StepDO implements HourCostIF {
	int totalRepeats;
	Date lastRepeat;
	int repeats;
	
	protected int hourCost;
	
	
	public StepWeekDO() {
		super();
		repeats = 0;
		lastRepeat = new Date(0);
		totalRepeats = 0;
		hourCost = 1;
	}
	
	public StepWeekDO(Cursor c) throws RGException {
		super(c);
	}
	
	public StepWeekDO(int pk) throws RGException {
		super(pk);
	}
	
	@Override
	float getCompletionScore() {
		int availableDays = 8 - caltoday.getDayOfWeek();
		float perc = (totalRepeats-repeats)/(float)availableDays;
		perc = (perc > 1) ? 1 : perc;
		return (float) Math.pow(perc, 1.5);
	}
	
	/**
	 * Removes ignored or steps complete at the day
	 */
	@Override
	public boolean displayFilter() {
		boolean r = super.displayFilter();
		if ( isIgnored() || isDoneToday() ) return false;
		return r;
	}
	
	public int getTotalRepeats() {
		return totalRepeats;
	}
	
	public int getRepeats() {
		return repeats;
	}
	
	public int incrementRepeats(Context context) throws RGException {
		MutableDateTime calrepeat = new RGMutableDateTime();
		calrepeat.setTime(lastRepeat.getTime());
		calrepeat.setDate(lastRepeat.getTime());
		
		//only increment if we are in a new (next) day and if we are not capping repeat count
		if ( !isDoneToday() && repeats < totalRepeats ) {
			repeats++;
			setWeekMask(getWeekMask() | (1 << (caltoday.getDayOfWeek()-1)));
			lastRepeat = new RGDate();
			if ( repeats == totalRepeats ) completion = true;
			update();
		}
		else throw new RGException("No more repeats today or this week!");
		
		return repeats;
	}
	
	public boolean isDoneToday() {
		if ( repeats == 0 ) return false;
		MutableDateTime calrepeat = new RGMutableDateTime();
		calrepeat.setTime(lastRepeat.getTime());
		calrepeat.setDate(lastRepeat.getTime());
		return !(lastRepeat.before(new RGDate()) &&
				( calrepeat.getDayOfYear() != caltoday.getDayOfYear()));
	}

	
	@Override
	void initialize(Cursor c) {
		super.initialize(c);
		repeats = c.getInt(c.getColumnIndex("repeats"));
		totalRepeats = c.getInt(c.getColumnIndex("totalrepeats"));
		lastRepeat = new Date(c.getLong(c.getColumnIndex("lastrepeat")));
		hourCost = c.getInt(c.getColumnIndex("deadline"));
		
		//weekly reset and notification cleanup
		MutableDateTime calrepeat = new RGMutableDateTime();
		calrepeat.setTime(lastRepeat.getTime());
		calrepeat.setDate(lastRepeat.getTime());
		if ( caltoday.getWeekOfWeekyear() > calrepeat.getWeekOfWeekyear() ) {
			sendEvents(this, EVENT_WEEKLY_RESET);
			repeats = 0;
			completion = false;
			setWeekMask(0);
			lastRepeat = new RGDate();
			update();
		}
	}
	
	@Override
	ContentValues preSaveHook() {
		ContentValues values = super.preSaveHook();
		values.put("repeats", repeats);
		values.put("totalrepeats", totalRepeats);
		values.put("lastrepeat", lastRepeat.getTime());
		values.put("deadline", hourCost);
		return values;
	}
	
	@Override
	public void setComplete(Context context) {
		try {
			sendEvents(this, EVENT_COMPLETE);
			incrementRepeats(context);
		} catch (RGException e) {
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
			return;
		}
	}
	
	public int setTotalRepeats(int inp) throws RGException  {
		if ( inp > 0 && inp <= 5) totalRepeats = inp;
		else throw new RGException("For daily type steps, total repeats must range from 1 to 5");
		return totalRepeats;
	}
	
	@Override
	public String toString() {
		String r = super.toString();
		r += " " + repeats + "/" + totalRepeats;
		if ( isDoneToday() ) r+= " (done today)";
		return r;
	}

	@Override
	protected int getTypeId() {
		return 1;
	}
	
	public int getHourCost() {
		return hourCost;
	}
	
	public void setHourCost(int cost) {
		if ( cost < 0 ) throw new RGRuntimeException("Invalid hour cost.");
		hourCost = cost;
		sendEvents(this, EVENT_ALTER_HOURCOST);
		update();
	}
}
