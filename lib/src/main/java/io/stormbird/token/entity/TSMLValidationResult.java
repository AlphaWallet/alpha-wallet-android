package io.stormbird.token.entity;

/**
 * Created by James on 19/04/2019.
 * Stormbird in Sydney
 */
public class TSMLValidationResult
{
    public boolean isValid;
    public String keyName;
    public String issuerPrincipal;
    public String subjectPrincipal;
    public String keyType;
    public String failureReason;

    public TSMLValidationResult()
    {
        isValid = false;
        keyName = "";
        issuerPrincipal = "";
        subjectPrincipal = "";
        keyType = "";
    }
}
