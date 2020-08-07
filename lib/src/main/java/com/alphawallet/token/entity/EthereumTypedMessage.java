package com.alphawallet.token.entity;

public class EthereumTypedMessage implements Signable {

    TypedData[] value;
    String displayOrigin;
    long leafPosition;

    public EthereumTypedMessage(TypedData[] value, String displayOrigin, long leafPosition) {
        this.value = value;
        this.displayOrigin = displayOrigin;
        this.leafPosition = leafPosition;
    }

    // TODO: the message before adding "Ethereum Signed Message" prefix
    public String getMessage() {
        return null;
    }

    public long getCallbackId() {
        return this.leafPosition;
    }

    public static class TypedData  {
        public final String name;
        public final String type;
        public final Object data;

        public TypedData(String name, String type, Object data) {
            this.name = name;
            this.type = type;
            this.data = data;
        }
    }

    public byte[] getPrehash() {
        return EthereumMessage.getEthereumMessage(getMessage().getBytes());
    }
}