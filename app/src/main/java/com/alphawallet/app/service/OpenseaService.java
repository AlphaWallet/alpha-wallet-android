package com.alphawallet.app.service;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alphawallet.app.entity.tokens.TokenFactory;
import com.google.gson.Gson;
import io.reactivex.Single;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.opensea.OpenseaServiceError;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final Context context;
    private final TokensService tokensService;
    private static final int PAGE_SIZE = 40;

    //TODO: remove old files not accessed for some time
    //      On service creation, check files for old files and delete

    public OpenseaService(Context ctx, TokensService tService) {
        context = ctx;
        tokensService = tService;
        balanceAccess.clear();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public Single<Token[]> getTokens(String address, int networkId, String networkName) {
        return queryBalance(address, networkId, networkName);
    }

    private Single<Token[]> queryBalance(String address, int networkId, String networkName)
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
                offset++;

                //process this page of results
                processOpenseaTokens(foundTokens, assets, address, networkId, networkName);
            }
            while (receivedTokens == PAGE_SIZE); //keep fetching until last page

            return foundTokens.values().toArray(new Token[0]);
        });
    }

    private void processOpenseaTokens(Map<String, Token> foundTokens, JSONArray assets, String address, int networkId, String networkName) throws Exception
    {
        TokenFactory tf = new TokenFactory();

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
                    TokenInfo tInfo;
                    ContractType type;
                    Token checkToken = tokensService.getToken(networkId, asset.getAssetContract().getAddress());
                    if (checkToken != null && checkClassification(checkToken, asset) && (checkToken.isERC721() || checkToken.isERC721Ticket()))
                    {
                        tInfo = checkToken.tokenInfo;
                        type = checkToken.getInterfaceSpec();
                    }
                    else //if we haven't seen the contract before, or it was previously logged as something other than a ERC721 variant then specify undetermined flag
                    {
                        tInfo = new TokenInfo(asset.getAssetContract().getAddress(), asset.getAssetContract().getName(), asset.getAssetContract().getSymbol(), 0, true, networkId);
                        type = ContractType.ERC721_UNDETERMINED;
                    }

                    token = tf.createToken(tInfo, type, networkName);
                    token.setTokenWallet(address);
                    foundTokens.put(asset.getAssetContract().getAddress(), token);
                }
                token.addAssetToTokenBalanceAssets(asset);
            }
        }
    }

    /**
     * See if Token has been incorrectly classified as ERC721Ticket. Some ERC721 have no 'name' function and this is only retrieved from opensea
     * If name and symbol are empty then re-check the classification; most likely the token was misclassified
     */
    private boolean checkClassification(Token checkToken, Asset asset)
    {
        if (TextUtils.isEmpty(checkToken.tokenInfo.name + checkToken.tokenInfo.symbol)) return false; //empty token name; suspicious
        if (checkToken.isERC721() && asset.getTraits().size() == 0 && TextUtils.isEmpty(asset.getDescription())) return false; //ERC721 with no traits or description; could be erc721 ticket
        return !checkToken.isERC721Ticket() || asset.getTraits().size() == 0 || TextUtils.isEmpty(asset.getDescription()); //ERC721Ticket with traits or description
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
