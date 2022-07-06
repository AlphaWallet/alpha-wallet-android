package com.alphawallet.app.api.v1.entity.response;

import android.net.Uri;

public abstract class ApiV1Response
{
    protected final String redirectUrl;

    public ApiV1Response(String redirectUrl)
    {
        this.redirectUrl = redirectUrl;
    }

    public abstract String getCallType();

    public abstract Uri uri();
}
