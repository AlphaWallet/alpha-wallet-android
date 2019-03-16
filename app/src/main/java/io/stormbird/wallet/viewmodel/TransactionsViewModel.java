package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.router.ExternalBrowserRouter;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.TransactionDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.EventService;
import io.stormbird.wallet.service.TokensService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static io.stormbird.wallet.entity.TransactionDecoder.isEndContract;

public class TransactionsViewModel extends BaseViewModel
{
    private static final String TAG = "TVM";

    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showEmpty = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> clearAdapter = new MutableLiveData<>();
    private final MutableLiveData<Boolean> refreshAdapter = new MutableLiveData<>();
    private final MutableLiveData<Boolean> queryVisibility = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> newTransactions = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AddTokenInteract addTokenInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final EventService eventService;

    private final TransactionDetailRouter transactionDetailRouter;
    private final ExternalBrowserRouter externalBrowserRouter;
    private final HomeRouter homeRouter;

    @Nullable
    private Disposable fetchTransactionDisposable;
    @Nullable
    private Disposable eventTimer;
    @Nullable
    private Disposable handleTerminatedContracts;

    private final ConcurrentLinkedQueue<Token> eventQueue;

    private boolean hasNewTransactions;

    TransactionsViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            SetupTokensInteract setupTokensInteract,
            AddTokenInteract addTokenInteract,
            TransactionDetailRouter transactionDetailRouter,
            ExternalBrowserRouter externalBrowserRouter,
            HomeRouter homeRouter,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            EventService eventService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.transactionDetailRouter = transactionDetailRouter;
        this.externalBrowserRouter = externalBrowserRouter;
        this.homeRouter = homeRouter;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.eventService = eventService;
        this.eventQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();

        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
        {
            fetchTransactionDisposable.dispose();
        }

