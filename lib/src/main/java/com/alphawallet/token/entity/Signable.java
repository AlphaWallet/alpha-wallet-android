package com.alphawallet.token.entity;

/**
 * Interface for Singable data, for stuff like TBSData (to-be-signed-data), with the view that
 * EthereumMessage, EthereumTypedMessage, EthereumTransaction, X.509 message (attestations)
 * etc eventually use from this
 * Weiwu, Aug 2020
*/

public interface Signable {
    String getMessage();
    long getCallbackId();
    byte[] getPrehash();
    String getOrigin();
    CharSequence getUserMessage();
    SignMessageType getMessageType();
}
