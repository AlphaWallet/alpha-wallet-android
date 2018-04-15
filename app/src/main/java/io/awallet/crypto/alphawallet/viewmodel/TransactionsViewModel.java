package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;

import io.awallet.crypto.alphawallet.C;
import io.awallet.crypto.alphawallet.entity.ErrorEnvelope;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenInfo;
import io.awallet.crypto.alphawallet.entity.TokenTransaction;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.TransactionsCallback;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.AddTokenInteract;
import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FetchTransactionsInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.GetDefaultWalletBalance;
import io.awallet.crypto.alphawallet.interact.SetupTokensInteract;
import io.awallet.crypto.alphawallet.router.ExternalBrowserRouter;
import io.awallet.crypto.alphawallet.router.HomeRouter;
import io.awallet.crypto.alphawallet.router.MarketBrowseRouter;
import io.awallet.crypto.alphawallet.router.MarketplaceRouter;
import io.awallet.crypto.alphawallet.router.MyTokensRouter;
import io.awallet.crypto.alphawallet.router.NewSettingsRouter;
import io.awallet.crypto.alphawallet.router.SettingsRouter;
import io.awallet.crypto.alphawallet.router.TransactionDetailRouter;
import io.awallet.crypto.alphawallet.router.WalletRouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.awallet.crypto.alphawallet.ui.HomeActivity;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static io.awallet.crypto.alphawallet.interact.SetupTokensInteract.EXPIRED_CONTRACT;

public class TransactionsViewModel extends BaseViewModel {
    private static final long GET_BALANCE_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;
    private static final long FETCH_TRANSACTIONS_INTERVAL = 12 * DateUtils.SECOND_IN_MILLIS;
    private static final String TAG = "TVM";

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> defaultWalletBalance = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenInteract addTokenInteract;
    private final SetupTokensInteract setupTokensInteract;

    private final SettingsRouter settingsRouter;
    private final TransactionDetailRouter transactionDetailRouter;
    private final MyTokensRouter myTokensRouter;
    private final ExternalBrowserRouter externalBrowserRouter;
    private final MarketBrowseRouter marketBrowseRouter;
    private final WalletRouter walletRouter;
    private final MarketplaceRouter marketplaceRouter;
    private final NewSettingsRouter newSettingsRouter;
    private final HomeRouter homeRouter;

    @Nullable
    private Disposable fetchTransactionDisposable;
    private Handler handler = new Handler();

    private boolean isVisible = false;
    private Transaction[] txArray;
    private Map<String, Transaction> txMap = new ConcurrentHashMap<>();
    private List<Token> tokenCheckList;
    private boolean needsUpdate = false;

    TransactionsViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            FetchTokensInteract fetchTokensInteract,
            GetDefaultWalletBalance getDefaultWalletBalance,
            SetupTokensInteract setupTokensInteract,
            SettingsRouter settingsRouter,
            AddTokenInteract addTokenInteract,
            TransactionDetailRouter transactionDetailRouter,
            MyTokensRouter myTokensRouter,
            ExternalBrowserRouter externalBrowserRouter,
            MarketBrowseRouter marketBrowseRouter,
            WalletRouter walletRouter,
            MarketplaceRouter marketplaceRouter,
            NewSettingsRouter newSettingsRouter,
            HomeRouter homeRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.settingsRouter = settingsRouter;
        this.transactionDetailRouter = transactionDetailRouter;
        this.myTokensRouter = myTokensRouter;
        this.externalBrowserRouter = externalBrowserRouter;
        this.marketBrowseRouter = marketBrowseRouter;
        this.walletRouter = walletRouter;
        this.marketplaceRouter = marketplaceRouter;
        this.newSettingsRouter = newSettingsRouter;
        this.homeRouter = homeRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        handler.removeCallbacks(startFetchTransactionsTask);
        handler.removeCallbacks(startGetBalanceTask);

        isVisible = false;
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
        {
            fetchTransactionDisposable.dispose();
        }
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

