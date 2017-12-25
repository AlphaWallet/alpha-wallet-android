package com.wallet.crypto.trustapp.views;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.wallet.crypto.trustapp.BuildConfig;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.router.ManageWalletsRouter;
import com.wallet.crypto.trustapp.router.TransactionsRouter;

import io.fabric.sdk.android.Fabric;

public class SplashActivity extends AppCompatActivity {

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build());


        // Start home activity
        if (Controller.with(this).getAccounts().size() == 0) {
            new ManageWalletsRouter().open(this);
        } else {
            new TransactionsRouter().open(this);
        }

        // close splash activity

        finish();

    }

}
