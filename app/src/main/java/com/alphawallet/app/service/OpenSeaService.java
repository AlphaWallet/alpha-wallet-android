package com.alphawallet.app.service;

import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.SEPOLIA_TESTNET_ID;

import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.LongSparseArray;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.app.util.JsonUtils;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import timber.log.Timber;

/**
 * Created by James on 2/10/2018.
 * Stormbird in Singapore
 */

public class OpenSeaService
{
    private final OkHttpClient httpClient;
    private static final int PAGE_SIZE = 200;
    private final Map<String, String> imageUrls = new HashMap<>();
    private static final TokenFactory tf = new TokenFactory();
    private final LongSparseArray<Long> networkCheckTimes = new LongSparseArray<>();
    private final Map<Long, String> pageOffsets = new ConcurrentHashMap<>();

    private final Map<Long, String> API_CHAIN_MAP = Map.of(
            MAINNET_ID, "ethereum",
            KLAYTN_ID, "klaytn",
            POLYGON_TEST_ID, "mumbai",
            POLYGON_ID, "matic",
            OPTIMISTIC_MAIN_ID, "optimism",
            ARBITRUM_MAIN_ID, "arbitrum",
            SEPOLIA_TESTNET_ID, "sepolia",
            AVALANCHE_ID, "avalanche",
            BINANCE_MAIN_ID, "bsc"
    );

