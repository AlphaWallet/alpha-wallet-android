package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.util.BalanceUtils;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;

/**
 * Created by Jenny Jingjing Li on 4/3/2021
 */

public class BalanceDisplay extends LinearLayout
{
    public final TextView balance;
    public final TextView newBalance;
    public final ChainName chainName;

    private Token token;
    private Activity activity;

    public BalanceDisplay(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_balance_display,this);
        balance = findViewById(R.id.text_balance);
        newBalance = findViewById(R.id.text_new_balance);
        chainName = findViewById(R.id.chain_name);

        activity = null;
        token = null;

    }

    public void setupBalance(Token t, Activity act)
    {
        token = t;
        activity = act;


        if (token.tokenInfo.chainId == MAINNET_ID)
        {
            chainName.setVisibility(View.GONE);
        }
        else
        {
            chainName.setVisibility(View.VISIBLE);
            chainName.setChainID(token.tokenInfo.chainId);
        }
        balance.setText(activity.getString(R.string.total_cost, token.getStringBalance(), token.getSymbol()));
    }

    public void setNewBalanceText(Token token, BigDecimal transactionAmount, BigInteger networkFee, BigInteger balanceAfterTransaction, boolean isSendingTransaction)
    {
        balance.setText(activity.getString(R.string.total_cost, token.getStringBalance(), token.getSymbol()));

        if (token.isEthereum())
        {
            balanceAfterTransaction = balanceAfterTransaction.subtract(networkFee).max(BigInteger.ZERO);
        }
        else if (isSendingTransaction)
        {
            balanceAfterTransaction = token.getBalanceRaw().subtract(transactionAmount).toBigInteger();
        }
        //convert to ETH amount
        String newBalanceVal = BalanceUtils.getScaledValueScientific(new BigDecimal(balanceAfterTransaction), token.tokenInfo.decimals);
        newBalance.setText(getContext().getString(R.string.new_balance, newBalanceVal, token.getSymbol()));
    }
}