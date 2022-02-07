package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.WalletConnectV2Activity;
import com.alphawallet.app.widget.UserAvatar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WalletAdapter extends ArrayAdapter<Wallet>
{
    public WalletAdapter(@NonNull Context context, Wallet[] wallets)
    {
        super(context, 0, wallets);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        Wallet wallet = getItem(position);
        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_wallet, parent, false);
        }

        TextView walletName = convertView.findViewById(R.id.wallet_name);
        TextView walletAddress = convertView.findViewById(R.id.wallet_address);
        TextView balance = convertView.findViewById(R.id.wallet_balance);
        TextView walletAddressSeparator = convertView.findViewById(R.id.wallet_address_separator);
        UserAvatar userAvatar = convertView.findViewById(R.id.wallet_icon);

        if (!TextUtils.isEmpty(wallet.ENSname))
        {
            walletName.setText(wallet.ENSname);
        } else {
            walletName.setVisibility(View.GONE);
            walletAddressSeparator.setVisibility(View.GONE);
        }
        walletAddress.setText(wallet.address);
        userAvatar.bind(wallet);
        balance.setText(String.format("%s %s", wallet.balance, wallet.balanceSymbol));

        return convertView;
    }
}
