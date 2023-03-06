package com.alphawallet.token.entity;

import java.math.BigInteger;
import java.security.SignatureException;

public interface CryptoFunctionsInterface
{
    byte[] Base64Decode(String message);

    byte[] Base64Encode(byte[] data);

    BigInteger signedMessageToKey(byte[] data, byte[] signature) throws SignatureException;

    String getAddressFromKey(BigInteger recoveredKey);

    byte[] keccak256(byte[] message);

    /**
     * See class Utils: Uses Android text formatting
     */
    CharSequence formatTypedMessage(ProviderTypedData[] rawData);

    /**
     * See class Utils: Uses web3j: you need to provide this function to decode EIP712.
     * -- Currently web3j uses a different library for Android and Generic Java packages.
     * -- One day web3j could be united, then we can remove these functions
     */
    CharSequence formatEIP721Message(String messageData);

    /**
     * See class Utils: Uses web3j
     */
    byte[] getStructuredData(String messageData);

    long getChainId(String messageData);
}
