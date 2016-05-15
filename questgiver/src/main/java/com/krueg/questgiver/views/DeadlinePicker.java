package com.krueg.questgiver.views;

import android.content.Context;

public class DeadlinePicker extends ListPicker {

	public DeadlinePicker(Context context) {
		super(context);
	}
	
	@Override
	protected float[] getList() {
		return new float[] {1,2,3,6,12,24,48,72,96,120,144,168,216,264,312,360};
	}

	@Override
	public String getFormat() {
		return "#";
	}

}