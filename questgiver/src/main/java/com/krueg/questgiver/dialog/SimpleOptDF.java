package com.krueg.questgiver.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.krueg.questgiver.R;

public class SimpleOptDF extends DialogFragment {

    public interface SimpleOptIF {
        public void onItemClick(SimpleOptDF dialog,int which);
    }
    
    private SimpleOptIF mListener;
    public int[] tags;
    public Integer singleTag;
    public String operation;
    public Integer checked;
    
    public static SimpleOptDF newInstance(String title, String[] options, SimpleOptIF listener) {
        Bundle args = new Bundle();
        args.putStringArray("list", options);
        args.putString("title", title);
        SimpleOptDF r = new SimpleOptDF();
        r.mListener = listener;
        r.setArguments(args);
        return r;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle args) {
        checked = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        String[] options = getArguments().getStringArray("list");
        String title = getArguments().getString("title");
        
        builder.setTitle(title)
            .setItems(options, new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    checked = arg1;
                    mListener.onItemClick(SimpleOptDF.this, arg1);
                }
            })
            .setNegativeButton(R.string.cancel, null);
        
        return builder.create();
    }
    
}
