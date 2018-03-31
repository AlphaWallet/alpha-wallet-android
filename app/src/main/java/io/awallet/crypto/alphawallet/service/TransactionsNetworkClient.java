package io.awallet.crypto.alphawallet.service;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONObject;

import io.awallet.crypto.alphawallet.entity.EtherscanTransaction;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.TransactionsCallback;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;

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

	private Transaction[] fetchLastTransactionsTrust(Wallet wallet, Transaction lastTransaction)
	{
		@NonNull String lastTransactionHash = lastTransaction == null
				? "" : lastTransaction.hash;
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
							if (lastTransactionHash.equals(transaction.hash)) {
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
	public Observable<Transaction[]> fetchLastTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction lastTransaction)
	{
		final String lastBlock = (lastTransaction != null) ? lastTransaction.blockNumber : "0";

		return Observable.fromCallable(() -> {
			List<Transaction> result = new ArrayList<>();
			try
			{
				String response = readTransactions(networkInfo, wallet.address, lastBlock);

				Gson reader = new Gson();
				JSONObject stateData = new JSONObject(response);
				JSONArray orders = stateData.getJSONArray("result");
				EtherscanTransaction[] myTxs = reader.fromJson(orders.toString(), EtherscanTransaction[].class);
				for (EtherscanTransaction etx : myTxs)
				{
					result.add(etx.createTransaction());
				}
			}
			catch (Exception e)
			{
				//if this process fails, also try trust eth wallet API
				return fetchLastTransactionsTrust(wallet, lastTransaction);
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
