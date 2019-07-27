package io.stormbird.wallet.ui.widget.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Map;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.WalletType;
import io.stormbird.wallet.ui.widget.entity.WalletClickCallback;
import io.stormbird.wallet.ui.widget.holder.BinderViewHolder;
import io.stormbird.wallet.ui.widget.holder.TextHolder;
import io.stormbird.wallet.ui.widget.holder.WalletHolder;
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
                    if (w.type == WalletType.KEYSTORE)
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

    public void updateWalletNames(Map<String, String> namedWallets) {
        for (Wallet localWallet : wallets) {
            if (namedWallets.containsKey(localWallet.address)) {
                localWallet.ENSname = namedWallets.get(localWallet.address);
                namedWallets.remove(localWallet.address);
                if (namedWallets.size() == 0) break;
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void onWalletClicked(Wallet wallet)
    {
        onSetWalletDefaultListener.onSetDefault(wallet);
    }

    public interface OnSetWalletDefaultListener {
        void onSetDefault(Wallet wallet);
    }
}
