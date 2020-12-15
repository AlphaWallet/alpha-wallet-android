package com.alphawallet.app.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.TokenActivity;
import com.alphawallet.app.ui.widget.entity.ENSHandler;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.widget.ChainName;
import com.alphawallet.app.widget.TokenIcon;

import org.web3j.crypto.Keys;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;

public class TransactionHolder extends BinderViewHolder<TransactionMeta> implements View.OnClickListener
{
    public static final int VIEW_TYPE = 1003;

    public static final int TRANSACTION_BALANCE_PRECISION = 4;

    public static final String DEFAULT_ADDRESS_ADDITIONAL = "default_address";

    private final TokenIcon tokenIcon;
    private final TextView date;
    private final TextView type;
    private final TextView address;
    private final TextView value;
    private final ChainName chainName;
    private final TextView supplemental;
    private final TextView symbol;
    private final TextView toOrFrom;
    private final TokensService tokensService;
    private final LinearLayout transactionBackground;
    private final FetchTransactionsInteract transactionsInteract;
    private final AssetDefinitionService assetService;

    private Transaction transaction;
    private String defaultAddress;
    private boolean fromTokenView;

    public TransactionHolder(ViewGroup parent, TokensService service, FetchTransactionsInteract interact, AssetDefinitionService svs)
    {
        super(R.layout.item_transaction, parent);
        date = findViewById(R.id.text_tx_time);
        tokenIcon = findViewById(R.id.token_icon);
        address = findViewById(R.id.address);
        type = findViewById(R.id.type);
        value = findViewById(R.id.value);
        chainName = findViewById(R.id.chain_name);
        toOrFrom = findViewById(R.id.text_to_from);
        supplemental = findViewById(R.id.supplimental);
        transactionBackground = findViewById(R.id.layout_background);
        symbol = findViewById(R.id.symbol);
        tokensService = service;
        transactionsInteract = interact;
        assetService = svs;
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable TransactionMeta data, @NonNull Bundle addition)
    {
        defaultAddress = addition.getString(DEFAULT_ADDRESS_ADDITIONAL);
        supplemental.setText("");
        fromTokenView = false;

        //fetch data from database
        transaction = transactionsInteract.fetchCached(defaultAddress, data.hash);

        if (this.transaction == null) {
            return;
        }

        value.setVisibility(View.VISIBLE);

        setChainElement();

        Token token = getOperationToken();
        if (token == null) return;

        String operationName = token.getOperationName(transaction, getContext());

        String transactionOperation = token.getTransactionResultValue(transaction, TRANSACTION_BALANCE_PRECISION);
        value.setText(Utils.isContractCall(getContext(), operationName) ? "" : transactionOperation);
        value.setTextColor(getValueColour(token));

        type.setText(operationName);
        symbol.setText(Utils.isContractCall(getContext(), operationName) ? "" : token.getShortSymbol());

        //set address or contract name
        setupAddressField(token);
        setupToFromText(token);

        //set colours and up/down arrow
        tokenIcon.bindData(token, assetService);
        tokenIcon.setStatusIcon(token.getTxStatus(transaction));

        String supplementalTxt = transaction.getSupplementalInfo(token.getWallet(), tokensService.getNetworkName(token.tokenInfo.chainId));
        supplemental.setText(supplementalTxt);
        supplemental.setTextColor(transaction.getSupplementalColour(supplementalTxt));

        date.setText(Utils.localiseUnixTime(getContext(), transaction.timeStamp));
        date.setVisibility(View.VISIBLE);

        setTransactionStatus(transaction.blockNumber, transaction.error, transaction.isPending());
    }

    private void setupToFromText(Token token)
    {
        String toFrom = token.getToFromText(getContext(), transaction);
        if (!TextUtils.isEmpty(toFrom))
        {
            toOrFrom.setText(toFrom);
            toOrFrom.setVisibility(View.VISIBLE);
        }
        else
        {
            toOrFrom.setVisibility(View.GONE);
        }
    }

    private void setupAddressField(Token token)
    {
        String destinationOrContract = token.getTransactionDestination(transaction);
        String addressStr = ENSHandler.findMatchingENS(getContext(), destinationOrContract);
        if (Utils.isAddressValid(addressStr))
        {
            Token resolveToken = tokensService.getToken(transaction.chainId, addressStr);
            if (resolveToken == null || resolveToken.isEthereum())
            {
                addressStr = Utils.formatAddress(addressStr);
            }
            else
            {
                addressStr = resolveToken.getFullName();
            }
        }

        address.setText(addressStr);
    }

    @Override
    public void setFromTokenView()
    {
        fromTokenView = true;
    }

    private void setChainElement()
    {
        if (transaction.chainId == MAINNET_ID)
        {
            chainName.setVisibility(View.GONE);
        }
        else
        {
            chainName.setVisibility(View.VISIBLE);
            chainName.setChainID(transaction.chainId);
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

    private void setTransactionStatus(String blockNumber, String error, boolean isPending)
    {
        if (error != null && error.equals("1"))
        {
            setFailed();
            tokenIcon.setStatusIcon(StatusType.FAILED);
        }

        //Handle displaying the transaction item as pending or completed
        if (blockNumber.equals("-1"))
        {
            setFailed();
            tokenIcon.setStatusIcon(StatusType.REJECTED);
            address.setText(R.string.tx_rejected);
        }
        else if (isPending)
        {
            tokenIcon.setStatusIcon(StatusType.PENDING);
            type.setText(R.string.pending_transaction);
            transactionBackground.setBackgroundResource(R.drawable.background_pending_transaction);
            long fetchTxCompletionTime = transactionsInteract.fetchTxCompletionTime(defaultAddress, transaction.hash);
            tokenIcon.startPendingSpinner(transaction.timeStamp, fetchTxCompletionTime/1000);
        }
        else if (transactionBackground != null)
        {
            transactionBackground.setBackgroundResource(R.color.white);
        }
    }
}
