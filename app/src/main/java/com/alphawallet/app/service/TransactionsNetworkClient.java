package com.alphawallet.app.service;

import static com.alphawallet.app.repository.EthereumNetworkBase.COVALENT;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OKX_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_TEST_ID;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.CovalentTransaction;
import com.alphawallet.app.entity.EtherscanEvent;
import com.alphawallet.app.entity.EtherscanTransaction;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.ERC1155Token;
import com.alphawallet.app.entity.tokens.ERC721Token;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.transactionAPI.TransferFetchType;
import com.alphawallet.app.entity.transactions.TransferEvent;
import com.alphawallet.app.repository.KeyProvider;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.app.repository.TransactionsRealmCache;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.repository.entity.RealmTransfer;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ContractAddress;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import timber.log.Timber;

public class TransactionsNetworkClient implements TransactionsNetworkClientType
{
    private static final String TAG = "TXNETCLIENT";
    private final int PAGESIZE = 800;
    private final int SYNC_PAGECOUNT = 2; //how many pages to read when we first sync the account - means we store the first 1600 transactions only
    private final int TRANSFER_RESULT_MAX = 500;
    //Note: if user wants to view transactions older than this, we fetch from etherscan on demand.
    //Generally this would only happen when watching extremely active accounts for curiosity
    private final String BLOCK_ENTRY = "-erc20blockCheck-";
    private final int AUX_DATABASE_ID = 25; //increment this to do a one off refresh the AUX database, in case of changed design etc

    private final int TRANSACTION_FETCH_LIMIT = 20; //Limit on the number of transactions fetched when we receive transfer updates
                                                    //Note that if the tx isn't fetched here, it is fetched automatically if the user scrolls down their activity list
                                                    //This speeds up the first time account sync - potentially there may be hundreds of new transactions here
    private final String DB_RESET = BLOCK_ENTRY + AUX_DATABASE_ID;
    private final String ETHERSCAN_API_KEY;
    private final String BSC_EXPLORER_API_KEY;
    private final String POLYGONSCAN_API_KEY;
    private final String AURORASCAN_API_KEY;
    private final KeyProvider keyProvider = KeyProviderFactory.get();
    private final String JSON_EMPTY_RESULT = "{\"result\":[]}";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final RealmManager realmManager;

    public TransactionsNetworkClient(
            OkHttpClient httpClient,
            Gson gson,
            RealmManager realmManager) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.realmManager = realmManager;

