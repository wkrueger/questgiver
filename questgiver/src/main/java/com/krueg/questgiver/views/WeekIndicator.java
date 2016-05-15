package com.krueg.questgiver.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.joda.time.DateTime;

/**
 * Created by willian on 07/07/14.
 */
public class WeekIndicator extends View {

    private int LOST_COLOR = 0xFFD78375;        //reddish
    private int ACTIVE_COLOR = 0xFF7EDA72;      //greenish
    private int INACTIVE_COLOR = 0xFFA6A6A6;    //grayish
    private int AVAILABLE_COLOR = 0xFFFBFF7C;   //yellowish

    private Paint squaredPaint;

    private int mWeekMask = 0;
    DateTime caltoday = new DateTime();

    private int mRemaining = 0;

    protected void init() {
        squaredPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public WeekIndicator(Context context) {
        super(context);
        init();
    }

    public WeekIndicator(Context context, AttributeSet attrs) {
        super(context,attrs);
        init();
    }

    public void setState(int weekMask,int remaining) {
        mWeekMask = weekMask;
        mRemaining = remaining;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() == 0) return;
        int ref = Math.min(getWidth(),getHeight());
        int boxSize = (ref - 6) / 3;
        int centerx = (getWidth() - boxSize*3 - 6)/2;
        int centery = (getHeight() - boxSize*3 - 6)/2;

        int remaining = mRemaining;
        for (int day = 1; day <= 7; day++) {
            int thecolor = INACTIVE_COLOR;
            if (((mWeekMask >> (day - 1)) & 1) != 0)
                thecolor = ACTIVE_COLOR;
            else if (caltoday.getDayOfWeek() > day)
                thecolor = LOST_COLOR;
            else if (remaining > 0) {
                thecolor = AVAILABLE_COLOR;
                remaining--;
            }
            squaredPaint.setColor(thecolor);

            int top = (day + 1) / 3 * (boxSize + 2);
            top += centery;
            int xpos = (day == 1) ? 2 : (day - 2) % 3;
            xpos *= boxSize + 2;
            xpos += centerx;
            int left = xpos;
            canvas.drawRect(left, top, left + boxSize, top + boxSize, squaredPaint);

        }
    }
}
