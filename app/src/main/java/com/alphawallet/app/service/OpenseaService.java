package com.alphawallet.app.service;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import io.reactivex.Single;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.ERC721Token;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenInfo;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.opensea.OpenseaServiceError;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by James on 2/10/2018.
 * Stormbird in Singapore
 */

public class OpenseaService {
    private static OkHttpClient httpClient;
    private static Map<String, Long> balanceAccess = new ConcurrentHashMap<>();
    private Context context;

    //TODO: remove old files not accessed for some time
    //      On service creation, check files for old files and delete

    public OpenseaService(Context ctx) {
        context = ctx;
        balanceAccess.clear();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public Single<Token[]> getTokens(String address, int networkId, String networkName) {
        return queryBalance(address, networkId)
                .map(json -> gotOpenseaTokens(json, address, networkId, networkName));
    }

    private Token[] gotOpenseaTokens(JSONObject object, String address, int networkId, String networkName) throws Exception
    {
        Map<String, Token> foundTokens = new HashMap<>();

        if (!object.has("assets"))
        {
            throw new OpenseaServiceError("Opensea API Comms failure"); //if we didn't receive any sensible result then
        }
        JSONArray assets = object.getJSONArray("assets");

        for (int i = 0; i < assets.length(); i++)
        {
            Asset asset = new Gson().fromJson(assets.getJSONObject(i).toString(), Asset.class);
            if (asset != null && (asset.getAssetContract().getSchemaName() == null
                                || asset.getAssetContract().getSchemaName().length() == 0
                                || asset.getAssetContract().getSchemaName().equalsIgnoreCase("ERC721"))) //filter ERC721
            {
                Token token = foundTokens.get(asset.getAssetContract().getAddress());
                if (token == null)
                {
                    String tokenName = asset.getAssetContract().getName();
                    String tokenSymbol = asset.getAssetContract().getSymbol();

                    TokenInfo tInfo = new TokenInfo(asset.getAssetContract().getAddress(), tokenName, tokenSymbol, 0, true, networkId);
                    token = new ERC721Token(tInfo, null, System.currentTimeMillis(), networkName, ContractType.ERC721);
                    token.setTokenWallet(address);
                    foundTokens.put(asset.getAssetContract().getAddress(), token);
                }

                ((ERC721Token) token).tokenBalance.add(asset);
            }
        }

        return foundTokens.values().toArray(new Token[0]);
    }

    private Single<JSONObject> queryBalance(String address, int networkId)
    {
        return Single.fromCallable(() -> {
            String apiBase = "";
            // if no result we should throw an error - this distinguishes a comms error from a zero balance
            JSONObject result = new JSONObject("{\"noresult\":[]}");
            switch (networkId)
            {
                case 1:
                    apiBase = "https://api.opensea.io";
                    break;
                case 4:
                    apiBase = "https://rinkeby-api.opensea.io";
                    break;
                default:
                    return result;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(apiBase);
            sb.append("/api/v1/assets/?owner=");
            sb.append(address);
            sb.append("&order_direction=asc");

            try {
                if (balanceAccess.containsKey(address)) {
                    long lastAccess = balanceAccess.get(address);
                    if (lastAccess > 0 && (System.currentTimeMillis() - lastAccess) < 1000 * 30) {
                        Log.d("OPENSEA", "Polling Opensea very frequently: " + (System.currentTimeMillis() - lastAccess));
                    }
                }

                Request request = new Request.Builder()
                        .url(sb.toString())
                        .get()
                        .build();

                okhttp3.Response response = httpClient.newCall(request).execute();
                String jsonResult = response.body().string();
                balanceAccess.put(address, System.currentTimeMillis());

                if (jsonResult != null && jsonResult.length() > 10) {
                    result = new JSONObject(jsonResult);
                }
            } catch (java.net.SocketTimeoutException e) {
                Log.i("Opensea", "Socket timeout");
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        });
    }
}
