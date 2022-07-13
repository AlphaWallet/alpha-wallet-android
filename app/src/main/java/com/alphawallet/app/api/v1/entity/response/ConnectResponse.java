package com.alphawallet.app.api.v1.entity.response;

import android.net.Uri;
import android.text.TextUtils;

import com.alphawallet.app.api.v1.entity.ApiV1;

public class ConnectResponse extends ApiV1Response
{
    private final String address;

    public ConnectResponse(String redirectUrl, String address)
    {
        super(redirectUrl);
        this.address = address;
    }

    @Override
    public Uri uri()
    {
        Uri.Builder builder =
                Uri.parse(redirectUrl)
                        .buildUpon()
                        .appendQueryParameter(ApiV1.ResponseParams.CALL, ApiV1.CallType.CONNECT);

        if (!TextUtils.isEmpty(address))
        {
            builder.appendQueryParameter(ApiV1.ResponseParams.ADDRESS, address);
        }

        return builder.build();
    }

    @Override
    public String getCallType()
    {
        return ApiV1.CallType.CONNECT;
    }

    public String getAddress()
    {
        return address;
    }
}
