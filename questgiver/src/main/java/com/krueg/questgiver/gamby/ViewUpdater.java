package com.krueg.questgiver.gamby;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.UiThread;

@SuppressWarnings("static-method")
@EBean( scope = Scope.Singleton )
public class ViewUpdater {
    public ViewUpdater() {
    }
    
    @UiThread
    public void removeView(View view) {
        ((ViewGroup)view.getParent()).removeView(view);
    }

    @UiThread
    public void addView(LinearLayout parent,View inner,int index) {
        parent.addView(inner,index);
        //if ( ((StepView)inner).getStep() instanceof StepParentDO) Log.d("test-view", ((StepView) inner).getStep().title);
    }

    @UiThread
    public void addView(LinearLayout parent,View inner,int index,ViewGroup.LayoutParams params) {
        parent.addView(inner,index,params);
    }

    @UiThread
    public void invalidate(View view) {
        view.invalidate();
    }
    
    @UiThread
    public void setImageBitmap(ImageView view,Bitmap bmp) {
        view.setImageBitmap(bmp);
    }
    
}
