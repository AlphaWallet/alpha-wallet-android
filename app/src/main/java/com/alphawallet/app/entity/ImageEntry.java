package com.alphawallet.app.entity;

public class ImageEntry
{
    final public long chainId;
    final public String address;
    final public String imageUrl;

    public ImageEntry(long networkId, String address, String imageUrl)
    {
        this.chainId = networkId;
        this.address = address;
        this.imageUrl = imageUrl;
    }
}
