package com.alphawallet.app.repository.entity;

import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.XMLDsigDescriptor;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by JB on 4/02/2020.
 */
public class RealmCertificateData extends RealmObject
{
    @PrimaryKey
    private String instanceKey; //File hash
    private String result;
    private String subject;
    private String keyName;
    private String keyType;
    private String issuer;
    private String certificateName;
    private int type;

    public void setFromSig(XMLDsigDescriptor sig)
    {
        this.result = sig.result;
        this.subject = sig.subject;
        this.keyName = sig.keyName;
        this.keyType = sig.keyType;
        this.issuer = sig.issuer;
        this.certificateName = sig.certificateName;
        if (sig.type == null)
        {
            if (sig.result != null && sig.result.equals("pass")) type = SigReturnType.SIGNATURE_PASS.ordinal();
            else this.type = SigReturnType.SIGNATURE_INVALID.ordinal();
        }
        else
        {
            this.type = sig.type.ordinal();
        }
    }

    public XMLDsigDescriptor getDsigObject()
    {
        XMLDsigDescriptor sig = new XMLDsigDescriptor();
        sig.issuer = this.issuer;
        sig.certificateName = this.certificateName;
        sig.keyName = this.keyName;
        sig.keyType = this.keyType;
        sig.result = this.result;
        sig.subject = this.subject;
        sig.type = SigReturnType.values()[this.type];

        return sig;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public String getSubject()
    {
        return this.subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public SigReturnType getType()
    {
        return SigReturnType.values()[type];
    }

    public void setType(SigReturnType type)
    {
        if (type == null && this.result.equals("pass"))
        {
            this.type = type.ordinal();
        }
    }

    public String getIssuer()
    {
        return issuer;
    }

    public void setIssuer(String issuer)
    {
        this.issuer = issuer;
    }
}
