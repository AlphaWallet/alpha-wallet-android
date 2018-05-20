package io.stormbird.wallet.router;


import android.content.Context;
import android.support.v4.app.FragmentActivity;

import io.stormbird.wallet.ui.NewSettingsFragment;

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
