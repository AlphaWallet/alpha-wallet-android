package com.wallet.crypto.alphawallet.router;


import android.content.Context;
import android.support.v4.app.FragmentActivity;

import com.wallet.crypto.alphawallet.ui.NewSettingsFragment;

public class NewSettingsRouter {
    public void open(Context context, int resId) {
        NewSettingsFragment settingsFragment = new NewSettingsFragment();
        FragmentActivity activity = (FragmentActivity) context;
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(resId, settingsFragment)
                .commit();
    }
}
