package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GasWarningLayout extends FrameLayout
{
    private VisibilityCallback visibilityCallback;

    public GasWarningLayout(@NonNull Context context)
    {
        super(context);
    }

    public GasWarningLayout(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public void onVisibilityChanged(View changedView, int visibility)
    {
        super.onVisibilityChanged(changedView, visibility);
        if (visibilityCallback != null)
        {
            visibilityCallback.onVisibilityChanged(visibility == View.VISIBLE);
        }
    }
    public void setVisibilityCallback(VisibilityCallback vs)
    {
        visibilityCallback = vs;
    }
}
