package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.UnknownToken;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.router.TransactionDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class TransactionsViewModel extends BaseViewModel
{
    private static final String TAG = "TVM";
    private static final int MAX_DISPLAYABLE_TRANSACTIONS = 10000; //only display up to the last 10000 transactions

    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showEmpty = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> clearAdapter = new MutableLiveData<>();
    private final MutableLiveData<Boolean> refreshAdapter = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> newTransactions = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AddTokenInteract addTokenInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private final TransactionDetailRouter transactionDetailRouter;

    @Nullable
    private Disposable fetchTransactionDisposable;
    @Nullable
    private Disposable eventTimer;
    @Nullable
    private Disposable queryUnknownTokensDisposable;

    private final ConcurrentLinkedQueue<UnknownToken> unknownTokens;

    private Map<String, Transaction> txMap = new HashMap<>();
    private boolean parseTransactions;

    TransactionsViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            GenericWalletInteract genericWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            SetupTokensInteract setupTokensInteract,
            AddTokenInteract addTokenInteract,
            TransactionDetailRouter transactionDetailRouter,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.transactionDetailRouter = transactionDetailRouter;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.unknownTokens = new ConcurrentLinkedQueue<>();
        this.parseTransactions = false;
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();
        clearProcesses();
    }

    public void clearProcesses()
    {
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
        {
            fetchTransactionDisposable.dispose();
        }
        fetchTransactionDisposable = null;

        if (eventTimer != null && !eventTimer.isDisposed())
        {
            eventTimer.dispose();
        }
        eventTimer = null;

        if (queryUnknownTokensDisposable != null && !queryUnknownTokensDisposable.isDisposed())
        {
            queryUnknownTokensDisposable.dispose();
        }
        queryUnknownTokensDisposable = null;
    }

    private void startEventTimer()
    {
        fetchTransactionDisposable = null;
        //reset transaction timers
        if (eventTimer == null || eventTimer.isDisposed())
        {
            eventTimer = Observable.interval(0, 1, TimeUnit.SECONDS)
                    .doOnNext(l -> checkEvents()).subscribe();
        }

        //collect all unknown tokens
        if (!parseTransactions && tokensService.checkHasLoaded())
        {
            parseTransactions = true;
            //check transactions see if there are missing tokens
            disposable = setupTokensInteract.getUnknownTokens(txMap.values().toArray(new Transaction[0]), tokensService)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(this::addUnknownTokens);
        }
    }

    private void checkEvents()
    {
        //see which tokens need checking
        checkTransactionQueue();
        checkUnknownTokens();
    }

    private void checkUnknownTokens()
    {
        if (queryUnknownTokensDisposable == null && !unknownTokens.isEmpty())
        {
            UnknownToken t = unknownTokens.poll();

            if (tokensService.getToken(t.chainId, t.address) == null)
            {
                queryUnknownTokensDisposable = setupTokensInteract.addToken(t.address, t.chainId) //fetch tokenInfo
                        .flatMap(fetchTransactionsInteract::queryInterfaceSpecForService)
                        .filter(tokenInfo -> tokenInfo.name != null)
                        .flatMap(tokenInfo -> addTokenInteract.add(tokenInfo, tokensService.getInterfaceSpec(tokenInfo.chainId, tokenInfo.address), defaultWallet().getValue())) //add to database
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(this::updateTokenService, this::onError, this::reCheckUnknowns);
            }
        }
    }

    private void reCheckUnknowns()
    {
        queryUnknownTokensDisposable = null;
    }

    private void checkTransactionQueue()
    {
        if (fetchTransactionDisposable == null)
        {
            Token t = tokensService.getRequiresTransactionUpdate();

            if (t != null)
            {
                Log.d(TAG, "Checking Tx for: " + t.getFullName());
                NetworkInfo network = findDefaultNetworkInteract.getNetworkInfo(t.tokenInfo.chainId);
                String userAddress = t.isEthereum() ? null : wallet.getValue().address; //only specify address if we're scanning token transactions - not all are relevant to us.
                fetchTransactionDisposable =
                        fetchTransactionsInteract.fetchNetworkTransactions(network, t.getAddress(), t.lastBlockCheck, userAddress)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(transactions -> onUpdateTransactions(transactions, t), this::onTxError);
            }
        }
    }

    private void onTxError(Throwable throwable)
    {
        fetchTransactionDisposable = null;
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
        disposable = genericWalletInteract
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
        //first load transactions from storage, then start the event timer once loaded
        if (fetchTransactionDisposable == null)
        {
            Log.d(TAG, "Fetch start");

            fetchTransactionDisposable =
                    fetchTransactionsInteract.fetchCached(wallet, MAX_DISPLAYABLE_TRANSACTIONS)
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
        fetchTransactionDisposable = null;
        boolean pendingUpdate = false;
        Log.d("TRANSACTION", "Queried for " + token.tokenInfo.name + " : " + transactions.length + " Network transactions");

        token.lastTxCheck = System.currentTimeMillis();
        List<Transaction> newTxs = new ArrayList<>();

        //NB: final transaction is block marker transaction, it's not used. If it is relevant, then it's a duplicate.
        for (int i = 0; i < transactions.length - 1; i++)
        {
            Transaction tx = transactions[i];
            Transaction oldTx = txMap.get(tx.hash);
            if (!txMap.containsKey(tx.hash) || oldTx.blockNumber.equals("0"))
            {
                newTxs.add(tx);
                txMap.put(tx.hash, tx);
                if (oldTx != null && oldTx.blockNumber.equals("0")) pendingUpdate = true;
            }
        }

        if (newTxs.size() > 0)
        {
            Log.d(TAG, "Found " + transactions.length + " Network transactions");
            newTransactions.postValue(newTxs.toArray(new Transaction[0]));
            if (pendingUpdate) refreshAdapter.postValue(true);
            //store new transactions, so they will appear in the transaction view, then update the view
            disposable = fetchTransactionsInteract.storeTransactions(wallet.getValue(), newTxs.toArray(new Transaction[0]))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(txs -> siftUnknownTransactions(txs, token), this::onError);
        }

        //The final transaction is the last transaction read, and will have the highest block number we read
        if (transactions.length > 0)
        {
            token.lastBlockCheck = Long.parseLong(transactions[transactions.length - 1].blockNumber);

            if (token.hasTokenScript) assetDefinitionService.updateEthereumResults(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io()).subscribe();

            token.lastTxUpdate = token.lastTxCheck;
        }

        //Need to log that we scanned transactions for this token, even if there weren't any transactions.
        addTokenInteract.updateBlockRead(token, defaultWallet().getValue());
    }

    //run through the newly received tokens from a currency and see if there's any unknown tokens
    private void siftUnknownTransactions(Transaction[] transactions, Token token)
    {
        if (token == null || token.isEthereum()) //only get unknown transactions for base accounts
        {
            disposable = setupTokensInteract.getUnknownTokens(transactions, tokensService)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(this::addUnknownTokens, this::onError);
        }
    }

    /**
     * This function gets called once after the sift Single has completed. For every contract it gets, it updates the service.
     * The token view will be updated continuously while a wallet with a large number of tokens is first being imported.
     * @param unknowns
     */
    private void addUnknownTokens(List<UnknownToken> unknowns)
    {
        unknownTokens.addAll(unknowns);
    }

    /**
     * each time we get a new token, add it to the service, the main token view will update with the new token after a refresh
     * @param token the new token
     */
    private void updateTokenService(Token token)
    {
        tokensService.addToken(token);
    }

    private void onDefaultWallet(Wallet wallet) {
        this.wallet.postValue(wallet);
        fetchTransactions(wallet);
    }

    public void showDetails(Context context, Transaction transaction) {
        transactionDetailRouter.open(context, transaction);
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
