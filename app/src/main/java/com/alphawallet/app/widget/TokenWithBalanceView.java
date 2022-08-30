package com.alphawallet.app.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import timber.log.Timber;

public class TokenWithBalanceView extends LinearLayout
{
    TokenIcon tokenIcon;
    TextView tokenPrice;

    public TokenWithBalanceView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_token_with_balance_view, this);
        tokenIcon = findViewById(R.id.token_icon);
        tokenPrice = findViewById(R.id.title_set_price);
    }

    public void setTokenIconWithBalance(Token token)
    {
        tokenIcon.clearLoad();
        try
        {
            String coinBalance = token.getStringBalanceForUI(4);
            if (!TextUtils.isEmpty(coinBalance))
            {
                tokenPrice.setText(getContext().getString(R.string.valueSymbol, coinBalance, token.getTokenSymbol(token)));
            }
            tokenIcon.bindData(token.tokenInfo.chainId);
            if (!token.isEthereum())
            {
                tokenIcon.setChainIcon(token.tokenInfo.chainId); //Add in when we upgrade the design
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
    }

}

