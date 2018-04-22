package io.awallet.crypto.alphawallet.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import java.util.Map;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.GetDefaultWalletBalance;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import io.awallet.crypto.alphawallet.router.HelpRouter;
import io.awallet.crypto.alphawallet.router.HomeRouter;
import io.awallet.crypto.alphawallet.router.ManageWalletsRouter;
import io.awallet.crypto.alphawallet.router.MyAddressRouter;
import io.reactivex.disposables.Disposable;

public class NewSettingsViewModel extends BaseViewModel {
    private static final long GET_BALANCE_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> defaultWalletBalance = new MutableLiveData<>();
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;
    private final MyAddressRouter myAddressRouter;
    private final HelpRouter helpRouter;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final ManageWalletsRouter manageWalletsRouter;
    private final HomeRouter homeRouter;

    @Nullable
    private Disposable getBalanceDisposable;
    @Nullable
    private Disposable fetchTransactionDisposable;
    private Handler handler = new Handler();

    NewSettingsViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            GetDefaultWalletBalance getDefaultWalletBalance,
            MyAddressRouter myAddressRouter,
            HelpRouter helpRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            ManageWalletsRouter manageWalletsRouter,
            HomeRouter homeRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
        this.myAddressRouter = myAddressRouter;
        this.helpRouter = helpRouter;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.manageWalletsRouter = manageWalletsRouter;
        this.homeRouter = homeRouter;
    }

    public void showHome(Context context, boolean clearStack, boolean fromSettings) {
        homeRouter.open(context, clearStack, true);
    }

    public void showHome(Context context, boolean clearStack) {
        homeRouter.open(context, clearStack);
    }

    public void showManageWallets(Context context, boolean clearStack) {
        manageWalletsRouter.open(context, clearStack);
    }

    public void setNetwork(String selectedRpcServer) {
        NetworkInfo[] networks = ethereumNetworkRepository.getAvailableNetworkList();
        for (NetworkInfo networkInfo : networks) {
            if (networkInfo.name.equals(selectedRpcServer)) {
                ethereumNetworkRepository.setDefaultNetworkInfo(networkInfo);
                return;
            }
        }
    }

    public String[] getNetworkList() {
        NetworkInfo[] networks = ethereumNetworkRepository.getAvailableNetworkList();
        String[] networkList = new String[networks.length];
        for (int ii = 0; ii < networks.length; ii++) {
            networkList[ii] = networks[ii].name;
        }
        return networkList;
    }

    public NetworkInfo getDefaultNetworkInfo() {
        return ethereumNetworkRepository.getDefaultNetwork();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        handler.removeCallbacks(startGetBalanceTask);
    }

    public LiveData<NetworkInfo> defaultNetwork() {
        return defaultNetwork;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public LiveData<Transaction[]> transactions() {
        return transactions;
    }

    public void prepare() {
        progress.postValue(true);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    public void getBalance() {
        getBalanceDisposable = getDefaultWalletBalance
                .get(defaultWallet.getValue())
                .subscribe(values -> {
                    defaultWalletBalance.postValue(values);
                    handler.removeCallbacks(startGetBalanceTask);
                    handler.postDelayed(startGetBalanceTask, GET_BALANCE_INTERVAL);
                }, t -> {
                });
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    public void showMyAddress(Context context) {
        myAddressRouter.open(context, defaultWallet.getValue());
    }

    public void showHelp(Context context) {
        helpRouter.open(context);
    }

    private final Runnable startGetBalanceTask = this::getBalance;
}
