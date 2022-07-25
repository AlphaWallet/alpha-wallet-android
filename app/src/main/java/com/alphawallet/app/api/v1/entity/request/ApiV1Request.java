package com.alphawallet.app.api.v1.entity.request;

import com.alphawallet.app.api.v1.entity.ApiV1;
import com.alphawallet.app.api.v1.entity.Metadata;
import com.alphawallet.app.api.v1.entity.Method;
import com.google.gson.Gson;

import okhttp3.HttpUrl;

public class ApiV1Request
{
    protected HttpUrl requestUrl;
    protected Method method;
    protected String redirectUrl;
    protected Metadata metadata;
    protected boolean isValid;

    public ApiV1Request(String requestUrl)
    {
        parse(requestUrl);
    }

    public void parse(String urlString)
    {
        final HttpUrl url = HttpUrl.parse(urlString);
        if (url != null)
        {
            for (Method method : ApiV1.VALID_METHODS)
            {
                if (method.getPath().equals(url.encodedPath()))
                {
                    this.isValid = true;
                    this.requestUrl = url;
                    this.method = method;
                    this.redirectUrl = url.queryParameter(ApiV1.RequestParams.REDIRECT_URL);
                    final String metadataJson = url.queryParameter(ApiV1.RequestParams.METADATA);
                    this.metadata = new Gson().fromJson(metadataJson, Metadata.class);
                }
            }
        }
        else
        {
            isValid = false;
        }
    }

    public boolean isValid()
    {
        return this.isValid;
    }

    public Method getMethod()
    {
        return method;
    }

    public Metadata getMetadata()
    {
        return this.metadata;
    }

    public String getRequestUrl()
    {
        return requestUrl.toString();
    }

    public String getRedirectUrl()
    {
        return redirectUrl;
    }
}
