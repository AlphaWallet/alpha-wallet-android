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

    private Map<String, Transaction> txMap = new ConcurrentHashMap<>();
    private List<Transaction> newTransactionList = new ArrayList<>();
    private int transactionCount;
    private boolean hasNewTransactions;
    private boolean isVisible;

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

    public void setVisibility(boolean isVisible)
    {
        if (this.isVisible == false && isVisible == true)
        {
            eventQueue.clear();
            long currentTime = System.currentTimeMillis();
            //reset all the timers
            disposable = Observable.fromCallable(tokensService::getAllLiveTokens)
                    .flatMapIterable(token -> token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(token -> token.setTransactionUpdateTime(currentTime, true), this::onError);
        }
        this.isVisible = isVisible;
    }

    public void abortAndRestart(boolean refreshCache)
    {
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
        {
            fetchTransactionDisposable.dispose();
        }

        fetchTransactionDisposable = null;

        txMap.clear();
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
        if (isVisible)
        {
            disposable = Observable.fromCallable(tokensService::getAllLiveTokens)
                    .flatMapIterable(token -> token)
                    .filter(Token::requiresTransactionRefresh)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::addToEventQueue, this::onError, this::checkTransactionQueue);
        }
    }

    private void addToEventQueue(Token token)
    {
        //TODO:
        Log.d("TRANSACTION", "Queue Sz: " + eventQueue.size());
        switch (eventQueue.size())
        {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                Log.d("TRANSACTION", "Adding to queue: " + token.getFullName());
                eventQueue.add(token);
                break;
            case 5:
            case 6:
            case 7:
            case 8:
                if (token.hasRealValue() || token.isEthereum())
                {
                    Log.d("TRANSACTION", "Adding to queue[R]: " + token.getFullName());
                    eventQueue.add(token);
                }
                break;
            default:
                if (token.isEthereum() && token.hasRealValue())
                {
                    Log.d("TRANSACTION", "Adding to queue[RR]: " + token.getFullName());
                    eventQueue.add(token);
                }
                break;
        }
    }

    private void checkTransactionQueue()
    {
        if (isVisible && fetchTransactionDisposable == null)
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

    public void restartIfRequired()
    {
        if (defaultWallet().getValue() == null)
        {
            prepare();
        }
        else
        {
            fetchTransactions(defaultWallet().getValue());
        }
    }

    /**
     * 1. Get all transactions on wallet address.
     * First check wallet address is still valid (user may have restarted process)
     */
    private void fetchTransactions(Wallet wallet)
    {
        showEmpty.postValue(false);
        hasNewTransactions = false;
        if (fetchTransactionDisposable == null)
        {
            transactionCount = 0;
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
        this.transactions.postValue(transactions);

        transactionCount = transactions.length;

        for (Transaction tx : transactions)
        {
            txMap.put(tx.hash, tx);
            Token t = tokensService.getToken(tx.chainId, tx.getTokenAddress(wallet.getValue().address));
            if (t != null && Long.valueOf(tx.blockNumber) > t.lastBlockCheck)
            {
                t.lastBlockCheck = Long.valueOf(tx.blockNumber); //shouldn't need this
            }
        }

        fetchTransactionDisposable = null;
    }

    /**
     * 2. After fetching the stored transactions we display them so there's not a blank screen for too long.
     * After display, fetch any new transactions
     */
//    private void fetchNetworkTransactions()
//    {
//        Log.d(TAG, "Fetching network transactions.");
//        //now fetch new transactions on main account
//        //find block number of last transaction
//        fetchTransactionDisposable =
//                fetchTransactionsInteract.fetchNetworkTransactions(network.getValue(), wallet.getValue(), latestBlock, null)
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(this::onUpdateTransactions, this::onError, this::siftUnknownTransactions);
//    }

    /**
     * 2a. Receive transactions and add to transaction map
     * @param transactions
     */
    private void onUpdateTransactions(Transaction[] transactions, Token token) {
        storeNewTransactions(transactions, token);
    }

    private void storeNewTransactions(Transaction[] transactions, Token token)
    {
        Log.d("TRANSACTION", "Queried for " + token.tokenInfo.name + " : " + transactions.length + " Network transactions");
        newTransactionList.clear();
        for (Transaction tx : transactions)
        {
            if (!txMap.containsKey(tx.hash))
            {
                Long blockNumber = Long.valueOf(tx.blockNumber);
                txMap.put(tx.hash, tx);
                newTransactionList.add(tx);
                if (blockNumber > token.lastBlockCheck) token.lastBlockCheck = blockNumber;
                transactionCount++;
                hasNewTransactions = true;
            }
        }

        if (newTransactionList.size() > 0)
        {
            Log.d(TAG, "Found " + transactions.length + " Network transactions");
            newTransactions.postValue(newTransactionList.toArray(new Transaction[0]));
            //store new transactions, so they will appear in the transaction view, then update the view
            disposable = fetchTransactionsInteract.storeTransactions(wallet.getValue(), newTransactionList.toArray(new Transaction[0]))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateDisplay, this::onError);
            addTokenInteract.updateBlockRead(token, defaultWallet().getValue());
        }

        fetchTransactionDisposable = null;
    }

    //run through what remains in the map, see if there are any unknown tokens
    //if we find unknown tokens fetch them and add to the token watch list
    private void siftUnknownTransactions()
    {
        fetchTransactionDisposable = setupTokensInteract.getUnknownTokens(newTransactionList, tokensService)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(this::queryUnknownTokens, this::onError);
    }

    /**
     *  3. Once we have fetched all user account related transactions we need to fill in all the contract transactions
     *   This functions performs the following tasks:
     *   - fetch all cached tokens sequentially
     *
     *   --> on each token we fetch do the following:
     *   - add each token to a local map
     *   - fetch all transactions on the token contract
     *   - process those transactions to see if user wallet is involved with any
     *   - store the updated transactions
     *   - remove any updated transactions from the map fetched in the previous two steps
     *   - refresh the display with updated transactions
     *
     *
     *   ---------------------------
     *   finally go to siftUnknownTransactions
     */
//    private void enumerateTokens()
//    {
//        //stop the spinner
//        progress.postValue(false);
//        Log.d(TAG, "Enumerating tokens");
//
//        if (wallet.getValue() != null)
//        {
//            //TODO!!!: Sift for terminated tokens here
//            //Fetch all stored tokens, but no eth
//            fetchTransactionDisposable = Observable.fromCallable(tokensService::getAllTokens)
//                    .flatMapIterable(token -> token)
//                    .filter(token -> !token.isEthereum())
//                    .filter(token -> !token.isTerminated())
//                    .filter(token -> !token.independentUpdate()) //don't scan ERC721 transactions
//                    .concatMap(this::checkSpec)
//                    .filter(Token::checkIntrinsicType) //Don't scan tokens that appear to be setup incorrectly
//                    .concatMap(token -> fetchTransactionsInteract.fetchNetworkTransactions(network.getValue(), new Wallet(token.getAddress()), token.lastBlockCheck, wallet.getValue().address)) //single that fetches all the tx's from etherscan for each token from fetchSequential
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(Schedulers.io())
//                    .subscribe(transactions -> onContractTransactions(transactions, token), this::onError, this::finishTransactionScanCycle);
//        }
//        else
//        {
//            finishTransactionScanCycle();
//        }
//    }

    private Observable<Token> checkSpec(Token token)
    {
        return Observable.fromCallable(() -> {
            if (token.checkIntrinsicType()) return token;

            ContractType fromService = tokensService.getInterfaceSpec(token.getAddress());
            token.setInterfaceSpec(fromService);

            if (!token.isBad() && !token.checkIntrinsicType())
            {
                setupTokensInteract.tokenHasBadSpec(token.getAddress());
            }

            return token;
        });
    }

    private void onContractTransactions(Transaction[] transactions, Token token)
    {
        storeNewTransactions(transactions, token);
    }

    /**
     * This function gets called once after the sift Single has completed. For every contract it gets, it updates the service.
     * The token view will be updated continuously while a wallet with a large number of tokens is first being imported.
     * @param unknownTokens
     */
    private void queryUnknownTokens(List<String> unknownTokens)
    {
//        fetchTransactionDisposable = Observable.fromIterable(unknownTokens)
//                .flatMap(address -> setupTokensInteract.addToken(address, network.getValue().chainId)) //fetch tokenInfo
//                .flatMap(fetchTransactionsInteract::queryInterfaceSpecForService)
//                .flatMap(tokenInfo -> addTokenInteract.add(tokenInfo, tokensService.getInterfaceSpec(tokenInfo.address), defaultWallet().getValue())) //add to database
//                .flatMap(token -> addTokenInteract.addTokenFunctionData(token, assetDefinitionService))
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .subscribe(this::updateTokenService, this::onError);
//
//        if (transactionCount == 0)
//        {
//            Log.d(TAG, "No transactions");
//            progress.postValue(false);
//            showEmpty.postValue(true);
//        }
    }

    /**
     * each time we get a new token, add it to the service, the main token view will update with the new token after a refresh
     * @param token the new token
     */
    private void updateTokenService(Token token)
    {
        tokensService.addToken(token);
    }

    //update the display for newly fetched tokens
    private void updateDisplay(Transaction[] transactions)
    {
        Log.d(TAG,"New Network Tx: " + transactions.length);
    }

    public void forceUpdateTransactionView()
    {
        if (fetchTransactionDisposable == null)
        {
            //fetchTransactions(true);
        }
        else
        {
            //post a waiting dialog to appease the user
            //progress.postValue(true);
            Log.d(TAG, "must already be running, wait until termination");
        }
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

    //Called from the activity when it comes into view,
    //start updating transactions
    public void startTransactionRefresh() {
        forceUpdateTransactionView();

        //String address = checkQueue.poll();
//        if (address != null && fetchTransactionDisposable == null) //ready to restart the fetch == null || fetchTokensDisposable.isDisposed())
//        {
//            forceUpdateTransactionView();
//        }
    }

    private void finishTransactionScanCycle()
    {
        txMap.clear();
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
