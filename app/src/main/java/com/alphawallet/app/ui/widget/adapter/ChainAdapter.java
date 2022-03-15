package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.walletconnect.util.WalletConnectHelper;
import com.alphawallet.app.widget.ChainName;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChainAdapter extends ArrayAdapter<String>
{
    public ChainAdapter(Context context, List<String> chains)
    {
        super(context, 0, chains);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        long chainId = WalletConnectHelper.getChainId(getItem(position));
        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_chain, parent, false);
        }

        ChainName chainName = convertView.findViewById(R.id.chain_name);
        ImageView chainIcon = convertView.findViewById(R.id.chain_icon);

        chainName.setChainID(chainId);
        chainIcon.setImageResource(EthereumNetworkRepository.getChainLogo(chainId));

        return convertView;
    }
}
