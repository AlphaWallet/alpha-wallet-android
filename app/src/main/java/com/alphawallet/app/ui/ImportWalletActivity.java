package com.alphawallet.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.EIP681Type;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.ImportWalletCallback;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.KeyEncodingType;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.ui.QRScanning.QRScanner;
import com.alphawallet.app.ui.widget.OnImportKeystoreListener;
import com.alphawallet.app.ui.widget.OnImportPrivateKeyListener;
import com.alphawallet.app.ui.widget.OnImportSeedListener;
import com.alphawallet.app.ui.widget.adapter.TabPagerAdapter;
import com.alphawallet.app.util.QRParser;
import com.alphawallet.app.util.TabUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.ImportWalletViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.alphawallet.app.C.ErrorCode.ALREADY_ADDED;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;

@AndroidEntryPoint
public class ImportWalletActivity extends BaseActivity implements OnImportSeedListener, ImportWalletCallback, OnImportKeystoreListener, OnImportPrivateKeyListener
{
    private enum ImportType
    {
        SEED_FORM_INDEX, KEYSTORE_FORM_INDEX, PRIVATE_KEY_FORM_INDEX, WATCH_FORM_INDEX
    }

    private final List<Pair<String, Fragment>> pages = new ArrayList<>();

    ImportWalletViewModel importWalletViewModel;
    private AWalletAlertDialog dialog;
    private ImportType currentPage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_import_wallet);

        toolbar();
        setTitle(getString(R.string.title_import));

        currentPage = ImportType.SEED_FORM_INDEX;
        String receivedState = getIntent().getStringExtra(C.EXTRA_STATE);
        boolean isWatch = receivedState != null && receivedState.equals("watch");

        pages.add(ImportType.SEED_FORM_INDEX.ordinal(), new Pair<>(getString(R.string.tab_seed), ImportSeedFragment.create()));
        pages.add(ImportType.KEYSTORE_FORM_INDEX.ordinal(), new Pair<>(getString(R.string.tab_keystore), ImportKeystoreFragment.create()));
        pages.add(ImportType.PRIVATE_KEY_FORM_INDEX.ordinal(), new Pair<>(getString(R.string.tab_private_key), ImportPrivateKeyFragment.create()));
        if (isWatch) pages.add(ImportType.WATCH_FORM_INDEX.ordinal(), new Pair<>(getString(R.string.watch_wallet), SetWatchWalletFragment.create()));

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new TabPagerAdapter(this, pages));
        viewPager.setOffscreenPageLimit(pages.size());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int oldPos = currentPage.ordinal();
                currentPage = ImportType.values()[position];
                handlePageChange(oldPos, position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        new TabLayoutMediator(tabLayout, viewPager,
                ((tab, position) -> tab.setText(pages.get(position).first))
        ).attach();
        TabUtils.decorateTabLayout(this, tabLayout);

        if (isWatch)
        {
            tabLayout.setVisibility(View.GONE);
            viewPager.setCurrentItem(ImportType.WATCH_FORM_INDEX.ordinal(), false);
            viewPager.setUserInputEnabled(false);
            setTitle(getString(R.string.watch_wallet));
        }
        else
        {
            viewPager.setUserInputEnabled(true);
            setTitle(getString(R.string.title_import));
        }

        importWalletViewModel = new ViewModelProvider(this)
                .get(ImportWalletViewModel.class);
        importWalletViewModel.progress().observe(this, this::onProgress);
        importWalletViewModel.error().observe(this, this::onError);
        importWalletViewModel.wallet().observe(this, this::onWallet);
        importWalletViewModel.badSeed().observe(this, this::onBadSeed);
        importWalletViewModel.watchExists().observe(this, this::onWatchExists);

    }

    private void handlePageChange(int oldPos, int position)
    {
        if (getSupportFragmentManager().getFragments().size() >= oldPos + 1)
        {
            ((ImportFragment) getSupportFragmentManager().getFragments().get(oldPos)).leaveFocus();
        }

        if (getSupportFragmentManager().getFragments().size() >= position + 1)
        {
            ((ImportFragment) getSupportFragmentManager().getFragments().get(position)).comeIntoFocus();
        }
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

        if ( getIntent().getStringExtra(C.EXTRA_QR_CODE) != null) {
            // wait till import wallet fragment will be available
            new Handler().postDelayed(() -> handleScanQR(Activity.RESULT_OK, getIntent()), 500);
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
        }
    }

    private void hideDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    ActivityResultLauncher<Intent> getQRCode = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> handleScanQR(result.getResultCode(), result.getData()));

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && currentPage ==ImportType.KEYSTORE_FORM_INDEX)
        {
            if (((ImportKeystoreFragment) pages.get(ImportType.KEYSTORE_FORM_INDEX.ordinal()).second).backPressed()) return true;
        }
        else if (item.getItemId() == R.id.action_scan)
        {
            Intent intent = new Intent(this, QRScanner.class);
            getQRCode.launch(intent);
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
        if (Utils.isAddressValid(address))
        {
            onProgress(true);
            importWalletViewModel.checkKeystorePassword(keystore, address, password)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> keyStoreValid(result, address), this::reportKeystoreError)
                    .isDisposed();
        }
        else
        {
            keyImportError(getString(R.string.invalid_keystore));
        }
    }

    private void reportKeystoreError(Throwable e)
    {
        if (e instanceof CipherException)
        {
            keyImportError(e.getMessage());
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
            keyImportError(getString(R.string.invalid_keystore));
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
    public void walletValidated(String data, KeyEncodingType type, KeyService.AuthenticationLevel level)
    {
        if (data == null)
        {
            onProgress(false);
            keyImportError(getString(R.string.import_error));
        }
        else
        {
            switch (type)
            {
                case SEED_PHRASE_KEY:
                    importWalletViewModel.onSeed(data, level);
                    break;
                case KEYSTORE_KEY:
                    ImportKeystoreFragment importKeystoreFragment = (ImportKeystoreFragment) pages.get(ImportType.KEYSTORE_FORM_INDEX.ordinal()).second;
                    importWalletViewModel.onKeystore(importKeystoreFragment.getKeystore(), importKeystoreFragment.getPassword(), data, level);
                    break;
                case RAW_HEX_KEY:
                    ImportPrivateKeyFragment importPrivateKeyFragment = (ImportPrivateKeyFragment) pages.get(ImportType.PRIVATE_KEY_FORM_INDEX.ordinal()).second;
                    importWalletViewModel.onPrivateKey(importPrivateKeyFragment.getPrivateKey(), data, level);
                    break;
            }
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
                importWalletViewModel.completeAuthentication(taskCode);
            }
            else
            {
                importWalletViewModel.failedAuthentication(taskCode);
            }
        }
    }

    private void handleScanQR(int resultCode, Intent data)
    {
        switch (resultCode)
        {
            case Activity.RESULT_OK:
                if (data != null) {
                    String barcode = data.getStringExtra(C.EXTRA_QR_CODE);

                    //if barcode is still null, ensure we don't GPF
                    if (barcode == null) {
                        displayScanError();
                        return;
                    }

                    QRParser parser = QRParser.getInstance(EthereumNetworkBase.extraChains());
                    QRResult result = parser.parse(barcode);
                    String extracted_address = null;
                    if (result != null && result.type == EIP681Type.ADDRESS)
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
            case QRScanner.DENY_PERMISSION:
                showCameraDenied();
                break;
            default:
                Timber.tag("SEND").e(String.format(getString(R.string.barcode_error_format),
                                            "Code: " + resultCode
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

    private void onWatchExists(String address)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();

        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.WARNING);
        dialog.setTitle(R.string.title_no_need_to_watch);
        dialog.setMessage(getString(R.string.watch_exists, address));
        dialog.setButtonText(R.string.action_cancel);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
    }
}
