package com.alphawallet.app.service;

import android.support.annotation.Nullable;
import android.util.Log;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.app.service.TokensService.PENDING_TIME_LIMIT;

/**
 * Created by JB on 8/07/2020.
 */
public class TransactionsService
{
    private final TokensService tokensService;
    private final PreferenceRepositoryType preferenceRepository;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TransactionsNetworkClientType transactionsClient;
    private static final Map<String, Integer> pendingHashMap = new ConcurrentHashMap<>();
    private String currentAddress;
    private long pendingCheckTimeout = 0;

    @Nullable
    private Disposable fetchTransactionDisposable;
    @Nullable
    private Disposable eventTimer;
    @Nullable
    private Disposable queryUnknownTokensDisposable;

    public TransactionsService(TokensService tokensService,
                               PreferenceRepositoryType preferenceRepositoryType,
                               EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                               TransactionsNetworkClientType transactionsClient) {
        this.tokensService = tokensService;
        this.preferenceRepository = preferenceRepositoryType;
        this.ethereumNetworkRepository = ethereumNetworkRepositoryType;
        this.transactionsClient = transactionsClient;
        this.currentAddress = preferenceRepository.getCurrentWalletAddress();

        fetchTransactions();
    }

    private void fetchTransactions()
    {
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed()) fetchTransactionDisposable.dispose();
        fetchTransactionDisposable = null;
        //reset transaction timers
        if (eventTimer == null || eventTimer.isDisposed())
        {
            eventTimer = Observable.interval(0, 1, TimeUnit.SECONDS)
                    .doOnNext(l -> checkTransactionQueue()).subscribe();
        }
    }

    private void checkTransactionQueue()
    {
        if (fetchTransactionDisposable == null)
        {
            Token t = tokensService.getRequiresTransactionUpdate(pendingHashMap.values());

            if (t != null)
            {
                String tick = (t.isEthereum() && pendingHashMap.values().contains(t.tokenInfo.chainId)) ? "*" : "";
                if (t.isEthereum()) System.out.println("Transaction check for: " + t.tokenInfo.chainId + " (" + t.getNetworkName() + ") " + tick);
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(t.tokenInfo.chainId);
                fetchTransactionDisposable =
                        transactionsClient.storeNewTransactions(currentAddress, network, t.getAddress(), t.lastBlockCheck, t.txSync, t.isEthereum())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(transactions -> onUpdateTransactions(transactions, t), this::onTxError);
            }
        }
        checkPendingTransactionTimeout();
    }

    private void checkPendingTransactionTimeout()
    {
        if (pendingCheckTimeout > 0 && System.currentTimeMillis() > (pendingCheckTimeout + PENDING_TIME_LIMIT))
        {
            pendingCheckTimeout = 0;
            pendingHashMap.clear(); //transactions taking a long time to clear, resume normal checking
        }
    }

    private void onTxError(Throwable throwable)
    {
        fetchTransactionDisposable = null;
    }

    private void onUpdateTransactions(Transaction[] transactions, Token token)
    {
        //got a new transaction
        fetchTransactionDisposable = null;
        Log.d("TRANSACTION", "Queried for " + token.tokenInfo.name + " : " + transactions.length + " Network transactions");

        token.lastTxCheck = System.currentTimeMillis();

        Transaction placeHolder = transactions.length > 0 ? transactions[transactions.length - 1] : null;

        for (Transaction t : transactions)
        {
            //find any pending transactions that have been found
            if (pendingHashMap.containsKey(t.hash))
            {
                System.out.println("Removed pending tx: " + t.hash);
                pendingHashMap.remove(t.hash);
                if (pendingHashMap.size() == 0)
                {
                    System.out.println("All pending tx resolved");
                    pendingCheckTimeout = 0;
                }
            }
        }

        //The final transaction is the last transaction read, and will have the highest block number we read
        if (placeHolder != null)
        {
            token.lastBlockCheck = Long.parseLong(placeHolder.blockNumber);
            token.lastTxTime = placeHolder.timeStamp * 1000; //update last transaction received time
        }

        //update token details
        transactionsClient.storeBlockRead(token, currentAddress);
    }

    public void changeWallet(Wallet newWallet)
    {
        if (!newWallet.address.equalsIgnoreCase(currentAddress))
        {
            onDestroy();
            currentAddress = newWallet.address;
            fetchTransactions();
            pendingCheckTimeout = 0;
            pendingHashMap.clear();
        }
    }

    public void onDestroy()
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

    public void markPending(Transaction tx)
    {
        System.out.println("Marked Pending Tx Chain: " + tx.chainId);
        pendingHashMap.put(tx.hash, tx.chainId);
        pendingCheckTimeout = System.currentTimeMillis();
        tokensService.markChainPending(tx.chainId);
    }

    public void removePending(Transaction tx)
    {
        if (pendingHashMap.containsKey(tx.hash))
        {
            System.out.println("Removed Pending Hash: " + tx.hash);
            pendingHashMap.remove(tx.hash);


        }
    }
}
