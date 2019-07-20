package io.stormbird.wallet.ui.widget.adapter;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.widget.holder.BinderViewHolder;
import io.stormbird.wallet.ui.widget.holder.TextHolder;
import io.stormbird.wallet.ui.widget.holder.WalletHolder;

public class WalletsAdapter extends RecyclerView.Adapter<BinderViewHolder> {

    private final OnSetWalletDefaultListener onSetWalletDefaultListener;
    private ArrayList<Wallet> wallets;
    private Wallet defaultWallet = null;
    private NetworkInfo network;

    public WalletsAdapter(
            OnSetWalletDefaultListener onSetWalletDefaultListener) {
        this.onSetWalletDefaultListener = onSetWalletDefaultListener;
        this.wallets = new ArrayList<>();
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder binderViewHolder = null;
        WalletHolder h;
        switch (viewType) {
            case WalletHolder.VIEW_TYPE:
                h = new WalletHolder(R.layout.item_wallet_manage, parent);
                h.setOnSetWalletDefaultListener(onSetWalletDefaultListener);
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
    public void onBindViewHolder(BinderViewHolder holder, int position) {
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
            case KEYSTORE:
            case HDKEY:
                return WalletHolder.VIEW_TYPE;
            case NOT_DEFINED:
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
        if (wallets != null)
        {
            int walletsRemaining = wallets.length;
            //Add HD Wallets
            for (Wallet w : wallets)
            {
                if (w.type == Wallet.WalletType.HDKEY)
                {
                    this.wallets.add(w);
                    walletsRemaining--;
                }
            }

            if (walletsRemaining > 0)
            {
                Wallet legacyText = new Wallet("Legacy Wallets");
                legacyText.type = Wallet.WalletType.NOT_DEFINED;
                this.wallets.add(legacyText);

                for (Wallet w : wallets)
                {
                    if (w.type == Wallet.WalletType.KEYSTORE)
                    {
                        this.wallets.add(w);
                    }
                }
            }

            //List<Wallet> walletList = Arrays.asList(wallets);
            //this.wallets.addAll(walletList);
        }
        notifyDataSetChanged();
    }

    public Wallet getDefaultWallet() {
        return defaultWallet;
    }

    public void updateWalletBalances(Map<String, Wallet> balances) {
        for (Wallet wallet : wallets) {
            if (balances.containsKey(wallet.address)) {
                wallet.balance = balances.get(wallet.address).balance;
                wallet.ENSname = balances.get(wallet.address).ENSname;
                wallet.name = balances.get(wallet.address).name;
            }
        }
        notifyDataSetChanged();
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

    public interface OnSetWalletDefaultListener {
        void onSetDefault(Wallet wallet);
    }

    public interface OnWalletDeleteListener {
        void onDelete(Wallet delete);
    }

    public interface OnExportWalletListener {
        void onExport(Wallet wallet);
    }
}
