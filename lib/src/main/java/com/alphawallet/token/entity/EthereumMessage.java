package com.alphawallet.token.entity;

import com.alphawallet.token.tools.Numeric;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import static com.alphawallet.token.tools.Numeric.cleanHexPrefix;

/**
 * Class for EthereumMessages to be signed.
 * Weiwu, Aug 2020
*/
public class EthereumMessage implements Signable {

    public final byte[] messageBytes;
    private final CharSequence userMessage;
    public final String displayOrigin;
    public final long leafPosition;
    public final byte[] prehash; //this could be supplied on-demand
    public static final String MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";
    private final SignMessageType messageType;

    public EthereumMessage(String message, String displayOrigin, long leafPosition, SignMessageType type) {
        this.messageBytes = Numeric.hexStringToByteArray(message);
        this.displayOrigin = displayOrigin;
        this.leafPosition = leafPosition;
        this.prehash = getEthereumMessage(messageBytes);
        this.userMessage = message;
        messageType = type;
    }

    static byte[] getEthereumMessage(byte[] message) {
        byte[] prefix = MESSAGE_PREFIX.concat(String.valueOf(message.length)).getBytes();
        byte[] result = new byte[prefix.length + message.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(message, 0, result, prefix.length, message.length);
        return result;
    }

    @Override
    public String getMessage()
    {
        return this.userMessage.toString();
    }

    @Override
    public CharSequence getUserMessage()
    {
        if (messageType != SignMessageType.SIGN_PERSONAL_MESSAGE || !StandardCharsets.UTF_8.newEncoder().canEncode(userMessage))
        {
            return userMessage;
        }
        else
        {
            return hexToUtf8(userMessage);
        }
    }

    public byte[] getPrehash() {
        return this.prehash;
    }

    @Override
    public String getOrigin()
    {
        return displayOrigin;
    }

    public long getCallbackId() {
        return this.leafPosition;
    }

    private String hexToUtf8(CharSequence hexData) {
        String hex = cleanHexPrefix(hexData.toString());
        ByteBuffer buff = ByteBuffer.allocate(hex.length() / 2);
        for (int i = 0; i < hex.length(); i += 2) {
            buff.put((byte) Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        buff.rewind();
        CharBuffer cb = StandardCharsets.UTF_8.decode(buff);
        return cb.toString();
    }

    @Override
    public SignMessageType getMessageType()
    {
        return messageType;
    }
}
