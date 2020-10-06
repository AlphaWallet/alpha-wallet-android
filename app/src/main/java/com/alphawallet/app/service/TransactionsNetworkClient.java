package com.alphawallet.app.service;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.TransactionsRealmCache;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.google.gson.Gson;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.EtherscanTransaction;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InterruptedIOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class TransactionsNetworkClient implements TransactionsNetworkClientType
{
    private final int PAGESIZE = 800;
    private final int SYNC_PAGECOUNT = 2; //how many pages to read when we first sync the account - means we store the first 1600 transactions only
    //Note: if user wants to view transactions older than this, we fetch from etherscan on demand.
    //Generally this would only happen when watching extremely active accounts for curiosity

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final RealmManager realmManager;
    private final Context context;

    public TransactionsNetworkClient(
            OkHttpClient httpClient,
            Gson gson,
            RealmManager realmManager,
            Context context) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.realmManager = realmManager;
        this.context = context;
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
    public Single<Transaction[]> storeNewTransactions(String walletAddress, NetworkInfo networkInfo, long lastBlock)
    {
        long lastBlockNumber = lastBlock + 1;

        return Single.fromCallable(() -> {
            List<Transaction> updates = new ArrayList<>();
            EtherscanTransaction lastTransaction = null;
            try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
            {
                //deleteAllChainTransactions(instance, networkInfo.chainId, walletAddress);
                if (lastBlockNumber == 1) //first read of account. Read first 2 pages
                {
                    lastTransaction = syncDownwards(instance, walletAddress, networkInfo, 9999999999L);
                }
                else // try to sync upwards from the last read
                {
                    lastTransaction = syncUpwards(instance, walletAddress, networkInfo, lastBlockNumber);
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

            //Add last transaction as the final result to update block scan
            if (lastTransaction != null)
            {
                updates.add(lastTransaction.createTransaction(null, networkInfo.chainId));
            }

            return updates.toArray(new Transaction[0]);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * read PAGESIZE*2 transactions down from startingBlockNumber
     *
     * Note that this call is the only place that the 'earliest transaction' block can be written from.
     */
    private EtherscanTransaction syncDownwards(Realm instance, String walletAddress, NetworkInfo networkInfo, long startingBlockNumber) throws Exception
    {
        int page = 1;
        List<Transaction> txList = new ArrayList<>();
        EtherscanTransaction firstTransaction = null;
        EtherscanTransaction lastTransaction = null;
        boolean continueReading = true;

        while (continueReading) // only SYNC_PAGECOUNT pages at a time for each check, to avoid congestion
        {
            String response = readTransactions(networkInfo, walletAddress, String.valueOf(startingBlockNumber), false, page++, PAGESIZE);
            if (response == null) break;
            if (response.equals("0"))
            {
                storeEarliestBlockRead(instance, networkInfo.chainId, walletAddress, startingBlockNumber);
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

            if (page > SYNC_PAGECOUNT) continueReading = false;

            if (myTxs.length < PAGESIZE)
            {
                continueReading = false;
                //store earliest transaction
                if (lastTransaction != null) storeEarliestBlockRead(instance, networkInfo.chainId, walletAddress, Long.parseLong(lastTransaction.blockNumber));
            }
        }

        return firstTransaction;
    }

    private EtherscanTransaction syncUpwards(Realm instance, String walletAddress, NetworkInfo networkInfo, long lastBlockNumber) throws Exception
    {
        int page = 1;
        List<Transaction> txList = new ArrayList<>();
        EtherscanTransaction lastTransaction;

        //only sync upwards by 1 page. If not sufficient then reset; delete DB and start again
        String response = readTransactions(networkInfo, walletAddress, String.valueOf(lastBlockNumber), true, page, PAGESIZE);
        if (response == null || response.equals("0")) return null;
        EtherscanTransaction[] myTxs = getEtherscanTransactions(response);

        if (myTxs.length == PAGESIZE)
        {
            //too big, erase transaction list and start from top
            deleteAllChainTransactions(instance, networkInfo.chainId, walletAddress);
            lastTransaction = syncDownwards(instance, walletAddress, networkInfo, 999999999L); //re-sync downwards from top
            lastTransaction.nonce = -1; //signal to refresh list - this doesn't get written to DB
            return lastTransaction;
        }
        else
        {
            getRelatedTransactionList(txList, myTxs, walletAddress, networkInfo.chainId);
            writeTransactions(instance, txList); //record transactions here
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
            sb.append("&apikey=6U31FTHW3YYHKW6CYHKKGDPHI9HEJ9PU5F");
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
                long oldestTxTime = txList.size() > 0 ? txList.get(txList.size() - 1).timeStamp : lastTxTime;
                try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
                {
                    long oldestBlockRead = getOldestBlockRead(instance, network.chainId, oldestTxTime);
                    long oldestPossibleBlock = getFirstTransactionBlock(instance, network.chainId, walletAddress);
                    System.out.println("DIAGNOSE: " + oldestBlockRead + " : " + oldestPossibleBlock);
                    if (oldestBlockRead > 0 && oldestBlockRead != oldestPossibleBlock)
                    {
                        syncDownwards(instance, walletAddress, network, oldestBlockRead);
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
        //see if we already have transactions older
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

    private String databaseKey(Token token)
    {
        return token.tokenInfo.address.toLowerCase() + "-" + token.tokenInfo.chainId;
    }

    private String databaseKey(int chainId, String walletAddress)
    {
        return walletAddress.toLowerCase() + "-" + chainId;
    }

    @Override
    public void storeBlockRead(Token token, String walletAddress)
    {
        try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
        {
            instance.executeTransactionAsync(realm -> {
                RealmToken realmToken = realm.where(RealmToken.class)
                        .equalTo("address", databaseKey(token))
                        .equalTo("chainId", token.tokenInfo.chainId)
                        .findFirst();

                if (realmToken != null)
                {
                    token.setRealmLastBlock(realmToken);
                    realm.insertOrUpdate(realmToken);
                }
            });
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
}