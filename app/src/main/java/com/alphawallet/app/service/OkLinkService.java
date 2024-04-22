package com.alphawallet.app.service;

import static com.alphawallet.ethereum.EthereumNetworkBase.ARBITRUM_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BASE_MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CLASSIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.LINEA_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OKX_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_AMOY_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.SEPOLIA_TESTNET_ID;

import android.net.Uri;
import android.text.TextUtils;
import android.util.LongSparseArray;

import com.alphawallet.app.entity.EtherscanEvent;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.OkxEvent;
import com.alphawallet.app.entity.okx.TokenListReponse;
import com.alphawallet.app.entity.okx.OkServiceResponse;
import com.alphawallet.app.entity.okx.OkProtocolType;
import com.alphawallet.app.entity.okx.OkToken;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.transactionAPI.TransferFetchType;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.app.util.JsonUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class OkLinkService
{
    private static final String TAG = "OKLINK";
    private static final String BASE_URL = "https://www.oklink.com/api";
    private static final String LIMIT = "50"; // Max limit; default is 20

    public static OkLinkService instance;
    private final OkHttpClient httpClient;
    private final boolean hasKey;

    private static final LongSparseArray<String> shortNames = new LongSparseArray<>()
    {
        {
            put(MAINNET_ID, "ETH");
            put(OKX_ID, "OKTC");
            put(POLYGON_AMOY_ID, "AMOY_TESTNET");
            put(ARBITRUM_MAIN_ID, "ARBITRUM");
            put(BINANCE_MAIN_ID, "BSC");
            put(KLAYTN_ID, "KLAYTN");
            put(CLASSIC_ID, "ETC");
            put(POLYGON_ID, "POLYGON");
            put(AVALANCHE_ID, "AVAXC");
            put(FANTOM_ID, "FTM");
            put(OPTIMISTIC_MAIN_ID, "OP");
            put(LINEA_ID, "LINEA");
            put(BASE_MAINNET_ID, "BASE");
            put(SEPOLIA_TESTNET_ID, "SEPOLIA_TESTNET");
        }
    };

    private static final Map<String, LongSparseArray<Integer>> networkChecked = new HashMap<>();// LongSparseArray<>();

    public OkLinkService(OkHttpClient httpClient)
    {
        this.httpClient = httpClient;
        this.hasKey = !KeyProviderFactory.get().getOkLinkKey().isEmpty();
    }

    public static OkLinkService get(OkHttpClient httpClient)
    {
        if (instance == null)
        {
            instance = new OkLinkService(httpClient);
        }

        return instance;
    }

    public static boolean supportsChain(long chainId)
    {
        return !shortNames.get(chainId, "").isEmpty();
    }

    private String getChainShortName(long chainId)
    {
        return shortNames.get(chainId, "");
    }

    private Request buildRequest(String api, boolean useAlt)
    {
        Request.Builder requestB = new Request.Builder()
            .url(api)
            .header("User-Agent", "Chrome/74.0.3729.169")
            .addHeader("Content-Type", "application/json")
            .addHeader("Ok-Access-Key", useAlt ? KeyProviderFactory.get().getOkLBKey() : KeyProviderFactory.get().getOkLinkKey())
            .get();
        return requestB.build();
    }

    private String executeRequest(String api, boolean useAlt)
    {
        try (okhttp3.Response response = httpClient.newCall(buildRequest(api, useAlt)).execute())
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
                return Objects.requireNonNull(response.body()).string();
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
            return e.getMessage();
        }

        return JsonUtils.EMPTY_RESULT;
    }

    public EtherscanEvent[] getEtherscanEvents(long chainId, String address, long lastBlockRead, TransferFetchType tfType)
    {
        if (!hasKey || !supportsChain(chainId))
        {
            return new EtherscanEvent[0];
        }

        String protocolType = getOkxFetchType(tfType);
        List<OkxEvent> events = new ArrayList<>();
        int page = 1;
        int totalPage = 0;
        boolean reachedPreviousRead;

        do
        {
            OkServiceResponse response = new Gson().fromJson(
                fetchTransactions(chainId, address, protocolType, String.valueOf(page++)),
                OkServiceResponse.class);

            if (response.data != null && !response.data.isEmpty())
            {
                String totalPageStr = response.data.get(0).totalPage;
                if (!TextUtils.isEmpty(totalPageStr))
                {
                    totalPage = Integer.parseInt(totalPageStr);
                }
                events.addAll(response.data.get(0).transactionLists);
                reachedPreviousRead = compareEventsWithLastRead(events, lastBlockRead);
            }
            else
            {
                break;
            }
        }
        while (page <= totalPage && !reachedPreviousRead);

        List<EtherscanEvent> etherscanEvents = new ArrayList<>();
        for (OkxEvent ev : events)
        {
            try
            {
                boolean isNft = tfType == TransferFetchType.ERC_721 || tfType == TransferFetchType.ERC_1155;
                etherscanEvents.add(ev.getEtherscanTransferEvent(isNft));
            }
            catch (Exception e)
            {
                Timber.e(e);
            }
        }

        return etherscanEvents.toArray(new EtherscanEvent[0]);
    }

    public Single<List<OkToken>> getTokensForChain(long chainId, String address, OkProtocolType tokenType) //reserve this call for one per day
    {
        List<OkToken> txReturn = new ArrayList<>();
        if (!supportsChain(chainId) || !canCheckChain(chainId, address, tokenType))
        {
            return Single.fromCallable(() -> txReturn);
        }

        return Single.fromCallable(() -> {
            int page = 1;
            int totalPage = 0;

            LongSparseArray<Integer> thisChainCheck = getCheckList(address);

            int existingCheck = thisChainCheck.get(chainId, 0);

            int byteEntry = 2^tokenType.ordinal();

            thisChainCheck.put(chainId, existingCheck | byteEntry);

            do
            {
                OkServiceResponse response = new Gson().fromJson(
                        fetchTokens(chainId, address, tokenType, String.valueOf(page++)),
                        OkServiceResponse.class);

                if (response.data != null && !response.data.isEmpty())
                {
                    String totalPageStr = response.data.get(0).totalPage;
                    if (!TextUtils.isEmpty(totalPageStr))
                    {
                        totalPage = Integer.parseInt(totalPageStr);
                    }
                    txReturn.addAll(response.data.get(0).tokenList);
                }
                else
                {
                    break;
                }
            }
            while (page <= totalPage);

            return txReturn;
        });
    }

    private LongSparseArray<Integer> getCheckList(String address)
    {
        LongSparseArray<Integer> thisChainCheck = networkChecked.get(address);
        if (thisChainCheck == null)
        {
            thisChainCheck = new LongSparseArray<>();
        }

        return thisChainCheck;
    }

    public String fetchTokens(long chainId, String address,
                              OkProtocolType tokenType, String page)
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(BASE_URL + "/v5/explorer/address/token-balance")
                .appendQueryParameter("address", address)
                .appendQueryParameter("chainShortName", getChainShortName(chainId))
                .appendQueryParameter("protocolType", tokenType.getValue())
                .appendQueryParameter("limit", LIMIT)
                .appendQueryParameter("page", page);
        String url = builder.build().toString();
        return executeRequest(url, true);
    }

    //@SuppressWarnings("ConstantConditions")
    private boolean compareEventsWithLastRead(List<OkxEvent> events, long lastBlockRead)
    {
        for (OkxEvent ev : events)
        {
            if (!TextUtils.isEmpty(ev.height))
            {
                long height = Long.parseLong(ev.height);
                if (height < lastBlockRead)
                {
                    return true;
                }
            }
        }

        return false;
    }

    public String fetchTransactions(long chainId, String address,
                                    String protocolType,
                                    String page)
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(BASE_URL + "/v5/explorer/address/transaction-list")
            .appendQueryParameter("address", address)
            .appendQueryParameter("protocolType", protocolType)
            .appendQueryParameter("chainShortName", getChainShortName(chainId))
            .appendQueryParameter("limit", LIMIT)
            .appendQueryParameter("page", page);
        String url = builder.build().toString();
        return executeRequest(url, false);
    }

    public TokenInfo getTokenInfo(long chainId, String contractAddress)
    {
        TokenListReponse.TokenDetails tokenDetails = getTokenDetails(chainId, contractAddress);
        return new TokenInfo(
            tokenDetails.tokenContractAddress,
            tokenDetails.tokenFullName,
            tokenDetails.token,
            Integer.parseInt(tokenDetails.precision),
            true,
            chainId);
    }

    public TokenListReponse.TokenDetails getTokenDetails(long chainId, String contractAddress)
    {
        TokenListReponse response = new Gson().fromJson(fetchTokenDetails(chainId, contractAddress), TokenListReponse.class);
        if (!response.data.isEmpty())
        {
            List<TokenListReponse.TokenDetails> tokenList = response.data.get(0).tokenList;

            if (!tokenList.isEmpty())
            {
                return tokenList.get(0);
            }
        }
        return null;
    }

    public String fetchTokenDetails(long chainId, String contractAddress)
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(BASE_URL + "/v5/explorer/token/token-list")
            .appendQueryParameter("tokenContractAddress", contractAddress)
            .appendQueryParameter("chainShortName", getChainShortName(chainId));
        String url = builder.build().toString();
        return executeRequest(url, false);
    }

    private String getOkxFetchType(TransferFetchType tfType)
    {
        if (tfType == TransferFetchType.ERC_721)
        {
            return "token_721";
        }
        else if (tfType == TransferFetchType.ERC_1155)
        {
            return "token_1155";
        }
        else
        {
            return "token_20";
        }
    }

    private boolean canCheckChain(long networkId, String address, OkProtocolType checkType)
    {
        LongSparseArray<Integer> thisAddressCheck = networkChecked.get(address);
        return (thisAddressCheck == null || (thisAddressCheck.get(networkId, 0) & 2^checkType.ordinal()) == 1);
    }
}
