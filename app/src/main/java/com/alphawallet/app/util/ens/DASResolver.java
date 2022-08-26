package com.alphawallet.app.util.ens;

import com.alphawallet.app.util.das.DASBody;
import com.alphawallet.app.util.das.DASRecord;
import com.google.gson.Gson;

import org.web3j.protocol.http.HttpService;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import timber.log.Timber;

public class DASResolver implements Resolvable
{
    private static final String DAS_LOOKUP = "https://indexer.da.systems/";
    private static final String DAS_NAME = "[DAS_NAME]";
    private static final String DAS_PAYLOAD = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"das_searchAccount\",\"params\":[\"" + DAS_NAME + "\"]}";
    private final OkHttpClient client;

    public DASResolver(OkHttpClient client)
    {
        this.client = client;
    }

    public String resolve(String name)
    {
        String payload = DAS_PAYLOAD.replace(DAS_NAME, name);

        RequestBody requestBody = RequestBody.create(payload, HttpService.JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(DAS_LOOKUP)
                .post(requestBody)
                .build();

        try (okhttp3.Response response = client.newCall(request).execute())
        {
            //get result
            String result = response.body() != null ? response.body().string() : "";

            DASBody dasResult = new Gson().fromJson(result, DASBody.class);
            dasResult.buildMap();

            //find ethereum entry
            DASRecord ethLookup = dasResult.records.get("address.eth");
            if (ethLookup != null)
            {
                return ethLookup.getAddress();
            }
            else
            {
                return dasResult.getEthOwner();
            }
        }
        catch (Exception e)
        {
            Timber.tag("ENS").e(e);
        }

        return "";
    }
}
