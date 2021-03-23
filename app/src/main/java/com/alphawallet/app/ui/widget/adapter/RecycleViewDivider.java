package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.ui.widget.holder.TransferHolder;

public class RecycleViewDivider extends RecyclerView.ItemDecoration {

    private final int[] ATTRS = new int[]{android.R.attr.listDivider};
    private Drawable mDivider;
    private final Rect mBounds = new Rect();
    private int marginPx = 2;
    private int childTxMargin = marginPx;

    public RecycleViewDivider(Context context)
    {
        TypedArray a = context.obtainStyledAttributes(ATTRS);
        this.mDivider = a.getDrawable(0);
        if (this.mDivider == null)
        {
            Log.w("DividerItem", "@android:attr/listDivider was not set in the theme used for this DividerItemDecoration. Please set that attribute all call setDrawable()");
        }

        a.recycle();
    }

    public void setDrawable(@NonNull Drawable drawable)
    {
        this.mDivider = drawable;
    }

    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state)
    {
        canvas.save();
        int left = marginPx;
        int right = parent.getWidth() - marginPx;
        canvas.clipRect(left, parent.getPaddingTop(), right, parent.getHeight() - parent.getPaddingBottom());

        int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; ++i)
        {
            int dividerHeight = this.mDivider.getIntrinsicHeight();
            View child = parent.getChildAt(i);
            left = marginPx;
            if (i < (childCount - 1))
            {
                View nextView = parent.getChildAt(i+1);
                if (isChildView(nextView)) //since we are building the margin below, we have to look at the next view so we can effectively be building the margin above
                {
                    left = findChildViewMargin(nextView);
                }
            }

            parent.getDecoratedBoundsWithMargins(child, this.mBounds);
            int bottom = this.mBounds.bottom + Math.round(child.getTranslationY());
            int top = bottom - dividerHeight;
            this.mDivider.setBounds(left, top, right, bottom);
            this.mDivider.draw(canvas);
        }

        canvas.restore();
    }

    private int findChildViewMargin(View nextView)
    {
        if (childTxMargin == marginPx)
        {
            for (int index = 0; index < ((ViewGroup) nextView).getChildCount(); index++)
            {
                View nextChild = ((ViewGroup) nextView).getChildAt(index);
                if (nextChild instanceof RelativeLayout)
                {
                    childTxMargin = nextChild.getLeft();
                    break;
                }
            }
        }

        return childTxMargin;
    }

    private boolean isChildView(View child)
    {
        if (child.getLabelFor() == TransferHolder.VIEW_TYPE)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
    {
        outRect.set(0, 0, 0, this.mDivider.getIntrinsicHeight());
    }
}