package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.util.LocaleUtils;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * Created by Jenny Jingjing Li on 21/03/2021
 * */

public class AmountDisplayWidget extends LinearLayout {

    private final Locale deviceSettingsLocale = LocaleUtils.getDeviceLocale(getContext());
    private TextView amount;


    public AmountDisplayWidget(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);

        inflate(context, R.layout.item_amount_display, this);
        amount = findViewById(R.id.text_amount);
    }


    public void setAmountFromString(String displayStr)
    {
        amount.setText(displayStr);
    }

    public void setAmountFromBigInteger(BigInteger txAmount, String token)
    {
        NumberFormat decimalFormat = NumberFormat.getInstance(deviceSettingsLocale);
        setAmountFromString(decimalFormat.format(txAmount) + ' ' + token);
    }
}