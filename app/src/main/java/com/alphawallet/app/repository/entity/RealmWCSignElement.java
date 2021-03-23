package com.alphawallet.app.repository.entity;

import java.nio.charset.StandardCharsets;

import io.realm.RealmObject;

/**
 * Created by JB on 8/09/2020.
 */
public class RealmWCSignElement extends RealmObject
{
    private String sessionId;
    private byte[] signMessage;
    private long signTime;
    private String signType;

    public CharSequence getSignMessage()
    {
        return new String(signMessage, StandardCharsets.UTF_8);
    }

    public void setSignMessage(CharSequence msg)
    {
        signMessage = msg.toString().getBytes(StandardCharsets.UTF_8);
    }

    public String getSignType()
    {
        return signType;
    }

    public void setSignType(String signType)
    {
        this.signType = signType;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    public long getSignTime()
    {
        return signTime;
    }

    public void setSignTime(long signTime)
    {
        this.signTime = signTime;
    }
}