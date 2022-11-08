package com.alphawallet.app.service;

import android.text.TextUtils;

import com.alphawallet.app.entity.QueryResponse;
import com.alphawallet.app.entity.tokenscript.TestScript;
import com.alphawallet.app.util.Utils;

import java.io.IOException;
import java.net.SocketTimeoutException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * Created by JB on 3/11/2022.
 */
public class IPFSService implements IPFSServiceType
{
    private final OkHttpClient client;

    public IPFSService(OkHttpClient okHttpClient)
    {
        this.client = okHttpClient;
    }

    public String getContent(String url)
    {
        try
        {
            QueryResponse response = performIO(url, null);

            if (response.isSuccessful())
            {
                return response.body;
            }
            else
            {
                return "";
            }
        }
        catch (Exception e)
        {
            Timber.w(e);
            return "";
        }
    }

    public QueryResponse performIO(String url, String[] headers) throws IOException
    {
        if (!Utils.isValidUrl(url)) throw new IOException("URL not valid");

        if (Utils.isIPFS(url)) //note that URL might contain IPFS, but not pass 'isValidUrl'
        {
            return getFromIPFS(url);
        }
        else
        {
            return get(url, headers);
        }
    }

    private QueryResponse get(String url, String[] headers) throws IOException
    {
        Request.Builder bld = new Request.Builder()
                .url(url)
                .get();

        if (headers != null) addHeaders(bld, headers);

        Response response = client.newCall(bld.build()).execute();
        return new QueryResponse(response.code(), response.body().string());
    }

    private QueryResponse getFromIPFS(String url) throws IOException
    {
        if (isTestCode(url)) return loadTestCode();

        //try Infura first
        String tryIPFS = Utils.resolveIPFS(url, Utils.IPFS_IO_RESOLVER);
        //attempt to load content
        QueryResponse r;
        try
        {
            r = get(tryIPFS, null);
        }
        catch (SocketTimeoutException e)
        {
            //timeout, try second node. Any other failure simply throw back to calling function
            tryIPFS = Utils.resolveIPFS(url, Utils.IPFS_INFURA_RESOLVER);
            r = get(tryIPFS, null); //if this throws it will be picked up by calling function
        }

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

    //For testing
    private boolean isTestCode(String url)
    {
        return (!TextUtils.isEmpty(url) && url.endsWith("QmXXLFBeSjXAwAhbo1344wJSjLgoUrfUK9LE57oVubaRRp"));
    }

    private QueryResponse loadTestCode()
    {
        //restore the TokenScript for the certificate test
        return new QueryResponse(200, TestScript.testScriptXXLF);
    }
}
