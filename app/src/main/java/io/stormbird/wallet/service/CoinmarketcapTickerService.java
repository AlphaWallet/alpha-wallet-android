package io.stormbird.wallet.service;

import com.google.gson.Gson;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenTicker;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import com.google.gson.Gson;


public class CoinmarketcapTickerService implements TickerService
{

    private static final String COINMARKET_API_URL = "https://api.coinmarketcap.com";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private CoinmarketApiClient coinmarketApiClient;

    public CoinmarketcapTickerService(OkHttpClient httpClient, Gson gson)
    {
        this.httpClient = httpClient;
        this.gson = gson;
        buildApiClient(COINMARKET_API_URL);
    }

    private void buildApiClient(String baseUrl)
    {
        coinmarketApiClient = new Retrofit.Builder().baseUrl(baseUrl).client(httpClient).addConverterFactory(GsonConverterFactory.create(gson)).addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build().create(CoinmarketApiClient.class);
    }

    @Override
    public Observable<Ticker> fetchTickerPrice(String ticker)
    {
        return coinmarketApiClient.fetchTickerPrice(ticker).lift(apiError(gson)).map(r -> r[0]).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<TokenTicker[]> fetchTokenTickers(Token[] tokens, String currency)
    {
        return Single.just(new TokenTicker[0]);
    }

    private static @NonNull
    <T> ApiErrorOperator<T> apiError(Gson gson)
    {
        return new ApiErrorOperator<>();
    }

    public interface CoinmarketApiClient
    {
        @GET("/v1/ticker/{ticker}")
        Observable<Response<Ticker[]>> fetchTickerPrice(@Path("ticker") String ticker);
    }

    private final static class ApiErrorOperator<T> implements ObservableOperator<T, Response<T>>
    {

        @Override
        public Observer<? super Response<T>> apply(Observer<? super T> observer)
        {
            return new DisposableObserver<Response<T>>()
            {
                @Override
                public void onNext(Response<T> response)
                {
                    if (isDisposed())
                    {
                        return;
                    }
                    observer.onNext(response.body());
                    observer.onComplete();
                }

                @Override
                public void onError(Throwable e)
                {
                    if (!isDisposed())
                    {
                        observer.onError(e);
                    }
                }

                @Override
                public void onComplete()
                {
                    if (!isDisposed())
                    {
                        observer.onComplete();
                    }
                }
            };
        }
    }

    public Single<Double> convertPair(String currency1, String currency2)
    {
        return Single.fromCallable(() -> {
            String conversionURL = "http://currencies.apps.grandtrunk.net/getlatest/" + currency1 + "/" + currency2;
            Request request = new Request.Builder().url(conversionURL).get().build();

            try
            {
                okhttp3.Response response = httpClient.newCall(request).execute();
                int resultCode = response.code();
                if ((resultCode / 100) == 2 && response.body() != null)
                {
                    String responseBody = response.body().string();
                    double rate = Double.parseDouble(responseBody);
                    return rate;
                }
            }
            catch (Exception e)
            {
                //
            }

            return 1.0;
        });
    }


}
