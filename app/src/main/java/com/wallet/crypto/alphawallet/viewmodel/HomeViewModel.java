package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import com.wallet.crypto.alphawallet.C;
import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.FetchTransactionsInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.GetDefaultWalletBalance;
import com.wallet.crypto.alphawallet.router.AddTokenRouter;
import com.wallet.crypto.alphawallet.router.ExternalBrowserRouter;
import com.wallet.crypto.alphawallet.router.ManageWalletsRouter;
import com.wallet.crypto.alphawallet.router.MarketBrowseRouter;
import com.wallet.crypto.alphawallet.router.MarketplaceRouter;
import com.wallet.crypto.alphawallet.router.MyAddressRouter;
import com.wallet.crypto.alphawallet.router.MyTokensRouter;
import com.wallet.crypto.alphawallet.router.NewSettingsRouter;
import com.wallet.crypto.alphawallet.router.SendRouter;
import com.wallet.crypto.alphawallet.router.SettingsRouter;
import com.wallet.crypto.alphawallet.router.TransactionDetailRouter;
import com.wallet.crypto.alphawallet.router.WalletRouter;

import java.security.AccessControlContext;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class HomeViewModel extends BaseViewModel {
    private static final long GET_BALANCE_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;
    private static final long FETCH_TRANSACTIONS_INTERVAL = 12 * DateUtils.SECOND_IN_MILLIS;
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> defaultWalletBalance = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;
    private final FetchTransactionsInteract fetchTransactionsInteract;

    private final ManageWalletsRouter manageWalletsRouter;
    private final SettingsRouter settingsRouter;
    private final SendRouter sendRouter;
    private final TransactionDetailRouter transactionDetailRouter;
    private final MyAddressRouter myAddressRouter;
    private final MyTokensRouter myTokensRouter;
    private final ExternalBrowserRouter externalBrowserRouter;
    private final MarketBrowseRouter marketBrowseRouter;
    private final WalletRouter walletRouter;
    private final MarketplaceRouter marketplaceRouter;
    private final NewSettingsRouter newSettingsRouter;
    private final AddTokenRouter addTokenRouter;

    @Nullable
    private Disposable getBalanceDisposable;
    @Nullable
    private Disposable fetchTransactionDisposable;
    private Handler handler = new Handler();

    HomeViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            GetDefaultWalletBalance getDefaultWalletBalance,
            ManageWalletsRouter manageWalletsRouter,
            SettingsRouter settingsRouter,
            SendRouter sendRouter,
            TransactionDetailRouter transactionDetailRouter,
            MyAddressRouter myAddressRouter,
            MyTokensRouter myTokensRouter,
            ExternalBrowserRouter externalBrowserRouter,
            MarketBrowseRouter marketBrowseRouter,
            WalletRouter walletRouter,
            MarketplaceRouter marketplaceRouter,
            NewSettingsRouter newSettingsRouter,
            AddTokenRouter addTokenRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.manageWalletsRouter = manageWalletsRouter;
        this.settingsRouter = settingsRouter;
        this.sendRouter = sendRouter;
        this.transactionDetailRouter = transactionDetailRouter;
        this.myAddressRouter = myAddressRouter;
        this.myTokensRouter = myTokensRouter;
        this.externalBrowserRouter = externalBrowserRouter;
        this.marketBrowseRouter = marketBrowseRouter;
        this.walletRouter = walletRouter;
        this.marketplaceRouter = marketplaceRouter;
        this.newSettingsRouter = newSettingsRouter;
        this.addTokenRouter = addTokenRouter;
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        handler.removeCallbacks(startFetchTransactionsTask);
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

    public LiveData<Map<String, String>> defaultWalletBalance() {
        return defaultWalletBalance;
    }

    public void prepare() {
        progress.postValue(true);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    public void fetchTransactions(boolean shouldShowProgress) {
        handler.removeCallbacks(startFetchTransactionsTask);
        progress.postValue(shouldShowProgress);
        /*For specific address use: new Wallet("0x60f7a1cbc59470b74b1df20b133700ec381f15d3")*/
        Observable<Transaction[]> fetch = fetchTransactionsInteract.fetch(defaultWallet.getValue());
        fetchTransactionDisposable = fetch
                .subscribe(this::onTransactions, this::onError, this::onTransactionsFetchCompleted);
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
        getBalance();
        fetchTransactions(true);
    }

    private void onTransactions(Transaction[] transactions) {
        this.transactions.setValue(transactions);
        Boolean last = progress.getValue();
        if (transactions != null && transactions.length > 0 && last != null && last) {
            progress.postValue(true);
        }
    }

    private void onTransactionsFetchCompleted() {
        progress.postValue(false);
        Transaction[] transactions = this.transactions.getValue();
        if (transactions == null || transactions.length == 0) {
            error.postValue(new ErrorEnvelope(C.ErrorCode.EMPTY_COLLECTION, "empty collection"));
        }
        handler.postDelayed(
                startFetchTransactionsTask,
                FETCH_TRANSACTIONS_INTERVAL * DateUtils.SECOND_IN_MILLIS);
    }

    public void showWallets(Context context) {
        manageWalletsRouter.open(context, false);
    }

    public void showSettings(Context context) {
        settingsRouter.open(context);
    }

    public void showNewSettings(Context context, int resId) {
        newSettingsRouter.open(context, resId);
    }

    public void showSend(Context context) {
        sendRouter.open(context, defaultNetwork.getValue().symbol);
    }

    public void showDetails(Context context, Transaction transaction) {
        transactionDetailRouter.open(context, transaction);
    }

    public void showMyAddress(Context context) {
        myAddressRouter.open(context, defaultWallet.getValue());
    }

    public void showMarketplace(Context context) {
        marketBrowseRouter.open(context);
    }

    public void showTokens(Context context) {
        myTokensRouter.open(context, defaultWallet.getValue());
    }

    public void showWalletFragment(Context context, int resId) {
        walletRouter.open(context, resId);
    }

    public void showMarketplaceFragment(Context context, int resId) {
        marketplaceRouter.open(context, resId);
    }

    public void openDeposit(Context context, Uri uri) {
        externalBrowserRouter.open(context, uri);
    }

    private final Runnable startFetchTransactionsTask = () -> this.fetchTransactions(false);

    private final Runnable startGetBalanceTask = this::getBalance;

    public void showAddToken(Context context) {
        addTokenRouter.open(context);
    }
}
