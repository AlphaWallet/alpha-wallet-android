package com.alphawallet.app.service;

import android.net.Uri;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.util.JsonUtils;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class AlphaWalletNotificationService
{
    private static final String BASE_API_URL = BuildConfig.NOTIFICATION_API_BASE_URL;
    public static final String SUBSCRIPTIONS_API_PATH = BASE_API_URL + "/subscriptions";
    private final OkHttpClient httpClient;
    private final WalletRepositoryType walletRepository;
    private Disposable disposable;

    public AlphaWalletNotificationService(WalletRepositoryType walletRepository)
    {
        this.walletRepository = walletRepository;

        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(C.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
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

    private RequestBody buildSubscribeRequest(String address, String chainId)
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

        return body;
    }

    public Single<String> subscribe(long chainId)
    {
        return walletRepository.getDefaultWallet()
            .flatMap(wallet -> doSubscribe(wallet.address, chainId));
    }

    private Single<String> doSubscribe(String address, long chainId)
    {
        subscribeToFirebaseTopic(address, chainId);

        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(SUBSCRIPTIONS_API_PATH);
        String url = builder.build().toString();

        return Single.fromCallable(() ->
            executeRequest(buildPostRequest(url, buildSubscribeRequest(address, String.valueOf(chainId)))));
    }

    public Single<String> unsubscribe(long chainId)
    {
        return walletRepository.getDefaultWallet()
            .flatMap(wallet -> doUnsubscribe(wallet.address, chainId));
    }

    // TODO: [Notifications] Delete when unsubscribe is implemented
    public void unsubscribeToTopic(long chainId)
    {
        disposable = walletRepository.getDefaultWallet()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(wallet -> unsubscribeToFirebaseTopic(wallet.address, chainId));
    }

    public Single<String> doUnsubscribe(String address, long chainId)
    {
        unsubscribeToFirebaseTopic(address, chainId);

        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(SUBSCRIPTIONS_API_PATH)
            .appendEncodedPath(address)
            .appendEncodedPath(String.valueOf(chainId));
        String url = builder.build().toString();

        return Single.fromCallable(() -> executeRequest(buildDeleteRequest(url)));
    }

    private void subscribeToFirebaseTopic(String address, long chainId)
    {
        String topic = address + "-" + chainId;
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener(task -> {
                String msg = "Subscribed to " + topic;
                if (!task.isSuccessful())
                {
                    msg = "Subscribe failed";
                }
                Timber.d(msg);
            });
    }

    public void unsubscribeToFirebaseTopic(String address, long chainId)
    {
        String topic = address + "-" + chainId;
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener(task -> {
                String msg = "Unsubscribed to " + topic;
                if (!task.isSuccessful())
                {
                    msg = "Unsubscribe failed";
                }
                Timber.d(msg);
            });
    }
}
