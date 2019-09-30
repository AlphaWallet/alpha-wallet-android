package com.alphawallet.token.entity;

import java.math.BigInteger;
import java.security.SignatureException;

public interface CryptoFunctionsInterface
{
    byte[] Base64Decode(String message);
    byte[] Base64Encode(byte[] data);
    BigInteger signedMessageToKey(byte[] data, byte[] signature) throws SignatureException;
    String getAddressFromKey(BigInteger recoveredKey);
}
