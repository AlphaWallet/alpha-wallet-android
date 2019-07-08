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

    public ScrollControlViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollControlViewPager(@NonNull Context context)
    {
        super(context);
    }

    public void setInterface(ScrollControlInterface pInterface)
    {
        pageInterface = pInterface;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (pageInterface != null && pageInterface.isViewingDappBrowser())
        {
            return false;
        }
        else
        {
            return super.onInterceptTouchEvent(event);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (pageInterface != null && pageInterface.isViewingDappBrowser())
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
