package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.TransactionSuccessActivity;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.ProgressCompleteCallback;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.tools.Numeric;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

import io.realm.Realm;

import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;

/**
 * Created by JB on 17/11/2020.
 */
public class ActionSheetDialog extends BottomSheetDialog implements StandardFunctionInterface
{
    private final TextView balance;
    private final TextView newBalance;
    private final TextView amount;

    private final ImageView cancelButton;
    private final GasWidget gasWidget;
    private final ConfirmationWidget confirmationWidget;
    private final ChainName chainName;
    private final AddressDetailView addressDetail;
    private final FunctionButtonBar functionBar;

    private final Token token;
    private final TokensService tokensService;
    private final BigInteger tokenAmount;

    private final Web3Transaction candidateTransaction;
    private final ActionSheetCallback actionSheetCallback;

    private String txHash = null;

    public ActionSheetDialog(@NonNull Activity activity, Web3Transaction tx, Token t,
                             String destName, BigInteger tAmount, TokensService ts)
    {
        super(activity);
        setContentView(R.layout.dialog_action_sheet);

        balance = findViewById(R.id.text_balance);
        newBalance = findViewById(R.id.text_new_balance);
        amount = findViewById(R.id.text_amount);

        gasWidget = findViewById(R.id.gas_widgetx);
        cancelButton = findViewById(R.id.image_close);
        chainName = findViewById(R.id.chain_name);
        confirmationWidget = findViewById(R.id.confirmation_view);
        addressDetail = findViewById(R.id.recipient);
        functionBar = findViewById(R.id.layoutButtons);
        tokenAmount = tAmount;

        actionSheetCallback = (ActionSheetCallback) activity;
        if (actionSheetCallback == null)
        {
            throw new RuntimeException("Activity calling ActionSheetDialog must implement ActionSheetCallback");
        }

        token = t;
        tokensService = ts;
        candidateTransaction = tx;

        balance.setText(activity.getString(R.string.total_cost, token.getStringBalance(), token.getSymbol()));
        setNewBalanceText();
        setAmount();

        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_confirm)));
        functionBar.revealButtons();

        gasWidget.setupWidget(ts, token, candidateTransaction, activity);
        if (token.tokenInfo.chainId == MAINNET_ID)
        {
            chainName.setVisibility(View.GONE);
        }
        else
        {
            chainName.setVisibility(View.VISIBLE);
            chainName.setChainID(token.tokenInfo.chainId);
        }

        addressDetail.setupAddress(tx.recipient.toString(), destName);

        cancelButton.setOnClickListener(v -> {
           dismiss();
        });

        setOnDismissListener(v -> {
            actionSheetCallback.dismissed(txHash);
        });
    }

    public void onDestroy()
    {
        gasWidget.onDestroy();
    }

    public void setCurrentGasIndex(int gasSelectionIndex, BigDecimal customGasPrice, BigDecimal customGasLimit, long expectedTxTime, long nonce)
    {
        gasWidget.setCurrentGasIndex(gasSelectionIndex, customGasPrice, customGasLimit, expectedTxTime, nonce);
    }

    private void setNewBalanceText()
    {
        BigInteger networkFee = candidateTransaction.gasPrice.multiply(candidateTransaction.gasLimit);
        BigInteger balanceAfterTransaction = token.balance.toBigInteger().subtract(tokenAmount);
        if (token.isEthereum())
        {
            balanceAfterTransaction = balanceAfterTransaction.subtract(networkFee);
        }
        //convert to ETH amount
        String newBalanceVal = BalanceUtils.getScaledValueScientific(new BigDecimal(balanceAfterTransaction), token.tokenInfo.decimals);
        newBalance.setText(getContext().getString(R.string.new_balance, newBalanceVal, token.getSymbol()));
    }

    private void setAmount()
    {
        String amountVal = BalanceUtils.getScaledValueScientific(new BigDecimal(tokenAmount), token.tokenInfo.decimals);
        String displayStr = getContext().getString(R.string.total_cost, amountVal, token.getSymbol());

        //fetch ticker if required
        if (candidateTransaction.value.compareTo(BigInteger.ZERO) > 0)
        {
            try (Realm realm = tokensService.getTickerRealmInstance())
            {
                RealmTokenTicker rtt = realm.where(RealmTokenTicker.class)
                        .equalTo("contract", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.isEthereum() ? "eth" : token.getAddress().toLowerCase()))
                        .findFirst();

                if (rtt != null)
                {
                    //calculate equivalent fiat
                    double cryptoRate = Double.parseDouble(rtt.getPrice());
                    double cryptoAmount = Double.parseDouble(amountVal);
                    displayStr = getContext().getString(R.string.fiat_format, amountVal, token.getSymbol(),
                            TickerService.getCurrencyString(cryptoAmount * cryptoRate),
                            rtt.getCurrencySymbol()) ;
                }
            }
            catch (Exception e)
            {
                //
            }
        }

        amount.setText(displayStr);
    }

    @Override
    public void handleClick(String action, int id)
    {
        functionBar.setVisibility(View.GONE);
        //form Web3Transaction
        //get user gas settings
        final Web3Transaction finalTx = new Web3Transaction(
                candidateTransaction.recipient,
                candidateTransaction.contract,
                candidateTransaction.value,
                gasWidget.getGasPrice(),
                gasWidget.getGasLimit(),
                gasWidget.getNonce(),
                candidateTransaction.payload,
                candidateTransaction.leafPosition
        );

        //get approval and push transaction

        //authentication screen
        SignAuthenticationCallback signCallback = new SignAuthenticationCallback()
        {
            @Override
            public void gotAuthorisation(boolean gotAuth)
            {
                confirmationWidget.startProgressCycle(4);
                //send the transaction
                actionSheetCallback.sendTransaction(finalTx);
            }

            @Override
            public void cancelAuthentication()
            {
                functionBar.setVisibility(View.VISIBLE);
                confirmationWidget.hide();
                functionBar.setVisibility(View.VISIBLE);
            }
        };

        actionSheetCallback.getAuthorisation(signCallback);
    }

    public void transactionWritten(String tx)
    {
        txHash = tx;
        //dismiss on message completion
        confirmationWidget.completeProgressMessage(txHash, this::showTransactionSuccess);
        if (!TextUtils.isEmpty(tx))
        {
            updateRealmTransactionFinishEstimate(tx);
        }
    }

    public void showTransactionSuccess()
    {
        Intent intent = new Intent(getContext(), TransactionSuccessActivity.class);
        intent.putExtra(C.EXTRA_TXHASH, txHash);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        getContext().startActivity(intent);
        dismiss();
    }

    private void updateRealmTransactionFinishEstimate(String txHash)
    {
        try (Realm realm = tokensService.getWalletRealmInstance())
        {
            RealmTransaction rt = realm.where(RealmTransaction.class)
                    .equalTo("hash", txHash)
                    .findFirst();

            if (rt != null)
            {
                realm.executeTransaction(instance -> {
                    rt.setExpectedCompletion(System.currentTimeMillis() + gasWidget.getExpectedTransactionTime() * 1000);
                });
            }
        }
    }
}
