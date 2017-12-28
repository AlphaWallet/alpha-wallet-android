package com.wallet.crypto.trustapp.views;

import android.content.Intent;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.github.omadahealth.lollipin.lib.PinCompatActivity;
import com.wallet.crypto.trustapp.BuildConfig;

import io.fabric.sdk.android.Fabric;

public class SplashActivity extends PinCompatActivity {

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Fabric.with(this, new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build());

        // Start home activity

        startActivity(new Intent(this, TransactionListActivity.class));

        // close splash activity

        finish();

    }

}
