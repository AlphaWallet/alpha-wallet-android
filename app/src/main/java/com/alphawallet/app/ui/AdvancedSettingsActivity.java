package com.alphawallet.app.ui;

import static com.alphawallet.app.C.CHANGED_LOCALE;
import static com.alphawallet.app.C.CHANGE_CURRENCY;
import static com.alphawallet.app.C.EXTRA_CURRENCY;
import static com.alphawallet.app.C.EXTRA_LOCALE;
import static com.alphawallet.app.C.EXTRA_STATE;
import static com.alphawallet.app.C.PAGE_LOADED;
import static com.alphawallet.app.C.RESET_WALLET;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.viewmodel.AdvancedSettingsViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.SettingsItemView;
import com.bumptech.glide.Glide;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class AdvancedSettingsActivity extends BaseActivity {
    private AdvancedSettingsViewModel viewModel;

    private SettingsItemView console;
    private SettingsItemView clearBrowserCache;
    private SettingsItemView tokenScript;
    private SettingsItemView changeLanguage;
    private SettingsItemView tokenScriptManagement;
    private SettingsItemView changeCurrency;
    private SettingsItemView fullScreenSettings;
    private SettingsItemView refreshTokenDatabase;
    private AWalletAlertDialog waitDialog = null;

    @Nullable
    private Disposable clearTokenCache;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this)
                .get(AdvancedSettingsViewModel.class);

        setContentView(R.layout.activity_generic_settings);
        toolbar();
        setTitle(getString(R.string.title_advanced));

        viewModel.setLocale(this);

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

    private void initializeSettings() {
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

        tokenScript = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_tokenscript)
                .withTitle(R.string.title_tokenscript)
                .withListener(this::onTokenScriptClicked)
                .build();

        changeLanguage = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_language)
                .withTitle(R.string.title_change_language)
                .withListener(this::onChangeLanguageClicked)
                .build();

        changeCurrency = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_currency)
                .withTitle(R.string.settings_locale_currency)
                .withListener(this::onChangeCurrencyClicked)
                .build();

        //TODO Change Icon
        tokenScriptManagement = new SettingsItemView.Builder(this)
                .withIcon(R.drawable.ic_settings_tokenscript_manage)
                .withTitle(R.string.tokenscript_management)
                .withListener(this::onTokenScriptManagementClicked)
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

        changeLanguage.setSubtitle(LocaleUtils.getDisplayLanguage(viewModel.getActiveLocale(), viewModel.getActiveLocale()));
        fullScreenSettings.setToggleState(viewModel.getFullScreenState());
    }

    private void onFullScreenClicked()
    {
        viewModel.setFullScreenState(fullScreenSettings.getToggleState());
    }

    private void addSettingsToLayout() {
        LinearLayout advancedSettingsLayout = findViewById(R.id.layout);
        advancedSettingsLayout.addView(console);
        advancedSettingsLayout.addView(clearBrowserCache);

        if (!checkWritePermission() && EthereumNetworkRepository.extraChains() == null)
            advancedSettingsLayout.addView(tokenScript);

        advancedSettingsLayout.addView(changeLanguage);
        advancedSettingsLayout.addView(changeCurrency);
        advancedSettingsLayout.addView(tokenScriptManagement);
        advancedSettingsLayout.addView(fullScreenSettings);
        advancedSettingsLayout.addView(refreshTokenDatabase);
    }

    private void onConsoleClicked() {
        // TODO: Implementation
    }

    private void onClearBrowserCacheClicked() {
        WebView webView = new WebView(this);
        webView.clearCache(true);
        viewModel.blankFilterSettings();

        Single.fromCallable(() -> {
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

    private void onReloadTokenDataClicked() {
        if (clearTokenCache != null && !clearTokenCache.isDisposed())
        {
            Toast.makeText(this, getString(R.string.token_data_being_cleared), Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedSettingsActivity.this);
        AlertDialog dialog = builder.setTitle(R.string.title_reload_token_data)
                .setMessage(R.string.reload_token_data_desc)
                .setPositiveButton(R.string.action_reload, (d, w) -> {
                    //delete all Token data for this wallet
                    viewModel.stopChainActivity();
                    showWaitDialog();
                    clearTokenCache = viewModel.resetTokenData()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::showResetResult);

                    viewModel.blankFilterSettings();
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> d.dismiss())
                .setCancelable(true)
                .create();
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
        waitDialog.setOnCancelListener(v -> {
            if (clearTokenCache != null && !clearTokenCache.isDisposed()) clearTokenCache.dispose();
        });
        waitDialog.show();
    }

    private void removeWaitDialog()
    {
        if (waitDialog != null && waitDialog.isShowing())
        {
            waitDialog.dismiss();
        }
    }

    private void showResetResult(boolean resetResult)
    {
        removeWaitDialog();
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

    private void onTokenScriptClicked() {
        showXMLOverrideDialog();
    }

    ActivityResultLauncher<Intent> updateLocale = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                updateLocale(result.getData());
            });

    ActivityResultLauncher<Intent> updateCurrency = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                updateCurrency(result.getData());
            });

    private void onChangeLanguageClicked() {
        Intent intent = new Intent(this, SelectLocaleActivity.class);
        String selectedLocale = viewModel.getActiveLocale();
        intent.putExtra(EXTRA_LOCALE, selectedLocale);
        intent.putParcelableArrayListExtra(EXTRA_STATE, viewModel.getLocaleList(this));
        updateLocale.launch(intent);
    }

    private void onChangeCurrencyClicked() {
        Intent intent = new Intent(this, SelectCurrencyActivity.class);
        String currentLocale = viewModel.getDefaultCurrency();
        intent.putExtra(EXTRA_CURRENCY, currentLocale);
        intent.putParcelableArrayListExtra(EXTRA_STATE, viewModel.getCurrencyList());
        updateCurrency.launch(intent);
    }

    private void onTokenScriptManagementClicked() {
        Intent intent = new Intent(this, TokenScriptManagementActivity.class);
        startActivity(intent);
    }

    private void showXMLOverrideDialog() {
        AWalletConfirmationDialog cDialog = new AWalletConfirmationDialog(this);
        cDialog.setTitle(R.string.enable_xml_override_dir);
        cDialog.setSmallText(R.string.explain_xml_override);
        cDialog.setMediumText(R.string.ask_user_about_xml_override);
        cDialog.setPrimaryButtonText(R.string.dialog_ok);
        cDialog.setPrimaryButtonListener(v -> {
            //ask for OS permission and write directory
            askWritePermission();
            cDialog.dismiss();
        });
        cDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        cDialog.setSecondaryButtonListener(v -> {
            cDialog.dismiss();
        });
        cDialog.show();
    }

    private void askWritePermission() {
        final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        Timber.w("Folder write permission is not granted. Requesting permission");
        ActivityCompat.requestPermissions(this, permissions, HomeActivity.RC_ASSET_EXTERNAL_WRITE_PERM);
    }

    private boolean checkWritePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void updateLocale(Intent data) {
        if (data != null)
        {
            String newLocale = data.getStringExtra(C.EXTRA_LOCALE);
            String oldLocale = viewModel.getActiveLocale();
            if (!TextUtils.isEmpty(newLocale) && !newLocale.equals(oldLocale))
            {
                viewModel.updateLocale(newLocale, this);
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                intent.putExtra(CHANGED_LOCALE, true);
                finish();
            }
        }
    }

    public void updateCurrency(Intent data)
    {
        if (data == null) return;
        String currencyCode = data.getStringExtra(C.EXTRA_CURRENCY);

        //Check if selected currency code is previous selected one then don't update
        if(viewModel.getDefaultCurrency().equals(currencyCode)) return;

        viewModel.updateCurrency(currencyCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    intent.putExtra(CHANGE_CURRENCY, true);
                    finish();
                })
        .isDisposed();
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
                    //need to set up the listener
                    viewModel.startFileListeners();
                }
                break;
        }
    }

    private void showAlphaWalletDirectoryConfirmation() {
        AWalletAlertDialog cDialog = new AWalletAlertDialog(this);
        cDialog.setIcon(AWalletAlertDialog.SUCCESS);
        cDialog.setTitle(getString(R.string.created_aw_directory));
        cDialog.setMessage(getString(R.string.created_aw_directory_detail));
        cDialog.setButtonText(R.string.dialog_ok);
        cDialog.setButtonListener(v -> {
            cDialog.dismiss();
        });
        cDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        changeCurrency.setSubtitle(viewModel.getDefaultCurrency());
    }
}
