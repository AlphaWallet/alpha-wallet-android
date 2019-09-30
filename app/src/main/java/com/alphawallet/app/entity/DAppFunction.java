package com.alphawallet.app.entity;

import com.alphawallet.app.web3.entity.Message;

public interface DAppFunction
{
    void DAppError(Throwable error, Message<String> message);
    void DAppReturn(byte[] data, Message<String> message);
}
