package com.alphawallet.app.widget;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
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
    private final TokenIcon tokenIcon;
    private final AddressDetailView addressDetail;
    private final FunctionButtonBar functionBar;

    private final Token token;
    private final TokensService tokensService;
    private final BigInteger tokenAmount;

    private final Web3Transaction candidateTransaction;
    private final ActionSheetCallback actionSheetCallback;

    boolean wroteTransaction;

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
        tokenIcon = findViewById(R.id.token_icon);
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
        wroteTransaction = false;

        Token baseCurrency = tokensService.getToken(t.tokenInfo.chainId, t.getWallet());

        balance.setText(activity.getString(R.string.total_cost, token.getStringBalance(), token.getSymbol()));
        setNewBalanceText();
        setAmount();

        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_confirm)));
        functionBar.revealButtons();

        gasWidget.setupWidget(ts, token, candidateTransaction, activity);
        tokenIcon.bindData(baseCurrency, null); //show chain

        addressDetail.setupAddress(tx.recipient.toString(), destName);

        cancelButton.setOnClickListener(v -> {
           dismiss();
        });

        setOnDismissListener(v ->{
            actionSheetCallback.dismissed(wroteTransaction);
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
        //Token baseCurrency = tokensService.getToken(token.tokenInfo.chainId, token.getWallet());
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
        Token baseCurrency = tokensService.getToken(token.tokenInfo.chainId, token.getWallet());
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
                confirmationWidget.showAnimate();
                //send the transaction
                actionSheetCallback.sendTransaction(finalTx);
            }

            @Override
            public void cancelAuthentication()
            {
                functionBar.setVisibility(View.VISIBLE);
                confirmationWidget.hide();
            }
        };

        actionSheetCallback.getAuthorisation(signCallback);
    }

    public void transactionWritten(String txHash)
    {
        confirmationWidget.startAnimate(gasWidget.getExpectedTransactionTime(), tokensService.getRealmInstance(new Wallet(token.getWallet())), txHash);
        wroteTransaction = true;
    }
}
