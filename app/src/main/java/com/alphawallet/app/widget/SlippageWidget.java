package com.alphawallet.app.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.util.KeyboardUtils;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.math.BigDecimal;

public class SlippageWidget extends LinearLayout
{
    private static final String SLIPPAGE_VALUE_1 = "0.1%";
    private static final String SLIPPAGE_VALUE_2 = "0.5%";
    private static final String SLIPPAGE_VALUE_3 = "1%";
    private RadioGroup radioGroup;
    private MaterialRadioButton radio1;
    private MaterialRadioButton radio2;
    private MaterialRadioButton radio3;
    private MaterialRadioButton radio4;
    private EditText editText;

    public SlippageWidget(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_slippage_widget, this);

        radioGroup = findViewById(R.id.radio_group);
        radio1 = findViewById(R.id.radio1);
        radio1.setText(SLIPPAGE_VALUE_1);
        radio2 = findViewById(R.id.radio2);
        radio2.setText(SLIPPAGE_VALUE_2);
        radio3 = findViewById(R.id.radio3);
        radio3.setText(SLIPPAGE_VALUE_3);
        radio4 = findViewById(R.id.radio4);
        editText = findViewById(R.id.edit_text);

        radio4.setOnCheckedChangeListener((compoundButton, b) -> {
            editText.setEnabled(b);
            if (b)
            {
                editText.requestFocus();
                KeyboardUtils.showKeyboard(editText);
            }
            else
            {
                editText.clearFocus();
                KeyboardUtils.hideKeyboard(editText);
            }
        });
    }

    public String getSlippage()
    {
        if (radio1.isChecked())
        {
            return "0.001";

        }
        else if (radio2.isChecked())
        {
            return "0.005";

        }
        else if (radio3.isChecked())
        {
            return "0.01";
        }
        else
        {
            String customVal = editText.getText().toString();
            BigDecimal d = new BigDecimal(customVal);
            if (TextUtils.isEmpty(customVal))
            {
                return "0";
            }
            else
            {
                return d.movePointLeft(2).toString();
            }
        }
    }
}