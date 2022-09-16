package com.alphawallet.app.ui.widget.adapter;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.WalletItem;
import com.alphawallet.app.ui.widget.entity.WalletClickCallback;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.TextHolder;
import com.alphawallet.app.ui.widget.holder.WalletHolder;
import com.alphawallet.app.ui.widget.holder.WalletSummaryHeaderHolder;
import com.alphawallet.app.ui.widget.holder.WalletSummaryHolder;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;

public class WalletsSummaryAdapter extends RecyclerView.Adapter<BinderViewHolder> implements WalletClickCallback, Runnable
{
    private final OnSetWalletDefaultListener onSetWalletDefaultListener;
    private final boolean mainNetActivated;
    private final ArrayList<Wallet> wallets;
    private final Map<String, Pair<Double, Double>> valueMap = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Wallet defaultWallet = null;
    private final Wallet summaryWallet = new Wallet(ZERO_ADDRESS);
    private final Context context;
    private final Realm realm;
    private final GenericWalletInteract walletInteract;

    public WalletsSummaryAdapter(Context ctx,
                                 OnSetWalletDefaultListener onSetWalletDefaultListener, GenericWalletInteract genericWalletInteract, boolean activeMainnet)
    {
        this.onSetWalletDefaultListener = onSetWalletDefaultListener;
        this.mainNetActivated = activeMainnet;
        this.wallets = new ArrayList<>();
        this.context = ctx;
        this.realm = genericWalletInteract.getWalletRealm();
        this.walletInteract = genericWalletInteract;
    }

