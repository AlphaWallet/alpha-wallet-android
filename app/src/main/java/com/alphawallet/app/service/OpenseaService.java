package com.alphawallet.app.service;

import android.content.Context;
import android.text.TextUtils;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InterruptedIOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by James on 2/10/2018.
 * Stormbird in Singapore
 */

public class OpenseaService {
    private static OkHttpClient httpClient;
    private static Map<String, Long> balanceAccess = new ConcurrentHashMap<>();
    private final Context context;
    private static final int PAGE_SIZE = 50;
    private final Map<String, String> imageUrls = new HashMap<>();
    private final List<Integer> storedImagesForChain = new ArrayList<>();

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

    public Single<Token[]> getTokens(String address, int networkId, String networkName, TokensService tokensService)
    {
        return Single.fromCallable(() -> {
            int receivedTokens;
            int offset = 0;

            Map<String, Token> foundTokens = new HashMap<>();

            do
            {
                String jsonData = fetchTokensFromOpensea(address, networkId, offset);
                if (!verifyData(jsonData)) return foundTokens.values().toArray(new Token[0]); //on error return results found so far
                JSONObject result = new JSONObject(jsonData);
                JSONArray assets = result.getJSONArray("assets");
                receivedTokens = assets.length();
                offset += assets.length();

                //process this page of results
                processOpenseaTokens(foundTokens, assets, address, networkId, networkName, tokensService);
            }
            while (receivedTokens == PAGE_SIZE); //keep fetching until last page

            //now write the contract images
            if (!storedImagesForChain.contains(networkId))
            {
                storedImagesForChain.add(networkId);
                for (String keyAddr : imageUrls.keySet())
                {
                    tokensService.addTokenImageUrl(networkId, keyAddr, imageUrls.get(keyAddr));
                }
                imageUrls.clear();
            }

            return foundTokens.values().toArray(new Token[0]);
        });
    }

    private void processOpenseaTokens(Map<String, Token> foundTokens, JSONArray assets, String address,
                                      int networkId, String networkName, TokensService tokensService) throws Exception
    {
        final Map<String, Map<BigInteger, NFTAsset>> assetList = new HashMap<>();
        TokenFactory tf = new TokenFactory();
        for (int i = 0; i < assets.length(); i++)
        {
            NFTAsset asset = new NFTAsset(assets.getJSONObject(i).toString());
            AssetContract assetContract = assets.getJSONObject(i).has("asset_contract") ?
                    new Gson().fromJson(assets.getJSONObject(i).getString("asset_contract"), AssetContract.class)
                    : null;

            if (assetContract != null && !TextUtils.isEmpty(assetContract.getSchemaName())
                && assetContract.getSchemaName().equalsIgnoreCase("ERC721"))
            {
                BigInteger tokenId = assets.getJSONObject(i).has("token_id") ? new BigInteger(assets.getJSONObject(i).getString("token_id")) : null;
                if (tokenId == null) continue;

                addAssetImageToHashMap(imageUrls, assetContract, networkId);
                Token token = foundTokens.get(assetContract.getAddress());
                if (token == null)
                {
                    TokenInfo tInfo;
                    ContractType type;
                    long lastCheckTime = 0;
                    Token checkToken = tokensService.getToken(networkId, assetContract.getAddress());
                    if (checkToken != null && (checkToken.isERC721() || checkToken.isERC721Ticket()))
                    {
                        assetList.put(assetContract.getAddress(), checkToken.getTokenAssets());
                        tInfo = checkToken.tokenInfo;
                        type = checkToken.getInterfaceSpec();
                        lastCheckTime = checkToken.lastTxTime;
                    }
                    else //if we haven't seen the contract before, or it was previously logged as something other than a ERC721 variant then specify undetermined flag
                    {
                        tInfo = new TokenInfo(assetContract.getAddress(), assetContract.getName(), assetContract.getSymbol(), 0, true, networkId);
                        type = ContractType.ERC721_UNDETERMINED;
                    }

                    token = tf.createToken(tInfo, type, networkName);
                    token.setTokenWallet(address);
                    token.lastTxTime = lastCheckTime;
                    foundTokens.put(assetContract.getAddress(), token);
                }
                asset.updateAsset(tokenId, assetList.get(token.getAddress()));
                token.addAssetToTokenBalanceAssets(tokenId, asset);
            }
        }
    }

    private void addAssetImageToHashMap(Map<String, String> imageUrls, AssetContract assetContract, int networkId)
    {
        if (storedImagesForChain.contains(networkId)) return; //already recorded the image

        String address = assetContract.getAddress();
        if (!imageUrls.containsKey(address) && !TextUtils.isEmpty(assetContract.getImageUrl()))
        {
            imageUrls.put(address, assetContract.getImageUrl());
        }
    }

    private boolean verifyData(String jsonData)
    {
        return jsonData != null && jsonData.length() >= 10 && jsonData.contains("assets"); //validate return from API
    }

    private String fetchTokensFromOpensea(String address, int networkId, int offset)
    {
        String jsonResult = "{\"noresult\":[]}";
        String apiBase;
        switch (networkId)
        {
            case 1:
                apiBase = "https://api.opensea.io";
                break;
            case 4:
                apiBase = "https://rinkeby-api.opensea.io";
                break;
            default:
                return jsonResult;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(apiBase);
        sb.append("/api/v1/assets/?owner=");
        sb.append(address);
        sb.append("&limit=" + PAGE_SIZE);
        sb.append("&offset=");
        sb.append(offset);

        try
        {
            Request request = new Request.Builder()
                    .url(sb.toString())
                    .get()
                    .build();

            okhttp3.Response response = httpClient.newCall(request).execute();
            jsonResult = response.body().string();
            balanceAccess.put(address, System.currentTimeMillis());
        }
        catch (InterruptedIOException e)
        {
            //If user switches account or network during a fetch
            //this exception is going to be thrown because we're terminating the API call
            //Don't display error
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return jsonResult;
    }
}
