package com.alphawallet.app.service;

import com.google.gson.Gson;
import com.alphawallet.app.entity.TokenInfo;

import io.reactivex.Observable;
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

    private EthplorerApiClient ethplorerApiClient;

    public EthplorerTokenService(
            OkHttpClient httpClient,
            Gson gson) {
        ethplorerApiClient = new Retrofit.Builder()
                .baseUrl(ETHPLORER_API_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(EthplorerApiClient.class);
    }

    @Override
    public Observable<TokenInfo[]> fetch(String walletAddress) {
        return ethplorerApiClient.fetchTokens(walletAddress)
                .flatMap(response -> Observable.just(response.body()))
                .map(r -> {
                    if (r.tokens == null) {
                        return new TokenInfo[0];
                    } else {
                        int len = r.tokens.length;
                        TokenInfo[] result = new TokenInfo[len];
                        for (int i = 0; i < len; i++) {
                            result[i] = r.tokens[i].tokenInfo;
                        }
                        return result;
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    public interface EthplorerApiClient {
        @GET("/getAddressInfo/{address}?apiKey=freekey")
        Observable<Response<EthplorerResponse>> fetchTokens(@Path("address") String address);
    }

    private static class Token {
        TokenInfo tokenInfo;
    }

    private static class EthplorerResponse {
        Token[] tokens;
    }
}
