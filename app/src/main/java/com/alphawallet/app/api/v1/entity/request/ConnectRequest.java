package com.alphawallet.app.api.v1.entity.request;

public class ConnectRequest extends ApiV1Request
{
    public String address;

    public ConnectRequest(String requestUrl)
    {
        super(requestUrl);
    }

    public String getAddress()
    {
        return address;
    }
}
