package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;

/**
 * Created by James on 28/02/2018.
 */

public class QuantitySelectorHolder extends BinderViewHolder<Token> {

    public static final int VIEW_TYPE = 1298;

    private final RelativeLayout plusButton;
    private final RelativeLayout minusButton;
    private final TextView quantity;
    private int currentQuantity;
    private int quantityLimit;

    public int getCurrentQuantity()
    {
        return currentQuantity;
    }

    public QuantitySelectorHolder(int resId, ViewGroup parent, int quantityLimit) {
        super(resId, parent);
        quantity = findViewById(R.id.text_quantity);
        plusButton = findViewById(R.id.layout_quantity_add);
        minusButton = findViewById(R.id.layout_quantity_minus);
        currentQuantity = Integer.parseInt(quantity.getText().toString());
        this.quantityLimit = quantityLimit;
    }

    @Override
    public void bind(@Nullable Token token, @NonNull Bundle addition)
    {
        plusButton.setOnClickListener(v -> {
            int val = Integer.parseInt(quantity.getText().toString());
            if ((val + 1) <= quantityLimit) {
                val++;
                quantity.setText(String.valueOf(val));
            }
        });

        minusButton.setOnClickListener(v -> {
            int val = Integer.parseInt(quantity.getText().toString());
            if ((val-1) >= 0) {
                val--;
                quantity.setText(String.valueOf(val));
            }
        });

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

