package com.alphawallet.app.service;

import java.io.*;

import com.alphawallet.app.entity.AddressInformation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class KlerosCurateService
{
    public static final AddressInformation NULL_ADDRESS_INFORMATION = new AddressInformation("", "", "", "");
    private static final String IPFS_BASE_URL = "https://ipfs.kleros.io";
    private final IpfsClient ipfs;
    private final GraphClient graph;

    public KlerosCurateService(OkHttpClient httpClient)
    {
        Gson gson = new GsonBuilder().create();
        graph = new GraphClient(httpClient, gson);
        ipfs = new IpfsClient(httpClient, gson);
    }

    public AddressInformation request(String address) throws IOException
    {
        Timber.d("Trying Curate Ethereum for address %s...", address);
        AddressInformation info = requestCurateEthereum(address);
        if (info == null || info == NULL_ADDRESS_INFORMATION)
        {
            Timber.d("No information from Curate Ethereum, trying Curate Gnosis for address %s...", address);
            info = requestCurateGnosis(address);
        }
        if (info == null || info == NULL_ADDRESS_INFORMATION)
        {
            Timber.d("No information from Curate Gnosis for address %s...", address);
        }
        return info;
    }

    public AddressInformation requestCurateEthereum(String address) throws IOException
    {
        return processGraphResponse(graph.queryCurateEthereum(address));
    }

    public AddressInformation requestCurateGnosis(String address) throws IOException
    {
        return processGraphResponse(graph.queryCurateGnosis(address));
    }

    private AddressInformation processGraphResponse(GraphClient.GraphResponse graphResponse) throws IOException
    {
        GraphClient.GraphResponse.LItems.LItem[] lItems = graphResponse.data.litems;
        if (lItems.length == 0)
        {
            return NULL_ADDRESS_INFORMATION;
        }

        String ipfsCid = lItems[0].data;
        Timber.d("ipfsCid: %s", ipfsCid);

        String ipfsUrl = IPFS_BASE_URL + ipfsCid;
        IpfsClient.IpfsItem ipfsItem = ipfs.requestCurateItem(ipfsUrl);
        Timber.d("IpfsItemValues: %s", ipfsItem.values);

        return new AddressInformation(ipfsItem.values.publicNameTag,
                ipfsItem.values.contractAddress,
                ipfsItem.values.uiWebsiteLink,
                ipfsItem.values.publicNote);
    }
}

class GraphClient
{
    private static final String CURATE_ETHEREUM_GRAPH_URL = "https://api.thegraph.com/subgraphs/name/kleros/curate";
    private static final String CURATE_GNOSIS_GRAPH_URL = "https://api.thegraph.com/subgraphs/name/eccentricexit/curate-xdai-ii";
    private static final String ADDRESS_TOKEN = "_ADDRESS_";
    private static final String CURATE_ETHEREUM_QUERY = "{ litems( where: {registry: \"0x6e31d83b0c696f7d57241d3dffd0f2b628d14c67\", status: Registered, keywords_contains: \"" + ADDRESS_TOKEN + "\"} ) { keywords data } }";
    private static final String CURATE_GNOSIS_QUERY = "{ litems( where: {registry: \"0x76944a2678a0954a610096ee78e8ceb8d46d5922\", status: Registered, keywords_contains: \"" + ADDRESS_TOKEN + "\"} ) { keywords data } }";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final Gson gson;

    public GraphClient(OkHttpClient httpClient, Gson gson)
    {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public GraphResponse queryCurateEthereum(String address) throws IOException
    {
        return queryTheGraph(address, CURATE_ETHEREUM_QUERY, CURATE_ETHEREUM_GRAPH_URL);
    }

    public GraphResponse queryCurateGnosis(String address) throws IOException
    {
        return queryTheGraph(address, CURATE_GNOSIS_QUERY, CURATE_GNOSIS_GRAPH_URL);
    }

    private GraphResponse queryTheGraph(String address,
                                        String curateQuery,
                                        String curateGraphUrl) throws IOException
    {
        GraphQuery query = new GraphQuery(curateQuery.replace(ADDRESS_TOKEN, address));
        String json = gson.toJson(query);
        Timber.d(json);

        RequestBody body = RequestBody.Companion.create(json, JSON);
        Request request = new Request.Builder().url(curateGraphUrl)
                .post(body)
                .build();

        GraphResponse graphResponse;
        try (Response response = httpClient.newCall(request).execute())
        {
            Timber.d("Response: %s", response.toString());

            String responseBody = response.body().string();
            Timber.d("ResponseBody: %s", responseBody);
            graphResponse = gson.fromJson(responseBody, GraphResponse.class);
        }
        return graphResponse;
    }

    static class GraphQuery
    {
        public String query;

        public GraphQuery(String query)
        {
            this.query = query;
        }
    }

    static class GraphResponse
    {
        public LItems data;

        static class LItems
        {
            public LItem[] litems;

            static class LItem
            {
                public String keywords;
                public String data;
            }
        }
    }
}

class IpfsClient
{
    private final OkHttpClient httpClient;
    private final Gson gson;

    public IpfsClient(OkHttpClient httpClient, Gson gson)
    {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public IpfsItem requestCurateItem(String ipfsUrl) throws IOException
    {
        Request request = new Request.Builder().url(ipfsUrl).build();
        IpfsItem ipfsItem;
        try (Response response = httpClient.newCall(request).execute())
        {
            Timber.d("Response: %s", response);

            String body = response.body().string();
            Timber.d("ResponseBody: %s", body);
            ipfsItem = gson.fromJson(body, IpfsItem.class);
        }
        return ipfsItem;
    }

    static class IpfsItem
    {
        public IpfsItemValues values;

        static class IpfsItemValues
        {
            @SerializedName("Public Name Tag")
            public String publicNameTag;

            @SerializedName("Contract Address")
            public String contractAddress;

            @SerializedName("UI/Website Link")
            public String uiWebsiteLink;

            @SerializedName("Public Note")
            public String publicNote;

            @Override
            public String toString()
            {
                return "IpfsItemValues{"
                        + "publicNameTag='"
                        + publicNameTag + '\''
                        + ", contractAddress='"
                        + contractAddress + '\''
                        + ", uiWebsiteLink='"
                        + uiWebsiteLink + '\''
                        + ", publicNote='"
                        + publicNote + '\''
                        + '}';
            }
        }
    }
}


