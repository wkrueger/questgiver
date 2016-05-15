package com.krueg.questgiver.dataobject;

import android.database.Cursor;

import com.krueg.questgiver.Exceptions.RGException;
import com.krueg.questgiver.gamby.RGDate;

import java.util.Date;

/**
 * A step that only occurs a single time but also has an activation state.
 * <b>STATES</b>
 * - complete/incomplete (inherited)
 * - active/inactive
 */
public class StepSubsequentDO extends StepSingleDO {
    
    public StepSubsequentDO(){
        super();
    }
    
    public StepSubsequentDO(Cursor c) throws RGException {
        super(c);
    }
    
    public StepSubsequentDO(int pk) throws RGException {
        super(pk);
    }
    
    public boolean isActive() {
        if ( isComplete() ) return false;
        return getWeekMask() != 0;
    }
    
    /**
     * @throws RGException When step either complete or active
     */
    public void activate() throws RGException {
        if ( isComplete() )
            throw new RGException("activateSubsequent called for a complete step");
        else if ( isActive() )
            throw new RGException("activateSubsequent called for an active step");
        sendEvents(this, EVENT_ACTIVATE);
        setWeekMask(new RGDate().getTime());
        update();
    }
    
    @Override
    float getCompletionScore() {
        if ( !isActive() ) return 0;
        return super.getCompletionScore();
    }

    public Date getActivationDate() throws RGException {
        if ( !isActive() )
            throw new RGException("getActivationDate called for inactive step. Inactive check necessary");
        return new RGDate(getWeekMask());
    }
    
    /**
     * @throws RGException On subsequent steps, the step needs to be checked against
     * non active first
     */
    @Override
    public Date getDeadline() throws RGException {
        long r = getActivationDate().getTime() + 1000 * 60 * 60 * deadline;
        return new Date(r);
    }
    
    @Override
    public boolean displayFilter() {
        boolean rev = super.displayFilter();
        if ( !isActive() ) return false;
        return rev;
    }
    
    @Override
    void onSuccessfulUpdate() {
        super.onSuccessfulUpdate();
    }
    
    @Override
    void onSuccessfulInsert() {
        super.onSuccessfulInsert();
    }
    
    @Override
    protected int getTypeId() {
        return 2;
    }
}
