package com.alphawallet.token.entity;

/* bytes to be signed without Ethereum Signed Message prefix */
public class SignableBytes implements Signable{

    private byte[] value;
    public SignableBytes(byte[] value) {
        this.value = value;
    }
    @Override
    public byte[] getPrehash() {
        return this.value;
    }

    // TODO: weiwu: refactor this from a requirement of Signable eventually
    @Override
    public String getMessage() {
        return null;
    }

    // TODO: weiwu: remove this from a Signable eventually.
    @Override
    public long getCallbackId() {
        return 0;
    }

    @Override
    public String getOrigin()
    {
        return null;
    }

    @Override
    public CharSequence getUserMessage() {
        return "";
    }

    @Override
    public SignMessageType getMessageType()
    {
        return SignMessageType.SIGN_MESSAGE;
    }
}
