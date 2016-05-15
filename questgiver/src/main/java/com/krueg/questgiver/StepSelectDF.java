package com.krueg.questgiver;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

import com.krueg.questgiver.Exceptions.RGException;
import com.krueg.questgiver.Exceptions.RGRuntimeException;
import com.krueg.questgiver.Exceptions.RGTopLevelException;
import com.krueg.questgiver.dataobject.StepDO;
import com.krueg.questgiver.dataobject.StepParentDO;
import com.krueg.questgiver.dataobject.StepSingleDO;
import com.krueg.questgiver.dataobject.StepSubsequentDO;
import com.krueg.questgiver.dataobject.StepWeekDO;
import com.krueg.questgiver.dialog.DialogBuilder;
import com.krueg.questgiver.dialog.DialogBuilder.DialogBuilderCallback;
import com.krueg.questgiver.dialog.DialogBuilder.ListItem;
import com.krueg.questgiver.dialog.DialogBuilder.ViewItem;
import com.krueg.questgiver.dialog.SimpleConfirmDF;
import com.krueg.questgiver.dialog.SimpleConfirmDF.SimpleConfirmIF;
import com.krueg.questgiver.dialog.SimpleOptDF;
import com.krueg.questgiver.dialog.SimpleOptDF.SimpleOptIF;
import com.krueg.questgiver.views.DeadlinePicker;
import com.krueg.questgiver.views.IntegerWithLabelView;
import com.krueg.questgiver.views.StepGroupPicker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class StepSelectDF extends DialogFragment {
	
	public static final int ACT_MOVE = 0;
	public static final int ACT_REMOVE = 1;
	
	public interface StepSelectIF {
		public void stepAfterSelect(ListItem choice,StepDO step);
	}

	StepDO mStep;
	
	public static StepSelectDF newInstance (StepDO step,StepSelectIF ifs) {
		StepSelectDF df = new StepSelectDF();
		df.mStep = step;
		if ( ifs != null ) {
			df.mListener = ifs;
		} else {
			df.mListener = new StepSelectIF() {
				public void stepAfterSelect(ListItem choice, StepDO step) {}
			};
		}

		return df;
	}
	
	private StepSelectIF mListener;
	
	public static List<StepDO> listNextSteps(StepDO from) {
		List<StepDO> list;
		try {	//TODO make empty stepdo a query builder, or use a framework
			StepParentDO dummy = from.getParent();
			list = dummy.listMe(dummy.queryChildren());
		} catch ( RGTopLevelException e ) {
			StepDO dummy = new StepDO();
			list = dummy.listMe(dummy.queryRoot());
		}
		if ( list.size() == 0 ) return list;
		Deque<StepDO> toRemove = new ArrayDeque<StepDO>();
		for (StepDO s : list) { 
			if ( s.isComplete() || !(s instanceof StepSubsequentDO) ||
					((StepSubsequentDO)s).isActive() )
			toRemove.add(s);
		}
		for (StepDO s : toRemove) { list.remove(s); }
		//if I remove the list items while iterating i get a ConcurrentEx
		return list;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		String title = mStep.title + ": x" + String.format("%1$1.2f",
				mStep.getRelevanceTimesCompletion());
		DialogBuilderCallback callb = new DialogBuilderCallback() {
			public void onSelectItem(ListItem it) {
				mListener.stepAfterSelect(it,mStep);
			}
		};
		
		DialogBuilder builder = new DialogBuilder(title,getActivity(),callb);
		
		//ADD CHILD STEP
		if ( mStep instanceof StepParentDO && !mStep.isComplete() ) {
			ListItem itemChild = builder.new ListItem();
			itemChild.title = getString(R.string.new_step);
			itemChild.act = new DialogBuilder.ItemAction() { public void act() {
				Intent intent = new Intent(getActivity(), Step.class);
				intent.putExtra(Step.PARENT_SEND, mStep.getId());
				getActivity().startActivityForResult(intent,Landing.REQUEST_STEP);
			}};
			builder.listItems.add(itemChild);
		}
		
		//SET COMPLETE
		if ( (!(mStep instanceof StepWeekDO) && !mStep.isComplete()) ||
				(mStep instanceof StepWeekDO && !((StepWeekDO) mStep).isDoneToday() &&
				!mStep.isComplete()) ) {
			final ListItem l1 = builder.new ListItem();
			l1.title = getString(R.string.set_complete);
			l1.act = new DialogBuilder.ItemAction() {	public void act() {	
				try { mStep.setComplete(getActivity()); 	}
				catch ( RGRuntimeException e )  {
					Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG)
						.show();
					return;
				}
				
				if ( !(mStep instanceof StepWeekDO) ) {
					List<StepDO> list = listNextSteps(mStep);
					if ( list.size() > 0 ) {
						String[] slist = new String[list.size()];
						int it = 0;
						for ( StepDO s : list ) slist[it++] = s.title;
						SimpleOptIF ifs = new SimpleOptIF() {
							public void onItemClick(SimpleOptDF dialog, int which) {
								StepSubsequentDO step = (StepSubsequentDO) StepDO.newInstance(dialog.tags[which]);
								try { step.activate(); }
								catch (RGException e) {
									Toast.makeText(Singleton.context,
											"This step shouldn't be here!",
											Toast.LENGTH_LONG).show();
									e.printStackTrace();
								}
								mListener.stepAfterSelect(l1,mStep);
							}
						};
						
						SimpleOptDF opt = SimpleOptDF.newInstance(
								getString(R.string.step_choose_subsequent),
								slist, ifs);
						int[] taglist = new int[list.size()];
						it = 0;
						for ( StepDO step : list ) { taglist[it++] = step.getId(); }
						opt.tags = taglist;
						opt.show(getFragmentManager(), "next");
					}
				}
				
				return;
			}};
			builder.listItems.add(l1);
			
			//ACTIVATE SUBSEQUENT STEP
			if ( mStep.getClass() == StepSubsequentDO.class &&
				!((StepSubsequentDO)mStep).isActive() ) {
				ListItem activateItem = builder.new ListItem();
				activateItem.title = getString(R.string.activate);
				activateItem.act = new DialogBuilder.ItemAction() { public void act() {
					try { ((StepSubsequentDO)mStep).activate(); } catch (RGException e) {}
					//state check already performed, ok
				}};
				builder.listItems.add(activateItem);
			}
			
			//CHANGE DEADLINE
			if ( mStep.getClass() == StepSingleDO.class ||
					mStep.getClass() == StepSubsequentDO.class) {
				ViewItem deadlineItem = builder.new ViewItem();
				final DeadlinePicker deadlineview = new DeadlinePicker(getActivity());
				deadlineview.setText(getString(R.string.step_change_deadline));
				int currentDeadline = ((StepSingleDO)mStep).getDeadline1(); 
				deadlineview.setValue(currentDeadline);
				deadlineview.setTag(currentDeadline);
				deadlineItem.view = deadlineview;
				deadlineItem.onConfirm = new DialogBuilder.ViewAction() { public void onConfirm() {
					if ( !((Integer)deadlineview.getTag()).equals(deadlineview.getValue())  ) {	//"is changed"
						((StepSingleDO)mStep).setDeadline((int) deadlineview.getValue());
					}
				}};
				builder.viewItems.add(deadlineItem);
			}
		}
		
		//MOVE TO GROUP
		ListItem moveItem = builder.new ListItem();
		moveItem.title = getString(R.string.step_move);
		moveItem.id = ACT_MOVE;
		moveItem.act = new DialogBuilder.ItemAction() { public void act() {
		}};
		builder.listItems.add(moveItem);
		
		//CHANGE RELEVANCE
		if ( !mStep.isComplete() ) {
			ViewItem relevanceViewItem = builder.new ViewItem();
			IntegerWithLabelView t_view;
			if ( mStep instanceof StepParentDO ) t_view = new StepGroupPicker(getActivity());
			else t_view = new IntegerWithLabelView(getActivity());
			final IntegerWithLabelView view = t_view;
			view.setText(getString(R.string.step_change_relevance));
			view.setValue(mStep.relevance);
			view.setTag(mStep.relevance);
			relevanceViewItem.view = view;
			relevanceViewItem.onConfirm = new DialogBuilder.ViewAction() {
				public void onConfirm() {
					if ( !((Number)view.getTag()).equals(view.getValue()) ) {	//"is changed"
						mStep.setRelevance(view.getValue());
					}
				}
			};
			builder.viewItems.add(relevanceViewItem);
		}
		
		//CHANGE EXPECTED TIME
		if ( mStep instanceof StepWeekDO || mStep instanceof StepSingleDO) {
			ViewItem timeItem = builder.new ViewItem();
			final IntegerWithLabelView view = new IntegerWithLabelView(getActivity());
			view.setText(getString(R.string.step_hourcost_desc));
			if ( mStep instanceof StepWeekDO ) view.setValue(((StepWeekDO)mStep).getHourCost());
			else view.setValue(((StepSingleDO)mStep).getHourCost());
			timeItem.view = view;
			timeItem.onConfirm = new DialogBuilder.ViewAction() {
				public void onConfirm() {
					if ( mStep instanceof StepWeekDO )
						((StepWeekDO) mStep).setHourCost((int) view.getValue());
					else ((StepSingleDO) mStep).setHourCost((int) view.getValue());
				}
			};
			builder.viewItems.add(timeItem);
		}
		
		//NOT TODAY, BABY
		{
			ListItem l3 = builder.new ListItem();
			l3.title = getString(R.string.step_not_today); 
			l3.act = new DialogBuilder.ItemAction() { public void act() {
				mStep.setIgnored();
				Toast.makeText(getActivity(),R.string.step_not_today_help,Toast.LENGTH_LONG).show();
			}};
			builder.listItems.add(l3);
		}
		
		{
			//REMOVE
			final ListItem l4 = builder.new ListItem();
			l4.id = ACT_REMOVE;
			l4.title = getString(R.string.remove); 
			l4.act = new DialogBuilder.ItemAction() { public void act() {
				SimpleConfirmIF ifs = new SimpleConfirmIF() { public void onReturn(boolean ans) {
					if (ans) mStep.delete();
					mListener.stepAfterSelect(l4,mStep);
				}};
				String alert = String.format(getString(R.string.step_delete_confirm),mStep.title);
				if ( mStep instanceof StepParentDO ) alert += " " + getString(R.string.step_parent_delete); 
				SimpleConfirmDF cdf = SimpleConfirmDF.newInstance(alert,ifs);
				cdf.show(getFragmentManager(), "deletestep");
			}};
			builder.listItems.add(l4);
		}
		
		return builder.buildDialog();
	}
}
