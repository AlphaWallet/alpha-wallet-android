package com.alphawallet.app.entity;


public class EasAttestation
{
    public String version;
    public long chainId;
    public String verifyingContract;
    public String r;
    public String s;
    public long v;
    public String recipient;
    public String uid;
    public String schema;
    public String signer;
    public long time;
    public long expirationTime;
    public String refUID;
    public boolean revocable;
    public String data;
    public long nonce;

    public EasAttestation(String version, long chainId, String verifyingContract, String r, String s, long v, String recipient, String uid, String schema, String signer, long time, long expirationTime, String refUID, boolean revocable, String data, long nonce)
    {
        this.version = version;
        this.chainId = chainId;
        this.verifyingContract = verifyingContract;
        this.r = r;
        this.s = s;
        this.v = v;
        this.recipient = recipient;
        this.uid = uid;
        this.schema = schema;
        this.signer = signer;
        this.time = time;
        this.expirationTime = expirationTime;
        this.refUID = refUID;
        this.revocable = revocable;
        this.data = data;
        this.nonce = nonce;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public long getChainId()
    {
        return chainId;
    }

    public void setChainId(long chainId)
    {
        this.chainId = chainId;
    }

    public String getVerifyingContract()
    {
        return verifyingContract;
    }

    public void setVerifyingContract(String verifyingContract)
    {
        this.verifyingContract = verifyingContract;
    }

    public String getR()
    {
        return r;
    }

    public void setR(String r)
    {
        this.r = r;
    }

    public String getS()
    {
        return s;
    }

    public void setS(String s)
    {
        this.s = s;
    }

    public long getV()
    {
        return v;
    }

    public void setV(long v)
    {
        this.v = v;
    }

    public String getRecipient()
    {
        return recipient;
    }

    public void setRecipient(String recipient)
    {
        this.recipient = recipient;
    }

    public String getUid()
    {
        return uid;
    }

    public void setUid(String uid)
    {
        this.uid = uid;
    }

    public String getSchema()
    {
        return schema;
    }

    public void setSchema(String schema)
    {
        this.schema = schema;
    }

    public String getSigner()
    {
        return signer;
    }

    public void setSigner(String signer)
    {
        this.signer = signer;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public long getExpirationTime()
    {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime)
    {
        this.expirationTime = expirationTime;
    }

    public String getRefUID()
    {
        return refUID;
    }

    public void setRefUID(String refUID)
    {
        this.refUID = refUID;
    }

    public boolean isRevocable()
    {
        return revocable;
    }

    public void setRevocable(boolean revocable)
    {
        this.revocable = revocable;
    }

    public String getData()
    {
        return data;
    }

    public void setData(String data)
    {
        this.data = data;
    }

    public long getNonce()
    {
        return nonce;
    }

    public void setNonce(long nonce)
    {
        this.nonce = nonce;
    }
}