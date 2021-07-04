package com.alphawallet.app.service;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.TransactionType;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokenscript.EventUtils;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TransactionLocalSource;
import com.alphawallet.token.entity.ContractAddress;

import org.web3j.exceptions.MessageDecodingException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthTransaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.app.service.TransactionsNetworkClient.TRANSFER_RESULT_MAX;

/**
 * Created by JB on 8/07/2020.
 */
public class TransactionsService
{
    private static final String NO_TRANSACTION_EXCEPTION = "NoSuchElementException";
    private static final String TAG = "TRANSACTION";
    private final TokensService tokensService;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TransactionsNetworkClientType transactionsClient;
    private final TransactionLocalSource transactionsCache;
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
    @Nullable
    private Disposable eventFetch;
    @Nullable
    private Disposable pendingTransactionFetch;

    public TransactionsService(TokensService tokensService,
                               EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                               TransactionsNetworkClientType transactionsClient,
                               TransactionLocalSource transactionsCache)
    {
        this.tokensService = tokensService;
        this.ethereumNetworkRepository = ethereumNetworkRepositoryType;
        this.transactionsClient = transactionsClient;
        this.transactionsCache = transactionsCache;

        fetchTransactions();
    }

    private void fetchTransactions()
    {
        if (TextUtils.isEmpty(tokensService.getCurrentAddress())) return;

        currentChainIndex = 0;
        nftCheck = true; //check nft first to filter out NFT tokens

        transactionsClient.checkRequiresAuxReset(tokensService.getCurrentAddress());

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
            erc20EventCheckCycle = Observable.interval(2, 15, TimeUnit.SECONDS)
                    .doOnNext(l -> checkTransactions()).subscribe();
        }

        if (pendingTransactionFetch == null || pendingTransactionFetch.isDisposed())
        {
            pendingTransactionFetch = Observable.interval(15, 30, TimeUnit.SECONDS)
                    .doOnNext(l -> checkPendingTransactions()).subscribe();
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
        List<Integer> filters = tokensService.getNetworkFilters();
        if (tokensService.getCurrentAddress() == null || filters.size() == 0 ||
                (eventFetch != null && !eventFetch.isDisposed())) { return; } //skip check if the service isn't set up or if a current check is in progress
        if (currentChainIndex >= filters.size()) currentChainIndex = 0;

        if (readTokenMoves(filters.get(currentChainIndex), nftCheck) && !nftCheck) //check NFTs for same chain on next iteration or advance to next chain
        {
            nftCheck = true;
        }
        else
        {
            nftCheck = false;
            currentChainIndex++;
        }
    }

    private boolean readTokenMoves(int chainId, boolean isNFT)
    {
        //check if this route has combined NFT
        NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        eventFetch = transactionsClient.readTransfers(tokensService.getCurrentAddress(), info, tokensService, isNFT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> handleMoveCheck(count, chainId, isNFT), this::gotReadErr);

        return info.usesSeparateNFTTransferQuery();
    }

    private void gotReadErr(Throwable e)
    {
        eventFetch = null;
        if (BuildConfig.DEBUG) e.printStackTrace();
    }

    private void handleMoveCheck(int count, int chainId, boolean isNFT)
    {
        if (count == TRANSFER_RESULT_MAX)
        {
            //there's more moves to fetch
            readTokenMoves(chainId, isNFT);
        }
        else
        {
            eventFetch = null;
        }
    }

    private void checkTransactionQueue()
    {
        if (tokensService.getCurrentAddress() == null) return;
        if (fetchTransactionDisposable == null)
        {
            Token t = tokensService.getRequiresTransactionUpdate(getPendingChains());

            if (t != null)
            {
                String tick = (t.isEthereum() && getPendingChains().contains(t.tokenInfo.chainId)) ? "*" : "";
                if (t.isEthereum() && BuildConfig.DEBUG)
                    Log.d(TAG,"Transaction check for: " + t.tokenInfo.chainId + " (" + t.getNetworkName() + ") " + tick);
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(t.tokenInfo.chainId);
                fetchTransactionDisposable =
                        transactionsClient.storeNewTransactions(tokensService.getCurrentAddress(), network, t.getAddress(), t.lastBlockCheck)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(transactions -> onUpdateTransactions(transactions, t), this::onTxError);
            }
        }
    }

