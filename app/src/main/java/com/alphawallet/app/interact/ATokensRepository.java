package com.alphawallet.app.interact;

import android.util.Log;

import com.alphawallet.app.entity.CoinGeckoTicker;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.MessagePair;
import com.alphawallet.app.entity.SignaturePair;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.repository.entity.RealmAToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public
class ATokensRepository {

    private final OkHttpClient httpClient;
    private static List<String> aTokensList = new ArrayList<>();
    private final TokenLocalSource source;

    private final Disposable realmDisposable;

    public ATokensRepository(TokenLocalSource source) {
        httpClient = new OkHttpClient();
        this.source = source;
        realmDisposable = source.getATokenAddresses()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((list) -> aTokensList = list);
    }

    public void dispose() {
        if (realmDisposable != null && !realmDisposable.isDisposed()) realmDisposable.dispose();
    }

    public static boolean isAToken(String address) {
        return aTokensList.contains(address);
    }

    public Completable getTokensList() {
        return Completable.fromAction(() -> {
            Request request = new Request.Builder()
                    .url("https://aave.github.io/aave-addresses/mainnet.json")
                    .get()
                    .build();

            okhttp3.Response response = httpClient.newCall(request)
                    .execute();

            try {
                List<String> res = new ArrayList<>();
                JSONObject data = new JSONObject(response.body().string());
                JSONArray tokens = (JSONArray) data.get("proto");

                for (int i = 0; i < tokens.length(); i++)
                {
                    JSONObject obj = tokens.getJSONObject(i);
                    String address = obj.getString("aTokenAddress");
                    res.add(address.toLowerCase());
                }

                aTokensList = res;
                source.storeATokenAddresses(aTokensList);

            } catch (JSONException ex) {
            }
        });
    }
}
