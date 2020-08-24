package com.alphawallet.app.web3;

import com.alphawallet.token.entity.EthereumMessage;

public interface OnSignPersonalMessageListener {
    void onSignPersonalMessage(EthereumMessage message);
}
