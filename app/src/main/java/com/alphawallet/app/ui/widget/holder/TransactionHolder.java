package com.alphawallet.app.ui.widget.holder;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.entity.ERC875ContractTransaction;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionLookup;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.TransactionOperation;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.Utils;

import com.alphawallet.app.R;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnTransactionClickListener;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static com.alphawallet.app.C.*;

public class TransactionHolder extends BinderViewHolder<TransactionMeta> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1003;

    private static final int SIGNIFICANT_FIGURES = 3;

    public static final String DEFAULT_ADDRESS_ADDITIONAL = "default_address";
    public static final String DEFAULT_SYMBOL_ADDITIONAL = "network_symbol";

    private final TextView date;
    private final TextView type;
    private final TextView address;
    private final TextView value;
    private final TextView chainName;
    private final ImageView typeIcon;
    private final TextView supplimental;
    private final TokensService tokensService;
    private final ProgressBar pendingSpinner;
    private final RelativeLayout transactionBackground;
    private final FetchTransactionsInteract transactionsInteract;

    private Transaction transaction;
    private String defaultAddress;
    private OnTransactionClickListener onTransactionClickListener;

    public TransactionHolder(int resId, ViewGroup parent, TokensService service, FetchTransactionsInteract interact) {
        super(resId, parent);

        if (resId == R.layout.item_recent_transaction) {
            date = findViewById(R.id.transaction_date);
        } else {
            date = null;
        }
        typeIcon = findViewById(R.id.type_icon);
        address = findViewById(R.id.address);
        type = findViewById(R.id.type);
        value = findViewById(R.id.value);
        chainName = findViewById(R.id.text_chain_name);
        supplimental = findViewById(R.id.supplimental);
        pendingSpinner = findViewById(R.id.pending_spinner);
        transactionBackground = findViewById(R.id.layout_background);
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
        transaction = transactionsInteract.fetchCached(defaultAddress, hash);

        if (this.transaction == null) {
            return;
        }

        value.setVisibility(View.VISIBLE);
        if (pendingSpinner != null) pendingSpinner.setVisibility(View.GONE);
        typeIcon.setVisibility(View.VISIBLE);
        Token token = tokensService.getToken(transaction.chainId, defaultAddress);
        String tokenSymbol = "";
        if (token != null)
        {
            tokenSymbol = token.tokenInfo.symbol;
            if (chainName != null)
            {
                Utils.setChainColour(chainName, token.tokenInfo.chainId);
                chainName.setText(token.getNetworkName());
                chainName.setVisibility(View.VISIBLE);
            }
        }
        else if (chainName != null)
        {
            chainName.setVisibility(View.GONE);
        }

        if (date != null) setDate();

        if (transaction.blockNumber != null && transaction.blockNumber.equals("0")) { fillPending(); return; }
        if (transactionBackground != null) transactionBackground.setBackgroundResource(R.drawable.background_marketplace_event);

        boolean txSuccess = (transaction.error != null && transaction.error.equals("0"));
        // If operations include token transfer, display token transfer instead
        TransactionOperation operation = transaction.operations == null
                || transaction.operations.length == 0 ? null : transaction.operations[0];

        if (operation == null || operation.contract == null) {
            // default to ether transaction
            fill(txSuccess, transaction.from, transaction.to, tokenSymbol, transaction.value,
                    ETHER_DECIMALS, transaction.timeStamp);
        }
        else if (operation.contract instanceof ERC875ContractTransaction)
        {
            fillERC875(txSuccess, transaction, (ERC875ContractTransaction)operation.contract);
        }
        else if (operation.from == null)
        {
            fill(txSuccess, transaction.from, transaction.to, tokenSymbol, transaction.value,
                 ETHER_DECIMALS, transaction.timeStamp);
        }
        else
        {
            fillERC20(txSuccess, transaction);
        }
    }

    private void setDate()
    {
        Date txDate = LocaleUtils.getLocalDateFromTimestamp(transaction.timeStamp);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTime(txDate);
        date.setText(DateFormat.format("dd MMM yyyy", calendar));
    }

    private void fillPending()
    {
        transactionBackground.setBackgroundResource(R.drawable.background_pending_transaction);
        pendingSpinner.setVisibility(View.VISIBLE);
        typeIcon.setVisibility(View.GONE);
        type.setText(R.string.pending_transaction);
        String valueStr = getValueStr(transaction.value, true, getString(R.string.eth), 18);
        value.setText(valueStr);
        value.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
        value.setVisibility(View.VISIBLE);

        if (transaction.to.length() == 0)
        {
            address.setText(R.string.ticket_contract_constructor);
        }
        else
        {
            address.setText(transaction.to);
        }

    }

    private void fillERC875(boolean txSuccess, Transaction trans, ERC875ContractTransaction ct)
    {
        int colourResource;
        supplimental.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
        String name = tokensService.getTokenName(trans.chainId, ct.address);
        Token token = tokensService.getToken(trans.chainId, ct.address);

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
            case MAGICLINK_SALE: //we received ether from magiclink sale
                supplimentalTxt = "+" + Token.getScaledValue(transaction.value, ETHER_DECIMALS) + " " + ETH_SYMBOL;
                break;
            case MAGICLINK_PURCHASE: //we purchased a ticket from a magiclink
                supplimentalTxt = "-" + Token.getScaledValue(transaction.value, ETHER_DECIMALS) + " " + ETH_SYMBOL;
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
                typeIcon.setImageResource(R.drawable.ic_ethereum);
                colourResource = R.color.black;
                value.setVisibility(View.GONE);
                break;
            default:
                typeIcon.setImageResource(R.drawable.ic_error_outline_black_24dp);
                colourResource = R.color.black;
                break;
        }

        String operationName = getString(TransactionLookup.typeToName(ct.operation));

        type.setText(operationName);
        if (!transaction.error.equals("0"))
        {
            value.setVisibility(View.GONE);
        }
        else
        {
            value.setTextColor(ContextCompat.getColor(getContext(), colourResource));
            value.setText(ticketMove);
        }

        setSuccessIndicator(txSuccess, supplimentalTxt);
    }

    private void fill(
            boolean txSuccess,
            String from,
            String to,
            String symbol,
            String valueStr,
            long decimals,
            long timestamp)
    {
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

        valueStr = getValueStr(valueStr, isSent, symbol, decimals);

        this.value.setText(valueStr);
    }

    private String getValueStr(String valueStr, boolean isSent, String symbol, long decimals)
    {
        if (valueStr.equals("0")) {
            valueStr = "0 " + symbol;
        } else {
            valueStr = Token.getScaledValue(valueStr, decimals);
            if (!valueStr.startsWith("~"))
            {
                valueStr = (isSent ? "-" : "+") + valueStr;
            }
            valueStr = valueStr + " " + symbol;
        }

        return valueStr;
    }

    private void fillERC20(boolean txSuccess,
            Transaction transaction)
    {
        TransactionOperation operation = transaction.operations[0];

        String name = tokensService.getTokenName(transaction.chainId, operation.contract.address);
        String symbol = tokensService.getTokenSymbol(transaction.chainId, operation.contract.address);
        int decimals = tokensService.getTokenDecimals(transaction.chainId, operation.contract.address);
        Token token = tokensService.getToken(transaction.chainId, operation.contract.address);

        String from = operation.from;

        String supplimentalTxt = "";

        boolean isSent = from.toLowerCase().equals(defaultAddress.toLowerCase());
        String operationName = token != null ? token.getOperationName(transaction, getContext()) : null;
        if (operationName == null) operationName = isSent ? getString(R.string.sent) : getString(R.string.received);
        type.setText(operationName);

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

        address.setText(name);
        value.setTextColor(ContextCompat.getColor(getContext(), isSent ? R.color.red : R.color.green));

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
