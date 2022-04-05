package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkBase;

public class SwitchChainWidget extends LinearLayout
{
    private final ImageView oldChainLogo;
    private final ImageView newChainLogo;
    private final TextView oldChainName;
    private final TextView newChainName;
    private final TextView textMessage;

    public SwitchChainWidget(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        inflate(context, R.layout.item_switch_chain, this);
        oldChainLogo = findViewById(R.id.logo_old);
        newChainLogo = findViewById(R.id.logo_new);
        oldChainName = findViewById(R.id.name_old_chain);
        newChainName = findViewById(R.id.name_new_chain);
        textMessage = findViewById(R.id.text_message);
    }

    public void setupSwitchChainData(NetworkInfo oldNetwork, NetworkInfo newNetwork) {

        String message = getContext().getString(R.string.request_change_chain, newNetwork.name, String.valueOf(newNetwork.chainId));
        if (newNetwork.hasRealValue() && !oldNetwork.hasRealValue())
        {
            message += "\n" + getContext().getString(R.string.warning_switch_to_main);
        }
        else if (!newNetwork.hasRealValue() && oldNetwork.hasRealValue())
        {
            message += "\n" + getContext().getString(R.string.warning_switching_to_test);
        }

        oldChainLogo.setImageResource(EthereumNetworkBase.getChainLogo(oldNetwork.chainId));
        newChainLogo.setImageResource(EthereumNetworkBase.getChainLogo(newNetwork.chainId));
        oldChainName.setText(EthereumNetworkBase.getShortChainName(oldNetwork.chainId));
        newChainName.setText(EthereumNetworkBase.getShortChainName(newNetwork.chainId));
        textMessage.setText(message);
    }

}
