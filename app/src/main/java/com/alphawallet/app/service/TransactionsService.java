package com.alphawallet.app.service;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.LongSparseArray;
import android.util.Pair;

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
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ContractAddress;

import org.web3j.exceptions.MessageDecodingException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

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

    private final LongSparseArray<Long> chainTransferCheckTimes = new LongSparseArray<>(); //TODO: Use this to coordinate token checks on chains
    private final LongSparseArray<Long> chainTransactionCheckTimes = new LongSparseArray<>();
    private static final LongSparseArray<BigInteger> currentBlocks = new LongSparseArray<>();
    private static final ConcurrentLinkedQueue<String> requiredTransactions = new ConcurrentLinkedQueue<>();

    private final static int TRANSACTION_DROPPED = -1;
    private final static int TRANSACTION_SEEN = -2;

    @Nullable
    private Disposable fetchTransactionDisposable;
    @Nullable
    private Disposable transactionCheckCycle;
    @Nullable
    private Disposable tokenTransferCheckCycle;
    @Nullable
    private Disposable eventFetch;
    @Nullable
    private Disposable pendingTransactionCheckCycle;
    @Nullable
    private Disposable transactionResolve;

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
        if (transactionCheckCycle == null || transactionCheckCycle.isDisposed())
        {
            transactionCheckCycle = Observable.interval(0, 15, TimeUnit.SECONDS)
                    .doOnNext(l -> checkTransactionQueue()).subscribe();
        }

        if (tokenTransferCheckCycle == null || tokenTransferCheckCycle.isDisposed())
        {
            tokenTransferCheckCycle = Observable.interval(2, 17, TimeUnit.SECONDS) //attempt to not interfere with transaction check
                    .doOnNext(l -> checkTransfers()).subscribe();
        }

        if (pendingTransactionCheckCycle == null || pendingTransactionCheckCycle.isDisposed())
        {
            pendingTransactionCheckCycle = Observable.interval(15, 15, TimeUnit.SECONDS)
                    .doOnNext(l -> checkPendingTransactions()).subscribe();
        }
    }

    public void resumeFocus()
    {
        if (!Utils.isAddressValid(tokensService.getCurrentAddress())) return;

        if (transactionCheckCycle == null || transactionCheckCycle.isDisposed()
            || pendingTransactionCheckCycle == null || pendingTransactionCheckCycle.isDisposed()
            || tokenTransferCheckCycle == null || tokenTransferCheckCycle.isDisposed())
        {
            startUpdateCycle();
        }

        tokensService.clearFocusToken();
        tokensService.walletInFocus();
    }

    public void startUpdateCycle()
    {
        chainTransferCheckTimes.clear();
        chainTransactionCheckTimes.clear();
        tokensService.startUpdateCycle();

        if (transactionCheckCycle == null || transactionCheckCycle.isDisposed())
        {
            fetchTransactions();
        }
    }

    /**
     * This uses the Etherscan API routes returning ERC20 and ERC721 token transfers, both incoming and outgoing.
     */
    private void checkTransfers()
    {
        List<Long> filters = tokensService.getNetworkFilters();
        if (tokensService.getCurrentAddress() == null || filters.size() == 0 ||
                (eventFetch != null && !eventFetch.isDisposed())) { return; } //skip check if the service isn't set up or if a current check is in progress

        if (currentChainIndex >= filters.size()) currentChainIndex = 0;
        readTokenMoves(filters.get(currentChainIndex), nftCheck); //check NFTs for same chain on next iteration or advance to next chain
        Pair<Integer, Boolean> pendingChainData = getNextChainIndex(currentChainIndex, nftCheck, filters);
        if (pendingChainData.first != currentChainIndex)
        {
            updateCurrentBlock(filters.get(currentChainIndex));
        }
        currentChainIndex = pendingChainData.first;
        nftCheck = pendingChainData.second;
    }

    private Pair<Integer, Boolean> getNextChainIndex(int currentIndex, boolean nftCheck, List<Long> filters)
    {
        if (filters.size() == 0) return new Pair<>(currentIndex, nftCheck);

        while (true)
        {
            currentIndex++;
            if (currentIndex >= filters.size())
            {
                nftCheck = !nftCheck;
                currentIndex = 0;
            }
            NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(filters.get(currentIndex));
            if (!nftCheck || info.usesSeparateNFTTransferQuery()) break;
        }

        return new Pair<>(currentIndex, nftCheck);
    }

    /**
     * Sets up the next check to be on the chain that we just detected a transaction on if the transaction appeared to be a token move
     * @param chainId
     * @param isNft
     */
    private void setNextTransferCheck(long chainId, boolean isNft)
    {
        List<Long> filters = tokensService.getNetworkFilters();
        if (filters.contains(chainId))
        {
            currentChainIndex = filters.indexOf(chainId);
            NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
            nftCheck = isNft && info.usesSeparateNFTTransferQuery();
        }
    }

    private void readTokenMoves(long chainId, boolean isNFT)
    {
        //check if this route has combined NFT
        final NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (info == null) return;
        if (isNFT)
        {
            tokensService.checkingChain(chainId);
        }
        Timber.tag(TAG).d("Check transfers: %s : NFT=%s", chainId, isNFT);
        eventFetch = transactionsClient.readTransfers(tokensService.getCurrentAddress(), info, tokensService, isNFT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> handleMoveCheck(info.chainId, isNFT), this::gotReadErr);
    }

    private void gotReadErr(Throwable e)
    {
        eventFetch = null;
        Timber.e(e);
    }

    private void handleMoveCheck(long chainId, boolean isNFT)
    {
        chainTransferCheckTimes.put(chainId, System.currentTimeMillis());
        if (isNFT) tokensService.checkingChain(0); //this flags to TokensService that the check is complete. This avoids race condition
        eventFetch = null;
    }

    private void checkTransactionQueue()
    {
        if (tokensService.getCurrentAddress() == null) return;
        if (fetchTransactionDisposable == null)
        {
            Token t = getRequiresTransactionUpdate();

            if (t != null)
            {
                String tick = (t.isEthereum() && getPendingChains().contains(t.tokenInfo.chainId)) ? "*" : "";
                if (BuildConfig.DEBUG)
                    Timber.tag(TAG).d("Transaction check for: %s (%s) %s", t.tokenInfo.chainId, t.getNetworkName(), tick);
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(t.tokenInfo.chainId);
                fetchTransactionDisposable =
                        transactionsClient.storeNewTransactions(tokensService, network, t.getAddress(), t.lastBlockCheck)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(transactions -> onUpdateTransactions(transactions, t), this::onTxError);
            }
        }
    }

    private Token getRequiresTransactionUpdate()
    {
        List<Long> chains = tokensService.getNetworkFilters();

        long timeIndex = 1;
        long oldestCheck = Long.MAX_VALUE;
        long checkChainId = 0;

        for (long chainId : chains)
        {
            long checkTime = chainTransactionCheckTimes.get(chainId, 0L);
            if (checkTime == 0L)
            {
                chainTransactionCheckTimes.put(chainId, timeIndex++);
            }
            else if (checkTime < oldestCheck)
            {
                oldestCheck = checkTime;
                checkChainId = chainId;
            }
        }

        //check lowest value
        if ((System.currentTimeMillis() - oldestCheck) > 45* DateUtils.SECOND_IN_MILLIS)
        {
            chainTransactionCheckTimes.put(checkChainId, System.currentTimeMillis());
            return tokensService.getServiceToken(checkChainId);
        }
        else
        {
            return null;
        }
    }

    private void updateCurrentBlock(final long chainId)
    {
        fetchCurrentBlock(chainId).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(blockValue -> currentBlocks.put(chainId, blockValue), onError -> currentBlocks.put(chainId, BigInteger.ZERO)).isDisposed();
    }

    private static Single<BigInteger> fetchCurrentBlock(final long chainId)
    {
        return Single.fromCallable(() -> {
            Web3j web3j = TokenRepository.getWeb3jService(chainId);
            EthBlock ethBlock =
                    web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
            String blockValStr = ethBlock.getBlock().getNumberRaw();
            if (!TextUtils.isEmpty(blockValStr) && blockValStr.length() > 2)
                return Numeric.toBigInt(blockValStr);
            else return currentBlocks.get(chainId, BigInteger.ZERO);
        });
    }

    public Single<TransactionMeta[]> fetchAndStoreTransactions(long chainId, long lastTxTime)
    {
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId);
        return transactionsClient.fetchMoreTransactions(tokensService, network, lastTxTime);
    }

    private List<Long> getPendingChains()
    {
        List<Long> pendingChains = new ArrayList<>();
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

        Timber.tag(TAG).d("Queried for %s : %s Network transactions", token.tokenInfo.name, transactions.length);

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

    public void restartService()
    {
        stopAllChainUpdate();
        tokensService.stopUpdateCycle();
        tokensService.startUpdateCycle();
        fetchTransactions();
    }

    public void stopService()
    {
        tokensService.stopUpdateCycle();
        stopAllChainUpdate();
        tokensService.walletOutOfFocus();
    }

    public void lostFocus()
    {
        tokensService.walletOutOfFocus();
    }

    private void stopAllChainUpdate()
    {
        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed()) { fetchTransactionDisposable.dispose(); }
        if (transactionCheckCycle != null && !transactionCheckCycle.isDisposed()) { transactionCheckCycle.dispose(); }
        if (pendingTransactionCheckCycle != null && !pendingTransactionCheckCycle.isDisposed()) { pendingTransactionCheckCycle.dispose(); }
        if (tokenTransferCheckCycle != null && !tokenTransferCheckCycle.isDisposed()) { tokenTransferCheckCycle.dispose(); }
        if (eventFetch != null && !eventFetch.isDisposed()) { eventFetch.dispose(); }

        fetchTransactionDisposable = null;
        transactionCheckCycle = null;
        pendingTransactionCheckCycle = null;
        tokenTransferCheckCycle = null;
        eventFetch = null;
        tokensService.checkingChain(0);
        chainTransferCheckTimes.clear();
        chainTransactionCheckTimes.clear();
    }

    public static void addTransactionHashFetch(String txHash, long chainId, String wallet)
    {
        String hashDef = getTxHashDef(txHash, chainId, wallet);
        if (!requiredTransactions.contains(hashDef))
        {
            requiredTransactions.add(hashDef);
        }
    }

    private static String getTxHashDef(String txHash, long chainId, String wallet)
    {
        return txHash + "-" + chainId + "-" + wallet;
    }

    private void checkTransactionFetchQueue()
    {
        String txHashData = getNextUncachedTx();

        if (txHashData != null)
        {
            String[] txData = txHashData.split("-");
            if (txData.length != 3) return;
            final String txHash = txData[0];
            long chainId = Long.parseLong(txData[1]);
            final String wallet = txData[2].toLowerCase();

            Timber.d("Transaction Queue: fetch tx: %s", requiredTransactions.size());
            transactionResolve = doTransactionFetch(txHash, chainId)
                    .map(tx -> storeTransactionIfValid(tx, wallet))
                    .delay(1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(tx -> checkTransactionFetchQueue(), Timber::w);
        }
        else
        {
            transactionResolve = null;
        }
    }

    private String storeTransactionIfValid(Transaction transaction, String wallet)
    {
        if (!TextUtils.isEmpty(transaction.blockNumber))
        {
            transactionsCache.putTransaction(new Wallet(wallet), transaction);
        }

        return transaction.blockNumber;
    }

    private Single<Transaction> doTransactionFetch(final String txHash, final long chainId)
    {
        final Web3j web3j = TokenRepository.getWeb3jService(chainId);
        return EventUtils.getTransactionDetails(txHash, web3j)
                .map(this::getBlockNumber)
                .flatMap(blockData -> joinBlockTimestamp(blockData, web3j))
                .map(blockData -> formTransaction(blockData, chainId))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }

    private Pair<EthTransaction, BigInteger> getBlockNumber(EthTransaction etx)
    {
        org.web3j.protocol.core.methods.response.Transaction fetchedTx = etx.getResult(); //try to read the transaction data
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

        return new Pair<>(etx, blockNumber);
    }

    private Single<Pair<EthTransaction, Long>> joinBlockTimestamp(Pair<EthTransaction, BigInteger> txData, Web3j web3j)
    {
        if (txData.second.compareTo(BigInteger.ZERO) > 0)
        {
            return EventUtils.getBlockDetails(txData.first.getResult().getBlockHash(), web3j)
                    .map(blockReceipt -> new Pair<>(txData.first, blockReceipt.getBlock().getTimestamp().longValue()));
        }
        else
        {
            return Single.fromCallable(() -> new Pair<>(txData.first, 0L));
        }
    }

    private Transaction formTransaction(Pair<EthTransaction, Long> txData, long chainId) throws IOException
    {
        if (txData.second > 0L)
        {
            return new Transaction(txData.first.getResult(), chainId,
                    checkTransactionReceipt(txData.first.getResult().getHash(), chainId), txData.second);
        }
        else
        {
            return new Transaction(); // blank Tx
        }
    }


    public void markPending(Transaction tx)
    {
        Timber.tag(TAG).d("Marked Pending Tx Chain: %s", tx.chainId);
        tokensService.markChainPending(tx.chainId);
    }

    public static BigInteger getCurrentBlock(long chainId)
    {
        BigInteger currentBlock = currentBlocks.get(chainId, BigInteger.ZERO);
        if (currentBlock.equals(BigInteger.ZERO))
        {
            currentBlock = fetchCurrentBlock(chainId).blockingGet();
            currentBlocks.put(chainId, currentBlock);
        }

        return currentBlock;
    }

    private void checkPendingTransactions()
    {
        if (transactionResolve == null || transactionResolve.isDisposed()) checkTransactionFetchQueue();
        final String currentWallet = tokensService.getCurrentAddress();
        Transaction[] pendingTxs = fetchPendingTransactions();
        Timber.tag(TAG).d("Checking %s Transactions", pendingTxs.length);
        for (final Transaction tx : pendingTxs)
        {
            doTransactionFetch(tx.hash, tx.chainId)
                    .map(fetchedTx -> storeTransactionIfValid(fetchedTx, currentWallet))
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(bNumber -> {
                        if (TextUtils.isEmpty(bNumber) && !tx.blockNumber.equals(String.valueOf(TRANSACTION_SEEN)))
                        {
                            //detected the tx in the pool, mark as seen
                            transactionsCache.markTransactionBlock(currentWallet, tx.hash, TRANSACTION_SEEN);
                            triggerTokenMoveCheck(tx);
                        }
                    }, Timber::w).isDisposed();
        }
    }

    private boolean checkTransactionReceipt(String txHash, long chainId) throws IOException
    {
        final Web3j web3j = TokenRepository.getWeb3jService(chainId);
        return web3j.ethGetTransactionReceipt(txHash)
                .send().getResult().isStatusOK();
    }

    private void triggerTokenMoveCheck(Transaction transaction)
    {
        if (transaction.timeStamp == 0) return;
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
                    Timber.tag(TAG).d("Trigger check for %s", t.getFullName());
                    //setup next check to be for this chain
                    setNextTransferCheck(transaction.chainId, t.isNonFungible());
                default:
                    break;
            }
        }
    }

    public void stopActivity()
    {
        tokensService.stopUpdateCycle();
        stopAllChainUpdate();
    }

    public Single<Boolean> wipeDataForWallet()
    {
        if (TextUtils.isEmpty(tokensService.getCurrentAddress())) return Single.fromCallable(() -> false);
        tokensService.stopUpdateCycle();
        stopAllChainUpdate();

        return transactionsCache.deleteAllForWallet(tokensService.getCurrentAddress());
    }

    public Single<Boolean> wipeTickerData()
    {
        return transactionsCache.deleteAllTickers();
    }

    private String getNextUncachedTx()
    {
        String txHashData = requiredTransactions.poll();
        while (txHashData != null)
        {
            String[] txData = txHashData.split("-");
            if (txData.length != 3) continue;
            Transaction check = transactionsCache.fetchTransaction(new Wallet(txData[2].toLowerCase()), txData[0]);
            if (check == null)
            {
                break;
            }
            else
            {
                txHashData = requiredTransactions.poll();
            }
        }

        return txHashData;
    }
}
