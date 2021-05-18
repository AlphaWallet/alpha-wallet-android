package com.alphawallet.app.widget;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;

public class TokenInfoView extends LinearLayout {
    private final TextView label;
    private final TextView value;

    public TokenInfoView(Context context, String labelText)
    {
        super(context);
        inflate(context, R.layout.item_token_info, this);
        label = findViewById(R.id.label);
        value = findViewById(R.id.value);

        label.setText(labelText);
    }

    public void setLabel(String text)
    {
        label.setText(text);
    }

    public void setValue(String text)
    {
        value.setText(text);
    }
}
