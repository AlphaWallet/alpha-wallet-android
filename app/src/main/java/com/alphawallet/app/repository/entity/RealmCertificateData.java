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
        result = sig.result;
        subject = sig.subject;
        keyName = sig.keyName;
        keyType = sig.keyType;
        issuer = sig.issuer;
        certificateName = sig.certificateName;
        if (sig.type == null)
        {
            if (sig.result != null && sig.result.equals("pass")) type = SigReturnType.SIGNATURE_PASS.ordinal();
            else type = SigReturnType.SIGNATURE_INVALID.ordinal();
        }
        else
        {
            type = sig.type.ordinal();
        }
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public String getKeyName()
    {
        return keyName;
    }

    public void setKeyName(String keyName)
    {
        this.keyName = keyName;
    }

    public String getSubject()
    {
        return subject;
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
        this.type = type.ordinal();
    }

    public String getIssuer()
    {
        return issuer;
    }

    public void setIssuer(String issuer)
    {
        this.issuer = issuer;
    }

    public String getCertificateName()
    {
        return certificateName;
    }

    public void setCertificateName(String certificateName)
    {
        this.certificateName = certificateName;
    }

    public String getKeyType()
    {
        return keyType;
    }

    public void setKeyType(String keyType)
    {
        this.keyType = keyType;
    }
}
