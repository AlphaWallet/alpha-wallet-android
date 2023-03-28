package com.alphawallet.app.repository;

import static android.text.format.DateUtils.DAY_IN_MILLIS;

import android.content.Context;
import android.util.Pair;

import com.alphawallet.app.entity.TokensMapping;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ContractAddress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class TokensMappingRepository
{
    private static final String TOKENS_JSON_FILENAME = "tokens.json";
    private final Context context;

    public TokensMappingRepository(TokenLocalSource localSource, Context context)
    {
        this.context = context;

        //find when we last updated
        if (localSource.getLastMappingsUpdate() < (System.currentTimeMillis() - DAY_IN_MILLIS))
        {
            fetchTokenList() //Fetch the token list from assets and store into a pair of mappings, 1st mapping is the derivative tokens with a pointer to their base token. Second is the base token and grouping
                .map(localSource::storeTokensMapping) //Store these mappings into an optimal database. Note the mappings are not used again, we instead use the realm database
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
                .isDisposed();
        }
    }

    private Single<Pair<Map<String, ContractAddress>, Map<String, TokenGroup>>> fetchTokenList()
    {
        return Single.fromCallable(() -> {
            String mapping = Utils.loadJSONFromAsset(context, TOKENS_JSON_FILENAME);

            Map<String, ContractAddress> sourceMap = new HashMap<>();
            Map<String, TokenGroup> baseMappings = new HashMap<>();

            TokensMapping[] tokensMapping = new Gson().fromJson(mapping, new TypeToken<TokensMapping[]>()
            {
            }.getType());

            if (tokensMapping != null)
            {
                for (TokensMapping thisMapping : tokensMapping)
                {
                    ContractAddress baseAddress = thisMapping.getContracts().get(0);
                    baseMappings.put(baseAddress.getAddressKey(), thisMapping.getGroup()); //insert base mapping (eg DAI on mainnet) along with token type
                    for (int i = 1; i < thisMapping.getContracts().size(); i++)
                    {
                        sourceMap.put(thisMapping.getContracts().get(i).getAddressKey(), baseAddress); //insert mirrored token with pointer to base token (eg DAI on Arbitrum).
                    }
                }
            }

            return new Pair<>(sourceMap, baseMappings);
        });
    }
}
