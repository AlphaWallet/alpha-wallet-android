package com.alphawallet.app.service;

import android.content.Context;
import android.text.TextUtils;

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
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class TransactionsNetworkClient implements TransactionsNetworkClientType
{
    private final int PAGESIZE = 800;

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final RealmManager realmManager;
    private final Context context;
    private long oldestTxBlock;

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
     * Scans the transactions for an address and stores new transactions in the database
     *
     * @param walletAddress
     * @param networkInfo
     * @param checkingAddress
     * @param lastBlock
     * @return
     */
    @Override
    public Single<Transaction[]> storeNewTransactions(String walletAddress, NetworkInfo networkInfo, String checkingAddress, long lastBlock, long sync, boolean isEth)
    {
        oldestTxBlock = 0;
        long lastBlockNumber = lastBlock + 1;

        return Single.fromCallable(() -> {
            EtherscanTransaction lastTransaction = null;
            List<Transaction> updates = new ArrayList<>();

            try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
            {
                if (sync == -1)
                {
                    lastTransaction = syncUpwards(instance, updates, networkInfo, checkingAddress, lastBlockNumber, walletAddress, isEth);
                    if (oldestTxBlock != 0) updateTokenBlockSync(instance, networkInfo.chainId, checkingAddress); //require re-sync
                }
                else
                {
                    //not synced, read latest transactions (sync upwards) then move downward from previous sync point
                    if (lastBlockNumber > 1) lastTransaction = syncUpwards(instance, updates, networkInfo, checkingAddress, lastBlockNumber - 1, walletAddress, isEth); //-1 to ensure we pick up the highest tx value
                    if (oldestTxBlock == 0)
                    {
                        EtherscanTransaction firstTx = syncDownwards(instance, networkInfo, checkingAddress, sync, walletAddress, isEth);
                        if (lastTransaction == null)
                            lastTransaction = firstTx; //record highest read tx if there was no sync
                    }
                    updateTokenBlockSync(instance, networkInfo.chainId, checkingAddress);
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
                updates.add(lastTransaction.createTransaction(null, context, networkInfo.chainId));
            }

            return updates.toArray(new Transaction[0]);
        }).subscribeOn(Schedulers.io());
    }

    private EtherscanTransaction syncDownwards(Realm instance, NetworkInfo networkInfo, String checkingAddress, long sync, String walletAddress, boolean isAccount) throws Exception
    {
        int page = 1;
        if (sync == 0) sync = 999999999L;
        List<Transaction> txList = new ArrayList<>();
        EtherscanTransaction firstTransaction = null;
        String response = readTransactions(networkInfo, checkingAddress, String.valueOf(sync), false, page++, PAGESIZE);
        while (response != null && page <= 4) // only 4 pages at a time for each check, to avoid congestion
        {
            EtherscanTransaction[] myTxs = getEtherscanTransactions(response);
            getRelatedTransactionList(txList, myTxs, isAccount, walletAddress, context, networkInfo.chainId);
            if (myTxs.length > 0 && firstTransaction == null)
            {
                firstTransaction = myTxs[0];
            }

            if (myTxs.length > 0)
            {
                oldestTxBlock = Long.parseLong(myTxs[myTxs.length - 1].blockNumber);
            }

            writeTransactions(instance, txList); //record transactions here

            if (myTxs.length < PAGESIZE)
            {
                oldestTxBlock = -1;
                break;
            }

            response = readTransactions(networkInfo, checkingAddress, String.valueOf(sync), false, page++, PAGESIZE);
        }

        if (page <= 4) oldestTxBlock = -1;

        return firstTransaction;
    }

    private EtherscanTransaction syncUpwards(Realm instance, List<Transaction> updates, NetworkInfo networkInfo, String checkingAddress, long lastBlockNumber, String walletAddress, boolean isAccount) throws Exception
    {
        int page = 1;
        List<Transaction> txList = new ArrayList<>();
        EtherscanTransaction lastTransaction = null;
        String response = readTransactions(networkInfo, checkingAddress, String.valueOf(lastBlockNumber), true, page++, PAGESIZE);
        while (response != null && page <= 4) // only 4 pages at a time for each check, to avoid congestion
        {
            EtherscanTransaction[] myTxs = getEtherscanTransactions(response);
            getRelatedTransactionList(txList, myTxs, isAccount, walletAddress, context, networkInfo.chainId);
            if (myTxs.length > 0)
            {
                lastTransaction = myTxs[myTxs.length-1];
            }

            writeTransactions(instance, txList); //record transactions here
            updates.addAll(txList);

            response = readTransactions(networkInfo, checkingAddress, String.valueOf(lastBlockNumber), true, page++, PAGESIZE);
        }

        if (page > 4)
        {
            updates.clear();
            //didn't fully sync, skim off top 100 Txns to ensure user sees top txns - only happens if user has previously synced and hasn't checked for a while
            response = readTransactions(networkInfo, checkingAddress, "9999999999", false, 1, 100);
            EtherscanTransaction[] myTxs = getEtherscanTransactions(response);
            getRelatedTransactionList(txList, myTxs, isAccount, walletAddress, context, networkInfo.chainId);
            writeTransactions(instance, txList);
            updates.addAll(txList);
            lastTransaction = myTxs[0];
            oldestTxBlock = Long.parseLong(myTxs[myTxs.length - 1].blockNumber);
        }

        return lastTransaction;
    }

    private void getRelatedTransactionList(List<Transaction> txList, EtherscanTransaction[] myTxs, boolean isAccount, String walletAddress, Context context, int chainId)
    {
        txList.clear();
        for (EtherscanTransaction etx : myTxs)
        {
            Transaction tx = etx.createTransaction(isAccount ? null : walletAddress.toLowerCase(), context, chainId);
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

    private void updateTokenBlockSync(Realm realm, int chainId, String checkingAddress)
    {
        if (oldestTxBlock == 0) return;
        String key = TokensRealmSource.databaseKey(chainId, checkingAddress);
        RealmToken realmToken = realm.where(RealmToken.class)
                .equalTo("address", key)
                .equalTo("chainId", chainId)
                .findFirst();

        if (realmToken != null)
        {
            realm.beginTransaction();
            realmToken.setTXUpdateTime(oldestTxBlock);
            realm.commitTransaction();
        }
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
                    result = null;
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

    @Override
    public Single<ContractType> checkConstructorArgs(NetworkInfo networkInfo, String address)
    {
        return Single.fromCallable(() -> {
            ContractType result = ContractType.OTHER;
            try
            {
                String response = readTransactions(networkInfo, address, "0", true, 1, 5);

                if (response != null)
                {
                    JSONObject stateData = new JSONObject(response);
                    JSONArray orders = stateData.getJSONArray("result");
                    EtherscanTransaction[] myTxs = gson.fromJson(orders.toString(), EtherscanTransaction[].class);
                    for (EtherscanTransaction etx : myTxs)
                    {
                        Transaction tx = etx.createTransaction(null, context, networkInfo.chainId);
                        if (tx.isConstructor && tx.operations.length > 0)
                        {
                            TransactionContract ct = tx.operations[0].contract;
                            result = ContractType.values()[ct.decimals];
                            break;
                        }
                    }
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

            if (result == ContractType.OTHER && checkERC20(networkInfo, address))
            {
                result = ContractType.ERC20;
            }
            return result;
        });
    }

    private boolean checkERC20(NetworkInfo networkInfo, String address)
    {
        boolean isERC20 = false;
        if (networkInfo != null && !TextUtils.isEmpty(networkInfo.etherscanTxUrl))
        {
            okhttp3.Response response = null;
            StringBuilder sb = new StringBuilder();
            sb.append(networkInfo.etherscanTxUrl);
            sb.append("api?module=stats&action=tokensupply&contractaddress=");
            sb.append(address);
            sb.append("&apikey=6U31FTHW3YYHKW6CYHKKGDPHI9HEJ9PU5F");

            try
            {
                Request request = new Request.Builder()
                        .url(sb.toString())
                        .get()
                        .build();

                response = httpClient.newCall(request).execute();

                String result = response.body().string();
                if (result != null && result.length() > 20)
                {
                    JSONObject stateData = new JSONObject(result);
                    //{"status":"1","message":"OK","result":"92653503768584227777966602"}
                    String value = stateData.getString("result");
                    if (value.length() > 0 && Character.isDigit(value.charAt(0)))
                    {
                        System.out.println("ERC20: " + value);
                        BigInteger supply = new BigInteger(value, 10);
                        if (supply.compareTo(BigInteger.ZERO) > 0)
                        {
                            isERC20 = true;
                        }
                    }
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

        return isERC20;
    }

    private String databaseKey(Token token)
    {
        return token.tokenInfo.address.toLowerCase() + "-" + token.tokenInfo.chainId;
    }

    @Override
    public void storeBlockRead(Token token, String walletAddress)
    {
        try (Realm instance = realmManager.getRealmInstance(new Wallet(walletAddress)))
        {
            instance.executeTransactionAsync(new Realm.Transaction()
            {
                @Override
                public void execute(Realm realm)
                {
                    RealmToken realmToken = realm.where(RealmToken.class)
                            .equalTo("address", databaseKey(token))
                            .equalTo("chainId", token.tokenInfo.chainId)
                            .findFirst();

                    if (realmToken != null)
                    {
                        token.setRealmLastBlock(realmToken);
                        realm.insertOrUpdate(realmToken);
                    }
                }
            });
        }
    }
}