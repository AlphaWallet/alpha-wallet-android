package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.widget.UserAvatar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WalletAdapter extends ArrayAdapter<Wallet>
{
    public WalletAdapter(Context context, Wallet[] wallets)
    {
        super(context, 0, wallets);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        if (convertView == null)
        {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wallet, parent, false);

            convertView.setTag(new ViewHolder(convertView));
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();

        Wallet wallet = getItem(position);

        if (!TextUtils.isEmpty(wallet.ENSname))
        {
            holder.walletName.setText(wallet.ENSname);
        } else {
            holder.walletName.setVisibility(View.GONE);
            holder.walletAddressSeparator.setVisibility(View.GONE);
        }
        holder.walletAddress.setText(wallet.address);
        holder.userAvatar.bind(wallet);
        holder.balance.setText(String.format("%s %s", wallet.balance, wallet.balanceSymbol));
        return convertView;
    }

    static class ViewHolder {
        TextView walletName;
        TextView walletAddress;
        TextView balance;
        TextView walletAddressSeparator;
        UserAvatar userAvatar;

        public ViewHolder(@NonNull View view)
        {
            walletName = view.findViewById(R.id.wallet_name);
            walletAddress = view.findViewById(R.id.wallet_address);
            balance = view.findViewById(R.id.wallet_balance);
            walletAddressSeparator = view.findViewById(R.id.wallet_address_separator);
            userAvatar = view.findViewById(R.id.wallet_icon);
        }
    }
}
