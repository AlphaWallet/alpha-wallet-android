package com.alphawallet.app.widget;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;

public class TokenInfoCategoryView extends LinearLayout {
    private final TextView title;

    public TokenInfoCategoryView(Context context, String titleText)
    {
        super(context);
        inflate(context, R.layout.item_token_info_category, this);
        title = findViewById(R.id.title);

        title.setText(titleText);
    }

    public void setTitle(String text)
    {
        title.setText(text);
    }
}
