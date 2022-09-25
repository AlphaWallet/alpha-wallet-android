package com.alphawallet.app.repository;

import android.content.Context;

import com.alphawallet.app.entity.lifi.SwapProvider;
import com.alphawallet.app.util.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class SwapRepository implements SwapRepositoryType
{
    public static final String FETCH_CHAINS = "https://li.quest/v1/chains";
    public static final String FETCH_TOKENS = "https://li.quest/v1/connections";
    public static final String FETCH_QUOTE = "https://li.quest/v1/quote";
    public static final String FETCH_TOOLS = "https://li.quest/v1/tools";
    public static final String FETCH_ROUTES = "https://li.quest/v1/advanced/routes";
    private static final String SWAP_PROVIDERS_FILENAME = "swap_providers_list.json";

    private final Context context;

    public SwapRepository(Context context)
    {
        this.context = context;
    }

    @Override
    public List<SwapProvider> getProviders()
    {
        return new Gson().fromJson(Utils.loadJSONFromAsset(context, SWAP_PROVIDERS_FILENAME),
                new TypeToken<List<SwapProvider>>()
                {
                }.getType());
    }
}
