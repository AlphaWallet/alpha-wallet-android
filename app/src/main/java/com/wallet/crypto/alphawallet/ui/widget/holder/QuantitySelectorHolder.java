package com.wallet.crypto.alphawallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Token;

/**
 * Created by James on 28/02/2018.
 */

public class QuantitySelectorHolder extends BinderViewHolder<Token> {

    public static final int VIEW_TYPE = 1298;

    private final TextView select;
    private final EditText quantity;
    private int currentQuantity;

    public int getCurrentQuantity()
    {
        return currentQuantity;
    }

    public QuantitySelectorHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        select = findViewById(R.id.quantity_txt);
        quantity = findViewById(R.id.quantity);
        currentQuantity = 0;
    }

    @Override
    public void bind(@Nullable Token token, @NonNull Bundle addition)
    {
        quantity.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String quantityTxt = quantity.getText().toString();
                try
                {
                    quantityTxt.trim();
                    currentQuantity = Integer.parseInt(quantityTxt);
                }
                catch (Exception e)
                {

                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

    }
}

