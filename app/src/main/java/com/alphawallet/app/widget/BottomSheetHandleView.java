package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;

public class BottomSheetHandleView extends LinearLayout
{
    public BottomSheetHandleView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.layout_bottom_sheet_handle, this);
    }
}
