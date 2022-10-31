package com.alphawallet.app.ui;

import static com.alphawallet.app.C.IMPORT_REQUEST_CODE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.analytics.FirstWalletAction;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.router.ImportWalletRouter;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.util.RootUtil;
import com.alphawallet.app.viewmodel.SplashViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.SignTransactionDialog;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends BaseActivity implements CreateWalletCallbackInterface, Runnable
{
    private SplashViewModel viewModel;

    private Handler handler = new Handler(Looper.getMainLooper());
    private String errorMessage;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //detect previous launch
        viewModel = new ViewModelProvider(this)
                .get(SplashViewModel.class);

        viewModel.cleanAuxData(getApplicationContext());

        setContentView(R.layout.activity_splash);
        viewModel.wallets().observe(this, this::onWallets);
        viewModel.createWallet().observe(this, this::onWalletCreate);
        viewModel.fetchWallets();

        checkRoot();
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
            viewModel.setDefaultBrowser();
            findViewById(R.id.layout_new_wallet).setVisibility(View.VISIBLE);
            findViewById(R.id.button_create).setOnClickListener(v -> {
                AnalyticsProperties props = new AnalyticsProperties();
                props.put(FirstWalletAction.KEY, FirstWalletAction.CREATE_WALLET.getValue());
                viewModel.track(Analytics.Action.FIRST_WALLET_ACTION, props);
                viewModel.createNewWallet(this, this);
            });
            findViewById(R.id.button_watch).setOnClickListener(v -> {
                new ImportWalletRouter().openWatchCreate(this, IMPORT_REQUEST_CODE);
            });
            findViewById(R.id.button_import).setOnClickListener(v -> {
                AnalyticsProperties props = new AnalyticsProperties();
                props.put(FirstWalletAction.KEY, FirstWalletAction.IMPORT_WALLET.getValue());
                viewModel.track(Analytics.Action.FIRST_WALLET_ACTION, props);
                new ImportWalletRouter().openForResult(this, IMPORT_REQUEST_CODE);
            });
        }
        else
        {
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
                viewModel.completeAuthentication(taskCode);
            }
            else
            {
                viewModel.failedAuthentication(taskCode);
            }
        }
        else if (requestCode == IMPORT_REQUEST_CODE)
        {
            viewModel.fetchWallets();
        }
    }

    @Override
    public void HDKeyCreated(String address, Context ctx, KeyService.AuthenticationLevel level)
    {
        viewModel.StoreHDKey(address, level);
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
        new HomeRouter().open(this, true);
        finish();
    }

    private void checkRoot()
    {
        if (RootUtil.isDeviceRooted())
        {
            AWalletAlertDialog dialog = new AWalletAlertDialog(this);
            dialog.setTitle(R.string.root_title);
            dialog.setMessage(R.string.root_body);
            dialog.setButtonText(R.string.ok);
            dialog.setIcon(AWalletAlertDialog.ERROR);
            dialog.setButtonListener(v -> dialog.dismiss());
            dialog.show();
        }
    }
}
