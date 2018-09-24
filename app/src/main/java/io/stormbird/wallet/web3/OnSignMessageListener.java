package io.stormbird.wallet.web3;

import io.stormbird.wallet.web3.entity.Message;

public interface OnSignMessageListener {
    void onSignMessage(Message<String> message);
}
