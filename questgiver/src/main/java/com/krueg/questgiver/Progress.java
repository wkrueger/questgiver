package com.krueg.questgiver;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.krueg.questgiver.Exceptions.RGException;
import com.krueg.questgiver.dataobject.ActivityDO;
import com.krueg.questgiver.dataobject.StepDO;
import com.krueg.questgiver.dataobject.StepSingleDO;
import com.krueg.questgiver.dataobject.StepWeekDO;
import com.krueg.questgiver.gamby.BoundedValue;
import com.krueg.questgiver.gamby.HourCostIF;
import com.krueg.questgiver.gamby.RGDate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//THE GAME
public class Progress {

    public static final int EVENT_ON_DEDUCT = 1;
    public static final int EVENT_ON_INCREASE = 2;
    public static final int EVENT_ON_DECAY = 3;

    public static interface OnEventListener {
        public void onEvent(int event,int points);
    }

    private static Map<String,OnEventListener> mListeners = new HashMap<>();

    public static void addEventListener(String label,OnEventListener listener) {
        mListeners.put(label,listener);
    }

    private static void callListeners(int type,int points) {
        for (Map.Entry<String,OnEventListener> e : mListeners.entrySet()) {
            e.getValue().onEvent(type,points);
        }
    }


    static {
        //triggers for performance meter
        StepDO.setOnEventListener("progress",new StepDO.OnEventListener() {
            public boolean onEvent(StepDO step, int what) {
                
                //newly created steps are ignored
                long now = new RGDate().getTime();
                if ( now - step.getCreated().getTime() < 1000 * 15 ) return false;
                if ( step.getId() == null ) return false;
                
                try {
                    switch(what) {
                    
                    case StepDO.EVENT_COMPLETE:
                        Progress.increase(((HourCostIF)step).getHourCost(), step.getCompositeRelevance());
                        try {
                            //are we late?
                            long checktime = Math.max(((StepSingleDO) step).getDeadline().getTime(),
                                    Progress.getLastDeduction());
                            long latecheck = now - checktime;
                            int hourc = ((HourCostIF) step).getHourCost();
                            hourc = (hourc == 0) ? 1 : hourc;
                            if ( step instanceof StepSingleDO && latecheck > 0 )
                                Progress.deduct(latecheck/1000F/60F/60F/hourc, step.getCompositeRelevance());
                        } catch (RGException e) {}  //the step is not activated. No, we're not late
                        break;
                        
                    //altering a property inflicts a small penalty
                    case StepDO.EVENT_ALTER_DEADLINE:
                    case StepDO.EVENT_ALTER_HOURCOST:
                    case StepDO.EVENT_ALTER_RELEVANCE:
                        if (step.isComplete()) return false;
                        Progress.deduct(((HourCostIF)step).getHourCost()/5F, step.getCompositeRelevance());
                        break;
                        
                    case StepDO.EVENT_REMOVE:
                        if (step.isComplete()) return false;
                        Progress.deduct(((HourCostIF)step).getHourCost()/2F, step.getCompositeRelevance());
                        break;
                        
                    //this one is sent when a weekly DO is init and we have
                    //"turned" the week
                    case StepDO.EVENT_WEEKLY_RESET:
                        if (step.isComplete()) return false;;
                        StepWeekDO s = (StepWeekDO)step;
                        Progress.deduct(
                                s.getHourCost() *
                                ( s.getTotalRepeats()- s.getRepeats() ),
                                step.getCompositeRelevance());
                        break;
                    }
                } catch ( ClassCastException e ) {
                    //e.printStackTrace();
                    //swallow. i.e. if not intended, it is not meant to be
                }
                return false;
            }
        });
    }



    private static BoundedValue<Float> currentValue = new BoundedValue<Float>(0F,100F,0F);
    private static long lastDeduction = 0;
    
    static public float maxrel = 1;
    
    static final float DAILY_HOUR_GOAL = 4;
    static final float DAILY_DECAY = 20;
    
    public static void init(float progress, long lastDeduction) {
        Progress.lastDeduction = lastDeduction;
        currentValue.set(progress);
    }
    
