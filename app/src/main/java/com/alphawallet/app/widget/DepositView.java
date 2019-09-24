package com.alphawallet.app.widget;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.alphawallet.app.C;
import com.alphawallet.app.ui.widget.OnDepositClickListener;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;

public class DepositView extends FrameLayout implements View.OnClickListener {

    private static final Uri coinbaseri = Uri.parse("https://buy.coinbase.com/widget")
            .buildUpon()
            .appendQueryParameter("code", C.COINBASE_WIDGET_CODE)
            .appendQueryParameter("amount", "0")
//            .address={address}
            .appendQueryParameter("crypto_currency", C.ETH_SYMBOL)
            .build();
    private static final Uri shapeshiftUri = Uri.parse("https://shapeshift.io/shifty.html")
            .buildUpon()
            .appendQueryParameter("apiKey", C.SHAPESHIFT_KEY)
            .appendQueryParameter("amount", "0")
//            ?destination=\(address)
            .appendQueryParameter("output", C.ETH_SYMBOL)
            .build();

    private static final Uri changellyteUri = Uri.parse("https://changelly.com/widget/v1?auth=email&from=BTC")
            .buildUpon()
            .appendQueryParameter("to", C.ETH_SYMBOL)
            .appendQueryParameter("merchant_id", C.CHANGELLY_REF_ID)
//            address=\\(address)
            .appendQueryParameter("amount", "0")
            .appendQueryParameter("ref_id", C.CHANGELLY_REF_ID)
            .appendQueryParameter("color", "00cf70")
            .build();

    private OnDepositClickListener onDepositClickListener;
    @NonNull
    private Wallet wallet;

    public DepositView(Context context, @NonNull Wallet wallet) {
        this(context, R.layout.layout_dialog_deposit, wallet);
    }

    public DepositView(Context context, @LayoutRes int layoutId, @NonNull Wallet wallet) {
        super(context);

        init(layoutId, wallet);
    }

    private void init(@LayoutRes int layoutId, @NonNull Wallet wallet) {
        this.wallet = wallet;
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);
        findViewById(R.id.action_coinbase).setOnClickListener(this);
        findViewById(R.id.action_shapeshift).setOnClickListener(this);
        findViewById(R.id.action_changelly).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Uri uri;
        switch (v.getId()) {
            case R.id.action_shapeshift: {
                uri = shapeshiftUri.buildUpon()
                        .appendQueryParameter("destination", wallet.address).build();
            } break;
            case R.id.action_changelly: {
                uri = changellyteUri.buildUpon()
                        .appendQueryParameter("address", wallet.address).build();
            } break;
            default:
            case R.id.action_coinbase: {
                uri = coinbaseri.buildUpon()
                        .appendQueryParameter("address", wallet.address).build();
            } break;
        }
        if (onDepositClickListener != null) {
            onDepositClickListener.onDepositClick(v, uri);
        }
    }

    public void setOnDepositClickListener(OnDepositClickListener onDepositClickListener) {
        this.onDepositClickListener = onDepositClickListener;
    }
}
