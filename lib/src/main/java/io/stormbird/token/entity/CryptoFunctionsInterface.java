package io.stormbird.token.entity;

import java.math.BigInteger;

public interface CryptoFunctionsInterface
{
    byte[] Base64Decode(String message);
    byte[] Base64Encode(byte[] data);
    BigInteger signedMessageToKey(byte[] data, byte[] signature);
    String getAddressFromKey(BigInteger recoveredKey);
}
