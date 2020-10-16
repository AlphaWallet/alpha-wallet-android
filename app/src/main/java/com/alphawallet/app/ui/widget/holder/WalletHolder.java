package com.alphawallet.app.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.entity.RealmWalletData;
import com.alphawallet.app.ui.WalletActionsActivity;
import com.alphawallet.app.ui.widget.entity.WalletClickCallback;
import com.alphawallet.app.util.Blockies;
import com.alphawallet.app.util.Utils;

import io.realm.Realm;
import io.realm.RealmResults;

public class WalletHolder extends BinderViewHolder<Wallet> implements View.OnClickListener {

	public static final int VIEW_TYPE = 1001;
	public final static String IS_DEFAULT_ADDITION = "is_default";
	public static final String IS_LAST_ITEM = "is_last";

	private final LinearLayout manageWalletLayout;
	private final ImageView manageWalletBtn;
	private final ImageView walletIcon;
	private final LinearLayout walletClickLayout;
	private final TextView walletBalanceText;
	private final TextView walletBalanceCurrency;
	private final TextView walletNameText;
	private final TextView walletAddressSeparator;
	private final TextView walletAddressText;
	private final ImageView walletSelectedIcon;
	private final int greyColor;
	private final int blackColor;
	private final Realm realm;
	private RealmResults<RealmWalletData> realmUpdate;

	private final WalletClickCallback clickCallback;
	private Wallet wallet = null;

	public WalletHolder(int resId, ViewGroup parent, WalletClickCallback callback, Realm realm) {
		super(resId, parent);
		manageWalletBtn = findViewById(R.id.manage_wallet_btn);
		walletIcon = findViewById(R.id.wallet_icon);
		walletBalanceText = findViewById(R.id.wallet_balance);
		walletBalanceCurrency = findViewById(R.id.wallet_currency);
		walletNameText = findViewById(R.id.wallet_name);
		walletAddressSeparator = findViewById(R.id.wallet_address_separator);
		walletAddressText = findViewById(R.id.wallet_address);
		walletSelectedIcon = findViewById(R.id.selected_wallet_indicator);
		walletClickLayout = findViewById(R.id.wallet_click_layer);
		clickCallback = callback;
		manageWalletLayout = findViewById(R.id.layout_manage_wallet);
		greyColor = parent.getContext().getColor(R.color.greyffive);
		blackColor = parent.getContext().getColor(R.color.text_black);
		this.realm = realm;
	}

	@Override
	public void bind(@Nullable Wallet data, @NonNull Bundle addition) {
		walletAddressText.setText(null);
		if (realmUpdate != null) realmUpdate.removeAllChangeListeners();

		if (data != null) {
			wallet = fetchWallet(data);
			walletClickLayout.setOnClickListener(this);
			manageWalletLayout.setOnClickListener(this);

			manageWalletBtn.setVisibility(View.VISIBLE);

			if (wallet.name != null && !wallet.name.isEmpty()) {
				walletNameText.setText(wallet.name);
				walletAddressSeparator.setVisibility(View.VISIBLE);
				walletNameText.setVisibility(View.VISIBLE);
			} else {
				walletAddressSeparator.setVisibility(View.GONE);
				walletNameText.setVisibility(View.GONE);
			}

			if (wallet.ENSname != null && wallet.ENSname.length() > 0) {
				walletNameText.setText(wallet.ENSname);
				walletAddressSeparator.setVisibility(View.VISIBLE);
				walletNameText.setVisibility(View.VISIBLE);
			} else {
				walletAddressSeparator.setVisibility(View.GONE);
				walletNameText.setVisibility(View.GONE);
			}

			walletIcon.setImageBitmap(Blockies.createIcon(wallet.address.toLowerCase()));

			String walletBalance = wallet.balance;
			if (!TextUtils.isEmpty(walletBalance) && walletBalance.startsWith("*"))
			{
				walletBalance = walletBalance.substring(1);
				walletBalanceText.setTextColor(greyColor);
			}
			else
			{
				walletBalanceText.setTextColor(blackColor);
			}
			walletBalanceText.setText(walletBalance);
			walletBalanceCurrency.setText(wallet.balanceSymbol);

			walletAddressText.setText(Utils.formatAddress(wallet.address));

			walletSelectedIcon.setSelected(addition.getBoolean(IS_DEFAULT_ADDITION, false));

			checkLastBackUpTime();
			startRealmListener();
		}
	}

	private void startRealmListener()
	{
		realmUpdate = realm.where(RealmWalletData.class)
				.equalTo("address", wallet.address).findAllAsync();
		realmUpdate.addChangeListener(realmWallets -> {
			//update balance
			if (realmWallets.size() == 0) return;
			RealmWalletData realmWallet = realmWallets.first();
			walletBalanceText.setTextColor(blackColor);
			walletBalanceText.setText(realmWallet.getBalance());
			String ensName = realmWallet.getENSName();
			if (!TextUtils.isEmpty(ensName)) {
				walletNameText.setText(ensName);
				walletAddressSeparator.setVisibility(View.VISIBLE);
				walletNameText.setVisibility(View.VISIBLE);
			}
		});
	}

	private Wallet fetchWallet(Wallet w)
	{
		RealmWalletData realmWallet = realm.where(RealmWalletData.class)
				.equalTo("address", w.address)
				.findFirst();

		if (realmWallet != null)
		{
			w.balance = realmWallet.getBalance();
			w.ENSname = realmWallet.getENSName();
			w.name = realmWallet.getName();
		}

		return w;
	}

	private void checkLastBackUpTime() {
		boolean isBackedUp = wallet.lastBackupTime > 0;
		switch (wallet.type) {
			case KEYSTORE_LEGACY:
			case KEYSTORE:
			case HDKEY:
				switch (wallet.authLevel) {
					case NOT_SET:
					case TEE_NO_AUTHENTICATION:
					case STRONGBOX_NO_AUTHENTICATION:
						if (!isBackedUp) {
							// TODO: Display indicator
						} else {
							// TODO: Display indicator
						}
						break;
					case TEE_AUTHENTICATION:
					case STRONGBOX_AUTHENTICATION:
						// TODO: Display indicator
						break;
				}
				break;
			case WATCH:
			case NOT_DEFINED:
			case TEXT_MARKER:
				break;
		}
	}

	@Override
	public void onClick(View view) {
		//if (wallet == null) { return; } //protect against click between constructor and bind
		switch (view.getId()) {
			case R.id.wallet_click_layer:
				clickCallback.onWalletClicked(wallet);
				break;

			case R.id.layout_manage_wallet:
				Intent intent = new Intent(getContext(), WalletActionsActivity.class);
				intent.putExtra("wallet", wallet);
				intent.putExtra("currency", wallet.balanceSymbol);
				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				getContext().startActivity(intent);
				break;
		}
	}
}
