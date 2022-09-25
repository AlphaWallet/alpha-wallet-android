package com.alphawallet.app.service;

import android.net.Uri;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.lifi.RouteOptions;
import com.alphawallet.app.entity.lifi.Token;
import com.alphawallet.app.repository.SwapRepository;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class SwapService
{
    private static OkHttpClient httpClient;

    public SwapService()
    {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(C.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(C.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    private Request buildRequest(String api)
    {
        Request.Builder requestB = new Request.Builder()
                .url(api)
                .header("User-Agent", "Chrome/74.0.3729.169")
                .addHeader("Content-Type", "application/json")
                .get();
        return requestB.build();
    }

    private Request buildPostRequest(String api, RequestBody requestBody)
    {
        Request.Builder requestB = new Request.Builder()
                .url(api)
                .header("User-Agent", "Chrome/74.0.3729.169")
                .addHeader("Content-Type", "application/json")
                .post(requestBody);
        return requestB.build();
    }

    private String executeRequest(String api)
    {
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

    private String executePostRequest(String api, RequestBody requestBody)
    {
        try (okhttp3.Response response = httpClient.newCall(buildPostRequest(api, requestBody)).execute())
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

    public Single<String> getTools()
    {
        return Single.fromCallable(this::fetchTools);
    }

    public Single<String> getConnections(long from, long to)
    {
        return Single.fromCallable(() -> fetchPairs(from, to));
    }

    public Single<String> getQuote(Token source,
                                   Token dest,
                                   String address,
                                   String amount,
                                   String slippage,
                                   String allowExchanges)
    {
        return Single.fromCallable(() -> fetchQuote(source, dest, address, amount, slippage, allowExchanges));
    }

    public Single<String> getRoutes(Token source,
                                    Token dest,
                                    String address,
                                    String amount,
                                    String slippage,
                                    Set<String> exchanges)
    {
        return Single.fromCallable(() -> fetchRoutes(source, dest, address, amount, slippage, exchanges));
    }

    public Single<String> getRoutes(String fromChainId,
                                    String toChainId,
                                    String fromTokenAddress,
                                    String toTokenAddress,
                                    String fromAddress,
                                    String fromAmount,
                                    String slippage,
                                    Set<String> exchanges)
    {
        return Single.fromCallable(() -> fetchRoutes(fromChainId, toChainId, fromTokenAddress, toTokenAddress, fromAddress, fromAmount, slippage, exchanges));
    }

    public String fetchChains()
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(SwapRepository.FETCH_CHAINS);
        return executeRequest(builder.build().toString());
    }

    public String fetchTools()
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(SwapRepository.FETCH_TOOLS);
        return executeRequest(builder.build().toString());
    }

    public String fetchPairs(long fromChain, long toChain)
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(SwapRepository.FETCH_TOKENS)
                .appendQueryParameter("fromChain", String.valueOf(fromChain))
                .appendQueryParameter("toChain", String.valueOf(toChain));
        return executeRequest(builder.build().toString());
    }

    public String fetchQuote(Token source,
                             Token dest,
                             String address,
                             String amount,
                             String slippage,
                             String allowExchanges)
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(SwapRepository.FETCH_QUOTE)
                .appendQueryParameter("fromChain", String.valueOf(source.chainId))
                .appendQueryParameter("toChain", String.valueOf(dest.chainId))
                .appendQueryParameter("fromToken", source.address)
                .appendQueryParameter("toToken", dest.address)
                .appendQueryParameter("fromAddress", address)
                .appendQueryParameter("fromAmount", BalanceUtils.getRawFormat(amount, source.decimals))
                .appendQueryParameter("allowExchanges", allowExchanges)
                .appendQueryParameter("slippage", slippage);
        return executeRequest(builder.build().toString());
    }

    public String fetchRoutes(Token source,
                              Token dest,
                              String address,
                              String amount,
                              String slippage,
                              Set<String> exchanges)
    {
        RouteOptions options = new RouteOptions();
        options.slippage = slippage;
        options.exchanges.allow.addAll(exchanges);

        RequestBody body = null;
        try
        {
            JSONObject json = new JSONObject();
            json.put("fromChainId", String.valueOf(source.chainId));
            json.put("toChainId", String.valueOf(dest.chainId));
            json.put("fromTokenAddress", source.address);
            json.put("toTokenAddress", dest.address);
            json.put("fromAddress", address);
            json.put("fromAmount", BalanceUtils.getRawFormat(amount, source.decimals));
            json.put("options", options.getJson());
            body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        }
        catch (JSONException e)
        {
            Timber.e(e);
        }

        return executePostRequest(SwapRepository.FETCH_ROUTES, body);
    }

    public String fetchRoutes(String fromChainId,
                              String toChainId,
                              String fromTokenAddress,
                              String toTokenAddress,
                              String fromAddress,
                              String fromAmount,
                              String slippage,
                              Set<String> exchanges)
    {
        RouteOptions options = new RouteOptions();
        options.slippage = slippage;
        options.exchanges.allow.addAll(exchanges);

        RequestBody body = null;
        try
        {
            JSONObject json = new JSONObject();
            json.put("fromChainId", fromChainId);
            json.put("toChainId", toChainId);
            json.put("fromTokenAddress", fromTokenAddress);
            json.put("toTokenAddress", toTokenAddress);
            json.put("fromAddress", fromAddress);
            json.put("fromAmount", fromAmount);
            json.put("options", options.getJson());
            body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        }
        catch (JSONException e)
        {
            Timber.e(e);
        }

        return executePostRequest(SwapRepository.FETCH_ROUTES, body);
    }
}
