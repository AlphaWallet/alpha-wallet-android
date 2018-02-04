package com.wallet.crypto.trustapp.service;

import com.google.gson.Gson;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Call;
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

    @Override
    public Observable<Transaction[]> fetchLastTransactions(Wallet wallet, Transaction lastTransaction) {
	    return Observable.fromCallable(() -> {
            @NonNull String lastTransactionHash = lastTransaction == null
                    ? "" : lastTransaction.hash;
            List<Transaction> result = new ArrayList<>();
            int pages = 0;
            int page = 0;
            boolean hasMore = true;
	        do {
                page++;
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
            } while (page < pages && hasMore);
	        return result.toArray(new Transaction[result.size()]);
        })
        .subscribeOn(Schedulers.io());
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
