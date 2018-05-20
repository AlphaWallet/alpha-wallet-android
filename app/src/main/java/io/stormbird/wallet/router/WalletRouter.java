package io.stormbird.wallet.router;


import android.content.Context;
import android.support.v4.app.FragmentActivity;

import io.stormbird.wallet.ui.WalletFragment;

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
