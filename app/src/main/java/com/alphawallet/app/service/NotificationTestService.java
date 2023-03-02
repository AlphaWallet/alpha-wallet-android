package com.alphawallet.app.service;

import android.net.Uri;

import com.alphawallet.app.util.JsonUtils;

import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class NotificationTestService
{
    private static final String BASE_API_URL = "https://alphawallet.com"; // TODO:

    private static final String API_PATH = "/path/to/api"; // TODO:

    public static NotificationTestService instance;
    private final OkHttpClient httpClient;

    public NotificationTestService(OkHttpClient httpClient)
    {
        this.httpClient = httpClient;
    }

    public static NotificationTestService get(OkHttpClient httpClient)
    {
        if (instance == null)
        {
            instance = new NotificationTestService(httpClient);
        }
        return instance;
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

    public String fetchNotifications(String walletAddress) // TODO: Modify parameters
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(BASE_API_URL + API_PATH)
//            .appendQueryParameter("key", value1) // TODO: Add query params here
            .appendQueryParameter("address", walletAddress);
        String url = builder.build().toString();
        return executeRequest(url);
    }
}
