package com.alphawallet.app.service;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.Result;
import com.alphawallet.app.util.JsonUtils;
import com.google.gson.Gson;

import org.web3j.utils.Numeric;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class SignatureLookupService
{
    private static final String BASE_API_URL = "https://www.4byte.directory/api/v1/signatures/?hex_signature=";
    private final OkHttpClient httpClient;

    public SignatureLookupService()
    {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(C.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    public Single<String> getFunctionName(String payload)
    {
        return Single.fromCallable(() -> getTextSignature(executeRequest(buildRequest(payload))));
    }

    public String getTextSignature(String response)
    {
        Result result = new Gson().fromJson(response, Result.class);
        if (result != null)
        {
            return result.getFirst().text_signature;
        }

        return "";
    }

    private String getFirstFourBytes(String payload)
    {
        return (Numeric.prependHexPrefix(payload)).substring(0, 10);
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

    private Request buildRequest(String payload)
    {
        Request.Builder builder = new Request.Builder()
            .url(BASE_API_URL + getFirstFourBytes(payload))
            .header("User-Agent", "Chrome/74.0.3729.169")
            .addHeader("Content-Type", "application/json")
            .get();

        return builder.build();
    }
}
