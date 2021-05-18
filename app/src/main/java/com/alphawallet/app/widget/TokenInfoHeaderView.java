package com.alphawallet.app.widget;

import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkBase;

public class TokenInfoHeaderView extends LinearLayout {
    private final ImageView icon;
    private final TextView amount;
    private final TextView symbol;
    private final TextView marketValue;
    private final TextView priceChange;

    private Context context;
    private Token token;

    public TokenInfoHeaderView(Context context)
    {
        super(context);
        inflate(context, R.layout.item_token_info_header, this);
        this.context = context;
        icon = findViewById(R.id.token_icon);
        amount = findViewById(R.id.token_amount);
        symbol = findViewById(R.id.token_symbol);
        marketValue = findViewById(R.id.market_value);
        priceChange = findViewById(R.id.price_change);
    }

    public TokenInfoHeaderView(Context context, Token token)
    {
        this(context);
        this.token = token;
        setIcon(EthereumNetworkBase.getChainLogo(token.tokenInfo.chainId));
        setAmount(token.getFixedFormattedBalance());
        setSymbol(token.tokenInfo.symbol);
        // TODO: setMarketValue();
        // TODO: setPriceChange();
    }

    public void setIcon(int resId)
    {
        icon.setImageResource(resId);
    }

    public void setAmount(String text)
    {
        amount.setText(text);
    }

    public void setSymbol(String text)
    {
        symbol.setText(text);
    }

    public void setMarketValue(String text)
    {
        String formattedValue = String.format("$%s", text);
        marketValue.setText(formattedValue);
    }

    /**
     *
     * Automatically formats the string based on the passed value
     *
     * **/
    public void setPriceChange(String text)
    {
        String formattedValue = String.format("(+%s%%)", text);
        double value = Double.parseDouble(text);
        if (value > 0)
        {
            priceChange.setTextColor(context.getColor(R.color.green));
        }
        else if (value < 0)
        {
            priceChange.setTextColor(context.getColor(R.color.danger));
            formattedValue = String.format("(%s%%)", text);
        }
        else
        {
            priceChange.setTextColor(context.getColor(R.color.black));
        }

        priceChange.setText(formattedValue);
    }
}
