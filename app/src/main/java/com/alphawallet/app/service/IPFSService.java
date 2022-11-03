package com.alphawallet.app.service;

import android.text.TextUtils;

import com.alphawallet.app.util.Utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by JB on 3/11/2022.
 */
public class IPFSService implements IPFSServiceType
{
    private final OkHttpClient client;

    public IPFSService(OkHttpClient okHttpClient)
    {
        this.client = new OkHttpClient.Builder()
                //.connectTimeout(C.CONNECT_TIMEOUT*2, TimeUnit.SECONDS)
                //.readTimeout(C.READ_TIMEOUT*2, TimeUnit.SECONDS)
                //.writeTimeout(C.WRITE_TIMEOUT*2, TimeUnit.SECONDS)
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .writeTimeout(1, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(false)
                .build();
    }

    public String getContent(String request)
    {
        try (Response response = performIO(request))
        {
            if (response.isSuccessful())
            {
                String resp = response.body().string();
                System.out.println("YOLESS: TEXT: " + resp);
                return resp; //response.body().string();
            }
            else
            {
                return null;
            }
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public Response performIO(String request) throws Exception
    {
        return performIO(request, null);
    }

    public Response performIO(String request, String[] headers) throws IOException
    {
        if (TextUtils.isEmpty(request)) return null;

        if (Utils.isIPFS(request)) //note that URL might contain IPFS, but not pass 'isValidUrl' //TODO: Update URL Pattern for raw IPFS (see isValidUrl) and refactor
        {
            return getFromIPFS(request);
        }
        else if (Utils.isValidUrl(request))
        {
            return get(request, headers);
        }
        else
        {
            return new Response.Builder().build();
        }
    }

    private Response get(String request, String[] headers) throws IOException
    {
        Request.Builder bld = new Request.Builder()
                .url(request)
                .get();

        if (headers != null) addHeaders(bld, headers);

        return client.newCall(bld.build()).execute();
    }

    private Response getFromIPFS(String request) throws IOException
    {
        //try Infura first
        String tryIPFS = Utils.resolveIPFS(request, Utils.IPFS_IO_RESOLVER);
        //attempt to load content
        Response r = get(tryIPFS, null);
        System.out.println("YOLESS: Test ipfs " + tryIPFS);
        if (!r.isSuccessful())
        {
            tryIPFS = Utils.resolveIPFS(request, Utils.IPFS_INFURA_RESOLVER);
            System.out.println("YOLESS: Nope ipfs " + tryIPFS);
            r = get(tryIPFS, null);
        }

        System.out.println("YOLESS: Test ipfs " + (r.isSuccessful() ? "yep" : "nope"));

        return r;
    }

    private void addHeaders(Request.Builder bld, String[] headers) throws IOException
    {
        if (headers.length % 2 != 0)
            throw new IOException("Headers must be even value: [{name, value}, {...}]");

        String name = null;

        for (String header : headers)
        {
            if (name == null)
            {
                name = header;
            }
            else
            {
                bld.addHeader(name, header);
                name = null;
            }
        }
    }
}
