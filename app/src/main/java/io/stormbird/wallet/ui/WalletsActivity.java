package io.stormbird.wallet.ui;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.widget.adapter.WalletsAdapter;
import io.stormbird.wallet.viewmodel.WalletsViewModel;
import io.stormbird.wallet.viewmodel.WalletsViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.AddWalletView;
import io.stormbird.wallet.widget.SystemView;

public class WalletsActivity extends BaseActivity implements
        View.OnClickListener,
        AddWalletView.OnNewWalletClickListener,
        AddWalletView.OnImportWalletClickListener {

    @Inject
    WalletsViewModelFactory walletsViewModelFactory;
    WalletsViewModel viewModel;

    private RecyclerView list;
    private SwipeRefreshLayout refreshLayout;
    private SystemView systemView;
    private Dialog dialog;
    private AWalletAlertDialog aDialog;
    private WalletsAdapter adapter;

    private boolean walletChange = false;
    private boolean isSetDefault;
    private boolean isNewWalletCreated;
    private NetworkInfo networkInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallets);
        toolbar();
        setTitle(R.string.empty);
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel = ViewModelProviders.of(this, walletsViewModelFactory)
                .get(WalletsViewModel.class);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
        viewModel.error().observe(this, this::onError);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.wallets().observe(this, this::onFetchWallet);
        viewModel.defaultWallet().observe(this, this::onChangeDefaultWallet);
        viewModel.createdWallet().observe(this, this::onCreatedWallet);
        viewModel.createWalletError().observe(this, this::onCreateWalletError);
        viewModel.updateBalance().observe(this, this::onUpdatedBalance);
        viewModel.namedWallets().observe(this, this::onNamedWallets);
        viewModel.lastENSScanBlock().observe(this, this::onScanBlockReceived);
        viewModel.findNetwork();
    }

    private void initViews() {
        systemView = findViewById(R.id.system_view);
        refreshLayout = findViewById(R.id.refresh_layout);
        list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WalletsAdapter(this::onSetWalletDefault);
        list.setAdapter(adapter);

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this::onSwipeRefresh);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        this.networkInfo = networkInfo;
        adapter.setNetwork(networkInfo);
    }

    private void onSwipeRefresh() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        long lastBlockChecked = pref.getLong(C.ENS_SCAN_BLOCK, 0);
        viewModel.swipeRefreshWallets(0); //check all records
    }

    private void onScanBlockReceived(long blockNumber) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong(C.ENS_SCAN_BLOCK, blockNumber).apply();
    }

    private void onCreateWalletError(ErrorEnvelope errorEnvelope) {
        aDialog = new AWalletAlertDialog(this);
        aDialog.setTitle(R.string.title_dialog_error);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setMessage(TextUtils.isEmpty(errorEnvelope.message)
                ? getString(R.string.error_create_wallet)
                : errorEnvelope.message);
        aDialog.setButtonText(R.string.dialog_ok);
        aDialog.setButtonListener(v -> aDialog.dismiss());
        aDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideDialog();
    }

    @Override
    public void onBackPressed() {
        // User can't start work without wallet.
        if (adapter.getItemCount() > 0) {
            finish();
        } else {
            finish();
            System.exit(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (adapter.getItemCount() > 0) {
            getMenuInflater().inflate(R.menu.menu_add, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                onAddWallet();
            }
            break;
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == C.IMPORT_REQUEST_CODE) {
            showToolbar();
            if (resultCode == RESULT_OK) {
                Snackbar.make(systemView, getString(R.string.toast_message_wallet_imported), Snackbar.LENGTH_SHORT)
                        .show();
                onScanBlockReceived(0); //reset scan block
                //set as isSetDefault
                Wallet importedWallet = data.getParcelableExtra(C.Key.WALLET);
                if (importedWallet != null) {
                    isSetDefault = true;
                    walletChange = true;
                    viewModel.setDefaultWallet(importedWallet);
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.try_again: {
                viewModel.fetchWallets();
            }
            break;
        }
    }

    @Override
    public void onNewWallet(View view) {
        hideDialog();
        viewModel.newWallet();
    }

    @Override
    public void onImportWallet(View view) {
        hideDialog();
        viewModel.importWallet(this);
    }

    private void onUpdatedBalance(Wallet wallet) {
        adapter.updateWalletbalance(wallet); //updateWalletBalances(balances);
    }

    private void onNamedWallets(Map<String, String> walletNames) {
        adapter.updateWalletNames(walletNames);
    }

    private void onAddWallet() {
        AddWalletView addWalletView = new AddWalletView(this);
        addWalletView.setOnNewWalletClickListener(this);
        addWalletView.setOnImportWalletClickListener(this);
        dialog = new BottomSheetDialog(this);
        dialog.setContentView(addWalletView);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        BottomSheetBehavior behavior = BottomSheetBehavior.from((View) addWalletView.getParent());
        dialog.setOnShowListener(dialog -> behavior.setPeekHeight(addWalletView.getHeight()));
        dialog.show();
    }

    private void onChangeDefaultWallet(Wallet wallet) {
        if (walletChange) {
            walletChange = false;
            sendBroadcast(new Intent(C.RESET_WALLET));
        }

        adapter.setDefaultWallet(wallet);

        if (isSetDefault && !isNewWalletCreated)
        {
            viewModel.showHome(this);
        }
    }

    private void onFetchWallet(Wallet[] wallets) {
        if (wallets == null || wallets.length == 0) {
            dissableDisplayHomeAsUp();
            AddWalletView addWalletView = new AddWalletView(this, R.layout.layout_empty_add_account);
            addWalletView.setOnNewWalletClickListener(this);
            addWalletView.setOnImportWalletClickListener(this);
            systemView.showEmpty(addWalletView);
            adapter.setWallets(new Wallet[0]);
            hideToolbar();
        } else {
            enableDisplayHomeAsUp();
            adapter.setWallets(wallets);
            viewModel.updateBalancesIfRequired(wallets);
        }
        invalidateOptionsMenu();
    }

    private void onCreatedWallet(Wallet wallet) {
        hideToolbar();
        viewModel.setDefaultWallet(wallet);
        callNewWalletPage(wallet);
        finish();
    }

    private void callNewWalletPage(Wallet wallet)
    {
        Intent intent = new Intent(this, WalletActionsActivity.class);
        intent.putExtra("wallet", wallet);
        if (networkInfo != null) {
            intent.putExtra("currency", networkInfo.symbol);
        }
        intent.putExtra("walletCount", adapter.getItemCount());
        intent.putExtra("isNewWallet", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        systemView.showError(errorEnvelope.message, this);
    }

    private void onSetWalletDefault(Wallet wallet) {
        viewModel.setDefaultWallet(wallet);
        isSetDefault = true;
        walletChange = true;
        isNewWalletCreated = false;
    }

    private void hideDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }

        if (aDialog != null && aDialog.isShowing()) {
            aDialog.dismiss();
            aDialog = null;
        }
    }
}
