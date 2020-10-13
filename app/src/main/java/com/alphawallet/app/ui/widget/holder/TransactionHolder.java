package com.alphawallet.app.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.TokenActivity;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.widget.TokenIcon;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TransactionHolder extends BinderViewHolder<TransactionMeta> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1003;

    public static final int TRANSACTION_BALANCE_PRECISION = 4;

    public static final String DEFAULT_ADDRESS_ADDITIONAL = "default_address";

    private final TokenIcon tokenIcon;
    private final TextView date;
    private final TextView type;
    private final TextView address;
    private final TextView value;
    private final TextView chainName;
    private final TextView supplemental;
    private final TokensService tokensService;
    private final ProgressBar pendingSpinner;
    private final RelativeLayout transactionBackground;
    private final FetchTransactionsInteract transactionsInteract;
    private final AssetDefinitionService assetService;

    private Transaction transaction;
    private String defaultAddress;
    private boolean fromTokenView;

    public TransactionHolder(int resId, ViewGroup parent, TokensService service, FetchTransactionsInteract interact, AssetDefinitionService svs)
    {
        super(resId, parent);

        if (resId == R.layout.item_recent_transaction) {
            date = findViewById(R.id.transaction_date);
        } else {
            date = null;
        }
        tokenIcon = findViewById(R.id.token_icon);
        address = findViewById(R.id.address);
        type = findViewById(R.id.type);
        value = findViewById(R.id.value);
        chainName = findViewById(R.id.text_chain_name);
        supplemental = findViewById(R.id.supplimental);
        pendingSpinner = findViewById(R.id.pending_spinner);
        transactionBackground = findViewById(R.id.layout_background);
        tokensService = service;
        transactionsInteract = interact;
        assetService = svs;
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable TransactionMeta data, @NonNull Bundle addition) {
        defaultAddress = addition.getString(DEFAULT_ADDRESS_ADDITIONAL);
        supplemental.setText("");
        fromTokenView = false;

        //fetch data from database
        String hash = data.hash;
        transaction = transactionsInteract.fetchCached(defaultAddress, hash);

        if (this.transaction == null) {
            return;
        }

        value.setVisibility(View.VISIBLE);
        pendingSpinner.setVisibility(View.GONE);

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
        tokenIcon.bindData(token, assetService);
        tokenIcon.setStatusIcon(token.getTxStatus(transaction));

        String supplementalTxt = transaction.getSupplementalInfo(token.getWallet(), tokensService.getNetworkName(token.tokenInfo.chainId));
        supplemental.setText(supplementalTxt);
        supplemental.setTextColor(getSupplementalColour(supplementalTxt));

        if (date != null) setDate();

        if (transaction.error != null && transaction.error.equals("1"))
        {
            setFailed();
            tokenIcon.setStatusIcon(StatusType.FAILED);
        }

        //Handle displaying the transaction item as pending or completed
        if (transaction.blockNumber.equals("-1"))
        {
            setFailed();
            tokenIcon.setStatusIcon(StatusType.REJECTED);
            address.setText(R.string.tx_rejected);
        }
        else if (transaction.isPending())
        {
            tokenIcon.setStatusIcon(StatusType.PENDING);
            type.setText(R.string.pending_transaction);
            transactionBackground.setBackgroundResource(R.drawable.background_pending_transaction);
        }
        else if (transactionBackground != null)
        {
            transactionBackground.setBackgroundResource(R.color.white);
        }
    }

    @Override
    public void setFromTokenView()
    {
        fromTokenView = true;
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
    public void onClick(View view)
    {
        Intent intent = new Intent(getContext(), TokenActivity.class);
        intent.putExtra(C.EXTRA_TXHASH, transaction.hash);
        intent.putExtra(C.EXTRA_STATE, fromTokenView);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        getContext().startActivity(intent);
    }

    private void setFailed()
    {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) supplemental.getLayoutParams();
        layoutParams.setMarginStart(10);
        String failure = getString(R.string.failed) + " ☹";
        supplemental.setText(failure);
        supplemental.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
    }
}
