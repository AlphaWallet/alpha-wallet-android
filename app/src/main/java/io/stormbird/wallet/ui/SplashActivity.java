package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.fabric.sdk.android.Fabric;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ImportTokenRouter;
import io.stormbird.wallet.router.ManageWalletsRouter;
import io.stormbird.wallet.viewmodel.SplashViewModel;
import io.stormbird.wallet.viewmodel.SplashViewModelFactory;

import static io.stormbird.wallet.C.SHOW_NEW_ACCOUNT_PROMPT;

public class SplashActivity extends BaseActivity {

    @Inject
    SplashViewModelFactory splashViewModelFactory;
    SplashViewModel splashViewModel;

    private String importData;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        // Get the intent that started this activity
        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null)
        {
            importData = data.toString();
        }
        else
        {
            //try the clipboard
            importData = ImportTokenActivity.getMagiclinkFromClipboard(this);
        }

        splashViewModel = ViewModelProviders.of(this, splashViewModelFactory)
                .get(SplashViewModel.class);
        splashViewModel.wallets().observe(this, this::onWallets);
        splashViewModel.createWallet().observe(this, this::onWalletCreate);
        splashViewModel.setLocale(getApplicationContext());

        long getAppUpdateTime = getAppLastUpdateTime();

        splashViewModel.checkVersionUpdate(getBaseContext(), getAppUpdateTime);
        splashViewModel.startOverridesChain();
    }

    //wallet created, now check if we need to import
    private void onWalletCreate(Wallet wallet)
    {
        Wallet[] wallets = new Wallet[1];
        wallets[0] = wallet;
        onWallets(wallets);
    }

    private long getAppLastUpdateTime()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        long currentInstallDate = pref.getLong("install_time", 0);

        if (currentInstallDate == 0)
        {
            pref.edit().putLong("install_time", System.currentTimeMillis()).apply();
        }

        try
        {
            PackageInfo info =  getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            if (info.lastUpdateTime > currentInstallDate) currentInstallDate = info.lastUpdateTime;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        return currentInstallDate;
    }

    private void onWallets(Wallet[] wallets) {
        //event chain should look like this:
        //1. check if wallets are empty:
        //      - yes, get either create a new account or take user to wallet page if SHOW_NEW_ACCOUNT_PROMPT is set
        //              then come back to this check.
        //      - no. proceed to check if we are importing a link
        //2. repeat after step 1 is complete. Are we importing a ticket?
        //      - yes - proceed with import
        //      - no - proceed to home activity
        if (wallets.length == 0)
        {
            if (SHOW_NEW_ACCOUNT_PROMPT) //We always set this to false
            {
                //NB if SHOW_NEW_ACCOUNT_PROMPT is true and user is trying to import, this will fail
                //However our model is that user doesn't ever go to the backup page at first
                //So no action need be taken at the moment
                new ManageWalletsRouter().open(this, true);
            }
            else
            {
                splashViewModel.createNewWallet();
            }
        }
        else
        {
            //there is at least one account here
            if (importData != null)
            {
                new ImportTokenRouter().open(this, importData);
                finish();
            }
            else
            {
                new HomeRouter().open(this, true);
                finish();
            }
        }
    }
}
