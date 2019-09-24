package com.alphawallet.app.web3;

import com.alphawallet.app.web3.entity.Message;

public interface OnSignMessageListener {
    void onSignMessage(Message<String> message);
}
