package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.widget.adapter.WalletsAdapter;

public class WalletHolder extends BinderViewHolder<Wallet> implements View.OnClickListener {

	public static final int VIEW_TYPE = 1001;
	public final static String IS_DEFAULT_ADDITION = "is_default";
    public static final String IS_LAST_ITEM = "is_last";

    private final RelativeLayout container;
    private final RadioButton defaultAction;
	private final ImageView deleteAction;
	private final TextView address;
	private final TextView balance;
    private final ImageView exportAction;
    private final LinearLayout ethLayout;
    private WalletsAdapter.OnSetWalletDefaultListener onSetWalletDefaultListener;
	private WalletsAdapter.OnWalletDeleteListener onWalletDeleteListener;
	private WalletsAdapter.OnExportWalletListener onExportWalletListener;
	private Wallet wallet;

	public WalletHolder(int resId, ViewGroup parent) {
		super(resId, parent);

		container = findViewById(R.id.container);
		defaultAction = findViewById(R.id.default_action);
		deleteAction = findViewById(R.id.delete_action);
		exportAction = findViewById(R.id.export_action);
		address = findViewById(R.id.address);
		balance = findViewById(R.id.balance_eth);
		ethLayout = findViewById(R.id.layout_eth);

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
		if (data == null)
		{
			return;
		}
		this.wallet = data;
		address.setText(wallet.address);
		balance.setText(wallet.balance);
		defaultAction.setChecked(addition.getBoolean(IS_DEFAULT_ADDITION, false));
		defaultAction.setEnabled(true);
		container.setSelected(addition.getBoolean(IS_DEFAULT_ADDITION, false));
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
