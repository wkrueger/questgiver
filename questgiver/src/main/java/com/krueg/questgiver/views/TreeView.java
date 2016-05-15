package com.krueg.questgiver.views;

import android.animation.LayoutTransition;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.krueg.questgiver.Exceptions.RGCheckedNull;
import com.krueg.questgiver.Singleton;
import com.krueg.questgiver.Singleton.ViewTreeExecIF;
import com.krueg.questgiver.dataobject.StepDO;
import com.krueg.questgiver.gamby.ViewUpdater;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EViewGroup;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EViewGroup
public class TreeView extends LinearLayout {
	
	boolean mFlatInsert = false;
	TreeViewIF mTreeIF;
	private int mLeftMargin;
	Map<View,LinearLayout> mGroups = new ArrayMap<>();
	
	@Bean
	ViewUpdater updater;
	
	
	
	
	
	//PROPERTY SETTERS ETC
	
	/**
	 * <b>Flat insert:</b> Every new view is inserted at the root container.
	 * Affetcts inserts based on the data-object.
	 */
	public TreeView setFlatInsert(boolean opt) {
		mFlatInsert = opt;
		return this;
	}
	
	public TreeView setIdentMargin(int dp) {
		mLeftMargin = dp;
		return this;
	}
	
	public void setActionListeners(ActionListeners act) {
		if ( act == null ) mActions = new ActionListeners();
		else mActions = act;
	}
	
	
	
	
	
	//CONSTRUCT
	
	/**
	 *	Relationship between a view and a data object
	 */

    public static abstract class TreeViewIF {
        public abstract List<View> getChildren(View view);
        public abstract Object getObject(View v);
        public abstract void refreshView(View v);
        public abstract Object getObjectParent(Object o);
        public abstract int compareObjects(Object o1,Object o2);
        public abstract View createView(Object o);
    }
	
