package io.stormbird.wallet.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.stormbird.wallet.entity.LocaleItem;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.interact.GetDefaultWalletBalance;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.repository.PreferenceRepositoryType;
import io.stormbird.wallet.router.HelpRouter;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ManageWalletsRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.reactivex.disposables.Disposable;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.util.LocaleUtils;

public class NewSettingsViewModel extends BaseViewModel {
    private static final long GET_BALANCE_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;

    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> defaultWalletBalance = new MutableLiveData<>();
    private final MutableLiveData<String> backUpMessage = new MutableLiveData<>();
    private final GenericWalletInteract genericWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;
    private final MyAddressRouter myAddressRouter;
    private final HelpRouter helpRouter;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final ManageWalletsRouter manageWalletsRouter;
    private final HomeRouter homeRouter;
    private final PreferenceRepositoryType preferenceRepository;
    private final LocaleRepositoryType localeRepository;
    private final TokensService tokensService;

    @Nullable
    private Disposable getBalanceDisposable;
    @Nullable
    private Disposable fetchTransactionDisposable;
    private Handler handler = new Handler();

    NewSettingsViewModel(
            GenericWalletInteract genericWalletInteract,
            GetDefaultWalletBalance getDefaultWalletBalance,
            MyAddressRouter myAddressRouter,
            HelpRouter helpRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            ManageWalletsRouter manageWalletsRouter,
            HomeRouter homeRouter,
            PreferenceRepositoryType preferenceRepository,
            LocaleRepositoryType localeRepository,
            TokensService tokensService) {
        this.genericWalletInteract = genericWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
        this.myAddressRouter = myAddressRouter;
        this.helpRouter = helpRouter;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.manageWalletsRouter = manageWalletsRouter;
        this.homeRouter = homeRouter;
        this.preferenceRepository = preferenceRepository;
        this.localeRepository = localeRepository;
        this.tokensService = tokensService;
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

    public boolean getNotificationState()
    {
        return preferenceRepository.getNotificationsState();
    }
    public void setNotificationState(boolean notificationState)
    {
        preferenceRepository.setNotificationState(notificationState);
    }

    public String getDefaultLocale() {
        return localeRepository.getDefaultLocale();
    }

    public void setDefaultLocale(Context context, String locale) {
        localeRepository.setDefaultLocale(context, locale);
        showHome(context, true); //Refresh activity to reflect changes
    }

    public NetworkInfo[] getNetworkList() {
        return ethereumNetworkRepository.getAvailableNetworkList();
    }

    public String getFilterNetworkList() {
        List<Integer> networkIds = ethereumNetworkRepository.getFilterNetworkList();
        StringBuilder sb = new StringBuilder();
        boolean firstValue = true;
        for (int networkId : networkIds)
        {
            if (!firstValue) sb.append(",");
            sb.append(networkId);
            firstValue = false;
        }
        return sb.toString();
    }

    public void setFilterNetworks(Integer[] selectedItems)
    {
        int[] selectedIds = new int[selectedItems.length];
        int index = 0;
        for (Integer selectedId : selectedItems)
        {
            selectedIds[index++] = selectedId;
        }
        ethereumNetworkRepository.setFilterNetworkList(selectedIds);
        tokensService.setupFilter();
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
        progress.postValue(true);
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

    public void showHelp(Context context) {
        helpRouter.open(context);
    }

    private final Runnable startGetBalanceTask = this::getBalance;

    public ArrayList<LocaleItem> getLocaleList(Context context) {
        return localeRepository.getLocaleList(context);
    }

    public void setLocale(Context activity)
    {
        //get the current locale
        String currentLocale = localeRepository.getDefaultLocale();
        LocaleUtils.setLocale(activity, currentLocale);
    }

    public Single<String> setIsDismissed(String walletAddr, boolean isDismissed)
    {
        return genericWalletInteract.setIsDismissed(walletAddr, isDismissed);
    }
}
