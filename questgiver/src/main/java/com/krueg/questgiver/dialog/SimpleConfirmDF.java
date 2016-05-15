package com.krueg.questgiver.dialog;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.krueg.questgiver.R;

public class SimpleConfirmDF extends SimpleAlertDF {
	public interface SimpleConfirmIF {
		public void onReturn(boolean answ);
	}
	private SimpleConfirmIF mListener;
	public int singleTag;
	public boolean answer;
	
	public static SimpleConfirmDF newInstance (String stringResource, SimpleConfirmIF listener) {
		SimpleConfirmDF df = new SimpleConfirmDF();
		df.mListener = listener;
		df.answer = false;
		df.text = stringResource;
		return df;
	}
	
	@Override
	public AlertDialog.Builder getBuilder() {
		AlertDialog.Builder builder = super.getBuilder();
		builder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				answer = true;
				mListener.onReturn(true);
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				answer = false;
				mListener.onReturn(false);
			}
		});
		return builder;
	}
}
