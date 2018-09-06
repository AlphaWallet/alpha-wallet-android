package io.stormbird.wallet.web3;

import io.stormbird.wallet.web3.entity.Message;

public interface OnSignPersonalMessageListener {
    void onSignPersonalMessage(Message<String> message);
}
