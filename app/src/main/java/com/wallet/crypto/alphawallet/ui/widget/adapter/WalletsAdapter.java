package com.wallet.crypto.trustapp.ui.widget.adapter;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.ui.widget.holder.BinderViewHolder;
import com.wallet.crypto.trustapp.ui.widget.holder.WalletHolder;

public class WalletsAdapter extends RecyclerView.Adapter<BinderViewHolder> {

	private final OnSetWalletDefaultListener onSetWalletDefaultListener;
	private final OnWalletDeleteListener onWalletDeleteListener;
    private final OnExportWalletListener onExportWalletListener;

    private Wallet[] wallets = new Wallet[0];

	private Wallet defaultWallet = null;

	public WalletsAdapter(
			OnSetWalletDefaultListener onSetWalletDefaultListener,
			OnWalletDeleteListener onWalletDeleteListener,
            OnExportWalletListener onExportWalletListener) {
		this.onSetWalletDefaultListener = onSetWalletDefaultListener;
		this.onWalletDeleteListener = onWalletDeleteListener;
		this.onExportWalletListener = onExportWalletListener;
	}

	@Override
	public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		BinderViewHolder binderViewHolder = null;
		switch (viewType) {
			case WalletHolder.VIEW_TYPE: {
				WalletHolder h = new WalletHolder(R.layout.item_wallet_manage, parent);
				h.setOnSetWalletDefaultListener(onSetWalletDefaultListener);
				h.setOnWalletDeleteListener(onWalletDeleteListener);
				h.setOnExportWalletListener(onExportWalletListener);
				binderViewHolder = h;
			}
		}
		return binderViewHolder;
	}

	@Override
	public void onBindViewHolder(BinderViewHolder holder, int position) {
		switch (getItemViewType(position)) {
			case WalletHolder.VIEW_TYPE:{
				Wallet wallet = wallets[position];
				Bundle bundle = new Bundle();
				bundle.putBoolean(
						WalletHolder.IS_DEFAULT_ADDITION,
						defaultWallet != null && defaultWallet.sameAddress(wallet.address));
				bundle.putBoolean(WalletHolder.IS_LAST_ITEM, getItemCount() == 1);
				holder.bind(wallet, bundle);
			} break;
		}
	}

	@Override
	public int getItemCount() {
		return wallets.length;
	}

	@Override
	public int getItemViewType(int position) {
		return WalletHolder.VIEW_TYPE;
	}

	public void setDefaultWallet(Wallet wallet) {
		this.defaultWallet = wallet;
		notifyDataSetChanged();
	}

	public void setWallets(Wallet[] wallets) {
		this.wallets = wallets == null ? new Wallet[0] : wallets;
		notifyDataSetChanged();
	}

    public Wallet getDefaultWallet() {
        return defaultWallet;
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