    public static float getCurrentValue() {
        return currentValue.get();
    }
    
    public static long getLastDeduction() {
        return lastDeduction;
    }
    
    
    public static void debugSetCurrentValue(float v) {
        currentValue.set(v);
        if ( listener != null ) listener.onUpdate(v);
    }
    
    public interface ProgressListener {
        public void onUpdate(float currentValue);
    }
    
    static ProgressListener listener;
    
    public static void setListener(ProgressListener listener) {
        Progress.listener = listener;
    }
    
    
    //applies the daily decay and the penalty for overdue SINGLE steps
    public static void doDeduction() {
        StepDO dummy = new StepDO();
        List<StepDO> list = dummy.listMe();
        
        long now = new RGDate().getTime();
        //current - days-passed * daily-decay
        float decay = (now - lastDeduction) / (1000 * 60 * 60 * 24F) * DAILY_DECAY;
        currentValue.set(currentValue.get() - decay);
        ActivityDO.logAct("Applied decay of " + decay + "." );
        
        ActivityDO.logAct("Started applying late step penalty...");
        for ( StepDO step : list ) {
            try {
                //single, late, activated
                if ( step instanceof StepSingleDO &&
                        !step.isComplete() &&
                        new RGDate().getTime() - ((StepSingleDO) step).getDeadline().getTime() > 0 &&
                        ((StepSingleDO) step).isActive() )
                {
                    long start = Math.max(lastDeduction, ((StepSingleDO) step).getDeadline().getTime());
                    //for each hour late, deduct 2/DAILY_HOUR_GOAL hours
                    float todeduct = (new RGDate().getTime() - start)/(1000F*60*60*DAILY_HOUR_GOAL)*2;
                    deduct(todeduct, step.getCompositeRelevance());
                }
            } catch ( RGException e ) {  }  //ok,checked against inactive
        }
        ActivityDO.logAct("Ended applying late step penalty.");
        
        lastDeduction = now;
        if ( listener != null ) listener.onUpdate(currentValue.get());
    }
    
    private static double multiplier() {
        return (Math.pow((150-currentValue.get()*1.2F)/100,2)+1.2)*0.5;
    }
    
    //if the current step is not the most relevant, reduce the points gained
    private static float relevanceFactor(float relevance) {
        float out = 1 - relevance/maxrel;
        out *= out*out;
        return 1 - out;
    }
    
    
    //some goals:
    //if you do the daily hour goal, you'd score the equivalent of a daily decay
    //this value is then affected by multiplier(), which in turn increases
    //the points gained if the current points are low, or makes it harder
    //to fill up the bar if the points are high. multiplier = 1 at ~50.
    
    //the daily decay deduction is not affected by the multiplier
    public static void increase(float hours,float relevance) {
        float oldval = currentValue.get();
        double multiplier = multiplier();
        currentValue.set( (float) (currentValue.get() + hours/DAILY_HOUR_GOAL*multiplier*relevanceFactor(relevance)*DAILY_DECAY) );
        if ( listener != null ) listener.onUpdate(currentValue.get());
        save();
        ActivityDO.logAct("Changed " + (currentValue.get() - oldval) +
                " points. Relevance factor: " + relevanceFactor(relevance) + 
                " Multiplier: " + multiplier);
    }
    
    public static void deduct(float hours, float relevance) {
        float oldval = currentValue.get();
        double multiplier = multiplier();
        currentValue.set( (float) (currentValue.get() - hours/DAILY_HOUR_GOAL*multiplier*relevanceFactor(relevance)*DAILY_DECAY));
        if ( listener != null ) listener.onUpdate(currentValue.get());
        save();
        ActivityDO.logAct("Changed " + (currentValue.get() - oldval) +
                " points. Relevance factor: " + relevanceFactor(relevance) + 
                " Multiplier: " + multiplier);
    }
    
    public static void save() {
        SharedPreferences prefs2 = PreferenceManager
                .getDefaultSharedPreferences(Singleton.context);
        Editor editor = prefs2.edit();
        editor.putFloat("progressStore", getCurrentValue());
        editor.putLong("lastDeductionStore", getLastDeduction());
        editor.commit();
    }
}
