package com.alphawallet.app.service;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.AnalyticsProperties;
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
import com.alphawallet.token.entity.ContractAddress;

import org.web3j.exceptions.MessageDecodingException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

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
    private int currentChainIndex;
    private boolean nftCheck;

    private final static int TRANSACTION_DROPPED = -1;
    private final static int TRANSACTION_SEEN = -2;

    @Nullable
    private Disposable fetchTransactionDisposable;
    @Nullable
    private Disposable eventTimer;
    @Nullable
    private Disposable erc20EventCheckCycle;

    public TransactionsService(TokensService tokensService,
                               PreferenceRepositoryType preferenceRepositoryType,
                               EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                               TransactionsNetworkClientType transactionsClient,
                               TransactionLocalSource transactionsCache,
                               Context ctx)
    {
        this.tokensService = tokensService;
        this.preferenceRepository = preferenceRepositoryType;
        this.ethereumNetworkRepository = ethereumNetworkRepositoryType;
        this.transactionsClient = transactionsClient;
        this.transactionsCache = transactionsCache;
        this.currentAddress = preferenceRepository.getCurrentWalletAddress();
        this.context = ctx;

        checkTransactionReset();
        fetchTransactions();
    }

    private void checkTransactionReset()
    {
        if (currentAddress == null) return;
        //checks to see if we need a tx fetch reset
        transactionsClient.checkTransactionsForEmptyFunctions(currentAddress)
                .subscribeOn(Schedulers.computation())
                .subscribe();
    }

    private void fetchTransactions()
    {
        currentChainIndex = 0;
        nftCheck = false;

        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
            fetchTransactionDisposable.dispose();
        fetchTransactionDisposable = null;
        //reset transaction timers
        if (eventTimer == null || eventTimer.isDisposed())
        {
            eventTimer = Observable.interval(0, 1, TimeUnit.SECONDS)
                    .doOnNext(l -> checkTransactionQueue()).subscribe();
        }

        if (erc20EventCheckCycle == null || erc20EventCheckCycle.isDisposed())
        {
            erc20EventCheckCycle = Observable.interval(2, 10, TimeUnit.SECONDS)
                    .doOnNext(l -> checkTransactions()).subscribe();
        }
    }

    public void startUpdateCycle()
    {
        if (eventTimer == null || eventTimer.isDisposed())
        {
            fetchTransactions();
        }
        tokensService.appInFocus();
    }

    /**
     * Start the token transaction checker
     * This uses the Etherscan API routes returning ERC20 and ERC721 token transfers, both incoming and outgoing.
     */
    private void checkTransactions()
    {
        if (currentAddress == null) return;
        List<Integer> filters = tokensService.getNetworkFilters();
        if (currentChainIndex >= filters.size()) currentChainIndex = 0;
        int chainId = filters.get(currentChainIndex);

        transactionsClient.readTransactions(currentAddress, ethereumNetworkRepository.getNetworkByChain(chainId), tokensService, nftCheck)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> { System.out.println("Received: " + count); }).isDisposed();

        if (!nftCheck)
        {
            nftCheck = true;
        }
        else
        {
            nftCheck = false;
            currentChainIndex++;
        }
    }

    private void checkTransactionQueue()
    {
        if (currentAddress == null) return;
        if (fetchTransactionDisposable == null)
        {
            Token t = tokensService.getRequiresTransactionUpdate(getPendingChains());

            if (t != null)
            {
                String tick = (t.isEthereum() && getPendingChains().contains(t.tokenInfo.chainId)) ? "*" : "";
                if (t.isEthereum())
                    System.out.println("Transaction check for: " + t.tokenInfo.chainId + " (" + t.getNetworkName() + ") " + tick);
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(t.tokenInfo.chainId);
                fetchTransactionDisposable =
                        transactionsClient.storeNewTransactions(currentAddress, network, t.getAddress(), t.lastBlockCheck)
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
        checkPendingTransactions(token.tokenInfo.chainId);
        if (transactions.length == 0) return;

        Log.d("TRANSACTION", "Queried for " + token.tokenInfo.name + " : " + transactions.length + " Network transactions");

        //now check for unknown tokens
        checkTokens(transactions);
    }

    /**
     * Check new tokens for any unknowns, then find the unknowns
     *
     * @param txList
     */
    private void checkTokens(Transaction[] txList)
    {
        for (int i = 0; i < txList.length - 1; i++)
        {
            Transaction tx = txList[i];
            if (!tx.hasError() && tx.hasData()) //is this a successful contract transaction?
            {
                Token token = tokensService.getToken(tx.chainId, tx.to);
                if (token == null)
                    tokensService.addUnknownTokenToCheckPriority(new ContractAddress(tx.chainId, tx.to));
            }
        }
    }

    public void changeWallet(Wallet newWallet)
    {
        if (!newWallet.address.equalsIgnoreCase(currentAddress))
        {
            stopUpdate();
            currentAddress = newWallet.address;
            fetchTransactions();
            checkTransactionReset();
        }
    }

    public void lostFocus()
    {
        tokensService.appOutOfFocus();
        stopUpdate();
    }

    public void stopUpdate()
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
                    BigInteger blockNumber;
                    try
                    {
                        blockNumber = fetchedTx.getBlockNumber();
                    }
                    catch (MessageDecodingException e)
                    {
                        blockNumber = BigInteger.valueOf(-1);
                    }

                    if (blockNumber.compareTo(BigInteger.ZERO) > 0)
                    {
                        //Write to database (including detecting Transaction write error)
                        web3j.ethGetTransactionReceipt(tx.hash).sendAsync().thenAccept(receipt -> {
                            if (receipt != null)
                            {
                                //get timestamp and write tx
                                EventUtils.getBlockDetails(fetchedTx.getBlockHash(), web3j)
                                        .map(ethBlock -> transactionsCache.storeRawTx(new Wallet(currentWallet), chainId, txDetails, ethBlock.getBlock().getTimestamp().longValue(), receipt.getResult().getStatus().equals("0x1")))
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe().isDisposed();
                            }
                        }).exceptionally(throwable -> {
                            throwable.printStackTrace();
                            return null;
                        });
                    }
                    else if (!tx.blockNumber.equals(String.valueOf(TRANSACTION_SEEN)))
                    {
                        //detected the tx in the pool, mark as seen
                        transactionsCache.markTransactionBlock(currentAddress, tx.hash, TRANSACTION_SEEN);
                    }
                }).exceptionally(throwable -> {
                    String c1 = throwable.getMessage(); //java.util.NoSuchElementException: No value present
                    if (!TextUtils.isEmpty(c1) && c1.contains(NO_TRANSACTION_EXCEPTION) && tx.blockNumber.equals(String.valueOf(TRANSACTION_SEEN))) //we sighted this tx in the pool, now it's gone
                    {
                        //transaction is no longer in pool or on chain. Cause: dropped from mining pool
                        //mark transaction as dropped
                        transactionsCache.markTransactionBlock(currentAddress, tx.hash, TRANSACTION_DROPPED);
                    }
                    return null;
                });
            }
        }
    }
}
