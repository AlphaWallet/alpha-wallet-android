package com.alphawallet.app.ui;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.ImportWalletCallback;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.QrUrlResult;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.widget.OnImportKeystoreListener;
import com.alphawallet.app.ui.widget.OnImportPrivateKeyListener;
import com.alphawallet.app.ui.widget.OnImportSeedListener;
import com.alphawallet.app.ui.widget.adapter.TabPagerAdapter;
import com.alphawallet.app.ui.zxing.FullScannerFragment;
import com.alphawallet.app.ui.zxing.QRScanningActivity;
import com.alphawallet.app.util.QRURLParser;
import com.alphawallet.app.util.TabUtils;

import dagger.android.AndroidInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.viewmodel.ImportWalletViewModel;
import com.alphawallet.app.viewmodel.ImportWalletViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.SignTransactionDialog;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

import static com.alphawallet.app.C.ErrorCode.ALREADY_ADDED;
import static com.alphawallet.app.C.RESET_WALLET;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.InputAddressView.BARCODE_READER_REQUEST_CODE;

public class ImportWalletActivity extends BaseActivity implements OnImportSeedListener, ImportWalletCallback, OnImportKeystoreListener, OnImportPrivateKeyListener
{
    private static enum ImportType
    {
        SEED_FORM_INDEX, KEYSTORE_FORM_INDEX, PRIVATE_KEY_FORM_INDEX, WATCH_FORM_INDEX
    }

    private final List<Pair<String, Fragment>> pages = new ArrayList<>();

