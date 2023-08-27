package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.walletconnect.util.WalletConnectHelper;

import java.util.List;

public class ChainAdapter extends ArrayAdapter<String>
{
    public ChainAdapter(Context context, List<String> chains)
    {
        super(context, 0, chains);
    }

    @Override
    public boolean isEnabled(int position)
    {
        return false;
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

        TextView chainName = convertView.findViewById(R.id.chain_name);
        ImageView chainIcon = convertView.findViewById(R.id.chain_icon);

        NetworkInfo info = EthereumNetworkBase.getNetworkInfo(chainId);

        if (info != null)
        {
            chainName.setText(EthereumNetworkBase.getNetworkInfo(chainId).name);
            chainIcon.setImageResource(EthereumNetworkRepository.getChainLogo(chainId));
        }
        else
        {
            chainName.setText("Unhandled Chain");
            chainIcon.setImageResource(EthereumNetworkRepository.getChainLogo(R.drawable.ic_goerli));
        }

        return convertView;
    }
}
