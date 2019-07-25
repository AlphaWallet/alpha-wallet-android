package io.stormbird.wallet.ui;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import android.view.MenuItem;
import android.view.View;
import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.ImportWalletCallback;
import io.stormbird.wallet.entity.PinAuthenticationCallbackInterface;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.service.HDKeyService;
import io.stormbird.wallet.ui.widget.OnImportSeedListener;
import io.stormbird.wallet.ui.widget.adapter.TabPagerAdapter;
import io.stormbird.wallet.util.TabUtils;
import io.stormbird.wallet.viewmodel.ImportWalletViewModel;
import io.stormbird.wallet.viewmodel.ImportWalletViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.SignTransactionDialog;

import static io.stormbird.wallet.C.ErrorCode.ALREADY_ADDED;
import static io.stormbird.wallet.C.RESET_WALLET;

public class ImportWalletActivity extends BaseActivity implements OnImportSeedListener, ImportWalletCallback
{

    private static final int SEED_FORM_INDEX = 0;
    private static final int KEYSTORE_FORM_INDEX = 1;
    private static final int PRIVATE_KEY_FORM_INDEX = 2;

    private final List<Pair<String, Fragment>> pages = new ArrayList<>();

    @Inject
    ImportWalletViewModelFactory importWalletViewModelFactory;
    ImportWalletViewModel importWalletViewModel;
    private AWalletAlertDialog dialog;
    private PinAuthenticationCallbackInterface authInterface;
    private int currentPage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_import_wallet);
        toolbar();
        setTitle(R.string.empty);
        currentPage = SEED_FORM_INDEX;

        pages.add(SEED_FORM_INDEX, new Pair<>(getString(R.string.tab_seed), ImportSeedFragment.create()));
        pages.add(KEYSTORE_FORM_INDEX, new Pair<>(getString(R.string.tab_keystore), ImportKeystoreFragment.create()));
        pages.add(PRIVATE_KEY_FORM_INDEX, new Pair<>(getString(R.string.tab_private_key), ImportPrivateKeyFragment.create()));
        ViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new TabPagerAdapter(getSupportFragmentManager(), pages));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                currentPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        importWalletViewModel = ViewModelProviders.of(this, importWalletViewModelFactory)
                .get(ImportWalletViewModel.class);
        importWalletViewModel.progress().observe(this, this::onProgress);
        importWalletViewModel.error().observe(this, this::onError);
        importWalletViewModel.wallet().observe(this, this::onWallet);
        importWalletViewModel.badSeed().observe(this, this::onBadSeed);

        TabUtils.changeTabsFont(this, tabLayout);
    }

    private void onBadSeed(Boolean aBoolean)
    {
        ((ImportSeedFragment) pages.get(SEED_FORM_INDEX).second).onBadSeed();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ((ImportSeedFragment) pages.get(SEED_FORM_INDEX).second)
                .setOnImportSeedListener(this);
        ((ImportKeystoreFragment) pages.get(KEYSTORE_FORM_INDEX).second)
                .setOnImportKeystoreListener(importWalletViewModel);
        ((ImportPrivateKeyFragment) pages.get(PRIVATE_KEY_FORM_INDEX).second)
                .setOnImportPrivateKeyListener(importWalletViewModel);
    }

    private void resetFragments()
    {
        if (pages.get(KEYSTORE_FORM_INDEX) != null)
        {
            ((ImportKeystoreFragment) pages.get(KEYSTORE_FORM_INDEX).second)
                    .reset();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        hideDialog();
    }

    private void onWallet(Wallet wallet) {
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
                if (currentPage == KEYSTORE_FORM_INDEX)
                {
                    if (((ImportKeystoreFragment) pages.get(KEYSTORE_FORM_INDEX).second).backPressed()) return true;
                }
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSeed(String seedPhrase, Activity ctx)
    {
        HDKeyService hdKeyService = new HDKeyService(ctx);
        hdKeyService.importHDKey(seedPhrase, this);
    }

    @Override
    public void WalletValidated(String address, HDKeyService.AuthenticationLevel level)
    {
        importWalletViewModel.onSeed(address, level);
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
            int taskCode = requestCode - SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS;
            if (resultCode == RESULT_OK)
            {
                authInterface.CompleteAuthentication(taskCode);
            }
            else
            {
                authInterface.FailedAuthentication(taskCode);
            }
        }
    }
}
