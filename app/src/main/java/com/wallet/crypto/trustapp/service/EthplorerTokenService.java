package com.wallet.crypto.trustapp.service;

import com.google.gson.Gson;
import com.wallet.crypto.trustapp.entity.ApiErrorException;
import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.entity.TokenInfo;

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

import static com.wallet.crypto.trustapp.C.ErrorCode.UNKNOWN;

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
                .lift(apiError())
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

    private static @NonNull
    ApiErrorOperator apiError() {
        return new ApiErrorOperator();
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
        EthplorerError error;
    }

    private static class EthplorerError {
        int code;
        String message;
    }

    private final static class ApiErrorOperator implements ObservableOperator<EthplorerResponse, Response<EthplorerResponse>> {

        @Override
        public Observer<? super Response<EthplorerResponse>> apply(Observer<? super EthplorerResponse> observer) throws Exception {
            return new DisposableObserver<Response<EthplorerResponse>>() {
                @Override
                public void onNext(Response<EthplorerResponse> response) {
                    EthplorerResponse body = response.body();
                    if (body != null && body.error == null) {
                        observer.onNext(body);
                        observer.onComplete();
                    } else {
                        if (body != null) {
                            observer.onError(new ApiErrorException(new ErrorEnvelope(body.error.code, body.error.message)));
                        } else {
                            observer.onError(new ApiErrorException(new ErrorEnvelope(UNKNOWN, "Service not available")));
                        }
                    }
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
