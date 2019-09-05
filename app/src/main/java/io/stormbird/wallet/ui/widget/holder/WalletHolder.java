package io.stormbird.wallet.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.WalletActionsActivity;
import io.stormbird.wallet.ui.widget.entity.WalletClickCallback;

public class WalletHolder extends BinderViewHolder<Wallet> implements View.OnClickListener {

	public static final int VIEW_TYPE = 1001;
	public final static String IS_DEFAULT_ADDITION = "is_default";
    public static final String IS_LAST_ITEM = "is_last";

    private final RelativeLayout container;
    private final RadioButton defaultAction;
	private final TextView address;
	private final TextView balance;
	private final TextView currency;
    private final WalletClickCallback clickCallback;
	private Wallet wallet;
	private String currencySymbol;
	private TextView walletName;
	private final ImageView walletSelected;
	private ImageView currentSelection;

	public WalletHolder(int resId, ViewGroup parent, WalletClickCallback callback) {
		super(resId, parent);

		container = findViewById(R.id.container);
		defaultAction = findViewById(R.id.default_action);
		address = findViewById(R.id.address);
		balance = findViewById(R.id.balance_eth);
		currency = findViewById(R.id.text_currency);
		walletName = findViewById(R.id.wallet_name);
		walletSelected = findViewById(R.id.selected_tick);
		clickCallback = callback;
		findViewById(R.id.click_layer).setOnClickListener(this);
		findViewById(R.id.btn_more).setOnClickListener(this);
	}

	@Override
	public void bind(@Nullable Wallet data, @NonNull Bundle addition) {
		wallet = null;
		address.setText(null);
		defaultAction.setEnabled(true);
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
		if (addition.getBoolean(IS_DEFAULT_ADDITION, false))
		{
			container.setElevation(0.0f);
		}
		else
		{
			container.setElevation(5.0f);
		}

		boolean isBackedUp = wallet.lastBackupTime > 0;

		switch (wallet.type)
		{
			case KEYSTORE_LEGACY:
			case KEYSTORE:
			case HDKEY:
				switch (wallet.authLevel)
				{
					case NOT_SET:
					case TEE_NO_AUTHENTICATION:
					case STRONGBOX_NO_AUTHENTICATION:
						if (!isBackedUp)
						{
							defaultAction.setBackgroundResource(R.mipmap.ic_key_noauth);
						}
						else
						{
							defaultAction.setBackgroundResource(R.mipmap.ic_key_auth);
						}
						break;
					case TEE_AUTHENTICATION:
					case STRONGBOX_AUTHENTICATION:
						defaultAction.setBackgroundResource(R.mipmap.ic_key_fullauth);
						break;
				}
				break;
			case WATCH:
				defaultAction.setBackgroundResource(R.drawable.ic_ethereum);
				break;
			case NOT_DEFINED:
			case TEXT_MARKER:
				break;
		}

		walletSelected.setVisibility(addition.getBoolean(IS_DEFAULT_ADDITION, false) ? View.VISIBLE : View.GONE);
		//defaultAction.setChecked(addition.getBoolean(IS_DEFAULT_ADDITION, false));
		container.setSelected(addition.getBoolean(IS_DEFAULT_ADDITION, false));
		currency.setText(currencySymbol);

		if (addition.getBoolean(IS_DEFAULT_ADDITION, false))
		{
			currentSelection = walletSelected;
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
            case R.id.click_layer:
				clickCallback.onWalletClicked(wallet);
				if (currentSelection != null && currentSelection.getVisibility() == View.VISIBLE) currentSelection.setVisibility(View.GONE);
				walletSelected.setVisibility(View.VISIBLE);
				container.setElevation(0.0f);
				break;

			case R.id.btn_more:
				Intent intent = new Intent(getContext(), WalletActionsActivity.class);
				intent.putExtra("wallet", wallet);
				intent.putExtra("currency", currencySymbol);
				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				getContext().startActivity(intent);
				break;
		}
	}

    public void setCurrencySymbol(String symbol)
    {
		currencySymbol = symbol;
    }
}
