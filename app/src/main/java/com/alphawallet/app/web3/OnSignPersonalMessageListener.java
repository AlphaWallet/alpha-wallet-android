package com.alphawallet.app.web3;

import com.alphawallet.app.web3.entity.Message;

public interface OnSignPersonalMessageListener {
    void onSignPersonalMessage(Message<String> message);
}
