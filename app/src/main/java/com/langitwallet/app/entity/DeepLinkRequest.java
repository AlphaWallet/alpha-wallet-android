package com.langitwallet.app.entity;

public class DeepLinkRequest
{
    public DeepLinkType type;
    public final String data;

    public DeepLinkRequest(DeepLinkType type, String data)
    {
        this.type = type;
        this.data = data;
    }
}
