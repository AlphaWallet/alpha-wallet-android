package com.alphawallet.app.entity;

/**
 * Created by JB on 5/11/2022.
 */
public class QueryResponse
{
    public final int code;
    public final String body;

    public QueryResponse(int code, String body)
    {
        this.code = code;
        this.body = body;
    }

    public boolean isSuccessful()
    {
        return code >= 200 && code <= 299;
    }
}