        if (eventTimer != null && !eventTimer.isDisposed())
        {
            eventTimer.dispose();
        }
    }

    public void abortAndRestart(boolean refreshCache)
    {
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
        {
            fetchTransactionDisposable.dispose();
        }

        fetchTransactionDisposable = null;
    }

    private void startEventTimer()
    {
        //reset transaction timers
        if (eventTimer == null || eventTimer.isDisposed())
        {
            eventTimer = Observable.interval(0, 1, TimeUnit.SECONDS)
                    .doOnNext(l -> checkEvents()).subscribe();
        }
    }

    private void checkEvents()
    {
        //see which tokens need checking
        disposable = Observable.fromCallable(tokensService::getAllLiveTokens)
                    .flatMapIterable(token -> token)
                    .filter(Token::requiresTransactionRefresh)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(eventQueue::add, this::onError, this::checkTransactionQueue);
    }

    private void checkTransactionQueue()
    {
        if (fetchTransactionDisposable == null)
        {
            Token t = eventQueue.poll();

            if (t != null)
            {
                NetworkInfo network = findDefaultNetworkInteract.getNetworkInfo(t.tokenInfo.chainId);
                String userAddress = t.isEthereum() ? null : t.getAddress(); //only specify address if we're scanning token transactions - not all are relevant to us.
                fetchTransactionDisposable =
                        fetchTransactionsInteract.fetchNetworkTransactions(network, wallet.getValue(), t.lastBlockCheck, userAddress)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(transactions -> onUpdateTransactions(transactions, t), this::onError, this::checkTransactionQueue);
            }
            else
            {
                Log.d("TOKEN", "Completed Queue");
            }
        }
    }

    public LiveData<Wallet> defaultWallet() {
        return wallet;
    }

    public LiveData<Transaction[]> transactions() {
        return transactions;
    }
    public LiveData<Transaction[]> newTransactions() { return newTransactions; }
    public LiveData<Boolean> showEmpty() { return showEmpty; }
    public LiveData<Boolean> clearAdapter() { return clearAdapter; }
    public LiveData<Boolean> refreshAdapter() { return refreshAdapter; }
    public LiveData<Boolean> queryVisibility() { return queryVisibility; }

    public void prepare()
    {
        progress.postValue(true);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    /**
     * 1. Get all transactions on wallet address.
     * First check wallet address is still valid (user may have restarted process)
     */
    private void fetchTransactions(Wallet wallet)
    {
        showEmpty.postValue(false);
        hasNewTransactions = false;
        //first load transactions from storage, then start the event timer once loaded
        if (fetchTransactionDisposable == null)
        {
            Log.d(TAG, "Fetch start");

            fetchTransactionDisposable =
                    fetchTransactionsInteract.fetchCached(wallet)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onTransactions, this::onError, this::startEventTimer);
        }
    }

    @Override
    public void onError(Throwable throwable)
    {
        super.onError(throwable);
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
        {
            fetchTransactionDisposable.dispose();
        }
        fetchTransactionDisposable = null;
        showEmpty.postValue(false);
    }

    /**
     * 1a. Store the transactions we obtained in step 1 locally
     * @param transactions transaction array returned from query
     */
    private void onTransactions(Transaction[] transactions) {
        Map<String, Transaction> txMap = new HashMap<>();
        for (Transaction tx : transactions)
        {
            if (!txMap.containsKey(tx.hash))
            {
                txMap.put(tx.hash, tx);
            }
        }

        this.transactions.postValue(txMap.values().toArray(new Transaction[0]));
        fetchTransactionDisposable = null;
    }

    private void onUpdateTransactions(Transaction[] transactions, Token token)
    {
        Log.d("TRANSACTION", "Queried for " + token.tokenInfo.name + " : " + transactions.length + " Network transactions");
        Map<String, Transaction> txMap = new HashMap<>();
        for (Transaction tx : transactions)
        {
            if (!txMap.containsKey(tx.hash))
            {
                Long blockNumber = Long.valueOf(tx.blockNumber);
                txMap.put(tx.hash, tx);
                if (blockNumber > token.lastBlockCheck) token.lastBlockCheck = blockNumber;
            }
        }

        if (txMap.size() > 0)
        {
            Log.d(TAG, "Found " + transactions.length + " Network transactions");
            Collection<Transaction> txList = txMap.values();
            newTransactions.postValue(txList.toArray(new Transaction[0]));
            //store new transactions, so they will appear in the transaction view, then update the view
            disposable = fetchTransactionsInteract.storeTransactions(wallet.getValue(), txList.toArray(new Transaction[0]))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(txs -> siftUnknownTransactions(txs, token), this::onError);
            addTokenInteract.updateBlockRead(token, defaultWallet().getValue());
        }

        fetchTransactionDisposable = null;
    }

    //run through the newly received tokens from a currency and see if there's any unknown tokens
    private void siftUnknownTransactions(Transaction[] transactions, Token token)
    {
        if (token.isEthereum()) //only get unknown transactions for base accounts
        {
            fetchTransactionDisposable = setupTokensInteract.getUnknownTokens(transactions, tokensService)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(unknownTokenAddresses -> queryUnknownTokens(unknownTokenAddresses, token.tokenInfo.chainId), this::onError);
        }
    }

    /**
     * This function gets called once after the sift Single has completed. For every contract it gets, it updates the service.
     * The token view will be updated continuously while a wallet with a large number of tokens is first being imported.
     * @param unknownTokens
     */
    private void queryUnknownTokens(List<String> unknownTokens, int chainId)
    {
        fetchTransactionDisposable = Observable.fromIterable(unknownTokens)
                .flatMap(address -> setupTokensInteract.addToken(address, chainId)) //fetch tokenInfo
                .flatMap(fetchTransactionsInteract::queryInterfaceSpecForService)
                .flatMap(tokenInfo -> addTokenInteract.add(tokenInfo, tokensService.getInterfaceSpec(tokenInfo.address), defaultWallet().getValue())) //add to database
                .flatMap(token -> addTokenInteract.addTokenFunctionData(token, assetDefinitionService))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(this::updateTokenService, this::onError);
    }

    /**
     * each time we get a new token, add it to the service, the main token view will update with the new token after a refresh
     * @param token the new token
     */
    private void updateTokenService(Token token)
    {
        tokensService.addToken(token);
    }

    public void receiveVisibility(boolean isVisible)
    {
        if (isVisible)
        {
            //do next update
            //checkTokenQueue();
        }
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet.postValue(wallet);
        fetchTransactions(wallet);
    }

    public void showDetails(Context context, Transaction transaction) {
        transactionDetailRouter.open(context, transaction);
    }

    public void showHome(Context context) {
        homeRouter.open(context, true);
    }

    public void openDeposit(Context context, Uri uri) {
        externalBrowserRouter.open(context, uri);
    }

    private void finishTransactionScanCycle()
    {
        progress.postValue(false); //ensure spinner is off on completion (in case user forced update)
        fetchTransactionDisposable = null;
        if (hasNewTransactions)
        {
            refreshAdapter.postValue(true);
            hasNewTransactions = false;
        }

        Log.d(TAG, "Finish");
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
        {
            fetchTransactionDisposable.dispose();
        }
        fetchTransactionDisposable = null; //ready to restart the fetch
        queryVisibility.postValue(true);
    }

    /**
     * Detect any termination function. If we see one of these there's no need to do any further checking for this token
     * @param //transactions
     * @return
     */
//    private Transaction[] checkForContractTerminator(Transaction[] transactions)
//    {
//        if (transactions.length == 0) return transactions;
//
//        for (int index = transactions.length - 1; index >= 0; index--)
//        {
//            Transaction tx = transactions[index];
//            TransactionContract ct = tx == null ?
//                    null : tx.getOperation();
//            if (ct != null && ct.getOperationType() == TransactionType.TERMINATE_CONTRACT)
//            {
//                Token t = tokensService.getToken(tx.chainId, tx.to);
//                if (t != null) setupTokensInteract.terminateToken(tokensService.getToken(t.tokenInfo.chainId, t.getAddress()),
//                                                                  defaultWallet().getValue(), defaultNetwork().getValue());
//                tokensService.setTerminationFlag();
//                break;
//            }
//        }
//        return transactions;
//    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public FetchTransactionsInteract provideTransactionsInteract()
    {
        return fetchTransactionsInteract;
    }
}
