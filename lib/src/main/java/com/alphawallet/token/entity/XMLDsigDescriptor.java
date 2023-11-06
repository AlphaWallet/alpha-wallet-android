package com.alphawallet.token.entity;

public class XMLDsigDescriptor
{
    private static final String EIP5169_CERTIFIER = "Smart Token Labs";
    private static final String EIP5169_KEY_OWNER = "Contract Owner"; //TODO Source this from the contract via owner()
    private static final String MATCHES_DEPLOYER = "matches the contract deployer";

    public String result;
    public String subject;
    public String keyName;
    public String keyType;
    public String issuer;
    public SigReturnType type;
    public String certificateName = null;

    public XMLDsigDescriptor()
    {

    }

    public XMLDsigDescriptor(String issuerText)
    {
        issuer = issuerText;
        certificateName = EIP5169_CERTIFIER;
        keyName = EIP5169_KEY_OWNER;
        keyType = "ECDSA";
        result = "Pass";
        subject = "";
        type = SigReturnType.SIGNATURE_PASS;
    }

    public void setKeyDetails(boolean isDebug, String failKeyName)
    {
        if (pass())
        {
            if (isDebug)
            {
                this.type = SigReturnType.DEBUG_SIGNATURE_PASS;
            }
            else
            {
                this.type = SigReturnType.SIGNATURE_PASS;
            }

            if (this.certificateName != null && this.certificateName.contains(MATCHES_DEPLOYER))
            {
                this.keyName = EIP5169_KEY_OWNER;
            }
        }
        else
        {
            setFailedIssuer(isDebug, failKeyName);
        }
    }

    public boolean pass()
    {
        return this.result != null && (this.result.equals("pass") || this.result.equals("valid"));
    }

    private void setFailedIssuer(boolean isDebug, String failKeyName)
    {
        this.keyName = failKeyName;
        if (this.subject != null && this.subject.contains("Invalid"))
        {
            if (isDebug)
                this.type = SigReturnType.DEBUG_SIGNATURE_INVALID;
            else
                this.type = SigReturnType.SIGNATURE_INVALID;
        }
        else
        {
            if (isDebug)
                this.type = SigReturnType.DEBUG_NO_SIGNATURE;
            else
                this.type = SigReturnType.NO_SIGNATURE;
        }
    }


}
