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
import com.alphawallet.app.service.TokensService;
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
    private final ChainName chainName;
    private final TokenIcon chainIcon;

    private Token token;
    private TokensService tokenService;
    private Activity activity;

    public BalanceDisplay(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_balance_display,this);
        balance = findViewById(R.id.text_balance);
        newBalance = findViewById(R.id.text_new_balance);
        chainName = findViewById(R.id.chain_name);

        tokenService = null;
        token = null;

        chainIcon = findViewById(R.id.chain_icon);

    }

    public void setupBalance(Token t, TokensService ts ,Activity act)
    {
        token = t;
        activity = act;
        tokenService = ts;

        chainName.setChainID(token.tokenInfo.chainId);
        chainName.invertChainID(token.tokenInfo.chainId);
        chainName.setVisibility(View.VISIBLE);
        chainIcon.bindData(tokenService.getToken(token.tokenInfo.chainId, token.getAddress()), null);

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