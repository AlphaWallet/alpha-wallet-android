package com.wallet.crypto.trustapp.service;

import com.google.gson.Gson;
import com.wallet.crypto.trustapp.entity.Token;

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
import retrofit2.http.Path;

public class EthplorerTokenService implements TokenExplorerClientType {
    private static final String ETHPLORER_API_URL = "https://api.ethplorer.io";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private EthplorerApiClient ethplorerApiClient;

    public EthplorerTokenService(
            OkHttpClient httpClient,
            Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
        buildApiClient(ETHPLORER_API_URL);
    }

    private void buildApiClient(String baseUrl) {
        ethplorerApiClient = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(EthplorerApiClient.class);
    }
    @Override
    public Observable<Token[]> fetch(String walletAddress) {
        return ethplorerApiClient.fetchTokens(walletAddress)
                .lift(apiError(gson))
                .map(r -> r.tokens)
                .subscribeOn(Schedulers.io());
    }

    private static @NonNull
    <T> ApiErrorOperator<T> apiError(Gson gson) {
        return new ApiErrorOperator<>(gson);
    }

    public interface EthplorerApiClient {
        @GET("/getAddressInfo/{address}?apiKey=freekey")
        Observable<Response<EthplorerResponse>> fetchTokens(@Path("address") String address);
    }

    private static class EthplorerResponse {
        Token[] tokens;
    }

    private final static class ApiErrorOperator <T> implements ObservableOperator<T, Response<T>> {

        private final Gson gson;

        public ApiErrorOperator(Gson gson) {
            this.gson = gson;
        }

        @Override
        public Observer<? super Response<T>> apply(Observer<? super T> observer) throws Exception {
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
