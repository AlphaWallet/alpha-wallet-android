package com.alphawallet.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.VisibilityFilter;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.ui.widget.adapter.WalletsAdapter;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.viewmodel.WalletsViewModel;
import com.alphawallet.app.viewmodel.WalletsViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AddWalletView;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.app.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class WalletsActivity extends BaseActivity implements
        View.OnClickListener,
        AddWalletView.OnNewWalletClickListener,
        AddWalletView.OnImportWalletClickListener,
        AddWalletView.OnWatchWalletClickListener,
        AddWalletView.OnCloseActionListener,
        CreateWalletCallbackInterface
{
    @Inject
    WalletsViewModelFactory walletsViewModelFactory;
    WalletsViewModel viewModel;

    private RecyclerView list;
    private SwipeRefreshLayout refreshLayout;
    private SystemView systemView;
    private Dialog dialog;
    private AWalletAlertDialog aDialog;
    private WalletsAdapter adapter;
    private final Handler handler = new Handler();
    private Wallet selectedWallet;

    private boolean requiresHomeRefresh;
    private String dialogError;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallets);
        toolbar();
        setTitle(getString(R.string.title_change_add_wallet));
        initViews();
        requiresHomeRefresh = false;
        initViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initViewModel();
    }

    private void initViewModel()
    {
        if (viewModel == null)
        {
            viewModel = ViewModelProviders.of(this, walletsViewModelFactory)
                    .get(WalletsViewModel.class);
            viewModel.error().observe(this, this::onError);
            viewModel.progress().observe(this, systemView::showProgress);
            viewModel.wallets().observe(this, this::onFetchWallet);
            viewModel.defaultWallet().observe(this, this::onChangeDefaultWallet);
            viewModel.createdWallet().observe(this, this::onCreatedWallet);
            viewModel.createWalletError().observe(this, this::onCreateWalletError);
            viewModel.updateBalance().observe(this, this::onUpdatedBalance);
            viewModel.updateENSName().observe(this, this::updateWalletName);
            viewModel.noWalletsError().observe(this, this::noWallets);
        }

        viewModel.onPrepare();
    }

    protected Activity getThisActivity()
    {
        return this;
    }

    private void noWallets(Boolean aBoolean)
    {
        Intent intent = new Intent(this, SplashActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateWalletName(Wallet wallet)
    {
        adapter.updateWalletName(wallet);
    }

    private void initViews() {
        systemView = findViewById(R.id.system_view);
        refreshLayout = findViewById(R.id.refresh_layout);
        list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WalletsAdapter(this, this::onSetWalletDefault);
        list.setAdapter(adapter);
        list.addItemDecoration(new ListDivider(this));

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this::onSwipeRefresh);
    }

    private void onSwipeRefresh() {
        viewModel.swipeRefreshWallets(); //check all records
    }

    private void onCreateWalletError(ErrorEnvelope errorEnvelope)
    {
        dialogError = errorEnvelope.message;
        if (handler != null) handler.post(displayWalletError);
    }

    private Runnable displayWalletError = new Runnable()
    {
        @Override
        public void run()
        {
            aDialog = new AWalletAlertDialog(getThisActivity());
            aDialog.setTitle(R.string.title_dialog_error);
            aDialog.setIcon(AWalletAlertDialog.ERROR);
            aDialog.setMessage(TextUtils.isEmpty(dialogError)
                               ? getString(R.string.error_create_wallet)
                               : dialogError);
            aDialog.setButtonText(R.string.dialog_ok);
            aDialog.setButtonListener(v -> aDialog.dismiss());
            aDialog.show();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        hideDialog();
        viewModel.onPause(); //no need to update balances if view isn't showing
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        if (VisibilityFilter.canChangeWallets()) getMenuInflater().inflate(R.menu.menu_add, menu);
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
        initViewModel();

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
        else if (requestCode == C.IMPORT_REQUEST_CODE)
        {
            showToolbar();
            if (resultCode == RESULT_OK) {
                Snackbar.make(systemView, getString(R.string.toast_message_wallet_imported), Snackbar.LENGTH_SHORT)
                        .show();

                Wallet importedWallet = data.getParcelableExtra(C.Key.WALLET);
                if (importedWallet != null) {
                    requiresHomeRefresh = true;
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
        viewModel.newWallet(this, this);
    }

    @Override
    public void onWatchWallet(View view)
    {
        hideDialog();
        viewModel.watchWallet(this);
    }

    @Override
    public void onImportWallet(View view) {
        hideDialog();
        viewModel.importWallet(this);
    }

    @Override
    public void onClose(View view) {
        hideDialog();
    }

    private void onUpdatedBalance(Wallet wallet) {
        adapter.updateWalletbalance(wallet);
    }

    private void onAddWallet() {
        AddWalletView addWalletView = new AddWalletView(this);
        addWalletView.setOnNewWalletClickListener(this);
        addWalletView.setOnImportWalletClickListener(this);
        addWalletView.setOnWatchWalletClickListener(this);
        addWalletView.setOnCloseActionListener(this);
        dialog = new BottomSheetDialog(this);
        dialog.setContentView(addWalletView);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        BottomSheetBehavior behavior = BottomSheetBehavior.from((View) addWalletView.getParent());
        dialog.setOnShowListener(dialog -> behavior.setPeekHeight(addWalletView.getHeight()));
        dialog.show();
    }

    private void onChangeDefaultWallet(Wallet wallet) {
        if (selectedWallet != null && !wallet.sameAddress(selectedWallet.address))
        {
            requiresHomeRefresh = true;
        }

        selectedWallet = wallet;
        adapter.setDefaultWallet(wallet);
        if (requiresHomeRefresh)
        {
            requiresHomeRefresh = false;
            viewModel.showHome(this);
        }
    }

    private void onFetchWallet(Wallet[] wallets)
    {
        enableDisplayHomeAsUp();
        adapter.setWallets(wallets);
        viewModel.updateBalancesIfRequired(wallets);
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
        intent.putExtra("currency", viewModel.getNetwork().symbol);
        intent.putExtra("walletCount", adapter.getItemCount());
        intent.putExtra("isNewWallet", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        systemView.showError(errorEnvelope.message, this);
    }

    private void onSetWalletDefault(Wallet wallet) {
        requiresHomeRefresh = true;
        viewModel.setDefaultWallet(wallet);
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

    @Override
    public void HDKeyCreated(String address, Context ctx, KeyService.AuthenticationLevel level)
    {
        if (address == null) onCreateWalletError(new ErrorEnvelope(""));
        else viewModel.StoreHDWallet(address, level);
    }

    @Override
    public void keyFailure(String message)
    {
        onCreateWalletError(new ErrorEnvelope(message));
    }

    @Override
    public void cancelAuthentication()
    {
        onCreateWalletError(new ErrorEnvelope(getString(R.string.authentication_cancelled)));
    }

    @Override
    public void fetchMnemonic(String mnemonic)
    {

    }
}
