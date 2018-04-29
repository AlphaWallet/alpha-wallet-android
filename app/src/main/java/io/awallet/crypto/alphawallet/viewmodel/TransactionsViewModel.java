package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.AddTokenInteract;
import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FetchTransactionsInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.SetupTokensInteract;
import io.awallet.crypto.alphawallet.router.ExternalBrowserRouter;
import io.awallet.crypto.alphawallet.router.HomeRouter;
import io.awallet.crypto.alphawallet.router.TransactionDetailRouter;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TransactionsViewModel extends BaseViewModel {
    private static final long FETCH_TRANSACTIONS_INTERVAL = 12 * DateUtils.SECOND_IN_MILLIS;
    private static final String TAG = "TVM";

    private final MutableLiveData<NetworkInfo> network = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showEmpty = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenInteract addTokenInteract;
    private final SetupTokensInteract setupTokensInteract;

    private final TransactionDetailRouter transactionDetailRouter;
    private final ExternalBrowserRouter externalBrowserRouter;
    private final HomeRouter homeRouter;

    @Nullable
    private Disposable fetchTransactionDisposable;
    private Handler handler = new Handler();

    private boolean isVisible = false;
    private Transaction[] txArray;
    private Map<String, Transaction> txMap = new ConcurrentHashMap<>();
    private Map<String, Token> tokenMap = new ConcurrentHashMap<>();
    private String xmlContractAddress = null;
    private String feemasterUrl = null;
    private int transactionCount;

    TransactionsViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            FetchTokensInteract fetchTokensInteract,
            SetupTokensInteract setupTokensInteract,
            AddTokenInteract addTokenInteract,
            TransactionDetailRouter transactionDetailRouter,
            ExternalBrowserRouter externalBrowserRouter,
            HomeRouter homeRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.transactionDetailRouter = transactionDetailRouter;
        this.externalBrowserRouter = externalBrowserRouter;
        this.homeRouter = homeRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        handler.removeCallbacks(startFetchTransactionsTask);

        isVisible = false;
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
        {
            fetchTransactionDisposable.dispose();
        }
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

    public void prepare() {
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
                        fetchTransactionsInteract.fetchCached(wallet.getValue())
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
        for (Transaction tx : txArray)
        {
            txMap.put(tx.hash, tx);
        }
    }

    /**
     * 2. After fetching the stored transactions we display them so there's not a blank screen for too long.
     * After display, fetch any new transactions
     */
    private void fetchNetworkTransactions()
    {
        updateDisplay(txArray);

        Transaction lastTx = null;
        //simple sort on txArray
        if (txArray != null && txArray.length > 1)
        {
            lastTx = txArray[txArray.length - 1];
        }

        Log.d(TAG, "Fetching network transactions.");
        //now fetch new transactions on main account
        //find block number of last transaction
        fetchTransactionDisposable =
                fetchTransactionsInteract.fetchNetworkTransactions(wallet.getValue(), lastTx)
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
        for (Transaction tx : transactions)
        {
            txMap.put(tx.hash, tx);
        }

        updateDisplay(transactions);
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

        //Fetch all stored tokens, but no eth
        //TODO: after the map addTokenToChecklist stage we should be using a reduce instead of filtering in the fetch function
        fetchTransactionDisposable = fetchTokensInteract
                .fetchSequentialNoEth(wallet.getValue())
                .map(this::addTokenToChecklist)
                .flatMap(token -> fetchTransactionsInteract.fetch(new Wallet(token.tokenInfo.address), token)) //single that fetches all the tx's from etherscan for each token from fetchSequential
                .flatMap(tokenTransactions -> setupTokensInteract.processTokenTransactions(defaultWallet().getValue(), tokenTransactions)) //process these into a map
                .flatMap(transactions -> fetchTransactionsInteract.storeTransactionsObservable(network.getValue(), wallet.getValue(), transactions))
                .flatMap(this::removeFromMap)
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::updateDisplay, this::onError, this::siftUnknownTransactions);
    }

    private Token addTokenToChecklist(Token token)
    {
        tokenMap.put(token.getAddress(), token);
        setupTokensInteract.addTokenToMap(token);
        return token;
    }

    //run through what remains in the map, see if there are any unknown tokens
    //if we find unknown tokens fetch them and add to the token watch list
    private void siftUnknownTransactions()
    {
        //add in the XML contract address to list of unknowns to fetch if we don't have it already
        setupTokensInteract.setupUnknownList(tokenMap, xmlContractAddress);

        fetchTransactionDisposable = setupTokensInteract.processRemainingTransactions(txMap.values().toArray(new Transaction[0]), tokenMap) //patches tx's and returns unknown contracts
                .flatMap(transactions -> fetchTransactionsInteract.storeTransactions(network.getValue(), wallet.getValue(), transactions).toObservable()) //store patched TX
                .map(setupTokensInteract::getUnknownContracts) //emit a list of string addresses
                .flatMapIterable(address -> address) //change to a sequential stream
                .flatMap(setupTokensInteract::addToken) //fetch token info
                .flatMap(addTokenInteract::add) //add to cached tokens
                .flatMap(token -> setupTokensInteract.reProcessTokens(token, txMap)) //run through transactions now we have the new token
                .flatMap(this::removeFromMap) //remove the handled transactions from the map (so we don't need to scan these transactions again)
                .flatMap(transactions -> fetchTransactionsInteract.storeTransactions(network.getValue(), wallet.getValue(), transactions).toObservable()) //store newly fixed up transactions
                .subscribeOn(Schedulers.io())
                .subscribe(this::updateDisplay, this::onError, this::storeUnprocessedTx); //update the screen with any updated transactions

        if (transactionCount == 0)
        {
            Log.d(TAG, "No transactions");
            progress.postValue(false);
            showEmpty.postValue(true);
        }
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
                .subscribe(this::completeCycle, this::onError);
    }

    private void completeCycle(Transaction[] transactions)
    {
        checkInternalTx();
        progress.postValue(false); //ensure spinner is off on completion (in case user forced update)
        fetchTransactionDisposable = null;
        checkIfRegularUpdateNeeded();
    }

    private void checkInternalTx()
    {
        if (feemasterUrl != null)
        {
            disposable = fetchTransactionsInteract.fetchInternalTransactions(wallet.getValue(), feemasterUrl)
                    .flatMap(transactions -> setupTokensInteract.processRemainingTransactions(transactions, tokenMap))
                    .flatMap(transactions -> fetchTransactionsInteract.storeTransactions(network.getValue(), wallet.getValue(), transactions).toObservable())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateDisplay, this::onError);
        }
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
        this.wallet.setValue(wallet);
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
        if (txArray != null)
        {
            this.transactions.postValue(txArray);
        }
    }

    public void setVisibility(boolean visibility) {
        isVisible = visibility;
    }

    public void setXMLContractAddress(String address)
    {
        xmlContractAddress = address;
    }
    public void setFeemasterURL(String address) { feemasterUrl = address; };
}
