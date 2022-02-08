package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.widget.UserAvatar;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WalletAdapter extends ArrayAdapter<Wallet>
{
    private Wallet defaultWallet;
    private boolean[] selected;
    private boolean readOnly;

    public WalletAdapter(Context context, Wallet[] wallets, Wallet defaultWallet)
    {
        super(context, 0, wallets);
        selected = new boolean[wallets.length];
        this.defaultWallet = defaultWallet;
    }

    public WalletAdapter(Context context, List<Wallet> wallets)
    {
        super(context, 0, wallets);
        readOnly = true;
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

        if (readOnly)
        {
            holder.checkbox.setVisibility(View.GONE);
        } else {
            if (wallet.address.equals(defaultWallet.address))
            {
                holder.checkbox.setSelected(true);
                selected[position] = true;
            }

            holder.container.setOnClickListener(v ->
            {
                holder.checkbox.setSelected(!holder.checkbox.isSelected());
                selected[position] = !selected[position];
            });

        }
        return convertView;
    }

    public List<Wallet> getSelectedWallets()
    {
        List<Wallet> selectedWallets = new ArrayList<>();
        for (int i = 0; i < getCount(); i++)
        {
            if (selected[i])
            {
                selectedWallets.add(getItem(i));
            }
        }
        return selectedWallets;
    }

    static class ViewHolder {
        View container;
        TextView walletName;
        TextView walletAddress;
        TextView balance;
        TextView walletAddressSeparator;
        UserAvatar userAvatar;
        ImageView checkbox;

        public ViewHolder(@NonNull View view)
        {
            container = view;
            walletName = view.findViewById(R.id.wallet_name);
            walletAddress = view.findViewById(R.id.wallet_address);
            balance = view.findViewById(R.id.wallet_balance);
            walletAddressSeparator = view.findViewById(R.id.wallet_address_separator);
            userAvatar = view.findViewById(R.id.wallet_icon);
            checkbox = view.findViewById(R.id.checkbox);
        }
    }
}
