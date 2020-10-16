package com.alphawallet.app.service;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokenscript.EventUtils;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TransactionLocalSource;

import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by JB on 8/07/2020.
 */
public class TransactionsService
{
    private static final String NO_TRANSACTION_EXCEPTION = "NoSuchElementException";
    private final TokensService tokensService;
    private final PreferenceRepositoryType preferenceRepository;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TransactionsNetworkClientType transactionsClient;
    private final TransactionLocalSource transactionsCache;
    private final Context context;
    private String currentAddress;

    @Nullable
    private Disposable fetchTransactionDisposable;
    @Nullable
    private Disposable eventTimer;

    public TransactionsService(TokensService tokensService,
                               PreferenceRepositoryType preferenceRepositoryType,
                               EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                               TransactionsNetworkClientType transactionsClient,
                               TransactionLocalSource transactionsCache,
                               Context ctx) {
        this.tokensService = tokensService;
        this.preferenceRepository = preferenceRepositoryType;
        this.ethereumNetworkRepository = ethereumNetworkRepositoryType;
        this.transactionsClient = transactionsClient;
        this.transactionsCache = transactionsCache;
        this.currentAddress = preferenceRepository.getCurrentWalletAddress();
        this.context = ctx;

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
            Token t = tokensService.getRequiresTransactionUpdate(getPendingChains());

            if (t != null)
            {
                String tick = (t.isEthereum() && getPendingChains().contains(t.tokenInfo.chainId)) ? "*" : "";
                if (t.isEthereum()) System.out.println("Transaction check for: " + t.tokenInfo.chainId + " (" + t.getNetworkName() + ") " + tick);
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(t.tokenInfo.chainId);
                fetchTransactionDisposable =
                        transactionsClient.storeNewTransactions(currentAddress, network, t.lastBlockCheck)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(transactions -> onUpdateTransactions(transactions, t), this::onTxError);
            }
        }
    }

    public Single<TransactionMeta[]> fetchAndStoreTransactions(int chainId, long lastTxTime)
    {
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId);
        return transactionsClient.fetchMoreTransactions(currentAddress, network, lastTxTime);
    }

    private List<Integer> getPendingChains()
    {
        List<Integer> pendingChains = new ArrayList<>();
        Transaction[] pendingTransactions = fetchPendingTransactions();
        for (Transaction tx : pendingTransactions)
        {
            if (!pendingChains.contains(tx.chainId)) pendingChains.add(tx.chainId);
        }

        return pendingChains;
    }

    private Transaction[] fetchPendingTransactions()
    {
        if (!TextUtils.isEmpty(currentAddress))
        {
            return transactionsCache.fetchPendingTransactions(currentAddress);
        }
        else
        {
            return new Transaction[0];
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

        //The final transaction is the last transaction read, and will have the highest block number we read
        if (placeHolder != null)
        {
            token.lastBlockCheck = Long.parseLong(placeHolder.blockNumber);
            token.lastTxTime = placeHolder.timeStamp * 1000; //update last transaction received time

            if (placeHolder.nonce == -1)
            {
                context.sendBroadcast(new Intent(C.RESET_TRANSACTIONS));
            }
        }

        //update token details
        transactionsClient.storeBlockRead(token, currentAddress);
        checkPendingTransactions(token.tokenInfo.chainId);
    }

    public void changeWallet(Wallet newWallet)
    {
        if (!newWallet.address.equalsIgnoreCase(currentAddress))
        {
            onDestroy();
            currentAddress = newWallet.address;
            fetchTransactions();
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
    }

    public void markPending(Transaction tx)
    {
        System.out.println("Marked Pending Tx Chain: " + tx.chainId);
        tokensService.markChainPending(tx.chainId);
    }

    private void checkPendingTransactions(int chainId)
    {
        Transaction[] pendingTxs = fetchPendingTransactions();
        for (Transaction tx : pendingTxs)
        {
            if (tx.chainId == chainId)
            {
                final String currentWallet = currentAddress;
                Web3j web3j = TokenRepository.getWeb3jService(tx.chainId);
                web3j.ethGetTransactionByHash(tx.hash).sendAsync().thenAccept(txDetails -> {
                    org.web3j.protocol.core.methods.response.Transaction fetchedTx = txDetails.getTransaction().orElseThrow(); //try to read the transaction data
                    //if transaction is complete; record it here
                    if (fetchedTx.getBlockNumber() != null && fetchedTx.getBlockNumber().compareTo(BigInteger.ZERO) > 0)
                    {
                        //get timestamp and write tx
                        EventUtils.getBlockDetails(fetchedTx.getBlockHash(), web3j)
                                .map(ethBlock -> transactionsCache.storeRawTx(new Wallet(currentWallet), txDetails, ethBlock.getBlock().getTimestamp().longValue()))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe().isDisposed();
                    }
                }).exceptionally(throwable -> {
                    String c1 = throwable.getMessage(); //java.util.NoSuchElementException: No value present
                    if (!TextUtils.isEmpty(c1) && c1.contains(NO_TRANSACTION_EXCEPTION))
                    {
                        //transaction is no longer in pool or on chain. Cause: dropped from mining pool
                        //mark transaction as dropped
                        transactionsCache.markTransactionDropped(currentAddress, tx.hash);
                    }
                    return null;
                });
            }
        }
    }
}