    @NotNull
    @Override
    public BinderViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType)
    {
        BinderViewHolder binderViewHolder = null;
        switch (viewType)
        {
            case WalletHolder.VIEW_TYPE:
                binderViewHolder = new WalletSummaryHolder(R.layout.item_wallet_summary_manage, parent, this, realm);
                break;
            case TextHolder.VIEW_TYPE:
                binderViewHolder = new TextHolder(R.layout.item_standard_header, parent);
                break;
            case WalletSummaryHeaderHolder.VIEW_TYPE:
                binderViewHolder = new WalletSummaryHeaderHolder(R.layout.item_wallet_summary_large_title, parent, this, realm);
                break;
            default:
                break;
        }
        return binderViewHolder;
    }

    @Override
    public void onBindViewHolder(@NotNull BinderViewHolder holder, int position)
    {
        Bundle bundle;
        switch (getItemViewType(position))
        {
            case WalletHolder.VIEW_TYPE:
                Wallet wallet = wallets.get(position);
                bundle = new Bundle();
                bundle.putBoolean(
                        WalletHolder.IS_DEFAULT_ADDITION,
                        defaultWallet != null && defaultWallet.sameAddress(wallet.address));
                bundle.putBoolean(WalletHolder.IS_LAST_ITEM, getItemCount() == 1);
                bundle.putBoolean(WalletHolder.IS_SYNCED, wallet.isSynced);

                if (valueMap.containsKey(wallet.address.toLowerCase()))
                {
                    Pair<Double, Double> valuePair = valueMap.get(wallet.address.toLowerCase());
                    bundle.putDouble(WalletHolder.FIAT_VALUE, valuePair.first);
                    bundle.putDouble(WalletHolder.FIAT_CHANGE, valuePair.second);
                }

                bundle.putBoolean(WalletHolder.IS_MAINNET_ACTIVE, mainNetActivated);
                holder.bind(wallet, bundle);
                break;
            case TextHolder.VIEW_TYPE:
                wallet = wallets.get(position);
                holder.bind(wallet.address);
                break;
            case WalletSummaryHeaderHolder.VIEW_TYPE:
                wallet = summaryWallet;
                bundle = new Bundle();
                bundle.putBoolean(WalletHolder.IS_MAINNET_ACTIVE, mainNetActivated);
                if (mainNetActivated)
                {
                    Pair<Double, Double> totalPair = getSummaryBalance();
                    bundle.putDouble(WalletHolder.FIAT_VALUE, totalPair.first);
                    bundle.putDouble(WalletHolder.FIAT_CHANGE, totalPair.second);
                }
                holder.bind(wallet, bundle);
                break;

            default:
                break;
        }
    }

    @Override
    public int getItemCount()
    {
        return wallets.size();
    }

    @Override
    public int getItemViewType(int position)
    {
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
            case LARGE_TITLE:
                return WalletSummaryHeaderHolder.VIEW_TYPE;
        }
    }

    public void setDefaultWallet(Wallet wallet)
    {
        this.defaultWallet = wallet;
        notifyDataSetChanged();
    }

    private int getWalletIndex(String wallet)
    {
        for (int i = 0; i < wallets.size(); i++)
        {
            if (wallets.get(i).address.equalsIgnoreCase(wallet))
            {
                return i;
            }
        }

        return -1;
    }

    public void setUnsyncedWalletValue(String wallet, Pair<Double, Double> value)
    {
        int index = getWalletIndex(wallet);
        if (index >= 0)
        {
            if (value != null)
            {
                valueMap.put(wallet.toLowerCase(), value);
            }
            else
            {
                wallets.get(index).isSynced = false;
            }
            notifyItemChanged(index);
            updateWalletSummary();
        }
    }

    private Pair<Double, Double> getSummaryBalance()
    {
        double totalValue = 0.0;
        double totalOldValue = 0.0;
        for (Pair<Double, Double> value : valueMap.values())
        {
            totalValue += value.first;
            totalOldValue += value.second;
        }

        return new Pair<>(totalValue, totalOldValue);
    }

    private void updateWalletSummary()
    {
        handler.postDelayed(this, 1000); //updates can be bunched together
    }

    @Override
    public void run()
    {
        notifyItemChanged(1);
    }

    public void completeWalletSync(String walletAddress, Pair<Double, Double> value)
    {
        int index = getWalletIndex(walletAddress);
        if (index >= 0)
        {
            wallets.get(index).isSynced = true;
            updateWalletState(walletAddress, value);
        }
    }

    public void updateWalletState(String walletAddress, Pair<Double, Double> value)
    {
        int index = getWalletIndex(walletAddress);
        if (index >= 0)
        {
            valueMap.put(walletAddress.toLowerCase(), value);
            notifyItemChanged(index);
            updateWalletSummary();
        }
    }

    public void setWallets(Wallet[] wallets)
    {
        this.wallets.clear();
        boolean hasLegacyWallet = false;
        boolean hasWatchWallet = false;
        if (wallets != null)
        {
            Wallet summaryItem = new Wallet(context.getString(R.string.summary));
            summaryItem.type = WalletType.TEXT_MARKER;
            this.wallets.add(summaryItem);

            Wallet largeTitle = new Wallet(context.getString(R.string.summary));
            largeTitle.type = WalletType.LARGE_TITLE;
            this.wallets.add(largeTitle); //index 1

            Wallet yourWallets = new Wallet(context.getString(R.string.your_wallets));
            yourWallets.type = WalletType.TEXT_MARKER;
            this.wallets.add(yourWallets);

            //Add HD Wallets
            for (Wallet w : wallets)
            {
                switch (w.type)
                {
                    case KEYSTORE_LEGACY:
                    case KEYSTORE:
                        hasLegacyWallet = true;
                        w.isSynced = false;
                        break;
                    case HDKEY:
                        this.wallets.add(w);
                        w.isSynced = false;
                        break;
                    case WATCH:
                        hasWatchWallet = true;
                        w.isSynced = true;
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

    @Override
    public void onWalletClicked(Wallet wallet)
    {
        onSetWalletDefaultListener.onSetDefault(wallet);
    }

    @Override
    public void ensAvatar(Wallet wallet)
    {
        //update the ENS avatar in the database
        walletInteract.updateWalletItem(wallet, WalletItem.ENS_AVATAR, () -> {});
    }

    public void onDestroy()
    {
        realm.close();
    }

    public int getDefaultWalletIndex()
    {
        if (defaultWallet != null)
        {
            return getWalletIndex(defaultWallet.address);
        }
        return -1;
    }

    public interface OnSetWalletDefaultListener
    {
        void onSetDefault(Wallet wallet);
    }

    public void setTokens(Map<String, Token[]> walletTokens)
    {
        if (walletTokens == null) return;

        for (Token[] token : walletTokens.values())
        {
            Token[] t = walletTokens.get(token[0].getAddress());
            String walletAddress = token[0].getAddress();
            int walletIndex = getWalletIndex(walletAddress);
            if (walletIndex != -1)
            {
                this.wallets.get(walletIndex).tokens = t;
                notifyItemChanged(walletIndex);
            }
        }
    }
}
