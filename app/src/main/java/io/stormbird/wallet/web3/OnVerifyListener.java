package io.stormbird.wallet.web3;

import io.stormbird.wallet.web3.entity.Message;
import io.stormbird.wallet.web3.entity.TypedData;

public interface OnVerifyListener {
    void onSignTypedMessage(Message<TypedData[]> message);
}
