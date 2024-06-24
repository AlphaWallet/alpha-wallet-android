package com.langitwallet.app.entity.okx;

public class OkTokenCheck
{
    public final long chainId;
    public final OkProtocolType type;

    public OkTokenCheck(long chainId, OkProtocolType type)
    {
        this.chainId = chainId;
        this.type = type;
    }
}
