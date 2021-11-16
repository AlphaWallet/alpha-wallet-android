package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;

/**
 * Created by James on 28/02/2018.
 */

public class QuantitySelectorHolder extends BinderViewHolder<Token> {

    public static final int VIEW_TYPE = 1298;

    private final RelativeLayout plusButton;
    private final RelativeLayout minusButton;
    private final TextView quantity;
    private final TextView title;
    private int currentQuantity;
    private final int quantityLimit;
    private final AssetDefinitionService assetService;

    public int getCurrentQuantity()
    {
        return currentQuantity;
    }

    public QuantitySelectorHolder(int resId, ViewGroup parent, int quantityLimit, AssetDefinitionService assetService) {
        super(resId, parent);
        quantity = findViewById(R.id.text_quantity);
        title = findViewById(R.id.text_quantity_select);
        plusButton = findViewById(R.id.layout_quantity_add);
        minusButton = findViewById(R.id.layout_quantity_minus);
        currentQuantity = Integer.parseInt(quantity.getText().toString());
        this.quantityLimit = quantityLimit;
        this.assetService = assetService;
    }

    @Override
    public void bind(@Nullable Token token, @NonNull Bundle addition)
    {
        String typeName = assetService.getTokenName(token.tokenInfo.chainId, token.tokenInfo.address, 2);
        title.setText(getContext().getString(R.string.select_quantity_tickets, typeName != null ? typeName : "Tickets"));
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

