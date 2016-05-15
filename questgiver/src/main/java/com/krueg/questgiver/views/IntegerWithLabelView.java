package com.krueg.questgiver.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.krueg.questgiver.R;
import com.krueg.questgiver.Singleton;

import antistatic.spinnerwheel.WheelVerticalView;
import antistatic.spinnerwheel.adapters.AbstractWheelAdapter;
import antistatic.spinnerwheel.adapters.NumericWheelAdapter;

public class IntegerWithLabelView extends LinearLayout {

	TextView mLabel;
	WheelVerticalView mEdit;
	int mLowerBound = 0;
	
	private void init(Context context) {
        setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
        setLayoutParams(params);
        setPadding((int)getResources().getDimension(R.dimen.defaultMargin),
            Singleton.dpToPx(3),
            (int)getResources().getDimension(R.dimen.defaultMargin),
            Singleton.dpToPx(3));
		
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_new_skill_reward,this,true);
        mLabel = (TextView) findViewById(R.id.label);
        mEdit = (WheelVerticalView) findViewById(R.id.wheel);
        AbstractWheelAdapter adapter = getAdapter(context);
        mEdit.setViewAdapter(adapter);
	}
	
	//gamby - should create parent abstract class first
	@SuppressWarnings("static-method")
	protected AbstractWheelAdapter getAdapter(Context context) {
		NumericWheelAdapter out = new NumericWheelAdapter(context, 0, 10, "%02d");
        out.setItemResource(R.layout.sub_wheel_number);
        out.setItemTextResource(R.id.text);
		return out;
	}
	
	public IntegerWithLabelView(Context context,AttributeSet attrs) {
		super(context,attrs);
		init(context);
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs, R.styleable.IntegerWithLabelView,
				0, 0);
		
		try {
			String text = a.getString(R.styleable.IntegerWithLabelView_label);
			mLabel.setText(text);
			Integer val = a.getInteger(R.styleable.IntegerWithLabelView_value, 1);
			setValue(val);
		} finally {
			a.recycle();
		}
	}
	
	public IntegerWithLabelView(Context context) {
		super(context);
		init(context);
	}

	public void setText(String text) {
		mLabel.setText(text);
	}
	
	public void setValue(float value) {
		if ( value < 0 ) throw new IllegalArgumentException();
		mEdit.setCurrentItem(Math.round(value) - mLowerBound);
	}

	public void setBounds(int min,int max) {
		NumericWheelAdapter num = (NumericWheelAdapter) mEdit.getViewAdapter();
		mLowerBound = min;
		num.setMinValue(min);
		num.setMaxValue(max);
	}
	
	public float getValue() {
		NumericWheelAdapter num = (NumericWheelAdapter) mEdit.getViewAdapter();
		return Singleton.toInt(num.getItemText(mEdit.getCurrentItem()).toString());
	}
}
