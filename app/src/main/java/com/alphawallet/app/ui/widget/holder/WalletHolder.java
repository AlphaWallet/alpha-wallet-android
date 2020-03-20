package com.alphawallet.app.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.WalletActionsActivity;
import com.alphawallet.app.ui.widget.entity.WalletClickCallback;
import com.alphawallet.app.util.Blockies;
import com.alphawallet.app.util.Utils;

public class WalletHolder extends BinderViewHolder<Wallet> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1001;
    public final static String IS_DEFAULT_ADDITION = "is_default";
    public static final String IS_LAST_ITEM = "is_last";

    private final LinearLayout manageWalletLayout;
    private final ImageView manageWalletBtn;
    private final ImageView walletIcon;
    private final LinearLayout walletInfoLayout;
    private final TextView walletBalanceText;
    private final TextView walletNameText;
    private final TextView walletAddressSeparator;
    private final TextView walletAddressText;
    private final ImageView walletSelectedIcon;

    private final WalletClickCallback clickCallback;
    private Wallet wallet;
    private String currencySymbol;

    public WalletHolder(int resId, ViewGroup parent, WalletClickCallback callback) {
        super(resId, parent);
        manageWalletBtn = findViewById(R.id.manage_wallet_btn);
        walletIcon = findViewById(R.id.wallet_icon);
        walletBalanceText = findViewById(R.id.wallet_balance);
        walletNameText = findViewById(R.id.wallet_name);
        walletAddressSeparator = findViewById(R.id.wallet_address_separator);
        walletAddressText = findViewById(R.id.wallet_address);
        walletSelectedIcon = findViewById(R.id.selected_wallet_indicator);
        clickCallback = callback;
        walletInfoLayout = findViewById(R.id.wallet_info_layout);
        manageWalletLayout = findViewById(R.id.layout_manage_wallet);
    }

    @Override
    public void bind(@Nullable Wallet data, @NonNull Bundle addition) {
        wallet = null;
        walletAddressText.setText(null);
        if (data != null) {
            wallet = data;

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

            walletBalanceText.setText(String.format("%s %s", wallet.balance, currencySymbol));

            walletAddressText.setText(Utils.formatAddress(wallet.address));

            walletSelectedIcon.setImageResource(addition.getBoolean(IS_DEFAULT_ADDITION, false) ? R.drawable.ic_radio_on : R.drawable.ic_radio_off);

            checkLastBackUpTime();

            walletInfoLayout.setOnClickListener(this);

            manageWalletLayout.setOnClickListener(this);

            walletSelectedIcon.setOnClickListener(this);
        }

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
        switch (view.getId()) {
            case R.id.selected_wallet_indicator:
            case R.id.wallet_info_layout:
                clickCallback.onWalletClicked(wallet);
                break;

            case R.id.layout_manage_wallet:
                Intent intent = new Intent(getContext(), WalletActionsActivity.class);
                intent.putExtra("wallet", wallet);
                intent.putExtra("currency", currencySymbol);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                getContext().startActivity(intent);
                break;
        }
    }

    public void setCurrencySymbol(String symbol) {
        currencySymbol = symbol;
    }
}
