package com.alphawallet.app.ui.widget.holder;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.widget.StandardHeader;

/**
 * Created by JB on 10/01/2022.
 */
public class ChainNameHeaderHolder extends BinderViewHolder<Long>
{
    public static final int VIEW_TYPE = 2023; //TODO: Move these into an enum

    private final StandardHeader chainName;

    @Override
    public void bind(@Nullable Long chainId, @NonNull Bundle addition)
    {
        if (chainId == null) chainId = MAINNET_ID;
        NetworkInfo info = EthereumNetworkBase.getNetworkInfo(chainId);
        chainName.setText(info.name);
    }

    public ChainNameHeaderHolder(int res_id, ViewGroup parent)
    {
        super(res_id, parent);
        chainName = findViewById(R.id.header_chain_name);
    }
}
