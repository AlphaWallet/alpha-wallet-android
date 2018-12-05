package io.stormbird.wallet.service;

import android.text.TextUtils;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.ERC875ContractTransaction;
import io.stormbird.wallet.entity.EtherscanTransaction;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.TransactionContract;
import io.stormbird.wallet.entity.TransactionOperation;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.WalletUpdate;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class TransactionsNetworkClient implements TransactionsNetworkClientType {

	private final int PAGESIZE = 300;

    private final OkHttpClient httpClient;
	private final Gson gson;
	private final EthereumNetworkRepositoryType networkRepository;

	public TransactionsNetworkClient(
			OkHttpClient httpClient,
			Gson gson,
			EthereumNetworkRepositoryType networkRepository) {
		this.httpClient = httpClient;
		this.gson = gson;
		this.networkRepository = networkRepository;
	}

	@Override
	public Observable<Transaction[]> fetchLastTransactions(NetworkInfo networkInfo, Wallet wallet, long lastBlock, String userAddress)
	{
		long lastBlockNumber = lastBlock + 1;
		return Observable.fromCallable(() -> {
			List<Transaction> result = new ArrayList<>();
			try
			{
				int page = 1;
				String response = readTransactions(networkInfo, wallet.address, String.valueOf(lastBlockNumber), true, page, PAGESIZE);

				while (response != null)
				{
					JSONObject stateData = new JSONObject(response);
					JSONArray orders = stateData.getJSONArray("result");
					EtherscanTransaction[] myTxs = gson.fromJson(orders.toString(), EtherscanTransaction[].class);
					for (EtherscanTransaction etx : myTxs)
					{
					    Transaction tx = etx.createTransaction(userAddress);
					    if (tx != null)
                        {
                            result.add(tx);
                        }
					}
					response = readTransactions(networkInfo, wallet.address, String.valueOf(lastBlockNumber), true, page++, PAGESIZE);
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

    private static final String TENZID = "0xe47405AF3c470e91a02BFC46921C3632776F9C6b";

	@Override
	public Single<WalletUpdate> scanENSTransactionsForWalletNames(Wallet[] wallets, long lastBlock)
	{
		return Single.fromCallable(() -> {
			EtherscanTransaction.prepParser();
			WalletUpdate result = new WalletUpdate();
			result.wallets = new HashMap<>();
			result.lastBlock = lastBlock;
			boolean first = true;
			Map<String, Wallet> walletMap = new HashMap<>();
			for (Wallet w : wallets)
			{
				walletMap.put(w.address, w);
			}

			try
			{
				NetworkInfo network = networkRepository.getAvailableNetworkList()[0];
				int page = 1;
				String response = readTransactions(network, TENZID, String.valueOf(lastBlock), false, page, PAGESIZE);

				while (response != null)
				{
					JSONObject stateData = new JSONObject(response);
					JSONArray orders = stateData.getJSONArray("result");
					EtherscanTransaction[] myTxs = gson.fromJson(orders.toString(), EtherscanTransaction[].class);
					for (EtherscanTransaction etx : myTxs)
					{
						Wallet w = etx.scanForENS(walletMap);
						if (w != null && walletMap.containsKey(w.address)) //only accept the most recent ENS
						{
							walletMap.remove(w.address);
							result.wallets.put(w.address, w);
						}

						if (first) //first tx will be highest block (descending sort)
						{
							long block = Long.parseLong(etx.blockNumber);
							if (block > result.lastBlock)
								result.lastBlock = Long.parseLong(etx.blockNumber) + 1;
							first = false;
						}
					}
                    if (myTxs.length < PAGESIZE)
                    {
                        break; //no need to go any further
                    }
					response = readTransactions(network, TENZID, String.valueOf(lastBlock), false, page++, PAGESIZE);
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

			return result;
		}).subscribeOn(Schedulers.io());
	}

	@Override
	public Single<Integer> checkConstructorArgs(NetworkInfo networkInfo, String address)
	{
		return Single.fromCallable(() -> {
			int result = 256;
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
						Transaction tx = etx.createTransaction(null);
						if (tx.isConstructor && tx.operations.length > 0)
						{
							TransactionContract ct = tx.operations[0].contract;
							result = ct.decimals;
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
			return result;
		});
	}
}