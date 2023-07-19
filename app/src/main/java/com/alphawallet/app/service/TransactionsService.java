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
import com.alphawallet.app.entity.transactionAPI.TransferFetchType;
import com.alphawallet.app.entity.transactions.TransferEvent;
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final TransactionNotificationService transactionNotificationService;
    private final TransactionLocalSource transactionsCache;
    private int currentChainIndex;
    private boolean firstCycle;
    private boolean firstTxCycle;
    private final LongSparseArray<Long> chainTransferCheckTimes = new LongSparseArray<>(); //TODO: Use this to coordinate token checks on chains
    private final LongSparseArray<Long> chainTransactionCheckTimes = new LongSparseArray<>();
    private static final LongSparseArray<CurrentBlockTime> currentBlocks = new LongSparseArray<>();
    private static final ConcurrentLinkedQueue<String> requiredTransactions = new ConcurrentLinkedQueue<>();
    private final LongSparseArray<TransferFetchType> apiFetchProgress = new LongSparseArray<>();

    private final static int TRANSACTION_DROPPED = -1;
    private final static int TRANSACTION_SEEN = -2;
    private final static long START_CHECK_DELAY = 3;
    private final static long CHECK_CYCLE = 15;
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
    private boolean fromBackground;

    public TransactionsService(TokensService tokensService,
                               EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                               TransactionsNetworkClientType transactionsClient,
                               TransactionLocalSource transactionsCache,
                               TransactionNotificationService transactionNotificationService)
    {
        this.tokensService = tokensService;
        this.ethereumNetworkRepository = ethereumNetworkRepositoryType;
        this.transactionsClient = transactionsClient;
        this.transactionsCache = transactionsCache;
        this.transactionNotificationService = transactionNotificationService;
    }

    public void fetchTransactionsFromBackground()
    {
        fromBackground = true;
        fetchTransactions();
    }

    private void fetchTransactions()
    {
        if (TextUtils.isEmpty(tokensService.getCurrentAddress())) return;

        currentChainIndex = 0;
        firstCycle = true;
        firstTxCycle = true;

        transactionsClient.checkRequiresAuxReset(tokensService.getCurrentAddress());

        if (fetchTransactionDisposable != null && !fetchTransactionDisposable.isDisposed())
            fetchTransactionDisposable.dispose();
        fetchTransactionDisposable = null;
        //reset transaction timers
        startTransactionCheckCycle(START_CHECK_DELAY);

        readTransferCycle();

        if (pendingTransactionCheckCycle == null || pendingTransactionCheckCycle.isDisposed())
        {
            pendingTransactionCheckCycle = Observable.interval(CHECK_CYCLE, CHECK_CYCLE, TimeUnit.SECONDS)
                    .doOnNext(l -> checkPendingTransactions()).subscribe();
        }
    }

    private void startTransactionCheckCycle(long checkCycleTime)
    {
        if (transactionCheckCycle == null || transactionCheckCycle.isDisposed())
        {
            transactionCheckCycle = Observable.interval(START_CHECK_DELAY, checkCycleTime, TimeUnit.SECONDS)
                    .doOnNext(l -> checkTransactionQueue()).subscribe();
        }
    }

    private void readTransferCycle()
    {
        if (tokenTransferCheckCycle != null && !tokenTransferCheckCycle.isDisposed()) tokenTransferCheckCycle.dispose();

        tokenTransferCheckCycle = Observable.interval(firstCycle ? START_CHECK_DELAY : (START_CHECK_DELAY * 15) + 1,
                        firstCycle ? CHECK_CYCLE / 3 : CHECK_CYCLE, TimeUnit.SECONDS)
                .doOnNext(l -> checkTransfers()).subscribe();
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
        apiFetchProgress.clear();

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
                (BuildConfig.DEBUG && eventFetch != null)) //don't allow multiple calls while debugging
        {
            return; //skip check if the service isn't set up
        }

        long chainId = filters.get(currentChainIndex);
        boolean initiateRead = readTokenMoves(chainId); //check NFTs for same chain on next iteration or advance to next chain

        if (initiateRead)
        {
            currentChainIndex = getNextChainIndex(currentChainIndex, chainId, filters);
        }
    }

    private int getNextChainIndex(int currentIndex, long chainId, List<Long> filters)
    {
        final NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (filters.size() == 0 || info == null) return 0;
        TransferFetchType[] availableTxTypes = info.getTransferQueriesUsed();

        TransferFetchType tfType = apiFetchProgress.get(chainId, TransferFetchType.ERC_20);

        if (tfType.ordinal() >= availableTxTypes.length) //available API routes may be zero if unsupported (eg custom network)
        {
            apiFetchProgress.put(chainId, TransferFetchType.ERC_20); // completed reads from this chain, reset to start
            currentIndex++;
        }
        else
        {
            apiFetchProgress.put(chainId, TransferFetchType.values()[tfType.ordinal() + 1]);
        }

        if (currentIndex >= filters.size())
        {
            currentIndex = 0;
            firstCycle = false;
        }

        return currentIndex;
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
            TransferFetchType nftSelection = ethereumNetworkRepository.getNetworkByChain(chainId).getTransferQueriesUsed().length > 1 ? TransferFetchType.ERC_721 : TransferFetchType.ERC_20;
            apiFetchProgress.put(chainId, isNft ? nftSelection : TransferFetchType.ERC_20);
        }
    }

    private boolean readTokenMoves(long chainId)
    {
        //check if this route has combined NFT
        final NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (info == null || info.getTransferQueriesUsed().length == 0)
        {
            return true;
        }

        if (eventFetch != null && !eventFetch.isDisposed())
        {
            return false;
        }

        TransferFetchType tfType = apiFetchProgress.get(chainId, TransferFetchType.ERC_20);
        if (tfType.ordinal() > 0)
        {
            tokensService.checkingChain(chainId);
        }

        Timber.tag(TAG).d("Check transfers: %s : NFT=%s", chainId, tfType.getValue());
        eventFetch = transactionsClient.readTransfers(tokensService.getCurrentAddress(), info, tokensService, tfType)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tfMap -> handleMoveCheck(info.chainId, tfType.ordinal() > 0, tfMap), this::gotReadErr);

        return true;
    }

    private void gotReadErr(Throwable e)
    {
        eventFetch = null;
        Timber.e(e);
    }

    private void handleMoveCheck(long chainId, boolean isNFT, Map<String, List<TransferEvent>> tfMap)
    {
        chainTransferCheckTimes.put(chainId, System.currentTimeMillis());
        if (isNFT) tokensService.checkingChain(0); //this flags to TokensService that the check is complete. This avoids race condition
        eventFetch = null;

        checkForIncomingTransfers(chainId, tfMap);
    }

    private void checkForIncomingTransfers(long chainId, Map<String, List<TransferEvent>> tfMap)
    {
        tfMap.entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .map(entry -> new AbstractMap.SimpleEntry<>(entry.getValue().get(0), entry.getKey()))
            .filter(entry -> entry.getKey().activityName.equalsIgnoreCase("received"))
            .forEach(entry -> fetchTransaction(tokensService.getCurrentAddress(), chainId, entry.getValue())
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(t -> onTransactionFetched(t, entry.getKey()), Timber::e));
    }

    private void onTransactionFetched(Transaction tx, TransferEvent te)
    {
        Token token = tokensService.getToken(tx.chainId, te.contractAddress);
        showTransactionNotification(tx, token, te);
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

                checkFirstCycleCompletion();
            }
        }
    }

    private void checkFirstCycleCompletion()
    {
        if (firstTxCycle && hasCompletedFirstCycle())
        {
            Timber.tag(TAG).d("Completed first cycle of checks");
            if (transactionCheckCycle != null && !transactionCheckCycle.isDisposed())
            {
                transactionCheckCycle.dispose();
            }
            transactionCheckCycle = null;
            firstTxCycle = false;
            startTransactionCheckCycle(CHECK_CYCLE);
        }
    }

    private boolean hasCompletedFirstCycle()
    {
        for (int i = 0; i < chainTransactionCheckTimes.size(); i++)
        {
            long checkTime = chainTransactionCheckTimes.valueAt(i);
            if (checkTime < ethereumNetworkRepository.getAvailableNetworkList().length + 1)
            {
                return false;
            }
        }

        return true;
    }

    private Token getRequiresTransactionUpdate()
    {
        List<Long> chains = tokensService.getNetworkFilters();

        long timeIndex = 1;
        long oldestCheck = Long.MAX_VALUE;
        long checkChainId = 0;

        for (long chainId : chains)
        {
            NetworkInfo thisInfo = ethereumNetworkRepository.getNetworkByChain(chainId);
            if (TextUtils.isEmpty(thisInfo.etherscanAPI))
            {
                continue;
            }
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
        if ((System.currentTimeMillis() - oldestCheck) > 45 * DateUtils.SECOND_IN_MILLIS)
        {
            chainTransactionCheckTimes.put(checkChainId, System.currentTimeMillis());
            return tokensService.getServiceToken(checkChainId);
        }
        else
        {
            return null;
        }
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
            else return currentBlocks.get(chainId, new CurrentBlockTime(BigInteger.ZERO)).blockNumber;
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
        for (Transaction tx : txList)
        {
            Token token = tokensService.getToken(tx.chainId, tx.to);
            boolean isSuccessfulContractTx = !tx.hasError() && tx.hasData();
            if (isSuccessfulContractTx && (token == null && tx.to != null))
            {
                tokensService.addUnknownTokenToCheckPriority(new ContractAddress(tx.chainId, tx.to));
            }
            else
            {
                showTransactionNotification(tx, token, null);
            }
        }
    }

    private void showTransactionNotification(Transaction transaction, Token token, TransferEvent te)
    {
        if (token != null)
        {
            transactionNotificationService.showNotification(transaction, token, te);
            if (fromBackground && !tokensService.isOnFocus())
            {
                fromBackground = false;
                stopService();
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

        currentChainIndex = 0;
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
        CurrentBlockTime currentBlock = currentBlocks.get(chainId, new CurrentBlockTime(BigInteger.ZERO));
        if (currentBlock.blockReadRequiresUpdate())
        {
            currentBlock = new CurrentBlockTime(fetchCurrentBlock(chainId).blockingGet());
            currentBlocks.put(chainId, currentBlock);
        }

        return currentBlock.blockNumber;
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

    public Single<Transaction> fetchTransaction(String currentAddress, long chainId, String hash)
    {
        return doTransactionFetch(hash, chainId)
                .map(fetchedTx -> {
                    transactionsCache.putTransaction(new Wallet(currentAddress), fetchedTx);
                    return fetchedTx;
                });
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

    private static class CurrentBlockTime
    {
        public final long readTime;
        public final BigInteger blockNumber;

        public CurrentBlockTime(BigInteger blockNo)
        {
            readTime = System.currentTimeMillis();
            blockNumber = blockNo;
        }

        public boolean blockReadRequiresUpdate()
        {
            // update every 10 seconds if required
            return blockNumber.equals(BigInteger.ZERO) || System.currentTimeMillis() > (readTime + 10 * DateUtils.SECOND_IN_MILLIS);
        }
    }
}
