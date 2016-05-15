package com.krueg.questgiver.dataobject;

import android.content.Context;
import android.database.Cursor;

import com.krueg.questgiver.Exceptions.RGException;
import com.krueg.questgiver.Exceptions.RGRuntimeException;
import com.krueg.questgiver.R;
import com.krueg.questgiver.Singleton;
import com.krueg.questgiver.gamby.HourCostIF;

import java.util.ArrayList;
import java.util.List;

public class StepParentDO extends StepDO {

    public StepParentDO() {
        super();
    }

    public StepParentDO(Cursor c) throws RGException {
        super(c);
    }

    public StepParentDO(int pk) throws RGException {
        super(pk);
    }

    private float mLazyCompletionScore = -1;
    
    @Override
    public float getCompletionScore() {
        //TODO abomination query here
        if ( mLazyCompletionScore >= 0 ) return mLazyCompletionScore;
        if ( isComplete() ) return 1;
        float ret = 0;
        List<StepDO> list = new StepDO().listMe(queryChildren());
        List<StepDO> toRemove = new ArrayList<StepDO>();
        int ignoreCount = 0;
        for ( StepDO step : list ) if ( step instanceof StepWeekDO ) {
            toRemove.add(step);
            ignoreCount++;
        }
        int div = list.size()-ignoreCount;
        list.removeAll(toRemove);
        for ( StepDO step : list )  
            ret += step.getCompletionScore();
        if ( div == 0 ) return 0;   //avoid division by zero
        mLazyCompletionScore = ret/(float)div; 
        return mLazyCompletionScore;
    }

    public Cursor queryChildren() {
        Cursor c = Singleton.db_r.query(getTable(), null, "parent = ? AND _id <> ?",
                new String[]{ Integer.toString(id) , Integer.toString(id) }, null, null, null,
                null);
        return c;
    }
    
    @Override
    public boolean displayFilter() {
        return false;
    }
    
    @Override
    public void setComplete(Context context) {
        List<StepDO> list = new StepDO().listMe(queryChildren());
        for ( StepDO step : list ) {
            if ( !step.isComplete() ) 
                throw new RGRuntimeException(
                        context.getString(R.string.err_step_children_not_complete));
        }
        super.setComplete(context);
    }

    /**
     * A child can only have one parent, which is a property of the child<br/>
     * This method changes the child, not the parent.
     */
    public void addChild(StepDO child) {
        if ( id >= 0 ) { 
            sendEvents(child, StepDO.EVENT_CHANGE_PARENT);
            child.parent = id;
            child.update();
        }
        else throw new RGRuntimeException("Undefined parent trying to add child");
    }

    public void setAsRoot() { //this jailbreaks your phone!
        parent = PARENT_SET_ME_ROOT;
    }
    
    @Override
    protected int getTypeId() {
        return 3;
    }
    
    @Override
    public void delete() {
        List<StepDO> list = listMe(queryChildren());
        for ( StepDO child : list ) {
            child.delete();
        }
        super.delete();
    }

    @Override
    public void setIgnored() {
        super.setIgnored();
        List<StepDO> children = listMe(queryChildren());
        for ( StepDO child : children ) {
            child.setIgnored();
        }
    }

    public Integer[] getHourcostSum() {
        List<StepDO> li = listMe(queryChildren());
        int countAll = 0;
        int countOpen = 0;
        for ( StepDO inStep : li ) {
            if ( inStep instanceof HourCostIF) {
                int hc = ((HourCostIF) inStep).getHourCost();
                countAll += hc;
                if (inStep.isComplete()) countOpen += hc;
            } else if ( inStep instanceof StepParentDO ) {
                Integer[] hc = ((StepParentDO) inStep).getHourcostSum();
                countAll += hc[0];
                countOpen += hc[1];
            }
        }
        return new Integer[]{countAll,countOpen};
    }
}
