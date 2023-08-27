package com.alphawallet.token.entity;

/**
 * Created by JB on 15/03/2023.
 */
public enum AttestationValidationStatus
{
    Pass("Pass"),
    Expired("Attestation has Expired"),
    Issuer_Not_Valid("Untrusted Attestation Issuer"),
    Incorrect_Subject("Not Issued to this address"),
    ;

    private final String validationMessage;

    AttestationValidationStatus(String message)
    {
        this.validationMessage = message;
    }

    public String getValue()
    {
        return validationMessage;
    }
}
