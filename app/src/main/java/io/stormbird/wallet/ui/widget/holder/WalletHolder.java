package io.stormbird.wallet.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.WalletActionsActivity;
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
	private final TextView currency;
    private final ImageView exportAction;
    private final LinearLayout ethLayout;
    private WalletsAdapter.OnSetWalletDefaultListener onSetWalletDefaultListener;
	private WalletsAdapter.OnWalletDeleteListener onWalletDeleteListener;
	private WalletsAdapter.OnExportWalletListener onExportWalletListener;
	private Wallet wallet;
	private String currencySymbol;
	private TextView walletName;

	private final ImageView more;

	public WalletHolder(int resId, ViewGroup parent) {
		super(resId, parent);

		container = findViewById(R.id.container);
		defaultAction = findViewById(R.id.default_action);
		deleteAction = findViewById(R.id.delete_action);
		exportAction = findViewById(R.id.export_action);
		address = findViewById(R.id.address);
		balance = findViewById(R.id.balance_eth);
		ethLayout = findViewById(R.id.layout_eth);
		currency = findViewById(R.id.text_currency);
		walletName = findViewById(R.id.wallet_name);
		more = findViewById(R.id.btn_more);

		address.setOnClickListener(this);
		defaultAction.setOnClickListener(this);
		deleteAction.setOnClickListener(this);
		exportAction.setOnClickListener(this);
		more.setOnClickListener(this);
		more.setEnabled(false);

		balance.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if (!editable.toString().isEmpty()) {
					more.setEnabled(true);
				}
			}
		});
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
		if (wallet.ENSname != null && wallet.ENSname.length() > 0)
		{
			address.setText(wallet.ENSname);
		}
		else
		{
			address.setText(wallet.address);
		}
		if (wallet.name != null && !wallet.name.isEmpty()) {
			walletName.setText(wallet.name);
		} else {
			walletName.setText("--");
		}
		balance.setText(wallet.balance);
		defaultAction.setChecked(addition.getBoolean(IS_DEFAULT_ADDITION, false));
		defaultAction.setEnabled(true);
		container.setSelected(addition.getBoolean(IS_DEFAULT_ADDITION, false));
//		deleteAction.setVisibility(
//		        addition.getBoolean(IS_DEFAULT_ADDITION, false) && !addition.getBoolean(IS_LAST_ITEM, false)
//                    ? View.GONE : View.VISIBLE);
		currency.setText(currencySymbol);
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
				break;
			}
			case R.id.delete_action: {
				if (onWalletDeleteListener != null) {
					onWalletDeleteListener.onDelete(wallet);
				}
				break;
			}
            case R.id.export_action: {
                if (onExportWalletListener != null) {
                    onExportWalletListener.onExport(wallet);
                }
				break;
            }
			case R.id.btn_more: {
				Intent intent = new Intent(getContext(), WalletActionsActivity.class);
				intent.putExtra("wallet", wallet);
				intent.putExtra("currency", currencySymbol);
				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				getContext().startActivity(intent);
				break;
			}

		}
	}

    public void setCurrencySymbol(String symbol)
    {
		currencySymbol = symbol;
    }
}
