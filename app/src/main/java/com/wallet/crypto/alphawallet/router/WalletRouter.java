package com.wallet.crypto.alphawallet.router;


import android.content.Context;
import android.support.v4.app.FragmentActivity;

import com.wallet.crypto.alphawallet.ui.WalletFragment;

public class WalletRouter {
    public void open(Context context, int resId) {
        WalletFragment walletFragment = new WalletFragment();
        FragmentActivity activity = (FragmentActivity) context;
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(resId, walletFragment)
                .commit();
    }
}
