package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.R;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by JB on 20/01/2021.
 */
public class NumericInput extends AppCompatAutoCompleteTextView implements TextWatcher
{
    //If you want to create a release that targets different locale you'll need to update the Regex matcher
    //Also update the display settings in BalanceUtils
    private static final String AVAILABLE_DIGITS = "0123456789. ";
    private static final Pattern numberFormatMatcher = Pattern.compile("(^[0-9 ]+?\\.{0,1}|^\\.)([0-9 ]*)$"); //you will need to edit this if you
    private static final Pattern removeWhiteSpace = Pattern.compile("\\s");
    private final int colorResource;

    public NumericInput(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        //ensure we operate the input in US locale
        setKeyListener(DigitsKeyListener.getInstance(AVAILABLE_DIGITS));
        //ensure input is correct
        addTextChangedListener(this);
        colorResource = getCurrentTextColor();
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
            value = new BigDecimal(removeWhiteSpace.matcher(text.toString().trim()).replaceAll(""));
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return value;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s)
    {
        //run regex
        final Matcher addressMatch = numberFormatMatcher.matcher(s.toString());
        if (!addressMatch.find())
        {
            setTextColor(getResources().getColor(R.color.error));
        }
        else
        {
            setTextColor(colorResource);
        }
    }
}
