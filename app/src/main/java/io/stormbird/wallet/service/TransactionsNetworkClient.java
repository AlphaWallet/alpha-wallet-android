package io.stormbird.wallet.service;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONObject;

import io.stormbird.wallet.entity.EtherscanTransaction;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.TransactionsCallback;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class TransactionsNetworkClient implements TransactionsNetworkClientType {

    private static final int PAGE_LIMIT = 20;

    private final OkHttpClient httpClient;
	private final Gson gson;

    private ApiClient apiClient;

	public TransactionsNetworkClient(
			OkHttpClient httpClient,
			Gson gson,
			EthereumNetworkRepositoryType networkRepository) {
		this.httpClient = httpClient;
		this.gson = gson;

		networkRepository.addOnChangeDefaultNetwork(this::onNetworkChanged);
		NetworkInfo networkInfo = networkRepository.getDefaultNetwork();
		onNetworkChanged(networkInfo);
	}

	private void buildApiClient(String baseUrl) {
		apiClient = new Retrofit.Builder()
				.baseUrl(baseUrl)
				.client(httpClient)
				.addConverterFactory(GsonConverterFactory.create(gson))
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
				.build()
				.create(ApiClient.class);
	}

	@Override
	public Observable<Transaction[]> fetchTransactions(String address) {
		return apiClient
				.fetchTransactions(address)
				.lift(apiError())
				.map(r -> r.docs)
				.subscribeOn(Schedulers.io());
	}

	private Transaction[] fetchLastTransactionsTrust(Wallet wallet, long lastBlock)
	{
		final long lastBlockPlus = lastBlock + 1;
		List<Transaction> result = new ArrayList<>();
		int pages = 0;
		int page = 0;
		boolean hasMore = true;
		do {
			page++;
			try {
				Call<ApiClientResponse> call = apiClient.fetchTransactions(PAGE_LIMIT, page, wallet.address);
				Response<ApiClientResponse> response = call.execute();
				if (response.isSuccessful()) {
					ApiClientResponse body = response.body();
					if (body != null) {
						pages = body.pages;
						for (Transaction transaction : body.docs) {
							if (Long.valueOf(transaction.blockNumber) <= lastBlockPlus) {
								hasMore = false;
								break;
							}
							result.add(transaction);
						}
					}
				}
			}
			catch (IOException e) //Connection gets severed if the owning fragment is cleared by Android OS
			//The best we can do is catch the exception thrown by OKHTTP
			{
				return result.toArray(new Transaction[result.size()]);
			}
		} while (page < pages && hasMore);
		return result.toArray(new Transaction[result.size()]);
	}

	@Override
	public Observable<Transaction[]> fetchLastTransactions(NetworkInfo networkInfo, Wallet wallet, long lastBlock)
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
						result.add(etx.createTransaction());
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				//if this process fails, also try trust eth wallet API
				return fetchLastTransactionsTrust(wallet, lastBlock);
			}

			return result.toArray(new Transaction[result.size()]);
		}).subscribeOn(Schedulers.io());
	}

	@Override
	public Observable<Transaction[]> fetchContractTransactions(String address, String feemaster)
	{
		return Observable.fromCallable(() -> {
			List<Transaction> result = new ArrayList<>();
			try
			{
				String response = readContractTransactions(address, feemaster);

				Gson reader = new Gson();
				JSONArray stateData = new JSONArray(response);
				EtherscanTransaction[] txs = reader.fromJson(stateData.toString(), EtherscanTransaction[].class);
				for (EtherscanTransaction etx : txs)
				{
					etx.internal = true;
					result.add(etx.createTransaction());
				}
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

	private String readContractTransactions(String address, String feemaster)
	{
		okhttp3.Response response = null;
		String result = null;

		StringBuilder sb = new StringBuilder();
		sb.append(feemaster);
		sb.append("internalTx?address=");
		sb.append(address);

		try
		{
			Request request = new Request.Builder()
					.url(sb.toString())
					.get()
					.build();

			response = httpClient.newCall(request).execute();

			result = response.body().string();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

    private void onNetworkChanged(NetworkInfo networkInfo) {
		buildApiClient(networkInfo.backendUrl);
	}

	private static @NonNull <T> ApiErrorOperator<T> apiError() {
		return new ApiErrorOperator<>();
	}

	private interface ApiClient {
		@GET("/transactions?limit=50")
		Observable<Response<ApiClientResponse>> fetchTransactions(
				@Query("address") String address);

        @GET("/transactions")
        Call<ApiClientResponse> fetchTransactions(
                @Query("limit") int pageLimit,
                @Query("page") int page,
                @Query("address") String address);
    }

	private final static class ApiClientResponse {
		Transaction[] docs;
		int pages;
	}

	private final static class ApiErrorOperator <T> implements ObservableOperator<T, Response<T>> {

		@Override
		public Observer<? super retrofit2.Response<T>> apply(Observer<? super T> observer) throws Exception {
            return new DisposableObserver<Response<T>>() {
                @Override
                public void onNext(Response<T> response) {
                    observer.onNext(response.body());
                    observer.onComplete();
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }

                @Override
                public void onComplete() {
                    observer.onComplete();
                }
            };
		}
	}
}
