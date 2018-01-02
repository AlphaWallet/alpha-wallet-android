package com.wallet.crypto.trustapp.ui.widget.adapter;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.ui.widget.holder.WalletManageHolder;
import com.wallet.crypto.trustapp.ui.widget.holder.BinderViewHolder;

public class WalletsManageAdapter extends RecyclerView.Adapter<BinderViewHolder> {

	private final OnSetWalletDefaultListener onSetWalletDefaultListener;
	private final OnWalletDeleteListener onWalletDeleteListener;
    private final OnExportWalletListener onExportWalletListener;

    private Wallet[] wallets = new Wallet[0];

	private Wallet defaultWallet = null;

	public WalletsManageAdapter(
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
			case WalletManageHolder.VIEW_TYPE: {
				WalletManageHolder h = new WalletManageHolder(R.layout.item_wallet_manage, parent);
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
			case WalletManageHolder.VIEW_TYPE:{
				Wallet wallet = wallets[position];
				Bundle bundle = new Bundle();
				bundle.putBoolean(
						WalletManageHolder.IS_DEFAULT_ADDITION,
						defaultWallet != null && defaultWallet.sameAddress(wallet.address));
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
		return WalletManageHolder.VIEW_TYPE;
	}

	public void setDefaultWallet(Wallet wallet) {
		this.defaultWallet = wallet;
		notifyDataSetChanged();
	}

	public void setWallets(Wallet[] wallets) {
		this.wallets = wallets == null ? new Wallet[0] : wallets;
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
