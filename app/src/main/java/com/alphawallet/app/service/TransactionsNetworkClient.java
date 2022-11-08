package com.alphawallet.app.service;

import static com.alphawallet.app.repository.EthereumNetworkBase.COVALENT;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static com.alphawallet.app.repository.TransactionsRealmCache.convert;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARTIS_TAU1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AURORA_TESTNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_TEST_ID;

import android.text.TextUtils;

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
import com.alphawallet.app.entity.tokens.ERC721Token;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
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
    public static final int TRANSFER_RESULT_MAX = 250; //check 200 records when we first get a new account
    //Note: if user wants to view transactions older than this, we fetch from etherscan on demand.
    //Generally this would only happen when watching extremely active accounts for curiosity
    private final String BLOCK_ENTRY = "-erc20blockCheck-";
    private final String ERC20_QUERY = "tokentx";
    private final String ERC721_QUERY = "tokennfttx";
    private final int AUX_DATABASE_ID = 26; //increment this to do a one off refresh the AUX database, in case of changed design etc
    private final String DB_RESET = BLOCK_ENTRY + AUX_DATABASE_ID;
    private final String ETHERSCAN_API_KEY;
    private final String BSC_EXPLORER_API_KEY;
    private final String POLYGONSCAN_API_KEY;
    private final String AURORASCAN_API_KEY;
    private final KeyProvider keyProvider = KeyProviderFactory.get();

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
     * Unsynced or out of sync:
     *
     * Read first 2400 transactions (3 pages) from top down.
     *
     * Delete any transactions in database older than 2400th stored tx
     *
     * Synced:
     *
     * Read upwards from sync point
     *
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
            Map<String, Transaction> updates = new HashMap<>();
            Transaction lastTransaction = null;
            try (Realm instance = realmManager.getRealmInstance(svs.getCurrentAddress()))
            {
                if (lastBlockNumber == 1) //first read of account. Read first 2 pages
                {
                    lastTransaction = syncDownwards(updates, instance, svs, networkInfo, tokenAddress, 9999999999L);
                }
                else // try to sync upwards from the last read
                {
                    lastTransaction = syncUpwards(updates, instance, svs, networkInfo, tokenAddress, lastBlockNumber);
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
            finally
            {
                //ensure transaction check time is always written
                String lastBlockRead = (lastTransaction != null) ? lastTransaction.blockNumber : String.valueOf(lastBlock);
                storeLatestBlockRead(svs.getCurrentAddress(), networkInfo.chainId, tokenAddress, lastBlockRead);
            }

            return updates.values().toArray(new Transaction[0]);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * read PAGESIZE*2 transactions down from startingBlockNumber
     *
     * Note that this call is the only place that the 'earliest transaction' block can be written from.
     */
    private Transaction syncDownwards(Map<String, Transaction> updates, Realm instance, TokensService svs, NetworkInfo networkInfo, String tokenAddress, long startingBlockNumber) throws Exception
    {
        int page = 1;
        List<Transaction> txList = new ArrayList<>();
        boolean continueReading = true;

        while (continueReading) // only SYNC_PAGECOUNT pages at a time for each check, to avoid congestion
        {
            EtherscanTransaction[] myTxs = readTransactions(networkInfo, svs, tokenAddress, String.valueOf(startingBlockNumber), false, page++, PAGESIZE);
            if (myTxs.length == 0) break;
            getRelatedTransactionList(txList, myTxs, svs.getCurrentAddress(), networkInfo.chainId);

            writeTransactions(instance, txList); //record transactions here
            writeUpdates(updates, txList);

            if (page > SYNC_PAGECOUNT) continueReading = false;

            if (myTxs.length < PAGESIZE)
            {
                continueReading = false;
                //store earliest transaction
                Transaction lastTransaction = findSortedTransaction(instance, networkInfo.chainId, Sort.ASCENDING);
                if (lastTransaction != null) storeEarliestBlockRead(instance, networkInfo.chainId, tokenAddress, Long.parseLong(lastTransaction.blockNumber));
            }
        }

        Transaction firstTransaction = findSortedTransaction(instance, networkInfo.chainId, Sort.DESCENDING);

        return firstTransaction;
    }

    private Transaction findSortedTransaction(Realm r, long chainId, Sort sort)
    {
        RealmTransaction item = r.where(RealmTransaction.class)
                .equalTo("chainId", chainId)
                .sort("timeStamp", sort)
                .findFirst();

        if (item != null)
        {
            return convert(item);
        }
        else
        {
            return null;
        }
    }

    private void writeUpdates(Map<String, Transaction> updates, List<Transaction> txList)
    {
        if (updates != null)
        {
            for (Transaction tx : txList)
            {
                updates.put(tx.hash, tx);
            }
        }
    }

    private Transaction syncUpwards(Map<String, Transaction> updates, Realm instance, TokensService svs, NetworkInfo networkInfo, String tokenAddress, long lastBlockNumber) throws Exception
    {
        int page = 1;
        List<Transaction> txList = new ArrayList<>();
        Transaction lastTransaction;

        //only sync upwards by 1 page. If not sufficient then reset; delete DB and start again
        EtherscanTransaction[] myTxs = readTransactions(networkInfo, svs, tokenAddress, String.valueOf(lastBlockNumber), true, page, PAGESIZE);
        if (myTxs.length == 0) { return null; }
        else if (myTxs.length == PAGESIZE)
        {
            //too big, erase transaction list and start from top
            deleteAllChainTransactions(instance, networkInfo.chainId, svs.getCurrentAddress());
            lastTransaction = syncDownwards(updates, instance, svs, networkInfo, tokenAddress, 999999999L); //re-sync downwards from top
            return lastTransaction;
        }
        else
        {
            getRelatedTransactionList(txList, myTxs, svs.getCurrentAddress(), networkInfo.chainId);
            writeTransactions(instance, txList); //record transactions here
            writeUpdates(updates, txList);
            lastTransaction = findSortedTransaction(instance, networkInfo.chainId, Sort.DESCENDING); //myTxs[myTxs.length-1]; //latest transaction
            return lastTransaction;
        }
    }

    private void getRelatedTransactionList(List<Transaction> txList, EtherscanTransaction[] myTxs, String walletAddress, long chainId)
    {
        txList.clear();
        for (EtherscanTransaction etx : myTxs)
        {
            Transaction tx = etx.createTransaction(walletAddress, chainId);
            if (tx != null)
            {
                txList.add(tx);
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

    private EtherscanTransaction[] readTransactions(NetworkInfo networkInfo, TokensService svs, String tokenAddress, String firstBlock, boolean ascending, int page, int pageSize) throws JSONException
    {
        if (networkInfo == null) return new EtherscanTransaction[0];
        if (networkInfo.etherscanAPI.contains(COVALENT)) { return readCovalentTransactions(svs, tokenAddress, networkInfo, ascending, page, pageSize); }

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
                sb.append(firstBlock);
                sb.append("&endblock=999999999&sort=");
            }
            else
            {
                sb.append("&startblock=0");
                sb.append("&endblock=");
                sb.append(firstBlock);
                sb.append("&sort=");
            }

            sb.append(sort);
            if (page > 0)
            {
                sb.append("&page=");
                sb.append(page);
                sb.append("&offset=");
                sb.append(pageSize);
            }

            sb.append(getNetworkAPIToken(networkInfo));

            fullUrl = sb.toString();

            if (!Utils.isValidUrl(fullUrl))
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

                result = response.body().string();
                if (result.length() < 80 && result.contains("No transactions found"))
                {
                    return new EtherscanTransaction[0];
                }
                else
                {
                    return getEtherscanTransactions(result);
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
                        syncDownwards(null, instance, svs, network, svs.getCurrentAddress(), oldestBlockRead);
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
     * @param isNFTCheck hint for whether to check NFT transfers
     * @return
     */
    @Override
    public Single<Integer> readTransfers(String walletAddress, NetworkInfo networkInfo, TokensService svs, boolean isNFTCheck)
    {
        final boolean nftCheck = isNFTCheck && networkInfo.usesSeparateNFTTransferQuery();
        if (nftCheck && svs.openSeaUpdateInProgress(networkInfo.chainId)) { return Single.fromCallable(() -> 0); } //don't allow simultaneous NFT checking
        else return Single.fromCallable(() -> {
            //get latest block read
            int eventCount = 0;
            try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
            {
                //get last tokencheck
                long lastBlockChecked = getTokenBlockRead(instance, networkInfo.chainId, nftCheck);
                //fetch transfers from end point
                String fetchTransactions = readNextTxBatch(walletAddress, networkInfo, lastBlockChecked, nftCheck ? ERC721_QUERY : ERC20_QUERY);

                if (fetchTransactions != null && fetchTransactions.length() > 100)
                {
                    //convert to gson
                    EtherscanEvent[] events = getEtherscanEvents(fetchTransactions);

                    eventCount = processEtherscanEvents(instance, walletAddress, networkInfo,
                            svs, events, isNFTCheck);
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
            return eventCount;
        }).observeOn(Schedulers.io());
    }

    private int processEtherscanEvents(Realm instance, String walletAddress, NetworkInfo networkInfo,
                                        TokensService svs, EtherscanEvent[] events, boolean isNFTCheck) throws Exception
    {
        //Now update tokens if we don't already know this token
        writeTokens(walletAddress, networkInfo, events, svs);

        //we know all these events are relevant to the wallet, and they are all ERC20 events
        writeEvents(instance, events, walletAddress, networkInfo, isNFTCheck);

        //and update the top block read
        long lastBlockChecked = Long.parseLong(events[events.length - 1].blockNumber);
        writeTokenBlockRead(instance, networkInfo.chainId, lastBlockChecked + 1, isNFTCheck);

        return events.length;
    }

    private void writeTokens(String walletAddress, NetworkInfo networkInfo, EtherscanEvent[] events, TokensService svs)
    {
        Map<String, List<EtherscanEvent>> eventMap = getEventMap(events);

        for (String contract : eventMap.keySet())
        {
            EtherscanEvent ev0 = eventMap.get(contract).get(0);
            Token token = svs.getToken(networkInfo.chainId, contract);
            boolean newToken = false;

            int tokenDecimal = calcTokenDecimals(ev0);

            if (tokenDecimal == -1 && (token == null ||
                    ( token.getInterfaceSpec() != ContractType.ERC721 &&
                            token.getInterfaceSpec() != ContractType.ERC721_LEGACY &&
                            token.getInterfaceSpec() != ContractType.ERC721_TICKET &&
                            token.getInterfaceSpec() != ContractType.ERC721_UNDETERMINED )))
            {
                token = createNewERC721Token(eventMap.get(contract).get(0), networkInfo, walletAddress, false);
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
                writeAssets(eventMap, token, walletAddress, contract, svs, newToken);
            }
            else if (newToken) // new Fungible token
            {
                svs.storeToken(token);
            }
            else
            {
                //instruct tokensService to update balance
                svs.addBalanceCheck(token);
            }

            //Send to storage as soon as each token is done
            token.lastTxTime = System.currentTimeMillis();
        }
    }

    private int calcTokenDecimals(EtherscanEvent ev0)
    {
        int tokenDecimal = (!TextUtils.isEmpty(ev0.tokenDecimal) && Character.isDigit(ev0.tokenDecimal.charAt(0))) ? Integer.parseInt(ev0.tokenDecimal) : -1;

        if (tokenDecimal < 1 && ev0.tokenID != null && ev0.value == null && (ev0.tokenDecimal == null || ev0.tokenDecimal.equals("0")))
        {
            tokenDecimal = -1;
        }

        return tokenDecimal;
    }

    private void writeAssets   (Map<String, List<EtherscanEvent>> eventMap, Token token, String walletAddress,
                                String contractAddress, TokensService svs, boolean newToken)
    {
        List<BigInteger> additions = new ArrayList<>();
        List<BigInteger> removals = new ArrayList<>();

        for (EtherscanEvent ev : eventMap.get(contractAddress))
        {
            BigInteger tokenId = getTokenId(ev.tokenID);

            if (tokenId.compareTo(new BigInteger("-1")) == 0) continue;

            if (ev.to.equalsIgnoreCase(walletAddress))
            {
                if (!additions.contains(tokenId)) { additions.add(tokenId); }
                removals.remove(tokenId);
            }
            else
            {
                if (!removals.contains(tokenId)) { removals.add(tokenId); }
                additions.remove(tokenId);
            }
        }

        if (additions.size() > 0 && newToken)
        {
            token.setInterfaceSpec(ContractType.ERC721);
        }

        if (additions.size() > 0 || removals.size() > 0)
        {
            svs.updateAssets(token, additions, removals);
        }
    }

    private String readNextTxBatch(String walletAddress, NetworkInfo networkInfo, long currentBlock, String queryType)
    {
        if (TextUtils.isEmpty(networkInfo.etherscanAPI) || networkInfo.etherscanAPI.contains(COVALENT)) return ""; //Covalent transfers are handled elsewhere
        String result = "0";
        if (currentBlock == 0) currentBlock = 1;

        String fullUrl = networkInfo.etherscanAPI + "module=account&action=" + queryType +
                "&startblock=" + currentBlock + "&endblock=9999999999" +
                "&address=" + walletAddress +
                "&page=1&offset=" + TRANSFER_RESULT_MAX +
                "&sort=asc" + getNetworkAPIToken(networkInfo);

        if (!Utils.isValidUrl(fullUrl))
        {
            return "0";
        }

        Request request = new Request.Builder()
                .url(fullUrl)
                .header("User-Agent", "Chrome/74.0.3729.169")
                .method("GET", null)
                .addHeader("Content-Type", "application/json")
                .build();

        try (okhttp3.Response response = httpClient.newCall(request).execute())
        {
            result = response.body().string();
            if (result.length() < 80 && result.contains("No transactions found"))
            {
                result = "0";
            }
        }
        catch (Exception e)
        {
            if (networkInfo.chainId != ARTIS_TAU1_ID && BuildConfig.DEBUG) Timber.e(e);
        }

        return result;
    }

    private String getNetworkAPIToken(NetworkInfo networkInfo)
    {
        if (networkInfo.etherscanAPI.contains("etherscan"))
        {
            return ETHERSCAN_API_KEY;
        }
        else if (networkInfo.chainId == BINANCE_TEST_ID || networkInfo.chainId == BINANCE_MAIN_ID)
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
                    svs, events, false);
        }
        catch (Exception e)
        {
            //
        }

        EtherscanTransaction[] rawTransactions = CovalentTransaction.toRawEtherscanTransactions(covalentTransactions, networkInfo);

        //List of transaction hashes that still need handling
        return rawTransactions;
    }

    private long getTokenBlockRead(Realm instance, long chainId, boolean isNFT)
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
            return isNFT ? rd.getResultReceivedTime() : rd.getResultTime();
        }
    }

    private void writeTokenBlockRead(Realm instance, long chainId, long lastBlockChecked, boolean isNFT)
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

            if (isNFT)
            {
                rd.setResultReceivedTime(lastBlockChecked);
            }
            else
            {
                rd.setResultTime(lastBlockChecked);
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

    private void writeEvents   (Realm instance, EtherscanEvent[] events, String walletAddress,
                                @NonNull NetworkInfo networkInfo, final boolean isNFT) throws Exception
    {
        String TO_TOKEN = "[TO_ADDRESS]";
        String FROM_TOKEN = "[FROM_ADDRESS]";
        String AMOUNT_TOKEN = "[AMOUNT_TOKEN]";
        String VALUES = "from,address," + FROM_TOKEN + ",to,address," + TO_TOKEN + ",amount,uint256," + AMOUNT_TOKEN;

        HashSet<String> txFetches = new HashSet<>();

        instance.executeTransaction(r -> {
            //write event list
            for (EtherscanEvent ev : events)
            {
                boolean scanAsNFT = isNFT || ((ev.tokenDecimal == null || ev.tokenDecimal.length() == 0 || ev.tokenDecimal.equals("0")) &&
                        (ev.tokenID != null && ev.tokenID.length() > 0));
                Transaction tx = scanAsNFT ? ev.createNFTTransaction(networkInfo) : ev.createTransaction(networkInfo);

                //find tx name
                String activityName = tx.getEventName(walletAddress);
                //Etherscan sometimes interprets NFT transfers as FT's
                String valueList = VALUES.replace(TO_TOKEN, ev.to).replace(FROM_TOKEN, ev.from).replace(AMOUNT_TOKEN, scanAsNFT ? ev.tokenID : ev.value);
                storeTransferData(r, tx.hash, valueList, activityName, ev.contractAddress);
                //ensure we have fetched the transaction for each hash
                writeTransaction(r, tx, ev.contractAddress, networkInfo.etherscanAPI.contains(COVALENT) ? null : txFetches);
            }
        });

        fetchRequiredTransactions(networkInfo.chainId, txFetches, walletAddress);
    }

    private void storeTransferData(Realm instance, String hash, String valueList, String activityName, String tokenAddress)
    {
        RealmTransfer matchingEntry = instance.where(RealmTransfer.class)
                .equalTo("hash", hash)
                .equalTo("tokenAddress", tokenAddress)
                .equalTo("eventName", activityName)
                .equalTo("transferDetail", valueList)
                .findFirst();

        if (matchingEntry == null) //prevent duplicates
        {
            RealmTransfer realmTransfer = instance.createObject(RealmTransfer.class);
            realmTransfer.setHash(hash);
            realmTransfer.setTokenAddress(tokenAddress);
            realmTransfer.setEventName(activityName);
            realmTransfer.setTransferDetail(valueList);
        }
        else
        {
            Timber.d("Prevented collision: %s", tokenAddress);
        }
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
        for (String txHash : txFetches)
        {
            TransactionsService.addTransactionHashFetch(txHash, chainId, walletAddress);
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
}
