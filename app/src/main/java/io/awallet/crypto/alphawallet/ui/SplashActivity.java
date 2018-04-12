package io.awallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.router.HomeRouter;
import io.awallet.crypto.alphawallet.router.ImportTokenRouter;
import io.awallet.crypto.alphawallet.router.ManageWalletsRouter;
import io.awallet.crypto.alphawallet.viewmodel.SplashViewModel;
import io.awallet.crypto.alphawallet.viewmodel.SplashViewModelFactory;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static io.awallet.crypto.alphawallet.C.SHOW_NEW_ACCOUNT_PROMPT;

public class SplashActivity extends BaseActivity {

    @Inject
    SplashViewModelFactory splashViewModelFactory;
    SplashViewModel splashViewModel;

    private String importData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
//        Fabric.with(this, new Crashlytics.Builder()
//                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build());

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

        splashViewModel.startOverridesChain();
    }

    private void onWallets(Wallet[] wallets) {
        // Start home activity
        if (importData != null) {
            new ImportTokenRouter().open(this, importData);
            finish();
        }
        else if (wallets.length == 0 && SHOW_NEW_ACCOUNT_PROMPT) {
            new ManageWalletsRouter().open(this, true);
        } else {
            new HomeRouter().open(this, true);
        }
    }
}
