package com.wallet.crypto.trustapp.ui.widget.holder;

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

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.TransactionOperation;
import com.wallet.crypto.trustapp.ui.widget.OnTransactionClickListener;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.wallet.crypto.trustapp.C.ETHER_DECIMALS;

public class TransactionHolder extends BinderViewHolder<Transaction> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1003;

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

    public TransactionHolder(int resId, ViewGroup parent) {
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
                    ETHER_DECIMALS);
        } else {
            fill(transaction.error, operation.from, operation.to, operation.contract.symbol, operation.value,
                    operation.contract.decimals);
        }
    }

    private void fill(
            String error,
            String from,
            String to,
            String symbol,
            String valueStr,
            long decimals) {
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
            valueStr = (isSent ? "-" : "+") + getScaledValue(valueStr, decimals) + " " + symbol;
        }

        this.value.setText(valueStr);
    }

    private String getScaledValue(String valueStr, long decimals) {
        // Perform decimal conversion
        BigDecimal value = new BigDecimal(valueStr);
        value = value.divide(new BigDecimal(Math.pow(10, decimals)));
        int scale = SIGNIFICANT_FIGURES - value.precision() + value.scale();
        return value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
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
