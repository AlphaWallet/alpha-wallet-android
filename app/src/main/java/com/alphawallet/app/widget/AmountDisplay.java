package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;

import javax.annotation.Nullable;

/**
 * Created by Jenny Jingjing Li on 21/03/2021
 * */

public class AmountDisplay extends LinearLayout {
    private TextView amount;

    public AmountDisplay(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_amount_display, this);
        amount = findViewById(R.id.text_amount);
    }


    public void setAmountString(String displayStr)
    {
        amount.setText(displayStr);
    }
}


