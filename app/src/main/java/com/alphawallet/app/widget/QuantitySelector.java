package com.alphawallet.app.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.OnQuantityChangedListener;

import java.math.BigInteger;

public class QuantitySelector extends RelativeLayout implements TextWatcher, TextView.OnEditorActionListener
{
    private final EditText quantityText;
    private final ImageButton incrementBtn;
    private final ImageButton decrementBtn;
    private final int min = 1;
    private OnQuantityChangedListener listener;
    private int quantity = 1;
    private int max;

    public QuantitySelector(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.quantity_selector, this);
        quantityText = findViewById(R.id.quantity);
        incrementBtn = findViewById(R.id.number_up);
        decrementBtn = findViewById(R.id.number_down);
    }

    public QuantitySelector(Context context)
    {
        this(context, null);
    }

    public void init(int maxAmount, OnQuantityChangedListener listener)
    {
        this.max = maxAmount;
        this.listener = listener;
        incrementBtn.setOnClickListener(v -> increment());
        decrementBtn.setOnClickListener(v -> decrement());
        quantityText.setOnEditorActionListener(this);
        quantityText.addTextChangedListener(this);
        set(quantity);
    }

    public void set(int q)
    {
        quantity = q;
        quantityText.getText().clear();
        String qStr = String.valueOf(q);
        if (q > 0)
        {
            quantityText.setText(qStr);
        }
        quantityText.clearFocus();
    }

    public void increment()
    {
        if (quantity + 1 <= max)
        {
            quantityText.setText(String.valueOf(++quantity));
        }
        quantityText.clearFocus();
    }

    public void decrement()
    {
        if (quantity - 1 >= min)
        {
            quantityText.setText(String.valueOf(--quantity));
        }
        quantityText.clearFocus();
    }

    public void reset()
    {
        set(min);
    }

    public int getQuantity()
    {
        return quantity;
    }

    public int getMax()
    {
        return this.max;
    }

    public int getMin()
    {
        return this.min;
    }

    public void setOnQuantityChangedListener(OnQuantityChangedListener listener)
    {
        this.listener = listener;
    }

    public boolean isValid(int quantity)
    {
        return quantity <= getMax() && quantity >= getMin();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void afterTextChanged(Editable s)
    {
        quantity = s.toString().isEmpty() ? 0 : new BigInteger(s.toString()).intValue();
        listener.onQuantityChanged(quantity);
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent)
    {
        if (actionId == EditorInfo.IME_ACTION_DONE)
        {
            quantityText.clearFocus();
        }
        return false;
    }
}
