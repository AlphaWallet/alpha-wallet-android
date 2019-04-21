package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.router.SendTokenRouter;
import io.stormbird.wallet.router.TransactionDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;

public class Erc20DetailViewModel extends BaseViewModel {
    private static final long CHECK_ETHPRICE_INTERVAL = 5;

    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Token> token = new MutableLiveData<>();
    private final MutableLiveData<Ticker> tokenTicker = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactionUpdate = new MutableLiveData<>();

    private final MyAddressRouter myAddressRouter;
    private final FetchTokensInteract fetchTokensInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final TransactionDetailRouter transactionDetailRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final AddTokenInteract addTokenInteract;

    private int transactionFetchCount;

    @Nullable
    private Disposable fetchTransactionDisposable;

    @Nullable
    private Disposable getBalanceDisposable;

    public Erc20DetailViewModel(MyAddressRouter myAddressRouter,
                                FetchTokensInteract fetchTokensInteract,
                                FetchTransactionsInteract fetchTransactionsInteract,
                                FindDefaultNetworkInteract findDefaultNetworkInteract,
                                FindDefaultWalletInteract findDefaultWalletInteract,
                                TransactionDetailRouter transactionDetailRouter,
                                AssetDefinitionService assetDefinitionService,
                                TokensService tokensService,
                                AddTokenInteract addTokenInteract) {
        this.myAddressRouter = myAddressRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.transactionDetailRouter = transactionDetailRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.addTokenInteract = addTokenInteract;
        transactionFetchCount = 0;
    }

    public void showMyAddress(Context context, Wallet wallet, Token token) {
        myAddressRouter.open(context, wallet, token);
    }

    public void showContractInfo(Context ctx, Token token) {
        myAddressRouter.open(ctx, wallet.getValue(), token);
    }

    public void startEthereumTicker(Token token)
    {
        if (token.isEthereum())
        {
            disposable = Observable.interval(0, CHECK_ETHPRICE_INTERVAL, TimeUnit.SECONDS)
                    .doOnNext(l -> fetchTokensInteract
                            .getEthereumTicker(token.tokenInfo.chainId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onTicker, this::onError)).subscribe();
        }
    }

    private void onTicker(Ticker ticker)
    {
        if (ticker != null && ticker.price_usd != null) {
            this.tokenTicker.postValue(ticker);
        }
    }

    public boolean hasIFrame(String address) {
        return assetDefinitionService.hasIFrame(address);
    }

    public String getTokenData(String address) {
        return assetDefinitionService.getIntroductionCode(address);
    }

    public void fetchTransactions(Token token, int historyCount) {
        fetchTransactionDisposable =
                fetchTransactionsInteract.fetchTransactionsFromStorage(wallet.getValue(), token, historyCount)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onUpdateTransactions, this::onError);
    }

    public void fetchNetworkTransactions(Token token, int historyCount) {
        NetworkInfo network = findDefaultNetworkInteract.getNetworkInfo(token.tokenInfo.chainId);
        String userAddress = token.isEthereum() ? null : wallet.getValue().address; //only specify address if we're scanning token transactions - not all are relevant to us.

        fetchTransactionDisposable =
                fetchTransactionsInteract.fetchNetworkTransactions(network, token.getAddress(), token.lastBlockCheck, userAddress)
                        .map(txs -> removeTopTx(txs, token)) //remove block marker TX
                        .flatMap(txs -> fetchTransactionsInteract.storeTransactions(wallet.getValue(), txs).toObservable())
                        .map(txs -> filterToHistory(txs, historyCount))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onNewTransactions, this::onError);
    }

    private Transaction[] removeTopTx(Transaction[] transactions, Token token)
    {
        if (transactions.length > 0)
        {
            long lastTxBlockNumber = Long.parseLong(transactions[transactions.length - 1].blockNumber);
            if (transactions.length > 1)
            {
                Transaction[] newTxList = new Transaction[transactions.length-1];
                System.arraycopy(transactions, 0, newTxList, 0, transactions.length-1);
                transactions = newTxList;
            }
            else
            {
                transactions = new Transaction[0];
            }

            //now store the update
            token.lastBlockCheck = lastTxBlockNumber;
            addTokenInteract.updateBlockRead(token, defaultWallet().getValue());
        }

        return transactions;
    }

    private Transaction[] filterToHistory(Transaction[] transactions, int historyCount)
    {
        //pull out the first -historyCount- values
        if (transactions.length > historyCount)
        {
            Transaction[] newTxList = new Transaction[historyCount];
            System.arraycopy(transactions, 0, newTxList, 0, historyCount);
            transactions = newTxList;
        }

        return transactions;
    }

    private void onUpdateTransactions(Transaction[] transactions) {
        this.transactions.postValue(transactions);
    }

    private void onNewTransactions(Transaction[] transactions) {
        this.transactionUpdate.postValue(transactions);
    }

    public LiveData<Transaction[]> transactions() {
        return transactions;
    }

    public LiveData<Transaction[]> transactionUpdate() {
        return transactionUpdate;
    }

    public void cleanUp() {
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed()) {
            fetchTransactionDisposable.dispose();
        }

        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }

        if (getBalanceDisposable != null && !getBalanceDisposable.isDisposed()) {
            getBalanceDisposable.dispose();
        }

        tokensService.clearFocusToken();
    }

    public FetchTransactionsInteract getTransactionsInteract() {
        return fetchTransactionsInteract;
    }

    public TokensService getTokensService() {
        return tokensService;
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet.postValue(wallet);
    }

    public LiveData<Wallet> defaultWallet() {
        return wallet;
    }

    public void prepare(Token token)
    {
        tokensService.setFocusToken(token);
        progress.postValue(true);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    public void showDetails(Context context, Transaction transaction) {
        transactionDetailRouter.open(context, transaction);
    }

    public AssetDefinitionService getAssetDefinitionService() {
        return this.assetDefinitionService;
    }

    public void updateDefaultBalance(Token token) {
        getBalanceDisposable = Observable.interval(CHECK_ETHPRICE_INTERVAL, CHECK_ETHPRICE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> updateToken(token)).subscribe();
    }

    private void updateToken(Token t) {
        Token update = tokensService.getToken(t.tokenInfo.chainId, t.getAddress());
        this.token.postValue(update);
        if (transactionFetchCount > 0)
        {
            fetchTransactions(update, transactionFetchCount);
        }
    }

    public LiveData<Token> token() {
        return token;
    }

    public LiveData<Ticker> tokenTicker() {
        return tokenTicker;
    }

    public void showSendToken(Context ctx, Token token)
    {
        new SendTokenRouter().open(ctx, token.getAddress(), token.tokenInfo.symbol, token.tokenInfo.decimals,
                                   true, wallet.getValue(), token);
    }

    public void showSendToken(Context ctx, String address, Token token)
    {
        new SendTokenRouter().open(ctx, address, token.tokenInfo.symbol, token.tokenInfo.decimals,
                                   false, wallet.getValue(), token);
    }
}
