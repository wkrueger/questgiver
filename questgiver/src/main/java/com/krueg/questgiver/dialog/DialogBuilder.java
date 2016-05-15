package com.krueg.questgiver.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.krueg.questgiver.R;
import com.krueg.questgiver.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * AlertDialog.Builder "wrapper" <br/>
 * ListItem > A clickable Item on the dialog (set by "setItems"). 
 * ListItem includes the action to be taken after clicking it (ItemAction).<br/>
 * ViewItem > A view to be appended to the dialog. On clicking the dialog positive
 * button, all of the "ViewActions" will be run in sequence. <br/>
 */
public class DialogBuilder {

    Context context;
    String mTitle;
    
    public interface DialogBuilderCallback {
        public void onSelectItem(ListItem item);
    }
    
    public final ListItem ACTION_DONE = new ListItem();
    
    DialogBuilderCallback listener;
    
    public interface ItemAction {
        void act();
    }
    
    public interface ViewAction {
        void onConfirm();
    }
    
    public class ListItem {
        public Integer id;
        public String title;
        public ItemAction act;
    }
    
    public class ViewItem {
        public View view;
        public ViewAction onConfirm;
    }
    
    public DialogBuilder(String title, Context cont,
            DialogBuilderCallback listn) {
        mTitle = title;
        context = cont;
        listener = listn;
    }
    
    public DialogBuilder(String title, Context cont) {
        mTitle = title;
        context = cont;
        listener = new DialogBuilderCallback() {
            public void onSelectItem(ListItem item) {}
        };
    }
    
    public final List<ListItem> listItems = new ArrayList<ListItem>();
    public final List<ViewItem> viewItems = new ArrayList<ViewItem>();
    
    public AlertDialog buildDialog() {
        String[] titles = new String[listItems.size()];
        for ( int it = 0 ; it < listItems.size(); it++ )
            titles[it] = listItems.get(it).title;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        builder.setTitle(mTitle)
            .setItems(titles, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ListItem sact = listItems.get(which); 
                    sact.act.act();
                    listener.onSelectItem(sact);
                }
            })
            .setNegativeButton(R.string.cancel, null);
        
        if ( viewItems.size() > 0 ) {
            
            LayoutParams divp = new LayoutParams(LayoutParams.MATCH_PARENT, Singleton.dpToPx(1));
            
            LayoutParams commonp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            LinearLayout grp = new LinearLayout(context);
            grp.setLayoutParams(commonp);
            grp.setOrientation(LinearLayout.VERTICAL);
            
            for ( ViewItem act : viewItems ) {
                View divider = new View(context);
                divider.setLayoutParams(divp);
                divider.setBackgroundColor(context.getResources().getColor(R.color.lightGray));
                grp.addView(divider);
                grp.addView(act.view);
            }
            
            builder.setView(grp).setPositiveButton(R.string.done, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                for ( ViewItem act : viewItems ) act.onConfirm.onConfirm();
                listener.onSelectItem(ACTION_DONE);
            }});
        }
        
        return builder.create();
    }
    
}
