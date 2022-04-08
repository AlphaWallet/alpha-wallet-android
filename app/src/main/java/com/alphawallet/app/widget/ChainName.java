package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkBase;

/**
 * Created by JB on 3/12/2020.
 */
public class ChainName extends LinearLayout
{
    private final TextView chainName;
    private boolean invertNameColour;

    public ChainName(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_chain_name, this);
        chainName = findViewById(R.id._text_chain_name);
        getAttrs(context, attrs);
    }

    public void setChainID(long chainId)
    {
        chainName.setText(EthereumNetworkBase.getShortChainName(chainId));
        if (invertNameColour)
        {
            chainName.setTextColor(getContext().getColor(EthereumNetworkBase.getChainColour(chainId)));
            chainName.setBackgroundResource(R.drawable.background_chain_inverse);
        }
        else
        {
            chainName.setTextColor(getContext().getColor(R.color.white));
            chainName.getBackground().setTint(ContextCompat.getColor(getContext(),
                    EthereumNetworkBase.getChainColour(chainId)));
        }
    }

    private void getAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.InputView,
                0, 0
        );

        try
        {
            int fontSize = a.getInteger(R.styleable.InputView_font_size, 12);
            chainName.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            invertNameColour = a.getBoolean(R.styleable.InputView_invert, false);
        }
        finally
        {
            a.recycle();
        }
    }
}
