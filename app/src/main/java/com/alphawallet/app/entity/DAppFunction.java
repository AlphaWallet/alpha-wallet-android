package com.alphawallet.app.entity;

import com.alphawallet.token.entity.EthereumMessage;

public interface DAppFunction
{
    void DAppError(Throwable error, EthereumMessage message);
    void DAppReturn(byte[] data, EthereumMessage message);
}
