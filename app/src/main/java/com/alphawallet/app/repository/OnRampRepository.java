package com.alphawallet.app.repository;

import android.content.Context;
import android.net.Uri;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.OnRampContract;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.util.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

public class OnRampRepository implements OnRampRepositoryType {
    public static final String DEFAULT_PROVIDER = "Ramp";
    private static final String RAMP = "ramp";
    private static final String ONRAMP_CONTRACTS_FILE_NAME = "onramp_contracts.json";

    static
    {
        System.loadLibrary("keys");
    }

    private final Context context;

    public OnRampRepository(Context context)
    {
        this.context = context;
    }

    public static native String getRampKey();

    @Override
    public String getDefaultProvider()
    {
        return DEFAULT_PROVIDER;
    }

    @Override
    public String getUri(String address, Token token)
    {
        OnRampContract contract = getContract(token);
        switch (contract.getProvider().toLowerCase())
        {
            case RAMP:
            default:
                return buildRampUri(address, contract.getSymbol()).toString();
        }
    }

    @Override
    public OnRampContract getContract(Token token)
    {
        Map<String, OnRampContract> contractMap = getKnownContracts();
        OnRampContract contract = contractMap.get(token.getAddress().toLowerCase());
        if (contract != null) return contract;
        else return new OnRampContract();
    }

    @Override
    public boolean isInKnownContractsList(Token token)
    {
        Map<String, OnRampContract> contractMap = getKnownContracts();
        OnRampContract contract = contractMap.get(token.getAddress().toLowerCase());
        return contract != null;
    }

    private Map<String, OnRampContract> getKnownContracts()
    {
        return new Gson().fromJson(Utils.loadJSONFromAsset(context, ONRAMP_CONTRACTS_FILE_NAME),
                new TypeToken<Map<String, OnRampContract>>() {
                }.getType());
    }

    private static Uri buildRampUri(String address, String symbol)
    {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority("buy.ramp.network")
                .appendQueryParameter("hostApiKey", getRampKey())
                .appendQueryParameter("hostLogoUrl", C.ALPHAWALLET_LOGO_URI)
                .appendQueryParameter("hostAppName", "AlphaWallet")
                .appendQueryParameter("userAddress", address);

        if (!symbol.isEmpty())
        {
            builder.appendQueryParameter("swapAsset", symbol);
        }

        return builder.build();
    }
}
