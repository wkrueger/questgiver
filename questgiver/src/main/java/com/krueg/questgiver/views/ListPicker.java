package com.krueg.questgiver.views;

import android.content.Context;
import android.util.AttributeSet;

import com.krueg.questgiver.R;

import java.text.DecimalFormat;

import antistatic.spinnerwheel.adapters.AbstractWheelAdapter;
import antistatic.spinnerwheel.adapters.AbstractWheelTextAdapter;

abstract public class ListPicker extends IntegerWithLabelView {

    public ListPicker(Context context) {
        super(context);
    }
    
    public ListPicker(Context context,AttributeSet attrs) {
        super(context,attrs);
    }

    @Override
    protected AbstractWheelAdapter getAdapter(Context context) {
        AbstractWheelAdapter out = new DdAdapter(context);
        return out;
    }
    
    //Round up input value
    @Override
    public void setValue(float value) {
        int index = 0;
        while ( index < getList().length - 1 ) {
            if ( value <= getList()[index] ) break;
            index++;
        }
        mEdit.setCurrentItem(index);
    }
    
    @Override
    public float getValue() {
        return getList()[mEdit.getCurrentItem()];
    }
    
    abstract protected String getFormat();
    
    protected abstract float[] getList();
    
    private class DdAdapter extends AbstractWheelTextAdapter {
        
        protected DdAdapter(Context context) {
            super(context,R.layout.sub_wheel_number,NO_RESOURCE);
            setItemTextResource(R.id.text);
        }

        @Override
        public int getItemsCount() {
            return getList().length;
        }

        @Override
        protected CharSequence getItemText(int index) {
            DecimalFormat fmt = new DecimalFormat(getFormat());
            return fmt.format(getList()[index]);
        }
        
    }
    
}
