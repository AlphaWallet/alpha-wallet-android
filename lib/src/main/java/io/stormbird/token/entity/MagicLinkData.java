package io.stormbird.token.entity;


import java.math.BigInteger;

public class MagicLinkData
{
    public long expiry;
    public double price;
    public BigInteger priceWei;
    public int[] tickets;
    public int ticketStart;
    public int ticketCount;
    public String contractAddress;
    public byte[] signature = new byte[65];
    public byte[] message;
    public String ownerAddress;
}