	public TreeView(Context context, TreeViewIF treeIF) {
		super(context);
		mTreeIF = treeIF;
		mLeftMargin = 15;
		setActionListeners(null);
        setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        setDividerDrawable(getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
        Handler mainThread = new Handler(Looper.getMainLooper());
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                propertyInit(TreeView.this);
            }
        });
	}
	
	//Common properties
	public void propertyInit(LinearLayout in) {

        ViewGroup parent = (ViewGroup) in.getParent();
        ViewGroup.MarginLayoutParams params = new MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        if ( parent instanceof LinearLayout || parent == null ) {
            params = new LinearLayout.LayoutParams(params);
        }
        else {
            params = new FrameLayout.LayoutParams(params);
        }

        int appliedMargin = ( in instanceof TreeView ) ? 0 : Singleton.dpToPx(mLeftMargin);
        params.setMargins(appliedMargin,0,0,0);
		in.setDividerDrawable(this.getDividerDrawable());
		in.setShowDividers(getShowDividers());
		in.setOrientation(LinearLayout.VERTICAL);

        in.setLayoutParams(params);

		LayoutTransition transition = new LayoutTransition();
		transition.setDuration(200);
		in.setLayoutTransition(transition);
	}

	
	
	
	

	
	//EXPAND, CONTRACT
	
	@Background
	public void expand(View view) {
		List<View> children = mTreeIF.getChildren(view);
        if ( children.size() == 0 ) return;
		LinearLayout innerGroup = new LinearLayout(getContext());
		propertyInit(innerGroup);

		LinearLayout parent = (LinearLayout) view.getParent();
		updater.addView(parent,innerGroup,parent.indexOfChild(view)+1);
		mGroups.put(view, innerGroup);
		
		for ( View child : children ) {
            setCommonOnLongClickListener(child);
            setCommonOnClickListener(child);
			pushView(child);
		}
	}
	
	public void contract(View view) {
		LinearLayout attachedGroup = mGroups.get(view);
		updater.removeView(attachedGroup);
		mGroups.remove(view);
	}
	
	
	//LISTENERS
	
	public class ActionListeners {
		public void click(View v) {
			if ( mGroups.get(v) == null ) expand(v);
			else contract(v);
		}
		public boolean longClick(View v) {
			return true;
		}
	}
	
	ActionListeners mActions;
	
	public void setCommonOnClickListener(final View view) {
		view.setOnClickListener( new OnClickListener() {
			public void onClick(View v) {
				mActions.click(v);
			}
		});
	}
	
	public void setCommonOnLongClickListener(final View view) {
		view.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                return mActions.longClick(v);
            }

        });
	}
	
	
	//QUERY OPERATIONS
	
	public interface ViewQuery {
		public boolean isSelected(Object o);
	}
	
	@Background
	public void removeViews(final ViewQuery query) {
		
		ViewTreeExecIF ifs = new ViewTreeExecIF() {	
			@Override
			public Object exec(View v) {
				StepDO step = (StepDO) mTreeIF.getObject(v);
				if ( query.isSelected(step) ) {
                    updater.removeView(v);
                    if (mGroups.get(v) != null) {
                        updater.removeView(mGroups.get(v));
                        mGroups.remove(v);
                    }
                }
				return null;
			}
		};
		
		Singleton.viewTreeExec(this,ifs);
		
	}

    public void removeItem(Object o) {
        removeItem(o,false);
    }

	@Background
	public void removeItem(final Object o, final boolean updateParent) {

		ViewTreeExecIF ex = new ViewTreeExecIF() { 
			public Object exec(View v) {
            if ( mTreeIF.getObject(v).equals(o) ) {
                updater.removeView(v);
                View attached = mGroups.get(v);
                if ( attached != null ) updater.removeView(attached);
                mGroups.remove(v);
                if (updateParent) updateParent(v);
                return new Object();
            }
            return null;
			}
		};
		Singleton.viewTreeExec(this,ex);
	
	}
	
	public View findView(final Object o) throws RGCheckedNull {
		ViewTreeExecIF ex = new ViewTreeExecIF() {
			public Object exec(View v) {
				if ( mTreeIF.getObject(v).equals(o) ) return v;
				else return null;
			}
		};
		View out = (View) Singleton.viewTreeExec(this, ex);
		if ( out == null ) throw new RGCheckedNull();
		return out;
	}

    private void updateParent(View view) {
        try {
            ViewGroup parent = (ViewGroup) view.getParent();
            Iterator it = mGroups.entrySet().iterator();
            StepView parentView = null;
            while (it.hasNext()) {
                Map.Entry<View, LinearLayout> e = (Map.Entry<View, LinearLayout>) it.next();
                if (e.getValue() == parent) parentView = (StepView) e.getKey();
            }
            if (parentView != null) updater.invalidate(parentView);
        } catch ( ClassCastException | NullPointerException e ) {} //naum kero nem sabe, engole se der m*
    }

	@Background( serial = "treeUpdater" )
	public void updateItem(final Object o) {

		final View view;
		try {
			view = findView(o);
		} catch (RGCheckedNull e) {
			//view not found, ignore
			return;
		}

        //differently from removeItem(), here the attached view is
        //not removed from the mGroups container since this is a
        //temporary removal
		mTreeIF.refreshView(view);
		updater.invalidate(view);

        updateParent(view);

		updater.removeView(view);
        View attached = mGroups.get(view);
        if ( attached != null ) updater.removeView(attached);

        //we need to wait until the fancy animations perform and the views
        //are actually removed from its parents
        while ( view.getParent() != null ||
                (attached != null && attached.getParent() != null) ) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }

        insertView(view);
	}
	
	@Background (serial="treeUpdater")
	public void insertView(final View view) {

		final Object o = mTreeIF.getObject(view);

        final LinearLayout container;
        try {
            container = findContainer(o);
        } catch (RGCheckedNull rgCheckedNull) {
            return;
        }

        //traverse to find the position
		int index = findPosition(o,container);
		
		//add
		updater.addView(container, view, index);
        View attached = mGroups.get(view);
		if ( attached != null && attached.getParent() == null ) {
			int gindex = (index==-1) ? -1 : index+1;
			updater.addView(container, mGroups.get(view), gindex);
		}
        while (container.indexOfChild(view) == -1) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }
	}

    //**BLOCKING
    public void pushView(final View view) {
        final Object o = mTreeIF.getObject(view);
        final LinearLayout container;
        try {
            container = findContainer(o);
        } catch (RGCheckedNull rgCheckedNull) {
            return;
        }
        int index = -1;

        updater.addView(container, view, index);
        //if ( ((StepView)view).getStep() instanceof StepParentDO ) Log.d("test",((StepView)view).getStep().title);
        View attached = mGroups.get(view);
        if (attached != null && attached.getParent() == null) {
            int gindex = (index == -1) ? -1 : index + 1;
            updater.addView(container, mGroups.get(view), gindex);
        }
        while (container.indexOfChild(view) == -1) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }
    }

    private LinearLayout findContainer(Object o) throws RGCheckedNull {
        final Object o_parent = mTreeIF.getObjectParent(o);

        //traverse to find the container
        LinearLayout container1;
        if ( o_parent.equals(-1) || mFlatInsert ) container1 = this;
        else {
            ViewTreeExecIF ex = new ViewTreeExecIF() {
                public Object exec(View v) {

                    Object it_o = mTreeIF.getObject(v);
                    if ( it_o.equals(o_parent) ) {
                        return v;
                    }
                    return null;

                }
            };
            View parent = (View) Singleton.viewTreeExec(this, ex);
            if ( parent == null ) container1 = this;
            else container1 = mGroups.get(parent);
        }
        if (container1 == null) throw new RGCheckedNull();
        return container1;
    }

    private int findPosition(final Object o ,final ViewGroup container) {
        if ( container == null ) return -1;
        ViewTreeExecIF ex2 = new ViewTreeExecIF() {
            @Override
            public Object exec(View v) {
                Object vo = mTreeIF.getObject(v);
                if ( container.indexOfChild(v) != -1 &&
                        mTreeIF.compareObjects(o, vo) > 0)
                    return container.indexOfChild(v);
                return null;
            }
        };
        Integer	index = (Integer) Singleton.viewTreeExec(container,ex2);
        index = ( index==null ) ? -1 : index;
        return index;
    }

    public void insertItem(final Object o) {
        insertItem(o,false);
    }

	public void insertItem(final Object o,boolean updateParent) {
		final View view = mTreeIF.createView(o);
		setCommonOnClickListener(view);
		setCommonOnLongClickListener(view);
		
		insertView(view);
        if ( updateParent ) updateParent(view);
	}

    @Background ( serial = "treeUpdater")
    public void pushItem(Object o) {
        View view = mTreeIF.createView(o);
        setCommonOnClickListener(view);
        setCommonOnLongClickListener(view);

        pushView(view);
    }

    public Map<View,LinearLayout> getExpandedViews() {
        return mGroups;
    }
}
