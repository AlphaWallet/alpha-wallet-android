package io.stormbird.wallet.service;

import android.text.TextUtils;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.EtherscanTransaction;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class TransactionsNetworkClient implements TransactionsNetworkClientType {

    private final OkHttpClient httpClient;
	private final Gson gson;

	public TransactionsNetworkClient(
			OkHttpClient httpClient,
			Gson gson,
			EthereumNetworkRepositoryType networkRepository) {
		this.httpClient = httpClient;
		this.gson = gson;
	}

	@Override
	public Observable<Transaction[]> fetchLastTransactions(NetworkInfo networkInfo, Wallet wallet, long lastBlock, String userAddress)
	{
		long lastBlockNumber = lastBlock + 1;
		return Observable.fromCallable(() -> {
			List<Transaction> result = new ArrayList<>();
			try
			{
				String response = readTransactions(networkInfo, wallet.address, String.valueOf(lastBlockNumber));

				if (response != null)
				{
					Gson reader = new Gson();
					JSONObject stateData = new JSONObject(response);
					JSONArray orders = stateData.getJSONArray("result");
					EtherscanTransaction[] myTxs = reader.fromJson(orders.toString(), EtherscanTransaction[].class);
					for (EtherscanTransaction etx : myTxs)
					{
					    Transaction tx = etx.createTransaction(userAddress);
					    if (tx != null)
                        {
                            result.add(tx);
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

			return result.toArray(new Transaction[result.size()]);
		}).subscribeOn(Schedulers.io());
	}

    private String readTransactions(NetworkInfo networkInfo, String address, String firstBlock)
    {
        okhttp3.Response response = null;
        String result = null;
        String fullUrl = null;

		if (networkInfo != null && !TextUtils.isEmpty(networkInfo.etherscanTxUrl))
		{
			StringBuilder sb = new StringBuilder();
			sb.append(networkInfo.etherscanTxUrl);
			sb.append("api?module=account&action=txlist&address=");
			sb.append(address);
			sb.append("&startblock=");
			sb.append(firstBlock);
			sb.append("&endblock=99999999&sort=asc&apikey=6U31FTHW3YYHKW6CYHKKGDPHI9HEJ9PU5F");
			fullUrl = sb.toString();

			try
			{
				Request request = new Request.Builder()
						.url(fullUrl)
						.get()
						.build();

				response = httpClient.newCall(request).execute();

				result = response.body().string();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

        return result;
    }
}
