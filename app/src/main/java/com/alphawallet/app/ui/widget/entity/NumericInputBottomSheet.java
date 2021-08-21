package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.alphawallet.app.R;
import com.alphawallet.app.util.KeyboardUtils;

import java.math.BigDecimal;

/**
 * Created by JB on 17/08/2021.
 */
public class NumericInputBottomSheet extends ConstraintLayout
{
    private final TextView textAmountMax;
    private final NumericInput textAmount;
    private final Button buttonUp;
    private final Button buttonDown;
    private AmountReadyCallback amountReady;
    private boolean gotFocus;
    private int activePosition = -1;

    public NumericInputBottomSheet(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.numeric_input_bottom, this);
        textAmount = findViewById(R.id.number);
        textAmountMax = findViewById(R.id.select_amount_max);
        buttonUp = findViewById(R.id.number_up);
        buttonDown = findViewById(R.id.number_down);
        gotFocus = false;
    }

    public void initAmount(final int maxAmount, int startingAmount, AmountReadyCallback callback, int position)
    {
        textAmountMax.setText(getContext().getString(R.string.input_amount_max, String.valueOf(maxAmount)));

        if (amountReady != null && activePosition != position)
        {
            amountReady.amountReady(textAmount.getBigDecimalValue(), BigDecimal.valueOf(activePosition));
        }

        textAmount.setText(String.valueOf(startingAmount));
        amountReady = callback;

        buttonUp.setOnClickListener(v -> {
            int currentAmount = textAmount.getBigDecimalValue().intValue();
            if (currentAmount < maxAmount)
            {
                textAmount.setText(String.valueOf(currentAmount + 1));
            }
        });
        buttonDown.setOnClickListener(v -> {
            int currentAmount = textAmount.getBigDecimalValue().intValue();
            if (currentAmount > 1)
            {
                textAmount.setText(String.valueOf(currentAmount - 1));
            }
        });

        textAmount.requestFocus();
        setupCallbacks(position);
    }

    private void setupCallbacks(final int position)
    {
        activePosition = position;
        textAmount.setOnEditorActionListener((v, actionId, event) -> {
            if (amountReady != null)
            {
                amountReady.amountReady(textAmount.getBigDecimalValue(), BigDecimal.valueOf(position));
                amountReady = null;
                activePosition = -1;
            }

            setVisibility(View.GONE);
            KeyboardUtils.hideKeyboard(this);
            return false;
        });

        textAmount.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus)
            {
                if (amountReady != null)
                    amountReady.amountReady(textAmount.getBigDecimalValue(), BigDecimal.valueOf(position));
                setVisibility(View.GONE);
                KeyboardUtils.hideKeyboard(this);
                activePosition = -1;
                amountReady = null;
            }
        });
    }

    public NumericInput getInputField()
    {
        return textAmount;
    }
}
