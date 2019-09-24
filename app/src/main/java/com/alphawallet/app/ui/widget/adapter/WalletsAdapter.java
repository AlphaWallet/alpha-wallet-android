package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.ui.widget.entity.WalletClickCallback;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.TextHolder;
import com.alphawallet.app.ui.widget.holder.WalletHolder;
import org.jetbrains.annotations.NotNull;

public class WalletsAdapter extends RecyclerView.Adapter<BinderViewHolder> implements WalletClickCallback
{
    private final OnSetWalletDefaultListener onSetWalletDefaultListener;
    private ArrayList<Wallet> wallets;
    private Wallet defaultWallet = null;
    private NetworkInfo network;
    private final Context context;

    public WalletsAdapter(Context ctx,
            OnSetWalletDefaultListener onSetWalletDefaultListener) {
        this.onSetWalletDefaultListener = onSetWalletDefaultListener;
        this.wallets = new ArrayList<>();
        this.context = ctx;
    }

    @NotNull
    @Override
    public BinderViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        BinderViewHolder binderViewHolder = null;
        WalletHolder h;
        switch (viewType) {
            case WalletHolder.VIEW_TYPE:
                h = new WalletHolder(R.layout.item_wallet_manage, parent, this);
                if (network != null) {
                    h.setCurrencySymbol(network.symbol);
                }
                binderViewHolder = h;
            break;
            case TextHolder.VIEW_TYPE:
                binderViewHolder = new TextHolder(R.layout.item_text_view, parent);
                break;
            default:
                break;
        }
        return binderViewHolder;
    }

    @Override
    public void onBindViewHolder(@NotNull BinderViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case WalletHolder.VIEW_TYPE:
                Wallet wallet = wallets.get(position);
                Bundle bundle = new Bundle();
                bundle.putBoolean(
                        WalletHolder.IS_DEFAULT_ADDITION,
                        defaultWallet != null && defaultWallet.sameAddress(wallet.address));
                bundle.putBoolean(WalletHolder.IS_LAST_ITEM, getItemCount() == 1);
                holder.bind(wallet, bundle);
                break;
            case TextHolder.VIEW_TYPE:
                wallet = wallets.get(position);
                holder.bind(wallet.address);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return wallets.size();
    }

    @Override
    public int getItemViewType(int position) {
        switch (wallets.get(position).type)
        {
            default:
            case WATCH:
            case KEYSTORE:
            case KEYSTORE_LEGACY:
            case HDKEY:
                return WalletHolder.VIEW_TYPE;
            case TEXT_MARKER:
                return TextHolder.VIEW_TYPE;
        }
    }

    public void setNetwork(NetworkInfo network) {
        this.network = network;
        notifyDataSetChanged();
    }

    public void setDefaultWallet(Wallet wallet) {
        this.defaultWallet = wallet;
        notifyDataSetChanged();
    }

    public void setWallets(Wallet[] wallets) {
        this.wallets.clear();
        boolean hasLegacyWallet = false;
        boolean hasWatchWallet = false;
        if (wallets != null)
        {
            //Add HD Wallets
            for (Wallet w : wallets)
            {
                switch (w.type)
                {
                    case KEYSTORE_LEGACY:
                    case KEYSTORE:
                        hasLegacyWallet = true;
                        break;
                    case HDKEY:
                        this.wallets.add(w);
                        break;
                    case WATCH:
                        hasWatchWallet = true;
                        break;
                    default:
                        break;
                }
            }

            if (hasLegacyWallet)
            {
                Wallet legacyText = new Wallet(context.getString(R.string.legacy_wallets));
                legacyText.type = WalletType.TEXT_MARKER;
                this.wallets.add(legacyText);

                for (Wallet w : wallets)
                {
                    if (w.type == WalletType.KEYSTORE || w.type == WalletType.KEYSTORE_LEGACY)
                    {
                        this.wallets.add(w);
                    }
                }
            }

            if (hasWatchWallet)
            {
                Wallet watchText = new Wallet(context.getString(R.string.watch_wallet));
                watchText.type = WalletType.TEXT_MARKER;
                this.wallets.add(watchText);

                for (Wallet w : wallets)
                {
                    if (w.type == WalletType.WATCH)
                    {
                        this.wallets.add(w);
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    public Wallet getDefaultWallet() {
        return defaultWallet;
    }
    
    public void updateWalletbalance(Wallet wallet)
    {
        boolean found = false;
        for (Wallet w : wallets)
        {
            if (w.address.equals(wallet.address))
            {
                w.name = wallet.name;
                w.ENSname = wallet.ENSname;
                w.balance = wallet.balance;
                w.lastBackupTime = wallet.lastBackupTime;
                found = true;
                break;
            }
        }

        if (!found) wallets.add(wallet);

        notifyDataSetChanged();
    }

    public void updateWalletNames(List<Wallet> updatedWallets)
    {
        Map<String, String> ensUpdates = new HashMap<>();
        for (Wallet wallet : updatedWallets) ensUpdates.put(wallet.address, wallet.ENSname);
        for (int i = 0; i < wallets.size(); i++)
        {
            wallets.get(i).ENSname = ensUpdates.get(wallets.get(i).address);
        }
    }

    @Override
    public void onWalletClicked(Wallet wallet)
    {
        onSetWalletDefaultListener.onSetDefault(wallet);
    }

    public void updateWalletName(Wallet wallet)
    {
        for (int i = 0; i < wallets.size(); i++)
        {
            if (wallet.address.equalsIgnoreCase(wallets.get(i).address))
            {
                wallets.get(i).ENSname = wallet.ENSname;
                notifyItemChanged(i);
                break;
            }
        }
    }

    public interface OnSetWalletDefaultListener {
        void onSetDefault(Wallet wallet);
    }
}
