package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.stormbird.wallet.entity.ERC875ContractTransaction;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.TransactionContract;
import io.stormbird.wallet.entity.TransactionType;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.AddTokenInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FetchTransactionsInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.SetupTokensInteract;
import io.stormbird.wallet.router.ExternalBrowserRouter;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.TransactionDetailRouter;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;

import static io.stormbird.wallet.entity.TransactionDecoder.isEndContract;

public class TransactionsViewModel extends BaseViewModel
{
    private static final long FETCH_TRANSACTIONS_INTERVAL = 12 * DateUtils.SECOND_IN_MILLIS;
    private static final int  MAX_TOKEN_CONTRACT_FETCH_PER_UPDATE_CYCLE = 20;
    private static final String TAG = "TVM";

    private final MutableLiveData<NetworkInfo> network = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showEmpty = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> clearAdapter = new MutableLiveData<>();

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
    private Transaction[] txArray;
    private Map<String, Transaction> txMap = new ConcurrentHashMap<>();
    private int transactionCount;
    private long lastBlock = 0;
    private boolean restoreRequired = false;
    private boolean immediateCycleStart = false;//this is used to flag the need to start a new cycle,
                                                // usually only seen when importing a new wallet with a lot of transactions on it

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

        txArray = null;
        txMap.clear();
        setupTokensInteract.clearAll();
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
    public LiveData<Boolean> showEmpty() { return showEmpty; }
    public LiveData<Boolean>  clearAdapter() { return clearAdapter; }

