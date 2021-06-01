package com.alphawallet.token.entity;

import com.alphawallet.token.tools.Numeric;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import static com.alphawallet.token.tools.Numeric.cleanHexPrefix;

/**
 * Class for EthereumMessages to be signed.
 * Weiwu, Aug 2020
*/
public class EthereumMessage implements Signable {

    private final CharSequence userMessage;
    public final String displayOrigin;
    public final long leafPosition;
    public final byte[] prehash; //this could be supplied on-demand
    public static final String MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";
    private final SignMessageType messageType;

    public EthereumMessage(String message, String displayOrigin, long leafPosition, SignMessageType type) {
        this.displayOrigin = displayOrigin;
        this.leafPosition = leafPosition;
        this.messageType = type;
        this.prehash = getEthereumMessage(message);
        this.userMessage = message;
    }

    private byte[] getEthereumMessage(String message) {
        byte[] encodedMessage;
        if (isHex(message))
        {
            encodedMessage = Numeric.hexStringToByteArray(message);
        }
        else
        {
            encodedMessage = message.getBytes();
        }

        byte[] result;
        if (messageType == SignMessageType.SIGN_PERSONAL_MESSAGE)
        {
            byte[] prefix = getEthereumMessagePrefix(encodedMessage.length);

            result = new byte[prefix.length + encodedMessage.length];
            System.arraycopy(prefix, 0, result, 0, prefix.length);
            System.arraycopy(encodedMessage, 0, result, prefix.length, encodedMessage.length);
        }
        else
        {
            result = encodedMessage;
        }
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
        if (!StandardCharsets.UTF_8.newEncoder().canEncode(userMessage))
        {
            return userMessage;
        }
        else
        {
            try
            {
                return hexToUtf8(userMessage);
            }
            catch (NumberFormatException e)
            {
                return userMessage;
            }
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

    @Override
    public SignMessageType getMessageType()
    {
        return messageType;
    }

    private String hexToUtf8(CharSequence hexData) {
        String hex = cleanHexPrefix(hexData.toString());
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        for (int i = 0; i < hex.length(); i += 2) {
            byteBuffer.write((byte) Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        CharBuffer cb = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(byteBuffer.toByteArray()));
        return cb.toString();
    }

    private boolean isHex(String testMsg)
    {
        if (testMsg == null || testMsg.length() == 0) return false;
        testMsg = Numeric.cleanHexPrefix(testMsg);

        for (int i = 0; i < testMsg.length(); i++)
        {
            if (Character.digit(testMsg.charAt(i), 16) == -1) { return false; }
        }

        return true;
    }

    private byte[] getEthereumMessagePrefix(int messageLength) {
        return MESSAGE_PREFIX.concat(String.valueOf(messageLength)).getBytes();
    }
}
