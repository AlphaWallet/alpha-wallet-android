package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;

/**
 * Created by JB on 26/08/2021.
 */
public class StandardHeader extends LinearLayout
{
    public StandardHeader(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_standard_header, this);
        getAttrs(context, attrs);
    }

    private void getAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.StandardHeader,
                0, 0
        );

        try
        {
            int headerId = a.getResourceId(R.styleable.StandardHeader_headerText, R.string.empty);
            TextView headerText = findViewById(R.id.text_header);
            headerText.setText(headerId);
        }
        finally
        {
            a.recycle();
        }
    }
}
