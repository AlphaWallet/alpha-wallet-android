package com.alphawallet.app.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.router.ImportTokenRouter;
import com.alphawallet.app.router.ImportWalletRouter;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.viewmodel.SplashViewModel;
import com.alphawallet.app.viewmodel.SplashViewModelFactory;
import com.alphawallet.app.walletconnect.WCSession;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.ParseMagicLink;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.IMPORT_REQUEST_CODE;

public class SplashActivity extends BaseActivity implements CreateWalletCallbackInterface, Runnable
{
    @Inject
    SplashViewModelFactory splashViewModelFactory;
    SplashViewModel splashViewModel;

    private String importData;
    private String importPath = null;
    private String importPassData = null;
    private Handler handler = new Handler();
    private String errorMessage;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        setContentView(R.layout.activity_splash);
        super.onCreate(savedInstanceState);

        LocaleUtils.setDeviceLocale(getBaseContext());

        // Get the intent that started this activity
        Intent intent = getIntent();
        Uri data = intent.getData();
        ImportTokenActivity importTokenActivity = new ImportTokenActivity();

        if (data != null)
        {
            importData = data.toString();
            if (importData.startsWith("content://"))
            {
                importPath = data.getPath();
            }
        }
        else
        {
            //try the clipboard
            importData = importTokenActivity.getMagiclinkFromClipboard(this);
        }

        splashViewModel = new ViewModelProvider(this, splashViewModelFactory)
                .get(SplashViewModel.class);
        splashViewModel.wallets().observe(this, this::onWallets);
        splashViewModel.createWallet().observe(this, this::onWalletCreate);
        splashViewModel.setLocale(getApplicationContext());
        splashViewModel.setCurrency();

        long getAppUpdateTime = getAppLastUpdateTime();

        splashViewModel.fetchWallets();
        splashViewModel.checkVersionUpdate(getBaseContext(), getAppUpdateTime);
        splashViewModel.cleanAuxData(getApplicationContext());

        TokensService.setWalletStartup();
    }

    protected Activity getThisActivity()
    {
        return this;
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
            splashViewModel.setDefaultBrowser();
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
                    ParseMagicLink parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());
                    if (parser.parseUniversalLink(importData).chainId > 0)
                    {
                        new ImportTokenRouter().open(this, importData);
                        finish();
                        return;
                    }
                }
                catch (SalesOrderMalformed ignored) { }
            }
            else if (importData != null && importData.startsWith("wc:"))
            {
                importPassData = WalletConnectActivity.WC_INTENT + importData;
                WCSession session = WCSession.Companion.from(importData);
                if (session == null)
                {
                    //this is a 'signing' intent, used with an existing, active connection
                    Intent intent = new Intent(this, WalletConnectActivity.class);
                    intent.putExtra("qrCode", importPassData);
                    //re-open the existing activity, when using WalletConnect locally (that is, with a browser app running on the same device)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    if (!startActivityIfNeeded(intent, 0))
                    {
                        //didn't need to start a new activity,
                    }
                    setResult(RESULT_OK);
                    finish();
                }
                else
                {
                    handler.post(this);
                }
                return;
            }
            else if (importPath != null)
            {
                boolean useAppExternalDir = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || !splashViewModel.checkDebugDirectory();
                splashViewModel.importScriptFile(this, importData, useAppExternalDir);
            }

            handler.postDelayed(this, CustomViewSettings.startupDelay());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            Operation taskCode = Operation.values()[requestCode - SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS];
            if (resultCode == RESULT_OK)
            {
                splashViewModel.completeAuthentication(taskCode);
            }
            else
            {
                splashViewModel.failedAuthentication(taskCode);
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
    public void onDestroy()
    {
        super.onDestroy();
        handler = null;
    }

    @Override
    public void keyFailure(String message)
    {
        errorMessage = message;
        if (handler != null) handler.post(displayError);
    }

    Runnable displayError = new Runnable()
    {
        @Override
        public void run()
        {
            AWalletAlertDialog aDialog = new AWalletAlertDialog(getThisActivity());
            aDialog.setTitle(R.string.key_error);
            aDialog.setIcon(AWalletAlertDialog.ERROR);
            aDialog.setMessage(errorMessage);
            aDialog.setButtonText(R.string.dialog_ok);
            aDialog.setButtonListener(v -> aDialog.dismiss());
            aDialog.show();
        }
    };

    @Override
    public void cancelAuthentication()
    {

    }

    @Override
    public void fetchMnemonic(String mnemonic)
    {

    }

    @Override
    public void run()
    {
        new HomeRouter().openWithIntent(this, importPassData);
        finish();
    }
}
