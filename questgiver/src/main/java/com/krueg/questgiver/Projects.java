package com.krueg.questgiver;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.krueg.questgiver.Exceptions.RGCheckedNull;
import com.krueg.questgiver.StepSelectDF.StepSelectIF;
import com.krueg.questgiver.dataobject.StepDO;
import com.krueg.questgiver.dataobject.StepParentDO;
import com.krueg.questgiver.dialog.DialogBuilder.ListItem;
import com.krueg.questgiver.views.StepView;
import com.krueg.questgiver.views.TreeView;
import com.krueg.questgiver.views.TreeView_;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EFragment
public class Projects extends Fragment {

    private ViewGroup mRootView;
    private TreeView mTree;
    private TreeView.ActionListeners mDefaultListeners;
    
    boolean mAnimateNew = false;
    Integer mAddedStep = null;
    List<StepDO> mExpandStack = new ArrayList<StepDO>();
    StepDO stepToAnimate;
    
    public void animateOnNextRefresh(StepDO step) {
        mAnimateNew = true;
        stepToAnimate = step;
    }

    public static Projects newInstance() {
        Projects fragment = new Projects();
        return fragment;
    }
    
    public Projects() {
        // Required empty public constructor
    }
    
    class SwipeGesture extends GestureDetector.SimpleOnGestureListener {
        public final static int X_THRESHOLD = 100;
        public final static int Y_THRESHOLD = 50;
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            if ( Math.abs(e1.getX()-e2.getX()) > X_THRESHOLD && 
                Math.abs(e1.getY()-e2.getY()) < Y_THRESHOLD ) return true;
            return false;
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        StepDO.setOnEventListener("projectRefresher", new StepDO.OnEventListener() {
            public boolean onEvent(final StepDO step, final int what) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        switch ( what ) {
                        //events that trigger a remove
                        case StepDO.EVENT_REMOVE:
                            mTree.removeItem(step,true);
                            break;
                            
                        //events that trigger a reorder
                        case StepDO.EVENT_COMPLETE:
                        case StepDO.EVENT_ALTER_RELEVANCE:
                        case StepDO.EVENT_ALTER_DEADLINE:
                        case StepDO.EVENT_WEEKLY_RESET:
                        case StepDO.EVENT_CHANGE_PARENT:
                        case StepDO.EVENT_ACTIVATE:
                            step.setLocalOnEventListener( "projectRefresher" , new StepDO.OnEventListener() {
                                @Override
                                public boolean onEvent(StepDO step, int what) {
                                    if ( what != StepDO.EVENT_AFTER_UPDATE) return false;
                                    mTree.updateItem(step);
                                    return true;
                                }
                            });
                            break;
                            
                        //events that add a view
                        case StepDO.EVENT_NEW:
                            mTree.insertItem(step,true);
                            break;
                            
                        //events that trigger nothing
                        case StepDO.EVENT_ALTER_HOURCOST:
                        case StepDO.EVENT_POSTPONE:
                            break;
                        }
                    }
                });
                return false;
            }
        });
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        TreeView.TreeViewIF treeAdapter = new Singleton.CommonTreeIF();
        mTree =
                new TreeView_(getActivity(),treeAdapter);
        mDefaultListeners = new Singleton.CommonAction(mTree,this,stepIF);
        mTree.setActionListeners(mDefaultListeners);
        
        mRootView = (ViewGroup) getActivity().getLayoutInflater()
                .inflate(R.layout.fragment_frprofile_perk, null);
        LinearLayout linearRoot = (LinearLayout) mRootView.findViewById(R.id.rootLinear);
        pop();
        linearRoot.addView(mTree, 0);

        TextView addNew = (TextView) linearRoot.findViewById(R.id.buttonAddNew);
        addNew.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Landing)getActivity()).newPerk();
            }
        });

        return mRootView;
    }
    
    @Background
    public void pop() {
        mTree.removeAllViews();

        StepDO dummy = new StepDO();
        List<StepDO> pl = dummy.listMe(dummy.queryRoot());
        
        for ( StepDO step : pl ) {
            //if ( step instanceof  StepParentDO ) Log.d("first", step.title);
            mTree.pushItem(step);
        }

    }

    public void popDisabled() {
        Map<View,LinearLayout> expanded = mTree.getExpandedViews();
        Set<View> keyset = expanded.keySet();

        for ( View v : keyset ) {
            if (!(v instanceof  StepView)) continue;
            StepDO s = ((StepView) v).getStep();
            if (!(s instanceof StepParentDO)) continue;
            List<StepDO> children = s.listMe(((StepParentDO) s).queryChildren());
            for ( StepDO child : children ) {
                if ( child.isComplete() ) mTree.insertItem(child);
            }
        }
    }
    
    public void hideComplete() {
        mTree.removeViews(new TreeView.ViewQuery() {
            
            @Override
            public boolean isSelected(Object o) {
                StepDO s = (StepDO) o;
                if ( s.isComplete() ) return true;
                return false;
            }
            
        });
    }
    
    
    StepSelectIF stepIF = new StepSelectIF() { public void stepAfterSelect(ListItem choice,StepDO inStep) {
        //start move mode
        if ( choice.id != null && choice.id == StepSelectDF.ACT_MOVE ) { 
            ((Landing)getActivity()).moveAct = getActivity().startActionMode(moveStepCb);
            ((Landing)getActivity()).moveAct.setTitle(R.string.step_move_to_arg);
            try {
                StepView selected = (StepView) mTree.findView(inStep);
                selected.performClick();
            } catch ( RGCheckedNull e ) {}
        }
    }};
    
    class MoveModeAction extends TreeView.ActionListeners {
        Set<StepView> mToMove;
        StepView[] mTarget;
        
        MoveModeAction(TreeView t,Set<StepView> toMove,StepView[] target) {
            t.super();
            mToMove = toMove;
            mTarget = target;
        }
        
        @Override
        public void click(View v) {
            if ( !(v instanceof StepView) || v.isSelected() ) return;
            if ( v.isActivated() ) {
                v.setActivated(false);
                mToMove.remove(v);
            } else {
                v.setActivated(true);
                mToMove.add((StepView) v);
            }
        }
        
        @Override
        public boolean longClick(View v) {
            if ( !(v instanceof StepView) || v.isActivated() ) return false;
            StepView sv = (StepView) v;
            StepDO step = sv.getStep();
            if ( !(step instanceof StepParentDO) ) return false;
            
            if ( mTarget[0] != null ) mTarget[0].setSelected(false);
            mTarget[0] = sv;
            mTarget[0].setSelected(true);
            
            return true;
        }
    }
    
    //move mode stuff
    ActionMode.Callback moveStepCb = new ActionMode.Callback() {
        
        boolean hasPerformed;
        
        Set<StepView> toMove = new HashSet<StepView>();
        StepView[] target = new StepView[1];

        
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
        
        public void onDestroyActionMode(ActionMode mode) {  
            if ( hasPerformed == false ) perform();
            mTree.setActionListeners(mDefaultListeners);
            for ( StepView s : toMove ) s.setActivated(false);
            if ( target[0] != null ) target[0].setSelected(false);
            toMove.clear();
        }
        
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            
            hasPerformed = false;
            Toast.makeText(getActivity(), getString(R.string.hold_to_select),
                Toast.LENGTH_LONG).show();
            
            TreeView.ActionListeners moveActions =
                    new MoveModeAction(mTree,toMove,target);
            mTree.setActionListeners(moveActions);
            
            mode.getMenuInflater().inflate(R.menu.act_move, menu);
            return true;
            
        }
        
        public void perform() {
            hasPerformed = true;
            if ( target[0] == null ) return;
            StepParentDO parent = (StepParentDO) target[0].getStep();
            
            for ( StepView moving : toMove ) {
                StepDO step = moving.getStep();
                parent.addChild(step);
            }
        }
        
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            //default context action bar creates a "done" button
            switch ( item.getItemId() ) {
            case R.id.item_done:
                perform();
                break;
            case R.id.item_cancel:
                break;
            }
            hasPerformed = true;
            mode.finish();
            return true;
        }
    };
    
    @Override
    public void onPause() {
        super.onPause();
        if (((Landing)getActivity()).moveAct != null)
            ((Landing)getActivity()).moveAct.finish();
    }
    
    
}
