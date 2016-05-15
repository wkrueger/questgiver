package com.krueg.questgiver.dialog;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import com.krueg.questgiver.R;
import com.krueg.questgiver.Singleton;

public class SimpleIntegerDF extends SimpleAlertDF {

    public interface SimpleIntegerIF {
        public void onPositive(SimpleIntegerDF dialog,int value);
    }
    
    private SimpleIntegerIF mListener;
    
    public static SimpleIntegerDF newInstance (int textResource,SimpleIntegerIF listener) {
        SimpleIntegerDF df = new SimpleIntegerDF();
        df.mListener = listener;
        Bundle args =  new Bundle();
        args.putInt("textResource", textResource);
        df.setArguments(args);
        
        return df;
    }
    
    public static SimpleIntegerDF newInstance (String stringResource,SimpleIntegerIF listener) {
        SimpleIntegerDF df = new SimpleIntegerDF();
        df.mListener = listener;
        
        Bundle args =  new Bundle();
        args.putString("stringResource", stringResource);
        df.setArguments(args);
        
        return df;
    }
    
    @Override
    public AlertDialog.Builder getBuilder() {
        AlertDialog.Builder builder = super.getBuilder();
        final EditText edit = new EditText(getActivity());
        edit.setHint("0");
        edit.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(edit)
            .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mListener.onPositive(SimpleIntegerDF.this,Singleton.toInt(edit.getText().toString()));
                }
            })
            .setNegativeButton(R.string.cancel, null);
        return builder;
    }
    
}
