package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

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

import java.util.List;
import java.util.Map;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TransactionsViewModel extends BaseViewModel {
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
    private List<Token> tokenCheckList;

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

    //1. Get all transactions on wallet address
    public void fetchTransactions(boolean shouldShowProgress) {
        handler.removeCallbacks(startFetchTransactionsTask);
        if (defaultWallet().getValue() != null) {
            setupTokensInteract.setWalletAddr(defaultWallet().getValue().address);
            progress.postValue(shouldShowProgress);
            fetchTransactionDisposable =
                    fetchTransactionsInteract.fetch(defaultWallet.getValue())
                            .subscribeOn(Schedulers.io())
                            .subscribe(this::onTransactions, this::onError, this::enumerateTokens);
        }
        else {
            disposable = findDefaultWalletInteract
                    .find()
                    .subscribe(this::onDefaultWallet, this::onError);
        }
    }

//    public void fetchTransactions2(boolean shouldShowProgress) {
//        handler.removeCallbacks(startFetchTransactionsTask);
//        setupTokensInteract.setWalletAddr(defaultWallet().getValue().address);
//        progress.postValue(shouldShowProgress);
//        fetchTransactionsInteract.fetchTx2(defaultWallet.getValue(), txCallback);
//    }

    private TransactionsCallback txCallback = new TransactionsCallback() {
        @Override
        public void recieveTransactions(Transaction[] txList) {
            txArray = txList;
            enumerateTokens();
        }
    };

    //Store the transactions we obtained in step 1 locally
    private void onTransactions(Transaction[] transactions) {
        txArray = transactions;
    }

    //2. Once we have fetched all user account related transactions we need to fill in all the contract transactions
    //First get a list of tokens, on each token see if it's an ERC875, if it is then scan the contract transactions
    //for any that relate to the current user account (given by wallet address)
    private void enumerateTokens()
    {
        fetchTransactionDisposable = fetchTokensInteract
                .fetchStored(defaultWallet.getValue())
                .subscribeOn(Schedulers.io())
                .subscribe(this::onTokens, this::onError, this::categoriseAccountTransactions);
    }

    //receive cached tokens and store them in the service
    private void onTokens(Token[] tokens) {
        setupTokensInteract.setTokens(tokens);
    }

    //3. once we have a list of user tokens and transactions on the wallet account
    //we need to build a map of transactions and associate them with local tokens
    private void categoriseAccountTransactions()
    {
        Boolean last = progress.getValue();
        if (txArray != null && txArray.length > 0 && last != null && last) {
            progress.postValue(true);
        }
        else
        {
            if (setupTokensInteract.getLocalTokensCount() == 0) {
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

     private void startCheckingTokenInterations(Object o)
    {
        tokenCheckList = setupTokensInteract.getTokenCheckList();
        consumeTokenCheckList();
    }

    //4. Get all the transactions from all of our cached tokens.
    // This funtion recursively calls itself, consuming each token we loaded in step 2.
    private void consumeTokenCheckList()
    {
        if (tokenCheckList.size() == 0)
        {
            processTokenTransactions();
        }
        else {
            Token t = tokenCheckList.remove(0);

            fetchTransactionDisposable = fetchTransactionsInteract.fetch(new Wallet(t.tokenInfo.address), t)
                    .subscribeOn(Schedulers.io())
                    .subscribe(this::addTxs, this::onConsumeError, this::consumeTokenCheckList);
        }
    }

    private void addTxs(TokenTransaction[] txList)
    {
        setupTokensInteract.addTokenTransactions(txList);
    }

    private void onConsumeError(Throwable e)
    {
        consumeTokenCheckList();
    }

    //5. Now parse all the transactions we obtained in step 1 and step 4
    //match them against our known contracts and see what action was taking place (eg trade, transferFrom etc)
    private void processTokenTransactions()
    {
        fetchTransactionDisposable = setupTokensInteract
                .processTransactions(defaultWallet().getValue())
                .observeOn(Schedulers.newThread())
                .subscribe(this::showTransactions, this::onError);
    }

    //6. finally receive the list of parsed transactions and update the list adapter
    private void showTransactions(Transaction[] processedTransactions)
    {
        if (processedTransactions.length > 0)
        {
            this.transactions.postValue(processedTransactions);
        }
        else
        {
            error.postValue(new ErrorEnvelope(C.ErrorCode.EMPTY_COLLECTION, "empty collection"));
        }

        boolean regenerate = false;

        //if there are detected contract transactions that we don't already know about add them in here
        for (String contractAddress : setupTokensInteract.getRequiredContracts())
        {
            if (regenerate == false)
            {
                setupTokensInteract.regenerateTransactionList();
                regenerate = true;
            }
            //detected interaction with these unknown contracts
            //add them to our watch list
            setupTokenAddr(contractAddress);
        }

        progress.postValue(false);

        if (isVisible) {
            handler.postDelayed(
                    startFetchTransactionsTask,
                    FETCH_TRANSACTIONS_INTERVAL);
        }
        else
        {
            //no longer any need to refresh
            System.out.println("TVM Finish");
            if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
            {
                fetchTransactionDisposable.dispose();
            }
            fetchTransactionDisposable = null; //ready to restart the fetch
        }
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
            fetchTransactions(true);
        }
    }

    //Fetch contract details
    private void setupTokenAddr(String contractAddress)
    {
        disposable = setupTokensInteract
                .update(contractAddress)
                .subscribeOn(Schedulers.io())
                .subscribe(this::onTokensSetup, this::onError);
    }

    //Store contract details if the contract is live,
    //otherwise remove from the contract watch list
    private void onTokensSetup(TokenInfo tokenInfo) {
        //check this contract is good to add
        if ((tokenInfo.name == null || tokenInfo.name.length() < 3)
            || tokenInfo.isEnabled == false
            || (tokenInfo.symbol == null || tokenInfo.symbol.length() < 2))
        {
            setupTokensInteract.putDeadContract(tokenInfo.address);
        }
        else {
            disposable = addTokenInteract
                    .add(tokenInfo)
                    .subscribeOn(Schedulers.io())
                    .subscribe(this::onSaved, this::onError);
        }
    }

    private void onSaved()
    {
        System.out.println("saved contract");
    }

    public void setVisibility(boolean visibility) {
        isVisible = visibility;
    }
}
