package com.alphawallet.app.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import com.alphawallet.app.entity.CurrencyItem;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.GetDefaultWalletBalance;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.router.ManageWalletsRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.TokensService;

import java.util.ArrayList;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

public class NewSettingsViewModel extends BaseViewModel {
    private static final long GET_BALANCE_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;

    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> defaultWalletBalance = new MutableLiveData<>();
    private final MutableLiveData<String> backUpMessage = new MutableLiveData<>();
    private final GenericWalletInteract genericWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;
    private final MyAddressRouter myAddressRouter;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final ManageWalletsRouter manageWalletsRouter;
    private final PreferenceRepositoryType preferenceRepository;
    private final LocaleRepositoryType localeRepository;
    private final TokensService tokensService;
    private final CurrencyRepositoryType currencyRepository;

    @Nullable
    private Disposable getBalanceDisposable;
    @Nullable
    private Disposable fetchTransactionDisposable;
    private Handler handler = new Handler();

    NewSettingsViewModel(
            GenericWalletInteract genericWalletInteract,
            GetDefaultWalletBalance getDefaultWalletBalance,
            MyAddressRouter myAddressRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            ManageWalletsRouter manageWalletsRouter,
            PreferenceRepositoryType preferenceRepository,
            LocaleRepositoryType localeRepository,
            TokensService tokensService,
            CurrencyRepositoryType currencyRepository) {
        this.genericWalletInteract = genericWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
        this.myAddressRouter = myAddressRouter;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.manageWalletsRouter = manageWalletsRouter;
        this.preferenceRepository = preferenceRepository;
        this.localeRepository = localeRepository;
        this.tokensService = tokensService;
        this.currencyRepository = currencyRepository;
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

    public boolean getNotificationState()
    {
        return preferenceRepository.getNotificationsState();
    }
    public void setNotificationState(boolean notificationState)
    {
        preferenceRepository.setNotificationState(notificationState);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        handler.removeCallbacks(startGetBalanceTask);
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public LiveData<Transaction[]> transactions() {
        return transactions;
    }
    public LiveData<String> backUpMessage() { return backUpMessage; }

    public void prepare() {
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
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

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);

        TestWalletBackup();
    }

    public void TestWalletBackup()
    {
        if (defaultWallet.getValue() != null)
        {
            genericWalletInteract.getWalletNeedsBackup(defaultWallet.getValue().address)
                    .subscribe(backUpMessage::postValue).isDisposed();
        }
    }

    public void showMyAddress(Context context) {
        myAddressRouter.open(context, defaultWallet.getValue());
    }

    private final Runnable startGetBalanceTask = this::getBalance;

    public Single<String> setIsDismissed(String walletAddr, boolean isDismissed)
    {
        return genericWalletInteract.setIsDismissed(walletAddr, isDismissed);
    }

    public String getDefaultCurrency() {
        return currencyRepository.getDefaultCurrency();
    }

    public ArrayList<CurrencyItem> getCurrencyList() {
        return currencyRepository.getCurrencyList();
    }
}