    public Single<TransactionMeta[]> fetchAndStoreTransactions(int chainId, long lastTxTime)
    {
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId);
        return transactionsClient.fetchMoreTransactions(tokensService.getCurrentAddress(), network, lastTxTime);
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
        if (!TextUtils.isEmpty(tokensService.getCurrentAddress()))
        {
            return transactionsCache.fetchPendingTransactions(tokensService.getCurrentAddress());
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
        if (transactions.length == 0) return;

        if (BuildConfig.DEBUG) Log.d(TAG, "Queried for " + token.tokenInfo.name + " : " + transactions.length + " Network transactions");

        //should we only check here for chain moves?

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
                if (token == null && tx.to != null)
                    tokensService.addUnknownTokenToCheckPriority(new ContractAddress(tx.chainId, tx.to));
            }
        }
    }

    public void changeWallet(Wallet newWallet)
    {
        if (!newWallet.address.equalsIgnoreCase(tokensService.getCurrentAddress()))
        {
            stopAllChainUpdate();
            fetchTransactions();
        }
    }

    public void lostFocus()
    {
        tokensService.appOutOfFocus();
        stopAllChainUpdate();
    }

    private void stopAllChainUpdate()
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

        if (erc20EventCheckCycle != null && !erc20EventCheckCycle.isDisposed())
        {
            erc20EventCheckCycle.dispose();
            erc20EventCheckCycle = null;
        }
        eventTimer = null;
    }

    public void markPending(Transaction tx)
    {
        if (BuildConfig.DEBUG) Log.d(TAG,"Marked Pending Tx Chain: " + tx.chainId);
        tokensService.markChainPending(tx.chainId);
    }

    private void checkPendingTransactions()
    {
        final String currentWallet = tokensService.getCurrentAddress();
        Transaction[] pendingTxs = fetchPendingTransactions();
        if (BuildConfig.DEBUG) Log.d(TAG, "Checking " + pendingTxs.length + " Transactions");
        for (final Transaction tx : pendingTxs)
        {
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
                                    .map(ethBlock -> storeRawTx(ethBlock, tx.chainId, receipt, txDetails, currentWallet))
                                    .map(this::triggerTokenMoveCheck)
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
                    transactionsCache.markTransactionBlock(currentWallet, tx.hash, TRANSACTION_SEEN);
                }
            }).exceptionally(throwable -> {
                String c1 = throwable.getMessage(); //java.util.NoSuchElementException: No value present
                if (!TextUtils.isEmpty(c1) && c1.contains(NO_TRANSACTION_EXCEPTION) && tx.blockNumber.equals(String.valueOf(TRANSACTION_SEEN))) //we sighted this tx in the pool, now it's gone
                {
                    //transaction is no longer in pool or on chain. Cause: dropped from mining pool
                    //mark transaction as dropped
                    transactionsCache.markTransactionBlock(currentWallet, tx.hash, TRANSACTION_DROPPED);
                }
                return null;
            });
        }
    }

    private Transaction triggerTokenMoveCheck(Transaction transaction)
    {
        if (transaction.timeStamp == 0) return transaction;
        final String currentWallet = tokensService.getCurrentAddress();
        Token t = tokensService.getToken(transaction.chainId, transaction.to);
        if (t != null && transaction.hasInput() && (t.isERC20() || t.isERC721()))
        {
            //trigger ERC20 token move check
            TransactionType type = transaction.getTransactionType(currentWallet);
            switch (type)
            {
                case TRANSFER_TO:
                case RECEIVE_FROM:
                case TRANSFER_FROM:
                case RECEIVED:
                case SEND:
                    if (BuildConfig.DEBUG) Log.d(TAG, "Checking Token moves for " + t.getFullName());
                    readTokenMoves(transaction.chainId, t.isERC721());
                default:
                    break;
            }
        }

        return transaction;
    }

    private Transaction storeRawTx(EthBlock ethBlock, int chainId, EthGetTransactionReceipt receipt, EthTransaction txDetails, String currentWallet)
    {
        if (ethBlock != null && ethBlock.getBlock() != null && receipt != null && receipt.getResult() != null)
        {
            return transactionsCache.storeRawTx(new Wallet(currentWallet), chainId, txDetails, ethBlock.getBlock().getTimestamp().longValue(), receipt.getResult().getStatus().equals("0x1"));
        }
        else
        {
            return new Transaction();
        }
    }

    public Single<Boolean> wipeDataForWallet()
    {
        if (TextUtils.isEmpty(tokensService.getCurrentAddress())) return Single.fromCallable(() -> false);
        tokensService.stopUpdateCycle();
        stopAllChainUpdate();

        return transactionsCache.deleteAllForWallet(tokensService.getCurrentAddress());
    }
}
