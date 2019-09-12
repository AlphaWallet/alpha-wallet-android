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
import android.view.View;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import java.io.File;

import dagger.android.AndroidInjection;
import io.fabric.sdk.android.Fabric;
import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.tools.ParseMagicLink;
import io.stormbird.wallet.BuildConfig;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.CreateWalletCallbackInterface;
import io.stormbird.wallet.entity.CryptoFunctions;
import io.stormbird.wallet.entity.Operation;
import io.stormbird.wallet.entity.PinAuthenticationCallbackInterface;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ImportTokenRouter;
import io.stormbird.wallet.router.ImportWalletRouter;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.viewmodel.SplashViewModel;
import io.stormbird.wallet.viewmodel.SplashViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.SignTransactionDialog;

import javax.inject.Inject;

import static io.stormbird.wallet.C.IMPORT_REQUEST_CODE;

public class SplashActivity extends BaseActivity implements CreateWalletCallbackInterface
{
    @Inject
    SplashViewModelFactory splashViewModelFactory;
    SplashViewModel splashViewModel;

    private String importData;
    private PinAuthenticationCallbackInterface authInterface;
    private String importPath = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        setContentView(R.layout.activity_splash);
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG)
        {
            CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
            Fabric.with(this, new Crashlytics.Builder().core(core).build());
        }

        // Get the intent that started this activity
        Intent intent = getIntent();
        Uri data = intent.getData();
        ImportTokenActivity importTokenActivity = new ImportTokenActivity();

        if (data != null)
        {
            importData = data.toString();
            if (importData.startsWith("content://"))
            {
                importPath = data.getPath();// data.getPath();
            }
        }
        else
        {
            //try the clipboard
            importData = importTokenActivity.getMagiclinkFromClipboard(this);
        }

        splashViewModel = ViewModelProviders.of(this, splashViewModelFactory)
                .get(SplashViewModel.class);
        splashViewModel.wallets().observe(this, this::onWallets);
        splashViewModel.createWallet().observe(this, this::onWalletCreate);
        splashViewModel.setLocale(getApplicationContext());

        long getAppUpdateTime = getAppLastUpdateTime();

        splashViewModel.fetchWallets();
        splashViewModel.checkVersionUpdate(getBaseContext(), getAppUpdateTime);
    }

    //wallet created, now check if we need to import
    private void onWalletCreate(Wallet wallet)
    {
        System.out.println("KEYS: OnWalletCreate");
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
        System.out.println("KEYS: " + wallets.length);
        if (wallets.length == 0)
        {
            findViewById(R.id.layout_new_wallet).setVisibility(View.VISIBLE);
            findViewById(R.id.button_create).setOnClickListener(v -> {
                splashViewModel.createNewWallet(this, this);
            });
            findViewById(R.id.button_watch).setOnClickListener(v -> {
                new ImportWalletRouter().openWatchCreate(this, IMPORT_REQUEST_CODE);
            });
            findViewById(R.id.button_import).setOnClickListener(v -> {
                new ImportWalletRouter().openForResult(this, IMPORT_REQUEST_CODE);
            });
        }
        else
        {
            //there is at least one account here

            //See if this is a valid import magiclink
            if (importData != null && importData.length() > 60 && importData.contains("aw.app") )
            {
                try
                {
                    ParseMagicLink parser = new ParseMagicLink(new CryptoFunctions());
                    if (parser.parseUniversalLink(importData).chainId > 0)
                    {
                        new ImportTokenRouter().open(this, importData);
                        finish();
                        return;
                    }
                }
                catch (SalesOrderMalformed ignored) { }
            }
            else if (importPath != null)
            {
                if (splashViewModel.checkDebugDirectory())
                {
                    splashViewModel.importScriptFile(this, importPath);
                }
                else
                {
                    displayEnableDebugSupport();
                }
            }

            new HomeRouter().open(this, true);
            finish();
        }
    }

    private void displayEnableDebugSupport()
    {
        AWalletAlertDialog aDialog = new AWalletAlertDialog(this);
        aDialog.setTitle(R.string.title_enable_debug);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setMessage(R.string.need_to_enable_debug);
        aDialog.setButtonText(R.string.ok);
        aDialog.setButtonListener(v -> {
            aDialog.dismiss();
        });
        aDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            Operation taskCode = Operation.values()[requestCode - SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS];
            if (resultCode == RESULT_OK)
            {
                authInterface.CompleteAuthentication(taskCode);
            }
            else
            {
                authInterface.FailedAuthentication(taskCode);
            }
        }
        else if (requestCode == IMPORT_REQUEST_CODE)
        {
            splashViewModel.fetchWallets();
        }
    }

    @Override
    public void HDKeyCreated(String address, Context ctx, KeyService.AuthenticationLevel level)
    {
        splashViewModel.StoreHDKey(address, level);
    }

    @Override
    public void keyFailure(String message)
    {

    }

    @Override
    public void cancelAuthentication()
    {

    }

    @Override
    public void FetchMnemonic(String mnemonic)
    {

    }

    @Override
    public void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback)
    {
        authInterface = authCallback;
    }
}
