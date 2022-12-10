package com.alphawallet.app.util.ens;

import android.net.Uri;
import android.text.TextUtils;

import com.alphawallet.app.entity.unstoppable.GetRecordsResult;
import com.alphawallet.app.repository.KeyProvider;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.google.gson.Gson;

import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class UnstoppableDomainsResolver implements Resolvable
{
    private static final String GET_RECORDS_FOR_DOMAIN = "https://resolve.unstoppabledomains.com/domains/";
    private final KeyProvider keyProvider = KeyProviderFactory.get();
    private final OkHttpClient client;
    private final long chainId;

    public UnstoppableDomainsResolver(OkHttpClient client, long chainId)
    {
        this.client = client;
        this.chainId = chainId;
    }

    @Override
    public String resolve(String domainName) throws Exception
    {
        Uri.Builder builder = new Uri.Builder();
        builder.encodedPath(GET_RECORDS_FOR_DOMAIN)
                .appendEncodedPath(domainName);

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + keyProvider.getUnstoppableDomainsKey())
                .url(builder.build().toString())
                .get()
                .build();

        try (okhttp3.Response response = client.newCall(request).execute())
        {
            ResponseBody responseBody = response.body();
            if (responseBody != null)
            {
                GetRecordsResult result = new Gson().fromJson(responseBody.string(), GetRecordsResult.class);
                response.close();
                return getAddressFromRecords(result.records, chainId);
            }
            response.close();
            return "";
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return "";
    }

    private String getAddressFromRecords(HashMap<String, String> records, long chainId)
    {
        String ethAddress = records.getOrDefault("crypto.ETH.address", "");
        String maticAddress = records.getOrDefault("crypto.MATIC.version.MATIC.address", "");
        if (chainId == EthereumNetworkBase.MAINNET_ID)
        {
            return ethAddress;
        }
        else if (chainId == EthereumNetworkBase.POLYGON_ID)
        {
            return maticAddress;
        }
        else
        {
            if (!TextUtils.isEmpty(ethAddress))
            {
                return ethAddress;
            }
            else if (!TextUtils.isEmpty(maticAddress))
            {
                return maticAddress;
            }
        }
        return "";
    }
}
