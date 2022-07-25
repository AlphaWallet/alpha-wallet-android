package com.alphawallet.app.api.v1.entity.response;

import android.net.Uri;
import android.text.TextUtils;

import com.alphawallet.app.api.v1.entity.ApiV1;

public class SignPersonalMessageResponse extends ApiV1Response
{
    private final String signature;

    public SignPersonalMessageResponse(String redirectUrl, String signature)
    {
        super(redirectUrl);
        this.signature = signature;
    }

    @Override
    public Uri uri()
    {
        Uri.Builder builder =
                Uri.parse(redirectUrl)
                        .buildUpon()
                        .appendQueryParameter(ApiV1.ResponseParams.CALL, ApiV1.CallType.SIGN_PERSONAL_MESSAGE);

        if (!TextUtils.isEmpty(signature))
        {
            builder.appendQueryParameter(ApiV1.ResponseParams.SIGNATURE, signature);
        }

        return builder.build();
    }

    @Override
    public String getCallType()
    {
        return ApiV1.CallType.SIGN_PERSONAL_MESSAGE;
    }

    public String getSignature()
    {
        return signature;
    }
}