    /**
     * 1. Get all transactions on wallet address.
     * First check wallet address is still valid (user may have restarted process)
     * @param shouldShowProgress
     */
    private void fetchTransactions(boolean shouldShowProgress) {
        if (defaultWallet().getValue() != null)
        {
            if (fetchTransactionDisposable == null)
            {
                Log.d(TAG, "Fetch start");
                setupTokensInteract.setWalletAddr(defaultWallet().getValue().address);
                progress.postValue(shouldShowProgress);
                needsUpdate = true;
                fetchTransactionDisposable =
                        fetchTransactionsInteract.fetchCached(defaultWallet.getValue())
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
            needsUpdate = false;
        }
    }

    /**
     * 2. After fetching the stored transactions we display them so there's not a blank screen for too long.
     * After display, fetch any new transactions
     */
    private void fetchNetworkTransactions()
    {
        //update the UI, display cached transactions
        this.transactions.postValue(txArray);

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
                fetchTransactionsInteract.fetchNetworkTransactions(defaultWallet.getValue(), lastTx)
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
    }

    /**
     *  3. Once we have fetched all user account related transactions we need to fill in all the contract transactions
     *   First get a list of tokens, on each token see if it's an ERC875, if it is then scan the contract transactions
     *   for any that relate to the current user account (given by wallet address)
     */
    private void enumerateTokens()
    {
        Log.d(TAG, "Enumerating tokens");
        txArray = txMap.values().toArray(new Transaction[txMap.size()]);
        if (needsUpdate && txArray.length > 0) this.transactions.postValue(txArray); //intermediate update transactions on wallet first initialised
        fetchTransactionDisposable = fetchTokensInteract
                .fetchStored(defaultWallet.getValue())
                .subscribeOn(Schedulers.io())
                .subscribe(this::onTokens, this::onError, this::categoriseAccountTransactions);
    }

    /**
     * 3a. receive cached tokens and store them in the service
     */
    private void onTokens(Token[] tokens) {
        Log.d(TAG, "Found " + tokens.length + " Stored tokens");
        setupTokensInteract.setTokens(tokens);
    }

    /**
     * 4. once we have a list of user tokens and transactions on the wallet account
     * we need to build a map of transactions and associate them with local tokens
     */
    private void categoriseAccountTransactions()
    {
        Boolean last = progress.getValue();
        if (txArray != null && txArray.length > 0 && last != null && last) {
            progress.postValue(true);
        }
        else
        {
            if (setupTokensInteract.getLocalTokensCount() == 0) {
                Log.d(TAG, "No transactions");
                progress.postValue(false);
                return; // no local transactions, no ERC875 tokens, no need to check any further
            }
        }

        //go ahead and build the map associating account transactions with ERC875 tokens
        fetchTransactionDisposable = setupTokensInteract
                .checkTransactions(txArray)
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::startCheckingTokenInterations);
    }

    /**
     * 4a. Finish checking tokens and fetch the token check list before consuming it.
     * @param txList - dummy param: not used here
     */
     private void startCheckingTokenInterations(Transaction[] txList) {
         txArray = txList;
         tokenCheckList = setupTokensInteract.getTokenCheckList();

         consumeTokenCheckList();
     }

