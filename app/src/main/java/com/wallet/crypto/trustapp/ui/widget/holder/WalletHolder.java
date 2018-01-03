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
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.ui.widget.adapter.WalletsAdapter;

public class WalletHolder extends BinderViewHolder<Wallet> implements View.OnClickListener {

	public static final int VIEW_TYPE = 1001;
	public final static String IS_DEFAULT_ADDITION = "is_default";
    public static final String IS_LAST_ITEM = "is_last";

    private final RadioButton defaultAction;
	private final ImageView deleteAction;
	private final TextView address;
    private final ImageView exportAction;
    private WalletsAdapter.OnSetWalletDefaultListener onSetWalletDefaultListener;
	private WalletsAdapter.OnWalletDeleteListener onWalletDeleteListener;
	private WalletsAdapter.OnExportWalletListener onExportWalletListener;
	private Wallet wallet;

	public WalletHolder(int resId, ViewGroup parent) {
		super(resId, parent);

		defaultAction = findViewById(R.id.default_action);
		deleteAction = findViewById(R.id.delete_action);
		exportAction = findViewById(R.id.export_action);
		address = findViewById(R.id.address);

		address.setOnClickListener(this);
		defaultAction.setOnClickListener(this);
		deleteAction.setOnClickListener(this);
		exportAction.setOnClickListener(this);
	}

	@Override
	public void bind(@Nullable Wallet data, @NonNull Bundle addition) {
		wallet = null;
		address.setText(null);
		defaultAction.setEnabled(false);
		if (data == null) {
			return;
		}
		this.wallet = data;
		address.setText(wallet.address);
		defaultAction.setChecked(addition.getBoolean(IS_DEFAULT_ADDITION, false));
		defaultAction.setEnabled(true);
		deleteAction.setVisibility(
		        addition.getBoolean(IS_DEFAULT_ADDITION, false) && !addition.getBoolean(IS_LAST_ITEM, false)
                    ? View.GONE : View.VISIBLE);
	}

	public void setOnSetWalletDefaultListener(WalletsAdapter.OnSetWalletDefaultListener onSetWalletDefaultListener) {
		this.onSetWalletDefaultListener = onSetWalletDefaultListener;
	}

	public void setOnWalletDeleteListener(WalletsAdapter.OnWalletDeleteListener onWalletDeleteListener) {
		this.onWalletDeleteListener = onWalletDeleteListener;
	}

    public void setOnExportWalletListener(WalletsAdapter.OnExportWalletListener onExportWalletListener) {
        this.onExportWalletListener = onExportWalletListener;
    }

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
            case R.id.address:
			case R.id.default_action: {
				if (onSetWalletDefaultListener != null) {
					onSetWalletDefaultListener.onSetDefault(wallet);
				}
			} break;
			case R.id.delete_action: {
				if (onWalletDeleteListener != null) {
					onWalletDeleteListener.onDelete(wallet);
				}
			} break;
            case R.id.export_action: {
                if (onExportWalletListener != null) {
                    onExportWalletListener.onExport(wallet);
                }
            } break;
		}
	}
}
