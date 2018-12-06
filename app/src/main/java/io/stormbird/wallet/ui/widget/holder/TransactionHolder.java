package io.stormbird.wallet.ui.widget.holder;

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

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.FetchTransactionsInteract;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.ui.widget.OnTransactionClickListener;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static io.stormbird.wallet.C.ETHER_DECIMALS;
import static io.stormbird.wallet.C.ETH_SYMBOL;

public class TransactionHolder extends BinderViewHolder<TransactionMeta> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1003;

    private static final int SIGNIFICANT_FIGURES = 3;

    public static final String DEFAULT_ADDRESS_ADDITIONAL = "default_address";
    public static final String DEFAULT_SYMBOL_ADDITIONAL = "network_symbol";

    private final TextView type;
    private final TextView address;
    private final TextView value;
    private final ImageView typeIcon;
    private final TextView supplimental;
    private final TokensService tokensService;
    private final FetchTransactionsInteract transactionsInteract;

    private Transaction transaction;
    private String defaultAddress;
    private OnTransactionClickListener onTransactionClickListener;

    public TransactionHolder(int resId, ViewGroup parent, TokensService service, FetchTransactionsInteract interact) {
        super(resId, parent);

        typeIcon = findViewById(R.id.type_icon);
        address = findViewById(R.id.address);
        type = findViewById(R.id.type);
        value = findViewById(R.id.value);
        supplimental = findViewById(R.id.supplimental);
        tokensService = service;
        transactionsInteract = interact;

        typeIcon.setColorFilter(
                ContextCompat.getColor(getContext(), R.color.item_icon_tint),
                PorterDuff.Mode.SRC_ATOP);

        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable TransactionMeta data, @NonNull Bundle addition) {
        defaultAddress = addition.getString(DEFAULT_ADDRESS_ADDITIONAL);
        supplimental.setText("");

        //fetch data from database
        String hash = data.hash;
        transaction = transactionsInteract.fetchCached(tokensService.getCurrentAddress(), hash);

        if (data.hash.startsWith("0x942b"))
        {
            System.out.println("yoless");
        }

        if (this.transaction == null) {
            return;
        }

        String networkSymbol = addition.getString(DEFAULT_SYMBOL_ADDITIONAL);
        boolean txSuccess = (transaction.error != null && transaction.error.equals("0"));
        // If operations include token transfer, display token transfer instead
        TransactionOperation operation = transaction.operations == null
                || transaction.operations.length == 0 ? null : transaction.operations[0];

        if (operation == null || operation.contract == null) {
            // default to ether transaction
            fill(txSuccess, transaction.from, transaction.to, networkSymbol, transaction.value,
                    ETHER_DECIMALS, transaction.timeStamp);
        }
        else if (operation.contract instanceof ERC875ContractTransaction)
        {
            fillERC875(txSuccess, transaction, (ERC875ContractTransaction)operation.contract);
        }
        else if (operation.from == null)
        {
            fill(txSuccess, transaction.from, transaction.to, networkSymbol, transaction.value,
                 ETHER_DECIMALS, transaction.timeStamp);
        }
        else
        {
            fillERC20(txSuccess, transaction);
        }
    }

    private void fillERC875(boolean txSuccess, Transaction trans, ERC875ContractTransaction ct)
    {
        int colourResource;
        supplimental.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
        String name = tokensService.getTokenName(ct.address);
        Token token = tokensService.getToken(ct.address);

        address.setText(name);
        supplimental.setTextSize(12.0f);

        String ticketMove = "";

        if (token != null)
        {
            ticketMove = token.getTransactionValue(trans, getContext());
        }
        else if (ct.indices != null && ct.indices.size() > 0)
        {
            ticketMove = "x" + ct.indices.size() + " " + getString(R.string.tickets);
        }

        String supplimentalTxt = "";

        switch (ct.operation)
        {
            case MAGICLINK_TRANSFER: //transferred out of our wallet via magic link (0 value)
                break;
            case MAGICLINK_PICKUP: //received ticket from a magic link
                break;
            case TRANSFER_TO:
                break;
            case TRANSFER_FROM:
                break;
            case MAGICLINK_SALE: //we received ether from magiclink sale
                supplimentalTxt = "+" + Token.getScaledValue(transaction.value, ETHER_DECIMALS) + " " + ETH_SYMBOL;
                break;
            case MAGICLINK_PURCHASE: //we purchased a ticket from a magiclink
                supplimentalTxt = "-" + Token.getScaledValue(transaction.value, ETHER_DECIMALS) + " " + ETH_SYMBOL;
                break;
            case RECEIVE_FROM:
                supplimentalTxt = "";//"+" + getScaledValue(transaction.value, ETHER_DECIMALS) + " " + ETH_SYMBOL;
                break;
            case LOAD_NEW_TOKENS:
                //ticketMove = "x" + operation.value + " " + getString(R.string.tickets);
                break;
            case PASS_TO:
                break;
            default:
                break;
        }

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

        String operationName = getString(TransactionLookup.typeToName(ct.operation));

        type.setText(operationName);
        value.setTextColor(ContextCompat.getColor(getContext(), colourResource));
        value.setText(ticketMove);

        setSuccessIndicator(txSuccess, supplimentalTxt);
    }

    private void fill(
            boolean txSuccess,
            String from,
            String to,
            String symbol,
            String valueStr,
            long decimals,
            long timestamp) {
        boolean isSent = from.toLowerCase().equals(defaultAddress);
        type.setText(isSent ? getString(R.string.sent) : getString(R.string.received));

        if (txSuccess)
        {
            if (!isSent)
            {
                typeIcon.setImageResource(R.drawable.ic_arrow_upward_black_24dp);
            }
            else
            {
                typeIcon.setImageResource(R.drawable.ic_arrow_downward_black_24dp);
            }
        }

        address.setText(isSent ? to : from);
        value.setTextColor(ContextCompat.getColor(getContext(), isSent ? R.color.red : R.color.green));

        setSuccessIndicator(txSuccess, "");

        if (valueStr.equals("0")) {
            valueStr = "0 " + symbol;
        } else {
            valueStr = (isSent ? "-" : "+") + Token.getScaledValue(valueStr, decimals) + " " + symbol;
        }

        this.value.setText(valueStr);
    }

    private void fillERC20(boolean txSuccess,
            Transaction transaction)
    {
        TransactionOperation operation = transaction.operations[0];

        String name = tokensService.getTokenName(operation.contract.address);
        String symbol = tokensService.getTokenSymbol(operation.contract.address);
        int decimals = tokensService.getTokenDecimals(operation.contract.address);
        Token token = tokensService.getToken(operation.contract.address);

        String from = operation.from;

        String supplimentalTxt = "";

        boolean isSent = from.toLowerCase().equals(defaultAddress);
        String operationName = token != null ? token.getOperationName(transaction, getContext()) : null;
        if (operationName == null) operationName = isSent ? getString(R.string.sent) : getString(R.string.received);
        type.setText(operationName);

        if (txSuccess)
        {
            if (!isSent)
            {
                typeIcon.setImageResource(R.drawable.ic_arrow_upward_black_24dp);
                supplimentalTxt = getString(R.string.label_from) + " " + operation.from;
            }
            else
            {
                typeIcon.setImageResource(R.drawable.ic_arrow_downward_black_24dp);
                supplimentalTxt = getString(R.string.label_to) + " " + operation.to;
            }
        }

        address.setText(name);
        value.setTextColor(ContextCompat.getColor(getContext(), isSent ? R.color.red : R.color.green));

        if (supplimentalTxt.length() > 0)
        {
            supplimental.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
            supplimental.setText(supplimentalTxt);
            supplimental.setTextSize(10.0f);
        }

        setSuccessIndicator(txSuccess, supplimentalTxt);

        String valueStr;
        if (token != null)
        {
            valueStr = token.getTransactionValue(transaction, getContext());
        }
        else
        {
            valueStr = operation.value;

            if (valueStr.equals("0") || !Character.isDigit(valueStr.charAt(0))) {
                valueStr = valueStr + " " + symbol;
            } else {
                valueStr = (isSent ? "-" : "+") + Token.getScaledValue(valueStr, decimals) + " " + symbol;
            }
        }

        this.value.setText(valueStr);
    }

    @Override
    public void onClick(View view) {
        if (onTransactionClickListener != null) {
            onTransactionClickListener.onTransactionClick(view, transaction);
        }
    }

    private void setSuccessIndicator(boolean txSuccess, String supplimentalTxt)
    {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) supplimental.getLayoutParams();
        if (txSuccess)
        {
            layoutParams.setMarginStart(30);
            supplimental.setText(supplimentalTxt);
            supplimental.setVisibility(View.VISIBLE);
            supplimental.setTextColor(ContextCompat.getColor(getContext(), R.color.green));

            typeIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.black),
                                    PorterDuff.Mode.SRC_ATOP);
        }
        else
        {
            layoutParams.setMarginStart(10);
            String failure = getString(R.string.failed) + " ☹";
            supplimental.setText(failure);
            supplimental.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
            typeIcon.setImageResource(R.drawable.ic_error);
            typeIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.red),
                                    PorterDuff.Mode.SRC_ATOP);
            value.setText("");
        }
    }

    public void setOnTransactionClickListener(OnTransactionClickListener onTransactionClickListener) {
        this.onTransactionClickListener = onTransactionClickListener;
    }
}
