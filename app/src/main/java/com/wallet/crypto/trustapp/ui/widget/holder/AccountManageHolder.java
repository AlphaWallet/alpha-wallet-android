package com.wallet.crypto.trustapp.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.ui.widget.adapter.AccountsManageAdapter;

public class AccountManageHolder extends BinderViewHolder<Account> implements View.OnClickListener {

	public static final int VIEW_TYPE = 1001;
	public final static String IS_DEFAULT_ADDITION = "is_default";

	private final RadioButton defaultAction;
	private final ImageView deleteAction;
	private final TextView address;
	private AccountsManageAdapter.OnSetAccountDefaultListener onSetAccountDefaultListener;
	private AccountsManageAdapter.OnAccountDeleteListener onAccountDeleteListener;
	private Account account;

	public AccountManageHolder(int resId, ViewGroup parent) {
		super(resId, parent);

		defaultAction = findViewById(R.id.default_action);
		deleteAction = findViewById(R.id.delete_action);
		address = findViewById(R.id.address);

		defaultAction.setOnClickListener(this);
		deleteAction.setOnClickListener(this);
	}

	@Override
	public void bind(@Nullable Account data, @NonNull Bundle addition) {
		account = null;
		address.setText(null);
		defaultAction.setEnabled(false);
		if (data == null) {
			return;
		}
		this.account = data;
		address.setText(account.address);
		defaultAction.setChecked(addition.getBoolean(IS_DEFAULT_ADDITION, false));
		defaultAction.setEnabled(true);
	}

	public void setOnSetAccountDefaultListener(AccountsManageAdapter.OnSetAccountDefaultListener onSetAccountDefaultListener) {
		this.onSetAccountDefaultListener = onSetAccountDefaultListener;
	}

	public void setOnAccountDeleteListener(AccountsManageAdapter.OnAccountDeleteListener onAccountDeleteListener) {
		this.onAccountDeleteListener = onAccountDeleteListener;
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.default_action: {
				if (onSetAccountDefaultListener != null) {
					onSetAccountDefaultListener.onSetDefault(account);
				}
			} break;
			case R.id.delete_action: {
				if (onAccountDeleteListener != null) {
					onAccountDeleteListener.onDelete(account);
				}
			}
		}
	}
}
