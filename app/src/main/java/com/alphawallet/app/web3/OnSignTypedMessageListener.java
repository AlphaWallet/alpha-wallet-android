package com.alphawallet.app.web3;


import com.alphawallet.token.entity.EthereumTypedMessage;

public interface OnSignTypedMessageListener {
    void onSignTypedMessage(EthereumTypedMessage message);
}
