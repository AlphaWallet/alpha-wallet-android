package com.alphawallet.app.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.SyncCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.ui.widget.adapter.WalletsSummaryAdapter;
import com.alphawallet.app.viewmodel.WalletsViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AddWalletView;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.app.widget.SystemView;
import com.alphawallet.hardware.HardwareCallback;
import com.alphawallet.hardware.HardwareDevice;
import com.alphawallet.hardware.SignatureFromKey;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.security.SignatureException;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@AndroidEntryPoint
public class WalletsActivity extends BaseActivity implements
        View.OnClickListener,
        AddWalletView.OnNewWalletClickListener,
        AddWalletView.OnImportWalletClickListener,
        AddWalletView.OnWatchWalletClickListener,
        AddWalletView.OnCloseActionListener,
        AddWalletView.OnHardwareCardActionListener,
        CreateWalletCallbackInterface,
        HardwareCallback,
        SyncCallback
{
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long balanceChain = EthereumNetworkRepository.getOverrideToken().chainId;
    private WalletsViewModel viewModel;
    private RecyclerView list;
    private SystemView systemView;
    private Dialog dialog;
    private AWalletAlertDialog aDialog;
    private WalletsSummaryAdapter adapter;
    private ActivityResultLauncher<Intent> editWalletDetails;
    private AWalletAlertDialog cardReadDialog;
    private String dialogError;
    private final Runnable displayWalletError = new Runnable()
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

    private final HardwareDevice hardwareCard = new HardwareDevice(this);

    @Inject
    PreferenceRepositoryType preferenceRepository;

    private Wallet lastActiveWallet;
    private boolean reloadRequired;
    private Disposable disposable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallets);
        toolbar();
        setTitle(getString(R.string.title_wallets_summary));
        initResultLaunchers();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        initViewModel();
        hardwareCard.activateReader(this);
        hardwareCard.setSigningData(org.web3j.crypto.Hash.sha3(WalletsViewModel.TEST_STRING.getBytes()));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        hideDialog();
        viewModel.onPause(); //no need to update balances if view isn't showing
    }

    private void scrollToDefaultWallet()
    {
        int position = adapter.getDefaultWalletIndex();
        if (position != -1)
        {
            list.getLayoutManager().scrollToPosition(position);
        }
    }

    private void initViewModel()
    {
        if (viewModel == null)
        {
            systemView = findViewById(R.id.system_view);
            viewModel = new ViewModelProvider(this)
                    .get(WalletsViewModel.class);
            viewModel.error().observe(this, this::onError);
            viewModel.progress().observe(this, systemView::showProgress);
            viewModel.wallets().observe(this, this::onFetchWallets);
            viewModel.setupWallet().observe(this, this::setupWallet); //initial wallet setup at activity startup
            viewModel.newWalletCreated().observe(this, this::onNewWalletCreated); //new wallet was created
            viewModel.changeDefaultWallet().observe(this, this::walletChanged);
            viewModel.createWalletError().observe(this, this::onCreateWalletError);
            viewModel.noWalletsError().observe(this, this::noWallets);
            viewModel.baseTokens().observe(this, this::updateBaseTokens);
        }

        disposable = viewModel.getWalletInteract().find()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onActiveWalletFetched);

        initViews();
        viewModel.onPrepare(balanceChain, this);
    }

    private void onActiveWalletFetched(Wallet activeWallet)
    {
        if (lastActiveWallet != null)
        {
            reloadRequired = !lastActiveWallet.equals(activeWallet);
        }

        lastActiveWallet = activeWallet;
    }

    private void updateBaseTokens(Map<String, Token[]> walletTokens)
    {
        adapter.setTokens(walletTokens);
    }

    protected Activity getThisActivity()
    {
        return this;
    }

    private void noWallets(Boolean aBoolean)
    {
        Intent intent = new Intent(this, SplashActivity.class);
        startActivity(intent);
        preFinish();
    }

    private void initViews()
    {
        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WalletsSummaryAdapter(this, this::onSetWalletDefault, viewModel.getWalletInteract());
        list.setAdapter(adapter);

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this::onSwipeRefresh);
    }

    private void onSwipeRefresh()
    {
        viewModel.swipeRefreshWallets(); //check all records
    }

    private void onCreateWalletError(ErrorEnvelope errorEnvelope)
    {
        dialogError = errorEnvelope.message;
        if (handler != null) handler.post(displayWalletError);
    }

    @Override
    public void syncUpdate(String wallet, Pair<Double, Double> value)
    {
        runOnUiThread(() -> adapter.updateWalletState(wallet, value));
    }

    @Override
    public void syncCompleted(String wallet, Pair<Double, Double> value)
    {
        runOnUiThread(() -> adapter.completeWalletSync(wallet, value));
    }

    @Override
    public void syncStarted(String wallet, Pair<Double, Double> value)
    {
        runOnUiThread(() -> adapter.setUnsyncedWalletValue(wallet, value));
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (reloadRequired)
        {
            walletChanged(lastActiveWallet);
        }

        if (disposable != null && !disposable.isDisposed())
            disposable.dispose();

        if (adapter != null) adapter.onDestroy();
        if (viewModel != null) viewModel.onDestroy();
    }

    private void backPressed()
    {
        preFinish();
        // User can't start work without wallet.
        if (adapter.getItemCount() == 0)
        {
            System.exit(0);
        }
    }

    @Override
    public void handleBackPressed()
    {
        backPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (CustomViewSettings.canChangeWallets()) getMenuInflater().inflate(R.menu.menu_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_add)
        {
            onAddWallet();
        }
        else if (item.getItemId() == android.R.id.home)
        {
            backPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
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
            if (resultCode == RESULT_OK)
            {
                Snackbar.make(systemView, getString(R.string.toast_message_wallet_imported), Snackbar.LENGTH_SHORT)
                        .show();

                Wallet importedWallet = data.getParcelableExtra(C.Key.WALLET);
                if (importedWallet != null)
                {
                    //switch to this wallet
                    viewModel.setNewWallet(importedWallet);
                }
            }
        }
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.try_again)
        {
            viewModel.fetchWallets();
        }
    }

    @Override
    public void onNewWallet(View view)
    {
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
    public void onImportWallet(View view)
    {
        hideDialog();
        viewModel.importWallet(this);
    }

    @Override
    public void onClose(View view)
    {
        hideDialog();
    }

    private void onAddWallet()
    {
        AddWalletView addWalletView = new AddWalletView(this);
        addWalletView.setOnNewWalletClickListener(this);
        addWalletView.setOnImportWalletClickListener(this);
        addWalletView.setOnWatchWalletClickListener(this);
        addWalletView.setOnHardwareCardClickListener(this);
        addWalletView.setHardwareActive(hardwareCard.isStub());
        dialog = new BottomSheetDialog(this);
        dialog.setContentView(addWalletView);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    /**
     * Called once at Activity startup
     * @param wallet
     */
    private void setupWallet(Wallet wallet)
    {
        if (adapter != null)
        {
            adapter.setDefaultWallet(wallet);
            scrollToDefaultWallet();
        }
    }

    /**
     * Called after new wallet has been stored, take user to WalletActionsActivity to finish setup
     * @param wallet
     */
    private void onNewWalletCreated(Wallet wallet)
    {
        // TODO: [Notifications] Uncomment when backend service is implemented
        // viewModel.subscribeToNotifications();
        updateCurrentWallet(wallet);
        hideToolbar();
        callNewWalletPage(wallet);
    }

    /**
     * User selected new wallet, change to that wallet and jump to wallet page
     * @param wallet
     */
    private void walletChanged(Wallet wallet)
    {
        // TODO: [Notifications] Uncomment when backend service is implemented
        // viewModel.subscribeToNotifications();
        updateCurrentWallet(wallet);
        viewModel.showHome(this);
    }

    private void updateCurrentWallet(Wallet wallet)
    {
        viewModel.logIn(wallet.address);

        if (adapter == null)
        {
            recreate();
            return;
        }

        adapter.setDefaultWallet(wallet);
        scrollToDefaultWallet();

        viewModel.stopUpdates();
    }

    private void onFetchWallets(Wallet[] wallets)
    {
        enableDisplayHomeAsUp();
        if (adapter != null)
        {
            adapter.setWallets(wallets);
            scrollToDefaultWallet();
        }

        invalidateOptionsMenu();
    }

    private void preFinish()
    {
        hardwareCard.deactivateReader();
        finish();
    }

    private void callNewWalletPage(Wallet wallet)
    {
        Intent intent = new Intent(this, WalletActionsActivity.class);
        intent.putExtra("wallet", wallet);
        intent.putExtra("currency", viewModel.getNetwork().symbol);
        intent.putExtra("isNewWallet", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        editWalletDetails.launch(intent);
    }

    private void initResultLaunchers()
    {
        editWalletDetails = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    viewModel.showHome(this);
                });
    }

    private void onError(ErrorEnvelope errorEnvelope)
    {
        systemView.showError(errorEnvelope.message, this);
    }

    private void onSetWalletDefault(Wallet wallet)
    {
        reloadRequired = false;
        viewModel.changeDefaultWallet(wallet);
    }

    private void hideDialog()
    {
        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
            dialog = null;
        }

        if (aDialog != null && aDialog.isShowing())
        {
            aDialog.dismiss();
            aDialog = null;
        }
    }

    @Override
    public void HDKeyCreated(String address, Context ctx, KeyService.AuthenticationLevel level)
    {
        if (address == null) onCreateWalletError(new ErrorEnvelope(""));
        else viewModel.storeHDWallet(address, level);
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
    public void detectCard(View view)
    {
        //TODO: Hardware: Show waiting for card scan. Inform user to keep the card still and in place
        Toast.makeText(this, hardwareCard.getPlaceCardMessage(this), Toast.LENGTH_SHORT).show();
        hideDialog();
    }

    @Override
    public void fetchMnemonic(String mnemonic)
    {

    }

    // Callbacks from HardwareDevice

    @Override
    public void hardwareCardError(String errorMessage)
    {
        cardReadDialog.dismiss();
        //TODO: Hardware Improve error reporting UI (Popup?)
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void signedMessageFromHardware(SignatureFromKey returnSig)
    {
        cardReadDialog.dismiss();
        try
        {
            viewModel.storeHardwareWallet(returnSig);
        }
        catch (SignatureException ex)
        {
            //TODO: Hardware: Display this in a popup
            Toast.makeText(this, "Import Card: " + ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCardReadStart()
    {
        //TODO: Hardware; display popup graphic - this popup doesn't show
        runOnUiThread(() -> {
            if (cardReadDialog != null && cardReadDialog.isShowing()) cardReadDialog.dismiss();
            cardReadDialog = new AWalletAlertDialog(this);
            cardReadDialog.setTitle(hardwareCard.getPlaceCardMessage(this));
            cardReadDialog.setIcon(AWalletAlertDialog.NONE);
            cardReadDialog.setProgressMode();
            cardReadDialog.setCancelable(false);
            cardReadDialog.show();
        });
    }
}
