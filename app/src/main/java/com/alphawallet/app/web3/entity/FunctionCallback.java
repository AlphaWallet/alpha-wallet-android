package com.alphawallet.app.web3.entity;

import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.token.entity.EthereumMessage;

/**
 * Created by James on 6/04/2019.
 * Stormbird in Singapore
 */
public interface FunctionCallback
{
    void signMessage(byte[] sign, DAppFunction dAppFunction, EthereumMessage message);
    void functionSuccess();
    void functionFailed();
}
