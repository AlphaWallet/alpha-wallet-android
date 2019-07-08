package io.stormbird.wallet.ui.widget.entity;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import static android.view.MotionEvent.*;

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
        if (pageInterface != null && pageInterface.isViewingDappBrowser())
        {
            //Simple algorithm to ensure only a 'fling' to left or right will page the view.
            //the threshold for a 'fling' seems to be velocity of 5.5 or above.
            switch (ev.getAction())
            {
                case ACTION_DOWN:
                    trackMove = ev.getX();
                    lastX = trackMove;
                    alwaysLeft = true;
                    alwaysRight = true;
                    break;
                case ACTION_UP:
                    float flingDistance = Math.abs(ev.getX() - trackMove);
                    float velocity = (float)flingDistance / (float) (ev.getEventTime() - ev.getDownTime());
                    if (flingDistance > flingRequired && velocity > 5.5f)
                    {
                        if (alwaysLeft) pageInterface.moveLeft();
                        else if (alwaysRight) pageInterface.moveRight();
                    }
                    break;
                case ACTION_MOVE:
                    //moving left, value decreases
                    if ((ev.getX() - lastX) > 0)
                    {
                        alwaysRight = false;
                    }
                    else if ((ev.getX() - lastX) < 0)
                    {
                        alwaysLeft = false;
                    }
                    lastX = ev.getX();
                    break;
                default:
                    break;
            }
        }
        else
        {
            return super.onInterceptTouchEvent(ev);
        }

        return false;
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
