package com.wallet.crypto.alphawallet.router;


import android.content.Context;
import android.support.v4.app.FragmentActivity;

import com.wallet.crypto.alphawallet.ui.HelpFragment;

public class HelpRouter {
    public void open(Context context, int resId) {
        HelpFragment helpFragment = new HelpFragment();
        FragmentActivity activity = (FragmentActivity) context;
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(resId, helpFragment)
                .commit();
    }
}
