package com.alphawallet.app.ui.widget.holder;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnTransactionClickListener;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.Utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TransactionHolder extends BinderViewHolder<TransactionMeta> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1003;

    public static final int TRANSACTION_BALANCE_PRECISION = 4;

    public static final String DEFAULT_ADDRESS_ADDITIONAL = "default_address";
    public static final String DEFAULT_SYMBOL_ADDITIONAL = "network_symbol";

    private final TextView date;
    private final TextView type;
    private final TextView address;
    private final TextView value;
    private final TextView chainName;
    private final ImageView typeIcon;
    private final TextView supplemental;
    private final TokensService tokensService;
    private final ProgressBar pendingSpinner;
    private final RelativeLayout transactionBackground;
    private final FetchTransactionsInteract transactionsInteract;
    private final ImageView txRejected;

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
        supplemental = findViewById(R.id.supplimental);
        pendingSpinner = findViewById(R.id.pending_spinner);
        transactionBackground = findViewById(R.id.layout_background);
        txRejected = findViewById(R.id.icon_tx_rejected);
        tokensService = service;
        transactionsInteract = interact;
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable TransactionMeta data, @NonNull Bundle addition) {
        defaultAddress = addition.getString(DEFAULT_ADDRESS_ADDITIONAL);
        supplemental.setText("");

        //fetch data from database
        String hash = data.hash;
        transaction = transactionsInteract.fetchCached(defaultAddress, hash);

        if (this.transaction == null) {
            return;
        }

        value.setVisibility(View.VISIBLE);
        pendingSpinner.setVisibility(View.GONE);
        typeIcon.setVisibility(View.VISIBLE);

        setChainElement();

        Token token = getOperationToken();
        if (token == null) return;

        String transactionOperation = token.getTransactionResultValue(transaction, TRANSACTION_BALANCE_PRECISION);
        value.setText(transactionOperation);
        value.setTextColor(getValueColour(token));

        String operationName = token.getOperationName(transaction, getContext());
        type.setText(operationName);

        //set address or contract name
        String destinationOrContract = token.getTransactionDestination(transaction);
        address.setText(destinationOrContract);

        //set colours and up/down arrow
        typeIcon.setImageResource(token.getTxImage(transaction));

        String supplementalTxt = transaction.getSupplementalInfo(token.getWallet(), tokensService.getNetworkName(token.tokenInfo.chainId));
        supplemental.setText(supplementalTxt);
        supplemental.setTextColor(getSupplementalColour(supplementalTxt));

        if (date != null) setDate();

        if (transaction.error != null && transaction.error.equals("1"))
        {
            setFailed();
        }
        else
        {
            typeIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.black),
                                    PorterDuff.Mode.SRC_ATOP);
        }

        //Handle displaying the transaction item as pending or completed
        if (transaction.blockNumber.equals("-1"))
        {
            setFailed();
            txRejected.setVisibility(View.VISIBLE);
            pendingSpinner.setVisibility(View.GONE);
            address.setText(R.string.tx_rejected);
        }
        else if (transaction.blockNumber.equals("0"))
        {
            txRejected.setVisibility(View.GONE);
            type.setText(R.string.pending_transaction);
            transactionBackground.setBackgroundResource(R.drawable.background_pending_transaction);
            pendingSpinner.setVisibility(View.VISIBLE);
            typeIcon.setVisibility(View.GONE);
        }
        else if (transactionBackground != null)
        {
            txRejected.setVisibility(View.GONE);
            pendingSpinner.setVisibility(View.GONE);
            transactionBackground.setBackgroundResource(R.color.white);
        }
    }

    private int getSupplementalColour(String supplementalTxt)
    {
        if (!TextUtils.isEmpty(supplementalTxt))
        {
            switch (supplementalTxt.charAt(0))
            {
                case '-':
                    return ContextCompat.getColor(getContext(), R.color.red);
                case '+':
                    return ContextCompat.getColor(getContext(), R.color.green);
                default:
                    break;
            }
        }

        return ContextCompat.getColor(getContext(), R.color.black);
    }

    private void setChainElement()
    {
        if (chainName != null)
        {
            Utils.setChainColour(chainName, transaction.chainId);
            String chainNameStr = tokensService.getNetworkName(transaction.chainId);
            if (!TextUtils.isEmpty(chainNameStr))
            {
                chainName.setText(chainNameStr);
                chainName.setVisibility(View.VISIBLE);
            }
            else
            {
                chainName.setVisibility(View.GONE);
            }
        }
    }

    private Token getOperationToken()
    {
        String operationAddress = transaction.getOperationTokenAddress();
        Token operationToken = tokensService.getToken(transaction.chainId, operationAddress);

        if (operationToken == null)
        {
            operationToken = tokensService.getToken(transaction.chainId, defaultAddress);
        }

        return operationToken;
    }

    private int getValueColour(Token token)
    {
        boolean isSent = token.getIsSent(transaction);
        boolean isSelf = transaction.from.equalsIgnoreCase(transaction.to);

        int colour = ContextCompat.getColor(getContext(), isSent ? R.color.red : R.color.green);
        if (isSelf) colour = ContextCompat.getColor(getContext(), R.color.warning_dark_red);

        return colour;
    }

    private void setDate()
    {
        Date txDate = LocaleUtils.getLocalDateFromTimestamp(transaction.timeStamp);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTime(txDate);
        date.setText(DateFormat.format("dd MMM yyyy", calendar));
    }

    @Override
    public void onClick(View view) {
        if (onTransactionClickListener != null) {
            onTransactionClickListener.onTransactionClick(view, transaction);
        }
    }

    private void setFailed()
    {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) supplemental.getLayoutParams();
        layoutParams.setMarginStart(10);
        String failure = getString(R.string.failed) + " ☹";
        supplemental.setText(failure);
        supplemental.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
        typeIcon.setImageResource(R.drawable.ic_error);
        typeIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.red),
                                PorterDuff.Mode.SRC_ATOP);
    }

    public void setOnTransactionClickListener(OnTransactionClickListener onTransactionClickListener) {
        this.onTransactionClickListener = onTransactionClickListener;
    }
}
