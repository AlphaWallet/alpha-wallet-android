package com.alphawallet.app.entity.cryptokeys;

/**
 * Created by JB on 9/02/2020.
 */

/**
 * SignatureFromKey is the returned structure from an attempt to sign a message using a user private key
 * It can be a success, or it can be a fail type
 */
public class SignatureFromKey
{
    public byte[] signature;
    public SignatureReturnType sigType;
    public String failMessage;
}
