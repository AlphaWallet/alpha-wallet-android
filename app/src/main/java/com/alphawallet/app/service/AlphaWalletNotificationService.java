package com.alphawallet.app.service;

import android.net.Uri;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.util.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class AlphaWalletNotificationService
{
    private static final String BASE_API_URL = BuildConfig.NOTIFICATION_API_BASE_URL;
    public static final String SUBSCRIBE_API_PATH = BASE_API_URL + "/subscriptions";
    public static final String UNSUBSCRIBE_API_PATH = BASE_API_URL + "/subscriptions";

    public static AlphaWalletNotificationService instance;
    private final OkHttpClient httpClient;

    public AlphaWalletNotificationService()
    {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(C.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(C.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    public AlphaWalletNotificationService(OkHttpClient httpClient)
    {
        this.httpClient = httpClient;
    }

    public static AlphaWalletNotificationService get()
    {
        if (instance == null)
        {
            instance = new AlphaWalletNotificationService();
        }
        return instance;
    }

    public static AlphaWalletNotificationService get(OkHttpClient httpClient)
    {
        if (instance == null)
        {
            instance = new AlphaWalletNotificationService(httpClient);
        }
        return instance;
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

    private Request buildDeleteRequest(String api)
    {
        Request.Builder requestB = new Request.Builder()
            .url(api)
            .header("User-Agent", "Chrome/74.0.3729.169")
            .addHeader("Content-Type", "application/json")
            .delete();
        return requestB.build();
    }

    private String executeRequest(Request request)
    {
        try (okhttp3.Response response = httpClient.newCall(request).execute())
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

    public String subscribe(String address, String chainId)
    {
        RequestBody body = null;
        try
        {
            JSONObject json = new JSONObject();
            json.put("wallet", address);
            json.put("chainId", Long.parseLong(chainId));
            body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        }
        catch (JSONException e)
        {
            Timber.e(e);
        }

        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(SUBSCRIBE_API_PATH);
        String url = builder.build().toString();

        return executeRequest(buildPostRequest(url, body));
    }

    public String unsubscribe(String address, String chainId)
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(UNSUBSCRIBE_API_PATH)
            .appendEncodedPath(address)
            .appendEncodedPath(chainId);
        String url = builder.build().toString();

        return executeRequest(buildDeleteRequest(url));
    }
}
