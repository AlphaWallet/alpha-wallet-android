package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.wallet.crypto.alphawallet.BuildConfig;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.router.HomeRouter;
import com.wallet.crypto.alphawallet.router.ImportTokenRouter;
import com.wallet.crypto.alphawallet.router.ManageWalletsRouter;
import com.wallet.crypto.alphawallet.router.TransactionsRouter;
import com.wallet.crypto.alphawallet.viewmodel.SplashViewModel;
import com.wallet.crypto.alphawallet.viewmodel.SplashViewModelFactory;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.fabric.sdk.android.Fabric;

public class SplashActivity extends BaseActivity {

    @Inject
    SplashViewModelFactory splashViewModelFactory;
    SplashViewModel splashViewModel;

    private String importData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build());

        // Get the intent that started this activity
        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null)
        {
            importData = data.toString();
        }

        splashViewModel = ViewModelProviders.of(this, splashViewModelFactory)
                .get(SplashViewModel.class);
        splashViewModel.wallets().observe(this, this::onWallets);
    }

    private void onWallets(Wallet[] wallets) {
        // Start home activity
        if (importData != null) {
            new ImportTokenRouter().open(this, importData);
        }
        else if (wallets.length == 0) {
            new ManageWalletsRouter().open(this, true);
        } else {
//            new TransactionsRouter().open(this, true);
            new HomeRouter().open(this, true);
        }
    }
}
