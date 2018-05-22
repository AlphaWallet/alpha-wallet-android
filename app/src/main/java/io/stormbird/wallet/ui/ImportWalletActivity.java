package io.stormbird.wallet.ui;

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

import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.widget.adapter.TabPagerAdapter;
import io.stormbird.wallet.util.TabUtils;
import io.stormbird.wallet.viewmodel.ImportWalletViewModel;
import io.stormbird.wallet.viewmodel.ImportWalletViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;

import static io.stormbird.wallet.C.ErrorCode.ALREADY_ADDED;
import static io.stormbird.wallet.C.RESET_WALLET;

public class ImportWalletActivity extends BaseActivity {

    private static final int KEYSTORE_FORM_INDEX = 0;
    private static final int PRIVATE_KEY_FORM_INDEX = 1;

    private final List<Pair<String, Fragment>> pages = new ArrayList<>();

    @Inject
    ImportWalletViewModelFactory importWalletViewModelFactory;
    ImportWalletViewModel importWalletViewModel;
    private AWalletAlertDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_import_wallet);
        toolbar();
        setTitle(R.string.empty);

        pages.add(KEYSTORE_FORM_INDEX, new Pair<>(getString(R.string.tab_keystore), ImportKeystoreFragment.create()));
        pages.add(PRIVATE_KEY_FORM_INDEX, new Pair<>(getString(R.string.tab_private_key), ImportPrivateKeyFragment.create()));
        ViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new TabPagerAdapter(getSupportFragmentManager(), pages));
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        importWalletViewModel = ViewModelProviders.of(this, importWalletViewModelFactory)
                .get(ImportWalletViewModel.class);
        importWalletViewModel.progress().observe(this, this::onProgress);
        importWalletViewModel.error().observe(this, this::onError);
        importWalletViewModel.wallet().observe(this, this::onWallet);

        TabUtils.changeTabsFont(this, tabLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ((ImportKeystoreFragment) pages.get(KEYSTORE_FORM_INDEX).second)
                .setOnImportKeystoreListener(importWalletViewModel);
        ((ImportPrivateKeyFragment) pages.get(PRIVATE_KEY_FORM_INDEX).second)
                .setOnImportPrivateKeyListener(importWalletViewModel);
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
}
