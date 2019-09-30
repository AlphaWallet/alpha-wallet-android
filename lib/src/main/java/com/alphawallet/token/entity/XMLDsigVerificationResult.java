package com.alphawallet.token.entity;

/**
 * Created by James on 19/04/2019.
 * Stormbird in Sydney
 */
public class XMLDsigVerificationResult
{
    public boolean isValid;
    public String keyName;
    public String issuerPrincipal;
    public String subjectPrincipal;
    public String keyType;
    public String failureReason;

    public XMLDsigVerificationResult()
    {
        isValid = false;
        keyName = "";
        issuerPrincipal = "";
        subjectPrincipal = "";
        keyType = "";
    }
}
