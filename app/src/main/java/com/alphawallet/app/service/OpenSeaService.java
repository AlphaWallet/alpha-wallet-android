package com.alphawallet.app.service;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LongSparseArray;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InterruptedIOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import timber.log.Timber;

/**
 * Created by James on 2/10/2018.
 * Stormbird in Singapore
 */

public class OpenSeaService
{
    private static OkHttpClient httpClient;
    private static final int PAGE_SIZE = 50;
    private final Map<String, String> imageUrls = new HashMap<>();
    private static final TokenFactory tf = new TokenFactory();
    private final LongSparseArray<Long> networkCheckTimes = new LongSparseArray<>();
    private final LongSparseArray<Integer> pageOffsets = new LongSparseArray<>();

    static {
        System.loadLibrary("keys");
    }

    public static native String getOpenSeaKey();

    public OpenSeaService() {
        pageOffsets.clear();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public Single<Token[]> getTokens(String address, long networkId, String networkName, TokensService tokensService)
    {
        return Single.fromCallable(() -> {
            int receivedTokens;
            int currentPage = 0;
            Map<String, Token> foundTokens = new HashMap<>();

            long currentTime = System.currentTimeMillis();
            if (!canCheckChain(networkId)) return new Token[0];
            networkCheckTimes.put(networkId, currentTime);

            int pageOffset = pageOffsets.get(networkId, 0);


            Timber.tag("CHECKOPENSEA").d("Fetch from opensea : %s", networkName);

            do
            {
                String jsonData = fetchTokensFromOpensea(address, networkId, pageOffset);
                if (!verifyData(jsonData)) return foundTokens.values().toArray(new Token[0]); //on error return results found so far
                JSONObject result = new JSONObject(jsonData);
                JSONArray assets;
                if (result.has("assets")) assets = result.getJSONArray("assets");
                else assets = result.getJSONArray("results");
                receivedTokens = assets.length();
                pageOffset += assets.length();

                //process this page of results
                processOpenseaTokens(foundTokens, assets, address, networkId, networkName, tokensService);
                currentPage++;
            }
            while (receivedTokens == PAGE_SIZE && currentPage <= 3); //fetch 4 pages for each loop

            if (receivedTokens < PAGE_SIZE)
            {
                Timber.d("Reset OpenSeaAPI reads at: %s", pageOffset);
                pageOffsets.put(networkId, 0);
            }
            else
            {
                pageOffsets.put(networkId, pageOffset);
                networkCheckTimes.put(networkId, currentTime - 55*DateUtils.SECOND_IN_MILLIS); //do another read within 5 seconds
            }

            //now write the contract images
            for (Map.Entry<String, String> entry : imageUrls.entrySet())
            {
                tokensService.addTokenImageUrl(networkId, entry.getKey(), entry.getValue());
            }
            imageUrls.clear();

            return foundTokens.values().toArray(new Token[0]);
        });
    }

    private void processOpenseaTokens(Map<String, Token> foundTokens, JSONArray assets, String address,
                                      long networkId, String networkName, TokensService tokensService) throws Exception
    {
        final Map<String, Map<BigInteger, NFTAsset>> assetList = new HashMap<>();

        for (int i = 0; i < assets.length(); i++)
        {
            JSONObject assetJSON = assets.getJSONObject(i);
            AssetContract assetContract = assetJSON.has("asset_contract") ?
                    new Gson().fromJson(assetJSON.getString("asset_contract"), AssetContract.class)
                    : null;

            if (assetContract != null && !TextUtils.isEmpty(assetContract.getSchemaName()))
            {
                switch (assetContract.getSchemaName())
                {
                    case "ERC721":
                        handleERC721(assetList, assetJSON, networkId, foundTokens, tokensService,
                                networkName, address);
                        break;
                    case "ERC1155":
                        handleERC1155(assetList, assetJSON, networkId, foundTokens, tokensService,
                                networkName, address);
                        break;
                }
            }
        }
    }

    private void handleERC721(Map<String, Map<BigInteger, NFTAsset>> assetList, JSONObject assetJSON, long networkId,
                              Map<String, Token> foundTokens, TokensService svs, String networkName, String address) throws Exception
    {
        NFTAsset asset = new NFTAsset(assetJSON.toString());
        AssetContract assetContract = new Gson().fromJson(assetJSON.getString("asset_contract"), AssetContract.class);

        BigInteger tokenId = assetJSON.has("token_id") ? new BigInteger(assetJSON.getString("token_id")) : null;
        if (tokenId == null) return;

        addAssetImageToHashMap(imageUrls, assetContract, networkId);
        Token token = foundTokens.get(assetContract.getAddress());
        if (token == null)
        {
            TokenInfo tInfo;
            ContractType type;
            long lastCheckTime = 0;
            Token checkToken = svs.getToken(networkId, assetContract.getAddress());
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

    private void handleERC1155(Map<String, Map<BigInteger, NFTAsset>> assetList, JSONObject assetJSON, long networkId,
                              Map<String, Token> foundTokens, TokensService svs, String networkName, String address) throws Exception
    {
        NFTAsset asset = new NFTAsset(assetJSON.toString());
        AssetContract assetContract = new Gson().fromJson(assetJSON.getString("asset_contract"), AssetContract.class);

        BigInteger tokenId = assetJSON.has("token_id") ? new BigInteger(assetJSON.getString("token_id")) : null;
        if (tokenId == null) return;

        addAssetImageToHashMap(imageUrls, assetContract, networkId);
        Token token = foundTokens.get(assetContract.getAddress());
        if (token == null)
        {
            TokenInfo tInfo;
            ContractType type;
            long lastCheckTime = 0;
            Token checkToken = svs.getToken(networkId, assetContract.getAddress());
            if (checkToken != null && checkToken.getInterfaceSpec() == ContractType.ERC1155)
            {
                assetList.put(assetContract.getAddress(), checkToken.getTokenAssets());
                tInfo = checkToken.tokenInfo;
                type = checkToken.getInterfaceSpec();
                lastCheckTime = checkToken.lastTxTime;
            }
            else
            {
                tInfo = new TokenInfo(assetContract.getAddress(), assetContract.getName(), assetContract.getSymbol(), 0, true, networkId);
                type = ContractType.ERC1155;
            }

            token = tf.createToken(tInfo, type, networkName);
            token.setTokenWallet(address);
            token.lastTxTime = lastCheckTime;
            token.setAssetContract(assetContract);
            foundTokens.put(assetContract.getAddress(), token);
        }
        asset.updateAsset(tokenId, assetList.get(token.getAddress()));
        token.addAssetToTokenBalanceAssets(tokenId, asset);
    }

    private void addAssetImageToHashMap(Map<String, String> imageUrls, AssetContract assetContract, long networkId)
    {
        //if (storedImagesForChain.contains(networkId)) return; //already recorded the image

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

    public static final Map<Long, String> apiMap = new HashMap<Long, String>(){
        {
            put(EthereumNetworkBase.MAINNET_ID, "https://api.opensea.io/api/v1/assets/?owner=");
            put(EthereumNetworkBase.RINKEBY_ID, "https://rinkeby-api.opensea.io/api/v1/assets/?owner=");
            put(EthereumNetworkBase.MATIC_ID, "https://api.opensea.io/api/v2/assets/matic?owner_address=");
        }
    };

    private String fetchTokensFromOpensea(String address, long networkId, int offset)
    {
        String jsonResult = "{\"noresult\":[]}";
        String apiBase = apiMap.get(networkId);
        if (apiBase == null) return jsonResult;

        StringBuilder sb = new StringBuilder();
        sb.append(apiBase);
        sb.append(address);
        sb.append("&limit=" + PAGE_SIZE);
        sb.append("&offset=");
        sb.append(offset);

        Request.Builder requestB = new Request.Builder()
                .url(sb.toString())
                .header("User-Agent", "Chrome/74.0.3729.169")
                .method("GET", null)
                .addHeader("Content-Type", "application/json");

        String apiKey = getOpenSeaKey();
        if (!TextUtils.isEmpty(apiKey) && !apiKey.equals("..."))
        {
            requestB.addHeader("X-API-KEY", apiKey);
        }

        Request request = requestB.build();

        try (okhttp3.Response response = httpClient.newCall(request).execute())
        {
            jsonResult = response.body().string();
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

    public void resetOffsetRead(List<Long> networkFilter)
    {
        long offsetTime = System.currentTimeMillis() - 57 * DateUtils.SECOND_IN_MILLIS;
        for (long networkId : networkFilter)
        {
            if (com.alphawallet.app.repository.EthereumNetworkBase.hasOpenseaAPI(networkId))
            {
                networkCheckTimes.put(networkId, offsetTime);
                offsetTime += 10 * DateUtils.SECOND_IN_MILLIS;
            }
        }
        pageOffsets.clear();
    }

    public boolean canCheckChain(long networkId)
    {
        long lastCheckTime = networkCheckTimes.get(networkId, 0L);
        return System.currentTimeMillis() > (lastCheckTime + DateUtils.MINUTE_IN_MILLIS);
    }
}
