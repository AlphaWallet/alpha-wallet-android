package com.alphawallet.hardware;

/**
 * Created by JB on 9/02/2020.
 */
public enum SignatureReturnType
{
    SIGNATURE_GENERATED,
    KEY_FILE_ERROR,
    KEY_AUTHENTICATION_ERROR,
    KEY_CIPHER_ERROR,
    SIGNING_POSTPONED,
}
