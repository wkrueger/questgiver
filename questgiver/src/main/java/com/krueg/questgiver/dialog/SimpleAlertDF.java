package com.krueg.questgiver.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * A simple alert with a piece of text.
 * Use "newInstance" to construct me.
 */
public class SimpleAlertDF extends DialogFragment {
    
    String text;
    
    public static SimpleAlertDF newInstance (String stringResource) {
        SimpleAlertDF df = new SimpleAlertDF();
        df.text = stringResource;
        
        return df;
    }
    
    AlertDialog.Builder getBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        builder.setMessage(text);
        return builder;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle b){
        return getBuilder().create();
    }
    
}