        BSC_EXPLORER_API_KEY = keyProvider.getBSCExplorerKey().length() > 0 ? "&apikey=" + keyProvider.getBSCExplorerKey() : "";
        ETHERSCAN_API_KEY = "&apikey=" + keyProvider.getEtherscanKey();
        POLYGONSCAN_API_KEY = keyProvider.getPolygonScanKey().length() > 3 ? "&apikey=" + keyProvider.getPolygonScanKey() : "";
        AURORASCAN_API_KEY = keyProvider.getAuroraScanKey().length() > 3 ? "&apikey=" + keyProvider.getAuroraScanKey() : "";
    }

    @Override
    public void checkRequiresAuxReset(String walletAddr)
    {
        //See if we require a refresh of transaction checks
        try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddr)))
        {
            instance.executeTransactionAsync(r -> {
                RealmAuxData checkMarker = r.where(RealmAuxData.class)
                        .like("instanceKey", BLOCK_ENTRY + "*")
                        .findFirst();

                if (checkMarker != null && checkMarker.getResult() != null && !checkMarker.getResult().equals(DB_RESET))
                {
                    RealmResults<RealmAuxData> realmEvents = r.where(RealmAuxData.class)
                            .findAll();
                    realmEvents.deleteAllFromRealm();
                    RealmResults<RealmTransfer> realmTransfers = r.where(RealmTransfer.class)
                            .findAll();
                    realmTransfers.deleteAllFromRealm();
                }
            });
        }
        catch (Exception e)
        {
            //
        }
    }

    /*
     *
     * Transaction sync strategy:
     *
     * 1. Always sync downward from latest block to the last scan to get newest transactions (latest transactions are the most important)
     *
     * 2. Check the count of synced transactions; sync up to 800
     * If we don't reach the previous fetch then execute a second fetch.
     *
     * On user scrolling down transaction list: if user is nearing bottom transaction limit begin fetching next 800
     *
     */

    /**
     * Scans the transactions for an address and stores new transactions in the database
     *
     * If first sync, sync from top and read 2 pages.
     * If not first sync, sync upward from existing entry.
     * If reading more than one page, blank database and start from first sync.
     *
     * SyncBlock: set to current lastBlock read if zero. If it is zero, then at finality remove all transactions before writing the new ones
     * Then write the lowest block we have read in.
     *
     *
     * @param svs
     * @param networkInfo
     * @param lastBlock
     * @return
     */
    @Override
    public Single<Transaction[]> storeNewTransactions(TokensService svs, NetworkInfo networkInfo, String tokenAddress, final long lastBlock)
    {
        return Single.fromCallable(() -> {
            long lastBlockNumber = lastBlock + 1;
            List<Transaction> sortedTx = null;
            try (Realm instance = realmManager.getRealmInstance(svs.getCurrentAddress()))
            {
                long syncToBlock = getTokenBlockRead(instance, networkInfo.chainId, TransferFetchType.ETHEREUM);
                if (syncToBlock == 0)
                {
                    lastBlockNumber = 0;
                }

                sortedTx = syncDownwards(svs, networkInfo, tokenAddress, lastBlockNumber, 999999999);

                if (sortedTx.size() > 0)
                {
                    String highestBlockStr = sortedTx.get(sortedTx.size() - 1).blockNumber;

                    storeLatestBlockRead(svs.getCurrentAddress(), networkInfo.chainId, tokenAddress, highestBlockStr);

                    if (syncToBlock == 0 || sortedTx.size() == PAGESIZE * SYNC_PAGECOUNT)
                    {
                        //blank all entries
                        eraseAllTransactions(instance, networkInfo.chainId);
                        writeTokenBlockRead(instance, networkInfo.chainId, Long.parseLong(sortedTx.get(0).blockNumber), TransferFetchType.ETHEREUM);
                    }

                    //now write transactions
                    writeTransactions(instance, sortedTx);
                }
            }
            catch (JSONException e)
            {
                //silent fail
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return sortedTx != null ? sortedTx.toArray(new Transaction[0]) : new Transaction[0];
        }).subscribeOn(Schedulers.io());
    }

    /**
     * read PAGESIZE*2 transactions down from startingBlockNumber
     *
     * Note that this call is the only place that the 'earliest transaction' block can be written from.
     */
    private List<Transaction> syncDownwards(TokensService svs, NetworkInfo networkInfo, String tokenAddress, long lowBlockNumber, long highBlockNumber) throws Exception
    {
        int page = 1;
        HashMap<String, Transaction> txMap = new HashMap<>();
        boolean continueReading = true;

        while (continueReading) // only SYNC_PAGECOUNT pages at a time for each check, to avoid congestion
        {
            EtherscanTransaction[] myTxs = readTransactions(networkInfo, svs, tokenAddress, String.valueOf(lowBlockNumber), String.valueOf(highBlockNumber), false, page++);
            if (myTxs.length == 0) break;
            populateTransactionMap(txMap, myTxs, networkInfo.chainId); //use all transactions (wallet address null)

            if (page > SYNC_PAGECOUNT) continueReading = false;

            if (myTxs.length < PAGESIZE)
            {
                continueReading = false;
            }
        }

        return sortTransactions(txMap.values());
    }

    private void populateTransactionMap(HashMap<String, Transaction> txMap, EtherscanTransaction[] myTxs, long chainId)
    {
        for (EtherscanTransaction etx : myTxs)
        {
            Transaction tx = etx.createTransaction(null, chainId);
            if (tx != null)
            {
                txMap.put(tx.hash, tx);
            }
        }
    }

    private EtherscanTransaction[] getEtherscanTransactions(String response) throws JSONException
    {
        JSONObject stateData;
        try
        {
            stateData = new JSONObject(response);
        }
        catch (JSONException e)
        {
            Timber.w(e);
            return new EtherscanTransaction[0];
        }

        JSONArray orders = stateData.getJSONArray("result");
        return gson.fromJson(orders.toString(), EtherscanTransaction[].class);
    }

    private CovalentTransaction[] getCovalentTransactions(String response, String walletAddress) throws JSONException
    {
        if (response == null || response.length() < 80)
        {
            return new CovalentTransaction[0];
        }
        JSONObject stateData = new JSONObject(response);
        JSONObject data = stateData.getJSONObject("data");
        JSONArray orders = data.getJSONArray("items");
        CovalentTransaction[] ctxs = gson.fromJson(orders.toString(), CovalentTransaction[].class);
        //reformat list to remove any transactions already seen
        List<CovalentTransaction> cvList = new ArrayList<>();
        try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
        {
            for (CovalentTransaction ctx : ctxs)
            {
                RealmTransaction realmTx = instance.where(RealmTransaction.class)
                        .equalTo("hash", ctx.tx_hash)
                        .findFirst();

                if (realmTx == null)
                {
                    cvList.add(ctx);
                }
            }
        }

        return cvList.toArray(new CovalentTransaction[0]);
    }

    private EtherscanEvent[] getEtherscanEvents(String response) throws JSONException
    {
        JSONObject stateData = new JSONObject(response);
        JSONArray orders = stateData.getJSONArray("result");
        return gson.fromJson(orders.toString(), EtherscanEvent[].class);
    }

    private void writeTransactions(Realm instance, List<Transaction> txList) throws Exception
    {
        if (txList.size() == 0) return;

        instance.executeTransaction(r -> {
            for (Transaction tx : txList)
            {
                String oldGasFeeMax = "";
                String oldPriorityFee = "";
                RealmTransaction realmTx = r.where(RealmTransaction.class)
                        .equalTo("hash", tx.hash)
                        .findFirst();

                if (realmTx == null)
                {
                    realmTx = r.createObject(RealmTransaction.class, tx.hash);
                }
                else
                {
                    oldGasFeeMax = !TextUtils.isEmpty(realmTx.getMaxFeePerGas()) ? realmTx.getMaxFeePerGas() : tx.maxFeePerGas;
                    oldPriorityFee = !TextUtils.isEmpty(realmTx.getPriorityFee()) ? realmTx.getPriorityFee() : tx.maxPriorityFee;
                }

                TransactionsRealmCache.fill(realmTx, tx);
                realmTx.setMaxFeePerGas(oldGasFeeMax);
                realmTx.setMaxPriorityFee(oldPriorityFee);
                r.insertOrUpdate(realmTx);
            }
        });
    }

    private EtherscanTransaction[] readTransactions(NetworkInfo networkInfo, TokensService svs, String tokenAddress, String lowBlock, String highBlock, boolean ascending, int page) throws JSONException
    {
        if (networkInfo == null) return new EtherscanTransaction[0];
        if (networkInfo.etherscanAPI.contains(COVALENT))
        {
            return readCovalentTransactions(svs, tokenAddress, networkInfo, ascending, page, PAGESIZE);
        }
        else if (networkInfo.chainId == OKX_ID)
        {
            return new EtherscanTransaction[0];
        }

        String result = null;
        String fullUrl;

        String sort = "asc";
        if (!ascending) sort = "desc";

        if (!TextUtils.isEmpty(networkInfo.etherscanAPI))
        {
            StringBuilder sb = new StringBuilder();
            sb.append(networkInfo.etherscanAPI);
            sb.append("module=account&action=txlist&address=");
            sb.append(tokenAddress);
            if (ascending)
            {
                sb.append("&startblock=");
                sb.append(lowBlock);
                sb.append("&endblock=999999999&sort=");
            }
            else
            {
                sb.append("&startblock=");
                sb.append(lowBlock);
                sb.append("&endblock=");
                sb.append(highBlock);
                sb.append("&sort=");
            }

            sb.append(sort);
            if (page > 0)
            {
                sb.append("&page=");
                sb.append(page);
                sb.append("&offset=");
                sb.append(PAGESIZE);
            }

            sb.append(getNetworkAPIToken(networkInfo));

            fullUrl = sb.toString();

            if (networkInfo.isCustom && !Utils.isValidUrl(networkInfo.etherscanAPI))
            {
                return new EtherscanTransaction[0];
            }

            Request request = new Request.Builder()
                .url(fullUrl)
                .get()
                .build();

            try (okhttp3.Response response = httpClient.newCall(request).execute())
            {
                if (response.body() == null) return new EtherscanTransaction[0];
                if (response.code() / 200 == 1)
                {
                    result = response.body().string();
                    if (result.length() >= 80 && !result.contains("No transactions found"))
                    {
                        return getEtherscanTransactions(result);
                    }
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
        }

        return new EtherscanTransaction[0];
    }

    /**
     * This is the function called when a user scrolls to the bottom of a transaction list.
     * First try to provide more transactions from the stored database. If there aren't any more then populate another page (800) from etherscan
     *
     * TODO: We should also check transfers over the same block range
     *
     * @param svs
     * @param network
     * @param lastTxTime
     * @return
     */
    @Override
    public Single<TransactionMeta[]> fetchMoreTransactions(TokensService svs, NetworkInfo network, long lastTxTime)
    {
        return Single.fromCallable(() -> {
            List<TransactionMeta> txList = fetchOlderThan(svs.getCurrentAddress(), lastTxTime, network.chainId);
            if (txList.size() < 800)
            {
                //fetch another page and return unless we already have the oldest Tx
                long oldestTxTime = txList.size() > 0 ? txList.get(txList.size() - 1).getTimeStampSeconds() : lastTxTime;
                try (Realm instance = realmManager.getRealmInstance(new Wallet(svs.getCurrentAddress())))
                {
                    long oldestBlockRead = getOldestBlockRead(instance, network.chainId, oldestTxTime);
                    long oldestPossibleBlock = getFirstTransactionBlock(instance, network.chainId, svs.getCurrentAddress());
                    Timber.d("DIAGNOSE: " + oldestBlockRead + " : " + oldestPossibleBlock);
                    if (oldestBlockRead > 0 && oldestBlockRead != oldestPossibleBlock)
                    {
                        List<Transaction> syncTx = syncDownwards(svs, network, svs.getCurrentAddress(), 0, oldestBlockRead);
                        writeTransactions(instance, syncTx);
                    }

                    //now re-read last blocks from DB
                    txList = fetchOlderThan(svs.getCurrentAddress(), lastTxTime, network.chainId);
                }
                catch (Exception e)
                {
                    //
                }
            }

            return txList.toArray(new TransactionMeta[0]);
        });
    }

    /**
     * Fetch the Token transfers observed on this wallet
     * @param walletAddress
     * @param networkInfo
     * @param svs
     * @param tfType
     * @return
     */
    @Override
    public Single<Map<String, List<TransferEvent>>> readTransfers(String walletAddress, NetworkInfo networkInfo, TokensService svs, TransferFetchType tfType)
    {
        return Single.fromCallable(() -> {
            //get latest block read
            Map<String, List<TransferEvent>> tfMap = new HashMap<>();
            try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
            {
                EtherscanEvent[] events = fetchEvents(instance, walletAddress, networkInfo, tfType);
                tfMap = processEtherscanEvents(instance, walletAddress, networkInfo, svs, events, tfType);
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
            return tfMap;
        }).observeOn(Schedulers.io());
    }

    private EtherscanEvent[] fetchEvents(Realm instance, String walletAddress, NetworkInfo networkInfo, TransferFetchType tfType) throws JSONException
    {
        EtherscanEvent[] events;
        List<EtherscanEvent> eventList = new ArrayList<>();
        //get oldest record
        long lastBlockFound = getTokenBlockRead(instance, networkInfo.chainId, tfType);

        if (networkInfo.chainId == OKX_ID)
        {
           events = OkLinkService.get(httpClient).getEtherscanEvents(walletAddress, lastBlockFound, tfType);
           eventList = new ArrayList<>(Arrays.asList(events));
        }
        else
        {
            long upperBlock = 99999999999L;
            long lowerBlock = (lastBlockFound == 0) ? 1 : lastBlockFound;

            while (true)
            {
                String fetchTransactions = readNextTxBatch(walletAddress, networkInfo, upperBlock, lowerBlock, tfType.getValue());
                events = getEtherscanEvents(fetchTransactions);

                if (events.length == 0)
                {
                    break;
                }

                upperBlock = Long.parseLong(events[events.length - 1].blockNumber) - 1;
                eventList.addAll(Arrays.asList(events));
                if (events.length == TRANSFER_RESULT_MAX && eventList.size() > TRANSFER_RESULT_MAX)
                {
                    //If still above the last read, blank all following reads to avoid 'sync-holes'. The new events read above will be added on the return
                    //TODO: See above - need to sync the lowest block here to the lowest block in the transaction reads
                    //      This is so we can add a 'view all transactions' button which takes the user to the relevant Etherscan/Blockscout page.
                    blankTransferData(instance, networkInfo.chainId);
                }

                if (eventList.size() > TRANSFER_RESULT_MAX || events.length < TRANSFER_RESULT_MAX)
                {
                    break;
                }
            }
        }

        return eventList.toArray(new EtherscanEvent[0]);
    }

    private Map<String, List<TransferEvent>> processEtherscanEvents(Realm instance, String walletAddress, NetworkInfo networkInfo,
                                        TokensService svs, EtherscanEvent[] events, TransferFetchType tfType) throws Exception
    {
        if (events.length == 0)
        {
            return new HashMap<>();
        }

        long lastBlockChecked = getTokenBlockRead(instance, networkInfo.chainId, tfType);

        //Now update tokens if we don't already know this token
        Map<String, Boolean> tokenTypes = writeTokens(walletAddress, networkInfo, events, svs, tfType);

        //we know all these events are relevant to the wallet, and they are all ERC20 events
        Pair<Long, Map<String, List<TransferEvent>>> txPair = writeEvents(instance, events, walletAddress, networkInfo, tokenTypes, lastBlockChecked);

        //and update the top block read
        writeTokenBlockRead(instance, networkInfo.chainId, txPair.first + 1, tfType);

        return txPair.second;
    }

    private Map<String, Boolean> writeTokens(String walletAddress, NetworkInfo networkInfo, EtherscanEvent[] events, TokensService svs, TransferFetchType tfType)
    {
        Map<String, List<EtherscanEvent>> eventMap = getEventMap(events);
        Map<String, Boolean> tokenTypeMap = new HashMap<>();

        for (Map.Entry<String, List<EtherscanEvent>> entry : eventMap.entrySet())
        {
            String contract = entry.getKey();
            EtherscanEvent ev0 = entry.getValue().get(0);
            Token token = svs.getToken(networkInfo.chainId, contract);
            boolean newToken = false;

            int tokenDecimal = calcTokenDecimals(ev0);

            if ((tfType == TransferFetchType.ERC_1155 || ev0.isERC1155(entry.getValue())) &&
                    (token == null || token.getInterfaceSpec() != ContractType.ERC1155))
            {
                token = createNewERC1155Token(entry.getValue().get(0), networkInfo, walletAddress);
                Timber.tag(TAG).d("Discover ERC1155: " + ev0.tokenName + " (" + ev0.tokenSymbol + ")");
                newToken = true;
            }
            if (tokenDecimal == -1 && (token == null ||
                    ( token.getInterfaceSpec() != ContractType.ERC721 &&
                            token.getInterfaceSpec() != ContractType.ERC721_LEGACY &&
                            token.getInterfaceSpec() != ContractType.ERC721_TICKET &&
                            token.getInterfaceSpec() != ContractType.ERC721_UNDETERMINED &&
                            token.getInterfaceSpec() != ContractType.ERC1155)))
            {
                token = createNewERC721Token(entry.getValue().get(0), networkInfo, walletAddress, false);
                token.setTokenWallet(walletAddress);
                newToken = true;
                Timber.tag(TAG).d("Discover NFT: " + ev0.tokenName + " (" + ev0.tokenSymbol + ")");
            }
            else if (tokenDecimal >= 0 && token == null)
            {
                TokenInfo info = new TokenInfo(ev0.contractAddress, ev0.tokenName, ev0.tokenSymbol, tokenDecimal, true, networkInfo.chainId);
                token = new Token(info, BigDecimal.ZERO, 0, networkInfo.getShortName(),
                        tokenDecimal > 0 ? ContractType.ERC20 : ContractType.MAYBE_ERC20);
                token.setTokenWallet(walletAddress);
                newToken = true;
                Timber.tag(TAG).d("Discover ERC20: " + ev0.tokenName + " (" + ev0.tokenSymbol + ")");
            }
            else if (token == null)
            {
                svs.addUnknownTokenToCheck(new ContractAddress(networkInfo.chainId, ev0.contractAddress));
                Timber.tag(TAG).d("Discover unknown: " + ev0.tokenName + " (" + ev0.tokenSymbol + ")");
                continue;
            }

            if (token.isNonFungible())
            {
                writeAssets(eventMap, token, walletAddress, contract, svs, newToken, tfType);
            }
            else
            {
                //instruct tokensService to update balance
                svs.addBalanceCheck(token);
            }

            //Send to storage as soon as each token is done
            token.lastTxTime = System.currentTimeMillis();
            tokenTypeMap.put(contract, token.isNonFungible());
        }

        return tokenTypeMap;
    }

    private int calcTokenDecimals(EtherscanEvent ev0)
    {
        int tokenDecimal = (!TextUtils.isEmpty(ev0.tokenDecimal) && Character.isDigit(ev0.tokenDecimal.charAt(0))) ? Integer.parseInt(ev0.tokenDecimal) : -1;

        if (tokenDecimal < 1 &&
                (ev0.tokenID != null || ev0.tokenIDs != null) &&
                (ev0.tokenDecimal == null || ev0.tokenDecimal.equals("0")))
        {
            tokenDecimal = -1;
        }

        return tokenDecimal;
    }

    private void writeAssets   (Map<String, List<EtherscanEvent>> eventMap, Token token, String walletAddress,
                                String contractAddress, TokensService svs, boolean newToken, TransferFetchType tfType)
    {
        HashSet<BigInteger> additions = new HashSet<>();
        HashSet<BigInteger> removals = new HashSet<>();

        //run through addition/removal in chronological order
        for (EtherscanEvent ev : eventMap.get(contractAddress))
        {
            BigInteger tokenId = getTokenId(ev.tokenID);

            if (tokenId.compareTo(new BigInteger("-1")) == 0) continue;

            if (ev.to.equalsIgnoreCase(walletAddress))
            {
                additions.add(tokenId);
                removals.remove(tokenId);
            }
            else
            {
                removals.add(tokenId);
                additions.remove(tokenId);
            }
        }

        if (additions.size() > 0 && newToken)
        {
            if (token.getInterfaceSpec() != ContractType.ERC1155)
            {
                token.setInterfaceSpec(ContractType.ERC721);
            }
        }

        if (additions.size() > 0 || removals.size() > 0)
        {
            svs.updateAssets(token, new ArrayList<>(additions), new ArrayList<>(removals));
        }
    }

    private String readNextTxBatch(String walletAddress, NetworkInfo networkInfo, long upperBlock, long lowerBlock, String queryType)
    {
        if (TextUtils.isEmpty(networkInfo.etherscanAPI) || networkInfo.etherscanAPI.contains(COVALENT)) return JSON_EMPTY_RESULT; //Covalent transfers are handled elsewhere
        String result = JSON_EMPTY_RESULT;
        if (lowerBlock == 0) lowerBlock = 1;

        String fullUrl = networkInfo.etherscanAPI + "module=account&action=" + queryType +
            "&startblock=" + lowerBlock + "&endblock=" + upperBlock +
            "&address=" + walletAddress +
            "&page=1&offset=" + TRANSFER_RESULT_MAX +
            "&sort=desc" + getNetworkAPIToken(networkInfo);

        if (networkInfo.isCustom && !Utils.isValidUrl(networkInfo.etherscanAPI))
        {
            return JSON_EMPTY_RESULT;
        }

        Request request = new Request.Builder()
            .url(fullUrl)
            .header("User-Agent", "Chrome/74.0.3729.169")
            .method("GET", null)
            .addHeader("Content-Type", "application/json")
            .build();

        try (okhttp3.Response response = httpClient.newCall(request).execute())
        {
            if (response.code() / 200 == 1)
            {
                result = response.body().string();
                if (result.length() < 80 && result.contains("No transactions found"))
                {
                    result = JSON_EMPTY_RESULT;
                }
            }
        }
        catch (Exception e)
        {
            if (BuildConfig.DEBUG) Timber.e(e);
        }

        return result;
    }

    private String getNetworkAPIToken(NetworkInfo networkInfo)
    {
        if (networkInfo.etherscanAPI.contains("etherscan"))
        {
            return ETHERSCAN_API_KEY;
        }
        else if (networkInfo.chainId == BINANCE_MAIN_ID)
        {
            return BSC_EXPLORER_API_KEY;
        }
        else if (networkInfo.chainId == POLYGON_ID || networkInfo.chainId == POLYGON_TEST_ID)
        {
            return POLYGONSCAN_API_KEY;
        }
        else if (networkInfo.chainId == AURORA_MAINNET_ID || networkInfo.chainId == AURORA_TESTNET_ID)
        {
            return AURORASCAN_API_KEY;
        }
        else
        {
            return "";
        }
    }

    private EtherscanTransaction[] readCovalentTransactions(TokensService svs, String accountAddress, NetworkInfo networkInfo, boolean ascending, int page, int pageSize) throws JSONException
    {
        String covalent = "" + networkInfo.chainId + "/address/" + accountAddress.toLowerCase() + "/transactions_v2/?";
        String args = "block-signed-at-asc=" + (ascending ? "true" : "false") + "&page-number=" + (page - 1) + "&page-size=" +
                pageSize + "&key=" + keyProvider.getCovalentKey(); //read logs to get all the transfers
        String fullUrl = networkInfo.etherscanAPI.replace(COVALENT, covalent);
        String result = null;

        Request request = new Request.Builder()
                .url(fullUrl + args)
                .get()
                .addHeader("Content-Type", "application/json")
                .build();

        try (okhttp3.Response response = httpClient.newCall(request).execute())
        {
            if (response.body() == null) return new EtherscanTransaction[0];

            result = response.body().string();
            if (result.length() < 80 && result.contains("No transactions found"))
            {
                return new EtherscanTransaction[0];
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
            return new EtherscanTransaction[0];
        }

        CovalentTransaction[] covalentTransactions = getCovalentTransactions(result, svs.getCurrentAddress());

        EtherscanTransaction[] unhandledTxs = processCovalentEvents(covalentTransactions, svs, networkInfo);

        return unhandledTxs;
    }

    private EtherscanTransaction[] processCovalentEvents(CovalentTransaction[] covalentTransactions, TokensService svs, NetworkInfo networkInfo)
    {
        EtherscanEvent[] events = CovalentTransaction.toEtherscanEvents(covalentTransactions);
        try (Realm instance = realmManager.getRealmInstance(new Wallet(svs.getCurrentAddress())))
        {
            processEtherscanEvents(instance, svs.getCurrentAddress(), networkInfo,
                    svs, events, TransferFetchType.ERC_20);
        }
        catch (Exception e)
        {
            //
        }

        EtherscanTransaction[] rawTransactions = CovalentTransaction.toRawEtherscanTransactions(covalentTransactions, networkInfo);

        //List of transaction hashes that still need handling
        return rawTransactions;
    }

    private long getTokenBlockRead(Realm instance, long chainId, TransferFetchType tfType)
    {
        RealmAuxData rd = instance.where(RealmAuxData.class)
                .equalTo("instanceKey", BLOCK_ENTRY + chainId)
                .findFirst();

        if (rd == null)
        {
            return 0L;
        }
        else
        {
            switch (tfType)
            {
                case ETHEREUM:
                    return rd.getBaseChainBlock();
                case ERC_20:
                default:
                    return rd.getResultTime();
                case ERC_721:
                    return rd.getResultReceivedTime();
                case ERC_1155:
                    return rd.getChainId();
            }
        }
    }

    private void writeTokenBlockRead(Realm instance, long chainId, long lastBlockChecked, TransferFetchType tfType)
    {
        instance.executeTransaction(r -> {
            RealmAuxData rd = r.where(RealmAuxData.class)
                    .equalTo("instanceKey", BLOCK_ENTRY + chainId)
                    .findFirst();

            if (rd == null)
            {
                rd = r.createObject(RealmAuxData.class, BLOCK_ENTRY + chainId);
                rd.setResult(DB_RESET);
            }

            switch (tfType)
            {
                case ETHEREUM:
                    rd.setBaseChainBlock(lastBlockChecked);
                    break;
                case ERC_20:
                default:
                    rd.setResultTime(lastBlockChecked);
                    break;
                case ERC_721:
                    rd.setResultReceivedTime(lastBlockChecked);
                    break;
                case ERC_1155:
                    rd.setChainId(lastBlockChecked);
                    break;
            }
        });
    }

    private long getOldestBlockRead(Realm instance, long chainId, long lastTxTime)
    {
        long txBlockRead = 0;
        try
        {
            RealmResults<RealmTransaction> txs = instance.where(RealmTransaction.class)
                    .equalTo("chainId", chainId)
                    .sort("timeStamp", Sort.ASCENDING)
                    .limit(1)
                    .findAll();

            if (txs != null && txs.size() > 0)
            {
                String blockNumber = txs.first().getBlockNumber();
                txBlockRead = Long.parseLong(blockNumber);
            }
        }
        catch (Exception e)
        {
            //
        }

        return txBlockRead;
    }

    private long getFirstTransactionBlock(Realm instance, long chainId, String walletAddress)
    {
        long txBlockRead = 0;
        try
        {
            RealmToken realmToken = instance.where(RealmToken.class)
                    .equalTo("address", databaseKey(chainId, walletAddress))
                    .findFirst();

            if (realmToken != null)
            {
                txBlockRead = realmToken.getEarliestTransactionBlock();
            }
        }
        catch (Exception e)
        {
            //
        }

        return txBlockRead;
    }

    private List<TransactionMeta> fetchOlderThan(String walletAddress, long fetchTime, long chainId)
    {
        List<TransactionMeta> metas = new ArrayList<>();
        try (Realm instance = realmManager.getRealmInstance(walletAddress.toLowerCase()))
        {
            RealmResults<RealmTransaction> txs = instance.where(RealmTransaction.class)
                    .sort("timeStamp", Sort.DESCENDING)
                    .lessThan("timeStamp", fetchTime)
                    .equalTo("chainId", chainId)
                    .limit(PAGESIZE)
                    .findAll();

            for (RealmTransaction item : txs)
            {
                TransactionMeta tm = new TransactionMeta(item.getHash(), item.getTimeStamp(), item.getTo(), item.getChainId(), item.getBlockNumber());
                metas.add(tm);
            }
        }
        catch (Exception e)
        {
            //
        }

        return metas;
    }

    private void storeLatestBlockRead(String walletAddress, long chainId, String tokenAddress, String lastBlockRead)
    {
        try (Realm instance = realmManager.getRealmInstance(walletAddress))
        {
            instance.executeTransactionAsync(r -> {
                RealmToken realmToken = r.where(RealmToken.class)
                        .equalTo("address", databaseKey(chainId, tokenAddress))
                        .findFirst();

                if (realmToken != null)
                {
                    realmToken.setLastBlock(Long.parseLong(lastBlockRead));
                    realmToken.setLastTxTime(System.currentTimeMillis());
                }
            });
        }
        catch (Exception e)
        {
            //
        }
    }

    private void storeEarliestBlockRead(Realm instance, long chainId, String walletAddress, long earliestBlock)
    {
        try
        {
            instance.executeTransaction(r -> {
                RealmToken realmToken = r.where(RealmToken.class)
                        .equalTo("address", databaseKey(chainId, walletAddress))
                        .findFirst();

                if (realmToken != null)
                {
                    realmToken.setEarliestTransactionBlock(earliestBlock);
                }
            });
        }
        catch (Exception e)
        {
            //
        }
    }

    public void deleteAllChainTransactions(Realm instance, long chainId, String walletAddress)
    {
        try
        {
            instance.executeTransaction(r -> {
                RealmResults<RealmTransaction> txs = r.where(RealmTransaction.class)
                        .equalTo("chainId", chainId)
                        .findAll();

                if (txs != null && txs.size() > 0)
                {
                    txs.deleteAllFromRealm();
                }

                resetBlockRead(r, chainId, walletAddress);
            });
        }
        catch (Exception e)
        {
            //
        }
    }

    private void resetBlockRead(Realm r, long chainId, String walletAddress)
    {
        RealmToken realmToken = r.where(RealmToken.class)
                .equalTo("address", databaseKey(chainId, walletAddress))
                .findFirst();

        if (realmToken != null)
        {
            realmToken.setEarliestTransactionBlock(0);
            realmToken.setLastBlock(0);
            realmToken.setLastTxTime(0);
        }
    }

    private Pair<Long, Map<String, List<TransferEvent>>> writeEvents(Realm instance, EtherscanEvent[] events, String walletAddress,
                                                                     @NonNull NetworkInfo networkInfo, final Map<String, Boolean> tokenTypes, long lastBlockRead) throws Exception
    {
        String TO_TOKEN = "[TO_ADDRESS]";
        String FROM_TOKEN = "[FROM_ADDRESS]";
        String AMOUNT_TOKEN = "[AMOUNT_TOKEN]";
        String VALUES = "from,address," + FROM_TOKEN + ",to,address," + TO_TOKEN + ",amount,uint256," + AMOUNT_TOKEN;

        HashSet<String> txFetches = new HashSet<>();
        long highestBlockRead = lastBlockRead - 1; // -1 because we +1 when writing the value - if there's no events then keep value the same
        Map<String, List<TransferEvent>> transferEventMap = new HashMap<>();
        Map<String, Transaction> txWriteMap = new HashMap<>();
        //write event list
        for (EtherscanEvent ev : events)
        {
            long eventBlockNumber = Long.parseLong(ev.blockNumber);
            if (eventBlockNumber < lastBlockRead)
            {
                continue;
            }

            boolean scanAsNFT = tokenTypes.getOrDefault(ev.contractAddress, false);
            Transaction tx = scanAsNFT ? ev.createNFTTransaction(networkInfo) : ev.createTransaction(networkInfo);

            //find tx name
            String activityName = tx.getEventName(walletAddress);
            //Etherscan sometimes interprets NFT transfers as FT's
            //TODO: Handle ERC1155 multiple token/batch transfers
            //For now; just use first token
            ev.patchFirstTokenID();

            //Sometimes the value for TokenID in Etherscan is in the value field.
            String tokenValue = (scanAsNFT && ev.tokenID != null) ? ev.tokenID : (ev.value != null) ? ev.value : "0";

            String valueList = VALUES.replace(TO_TOKEN, ev.to).replace(FROM_TOKEN, ev.from).replace(AMOUNT_TOKEN, tokenValue);
            if (!TextUtils.isEmpty(ev.tokenValue))
            {
                valueList = valueList + "count,uint256," + ev.tokenValue;
            }

            List<TransferEvent> thisHashList = transferEventMap.computeIfAbsent(tx.hash, k -> new ArrayList<>());
            thisHashList.add(new TransferEvent(valueList, activityName, ev.contractAddress, tokenValue));

            //add to transaction write list
            txWriteMap.put(ev.contractAddress, tx);

            if (eventBlockNumber > highestBlockRead)
            {
                highestBlockRead = eventBlockNumber;
            }
        }

        instance.executeTransaction(r -> {
            storeTransferData(r, networkInfo.chainId, transferEventMap);
            storeTransactions(r, txWriteMap, networkInfo.etherscanAPI.contains(COVALENT) ? null : txFetches); //store the transaction data and initiate tx fetch if not already known
        });

        fetchRequiredTransactions(networkInfo.chainId, txFetches, walletAddress);
        return new Pair<>(highestBlockRead, transferEventMap);
    }

    private void storeTransactions(Realm r, Map<String, Transaction> txWriteMap, HashSet<String> txFetches)
    {
        for (Map.Entry<String, Transaction> entry : txWriteMap.entrySet())
        {
            String contractAddress = entry.getKey();
            Transaction tx = entry.getValue();
            RealmTransaction realmTx = r.where(RealmTransaction.class)
                    .equalTo("hash", tx.hash)
                    .findFirst();

            if (realmTx == null)
            {
                realmTx = r.createObject(RealmTransaction.class, tx.hash);
                //fetch the actual transaction here
                if (txFetches != null) txFetches.add(tx.hash);
            }
            else if (realmTx.getContractAddress() == null || !realmTx.getContractAddress().equalsIgnoreCase(contractAddress))
            {
                realmTx.setContractAddress(contractAddress);
            }

            if (realmTx.getInput() == null || realmTx.getInput().length() <= 10 || txFetches == null)
            {
                TransactionsRealmCache.fill(realmTx, tx);
                realmTx.setContractAddress(contractAddress); //for indexing by contract (eg Token Activity)
            }
        }
    }

    private void storeTransferData(Realm instance, long chainId, Map<String, List<TransferEvent>> transferEventMap)
    {
        for (Map.Entry<String, List<TransferEvent>> entry : transferEventMap.entrySet())
        {
            RealmTransfer realmPeek = instance.where(RealmTransfer.class)
                    .equalTo("hash", RealmTransfer.databaseKey(chainId, entry.getKey()))
                    .findFirst();

            if (realmPeek != null) continue;

            //write each event set
            for (TransferEvent thisEvent : entry.getValue())
            {
                RealmTransfer realmTransfer = instance.createObject(RealmTransfer.class);
                realmTransfer.setHashKey(chainId, entry.getKey());
                realmTransfer.setTokenAddress(thisEvent.contractAddress);
                realmTransfer.setEventName(thisEvent.activityName);
                realmTransfer.setTransferDetail(thisEvent.valueList);
            }
        }
    }

    private void blankTransferData(Realm instance, long chainId)
    {
        instance.executeTransaction(r -> {
            RealmResults<RealmTransfer> realmTx = r.where(RealmTransfer.class)
                    .like("hash", "*-" + chainId, Case.INSENSITIVE)
                    .findAll();

            realmTx.deleteAllFromRealm();
        });
    }

    /**
     * Write the transaction to Realm
     *
     * @param instance Realm
     * @param tx Transaction formed initially procedurally from the event, then from Ethereum node if we didn't already have it
     * @param txFetches build list of transactions that need fetching
     */
    private void writeTransaction(Realm instance, Transaction tx, String contractAddress, HashSet<String> txFetches)
    {
        RealmTransaction realmTx = instance.where(RealmTransaction.class)
                .equalTo("hash", tx.hash)
                .findFirst();

        if (realmTx == null)
        {
            realmTx = instance.createObject(RealmTransaction.class, tx.hash);

            //fetch the actual transaction here
            if (txFetches != null) txFetches.add(tx.hash);
        }
        else if (realmTx.getContractAddress() == null || !realmTx.getContractAddress().equalsIgnoreCase(contractAddress))
        {
            realmTx.setContractAddress(contractAddress);
        }

        if (realmTx.getInput() == null || realmTx.getInput().length() <= 10 || txFetches == null)
        {
            TransactionsRealmCache.fill(realmTx, tx);
            realmTx.setContractAddress(contractAddress); //for indexing by contract (eg Token Activity)
        }
    }

    /**
     * This thread will execute in the background filling in transactions.
     * It doesn't have to be cancelled if we switch wallets because these transactions need to be fetched anyway
     * @param chainId networkId
     * @param txFetches map of transactions that need writing. Note we use a map to de-duplicate
     */
    private void fetchRequiredTransactions(long chainId, HashSet<String> txFetches, String walletAddress)
    {
        int txLimitCount = 0;
        for (String txHash : txFetches)
        {
            TransactionsService.addTransactionHashFetch(txHash, chainId, walletAddress);
            if (txLimitCount++ > TRANSACTION_FETCH_LIMIT)
            {
                break;
            }
        }
    }

    /**
     * These functions are experimental, for discovering and populating NFT's without opensea.
     * So far the experiment appears to be working correctly,
     *
     * Tested: rapid discovery and update of tokens without opensea.
     * Once opensea is on, tokens are updated correctly.
     *
     * If tokens already discovered from opensea then we don't replace them here.
     */
    private Map<String, List<EtherscanEvent>> getEventMap(EtherscanEvent[] events)
    {
        Map<String, List<EtherscanEvent>> eventMap = new HashMap<>();
        for (EtherscanEvent ev : events)
        {
            List<EtherscanEvent> thisEventList = eventMap.get(ev.contractAddress);
            if (thisEventList == null)
            {
                thisEventList = new ArrayList<>();
                eventMap.put(ev.contractAddress, thisEventList);
            }

            thisEventList.add(ev);
        }

        return eventMap;
    }

    private BigInteger getTokenId(String tokenID)
    {
        BigInteger tokenIdBI;
        try
        {
            tokenIdBI = new BigInteger(tokenID);
        }
        catch (Exception e)
        {
            tokenIdBI = BigInteger.valueOf(-1);
        }

        return tokenIdBI;
    }

    private ERC721Token createNewERC721Token(EtherscanEvent ev, NetworkInfo networkInfo, String walletAddress, boolean knownERC721)
    {
        TokenInfo info = new TokenInfo(ev.contractAddress, ev.tokenName, ev.tokenSymbol, 0, false, networkInfo.chainId);
        ERC721Token newToken = new ERC721Token(info, null, BigDecimal.ZERO, 0, networkInfo.getShortName(), knownERC721 ? ContractType.ERC721 : ContractType.ERC721_UNDETERMINED);
        newToken.setTokenWallet(walletAddress);
        return newToken;
    }

    private ERC1155Token createNewERC1155Token(EtherscanEvent ev, NetworkInfo networkInfo, String walletAddress)
    {
        TokenInfo info = new TokenInfo(ev.contractAddress, ev.tokenName, ev.tokenSymbol, 0, false, networkInfo.chainId);
        ERC1155Token newToken = new ERC1155Token(info, null, 0, networkInfo.getShortName());
        newToken.setTokenWallet(walletAddress);
        return newToken;
    }

    private void eraseAllTransactions(Realm instance, long chainId)
    {
        instance.executeTransaction(r -> {
            RealmResults<RealmTransaction> realmTx = r.where(RealmTransaction.class)
                    .equalTo("chainId", chainId)
                    .findAll();

            realmTx.deleteAllFromRealm();
        });
    }

    private List<Transaction> sortTransactions(Collection<Transaction> txCollection)
    {
        List<Transaction> txList = new ArrayList<>(txCollection);

        Collections.sort(txList, (t1, t2) -> {
            long block1 = Long.parseLong(t1.blockNumber);
            long block2 = Long.parseLong(t2.blockNumber);

            if (block1 == block2)
            {
                block2++;
            }
            return Long.compare(block1, block2);
        });

        return txList;
    }
}
