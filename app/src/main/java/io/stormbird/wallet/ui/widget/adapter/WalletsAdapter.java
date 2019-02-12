package io.stormbird.wallet.ui.widget.adapter;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.Map;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.widget.holder.BinderViewHolder;
import io.stormbird.wallet.ui.widget.holder.WalletHolder;

public class WalletsAdapter extends RecyclerView.Adapter<BinderViewHolder>
{

	private final OnSetWalletDefaultListener onSetWalletDefaultListener;
	private final OnWalletDeleteListener onWalletDeleteListener;
	private final OnExportWalletListener onExportWalletListener;

	private Wallet[] wallets = new Wallet[0];

	private Wallet defaultWallet = null;
	private NetworkInfo network;

	public WalletsAdapter(
			OnSetWalletDefaultListener onSetWalletDefaultListener,
			OnWalletDeleteListener onWalletDeleteListener,
			OnExportWalletListener onExportWalletListener)
	{
		this.onSetWalletDefaultListener = onSetWalletDefaultListener;
		this.onWalletDeleteListener = onWalletDeleteListener;
		this.onExportWalletListener = onExportWalletListener;
	}

	public void setNetwork(NetworkInfo network)
	{
		this.network = network;
	}

	@Override
	public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		BinderViewHolder binderViewHolder = null;
		switch (viewType)
		{
			case WalletHolder.VIEW_TYPE:
			{
				WalletHolder h = new WalletHolder(R.layout.item_wallet_manage, parent);
				h.setOnSetWalletDefaultListener(onSetWalletDefaultListener);
				h.setOnWalletDeleteListener(onWalletDeleteListener);
				h.setOnExportWalletListener(onExportWalletListener);
				h.setCurrencySymbol(network.symbol);
				binderViewHolder = h;
			}
		}
		return binderViewHolder;
	}

	@Override
	public void onBindViewHolder(BinderViewHolder holder, int position)
	{
		switch (getItemViewType(position))
		{
			case WalletHolder.VIEW_TYPE:
			{
				Wallet wallet = wallets[position];
				Bundle bundle = new Bundle();
				bundle.putBoolean(
						WalletHolder.IS_DEFAULT_ADDITION,
						defaultWallet != null && defaultWallet.sameAddress(wallet.address));
				bundle.putBoolean(WalletHolder.IS_LAST_ITEM, getItemCount() == 1);
				holder.bind(wallet, bundle);
			}
			break;
		}
	}

	@Override
	public int getItemCount()
	{
		return wallets.length;
	}

	@Override
	public int getItemViewType(int position)
	{
		return WalletHolder.VIEW_TYPE;
	}

	public void setDefaultWallet(Wallet wallet)
	{
		this.defaultWallet = wallet;
		notifyDataSetChanged();
	}

	public void setWallets(Wallet[] wallets)
	{
		this.wallets = wallets == null ? new Wallet[0] : wallets;
		notifyDataSetChanged();
	}

	public Wallet getDefaultWallet()
	{
		return defaultWallet;
	}

	public void updateWalletBalances(Map<String, Wallet> balances)
	{
		for (Wallet wallet : wallets)
		{
			if (balances.containsKey(wallet.address))
			{
				wallet.balance = balances.get(wallet.address).balance;
				wallet.ENSname = balances.get(wallet.address).ENSname;
			}
		}
		notifyDataSetChanged();
	}

    public void updateWalletNames(Map<String, String> namedWallets)
	{
		for (Wallet localWallet : wallets)
		{
			if (namedWallets.containsKey(localWallet.address))
			{
				localWallet.ENSname = namedWallets.get(localWallet.address);
				namedWallets.remove(localWallet.address);
				if (namedWallets.size() == 0) break;
			}
		}

		notifyDataSetChanged();
	}

    public interface OnSetWalletDefaultListener
	{
		void onSetDefault(Wallet wallet);
	}

	public interface OnWalletDeleteListener
	{
		void onDelete(Wallet delete);
	}

	public interface OnExportWalletListener
	{
		void onExport(Wallet wallet);
	}
}
