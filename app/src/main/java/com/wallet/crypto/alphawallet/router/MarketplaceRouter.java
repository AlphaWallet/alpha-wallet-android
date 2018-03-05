package com.wallet.crypto.alphawallet.router;


import android.content.Context;
import android.support.v4.app.FragmentActivity;

import com.wallet.crypto.alphawallet.ui.MarketplaceFragment;

public class MarketplaceRouter {
    public void open(Context context, int resId) {
        MarketplaceFragment marketplaceFragment = new MarketplaceFragment();
        FragmentActivity activity = (FragmentActivity) context;
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(resId, marketplaceFragment)
                .commit();
    }
}
