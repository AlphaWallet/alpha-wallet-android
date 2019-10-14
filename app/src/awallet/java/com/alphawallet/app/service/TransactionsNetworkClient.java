package com.alphawallet.app.service;

import android.content.Context;
import android.text.TextUtils;

import com.alphawallet.app.service.TransactionsNetworkClientType;
import com.google.gson.Gson;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.EtherscanTransaction;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionContract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InterruptedIOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class TransactionsNetworkClient implements TransactionsNetworkClientType
{

    private final int PAGESIZE = 800;

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final EthereumNetworkRepositoryType networkRepository;
    private final Context context;

    public TransactionsNetworkClient(
            OkHttpClient httpClient,
            Gson gson,
            EthereumNetworkRepositoryType networkRepository,
            Context context) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.networkRepository = networkRepository;
        this.context = context;
    }

    /**
     * Fetch transactions for address starting from lastBlock.
     * If using to fetch contract transactions (eg ERC20) also specify 'userAddress' so the transactions can be filtered to only show those relevant transactions
     * ** NB: this function returns an extra transaction which is used to set the 'Last Block Read' to optimise checking transactions
     * **     If you are using this function, beware of the extra transaction
     * @param networkInfo
     * @param tokenAddress
     * @param lastBlock
     * @param userAddress
     * @return ** One extra transaction **
     */
    @Override
    public Single<Transaction[]> fetchLastTransactions(NetworkInfo networkInfo, String tokenAddress, long lastBlock, String userAddress)
    {
        long lastBlockNumber = lastBlock + 1;
        return Single.fromCallable(() -> {
            List<Transaction> result = new ArrayList<>();
            EtherscanTransaction lastTransaction = null;
            try
            {
                int page = 1;
                String response = readTransactions(networkInfo, tokenAddress, String.valueOf(lastBlockNumber), true, page, PAGESIZE);

                while (response != null)
                {
                    JSONObject stateData = new JSONObject(response);
                    JSONArray orders = stateData.getJSONArray("result");
                    EtherscanTransaction[] myTxs = gson.fromJson(orders.toString(), EtherscanTransaction[].class);
                    for (EtherscanTransaction etx : myTxs)
                    {
                        Transaction tx = etx.createTransaction(userAddress, context, networkInfo.chainId);
                        if (tx != null)
                        {
                            result.add(tx);
                        }
                    }
                    if (myTxs.length > 0)
                    {
                        lastTransaction = myTxs[myTxs.length-1];
                    }
                    response = readTransactions(networkInfo, tokenAddress, String.valueOf(lastBlockNumber), true, page++, PAGESIZE);
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
                result.add(lastTransaction.createTransaction(null, context, networkInfo.chainId));
            }

            return result.toArray(new Transaction[result.size()]);
        }).subscribeOn(Schedulers.io());
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
            sb.append("&startblock=");
            sb.append(firstBlock);
            sb.append("&endblock=99999999&sort=");
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
}