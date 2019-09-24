package com.alphawallet.app.ui.widget.holder;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionOperation;
import com.alphawallet.app.ui.widget.OnTransactionClickListener;

import static com.alphawallet.app.C.ETHER_DECIMALS;

/**
 * Created by James on 4/03/2018.
 */

public class ContractTransactionHolder extends BinderViewHolder<Transaction> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1689;

    private static final int SIGNIFICANT_FIGURES = 3;

    public static final String DEFAULT_ADDRESS_ADDITIONAL = "default_address";
    public static final String DEFAULT_SYMBOL_ADDITIONAL = "network_symbol";

    private final TextView type;
    private final TextView address;
    private final TextView value;
    private final ImageView typeIcon;

    private Transaction transaction;
    private String defaultAddress;
    private OnTransactionClickListener onTransactionClickListener;

    public ContractTransactionHolder(int resId, ViewGroup parent) {
        super(resId, parent);

        typeIcon = findViewById(R.id.type_icon);
        address = findViewById(R.id.address);
        type = findViewById(R.id.type);
        value = findViewById(R.id.value);

        typeIcon.setColorFilter(
                ContextCompat.getColor(getContext(), R.color.item_icon_tint),
                PorterDuff.Mode.SRC_ATOP);

        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable Transaction data, @NonNull Bundle addition) {
        transaction = data; // reset
        if (this.transaction == null) {
            return;
        }
        defaultAddress = addition.getString(DEFAULT_ADDRESS_ADDITIONAL);

        String networkSymbol = addition.getString(DEFAULT_SYMBOL_ADDITIONAL);
        // If operations include token transfer, display token transfer instead
        TransactionOperation operation = transaction.operations == null
                || transaction.operations.length == 0 ? null : transaction.operations[0];

        if (operation == null || operation.contract == null) {
            // default to ether transaction
            fill(transaction.error, transaction.from, transaction.to, networkSymbol, transaction.value,
                    ETHER_DECIMALS, transaction.timeStamp);
        } else {
            fill(transaction.error, operation.from, operation.to, operation.contract.symbol, operation.value,
                    operation.contract.decimals, transaction.timeStamp);
        }
    }

    private void fill(
            String error,
            String from,
            String to,
            String symbol,
            String valueStr,
            long decimals,
            long timestamp) {
        boolean isSent = from.toLowerCase().equals(defaultAddress);
        type.setText(isSent ? getString(R.string.sent) : getString(R.string.received));
        if (!TextUtils.isEmpty(error)) {
            typeIcon.setImageResource(R.drawable.ic_error_outline_black_24dp);
        } else if (isSent) {
            typeIcon.setImageResource(R.drawable.ic_arrow_upward_black_24dp);
        } else {
            typeIcon.setImageResource(R.drawable.ic_arrow_downward_black_24dp);
        }
        address.setText(isSent ? to : from);
        value.setTextColor(ContextCompat.getColor(getContext(), isSent ? R.color.red : R.color.green));

        if (valueStr.equals("0")) {
            valueStr = "0 " + symbol;
        } else {
            valueStr = (isSent ? "-" : "+") + Token.getScaledValue(valueStr, decimals) + " " + symbol;
        }

        this.value.setText(valueStr);
    }

    @Override
    public void onClick(View view) {
        if (onTransactionClickListener != null) {
            onTransactionClickListener.onTransactionClick(view, transaction);
        }
    }

    public void setOnTransactionClickListener(OnTransactionClickListener onTransactionClickListener) {
        this.onTransactionClickListener = onTransactionClickListener;
    }
}
