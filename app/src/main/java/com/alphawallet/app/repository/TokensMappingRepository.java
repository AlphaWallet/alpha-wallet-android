package com.alphawallet.app.repository;

import static android.text.format.DateUtils.DAY_IN_MILLIS;

import android.util.Pair;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.TokensMapping;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.token.entity.ContractAddress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import timber.log.Timber;

public class TokensMappingRepository {

    private final OkHttpClient httpClient;
    private final TokenLocalSource source;

    public TokensMappingRepository(TokenLocalSource localSource) {
        this.httpClient = new OkHttpClient();
        this.source = localSource;

        //find when we last updated
        if (source.getLastMappingsUpdate() < (System.currentTimeMillis() - DAY_IN_MILLIS))
        {
            fetchTokenList()
                    .map(source::storeTokensMapping)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe()
                    .isDisposed();
        }
    }

    public Single<Pair<Map<String, ContractAddress>, Map<String, TokenGroup>>> fetchTokenList()
    {
        return Single.fromCallable(() -> {

            Request request = new Request.Builder()
                    .url("https://raw.githubusercontent.com/AlphaWallet/TokenTest/master/tokens.json")
                    .get()
                    .build();

            Map<String, ContractAddress> sourceMap = new HashMap<>();
            Map<String, TokenGroup> baseMappings = new HashMap<>();

            try (okhttp3.Response response = httpClient.newCall(request)
                    .execute()) {
                TokensMapping[] tokensMapping = new Gson().fromJson(response.body().string(), new TypeToken<TokensMapping[]>() {}.getType());

                for (TokensMapping thisMapping : tokensMapping)
                {
                    if (thisMapping.getContracts().size() < 2) continue;
                    ContractAddress baseAddress = thisMapping.getContracts().get(0);
                    for (int i = 1; i < thisMapping.getContracts().size(); i++)
                    {
                        sourceMap.put(thisMapping.getContracts().get(i).getAddressKey(), baseAddress);
                    }
                    baseMappings.put(baseAddress.getAddressKey(), thisMapping.getGroup());
                }
            } catch (Exception e) {
                Timber.e(e);
            }

            return new Pair<>(sourceMap, baseMappings);
        });
    }

    public ContractAddress getBaseToken(long chainId, String address)
    {
        return source.getBaseToken(chainId, address);
    }
}