    /**
     * 5. Get all the transactions from all of our cached tokens.
     * This funtion recursively calls itself, consuming each token we loaded in step 2.
     */
    private void consumeTokenCheckList()
    {
        try
        {
            if (tokenCheckList.size() == 0)
            {
                processTokenTransactions();
            }
            else
            {
                Token t = tokenCheckList.remove(0);
                Log.d(TAG, "Consume " + t.getFullName());

                fetchTransactionDisposable = fetchTransactionsInteract.fetch(new Wallet(t.tokenInfo.address), t)
                        .subscribeOn(Schedulers.io())
                        .subscribe(this::addTxs, this::onConsumeError, this::consumeTokenCheckList);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 5a. Receive TokenTransaction pairs for a token and add to the check list
     * @param txList
     */
    private void addTxs(TokenTransaction[] txList)
    {
        if (txList.length > 0)
        {
            Log.d(TAG, "Found " + txList.length + " TokenTX for " + txList[0].token.getFullName());
        }

        setupTokensInteract.addTokenTransactions(txList);
    }

    private void onConsumeError(Throwable e)
    {
        consumeTokenCheckList();
    }

    /**
     * 6. Now parse all the transactions we obtained in steps 1, 2 and step 5
     */
    private void processTokenTransactions()
    {
        Log.d(TAG, "Processing " + setupTokensInteract.getMapSize() + " Map Transactions. " + setupTokensInteract.getLocalTokensCount() + " Tokens known.");
        fetchTransactionDisposable = setupTokensInteract
                .processTransactions(defaultWallet().getValue())
                .flatMap(transactions -> fetchTransactionsInteract.storeTransactions(defaultNetwork.getValue(), defaultWallet().getValue(), transactions))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showTransactions, this::onError);
    }

    private void onTxCount(Transaction[] tx)
    {
        Log.d(TAG, "Stored Transactions " + tx.length);
    }

    /**
     * 7. finally receive the list of parsed transactions and update the list adapter
     * @param processedTransactions
     */
    private void showTransactions(Transaction[] processedTransactions)
    {
        progress.postValue(false);
        txArray = processedTransactions;
        if (processedTransactions.length > 0)
        {
            this.transactions.postValue(processedTransactions);
        }
        else
        {
            error.postValue(new ErrorEnvelope(C.ErrorCode.EMPTY_COLLECTION, "empty collection"));
        }

        if (setupTokensInteract.getRequiredContracts().size() > 0)
        {
            Log.d(TAG, "Fetching " + setupTokensInteract.getRequiredContracts().size() + " Tokens");
            //if there are detected contract transactions that we don't already know about add them in here
            fetchTransactionDisposable = setupTokensInteract.addTokens()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.newThread())
                    .subscribe(this::onTokenInfo, this::onError);
        }
        else
        {
            fetchTransactionDisposable = null;
            checkIfRegularUpdateNeeded();
        }
    }

    /**
     * 8. Receive all the token data for currently unknown contracts
     * - we only receive valid contract tokens as all the dead ones are filtered out
     */
    private void onTokenInfo(TokenInfo[] tokenInfos)
    {
        setupTokensInteract.getRequiredContracts().clear();
        if (tokenInfos.length > 0)
        {
            fetchTransactionDisposable = addTokenInteract
                    .add(tokenInfos)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()) //we directly affect UI from the callback
                    .subscribe(this::onTokens, this::onError, this::onSaved);
        }
        else
        {
            fetchTransactionDisposable = null;
            checkIfRegularUpdateNeeded();
        }
    }

    /**
     * 8a. Finished storing the received tokens. Since we stored tokens we require a refresh to populate contract names
     */
    private void onSaved()
    {
        setupTokensInteract.regenerateTransactionList();
        Log.d(TAG,"saved contracts.");
        refreshTokens.postValue(true); //send directive to refresh token list
        //now re-process the tokens
        reProcessTransactions();
    }

    private void reProcessTransactions()
    {
        Log.d(TAG, "Re-processing " + txArray.length + " Transactions. " + setupTokensInteract.getLocalTokensCount() + " Tokens known.");
        //fill in the required info and store
        fetchTransactionDisposable = setupTokensInteract
                .checkTransactions(txArray)
                .flatMap(transactions -> fetchTransactionsInteract.storeTransactions(defaultNetwork.getValue(), defaultWallet().getValue(), transactions))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::refreshDisplay);
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
            progress.postValue(true);
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
            handler.removeCallbacks(startGetBalanceTask);
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


    private void refreshDisplay(Transaction[] txList)
    {
        txArray = txList;
        this.transactions.postValue(txArray);
        fetchTransactionDisposable = null;
        checkIfRegularUpdateNeeded(); //finally see if we need to start periodic update
    }

    //NB: We don't need to update balance in transaction view
    public void getBalance() {
//        getBalanceDisposable = getDefaultWalletBalance
//                .get(defaultWallet.getValue())
//                .subscribe(values -> {
//                    defaultWalletBalance.postValue(values);
//                    handler.removeCallbacks(startGetBalanceTask);
//                    handler.postDelayed(startGetBalanceTask, GET_BALANCE_INTERVAL);
//                }, t -> {
//                });
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

    public void showSettings(Context context) {
        settingsRouter.open(context);
    }

    public void showNewSettings(Context context, int resId) {
        newSettingsRouter.open(context, resId);
    }

    public void showDetails(Context context, Transaction transaction) {
        transactionDetailRouter.open(context, transaction);
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

    public void showHome(Context context) {
        homeRouter.open(context, true);
    }

    public void openDeposit(Context context, Uri uri) {
        externalBrowserRouter.open(context, uri);
    }

    private final Runnable startFetchTransactionsTask = () -> this.fetchTransactions(false);
    private final Runnable startGetBalanceTask = this::getBalance;

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
}
