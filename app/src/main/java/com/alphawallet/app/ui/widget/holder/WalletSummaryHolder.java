package com.alphawallet.app.ui.widget.holder;

import static com.alphawallet.app.ui.widget.holder.WalletHolder.FIAT_CHANGE;
import static com.alphawallet.app.ui.widget.holder.WalletHolder.FIAT_VALUE;
import static com.alphawallet.app.ui.widget.holder.WalletHolder.IS_MAINNET_ACTIVE;
import static com.alphawallet.app.ui.widget.holder.WalletHolder.IS_SYNCED;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.entity.RealmWalletData;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.WalletActionsActivity;
import com.alphawallet.app.ui.widget.entity.AvatarWriteCallback;
import com.alphawallet.app.ui.widget.entity.WalletClickCallback;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.widget.TokensBalanceView;
import com.alphawallet.app.widget.UserAvatar;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.reactivex.disposables.Disposable;
import io.realm.Realm;
import io.realm.RealmResults;

public class WalletSummaryHolder extends BinderViewHolder<Wallet> implements View.OnClickListener, AvatarWriteCallback
{

    public static final int VIEW_TYPE = 1001;
    public final static String IS_DEFAULT_ADDITION = "is_default";
    public static final String IS_LAST_ITEM = "is_last";

    private final LinearLayout manageWalletLayout;
    private final ImageView defaultWalletIndicator;
    private final ImageView manageWalletBtn;
    private final UserAvatar walletIcon;
    private final RelativeLayout arrowRight;
    private final TextView walletBalanceText;
    private final TextView walletNameText;
    private final TextView walletAddressSeparator;
    private final TextView walletAddressText;
    private final TextView wallet24hChange;
    private final TokensBalanceView tokensBalanceView;
    private final Realm realm;
    private RealmResults<RealmWalletData> realmUpdate;
    private final WalletClickCallback clickCallback;
    private Wallet wallet = null;
    protected Disposable disposable;

    public WalletSummaryHolder(int resId, ViewGroup parent, WalletClickCallback callback, Realm realm)
    {
        super(resId, parent);

        defaultWalletIndicator = findViewById(R.id.image_default_indicator);
        manageWalletBtn = findViewById(R.id.manage_wallet_btn);
        walletIcon = findViewById(R.id.wallet_icon);
        walletBalanceText = findViewById(R.id.wallet_balance);
        walletNameText = findViewById(R.id.wallet_name);
        walletAddressSeparator = findViewById(R.id.wallet_address_separator);
        walletAddressText = findViewById(R.id.wallet_address);
        arrowRight = findViewById(R.id.container);
        wallet24hChange = findViewById(R.id.wallet_24h_change);
        tokensBalanceView = findViewById(R.id.token_with_balance_view);
        clickCallback = callback;
        manageWalletLayout = findViewById(R.id.layout_manage_wallet);
        this.realm = realm;
    }

