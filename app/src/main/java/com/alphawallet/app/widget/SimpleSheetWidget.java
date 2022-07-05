package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;

public class SimpleSheetWidget extends LinearLayout
{
    private final TextView label;
    private final TextView value;
    private final TextView caption;

    public SimpleSheetWidget(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_simple_widget, this);
        label = findViewById(R.id.label);
        value = findViewById(R.id.value);
        caption = findViewById(R.id.caption);

        setupAttrs(context, attrs);
    }

    public SimpleSheetWidget(Context context, int labelRes)
    {
        this(context, (AttributeSet) null);
        label.setText(labelRes);
    }

    public SimpleSheetWidget(Context context, String labelRes)
    {
        this(context, (AttributeSet) null);
        label.setText(labelRes);
    }

    private void setupAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SimpleSheetWidget,
                0, 0
        );

        try
        {
            int labelRes = a.getResourceId(R.styleable.SimpleSheetWidget_swLabelRes, R.string.empty);
            int valueRes = a.getResourceId(R.styleable.SimpleSheetWidget_swValueRes, R.string.empty);
            int captionRes = a.getResourceId(R.styleable.SimpleSheetWidget_swCaptionRes, R.string.empty);

            label.setText(labelRes);
            value.setText(valueRes);
            if (captionRes == R.string.empty)
            {
                caption.setVisibility(View.GONE);
            }
            else
            {
                caption.setText(labelRes);
            }
        }
        finally
        {
            a.recycle();
        }
    }

    public void setLabel(String labelText)
    {
        label.setText(labelText);
    }

    public void setValue(String valueText)
    {
        value.setText(valueText);
    }

    public void setCaption(String captionText)
    {
        caption.setVisibility(View.VISIBLE);
        caption.setText(captionText);
    }
}