    @Inject
    ImportWalletViewModelFactory importWalletViewModelFactory;
    ImportWalletViewModel importWalletViewModel;
    private AWalletAlertDialog dialog;
    private PinAuthenticationCallbackInterface authInterface;
    private ImportType currentPage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_import_wallet);
        LockOrientation();

        toolbar();

        currentPage = ImportType.SEED_FORM_INDEX;
        String receivedState = getIntent().getStringExtra(C.EXTRA_STATE);
        boolean isWatch = receivedState != null && receivedState.equals("watch");

        pages.add(ImportType.SEED_FORM_INDEX.ordinal(), new Pair<>(getString(R.string.tab_seed), ImportSeedFragment.create()));
        pages.add(ImportType.KEYSTORE_FORM_INDEX.ordinal(), new Pair<>(getString(R.string.tab_keystore), ImportKeystoreFragment.create()));
        pages.add(ImportType.PRIVATE_KEY_FORM_INDEX.ordinal(), new Pair<>(getString(R.string.tab_private_key), ImportPrivateKeyFragment.create()));
        if (isWatch) pages.add(ImportType.WATCH_FORM_INDEX.ordinal(), new Pair<>(getString(R.string.watch_wallet), SetWatchWalletFragment.create()));

        ViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new TabPagerAdapter(getSupportFragmentManager(), pages));
        viewPager.setOffscreenPageLimit(4);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                currentPage = ImportType.values()[position];
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        if (isWatch)
        {
            tabLayout.setVisibility(View.GONE);
            viewPager.setCurrentItem(ImportType.WATCH_FORM_INDEX.ordinal());
            setTitle(getString(R.string.watch_wallet));
        }
        else
        {
            setTitle(R.string.empty);
        }

        importWalletViewModel = ViewModelProviders.of(this, importWalletViewModelFactory)
                .get(ImportWalletViewModel.class);
        importWalletViewModel.progress().observe(this, this::onProgress);
        importWalletViewModel.error().observe(this, this::onError);
        importWalletViewModel.wallet().observe(this, this::onWallet);
        importWalletViewModel.badSeed().observe(this, this::onBadSeed);

        TabUtils.changeTabsFont(this, tabLayout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        switch (currentPage)
        {
            default:
                break;
            case WATCH_FORM_INDEX:
                getMenuInflater().inflate(R.menu.menu_scan, menu);
                break;
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void onBadSeed(Boolean aBoolean)
    {
        ((ImportSeedFragment) pages.get(ImportType.SEED_FORM_INDEX.ordinal()).second).onBadSeed();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ((ImportSeedFragment) pages.get(ImportType.SEED_FORM_INDEX.ordinal()).second)
                .setOnImportSeedListener(this);
        ((ImportKeystoreFragment) pages.get(ImportType.KEYSTORE_FORM_INDEX.ordinal()).second)
                .setOnImportKeystoreListener(this);
        ((ImportPrivateKeyFragment) pages.get(ImportType.PRIVATE_KEY_FORM_INDEX.ordinal()).second)
                .setOnImportPrivateKeyListener(this);

        if (pages.size() > ImportType.WATCH_FORM_INDEX.ordinal() && pages.get(ImportType.WATCH_FORM_INDEX.ordinal()) != null)
        {
            ((SetWatchWalletFragment) pages.get(ImportType.WATCH_FORM_INDEX.ordinal()).second)
                    .setOnSetWatchWalletListener(importWalletViewModel);
        }
    }

    private void resetFragments()
    {
        if (pages.get(ImportType.KEYSTORE_FORM_INDEX.ordinal()) != null)
        {
            ((ImportKeystoreFragment) pages.get(ImportType.KEYSTORE_FORM_INDEX.ordinal()).second)
                    .reset();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideDialog();
        importWalletViewModel.resetSignDialog();
    }

    private void onWallet(Wallet wallet) {
        onProgress(false);
        Intent result = new Intent();
        result.putExtra(C.Key.WALLET, wallet);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        hideDialog();
        String message = TextUtils.isEmpty(errorEnvelope.message)
                ? getString(R.string.error_import)
                : errorEnvelope.message;
        if (errorEnvelope.code == ALREADY_ADDED) {
            message = getString(R.string.error_already_added);
        }
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(message);
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();

        resetFragments();
    }

    private void onProgress(boolean shouldShowProgress) {
        hideDialog();
        if (shouldShowProgress) {
            dialog = new AWalletAlertDialog(this);
            dialog.setTitle(R.string.title_dialog_handling);
            dialog.setProgressMode();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();

            sendBroadcast(new Intent(RESET_WALLET));
        }
    }

    private void hideDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (currentPage ==ImportType.KEYSTORE_FORM_INDEX)
                {
                    if (((ImportKeystoreFragment) pages.get(ImportType.KEYSTORE_FORM_INDEX.ordinal()).second).backPressed()) return true;
                }
                break;
            }
            case R.id.action_scan: {
                //scan QR address
                Intent intent = new Intent(this, QRScanningActivity.class);
                startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
            }
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSeed(String seedPhrase, Activity ctx)
    {
        importWalletViewModel.importHDWallet(seedPhrase, this, this);
    }

    @Override
    public void onKeystore(String keystore, String password)
    {
        String address = extractAddressFromStore(keystore);
        if (address != null && WalletUtils.isValidAddress(address))
        {
            onProgress(true);
            importWalletViewModel.checkKeystorePassword(keystore, address, password)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> keyStoreValid(result, address)).isDisposed();
        }
        else
        {
            keyImportError(getString(R.string.invalid_keystore));
        }
    }

    private void keyStoreValid(Boolean result, String address)
    {
        onProgress(false);
        if (!result)
        {
            keyImportError("Invalid Password for Keystore");
        }
        else if (importWalletViewModel.keystoreExists(address))
        {
            queryReplaceWalletKeystore(address);
        }
        else
        {
            importWalletViewModel.importKeystoreWallet(address, this, this);
        }
    }

    private void queryReplaceWalletKeystore(String address)
    {
        replaceWallet(address);
        dialog.setButtonListener(v -> {
            importWalletViewModel.importKeystoreWallet(address, this, this);
        });
        dialog.show();
    }

    @Override
    public void onPrivateKey(String privateKey)
    {
        try
        {
            BigInteger key = new BigInteger(privateKey, 16);
            if (!WalletUtils.isValidPrivateKey(privateKey)) throw new Exception(getString(R.string.invalid_private_key));
            ECKeyPair keypair = ECKeyPair.create(key);
            String address = Numeric.prependHexPrefix(Keys.getAddress(keypair));

            if (importWalletViewModel.keystoreExists(address))
            {
                queryReplaceWalletPrivateKey(address);
            }
            else
            {
                importWalletViewModel.importPrivateKeyWallet(address, this, this);
            }
        }
        catch (Exception e)
        {
            keyImportError(e.getMessage());
        }
    }

    private void queryReplaceWalletPrivateKey(String address)
    {
        replaceWallet(address);
        dialog.setButtonListener(v -> {
            importWalletViewModel.importPrivateKeyWallet(address, this, this);
        });
        dialog.show();
    }

    @Override
    public void WalletValidated(String address, KeyService.AuthenticationLevel level)
    {
        if (address == null) keyImportError(getString(R.string.import_error));
        else importWalletViewModel.onSeed(address, level);
    }

    @Override
    public void KeystoreValidated(String newPassword, KeyService.AuthenticationLevel level)
    {
        ImportKeystoreFragment importKeystoreFragment = (ImportKeystoreFragment) pages.get(ImportType.KEYSTORE_FORM_INDEX.ordinal()).second;
        if (importKeystoreFragment == null || newPassword == null) keyImportError(getString(R.string.import_error));
        else importWalletViewModel.onKeystore(importKeystoreFragment.getKeystore(), importKeystoreFragment.getPassword(), newPassword, level);
    }

    @Override
    public void KeyValidated(String newPassword, KeyService.AuthenticationLevel level)
    {
        ImportPrivateKeyFragment importPrivateKeyFragment = (ImportPrivateKeyFragment) pages.get(ImportType.PRIVATE_KEY_FORM_INDEX.ordinal()).second;
        if (importPrivateKeyFragment == null || newPassword == null) keyImportError(getString(R.string.import_error));
        else importWalletViewModel.onPrivateKey(importPrivateKeyFragment.getPrivateKey(), newPassword, level);
    }

    @Override
    public void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback)
    {
        authInterface = authCallback;
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
        else if (requestCode == BARCODE_READER_REQUEST_CODE)
        {
            //return code from scanning QR
            handleScanQR(resultCode, data);
        }
    }

    private void handleScanQR(int resultCode, Intent data)
    {
        switch (resultCode)
        {
            case FullScannerFragment.SUCCESS:
                if (data != null) {
                    String barcode = data.getStringExtra(FullScannerFragment.BarcodeObject);

                    //if barcode is still null, ensure we don't GPF
                    if (barcode == null) {
                        displayScanError();
                        return;
                    }

                    QRURLParser parser = QRURLParser.getInstance();
                    QrUrlResult result = parser.parse(barcode);
                    String extracted_address = null;
                    if (result != null && result.getProtocol().equals("address"))
                    {
                        extracted_address = result.getAddress();
                        if (currentPage == ImportType.WATCH_FORM_INDEX)
                        {
                            ((SetWatchWalletFragment) pages.get(ImportType.WATCH_FORM_INDEX.ordinal()).second)
                                    .setAddress(extracted_address);
                        }
                    }
                }
                break;
            case QRScanningActivity.DENY_PERMISSION:
                showCameraDenied();
                break;
            default:
                Log.e("SEND", String.format(getString(R.string.barcode_error_format),
                                            "Code: " + String.valueOf(resultCode)
                ));
                break;
        }
    }

    private void showCameraDenied()
    {
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.title_dialog_error);
        dialog.setMessage(R.string.error_camera_permission_denied);
        dialog.setIcon(ERROR);
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
    }

    private void keyImportError(String error)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();

        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.error_import);
        dialog.setMessage(error);
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void displayScanError()
    {
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.toast_qr_code_no_address);
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String extractAddressFromStore(String store) {
        try {
            JSONObject jsonObject = new JSONObject(store);
            return "0x" + Numeric.cleanHexPrefix(jsonObject.getString("address"));
        } catch (JSONException ex) {
            return null;
        }
    }

    private void replaceWallet(String address)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();

        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.WARNING);
        dialog.setTitle(R.string.reimport_wallet_title);
        dialog.setMessage(getString(R.string.reimport_wallet_detail, address));
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setSecondaryButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
    }

    private void LockOrientation()
    {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }
}
