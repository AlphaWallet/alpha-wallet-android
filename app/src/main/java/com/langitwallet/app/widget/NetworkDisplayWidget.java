package com.langitwallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.langitwallet.app.R;
import com.langitwallet.app.repository.EthereumNetworkBase;

public class NetworkDisplayWidget extends LinearLayout
{
    private final TextView networkName;
    private final TokenIcon networkIcon;

    public NetworkDisplayWidget(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_network_display, this);
        networkName = findViewById(R.id.network_name);
        networkIcon = findViewById(R.id.network_icon);
    }

    public NetworkDisplayWidget(Context context, int networkId)
    {
        this(context, null);
        setNetwork(networkId);
    }

    public void setNetwork(long networkId)
    {
        networkIcon.bindData(networkId);
        if (EthereumNetworkBase.isChainSupported(networkId))
        {
            networkName.setText(EthereumNetworkBase.getShortChainName(networkId));
        }
        else
        {
            networkName.setText(getContext().getString(R.string.unsupported_network, String.valueOf(networkId)));
        }
    }
}
