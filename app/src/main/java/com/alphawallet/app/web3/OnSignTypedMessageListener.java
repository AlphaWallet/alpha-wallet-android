package com.alphawallet.app.web3;

import com.alphawallet.app.web3.entity.Message;
import com.alphawallet.app.web3.entity.TypedData;

public interface OnSignTypedMessageListener {
    void onSignTypedMessage(Message<TypedData[]> message);
}
