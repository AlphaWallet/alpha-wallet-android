package com.alphawallet.app.service;

import android.net.Uri;

import com.alphawallet.app.entity.lifi.Connection;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.JsonUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class SwapService
{
    private static final String FETCH_CHAINS = "https://li.quest/v1/chains";
    private static final String FETCH_TOKENS = "https://li.quest/v1/connections";
    private static final String SWAP_TOKEN = "https://li.quest/v1/quote";
    private static OkHttpClient httpClient;

    public SwapService()
    {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    private Request buildRequest(String api)
    {
        Request.Builder requestB = new Request.Builder()
                .url(api)
                .header("User-Agent", "Chrome/74.0.3729.169")
                .method("GET", null)
                .addHeader("Content-Type", "application/json");
        return requestB.build();
    }

    private String executeRequest(String api)
    {
        Timber.d(api);
        try (okhttp3.Response response = httpClient.newCall(buildRequest(api)).execute())
        {
            if (response.isSuccessful())
            {
                ResponseBody responseBody = response.body();
                if (responseBody != null)
                {
                    return responseBody.string();
                }
            }
            else
            {
                return Objects.requireNonNull(response.body()).string();
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
            return e.getMessage();
        }

        return JsonUtils.EMPTY_RESULT;
    }

    public Single<String> getChains()
    {
        return Single.fromCallable(this::fetchChains);
    }

    public Single<String> getConnections(long from, long to)
    {
        return Single.fromCallable(() -> fetchPairs(from, to));
    }

    public Single<String> getQuote(Connection.LToken source, Connection.LToken dest, String address, String amount, String slippage)
    {
        return Single.fromCallable(() -> fetchQuote(source, dest, address, amount, slippage));
    }

    public String fetchChains()
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(FETCH_CHAINS);
        return executeRequest(builder.build().toString());
    }

    public String fetchPairs(long fromChain, long toChain)
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(FETCH_TOKENS)
                .appendQueryParameter("fromChain", String.valueOf(fromChain))
                .appendQueryParameter("toChain", String.valueOf(toChain));
        return executeRequest(builder.build().toString());
    }

    public String fetchQuote(Connection.LToken source, Connection.LToken dest, String address, String amount, String slippage)
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(SWAP_TOKEN)
                .appendQueryParameter("fromChain", String.valueOf(source.chainId))
                .appendQueryParameter("toChain", String.valueOf(dest.chainId))
                .appendQueryParameter("fromToken", source.address)
                .appendQueryParameter("toToken", dest.address)
                .appendQueryParameter("fromAddress", address)
                .appendQueryParameter("fromAmount", BalanceUtils.getRawFormat(amount, source.decimals))
//                .appendQueryParameter("order", "RECOMMENDED")
                .appendQueryParameter("slippage", slippage);
        return executeRequest(builder.build().toString());
    }
}
