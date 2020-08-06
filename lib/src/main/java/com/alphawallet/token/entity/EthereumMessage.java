package com.alphawallet.token.entity;

/**
 * Class for EthereumMessages to be sigden.
 * Weiwu, Aug 2020
*/
public class EthereumMessage implements Signable {

    public final String value;
    public final String message;
    public final String displayOrigin;
    public final long leafPosition;

    public EthereumMessage(String message, String displayOrigin, long leafPosition) {
        // for EthereumMessage, both are the same
        this.value = message;
        this.message = message;
        this.displayOrigin = displayOrigin;
        this.leafPosition = leafPosition;
    }

    public String getMessage() {
        return this.message;
    }

    public long getCallbackId() {
        return this.leafPosition;
    }
}
