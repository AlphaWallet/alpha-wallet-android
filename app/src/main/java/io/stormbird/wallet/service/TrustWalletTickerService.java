package io.stormbird.wallet.service;

import com.google.gson.Gson;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenTicker;

import io.reactivex.Observable;
import io.reactivex.Single;
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
    private static final String COINMARKET_API = "https://api.coinmarketcap.com";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private ApiClient apiClient;

    public TrustWalletTickerService(
            OkHttpClient httpClient,
            Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
        buildApiClient(COINMARKET_API);
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
                .map(r -> {
                    if (r.isSuccessful()) {
                        TrustResponse<Ticker> body = r.body();
                        if (body == null || body.response.length < 0) {
                            throw new Exception("server error");
                        }
                        return body.response[0];
                    } else {
                        throw new Exception("server error");
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<TokenTicker[]> fetchTokenTickers(Token[] tokens, String currency) {
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

    //{"status":true,"response":[{"percent_change_24h":"0","image":"https://files.coinmarketcap.com/static/img/coins/128x128/undefined.png"}]}
    public interface ApiClient {
        @GET("/v1/ticker/ethereum/")
        //@GET("prices?currency=USD&")
        Observable<Response<TrustResponse<Ticker>>> fetchTickerPrice(@Query("convert") String symbols);

        @POST("tokenPrices")
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
}
