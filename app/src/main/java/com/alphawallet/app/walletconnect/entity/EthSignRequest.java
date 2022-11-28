package com.alphawallet.app.walletconnect.entity;

import com.walletconnect.sign.client.Sign;

/**
 * Created by JB on 21/11/2022.
 */
public abstract class EthSignRequest
{
    public static BaseRequest getSignRequest(Sign.Model.SessionRequest sessionRequest)
    {
        BaseRequest signRequest = null;

        switch (sessionRequest.getRequest().getMethod())
        {
            case "eth_sign":
                // see https://docs.walletconnect.org/json-rpc-api-methods/ethereum
                // WalletConnect shouldn't provide access to deprecated eth_sign, as it can be used to scam people
                signRequest = new SignRequest(sessionRequest.getRequest().getParams());
                break;
            case "personal_sign":
                signRequest = new SignPersonalMessageRequest(sessionRequest.getRequest().getParams());
                break;
            case "eth_signTypedData":
                signRequest = new SignTypedDataRequest(sessionRequest.getRequest().getParams());
                break;
            default:
                break;
        }

        return signRequest;
    }
}
