package com.krueg.questgiver.views;

import android.content.Context;
import android.util.AttributeSet;

public class StepGroupPicker extends ListPicker {

	public StepGroupPicker(Context context) {
		super(context);
	}
	
	public StepGroupPicker(Context context,AttributeSet attrs) {
		super(context,attrs);
	}
	
	@Override
	protected float[] getList() {
		return new float[]{0,0.2F,0.4F,0.6F,0.8F,1.0F,1.2F,1.4F,1.6F,1.8F,2.0F};
	}

	@Override
	public String getFormat() {
		return "#.#";
	}
	
}