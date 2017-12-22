package com.wallet.crypto.trustapp.views;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.crashlytics.android.Crashlytics;
import com.wallet.crypto.trustapp.BuildConfig;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.router.ManageWalletsRouter;

import io.fabric.sdk.android.Fabric;

public class SplashActivity extends AppCompatActivity {

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }

        // Start home activity
        if (Controller.with(this).getAccounts().size() == 0) {
            new ManageWalletsRouter().open(this);
        } else {
            startActivity(new Intent(this, TransactionListActivity.class));
        }

        // close splash activity

        finish();

    }

}