    @Override
    public void bind(@Nullable Wallet data, @NonNull Bundle addition)
    {
        walletAddressText.setText(null);
        if (realmUpdate != null) realmUpdate.removeAllChangeListeners();

        if (data != null)
        {
            tokensBalanceView.blankView();
            wallet = fetchWallet(data);
            arrowRight.setOnClickListener(this);
            manageWalletLayout.setOnClickListener(this);

            if (addition.getBoolean(IS_DEFAULT_ADDITION, false))
            {
                defaultWalletIndicator.setVisibility(View.VISIBLE);
            }
            else
            {
                defaultWalletIndicator.setVisibility(View.INVISIBLE);
            }

            manageWalletBtn.setVisibility(View.VISIBLE);

            if (wallet.name != null && !wallet.name.isEmpty())
            {
                walletNameText.setText(wallet.name);
                walletAddressSeparator.setVisibility(View.VISIBLE);
                walletNameText.setVisibility(View.VISIBLE);
            }
            else if (wallet.ENSname != null && wallet.ENSname.length() > 0)
            {
                walletNameText.setText(wallet.ENSname);
                walletAddressSeparator.setVisibility(View.VISIBLE);
                walletNameText.setVisibility(View.VISIBLE);
            }
            else
            {
                walletAddressSeparator.setVisibility(View.GONE);
                walletNameText.setVisibility(View.GONE);
            }

            walletIcon.bind(wallet, this);

            String walletBalance = wallet.balance;
            if (!TextUtils.isEmpty(walletBalance) && walletBalance.startsWith("*"))
            {
                walletBalance = walletBalance.substring(1);
            }
            walletBalanceText.setText(String.format("%s %s", walletBalance, wallet.balanceSymbol));

            walletAddressText.setText(Utils.formatAddress(wallet.address));

            if (addition.getBoolean(IS_SYNCED, false))
            {
                walletIcon.finishWaiting();
            }
            else
            {
                walletIcon.setWaiting();
            }

            if (addition.getBoolean(IS_MAINNET_ACTIVE))
            {
                double fiatValue = addition.getDouble(FIAT_VALUE, 0.00);
                double oldFiatValue = addition.getDouble(FIAT_CHANGE, 0.00);

                String balanceTxt = TickerService.getCurrencyString(fiatValue);
                walletBalanceText.setVisibility(View.VISIBLE);
                walletBalanceText.setText(balanceTxt);
                setWalletChange(fiatValue != 0 ? ((fiatValue - oldFiatValue) / oldFiatValue) * 100.0 : 0.0);
            }
            else
            {
                walletBalanceText.setText("-");
                wallet24hChange.setVisibility(View.GONE);
            }

            checkLastBackUpTime();
            startRealmListener();
        }
    }

    public void setWaiting()
    {
        walletIcon.setWaiting();
    }

    private void setWalletChange(double percentChange24h)
    {
        //This sets the 24hr percentage change (rightmost value)
        try
        {
            wallet24hChange.setVisibility(View.VISIBLE);
            int color = ContextCompat.getColor(getContext(), percentChange24h < 0 ? R.color.negative : R.color.positive);
            BigDecimal percentChangeBI = BigDecimal.valueOf(percentChange24h).setScale(3, RoundingMode.DOWN);
            String formattedPercents = (percentChange24h < 0 ? "" : "+") + percentChangeBI + "%";
            //wallet24hChange.setBackgroundResource(percentage < 0 ? R.drawable.background_24h_change_red : R.drawable.background_24h_change_green);
            wallet24hChange.setText(formattedPercents);
            wallet24hChange.setTextColor(color);
            //image24h.setImageResource(percentage < 0 ? R.drawable.ic_price_down : R.drawable.ic_price_up);
        }
        catch (Exception ex)
        { /* Quietly */ }
    }

    private void startRealmListener()
    {
        realmUpdate = realm.where(RealmWalletData.class)
                .equalTo("address", wallet.address).findAllAsync();
        realmUpdate.addChangeListener(realmWallets ->
        {
            //update balance
            if (realmWallets.size() == 0) return;
            RealmWalletData realmWallet = realmWallets.first();
            //walletBalanceText.setTextColor(blackColor);
            //walletBalanceText.setText(realmWallet.getBalance());
            String ensName = realmWallet.getENSName();
            String name = realmWallet.getName();
            if (!TextUtils.isEmpty(name))
            {
                walletNameText.setText(name);
                walletAddressSeparator.setVisibility(View.VISIBLE);
                walletNameText.setVisibility(View.VISIBLE);
            }
            else if (!TextUtils.isEmpty(ensName))
            {
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
            w.ENSAvatar = realmWallet.getENSAvatar();

            if (w.tokens != null)
            {
                tokensBalanceView.bindTokens(w.tokens);
            }
        }
        return w;
    }

    private void checkLastBackUpTime()
    {
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
                            // TODO: Display indicator
                        }
                        else
                        {
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
    public void onClick(View view)
    {
        //if (wallet == null) { return; } //protect against click between constructor and bind
        switch (view.getId())
        {
            case R.id.container:
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

    @Override
    public void avatarFound(Wallet wallet)
    {
        if (clickCallback != null) clickCallback.ensAvatar(wallet);
    }
}
