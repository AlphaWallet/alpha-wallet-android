package com.alphawallet.token.entity;

public class EthereumTypedMessage implements Signable {

    byte[] structuredData;
    String displayOrigin;
    long leafPosition;
    CharSequence userMessage;

    public EthereumTypedMessage(byte[] value, CharSequence userMessage, String displayOrigin, long leafPosition) {
        this.structuredData = value;
        this.displayOrigin = displayOrigin;
        this.leafPosition = leafPosition;
        this.userMessage = userMessage;
    }

    // User message is the text shown in the popup window - note CharSequence is used because message contains text formatting
    public CharSequence getUserMessage() {
        return userMessage;
    }

    public long getCallbackId() {
        return this.leafPosition;
    }

    public byte[] getPrehash() {
        return structuredData;
    }

    @Override
    public String getOrigin()
    {
        return displayOrigin;
    }

    @Override
    public String getMessage()
    {
        return null;
    }
}