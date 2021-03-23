package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.TickerService;

import java.math.BigInteger;

import javax.annotation.Nullable;

import io.realm.Realm;

/**
 * Created by Jenny Jingjing Li on 21/03/2021
 * */

public class AmountDisplay extends LinearLayout {
    private TextView amount;

    private Token token;

    public AmountDisplay(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_amount_display, this);
        amount = findViewById(R.id.text_amount);

        token = null;
    }

    private void showAmount(String amountVal)
    {
        String displayStr = getContext().getString(R.string.total_cost, amountVal, token.getSymbol());

        //fetch ticker if required
        if (gasWidget.getValue().compareTo(BigInteger.ZERO) > 0)
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

        BigInteger networkFee = gasWidget.getGasPrice(candidateTransaction.gasPrice).multiply(gasWidget.getGasLimit());
        BigInteger balanceAfterTransaction = token.balance.toBigInteger().subtract(gasWidget.getValue());
        balanceDisplay.setNewBalanceText(token, getTransactionAmount(), networkFee, balanceAfterTransaction, isSendingTransaction());
    }
}


