package com.alphawallet.app.api.v1.entity;

public class Method
{
    private final String callType;
    private final String path;

    public Method(String path, String callType)
    {
        this.path = path;
        this.callType = callType;
    }

    public String getCallType()
    {
        return callType;
    }

    public String getPath()
    {
        return path;
    }
}