    public void prepare()
    {
        progress.postValue(true);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    /**
     * 1. Get all transactions on wallet address.
     * First check wallet address is still valid (user may have restarted process)
     * @param shouldShowProgress
     */
    private void fetchTransactions(boolean shouldShowProgress) {
        showEmpty.postValue(false);
        if (wallet.getValue() != null)
        {
            if (fetchTransactionDisposable == null)
            {
                transactionCount = 0;
                Log.d(TAG, "Fetch start");
                setupTokensInteract.setWalletAddr(wallet.getValue().address);

                fetchTransactionDisposable =
                        fetchTransactionsInteract.fetchCached(network.getValue(), wallet.getValue())
                                .flatMap(this::checkForContractTerminator)
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
     * @param transactions
     */
    private void onTransactions(Transaction[] transactions) {
        Log.d(TAG, "Found " + transactions.length + " Cached transactions");
        txArray = transactions;
    }

    /**
     * 2. After fetching the stored transactions we display them so there's not a blank screen for too long.
     * After display, fetch any new transactions
     */
    private void fetchNetworkTransactions()
    {
        if (restoreRequired)
        {
            lastBlock = 0;
            txArray = new Transaction[0];
            txMap.clear();
            restoreRequired = false;
        }
        else
        {
            updateDisplay(txArray);
        }

        Log.d(TAG, "Fetching network transactions.");
        //now fetch new transactions on main account
        //find block number of last transaction
        fetchTransactionDisposable =
                fetchTransactionsInteract.fetchNetworkTransactions(wallet.getValue(), lastBlock)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onUpdateTransactions, this::onError, this::enumerateTokens);
    }

    /**
     * 2a. Receive transactions and add to transaction map
     * @param transactions
     */
    private void onUpdateTransactions(Transaction[] transactions) {
        Log.d(TAG, "Found " + transactions.length + " Network transactions");
        //check against existing transactions
        List<Transaction> newTxs = new ArrayList<Transaction>();
        for (Transaction tx : transactions)
        {
            if (!txMap.containsKey(tx.hash))
            {
                txMap.put(tx.hash, tx);
                newTxs.add(tx);
            }
        }

        if (newTxs.size() > 0)
        {
            updateDisplay(newTxs.toArray(new Transaction[0]));
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
        txArray = txMap.values().toArray(new Transaction[txMap.size()]);
        transactionCount += txArray.length;

        //Fetch all stored tokens, but no ethd
        //TODO: after the map addTokenToChecklist stage we should be using a reduce instead of filtering in the fetch function
        fetchTransactionDisposable = Observable.fromCallable(tokensService::getAllTokens)
                .flatMapIterable(token -> token)
                .filter(token -> !token.isEthereum())
                .filter(token -> !token.isTerminated())
                .map(this::addTokenToChecklist)
                .map(token -> fetchTransactionsInteract.fetch(new Wallet(token.tokenInfo.address), token, tokensService.getLastBlock(token))) //single that fetches all the tx's from etherscan for each token from fetchSequential
                .map(tokenTransactions -> setupTokensInteract.processTokenTransactions(defaultWallet().getValue(), tokenTransactions.blockingLast(), tokensService)) //process these into a map
                .map(transactions -> fetchTransactionsInteract.storeTransactionsObservable(network.getValue(), wallet.getValue(), transactions.blockingLast()))
                .map(transactions -> removeFromMapTx(transactions.blockingLast()))
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::updateDisplay, this::onError, this::siftUnknownTransactions);
    }

    private Token addTokenToChecklist(Token token)
    {
        setupTokensInteract.addTokenToMap(token);
        return token;
    }

    //run through what remains in the map, see if there are any unknown tokens
    //if we find unknown tokens fetch them and add to the token watch list
    private void siftUnknownTransactions()
    {
        immediateCycleStart = false;
        //add in the XML contract address to list of unknowns to fetch if we don't have it already
        setupTokensInteract.setupUnknownList(tokensService, assetDefinitionService.getAllContracts(network.getValue().chainId));

        fetchTransactionDisposable = setupTokensInteract.processRemainingTransactions(txMap.values().toArray(new Transaction[txMap.size()]), tokensService) //patches tx's and returns unknown contracts
                .flatMap(transactions -> fetchTransactionsInteract.storeTransactions(network.getValue(), wallet.getValue(), transactions).toObservable()) //store patched TX
                .map(setupTokensInteract::getUnknownContracts) //emit a list of string addresses
                .map(this::limitToChunk)
                .flatMap(setupTokensInteract::addTokens) //fetch tokenInfo
                .flatMap(addTokenInteract::add) //add to database
                .subscribeOn(Schedulers.io())
                .subscribe(this::updateTransactions, this::onError, this::storeUnprocessedTx);

        if (transactionCount == 0)
        {
            Log.d(TAG, "No transactions");
            progress.postValue(false);
            showEmpty.postValue(true);
        }
    }

    private List<String> limitToChunk(List<String> strings)
    {
        if (strings.size() > MAX_TOKEN_CONTRACT_FETCH_PER_UPDATE_CYCLE)
        {
            strings = strings.subList(0, MAX_TOKEN_CONTRACT_FETCH_PER_UPDATE_CYCLE);
            immediateCycleStart = true;
        }
        return strings;
    }

    private void updateTransactions(Token[] tokens)
    {
        tokensService.addTokens(tokens); //add tokens to the token map in the service
    }

    //update the display for newly fetched tokens
    private void updateDisplay(Transaction[] transactions)
    {
        if (transactions.length > 0)
        {
            this.transactions.postValue(transactions);
        }
    }

    private void storeUnprocessedTx()
    {
        //store any remaining transactions
        Transaction[] transactions = txMap.values().toArray(new Transaction[txMap.size()]);
        fetchTransactionDisposable = fetchTransactionsInteract.storeTransactions(network.getValue(), wallet.getValue(), transactions).toObservable()
                .subscribeOn(Schedulers.io())
                .subscribe(this::completeCycle, this::onError, this::scanForTerminatedTokens);
    }

    private void completeCycle(Transaction[] transactions)
    {
        progress.postValue(false); //ensure spinner is off on completion (in case user forced update)
    }

    private Transaction[] removeFromMapTx(Transaction[] transactions)
    {
        Log.d(TAG, "GOT: " + transactions.length );
        //first remove all these transactions from the network + cached list
        for (Transaction t : transactions)
        {
            txMap.remove(t.hash);
        }

        Log.d(TAG, "Remaining unknown: " + txMap.size() );
        transactionCount += transactions.length;
        return transactions;
    }

    private Observable<Transaction[]> removeFromMap(Transaction[] transactions)
    {
        return Observable.fromCallable(() -> {
            Log.d(TAG, "GOT: " + transactions.length );
            //first remove all these transactions from the network + cached list
            for (Transaction t : transactions)
            {
                txMap.remove(t.hash);
            }

            Log.d(TAG, "Remaining unknown: " + txMap.size() );
            transactionCount += transactions.length;
            return transactions;
        });
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
        if (immediateCycleStart)
        {
            handler.removeCallbacks(startFetchTransactionsTask);
            handler.postDelayed(
                    startFetchTransactionsTask,
                    100);
        }
        else if (!isVisible)
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
        this.wallet.setValue(wallet);
        setupTokensInteract.setWalletAddr(wallet.address);
        fetchTransactions(true);
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

    private final Runnable startFetchTransactionsTask = () -> this.fetchTransactions(false);

    //Called from the activity when it comes into view,
    //start updating transactions
    public void startTransactionRefresh() {
        isVisible = true;
        if (fetchTransactionDisposable == null || fetchTransactionDisposable.isDisposed()) //ready to restart the fetch == null || fetchTokensDisposable.isDisposed())
        {
            checkIfRegularUpdateNeeded();
        }
        /*if (txArray != null)
        {
            this.transactions.postValue(txArray);
        }*/
    }

    public void setVisibility(boolean visibility) {
        isVisible = visibility;
    }

    private void scanForTerminatedTokens()
    {
        fetchTransactionDisposable = null;
        checkIfRegularUpdateNeeded();

        //run through the map and see if there were any tokens that have been terminated
        handleTerminatedContracts = Observable.fromCallable(tokensService::getTerminationList)
                .flatMapIterable(address -> address)
                .map(address -> setupTokensInteract.terminateToken(tokensService.getToken(address), defaultWallet().getValue(), defaultNetwork().getValue()))
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::onTokenForTermination, this::onScanError, this::wipeTerminationList);
    }

    private void onScanError(Throwable throwable)
    {
        handleTerminatedContracts.dispose();
    }

    private void wipeTerminationList()
    {
        if (handleTerminatedContracts != null && !handleTerminatedContracts.isDisposed()) handleTerminatedContracts.dispose();
        tokensService.clearTerminationList();
    }

    private void onTokenForTermination(Token token)
    {
        System.out.print("Terminated: " + token.getAddress());
    }

    /**
     * Detect any termination function. If we see one of these there's no need to do any further checking for this token
     * @param transactions
     * @return
     */
    private Observable<Transaction[]> checkForContractTerminator(Transaction[] transactions)
    {
        return Observable.fromCallable(() -> {
            for (Transaction tx : transactions)
            {
                if (tx != null)
                {
                    if (checkForIllegalType(tx)) break;
                    if (tx.input != null && tx.input.length() == 10)
                    {
                        Token t = tokensService.getToken(tx.to);
                        if (t != null && !t.isTerminated()
                                && isEndContract(tx.input))
                        {
                            //write to database this contract is terminated
                            setupTokensInteract.terminateToken(tokensService.getToken(t.getAddress()),
                                                               defaultWallet().getValue(), defaultNetwork().getValue());
                        }
                    }
                }
            }
            return transactions;
        });
    }

    private boolean checkForIllegalType(Transaction tx)
    {
        if (tx.operations != null && tx.operations.length > 0 && tx.operations[0] != null)
        {
            TransactionContract ct = tx.operations[0].contract;
            if (ct instanceof ERC875ContractTransaction && ((ERC875ContractTransaction)ct).operation == TransactionType.ILLEGAL_VALUE)
            {
                restoreRequired = true;
            }
        }

        return restoreRequired;
    }
}
