package io.stormbird.wallet.ui.widget.entity;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

import static android.view.MotionEvent.*;

/**
 * Created by James on 8/07/2019.
 * Stormbird in Sydney
 *
 * This class overrides the SwipeRefreshLayout and makes the swipe refresh less sensitive.
 * To create a swipe refresh event user must make a quick, medium to large downward swipe of less than 300ms;
 * Otherwise a slower event will be treated as a browser scroll event.
 *
 */
public class DappBrowserSwipeLayout extends SwipeRefreshLayout
{
    private float trackMove;
    private DappBrowserSwipeInterface refreshInterface;

    public DappBrowserSwipeLayout(Context context)
    {
        super(context);
    }

    public DappBrowserSwipeLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public void setRefreshInterface(DappBrowserSwipeInterface refresh)
    {
        refreshInterface = refresh;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        switch (ev.getAction())
        {
            case ACTION_DOWN:
                trackMove = ev.getRawY();
                break;
            case ACTION_UP:
                float flingDistance = ev.getRawY() - trackMove;
                if ((ev.getEventTime() - ev.getDownTime()) < 300 && flingDistance > 400) //User wants a swipe refresh
                {
                    refreshInterface.RefreshEvent();
                }
                break;
            case ACTION_MOVE:
                break;
            default:
                break;
        }

        return false;
    }
}
