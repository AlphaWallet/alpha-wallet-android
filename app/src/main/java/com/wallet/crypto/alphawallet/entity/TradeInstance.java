package com.wallet.crypto.alphawallet.entity;

import java.math.BigInteger;

/**
 * Created by James on 5/02/2018.
 */

//TradeInstance((price, expiryTimestamp, tickets, ticket, tradeData, sig)
public class TradeInstance
{
    final BigInteger expiry;
    final BigInteger price;
    final short[] tickets;
    final String contractAddress;
    final byte[] signature;
    final byte[] tradeData;

    public TradeInstance(BigInteger price, BigInteger expiry, short[] tickets, Token ticket, byte[] tradeData, byte[] sig)
    {
        this.price = price;
        this.expiry = expiry;
        this.tickets = tickets;
        this.contractAddress = ticket.getAddress();
        this.tradeData = tradeData;
        this.signature = sig;
    }
}
