package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FetchTransactionsInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.router.SendTokenRouter;
import io.stormbird.wallet.router.TransactionDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;

public class Erc20DetailViewModel extends BaseViewModel {
    private static final long CHECK_ETHPRICE_INTERVAL = 5;

    private final MutableLiveData<Double> ethPrice = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Token> tokenTicker = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactionUpdate = new MutableLiveData<>();

    private final MyAddressRouter myAddressRouter;
    private final FetchTokensInteract fetchTokensInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final TransactionDetailRouter transactionDetailRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

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
                                TokensService tokensService) {
        this.myAddressRouter = myAddressRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.transactionDetailRouter = transactionDetailRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        transactionFetchCount = 0;
    }

    public LiveData<Double> ethPriceReading() {
        return ethPrice;
    }

    public void showMyAddress(Context context, Wallet wallet, Token token) {
        myAddressRouter.open(context, wallet, token);
    }

    public void showContractInfo(Context ctx, Token token) {
        myAddressRouter.open(ctx, wallet.getValue(), token);
    }

    public void startEthereumTicker(int chainId) {
        disposable = Observable.interval(0, CHECK_ETHPRICE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .getEthereumTicker(chainId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onTicker, this::onError)).subscribe();
    }

    private void onTicker(Ticker ticker) {
        if (ticker != null && ticker.price_usd != null) {
            ethPrice.postValue(Double.valueOf(ticker.price_usd));
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

    private void onUpdateTransactions(Transaction[] transactions) {
        this.transactions.postValue(transactions);
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
                .doOnNext(l -> fetchTokensInteract
                        .fetchStoredToken(findDefaultNetworkInteract.getNetworkInfo(token.tokenInfo.chainId), wallet.getValue(), token.getAddress())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onToken, this::onError)).subscribe();
    }

    private void onToken(Token token) {
        this.tokenTicker.postValue(token);
        if (transactionFetchCount > 0)
        {
            fetchTransactions(token, transactionFetchCount);
        }
    }

    public LiveData<Token> token() {
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

    public void updateTransactions(Token token)
    {
        NetworkInfo network = findDefaultNetworkInteract.getNetworkInfo(token.tokenInfo.chainId);
        String userAddress = token.isEthereum() ? null : this.wallet.getValue().address;
        fetchTransactionDisposable =
                fetchTransactionsInteract.fetchCached(wallet.getValue())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::newTransactions, this::onError);

//                fetchTransactionsInteract.fetchNetworkTransactions(network, token.getAddress(), token.lastBlockCheck, userAddress)
//                        .flatMap(transactions -> fetchTransactionsInteract.storeTransactions(wallet.getValue(), transactions).toObservable())
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(this::newTransactions, this::onError);
    }

    private void newTransactions(Transaction[] transactions)
    {
        transactionUpdate.postValue(transactions);
    }

    public void listenNewTransactions(int historyLength)
    {
        transactionFetchCount = historyLength;
    }
}
