package com.alphawallet.app.service;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.EtherscanEvent;
import com.alphawallet.app.entity.EtherscanTransaction;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.ERC721Token;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokenscript.EventUtils;
import com.alphawallet.app.repository.TransactionsRealmCache;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.repository.entity.RealmTransfer;
import com.alphawallet.token.entity.ContractAddress;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthTransaction;

import java.io.InterruptedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static com.alphawallet.app.entity.TransactionDecoder.FUNCTION_LENGTH;
import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;

public class TransactionsNetworkClient implements TransactionsNetworkClientType
{
    private final int PAGESIZE = 800;
    private final int SYNC_PAGECOUNT = 2; //how many pages to read when we first sync the account - means we store the first 1600 transactions only
    //Note: if user wants to view transactions older than this, we fetch from etherscan on demand.
    //Generally this would only happen when watching extremely active accounts for curiosity
    private final String BLOCK_ENTRY = "-erc20blockCheck-";
    private final String ERC20_QUERY = "tokentx";
    private final String ERC721_QUERY = "tokennfttx";
    private final int AUX_DATABASE_ID = 5; //increment this to do a one off refresh the AUX database, in case of changed design etc
    private final String DB_RESET = BLOCK_ENTRY + AUX_DATABASE_ID;

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
    }

    /**
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
     * @param walletAddress
     * @param networkInfo
     * @param lastBlock
     * @return
     */
    @Override
    public Single<Transaction[]> storeNewTransactions(String walletAddress, NetworkInfo networkInfo, String tokenAddress, final long lastBlock)
    {
        return Single.fromCallable(() -> {
            long lastBlockNumber = lastBlock + 1;
            Map<String, Transaction> updates = new HashMap<>();
            EtherscanTransaction lastTransaction = null;
            try (Realm instance = realmManager.getRealmInstance(walletAddress))
            {
                if (lastBlockNumber == 1) //first read of account. Read first 2 pages
                {
                    lastTransaction = syncDownwards(updates, instance, walletAddress, networkInfo, tokenAddress, 9999999999L);
                }
                else // try to sydenc upwards from the last read
                {
                    lastTransaction = syncUpwards(updates, instance, walletAddress, networkInfo, tokenAddress, lastBlockNumber);
                }
            }
            catch (JSONException e)
            {
                //silent fail
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                //ensure transaction check time is always written
                String lastBlockRead = (lastTransaction != null) ? lastTransaction.blockNumber : String.valueOf(lastBlock);
                storeLatestBlockRead(walletAddress, networkInfo.chainId, tokenAddress, lastBlockRead);
            }

            return updates.values().toArray(new Transaction[0]);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * read PAGESIZE*2 transactions down from startingBlockNumber
     *
     * Note that this call is the only place that the 'earliest transaction' block can be written from.
     */
    private EtherscanTransaction syncDownwards(Map<String, Transaction> updates, Realm instance, String walletAddress, NetworkInfo networkInfo, String tokenAddress, long startingBlockNumber) throws Exception
    {
        int page = 1;
        List<Transaction> txList = new ArrayList<>();
        EtherscanTransaction firstTransaction = null;
        EtherscanTransaction lastTransaction = null;
        boolean continueReading = true;

        while (continueReading) // only SYNC_PAGECOUNT pages at a time for each check, to avoid congestion
        {
            String response = readTransactions(networkInfo, tokenAddress, String.valueOf(startingBlockNumber), false, page++, PAGESIZE);
            if (response == null) break;
            if (response.equals("0"))
            {
                storeEarliestBlockRead(instance, networkInfo.chainId, tokenAddress, startingBlockNumber);
                break;
            }
            EtherscanTransaction[] myTxs = getEtherscanTransactions(response);
            getRelatedTransactionList(txList, myTxs, walletAddress, networkInfo.chainId);
            if (myTxs.length > 0 && firstTransaction == null)
            {
                firstTransaction = myTxs[0];
            }
            if (myTxs.length > 0)
            {
                lastTransaction = myTxs[myTxs.length - 1];
            }

            writeTransactions(instance, txList); //record transactions here
            writeUpdates(updates, txList);

            if (page > SYNC_PAGECOUNT) continueReading = false;

            if (myTxs.length < PAGESIZE)
            {
                continueReading = false;
                //store earliest transaction
                if (lastTransaction != null) storeEarliestBlockRead(instance, networkInfo.chainId, tokenAddress, Long.parseLong(lastTransaction.blockNumber));
            }
        }

        return firstTransaction;
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

    private EtherscanTransaction syncUpwards(Map<String, Transaction> updates, Realm instance, String walletAddress, NetworkInfo networkInfo, String tokenAddress, long lastBlockNumber) throws Exception
    {
        int page = 1;
        List<Transaction> txList = new ArrayList<>();
        EtherscanTransaction lastTransaction;

        //only sync upwards by 1 page. If not sufficient then reset; delete DB and start again
        String response = readTransactions(networkInfo, tokenAddress, String.valueOf(lastBlockNumber), true, page, PAGESIZE);
        if (response == null || response.equals("0")) return null;
        EtherscanTransaction[] myTxs = getEtherscanTransactions(response);

        if (myTxs.length == PAGESIZE)
        {
            //too big, erase transaction list and start from top
            deleteAllChainTransactions(instance, networkInfo.chainId, walletAddress);
            lastTransaction = syncDownwards(updates, instance, walletAddress, networkInfo, tokenAddress, 999999999L); //re-sync downwards from top
            lastTransaction.nonce = -1; //signal to refresh list - this doesn't get written to DB
            return lastTransaction;
        }
        else
        {
            getRelatedTransactionList(txList, myTxs, walletAddress, networkInfo.chainId);
            writeTransactions(instance, txList); //record transactions here
            writeUpdates(updates, txList);
            lastTransaction = myTxs[myTxs.length-1];
            return lastTransaction;
        }
    }

    private void getRelatedTransactionList(List<Transaction> txList, EtherscanTransaction[] myTxs, String walletAddress, int chainId)
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
        JSONObject stateData = new JSONObject(response);
        JSONArray orders = stateData.getJSONArray("result");
        return gson.fromJson(orders.toString(), EtherscanTransaction[].class);
    }

    private EtherscanEvent[] getEtherscanEvents(String response) throws JSONException
    {
        JSONObject stateData = new JSONObject(response);
        JSONArray orders = stateData.getJSONArray("result");
        return gson.fromJson(orders.toString(), EtherscanEvent[].class);
    }

    private boolean writeTransactions(Realm instance, List<Transaction> txList) throws Exception
    {
        if (txList.size() == 0) return false;
        boolean startedReWriting = false;

        instance.beginTransaction();
        for (Transaction tx : txList)
        {
            RealmTransaction realmTx = instance.where(RealmTransaction.class)
                    .equalTo("hash", tx.hash)
                    .findFirst();

            if (realmTx == null)
            {
                realmTx = instance.createObject(RealmTransaction.class, tx.hash);
            }
            else
            {
                startedReWriting = true;
            }

            TransactionsRealmCache.fill(instance, realmTx, tx);
            instance.insertOrUpdate(realmTx);
        }
        instance.commitTransaction();

        return startedReWriting;
    }

    private String readTransactions(NetworkInfo networkInfo, String address, String firstBlock, boolean ascending, int page, int pageSize)
    {
        okhttp3.Response response = null;
        String result = null;
        String fullUrl = null;

        String sort = "asc";
        if (!ascending) sort = "desc";

        if (networkInfo != null && !TextUtils.isEmpty(networkInfo.etherscanTxUrl))
        {
            StringBuilder sb = new StringBuilder();
            sb.append(networkInfo.etherscanTxUrl);
            if (!networkInfo.etherscanTxUrl.endsWith("/"))
            {
                sb.append("/");
            }

            sb.append("api?module=account&action=txlist&address=");
            sb.append(address);
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

            if (networkInfo.etherscanTxUrl.contains("etherscan"))
            {
                sb.append("&apikey=6U31FTHW3YYHKW6CYHKKGDPHI9HEJ9PU5F");
            }

            fullUrl = sb.toString();

            try
            {
                Request request = new Request.Builder()
                        .url(fullUrl)
                        .get()
                        .build();

                response = httpClient.newCall(request).execute();

                result = response.body().string();
                if (result != null && result.length() < 80 && result.contains("No transactions found"))
                {
                    result = "0";
                }
            }
            catch (InterruptedIOException e)
            {
                //If user switches account or network during a fetch
                //this exception is going to be thrown because we're terminating the API call
                //Don't display error
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * This is the function called when a user scrolls to the bottom of a transaction list.
     * First try to provide more transactions from the stored database. If there aren't any more then populate another page (800) from etherscan
     *
     * @param walletAddress
     * @param network
     * @param lastTxTime
     * @return
     */
    @Override
    public Single<TransactionMeta[]> fetchMoreTransactions(String walletAddress, NetworkInfo network, long lastTxTime)
    {
        return Single.fromCallable(() -> {
            List<TransactionMeta> txList = fetchOlderThan(walletAddress, lastTxTime, network.chainId);
            if (txList.size() < 800)
            {
                //fetch another page and return unless we already have the oldest Tx
                long oldestTxTime = txList.size() > 0 ? txList.get(txList.size() - 1).getTimeStampSeconds() : lastTxTime;
                try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
                {
                    long oldestBlockRead = getOldestBlockRead(instance, network.chainId, oldestTxTime);
                    long oldestPossibleBlock = getFirstTransactionBlock(instance, network.chainId, walletAddress);
                    System.out.println("DIAGNOSE: " + oldestBlockRead + " : " + oldestPossibleBlock);
                    if (oldestBlockRead > 0 && oldestBlockRead != oldestPossibleBlock)
                    {
                        syncDownwards(null, instance, walletAddress, network, walletAddress, oldestBlockRead);
                    }

                    //now re-read last blocks from DB
                    txList = fetchOlderThan(walletAddress, lastTxTime, network.chainId);

                }
                catch (Exception e)
                {
                    //
                }
            }

            return txList.toArray(new TransactionMeta[0]);
        });
    }

    @Override
    public Single<Integer> readTransactions(String walletAddress, NetworkInfo networkInfo, TokensService svs, boolean nftCheck)
    {
        if (nftCheck)
        {
            return readNFTTransactions(walletAddress, networkInfo, svs);
        }
        else
        {
            return readERC20Transactions(walletAddress, networkInfo, svs);
        }
    }

    //See if we require a refresh of transaction checks
    @Override
    public Completable checkTransactionsForEmptyFunctions(String currentAddress)
    {
        return Completable.fromAction(() -> {
            try (Realm instance = realmManager.getRealmInstance(new Wallet(currentAddress)))
            {
                RealmResults<RealmAuxData> checkMarkers = instance.where(RealmAuxData.class)
                        .like("instanceKey", BLOCK_ENTRY + "*")
                        .findAll();

                boolean delete = false;

                for (RealmAuxData aux : checkMarkers)
                {
                    if (TextUtils.isEmpty(aux.getResult()) || !aux.getResult().equals(DB_RESET))
                    {
                        String chainIdStr = aux.getInstanceKey().substring(BLOCK_ENTRY.length());
                        int chainId = Integer.parseInt(chainIdStr);
                        writeNFTokenBlockRead(instance, chainId, 0); //check from start
                        writeTokenBlockRead(instance, chainId, 0); //check from start

                        instance.executeTransaction(r -> {
                            aux.setResult(DB_RESET);
                        });

                        delete = true;
                    }
                }

                if (delete)
                {
                    instance.beginTransaction();
                    RealmResults<RealmAuxData> realmEvents = instance.where(RealmAuxData.class)
                            .findAll();
                    realmEvents.deleteAllFromRealm();
                    RealmResults<RealmTransfer> realmTransfers = instance.where(RealmTransfer.class)
                                .findAll();
                    realmTransfers.deleteAllFromRealm();
                    instance.commitTransaction();
                }
            }
            catch (Exception e)
            {
                //
            }
        });
    }

    /**
     * Fetch the ERC20 transactions relevant to this wallet - ie deposits to and transfers from
     * @param walletAddress
     * @param networkInfo
     * @param svs
     * @return
     */
    public Single<Integer> readERC20Transactions(String walletAddress, NetworkInfo networkInfo, TokensService svs)
    {
        return Single.fromCallable(() -> {
            //get latest block read
            int eventCount = 0;
            try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
            {
                //get last tokencheck
                long lastBlockChecked = getTokenBlockRead(instance, networkInfo.chainId);
                //fetch erc20 tx from Etherscan
                String fetchTransactions = readNextTxBatch(walletAddress, networkInfo, lastBlockChecked, ERC20_QUERY);

                if (fetchTransactions != null && fetchTransactions.length() > 100)
                {
                    //convert to gson
                    EtherscanEvent[] events = getEtherscanEvents(fetchTransactions);
                    //we know all these events are relevant to the wallet, and they are all ERC20 events
                    writeEvents(instance, events, walletAddress, networkInfo, false);

                    //Now update tokens if we don't already know this token
                    writeERC20Tokens(instance, walletAddress, networkInfo, events, svs);

                    lastBlockChecked = Long.parseLong(events[events.length - 1].blockNumber);

                    //and update the top block read
                    writeTokenBlockRead(instance, networkInfo.chainId, lastBlockChecked);
                    eventCount = events.length;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return eventCount;
        }).observeOn(Schedulers.io());
    }

    public Single<Integer> readNFTTransactions(String walletAddress, @NonNull NetworkInfo networkInfo, TokensService svs)
    {
        return Single.fromCallable(() -> {
            //get latest block read
            int eventCount = 0;
            try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
            {
                //get last tokencheck
                long lastBlockChecked = getNFTokenBlockRead(instance, networkInfo.chainId);
                String fetchTransactions = readNextTxBatch(walletAddress, networkInfo, lastBlockChecked, ERC721_QUERY);

                if (fetchTransactions != null && fetchTransactions.length() > 100)
                {
                    //convert to gson
                    EtherscanEvent[] events = getEtherscanEvents(fetchTransactions);
                    writeEvents(instance, events, walletAddress, networkInfo, true);

                    //Now update tokens if we don't already know this token
                    writeERC721Tokens(instance, walletAddress, networkInfo, events, svs);

                    lastBlockChecked = Long.parseLong(events[events.length - 1].blockNumber);

                    //and update the top block read
                    writeNFTokenBlockRead(instance, networkInfo.chainId, lastBlockChecked);
                    eventCount = events.length;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return eventCount;
        }).observeOn(Schedulers.io());
    }

    private void writeERC20Tokens(Realm instance, String walletAddress, NetworkInfo networkInfo, EtherscanEvent[] events, TokensService svs)
    {
        for (EtherscanEvent ev : events)
        {
            //have this token?
            RealmToken realmItem = instance.where(RealmToken.class)
                    .equalTo("address", databaseKey(networkInfo.chainId, ev.contractAddress.toLowerCase()))
                    .equalTo("chainId", networkInfo.chainId)
                    .findFirst();

            int tokenDecimal = (!TextUtils.isEmpty(ev.tokenDecimal) && Character.isDigit(ev.tokenDecimal.charAt(0))) ? Integer.parseInt(ev.tokenDecimal) : -1;

            if (realmItem == null || realmItem.getInterfaceSpec() != ContractType.ERC20.ordinal()
                    || (tokenDecimal > 0 && tokenDecimal != realmItem.getDecimals())
                    || !ev.tokenName.equals(realmItem.getName()) //trust etherscan's name
            )
            {
                // write token to DB - note this also fetches the balance
                if (tokenDecimal >= 0)
                {
                    TokenInfo info = new TokenInfo(ev.contractAddress, ev.tokenName, ev.tokenSymbol, tokenDecimal, true, networkInfo.chainId);
                    Token newToken = new Token(info, BigDecimal.ZERO, 0, networkInfo.getShortName(), ContractType.ERC20);
                    newToken.setTokenWallet(walletAddress);
                    svs.storeToken(newToken);
                }
                else
                {
                    //unknown token
                    svs.addUnknownTokenToCheck(new ContractAddress(networkInfo.chainId, ev.contractAddress));
                }
            }
            else
            {
                //update token block read so we don't check events on this contract
                storeLatestBlockRead(walletAddress, networkInfo.chainId, ev.contractAddress, ev.blockNumber);
            }
        }
    }

    private void writeERC721Tokens(Realm instance, String walletAddress, NetworkInfo networkInfo, EtherscanEvent[] events, TokensService svs)
    {
        for (EtherscanEvent ev : events)
        {
            //have this token?
            RealmToken realmItem = instance.where(RealmToken.class)
                    .equalTo("address", databaseKey(networkInfo.chainId, ev.contractAddress.toLowerCase()))
                    .equalTo("chainId", networkInfo.chainId)
                    .findFirst();

            if (realmItem == null ||
                    ( realmItem.getInterfaceSpec() != ContractType.ERC721.ordinal() &&
                            realmItem.getInterfaceSpec() != ContractType.ERC721_LEGACY.ordinal() &&
                            realmItem.getInterfaceSpec() != ContractType.ERC721_TICKET.ordinal() &&
                            realmItem.getInterfaceSpec() != ContractType.ERC721_UNDETERMINED.ordinal()))
            {
                // write token to DB - note this also fetches the balance
                TokenInfo info = new TokenInfo(ev.contractAddress, ev.tokenName, ev.tokenSymbol, 0, true, networkInfo.chainId);
                ERC721Token newToken = new ERC721Token(info, null, 0, networkInfo.getShortName(), ContractType.ERC721);
                newToken.setTokenWallet(walletAddress);
                svs.storeToken(newToken);
            }
            else
            {
                //update token block read so we don't check events on this contract
                storeLatestBlockRead(walletAddress, networkInfo.chainId, ev.contractAddress, ev.blockNumber);
            }
        }
    }

    private String readNextTxBatch(String walletAddress, NetworkInfo networkInfo, long lastBlockChecked, String queryType)
    {
        okhttp3.Response response;
        String result = null;
        final String START_BLOCK = "[START_BLOCK]";
        final String WALLET_ADDR = "[WALLET_ADDR]";
        final String ETHERSCAN = "[ETHERSCAN]";
        final String QUERY_TYPE = "[QUERY_TYPE]";
        String fullUrl = ETHERSCAN + "api?module=account&action=" + QUERY_TYPE + "&startBlock=" + START_BLOCK + "&address=" + WALLET_ADDR + "&page=1&offset=100&sort=asc&apikey=6U31FTHW3YYHKW6CYHKKGDPHI9HEJ9PU5F";
        fullUrl = fullUrl.replace(QUERY_TYPE, queryType).replace(ETHERSCAN, networkInfo.etherscanTxUrl).replace(START_BLOCK, String.valueOf(lastBlockChecked + 1)).replace(WALLET_ADDR, walletAddress);
        //sb.append("&apikey=6U31FTHW3YYHKW6CYHKKGDPHI9HEJ9PU5F");

        try
        {
            Request request = new Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build();

            response = httpClient.newCall(request).execute();

            result = response.body().string();
            if (result != null && result.length() < 80 && result.contains("No transactions found"))
            {
                result = "0";
            }
        }
        catch (InterruptedIOException e)
        {
            //If user switches account or network during a fetch
            //this exception is going to be thrown because we're terminating the API call
            //Don't display error
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return result;
    }

    private long getTokenBlockRead(Realm instance, int chainId)
    {
        RealmAuxData rd = instance.where(RealmAuxData.class)
                .equalTo("instanceKey", BLOCK_ENTRY + chainId)
                .findFirst();

        if (rd == null)
        {
            return 1L;
        }
        else
        {
            return rd.getResultTime();
        }
    }

    private long getNFTokenBlockRead(Realm instance, int chainId)
    {
        RealmAuxData rd = instance.where(RealmAuxData.class)
                .equalTo("instanceKey", BLOCK_ENTRY + chainId)
                .findFirst();

        if (rd == null)
        {
            return 1L;
        }
        else
        {
            return rd.getResultReceivedTime();
        }
    }

    private void writeTokenBlockRead(Realm instance, int chainId, long lastBlockChecked)
    {
        instance.executeTransaction(r -> {
            RealmAuxData rd = instance.where(RealmAuxData.class)
                    .equalTo("instanceKey", BLOCK_ENTRY + chainId)
                    .findFirst();
            if (rd == null)
            {
                rd = instance.createObject(RealmAuxData.class, BLOCK_ENTRY + chainId);
                rd.setResult(DB_RESET);
            }
            rd.setResultTime(lastBlockChecked);
        });
    }

    private void writeNFTokenBlockRead(Realm instance, int chainId, long lastBlockChecked)
    {
        instance.executeTransaction(r -> {
            RealmAuxData rd = instance.where(RealmAuxData.class)
                    .equalTo("instanceKey", BLOCK_ENTRY + chainId)
                    .findFirst();
            if (rd == null)
            {
                rd = instance.createObject(RealmAuxData.class, BLOCK_ENTRY + chainId);
                rd.setResult(DB_RESET);
            }
            rd.setResultReceivedTime(lastBlockChecked);
        });
    }

    private long getOldestBlockRead(Realm instance, int chainId, long lastTxTime)
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

    private long getFirstTransactionBlock(Realm instance, int chainId, String walletAddress)
    {
        long txBlockRead = 0;
        try
        {
            RealmToken realmToken = instance.where(RealmToken.class)
                    .equalTo("address", databaseKey(chainId, walletAddress))
                    .equalTo("chainId", chainId)
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

    private List<TransactionMeta> fetchOlderThan(String walletAddress, long fetchTime, int chainId)
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

    private String databaseKey(int chainId, String walletAddress)
    {
        return walletAddress.toLowerCase() + "-" + chainId;
    }

    private void storeLatestBlockRead(String walletAddress, int chainId, String tokenAddress, String lastBlockRead)
    {
        try (Realm instance = realmManager.getRealmInstance(walletAddress))
        {
            RealmToken realmToken = instance.where(RealmToken.class)
                    .equalTo("address", databaseKey(chainId, tokenAddress))
                    .equalTo("chainId", chainId)
                    .findFirst();

            if (realmToken != null)
            {
                instance.executeTransaction(realm -> {
                    realmToken.setLastBlock(Long.parseLong(lastBlockRead));
                    realmToken.setLastTxTime(System.currentTimeMillis());
                });
            }
        }
        catch (Exception e)
        {
            //
        }
    }

    private void storeEarliestBlockRead(Realm instance, int chainId, String walletAddress, long earliestBlock)
    {
        try
        {
            RealmToken realmToken = instance.where(RealmToken.class)
                    .equalTo("address", databaseKey(chainId, walletAddress))
                    .equalTo("chainId", chainId)
                    .findFirst();

            if (realmToken != null)
            {
                instance.executeTransaction(realm -> {
                    realmToken.setEarliestTransactionBlock(earliestBlock);
                });
            }
        }
        catch (Exception e)
        {
            //
        }
    }

    public void deleteAllChainTransactions(Realm instance, int chainId, String walletAddress)
    {
        try
        {
            RealmResults<RealmTransaction> txs = instance.where(RealmTransaction.class)
                    .equalTo("chainId", chainId)
                    .findAll();

            if (txs != null && txs.size() > 0)
            {
                instance.executeTransaction(realm -> {
                    txs.deleteAllFromRealm();
                });
            }

            resetBlockRead(instance, chainId, walletAddress);
        }
        catch (Exception e)
        {
            //
        }
    }

    private void resetBlockRead(Realm instance, int chainId, String walletAddress)
    {
        try
        {
            RealmToken realmToken = instance.where(RealmToken.class)
                    .equalTo("address", databaseKey(chainId, walletAddress))
                    .equalTo("chainId", chainId)
                    .findFirst();

            if (realmToken != null)
            {
                instance.executeTransaction(realm -> {
                    realmToken.setEarliestTransactionBlock(0);
                    realmToken.setLastBlock(0);
                    realmToken.setLastTxTime(0);
                });
            }
        }
        catch (Exception e)
        {
            //
        }
    }

    private void writeEvents(Realm instance, EtherscanEvent[] events, String walletAddress, @NonNull NetworkInfo networkInfo, final boolean isNFT) throws Exception
    {
        String TO_TOKEN = "[TO_ADDRESS]";
        String FROM_TOKEN = "[FROM_ADDRESS]";
        String AMOUNT_TOKEN = "[AMOUNT_TOKEN]";
        String VALUES = "from,address," + FROM_TOKEN + ",to,address," + TO_TOKEN + ",amount,uint256," + AMOUNT_TOKEN;

        //write event list
        for (EtherscanEvent ev : events)
        {
            Transaction tx = isNFT ? ev.createNFTTransaction(networkInfo) : ev.createTransaction(networkInfo);
            //find tx name
            String activityName = tx.getEventName(walletAddress);
            String valueList = VALUES.replace(TO_TOKEN, ev.to).replace(FROM_TOKEN, ev.from).replace(AMOUNT_TOKEN,
                    (isNFT || ev.value == null) ? "1" : ev.value); //Etherscan sometimes interprets NFT transfers as FT's
            storeTransferData(instance, tx.hash, valueList, activityName, ev.contractAddress);
            //ensure we have fetched the transaction for each hash
            checkTransaction(instance, tx, walletAddress, networkInfo);
        }
    }

    private void storeTransferData(Realm instance, String hash, String valueList, String activityName, String tokenAddress) throws Exception
    {
        RealmTransfer matchingEntry = instance.where(RealmTransfer.class)
                .equalTo("hash", hash)
                .equalTo("tokenAddress", tokenAddress)
                .equalTo("eventName", activityName)
                .equalTo("transferDetail", valueList)
                .findFirst();

        if (matchingEntry == null) //prevent duplicates
        {
            instance.beginTransaction();
            RealmTransfer realmToken = instance.createObject(RealmTransfer.class);
            realmToken.setHash(hash);
            realmToken.setTokenAddress(tokenAddress);
            realmToken.setEventName(activityName);
            realmToken.setTransferDetail(valueList);
            instance.commitTransaction();
        }
        else
        {
            System.out.println("Prevented collision: " + tokenAddress);
        }
    }

    private void checkTransaction(Realm instance, Transaction tx, String walletAddress, NetworkInfo networkInfo) throws Exception
    {
        RealmTransaction matchingKey = instance.where(RealmTransaction.class)
                .equalTo("hash", tx.hash)
                .findFirst();

        if (matchingKey != null)
        {
            Transaction otx = TransactionsRealmCache.convert(matchingKey);
            if (otx.input.length() < FUNCTION_LENGTH)
            {
                instance.beginTransaction();
                matchingKey.deleteFromRealm();
                instance.commitTransaction();
                matchingKey = null;
            }
        }

        if (matchingKey == null)
        {
            fetchAndStoreTransaction(walletAddress, tx.hash, networkInfo.chainId, tx.timeStamp);
        }
    }

    public void fetchAndStoreTransaction(String walletAddress, String txHash, int chainId, long txTime)
    {
        Web3j web3j = getWeb3jService(chainId);
        EventUtils.getTransactionDetails(txHash, web3j)
                .map(ethTx -> new Transaction(ethTx.getResult(), chainId, txTime))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tx -> writeTransaction(walletAddress, tx), Throwable::printStackTrace)
                .isDisposed();
    }

    private void writeTransaction(String walletAddress, Transaction tx)
    {
        try (Realm instance = realmManager.getRealmInstance(walletAddress))
        {
            instance.beginTransaction();
            RealmTransaction realmTx = instance.where(RealmTransaction.class)
                    .equalTo("hash", tx.hash)
                    .findFirst();

            if (realmTx == null) realmTx = instance.createObject(RealmTransaction.class, tx.hash);
            TransactionsRealmCache.fill(instance, realmTx, tx);
            instance.commitTransaction();
        }
        catch (Exception e)
        {
            //
        }
    }
}