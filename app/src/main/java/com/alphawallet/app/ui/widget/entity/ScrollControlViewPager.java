package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by James on 8/07/2019.
 * Stormbird in Sydney
 */
public class ScrollControlViewPager extends ViewPager
{
    private boolean isLocked = true; //locked by default

    public ScrollControlViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollControlViewPager(@NonNull Context context)
    {
        super(context);
    }

    public void lockPages(boolean locked)
    {
        isLocked = locked;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        if (isLocked)
        {
            return false;
        }
        else
        {
            return super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isLocked)
        {
            return false;
        }
        else
        {
            return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick()
    {
        return super.performClick();
    }
}

