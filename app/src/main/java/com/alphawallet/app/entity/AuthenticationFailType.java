package com.alphawallet.app.entity;

/**
 * Created by James on 20/07/2019.
 * Stormbird in Sydney
 */
public enum AuthenticationFailType
{
    AUTHENTICATION_DIALOG_CANCELLED,
    FINGERPRINT_NOT_VALIDATED,
    PIN_FAILED,
    DEVICE_NOT_SECURE,
    BIOMETRIC_AUTHENTICATION_NOT_AVAILABLE,
    FINGERPRINT_ERROR_CANCELED
}
