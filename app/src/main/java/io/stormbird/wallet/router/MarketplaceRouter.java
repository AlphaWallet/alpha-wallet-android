package io.stormbird.wallet.router;


import android.content.Context;
import android.support.v4.app.FragmentActivity;

import io.stormbird.wallet.ui.MarketplaceFragment;

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
