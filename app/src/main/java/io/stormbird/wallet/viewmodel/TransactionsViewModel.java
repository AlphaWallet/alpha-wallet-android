package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
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
import io.stormbird.wallet.service.TokensService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.stormbird.wallet.entity.TransactionDecoder.isEndContract;

public class TransactionsViewModel extends BaseViewModel
{
    private static final long FETCH_TRANSACTIONS_INTERVAL = 12 * DateUtils.SECOND_IN_MILLIS;
    private static final String TAG = "TVM";

    private final MutableLiveData<NetworkInfo> network = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showEmpty = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> clearAdapter = new MutableLiveData<>();
    private final MutableLiveData<Boolean> refreshAdapter = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> newTransactions = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenInteract addTokenInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private final TransactionDetailRouter transactionDetailRouter;
    private final ExternalBrowserRouter externalBrowserRouter;
    private final HomeRouter homeRouter;

    @Nullable
    private Disposable fetchTransactionDisposable;
    @Nullable
    private Disposable handleTerminatedContracts;

    private Handler handler = new Handler();

    private boolean isVisible = false;
    private Map<String, Transaction> txMap = new ConcurrentHashMap<>();
    private List<Transaction> txContractList = new ArrayList<>();
    private int transactionCount;
    private long latestBlock = 0;
    private boolean hasNewTransactions;

    TransactionsViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            FetchTokensInteract fetchTokensInteract,
            SetupTokensInteract setupTokensInteract,
            AddTokenInteract addTokenInteract,
            TransactionDetailRouter transactionDetailRouter,
            ExternalBrowserRouter externalBrowserRouter,
            HomeRouter homeRouter,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.transactionDetailRouter = transactionDetailRouter;
        this.externalBrowserRouter = externalBrowserRouter;
        this.homeRouter = homeRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();

        handler.removeCallbacks(startFetchTransactionsTask);

