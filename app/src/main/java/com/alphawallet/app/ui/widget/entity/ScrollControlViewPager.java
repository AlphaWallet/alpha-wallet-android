package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.util.Utils;

/**
 * Created by James on 8/07/2019.
 * Stormbird in Sydney
 */
public class ScrollControlViewPager extends ViewPager
{
    private boolean isLocked = true; //locked by default
    private PagerCallback pagerCallback = null;

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
    public void setCompletionCallback(PagerCallback cb)
    {
        pagerCallback = cb;
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

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params)
    {
        super.addView(child, index, params);
        if (pagerCallback != null && index == (WalletPage.values().length - 1))
        {
            pagerCallback.loadingComplete();
        }
    }
}

