package com.wallet.crypto.trustapp.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
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

    private static final String DATE_TEMPLATE = "MM/dd/yy H:mm:ss zzz";
    public static final String DEFAULT_ADDRESS_ADDITIONAL = "default_address";
    public static final String DEFAULT_SYMBOL_ADDITIONAL = "network_symbol";

    private final TextView type;
    private final TextView date;
    private final TextView address;
    private final TextView value;

    private Transaction transaction;
    private String defaultAddress;
    private OnTransactionClickListener onTransactionClickListener;

    public TransactionHolder(int resId, ViewGroup parent) {
        super(resId, parent);

        date = findViewById(R.id.date);
        address = findViewById(R.id.address);
        type = findViewById(R.id.type);
        value = findViewById(R.id.value);

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
        long timestamp = transaction.timeStamp * DateUtils.SECOND_IN_MILLIS;
        // If operations include token transfer, display token transfer instead
        TransactionOperation operation = transaction.operations == null
                || transaction.operations.length == 0 ? null : transaction.operations[0];

        if (operation == null || operation.contract == null) {
            // default to ether transaction
            fill(timestamp, transaction.from, transaction.to, networkSymbol, transaction.value,
                    ETHER_DECIMALS, false);
        } else {
            fill(timestamp, operation.from, operation.to, operation.contract.symbol, operation.value,
                    operation.contract.decimals, true);
        }
    }

    private void fill(
            long timestamp,
            String from,
            String to,
            String symbol,
            String valueStr,
            long decimals,
            boolean isTokenTransfer) {
        date.setText(DateFormat.format(DATE_TEMPLATE, timestamp));
        boolean isSent = from.toLowerCase().equals(defaultAddress);
        type.setText(isTokenTransfer ? getString(R.string.transfer, symbol)
                : isSent ? getString(R.string.sent) : getString(R.string.received));
        address.setText(isSent ? to : from);
        value.setTextColor(getContext().getColor(isSent ? R.color.red : R.color.green));

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
