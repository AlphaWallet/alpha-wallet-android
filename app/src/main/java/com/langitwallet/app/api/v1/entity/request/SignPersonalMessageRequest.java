package com.langitwallet.app.api.v1.entity.request;

import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;
import com.langitwallet.app.api.v1.entity.ApiV1;

public class SignPersonalMessageRequest extends ApiV1Request
{
    private final String address;
    private final String message;

    public SignPersonalMessageRequest(String requestUrl)
    {
        super(requestUrl);
        this.message = this.requestUrl.queryParameter(ApiV1.RequestParams.MESSAGE);
        this.address = this.requestUrl.queryParameter(ApiV1.RequestParams.ADDRESS);
    }

    public String getMessage()
    {
        return message;
    }

    public String getAddress()
    {
        return address;
    }

    public Signable getSignable()
    {
        return new EthereumMessage(
                message,
                metadata.name,
                -1,
                SignMessageType.SIGN_PERSONAL_MESSAGE);
    }
}
