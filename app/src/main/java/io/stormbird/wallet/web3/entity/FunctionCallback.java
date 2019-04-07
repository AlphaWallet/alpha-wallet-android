package io.stormbird.wallet.web3.entity;

import io.stormbird.wallet.entity.DAppFunction;

/**
 * Created by James on 6/04/2019.
 * Stormbird in Singapore
 */
public interface FunctionCallback
{
    void signMessage(byte[] sign, DAppFunction dAppFunction, Message<String> message);
}
