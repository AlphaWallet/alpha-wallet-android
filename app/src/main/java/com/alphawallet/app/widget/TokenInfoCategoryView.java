package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;

public class TokenInfoCategoryView extends LinearLayout {
    private TextView title;

    public TokenInfoCategoryView(Context context, String titleText)
    {
        this(context, (AttributeSet) null);
        title.setText(titleText);
    }

    public TokenInfoCategoryView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_token_info_category, this);
        getAttrs(context, attrs);
    }

    private void getAttrs(Context context, AttributeSet attrs)
    {

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TokenInfoCategoryView,
                0, 0
        );

        try
        {
            int titleRes = a.getResourceId(R.styleable.TokenInfoCategoryView_title, R.string.empty);
            title = findViewById(R.id.title);
            title.setText(titleRes);
        }
        finally
        {
            a.recycle();
        }
    }

    public void setTitle(String text)
    {
        title.setText(text);
    }
}
