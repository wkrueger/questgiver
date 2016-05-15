package com.krueg.questgiver.views;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.InvocationTargetException;

public class ManagedView extends ViewGroup {

    public ViewGroup orig;
    Class<?> classToken;
    
    public ManagedView(Context context,Class<?> theClass) {
        super(context);
        classToken = theClass;
        try {
            orig = (ViewGroup) theClass.getConstructor(Context.class)
                    .newInstance(context);
        } catch (InstantiationException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e) {
            //...
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        try {
            classToken.getMethod("onLayout",
                    new Class<?>[]{boolean.class,int.class,
                    int.class,int.class})
                .invoke(orig, new Object[]{changed,l,t,r,b});
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException e) {
            //...
            throw new RuntimeException(e);
        }
    }
    
    @Override
  public void addView(View v) {
    /*...*/
  }

}
