package io.awallet.crypto.alphawallet.ui.widget.holder;

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

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.ERC875ContractTransaction;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.TransactionOperation;
import io.awallet.crypto.alphawallet.ui.widget.OnTransactionClickListener;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static io.awallet.crypto.alphawallet.C.ETHER_DECIMALS;
import static io.awallet.crypto.alphawallet.C.ETH_SYMBOL;
import static io.awallet.crypto.alphawallet.interact.SetupTokensInteract.RECEIVE_FROM_UNIVERSAL_LINK;

public class TransactionHolder extends BinderViewHolder<Transaction> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1003;

    private static final int SIGNIFICANT_FIGURES = 3;

    public static final String DEFAULT_ADDRESS_ADDITIONAL = "default_address";
    public static final String DEFAULT_SYMBOL_ADDITIONAL = "network_symbol";

    private final TextView type;
    private final TextView address;
    private final TextView value;
    private final ImageView typeIcon;
    private final TextView supplimental;

    private Transaction transaction;
    private String defaultAddress;
    private OnTransactionClickListener onTransactionClickListener;

    public TransactionHolder(int resId, ViewGroup parent) {
        super(resId, parent);

        typeIcon = findViewById(R.id.type_icon);
        address = findViewById(R.id.address);
        type = findViewById(R.id.type);
        value = findViewById(R.id.value);
        supplimental = findViewById(R.id.supplimental);

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
        supplimental.setText("");

        String networkSymbol = addition.getString(DEFAULT_SYMBOL_ADDITIONAL);
        // If operations include token transfer, display token transfer instead
        TransactionOperation operation = transaction.operations == null
                || transaction.operations.length == 0 ? null : transaction.operations[0];

        if (operation == null || operation.contract == null) {
            // default to ether transaction
            fill(transaction.error, transaction.from, transaction.to, networkSymbol, transaction.value,
                    ETHER_DECIMALS, transaction.timeStamp);
        }
        else if (operation.contract instanceof ERC875ContractTransaction)
        {
            fillERC875(transaction, (ERC875ContractTransaction)operation.contract);
        }
        else if (operation.from == null)
        {
            fill(transaction.error, transaction.from, transaction.to, networkSymbol, transaction.value,
                 ETHER_DECIMALS, transaction.timeStamp);
        }
        else {
            fill(transaction.error, operation.from, operation.to, operation.contract.symbol, operation.value,
                    operation.contract.decimals, transaction.timeStamp);
        }
    }

    private void fillERC875(Transaction trans, ERC875ContractTransaction ct)
    {
        int colourResource;
        BigInteger valueAmount = new BigInteger(transaction.value);
        supplimental.setTextColor(ContextCompat.getColor(getContext(), R.color.green));

        switch (ct.type)
        {
            case 1:
            case 2:
                supplimental.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
                typeIcon.setImageResource(R.drawable.ic_arrow_upward_black_24dp);
                colourResource = R.color.green;
                break;
            case -1:
                typeIcon.setImageResource(R.drawable.ic_arrow_downward_black_24dp);
                colourResource = R.color.red;
                break;
            case -2:
            case -3:
                //Contract creation
                typeIcon.setImageResource(R.drawable.token_icon);
                colourResource = R.color.black;
                break;
            default:
                typeIcon.setImageResource(R.drawable.ic_error_outline_black_24dp);
                colourResource = R.color.black;
                break;
        }
        type.setText(ct.operation);
        address.setText(ct.name);
        value.setTextColor(ContextCompat.getColor(getContext(), colourResource));

        if (ct.operation.equals(RECEIVE_FROM_UNIVERSAL_LINK))
        {
            String valueStr = "+" + getScaledValue(transaction.value, ETHER_DECIMALS) + " " + ETH_SYMBOL;
            value.setText(valueStr);
            valueAmount = BigInteger.ZERO;
        }
        else if (ct.indices != null && ct.indices.size() > 0) {
            String ticketMove = "x" + ct.indices.size() + " Tickets";
            value.setText(ticketMove);
        }
        else
        {
            value.setText("");
        }

        if (!trans.error.equals("0"))
        {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) supplimental.getLayoutParams();
            layoutParams.setMarginStart(10);
            supplimental.setText("Failed ☹");
            supplimental.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
            typeIcon.setImageResource(R.drawable.ic_error);
            typeIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.red),
                                    PorterDuff.Mode.SRC_ATOP);
        }
        else if (valueAmount.compareTo(BigInteger.ZERO) > 0)
        {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) supplimental.getLayoutParams();
            layoutParams.setMarginStart(30);
            String valueStr = (ct.type == 1 ? "-" : "+") + getScaledValue(transaction.value, 18) + " " + ETH_SYMBOL;
            supplimental.setText(valueStr);
            supplimental.setVisibility(View.VISIBLE);
        }
        else
        {
            supplimental.setText(""); //looks bad
            supplimental.setVisibility(View.GONE);
            typeIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.black),
                                    PorterDuff.Mode.SRC_ATOP);
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
        if (defaultAddress == null || from == null)
        {
             System.out.println("yo");
        }
        boolean isSent = from.toLowerCase().equals(defaultAddress);
        type.setText(isSent ? getString(R.string.sent) : getString(R.string.received));
        if (error == null || error.length() == 0) {
            typeIcon.setImageResource(R.drawable.ic_error_outline_black_24dp);
        } else if (!isSent) {
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
        int scale = 4; //SIGNIFICANT_FIGURES - value.precision() + value.scale();
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
