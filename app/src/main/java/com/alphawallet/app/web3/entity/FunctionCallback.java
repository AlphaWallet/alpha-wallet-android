package com.alphawallet.app.web3.entity;

import com.alphawallet.app.entity.DAppFunction;

/**
 * Created by James on 6/04/2019.
 * Stormbird in Singapore
 */
public interface FunctionCallback
{
    void signMessage(byte[] sign, DAppFunction dAppFunction, Message<String> message);
    void functionSuccess();
    void functionFailed();
}
