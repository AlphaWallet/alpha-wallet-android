package com.alphawallet.app.service;

import android.net.Uri;
import android.text.TextUtils;

import com.alphawallet.app.entity.EtherscanEvent;
import com.alphawallet.app.entity.OkxEvent;
import com.alphawallet.app.entity.okx.TokenListReponse;
import com.alphawallet.app.entity.okx.TransactionListResponse;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.transactionAPI.TransferFetchType;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.app.util.JsonUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class OkLinkService
{
    private static final String BASE_URL = "https://www.oklink.com/api";
    private static final String LIMIT = "50"; // Max limit; default is 20
    private static final String CHAIN_SHORT_NAME = "OKC"; // Max limit; default is 20
    public static OkLinkService instance;
    private final OkHttpClient httpClient;

    public OkLinkService(OkHttpClient httpClient)
    {
        this.httpClient = httpClient;
    }

    public static OkLinkService get(OkHttpClient httpClient)
    {
        if (instance == null)
        {
            instance = new OkLinkService(httpClient);
        }
        return instance;
    }

    private Request buildRequest(String api)
    {
        Request.Builder requestB = new Request.Builder()
            .url(api)
            .header("User-Agent", "Chrome/74.0.3729.169")
            .addHeader("Content-Type", "application/json")
            .addHeader("Ok-Access-Key", KeyProviderFactory.get().getOkLinkKey())
            .get();
        return requestB.build();
    }

    private String executeRequest(String api)
    {
        try (okhttp3.Response response = httpClient.newCall(buildRequest(api)).execute())
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

    public EtherscanEvent[] getEtherscanEvents(String address, long lastBlockRead, TransferFetchType tfType)
    {
        String protocolType = getOkxFetchType(tfType);
        List<OkxEvent> events = new ArrayList<>();
        int page = 1;
        int totalPage = 0;
        boolean reachedPreviousRead;

        do
        {
            TransactionListResponse response = new Gson().fromJson(
                fetchTransactions(address, protocolType, String.valueOf(page++)),
                TransactionListResponse.class);

            if (response.data != null && response.data.size() > 0)
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

    public String fetchTransactions(String address,
                                    String protocolType,
                                    String page)
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(BASE_URL + "/v5/explorer/address/transaction-list")
            .appendQueryParameter("address", address)
            .appendQueryParameter("protocolType", protocolType)
            .appendQueryParameter("chainShortName", CHAIN_SHORT_NAME)
            .appendQueryParameter("limit", LIMIT)
            .appendQueryParameter("page", page);
        String url = builder.build().toString();
        return executeRequest(url);
    }

    public TokenInfo getTokenInfo(String contractAddress)
    {
        TokenListReponse.TokenDetails tokenDetails = getTokenDetails(contractAddress);
        return new TokenInfo(
            tokenDetails.tokenContractAddress,
            tokenDetails.tokenFullName,
            tokenDetails.token,
            Integer.parseInt(tokenDetails.precision),
            true,
            66);
    }

    public TokenListReponse.TokenDetails getTokenDetails(String contractAddress)
    {
        TokenListReponse response = new Gson().fromJson(fetchTokenDetails(contractAddress), TokenListReponse.class);
        if (response.data.size() > 0)
        {
            List<TokenListReponse.TokenDetails> tokenList = response.data.get(0).tokenList;

            if (tokenList.size() > 0)
            {
                return tokenList.get(0);
            }
        }
        return null;
    }

    public String fetchTokenDetails(String contractAddress)
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(BASE_URL + "/v5/explorer/token/token-list")
            .appendQueryParameter("tokenContractAddress", contractAddress)
            .appendQueryParameter("chainShortName", CHAIN_SHORT_NAME);
        String url = builder.build().toString();
        return executeRequest(url);
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
}
