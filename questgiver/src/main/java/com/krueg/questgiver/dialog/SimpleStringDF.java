package com.krueg.questgiver.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.widget.EditText;

import com.krueg.questgiver.R;

public class SimpleStringDF extends DialogFragment {

    public interface SimpleStringIF {
        public void callback(String value);
    }
    
    private SimpleStringIF mListener;
    private String mTitle;
    
    public static SimpleStringDF newInstance (String title,SimpleStringIF listener) {
        SimpleStringDF df = new SimpleStringDF();
        df.mListener = listener;
        df.mTitle = title;
        
        return df;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        final EditText et = new EditText(getActivity());
        //tw.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT));
        et.setHint(getString(R.string.skill));
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        
        builder.setTitle(mTitle)
            .setView(et)
            .setPositiveButton(R.string.done, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mListener.callback(et.getText().toString());
                }
            });
        return builder.create();
    }
    
}