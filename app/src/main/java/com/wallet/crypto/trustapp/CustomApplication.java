package com.wallet.crypto.trustapp;

import android.support.multidex.MultiDexApplication;

import com.github.omadahealth.lollipin.lib.managers.LockManager;
import com.wallet.crypto.trustapp.views.CustomPinActivity;

/**
 * Created by Philipp Rieger on 28.12.17.
 */

public class CustomApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        // enable pin code for the application
        LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
        lockManager.getAppLock().setTimeout(10000);
        lockManager.enableAppLock(this, CustomPinActivity.class);
    }

}
