package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.alphawallet.app.util.Utils;

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

    public void setBottomMargin(boolean active)
    {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) this.getLayoutParams();

        if (active)
        {
            TypedValue tv = new TypedValue();
            if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            {
                layoutParams.bottomMargin = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            }
        }
        else
        {
            layoutParams.bottomMargin = 0;
        }
    }

    public boolean hasBottomMargin()
    {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) this.getLayoutParams();
        return layoutParams.bottomMargin > 0;
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

