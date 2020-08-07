package com.alphawallet.token.entity;

import com.alphawallet.token.tools.Numeric;

/**
 * Class for EthereumMessages to be sigden.
 * Weiwu, Aug 2020
*/
public class EthereumMessage implements Signable {

    public final byte[] messageBytes;
    public final String message;
    public final String displayOrigin;
    public final long leafPosition;
    public final byte[] prehash;
    public static final String MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";

    public EthereumMessage(String message, String displayOrigin, long leafPosition) {


        /* this logic certainly have edge conditions, but such a
         * problem is probably inherited from web3.js which might only
         * passes binary data to be signed in string format with no
         * other indicator than the leading 0x.  Anyway, the logic was
         * like this before I encapsulate this portion of code - Weiwu
         */
        if (message.substring(0, 2).equals("0x"))
        {
            messageBytes = Numeric.hexStringToByteArray(message);
        } else {
            messageBytes = message.getBytes();
        }

        this.message = message;
        this.displayOrigin = displayOrigin;
        this.leafPosition = leafPosition;
        this.prehash = getEthereumMessage(messageBytes);
    }

    static byte[] getEthereumMessage(byte[] message) {
        byte[] prefix = MESSAGE_PREFIX.concat(String.valueOf(message.length)).getBytes();
        byte[] result = new byte[prefix.length + message.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(message, 0, result, prefix.length, message.length);
        return result;
    }
    public String getMessage() {
        return this.message;
    }

    public byte[] getPrehash() {
        return this.prehash;
    }

    public long getCallbackId() {
        return this.leafPosition;
    }
}
