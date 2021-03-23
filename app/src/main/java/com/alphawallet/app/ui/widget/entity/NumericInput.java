package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import android.text.Editable;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.LocaleUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by JB on 20/01/2021.
 */
public class NumericInput extends AppCompatAutoCompleteTextView
{
    private final Locale deviceSettingsLocale = LocaleUtils.getDeviceLocale(getContext());

    public NumericInput(@NonNull Context context)
    {
        super(context);
    }

    public NumericInput(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        //ensure we use the decimal separator appropriate for the phone settings
        char separator = DecimalFormatSymbols.getInstance(deviceSettingsLocale).getDecimalSeparator();
        setKeyListener(DigitsKeyListener.getInstance("0123456789" + separator));
    }

    /**
     * Universal method to obtain an always valid BigDecimal value from user input
     *
     * @return
     */
    public BigDecimal getBigDecimalValue()
    {
        CharSequence text = super.getText();
        BigDecimal value = BigDecimal.ZERO;

        try
        {
            DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(deviceSettingsLocale);
            df.setParseBigDecimal(true);
            value = (BigDecimal) df.parseObject(text.toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return value;
    }
}