        isVisible = false;
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
        {
            fetchTransactionDisposable.dispose();
        }
    }

    public void abortAndRestart(boolean refreshCache)
    {
        handler.removeCallbacks(startFetchTransactionsTask);
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
        {
            fetchTransactionDisposable.dispose();
        }

        fetchTransactionDisposable = null;

        txMap.clear();
    }

    public LiveData<NetworkInfo> defaultNetwork() {
        return network;
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

    public void prepare()
    {
        progress.postValue(true);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    public void restartIfRequired()
    {
        if (defaultNetwork().getValue() == null
                || defaultWallet().getValue() == null)
        {
            prepare();
        }
        else
        {
            startTransactionRefresh();
        }
    }

    /**
     * 1. Get all transactions on wallet address.
     * First check wallet address is still valid (user may have restarted process)
     * @param shouldShowProgress whether to display progress spinner
     */
    private void fetchTransactions(boolean shouldShowProgress) {
        showEmpty.postValue(false);
        latestBlock = 0;
        hasNewTransactions = false;
        if (wallet.getValue() != null)
        {
            if (fetchTransactionDisposable == null)
            {
                transactionCount = 0;
                Log.d(TAG, "Fetch start");

                fetchTransactionDisposable =
                        fetchTransactionsInteract.fetchCached(network.getValue(), wallet.getValue())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(this::onTransactions, this::onError, this::fetchNetworkTransactions);
            }
        }
        else
        {
            Log.d(TAG, "No wallet");
            disposable = findDefaultWalletInteract
                    .find()
                    .subscribe(this::onDefaultWallet, this::onError);
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
            if (Long.valueOf(tx.blockNumber) > latestBlock) latestBlock = Long.valueOf(tx.blockNumber);
        }
    }

    /**
     * 2. After fetching the stored transactions we display them so there's not a blank screen for too long.
     * After display, fetch any new transactions
     */
    private void fetchNetworkTransactions()
    {
        Log.d(TAG, "Fetching network transactions.");
        //now fetch new transactions on main account
        //find block number of last transaction
        fetchTransactionDisposable =
                fetchTransactionsInteract.fetchNetworkTransactions(wallet.getValue(), latestBlock, null)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onUpdateTransactions, this::onError, this::enumerateTokens);
    }

    /**
     * 2a. Receive transactions and add to transaction map
     * @param transactions
     */
    private void onUpdateTransactions(Transaction[] transactions) {
        //check against existing transactions
        List<Transaction> newTxs = new ArrayList<Transaction>();
        for (Transaction tx : transactions)
        {
            if (!txMap.containsKey(tx.hash))
            {
                txMap.put(tx.hash, tx);
                newTxs.add(tx);
                if (Long.valueOf(tx.blockNumber) > latestBlock) latestBlock = Long.valueOf(tx.blockNumber);
                transactionCount++;
                hasNewTransactions = true;
            }
        }

        if (newTxs.size() > 0)
        {
            Log.d(TAG, "Found " + transactions.length + " Network transactions");
            newTransactions.postValue(newTxs.toArray(new Transaction[0]));
            //store new transactions, so they will appear in the transaction view, then update the view
            disposable = fetchTransactionsInteract.storeTransactions(network.getValue(), wallet.getValue(), newTxs.toArray(new Transaction[0]))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateDisplay, this::onError);
        }
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
    private void enumerateTokens()
    {
        //stop the spinner
        progress.postValue(false);
        Log.d(TAG, "Enumerating tokens");
        //transactionCount += txMap.size();
        txContractList.clear();

        if (wallet.getValue() != null)
        {
            //Fetch all stored tokens, but no eth
            fetchTransactionDisposable = Observable.fromCallable(tokensService::getAllTokens)
                    .flatMapIterable(token -> token)
                    .filter(token -> !token.isEthereum())
                    .filter(token -> !token.isTerminated())
                    .filter(token -> (!token.independentUpdate() && !token.isERC20())) //don't scan for ERC721 or ERC20 internal transactions
                    .concatMap(this::checkSpec)
                    .filter(Token::checkIntrinsicType) //Don't scan tokens that appear to be setup incorrectly
                    .concatMap(token -> fetchTransactionsInteract.fetchNetworkTransactions(new Wallet(token.getAddress()), token.lastBlockCheck, wallet.getValue().address)) //single that fetches all the tx's from etherscan for each token from fetchSequential
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(this::onContractTransactions, this::onError, this::siftUnknownTransactions);
        }
        else
        {
            siftUnknownTransactions();
        }
    }

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

    private void onContractTransactions(Transaction[] transactions)
    {
        disposable = fetchTransactionsInteract.storeTransactions(network.getValue(), wallet.getValue(), transactions)
                .map(this::setLatestBlock)
                .map(this::removeFromMapTx)
                .map(this::checkForContractTerminator)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(this::updateTransactionMap, this::onError);
    }

    private Transaction[] setLatestBlock(Transaction[] transactions)
    {
        if (transactions.length > 0)
        {
            Transaction lastTx = transactions[transactions.length - 1];
            Token t = tokensService.getToken(lastTx.to);
            if (t != null)
            {
                t.lastBlockCheck = Long.parseLong(lastTx.blockNumber);
                addTokenInteract.updateBlockRead(t, defaultNetwork().getValue(), defaultWallet().getValue());
            }
            hasNewTransactions = true;
        }

        return transactions;
    }

    private void updateTransactionMap(Transaction[] transactions)
    {
        txContractList.addAll(Arrays.asList(transactions));
    }

    //run through what remains in the map, see if there are any unknown tokens
    //if we find unknown tokens fetch them and add to the token watch list
    private void siftUnknownTransactions()
    {
        newTransactions.postValue(txContractList.toArray(new Transaction[0]));
        txContractList.clear();

        fetchTransactionDisposable = fetchTransactionsInteract.storeTransactions(network.getValue(), wallet.getValue(), txMap.values().toArray(new Transaction[0]))
                .flatMap(transactions -> setupTokensInteract.getUnknownTokens(transactions, tokensService, txMap))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(this::queryUnknownTokens, this::onError);
    }

    /**
     * This function gets called once after the sift Single has completed. For every contract it gets, it updates the service.
     * The token view will be updated continuously while a wallet with a large number of tokens is first being imported.
     * @param unknownTokens
     */
    private void queryUnknownTokens(List<String> unknownTokens)
    {
        fetchTransactionDisposable = Observable.fromIterable(unknownTokens)
                .flatMap(setupTokensInteract::addToken) //fetch tokenInfo
                .flatMap(fetchTransactionsInteract::queryInterfaceSpecForService)
                .flatMap(tokenInfo -> addTokenInteract.add(tokenInfo, tokensService.getInterfaceSpec(tokenInfo.address), defaultWallet().getValue())) //add to database
                .flatMap(token -> addTokenInteract.addTokenFunctionData(token, assetDefinitionService))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(this::updateTokenService, this::onError, this::finishTransactionScanCycle);

        if (transactionCount == 0)
        {
            Log.d(TAG, "No transactions");
            progress.postValue(false);
            showEmpty.postValue(true);
        }
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

    private Transaction[] removeFromMapTx(Transaction[] transactions)
    {
        //first remove all these transactions from the network + cached list
        for (Transaction t : transactions)
        {
            txMap.remove(t.hash);
        }

        transactionCount += transactions.length;
        return transactions;
    }

    public void forceUpdateTransactionView()
    {
        if (fetchTransactionDisposable == null)
        {
            handler.removeCallbacks(startFetchTransactionsTask);
            fetchTransactions(true);
        }
        else
        {
            //post a waiting dialog to appease the user
            //progress.postValue(true);
            Log.d(TAG, "must already be running, wait until termination");
        }
    }

    private void checkIfRegularUpdateNeeded()
    {
        txMap.clear();
        if (!isVisible)
        {
            //no longer any need to refresh
            Log.d(TAG, "Finish");
            if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
            {
                fetchTransactionDisposable.dispose();
            }
            fetchTransactionDisposable = null; //ready to restart the fetch

            handler.removeCallbacks(startFetchTransactionsTask);
        }
        else if (fetchTransactionDisposable == null)
        {
            handler.removeCallbacks(startFetchTransactionsTask);
            Log.d(TAG, "Delayed start in " + FETCH_TRANSACTIONS_INTERVAL);
            handler.postDelayed(
                    startFetchTransactionsTask,
                    FETCH_TRANSACTIONS_INTERVAL);
        }
        else
        {
            Log.d(TAG, "must already be running, wait until termination");
        }
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        network.setValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet.postValue(wallet);
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

    private final Runnable startFetchTransactionsTask = this::fetchNetworkTransactions;

    //Called from the activity when it comes into view,
    //start updating transactions
    public void startTransactionRefresh() {
        isVisible = true;

        if (fetchTransactionDisposable == null || fetchTransactionDisposable.isDisposed()) //ready to restart the fetch == null || fetchTokensDisposable.isDisposed())
        {
            checkIfRegularUpdateNeeded();
        }
    }

    public void setVisibility(boolean visibility) {
        isVisible = visibility;
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
        checkIfRegularUpdateNeeded();
    }

    /**
     * Detect any termination function. If we see one of these there's no need to do any further checking for this token
     * @param transactions
     * @return
     */
    private Transaction[] checkForContractTerminator(Transaction[] transactions)
    {
        if (transactions.length == 0) return transactions;

        for (int index = transactions.length - 1; index >= 0; index--)
        {
            Transaction tx = transactions[index];
            TransactionContract ct = tx == null ?
                    null : tx.getOperation();
            if (ct != null && ct.getOperationType() == TransactionType.TERMINATE_CONTRACT)
            {
                Token t = tokensService.getToken(tx.to);
                if (t != null) setupTokensInteract.terminateToken(tokensService.getToken(t.getAddress()),
                                                                  defaultWallet().getValue(), defaultNetwork().getValue());
                tokensService.setTerminationFlag();
                break;
            }
        }
        return transactions;
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public FetchTransactionsInteract provideTransactionsInteract()
    {
        return fetchTransactionsInteract;
    }
}
