package com.alphawallet.app.walletconnect;

import com.alphawallet.app.walletconnect.entity.BaseRequest;
import com.alphawallet.app.walletconnect.entity.WCEthereumSignMessage;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;

public class SignRequest extends BaseRequest
{
    public SignRequest(String params)
    {
        super(params, WCEthereumSignMessage.WCSignType.MESSAGE);
    }

    @Override
    public Signable getSignable()
    {
        return new EthereumMessage(getMessage(), "", 0, SignMessageType.SIGN_MESSAGE);
    }

    @Override
    public String getWalletAddress()
    {
        return params.get(0);
    }
}
