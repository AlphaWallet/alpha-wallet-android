package com.wallet.crypto.trustapp.service;

import com.google.gson.Gson;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class BlockExplorerClient implements BlockExplorerClientType {

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final EthereumNetworkRepositoryType networkRepository;

	private EtherScanApiClient etherScanApiClient;

	public BlockExplorerClient(
			OkHttpClient httpClient,
			Gson gson,
			EthereumNetworkRepositoryType networkRepository) {
		this.httpClient = httpClient;
		this.gson = gson;
		this.networkRepository = networkRepository;
		this.networkRepository.addOnChangeDefaultNetwork(this::onNetworkChanged);
		NetworkInfo networkInfo = networkRepository.getDefaultNetwork();
		onNetworkChanged(networkInfo);
	}

	private void buildApiClient(String baseUrl) {
		etherScanApiClient = new Retrofit.Builder()
				.baseUrl(baseUrl)
				.client(httpClient)
				.addConverterFactory(GsonConverterFactory.create(gson))
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
				.build()
				.create(EtherScanApiClient.class);
	}

	@Override
	public Observable<Transaction[]> fetchTransactions(String address) {
		return etherScanApiClient
				.fetchTransactions(address)
				.lift(apiError(gson))
				.map(r -> r.docs)
				.subscribeOn(Schedulers.io());
	}

	private void onNetworkChanged(NetworkInfo networkInfo) {
		buildApiClient(networkInfo.backendUrl);
	}

	private static @NonNull <T> ApiErrorOperator<T> apiError(Gson gson) {
		return new ApiErrorOperator<>(gson);
	}

	private interface EtherScanApiClient {
		@GET("/transactions?limit=50") // TODO: startBlock - it's pagination. Not work now
		Observable<Response<EtherScanResponse>> fetchTransactions(
				@Query("address") String address);
	}

	private final static class EtherScanResponse {
		Transaction[] docs;
	}

	private final static class ApiErrorOperator <T> implements ObservableOperator<T, Response<T>> {

		private final Gson gson;

		public ApiErrorOperator(Gson gson) {
			this.gson = gson;
		}

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
