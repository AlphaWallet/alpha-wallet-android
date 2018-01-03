package com.wallet.crypto.trustapp.service;

import com.google.gson.Gson;
import com.wallet.crypto.trustapp.entity.Ticker;

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


public class CoinmarketcapTickerService implements TickerService {

    private static final String COINMARKET_API_URL = "https://api.coinmarketcap.com";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private CoinmarketApiClient coinmarketApiClient;

    public CoinmarketcapTickerService(
            OkHttpClient httpClient,
            Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
        buildApiClient(COINMARKET_API_URL);
    }

    private void buildApiClient(String baseUrl) {
        coinmarketApiClient = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(CoinmarketApiClient.class);
    }

    @Override
    public Observable<Ticker> fetchTickerPrice(String ticker) {
        return coinmarketApiClient
                .fetchTickerPrice(ticker)
                .lift(apiError(gson))
                .map(r -> r[0])
                .subscribeOn(Schedulers.io());
    }

    private static @NonNull
    <T> ApiErrorOperator<T> apiError(Gson gson) {
        return new ApiErrorOperator<>(gson);
    }

    public interface CoinmarketApiClient {
        @GET("/v1/ticker/{ticker}")
        Observable<Response<Ticker[]>> fetchTickerPrice(@Path("ticker") String ticker);
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
