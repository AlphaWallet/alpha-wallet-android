package com.wallet.crypto.trustapp.ui.widget.adapter;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.ui.widget.holder.AccountManageHolder;
import com.wallet.crypto.trustapp.ui.widget.holder.BinderViewHolder;

public class AccountsManageAdapter extends RecyclerView.Adapter<BinderViewHolder> {

	private final OnSetAccountDefaultListener onSetAccountDefaultListener;
	private final OnAccountDeleteListener onAccountDeleteListener;

	private Account[] accounts = new Account[0];

	private Account defaultAccount = null;

	public AccountsManageAdapter(
			OnSetAccountDefaultListener onSetAccountDefaultListener,
			OnAccountDeleteListener onAccountDeleteListener) {
		this.onSetAccountDefaultListener = onSetAccountDefaultListener;
		this.onAccountDeleteListener = onAccountDeleteListener;
	}

	@Override
	public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		BinderViewHolder binderViewHolder = null;
		switch (viewType) {
			case AccountManageHolder.VIEW_TYPE: {
				AccountManageHolder h = new AccountManageHolder(R.layout.item_account_manage, parent);
				h.setOnSetAccountDefaultListener(onSetAccountDefaultListener);
				h.setOnAccountDeleteListener(onAccountDeleteListener);
				binderViewHolder = h;
			}
		}
		return binderViewHolder;
	}

	@Override
	public void onBindViewHolder(BinderViewHolder holder, int position) {
		switch (getItemViewType(position)) {
			case AccountManageHolder.VIEW_TYPE:{
				Account account = accounts[position];
				Bundle bundle = new Bundle();
				bundle.putBoolean(
						AccountManageHolder.IS_DEFAULT_ADDITION,
						defaultAccount != null && defaultAccount.sameAddress(account.address));
				holder.bind(account, bundle);
			} break;
		}
	}

	@Override
	public int getItemCount() {
		return accounts.length;
	}

	@Override
	public int getItemViewType(int position) {
		return AccountManageHolder.VIEW_TYPE;
	}

	public void setDefaultAccount(Account account) {
		this.defaultAccount = account;
		notifyDataSetChanged();
	}

	public void setAccounts(Account[] accounts) {
		this.accounts = accounts == null ? new Account[0] : accounts;
		notifyDataSetChanged();
	}

	public interface OnSetAccountDefaultListener {
		void onSetDefault(Account account);
	}

	public interface OnAccountDeleteListener {


		void onDelete(Account delete);
	}
}
