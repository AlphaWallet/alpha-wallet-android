package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import com.alphawallet.app.util.BalanceUtils;

import java.math.BigDecimal;

/**
 * Created by JB on 20/01/2021.
 */
public class NumericInput extends AppCompatAutoCompleteTextView
{
    public NumericInput(@NonNull Context context)
    {
        super(context);
    }

    public NumericInput(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public String getParsedValue()
    {
        CharSequence text = super.getText();

        //ensure text is pre-parsed to remove numeric groupings and convert to standard format
        return BalanceUtils.convertFromLocale(text.toString());
    }

    /**
     * Universal method to obtain an always valid BigDecimal value from user input
     *
     * @return
     */
    public BigDecimal getBigDecimalValue()
    {
        CharSequence text = super.getText();

        //ensure text is pre-parsed to remove numeric groupings and convert to standard format
        String parsedValue = BalanceUtils.convertFromLocale(text.toString());

        if (checkNumericValidity(parsedValue))
        {
            try
            {
                return new BigDecimal(parsedValue);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return BigDecimal.ZERO;
    }

    private boolean checkNumericValidity(String strValue)
    {
        return strValue != null && (strValue.length() > 1
                || (strValue.length() == 1 && Character.isDigit(strValue.charAt(0))));
    }
}