    public OpenSeaService()
    {
        pageOffsets.clear();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(C.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(C.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    private Request buildRequest(long networkId, String api)
    {
        Request.Builder requestB = new Request.Builder()
                .url(api)
                .method("GET", null);

        String apiKey = KeyProviderFactory.get().getOpenSeaKey();
        if (!TextUtils.isEmpty(apiKey)
                && !apiKey.equals("...")
                && com.alphawallet.app.repository.EthereumNetworkBase.hasRealValue(networkId)
        )
        {
            requestB.addHeader("X-API-KEY", apiKey);
        }

        return requestB.build();
    }

    private String executeRequest(long networkId, String api)
    {
        try (okhttp3.Response response = httpClient.newCall(buildRequest(networkId, api)).execute())
        {
            if (response.isSuccessful())
            {
                ResponseBody responseBody = response.body();
                if (responseBody != null)
                {
                    return responseBody.string();
                }
            }
            else
            {
                return JsonUtils.EMPTY_RESULT;
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return JsonUtils.EMPTY_RESULT;
    }

    public Single<Token[]> getTokens(String address,
                                     long networkId,
                                     String networkName,
                                     TokensService tokensService)
    {
        return Single.fromCallable(() ->
        {
            int receivedTokens;
            int currentPage = 0;
            Map<String, Token> foundTokens = new HashMap<>();

            long currentTime = System.currentTimeMillis();
            if (!canCheckChain(networkId)) return new Token[0];
            networkCheckTimes.put(networkId, currentTime);

            String pageCursor = pageOffsets.getOrDefault(networkId, "");

            Timber.d("Fetch from opensea : %s", networkName);

            do
            {
                String jsonData = fetchAssets(networkId, address, pageCursor);
                if (!JsonUtils.hasAssets(jsonData))
                {
                    return foundTokens.values().toArray(new Token[0]); //on error return results found so far
                }

                JSONObject result = new JSONObject(jsonData);
                JSONArray assets = result.getJSONArray("nfts");

                receivedTokens = assets.length();

                //process this page of results
                processOpenseaTokens(foundTokens, assets, address, networkId, networkName, tokensService);
                currentPage++;
                pageCursor = result.has("next") ? result.getString("next") : "";
                if (TextUtils.isEmpty(pageCursor))
                {
                    break;
                }
            }
            while (currentPage <= 3); //fetch 4 pages for each loop

            pageOffsets.put(networkId, pageCursor);

            if (receivedTokens < PAGE_SIZE)
            {
                Timber.d("Reset OpenSeaAPI reads at: %s", pageCursor);
            }
            else
            {
                networkCheckTimes.put(networkId, currentTime - 55 * DateUtils.SECOND_IN_MILLIS); //do another read within 5 seconds
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

    private void processOpenseaTokens(Map<String, Token> foundTokens,
                                      JSONArray assets,
                                      String address,
                                      long networkId,
                                      String networkName,
                                      TokensService tokensService) throws Exception
    {
        final Map<String, Map<BigInteger, NFTAsset>> assetList = new HashMap<>();

        for (int i = 0; i < assets.length(); i++)
        {
            JSONObject assetJSON = assets.getJSONObject(i);
            String tokenStandard = assetJSON.getString("token_standard").toLowerCase(Locale.ROOT);

            if (!TextUtils.isEmpty(tokenStandard))
            {
                switch (tokenStandard)
                {
                    case "erc721":
                        handleERC721(assetList, assetJSON, networkId, foundTokens, tokensService,
                                networkName, address);
                        break;
                    case "erc1155":
                        handleERC1155(assetList, assetJSON, networkId, foundTokens, tokensService,
                                networkName, address);
                        break;
                }
            }
        }
    }

    private void handleERC721(Map<String, Map<BigInteger, NFTAsset>> assetList,
                              JSONObject assetJSON,
                              long networkId,
                              Map<String, Token> foundTokens,
                              TokensService svs,
                              String networkName,
                              String address) throws Exception
    {
        NFTAsset asset = new NFTAsset(assetJSON.toString());

        BigInteger tokenId = assetJSON.has("identifier") ?
                new BigInteger(assetJSON.getString("identifier"))
                : null;
        if (tokenId == null) return;

        String contractAddress = assetJSON.getString("contract");
        String collectionName = assetJSON.getString("collection");

        Token token = foundTokens.get(contractAddress);
        if (token == null)
        {
            TokenInfo tInfo;
            ContractType type;
            long lastCheckTime = 0;
            Token checkToken = svs.getToken(networkId, contractAddress);
            if (checkToken != null && (checkToken.isERC721() || checkToken.isERC721Ticket()))
            {
                assetList.put(contractAddress, checkToken.getTokenAssets());
                tInfo = checkToken.tokenInfo;
                type = checkToken.getInterfaceSpec();
                lastCheckTime = checkToken.lastTxTime;

                if (!TextUtils.isEmpty(collectionName) && (TextUtils.isEmpty(checkToken.tokenInfo.name)))
                {
                    //Update to collection name if the token name is blank, or if the collection name is not blank and current token name is different
                    tInfo = new TokenInfo(contractAddress, collectionName, "", 0, tInfo.isEnabled, networkId);
                }
            }
            else //if we haven't seen the contract before, or it was previously logged as something other than a ERC721 variant then specify undetermined flag
            {
                tInfo = new TokenInfo(contractAddress, asset.getName(), "", 0, true, networkId);
                type = ContractType.ERC721;
            }

            token = tf.createToken(tInfo, type, networkName);
            token.setTokenWallet(address);
            token.lastTxTime = lastCheckTime;
            foundTokens.put(contractAddress, token);
        }
        asset.updateAsset(tokenId, assetList.get(token.getAddress()));
        token.addAssetToTokenBalanceAssets(tokenId, asset);
    }

    private void handleERC1155(Map<String, Map<BigInteger, NFTAsset>> assetList,
                               JSONObject assetJSON,
                               long networkId,
                               Map<String, Token> foundTokens,
                               TokensService svs,
                               String networkName,
                               String address) throws Exception
    {
        NFTAsset asset = new NFTAsset(assetJSON.toString());

        BigInteger tokenId = assetJSON.has("identifier") ?
                new BigInteger(assetJSON.getString("identifier"))
                : null;

        if (tokenId == null) return;

        String contractAddress = assetJSON.getString("contract");
        String collectionName = assetJSON.getString("collection");

        Token token = foundTokens.get(contractAddress);
        if (token == null)
        {
            TokenInfo tInfo;
            ContractType type;
            long lastCheckTime = 0;
            Token checkToken = svs.getToken(networkId, contractAddress);
            if (checkToken != null && checkToken.getInterfaceSpec() == ContractType.ERC1155)
            {
                assetList.put(contractAddress, checkToken.getTokenAssets());
                tInfo = checkToken.tokenInfo;
                type = checkToken.getInterfaceSpec();
                lastCheckTime = checkToken.lastTxTime;
            }
            else
            {
                tInfo = new TokenInfo(contractAddress, collectionName, "", 0, true, networkId);
                type = ContractType.ERC1155;
            }

            token = tf.createToken(tInfo, type, networkName);
            token.setTokenWallet(address);
            token.lastTxTime = lastCheckTime;
            foundTokens.put(contractAddress, token);
        }
        asset.updateAsset(tokenId, assetList.get(token.getAddress()));
        token.addAssetToTokenBalanceAssets(tokenId, asset);
    }

    private void addAssetImageToHashMap(String address, String imageUrl)
    {
        if (!imageUrls.containsKey(address) && !TextUtils.isEmpty(imageUrl))
        {
            imageUrls.put(address, imageUrl);
        }
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

    public Single<String> getAsset(Token token, BigInteger tokenId)
    {
        if (!com.alphawallet.app.repository.EthereumNetworkBase.hasOpenseaAPI(token.tokenInfo.chainId))
        {
            return Single.fromCallable(() -> "");
        }
        else
        {
            return Single.fromCallable(() ->
                    fetchAsset(token.tokenInfo.chainId, token.tokenInfo.address, tokenId.toString()));
        }
    }

    public Single<String> getCollection(Token token, String slug)
    {
        return Single.fromCallable(() ->
                fetchCollection(token.tokenInfo.chainId, slug));
    }

    public String fetchAssets(long networkId, String address, String pageCursor)
    {
        String mappingName = API_CHAIN_MAP.get(networkId);
        if (TextUtils.isEmpty(mappingName))
        {
            return JsonUtils.EMPTY_RESULT;
        }

        String api = C.OPENSEA_ASSETS_API_V2.replace("{CHAIN}", mappingName).replace("{ADDRESS}", address);

        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(api)
                .appendQueryParameter("limit", String.valueOf(PAGE_SIZE));

        if (!TextUtils.isEmpty(pageCursor))
        {
            builder.appendQueryParameter("next", pageCursor);
        }

        return executeRequest(networkId, builder.build().toString());
    }

    public String fetchAsset(long networkId, String contractAddress, String tokenId)
    {
        String mappingName = API_CHAIN_MAP.get(networkId);
        if (TextUtils.isEmpty(mappingName))
        {
            return JsonUtils.EMPTY_RESULT;
        }

        String api = C.OPENSEA_NFT_API_V2.replace("{CHAIN}", mappingName).replace("{ADDRESS}", contractAddress).replace("{TOKEN_ID}", tokenId);
        return executeRequest(networkId, api);
    }

    public String fetchCollection(long networkId, String slug)
    {
        String api = C.OPENSEA_COLLECTION_API_MAINNET + slug;
        return executeRequest(networkId, api);
    }
}
