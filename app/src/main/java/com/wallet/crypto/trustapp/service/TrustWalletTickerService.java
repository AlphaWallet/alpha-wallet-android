package com.wallet.crypto.trustapp.service;

import com.google.gson.Gson;
import com.wallet.crypto.trustapp.entity.Ticker;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.entity.TokenTicker;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;


public class TrustWalletTickerService implements TickerService {

    private static final String TRUST_API_URL = "https://api.trustwalletapp.com";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private ApiClient apiClient;

    public TrustWalletTickerService(
            OkHttpClient httpClient,
            Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
        buildApiClient(TRUST_API_URL);
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
    public Observable<Ticker> fetchTickerPrice(String symbols) {
        return apiClient
                .fetchTickerPrice(symbols)
                .lift(apiError())
                .map(r -> r.response[0])
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<TokenTicker[]> fetchTockenTickers(Token[] tokens, String currency) {
        return Single.fromCallable(() -> {
            if (tokens == null || tokens.length == 0) {
                return null;
            }
            int len = tokens.length;
            TokenTickerRequestBody requestBody = new TokenTickerRequestBody();
            requestBody.currency = currency;
            requestBody.tokens = new TokenDescriptionRequestBody[len];
            for (int i = 0; i < len; i++) {
                requestBody.tokens[i] = new TokenDescriptionRequestBody(
                        tokens[i].tokenInfo.address, tokens[i].tokenInfo.symbol);
            }
            return requestBody;
        })
        .flatMap(body -> apiClient.fetchTokenPrices(body))
        .map(r -> {
            TrustResponse<TokenTicker> body = r.body();
            return body == null ? null : body.response;
        });
    }

    private static @NonNull
    <T> ApiErrorOperator<T> apiError() {
        return new ApiErrorOperator<>();
    }

    public interface ApiClient {
        @GET("prices?currency=USD&")
        Observable<Response<TrustResponse<Ticker>>> fetchTickerPrice(@Query("symbols") String symbols);

        @POST("tokenPrices&")
        Single<Response<TrustResponse<TokenTicker>>> fetchTokenPrices(@Body TokenTickerRequestBody body);
    }

    private static class TokenTickerRequestBody {
        String currency;
        TokenDescriptionRequestBody[] tokens;
    }

    private static class TokenDescriptionRequestBody {
        String contract;
        String symbol;

        TokenDescriptionRequestBody(String contract, String symbol) {
            this.contract = contract;
            this.symbol = symbol;
        }
    }

    private static class TrustResponse<T> {
        T[] response;
    }

    private final static class ApiErrorOperator <T> implements ObservableOperator<T, Response<T>> {

        @Override
        public Observer<? super Response<T>> apply(Observer<? super T> observer) throws Exception {
            return new DisposableObserver<Response<T>>() {
                @Override
                public void onNext(Response<T> response) {
                    if (isDisposed()) {
                        return;
                    }
                    observer.onNext(response.body());
                    observer.onComplete();
                }

                @Override
                public void onError(Throwable e) {
                    if (!isDisposed()) {
                        observer.onError(e);
                    }
                }

                @Override
                public void onComplete() {
                    if (!isDisposed()) {
                        observer.onComplete();
                    }
                }
            };
        }
    }
}
