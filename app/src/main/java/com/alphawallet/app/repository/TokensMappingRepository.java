package com.alphawallet.app.repository;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.TokensMapping;
import com.alphawallet.app.repository.entity.RealmTokenMapping;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class TokensMappingRepository {

    private final OkHttpClient httpClient;
    private final TokenLocalSource source;

    private static List<RealmTokenMapping> tokensMapping = new ArrayList<>();

    private final Disposable realmDisposable;

    public TokensMappingRepository(TokenLocalSource localSource) {
        this.httpClient = new OkHttpClient();
        this.source = localSource;
        this.realmDisposable = source.getTokensMapping()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((list) -> tokensMapping = list);
    }

    public void dispose() {
        if (realmDisposable != null && !realmDisposable.isDisposed()) realmDisposable.dispose();
    }

    public Completable getTokensMapping() {
        return Completable.fromAction(() -> {

            // TODO: change url to the right tokens.json, atm it's empty
            Request request = new Request.Builder()
                    .url("https://raw.githubusercontent.com/mpaschenko/mpaschenko.github.io/master/tokens.json")
                    .get()
                    .build();

            try (okhttp3.Response response = httpClient.newCall(request)
                    .execute()) {
                TokensMapping[] tokensMapping = new Gson().fromJson(response.body().string(), new TypeToken<TokensMapping[]>() {
                }.getType());

                source.storeTokensMapping(toRealm(tokensMapping));
            } catch (Exception e) {
                if (BuildConfig.DEBUG) e.printStackTrace();
            }
        });
    }

    private List<RealmTokenMapping> toRealm(TokensMapping[] mappings) {
        ArrayList<RealmTokenMapping> realmTokenMappings = new ArrayList<>();
        for (TokensMapping tm : mappings) {
            for (TokensMapping.Contract tmc : tm.getContracts()) {
                realmTokenMappings.add(new RealmTokenMapping(tmc.getAddress(), tmc.getChainId(), tm.getGroup()));
            }
        }

        tokensMapping = realmTokenMappings;

        return realmTokenMappings;
    }

    public static String getGroup(long chainId, String address) {
        for (RealmTokenMapping rtm : tokensMapping) {
            if (rtm.address.equalsIgnoreCase(address) && rtm.chainId == chainId) {
                return rtm.group;
            }
        }

        return "Assets";
    }
}
