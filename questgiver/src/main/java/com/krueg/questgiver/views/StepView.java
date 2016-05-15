package com.krueg.questgiver.views;


import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.krueg.questgiver.Exceptions.PkNotFoundException;
import com.krueg.questgiver.Exceptions.RGException;
import com.krueg.questgiver.Exceptions.RGTopLevelException;
import com.krueg.questgiver.R;
import com.krueg.questgiver.Singleton;
import com.krueg.questgiver.dataobject.StepDO;
import com.krueg.questgiver.dataobject.StepParentDO;
import com.krueg.questgiver.dataobject.StepSingleDO;
import com.krueg.questgiver.dataobject.StepWeekDO;
import com.krueg.questgiver.gamby.RGMutableDateTime;

import org.joda.time.Duration;


//My main intent on writing this view was to learn a bit on the lower-end
//side of views. Later I found out this definitely was not the best way to
//achieve the "step view" goal, nevertheless it served a bit on the learning purpose
/**
 * Currently one must run "setStep" before attempting to show or change this view.<br/>
 */
public class StepView extends RelativeLayout {

    private TextView mStepTitle;
    private TextView mParentTitle;
    private ImageView mPriorityStrip;
    private ImageView mIcon;
    private StepDO mStep;

    private FrameLayout mIndicatorContainer;

    private boolean mShowProject = false;

    private void init(Context context) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.stepview,this,true);

        mStepTitle = (TextView)findViewById(R.id.stepDesc);
        mParentTitle = (TextView)findViewById(R.id.parentDesc);
        mIcon = (ImageView)findViewById(R.id.stepTypeIcon);
        mPriorityStrip = (ImageView)findViewById(R.id.priorityStrip);
        mIndicatorContainer = (FrameLayout) findViewById(R.id.indicator);

        setBackground(getResources().getDrawable(R.drawable.state_list));
        setMinimumHeight(Singleton.dpToPx(50));
    }

    public StepView(Context context,AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StepView(Context context) {
        super(context);
        init(context);
    }

    @Override
    public void invalidate() {
        try {
            setStep(StepDO.newInstance(mStep.getId()));
        } catch (PkNotFoundException e){ return; }
        super.invalidate();
    }

    /*
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        int hmode = MeasureSpec.getMode(heightMeasureSpec);
        int hsize = MeasureSpec.getSize(heightMeasureSpec);
        int wsize = MeasureSpec.getSize(widthMeasureSpec);

        int height = hsize;
        int width = wsize;

        if ( hmode == MeasureSpec.UNSPECIFIED ) height = Math.max(hsize,MINHEIGHT);

        setMeasuredDimension(width,height);
    }
    */

    public void setShowProject(boolean s) {
        mShowProject = s;
        //set the text
        String parentTitle;
        RelativeLayout.LayoutParams params = (LayoutParams) mStepTitle.getLayoutParams();
        if ( mShowProject ) {
            try { parentTitle = mStep.getParent().title; }
            catch ( RGTopLevelException e ) { parentTitle = ""; }
            mParentTitle.setVisibility(VISIBLE);
            params.removeRule(RelativeLayout.CENTER_VERTICAL);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        } else {
            parentTitle = "";
            mParentTitle.setVisibility(GONE);
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
        }
        mParentTitle.setText(parentTitle);
    }

    public void setStep(final StepDO step) {
        mStep = step;

        //choose icon
        int rid;
        if ( mStep instanceof StepWeekDO ) rid = R.drawable.ic_recurrent_step;
        else if ( mStep.isComplete() && mStep instanceof StepParentDO) rid = R.drawable.ic_checked_parent_step;
        else if ( mStep.isComplete() ) rid = R.drawable.ic_complete_step;
        else if ( mStep instanceof StepParentDO ) rid = R.drawable.ic_parent_step;
        else rid = R.drawable.ic_empty_step;

        mIcon.setImageResource(rid);

        //set the text
        setShowProject(mShowProject);
        mStepTitle.setText(mStep.title);

        //set the indicator
        mIndicatorContainer.removeAllViews();
        if ( mStep instanceof StepWeekDO ) {
            WeekIndicator ind = new WeekIndicator(getContext());
            ind.setState((int) mStep.getWeekMask(),
                    ((StepWeekDO) mStep).getTotalRepeats()-((StepWeekDO) mStep).getRepeats());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            ind.setLayoutParams(params);
            mIndicatorContainer.addView(ind);
        } else if ( mStep instanceof StepSingleDO || mStep instanceof StepParentDO ) {
            TextView t = new AutoResizeTextView(getContext());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            t.setLayoutParams(params);
            t.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            if ( mStep instanceof StepSingleDO && ((StepSingleDO) mStep).isActive() ) {
                long then = 0;
                try {
                    then = ((StepSingleDO) mStep).getDeadline().getTime();
                } catch (RGException e) {}  //ok, checked against inactive
                long now = new RGMutableDateTime().getMillis();
                Duration dur = new Duration(now,then);
                t.setText(dur.getStandardHours() + "h");
                mIndicatorContainer.addView(t);
            }
            else if ( mStep instanceof StepParentDO ) {
                Integer[] hc = ((StepParentDO) mStep).getHourcostSum();
                t.setText(hc[1] + "/" + hc[0]);
                mIndicatorContainer.addView(t);
            }
        }
    }

    public StepDO getStep() {
        return mStep;
    }
    
    @Override
    protected void onAnimationEnd() {
        super.onAnimationEnd();
        setBackground(getResources().getDrawable(R.drawable.state_list));
    }
    
    @Override
    public boolean equals(Object o) {
        return ( o instanceof StepView && ((StepView) o).getStep().equals(getStep()) );
    }
    
    @Override
    public int hashCode() {
        int init = 173;
        return init + getStep().getId() * 3;
    }
    
    @Override
    public String toString() {
        return "StepView:"+mStep.toString();
    }
}