package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.Uri;

import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.interact.FetchTransactionsInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.GetDefaultWalletBalance;
import com.wallet.crypto.trustapp.router.ExternalBrowserRouter;
import com.wallet.crypto.trustapp.router.ManageWalletsRouter;
import com.wallet.crypto.trustapp.router.MyAddressRouter;
import com.wallet.crypto.trustapp.router.MyTokensRouter;
import com.wallet.crypto.trustapp.router.SendRouter;
import com.wallet.crypto.trustapp.router.SettingsRouter;
import com.wallet.crypto.trustapp.router.TransactionDetailRouter;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class TransactionsViewModel extends BaseViewModel {
    private static final long GET_BALANCE_INTERVAL = 8;
    private static final long FETCH_TRANSACTIONS_INTERVAL = 10;
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
    private Disposable balanceDisposable;
    private Disposable transactionDisposable;

    TransactionsViewModel(
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
            ExternalBrowserRouter externalBrowserRouter) {
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
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        transactionDisposable.dispose();
        balanceDisposable.dispose();
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

    public void fetchTransactions() {
        progress.postValue(true);
        transactionDisposable = Observable.interval(0, FETCH_TRANSACTIONS_INTERVAL, TimeUnit.SECONDS)
            .doOnNext(l ->
                disposable = fetchTransactionsInteract
                        .fetch(defaultWallet.getValue()/*new Wallet("0x60f7a1cbc59470b74b1df20b133700ec381f15d3")*/)
                        .subscribe(this::onTransactions, this::onError))
            .subscribe();
    }

    public void getBalance() {
        balanceDisposable = Observable.interval(0, GET_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> getDefaultWalletBalance
                        .get(defaultWallet.getValue())
                        .subscribe(defaultWalletBalance::postValue, t -> {}))
                .subscribe();
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
        fetchTransactions();
    }

    private void onTransactions(Transaction[] transactions) {
        progress.postValue(false);
        this.transactions.postValue(transactions);
    }

    public void showWallets(Context context) {
        manageWalletsRouter.open(context, false);
    }

    public void showSettings(Context context) {
        settingsRouter.open(context);
    }

    public void showSend(Context context) { sendRouter.open(context); }

    public void showDetails(Context context, Transaction transaction) {
        transactionDetailRouter.open(context, transaction);
    }

    public void showMyAddress(Context context) {
        myAddressRouter.open(context, defaultWallet.getValue());
    }

    public void showTokens(Context context) {
        myTokensRouter.open(context, defaultWallet.getValue());
    }

    public void openDeposit(Context context, Uri uri) {
        externalBrowserRouter.open(context, uri);
    }
}
