package com.alphawallet.app.widget;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.alphawallet.app.ui.widget.OnDepositClickListener;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;

public class DepositView extends FrameLayout implements View.OnClickListener {

    private static final String coinbaseUrl = "https://buy.coinbase.com/";
    private static final String localethereum = "https://localethereum.com/";
    private static final String changelly = "https://payments.changelly.com/?crypto=ETH&amount=0";
    private final Context context;

    private OnDepositClickListener onDepositClickListener;
    @NonNull
    private Wallet wallet;

    public DepositView(Context context, @NonNull Wallet wallet) {
        this(context, R.layout.layout_dialog_deposit, wallet);
    }

    public DepositView(Context context, @LayoutRes int layoutId, @NonNull Wallet wallet) {
        super(context);
        this.context = context;
        init(layoutId, wallet);
    }

    private void init(@LayoutRes int layoutId, @NonNull Wallet wallet) {
        this.wallet = wallet;
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);
        findViewById(R.id.action_coinbase).setOnClickListener(this);
        findViewById(R.id.action_localeth).setOnClickListener(this);
        findViewById(R.id.action_changelly).setOnClickListener(this);
    }

    /**
     * After user selects where they want to buy, open Dapp browser at the site
     * @param v
     */
    @Override
    public void onClick(View v) {
        String url;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String userCurrency = pref.getString("currency_locale", "USD");

        final int action_localeth = R.id.action_localeth;
        final int action_changelly = R.id.action_changelly;
        final int action_coinbase = R.id.action_coinbase;

        switch (v.getId()) {
            case action_localeth: {
                url = localethereum;
            } break;
            case action_changelly: {
                url = changelly + "&fiat=" + userCurrency;
            } break;
            default:
            case action_coinbase: {
                url = coinbaseUrl;
            } break;
        }
        if (onDepositClickListener != null) {
            onDepositClickListener.onDepositClick(v, url);
        }
    }

    public void setOnDepositClickListener(OnDepositClickListener onDepositClickListener) {
        this.onDepositClickListener = onDepositClickListener;
    }
}
