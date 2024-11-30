package com.alphawallet.app.ui;

import static com.alphawallet.app.C.RESET_WALLET;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.viewmodel.AdvancedSettingsViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.SettingsItemView;
import com.bumptech.glide.Glide;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class AdvancedSettingsActivity extends BaseActivity
{
    private AdvancedSettingsViewModel viewModel;
    private SettingsItemView nodeStatus;
    private SettingsItemView console;
    private SettingsItemView clearBrowserCache;
    private SettingsItemView tokenScript;
    private SettingsItemView tokenScriptManagement;
    private SettingsItemView fullScreenSettings;
    private SettingsItemView refreshTokenDatabase;
    private SettingsItemView eip1559Transactions;
    private SettingsItemView analytics;
    private SettingsItemView crashReporting;
    private SettingsItemView developerOverride;
    private SettingsItemView tokenScriptViewer;
    private AWalletAlertDialog waitDialog = null;

    @Nullable
    private Disposable clearTokenCache;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this)
                .get(AdvancedSettingsViewModel.class);

        setContentView(R.layout.activity_generic_settings);
        toolbar();
        setTitle(getString(R.string.title_advanced));

        initializeSettings();

        addSettingsToLayout();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (clearTokenCache != null && !clearTokenCache.isDisposed())
        {
            //terminate the thread
            clearTokenCache.dispose();
        }
    }

    private void initializeSettings()
    {
        nodeStatus = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_node_status)
                .withTitle(R.string.action_node_status)
                .withListener(this::onNodeStatusClicked)
                .build();

        console = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_console)
                .withTitle(R.string.title_console)
                .withListener(this::onConsoleClicked)
                .build();

        clearBrowserCache = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_cache)
                .withTitle(R.string.title_clear_browser_cache)
                .withListener(this::onClearBrowserCacheClicked)
                .build();

        //TODO Change Icon
        tokenScriptManagement = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_tokenscript_manage)
                .withTitle(R.string.tokenscript_management)
                .withListener(this::onTokenScriptManagementClicked)
                .build();

        tokenScriptViewer = new SettingsItemView.Builder(this)
                .withType(SettingsItemView.Type.TOGGLE)
                .withIcon(R.drawable.ic_tokenscript)
                .withTitle(R.string.use_tokenscript_viewer)
                .withListener(this::onUseTokenScriptViewer)
                .build();

        fullScreenSettings = new SettingsItemView.Builder(this)
                .withType(SettingsItemView.Type.TOGGLE)
                .withIcon(R.drawable.ic_phoneicon)
                .withTitle(R.string.fullscreen)
                .withListener(this::onFullScreenClicked)
                .build();

        refreshTokenDatabase = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_reset_tokens)
                .withTitle(R.string.title_reload_token_data)
                .withListener(this::onReloadTokenDataClicked)
                .build();

        eip1559Transactions = new SettingsItemView.Builder(this)
                .withType(SettingsItemView.Type.TOGGLE)
                .withIcon(R.drawable.ic_icons_settings_1559)
                .withTitle(R.string.experimental_1559)
                .withListener(this::on1559TransactionsClicked)
                .build();

        analytics = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_analytics)
                .withTitle(R.string.settings_title_analytics)
                .withListener(this::onAnalyticsClicked)
                .build();

        crashReporting = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_crash_reporting)
                .withTitle(R.string.settings_title_crash_reporting)
                .withListener(this::onCrashReportingClicked)
                .build();

        developerOverride = new SettingsItemView.Builder(this)
                .withType(SettingsItemView.Type.TOGGLE)
                .withIcon(R.drawable.ic_settings_warning)
                .withTitle(R.string.developer_override)
                .withListener(this::onDeveloperOverride)
                .build();

        fullScreenSettings.setToggleState(viewModel.getFullScreenState());
        eip1559Transactions.setToggleState(viewModel.get1559TransactionsState());
        developerOverride.setToggleState(viewModel.getDeveloperOverrideState());
        tokenScriptViewer.setToggleState(viewModel.getTokenScriptViewerState());
    }

    @FunctionalInterface
    public interface Callback 
    {
        void onResult(boolean choice);
    }

    private void onDeveloperOverride()
    {
        boolean developerOverrideState = developerOverride.getToggleState();
        if (developerOverrideState)
        {
            //display warning popup
            showWarningPopup(R.string.developer_override_warning, result -> {
                viewModel.toggleDeveloperOverride(result);
                developerOverride.setToggleState(result);
            });
        }
        else
        {
            viewModel.toggleDeveloperOverride(developerOverride.getToggleState());
        }
    }

    private void showWarningPopup(int message, Callback callback)
    {
        AWalletAlertDialog dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.WARNING);
        dialog.setTitle(R.string.warning);
        dialog.setMessage(message);
        dialog.setButtonText(R.string.i_accept);
        dialog.setButtonListener(v -> {
            callback.onResult(true);
            dialog.dismiss();
        });
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setSecondaryButtonListener(v -> {
            callback.onResult(false);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void onFullScreenClicked()
    {
        viewModel.setFullScreenState(fullScreenSettings.getToggleState());
    }

    private void on1559TransactionsClicked()
    {
        viewModel.toggle1559Transactions(eip1559Transactions.getToggleState());
    }

    private void onUseTokenScriptViewer()
    {
        viewModel.toggleUseViewer(tokenScriptViewer.getToggleState());
    }

    private void addSettingsToLayout()
    {
        LinearLayout advancedSettingsLayout = findViewById(R.id.layout);
        advancedSettingsLayout.addView(nodeStatus);
        advancedSettingsLayout.addView(console);
        advancedSettingsLayout.addView(clearBrowserCache);
        advancedSettingsLayout.addView(tokenScriptManagement);
        advancedSettingsLayout.addView(fullScreenSettings);
        advancedSettingsLayout.addView(refreshTokenDatabase);
        advancedSettingsLayout.addView(eip1559Transactions);
        advancedSettingsLayout.addView(tokenScriptViewer);
        advancedSettingsLayout.addView(analytics);
        advancedSettingsLayout.addView(crashReporting);
        advancedSettingsLayout.addView(developerOverride);
    }

    private void onNodeStatusClicked()
    {
        startActivity(new Intent(this, NodeStatusActivity.class));
    }

    private void onConsoleClicked()
    {
        // TODO: Implementation
    }

    private void onClearBrowserCacheClicked()
    {
        Single.fromCallable(() ->
                {
                    WebView webView = new WebView(this);
                    webView.clearCache(true);
                    webView.clearFormData();
                    webView.clearHistory();
                    webView.clearSslPreferences();
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.removeAllCookies(null);
                    WebStorage.getInstance().deleteAllData();
                    viewModel.blankFilterSettings();
                    Glide.get(this).clearDiskCache();
                    return 1;
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(v ->
                {
                    Toast.makeText(this, getString(R.string.toast_browser_cache_cleared), Toast.LENGTH_SHORT).show();
                    finish();
                }).isDisposed();
    }

    private void onReloadTokenDataClicked()
    {
        if (clearTokenCache != null && !clearTokenCache.isDisposed())
        {
            Toast.makeText(this, getString(R.string.token_data_being_cleared), Toast.LENGTH_SHORT).show();
            return;
        }

        AWalletAlertDialog dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setTitle(R.string.title_reload_token_data);
        dialog.setMessage(R.string.reload_token_data_desc);
        dialog.setButtonText(R.string.action_reload);
        dialog.setButtonListener(v -> {
            viewModel.stopChainActivity();
            showWaitDialog();
            clearTokenCache = viewModel.resetTokenData()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::showResetResult);

            viewModel.blankFilterSettings();
        });
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setSecondaryButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showWaitDialog()
    {
        if (waitDialog != null && waitDialog.isShowing()) return;
        waitDialog = new AWalletAlertDialog(this);
        waitDialog.setTitle(getString(R.string.token_data_being_cleared));
        waitDialog.setIcon(AWalletAlertDialog.NONE);
        waitDialog.setProgressMode();
        waitDialog.setCancelable(true);
        waitDialog.setOnCancelListener(v ->
        {
            if (clearTokenCache != null && !clearTokenCache.isDisposed()) clearTokenCache.dispose();
        });
        waitDialog.show();
    }

    private void showResetResult(boolean resetResult)
    {
        if (resetResult)
        {
            Toast.makeText(this, getString(R.string.toast_token_data_cleared), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            intent.putExtra(RESET_WALLET, true);
            finish();
        }
        else
        {
            Toast.makeText(this, getString(R.string.error_deleting_account), Toast.LENGTH_SHORT).show();
        }
    }

    private void onTokenScriptManagementClicked()
    {
        Intent intent = new Intent(this, TokenScriptManagementActivity.class);
        startActivity(intent);
    }

    private void onCrashReportingClicked()
    {
        Intent intent = new Intent(this, CrashReportSettingsActivity.class);
        startActivity(intent);
    }

    private void onAnalyticsClicked()
    {
        Intent intent = new Intent(this, AnalyticsSettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case HomeActivity.RC_ASSET_EXTERNAL_WRITE_PERM:
                if (viewModel.createDirectory())
                {
                    LinearLayout advancedSettingsLayout = findViewById(R.id.layout);
                    advancedSettingsLayout.removeView(tokenScript);
                    showAlphaWalletDirectoryConfirmation();
                }
                break;
        }
    }

    private void showAlphaWalletDirectoryConfirmation()
    {
        AWalletAlertDialog cDialog = new AWalletAlertDialog(this);
        cDialog.setIcon(AWalletAlertDialog.SUCCESS);
        cDialog.setTitle(getString(R.string.created_aw_directory));
        cDialog.setMessage(getString(R.string.created_aw_directory_detail));
        cDialog.setButtonText(R.string.dialog_ok);
        cDialog.setButtonListener(v ->
        {
            cDialog.dismiss();
        });
        cDialog.show();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }
}
