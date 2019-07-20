package io.stormbird.wallet.ui.widget.entity;

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
    private ScrollControlInterface pageInterface;
    private float trackMove;
    private boolean alwaysLeft;
    private boolean alwaysRight;
    private float lastX;
    private float flingRequired;

    public ScrollControlViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollControlViewPager(@NonNull Context context)
    {
        super(context);
    }

    public void setInterface(ScrollControlInterface pInterface, int width)
    {
        pageInterface = pInterface;
        flingRequired = ((float)width)*0.6f;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return false;
    }

    @Override
    public boolean performClick()
    {
        return super.performClick();
    }
}
