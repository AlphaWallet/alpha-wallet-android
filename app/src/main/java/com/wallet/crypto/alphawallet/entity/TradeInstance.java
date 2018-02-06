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
    final byte[] tradeData;
    byte[] signature;

    public TradeInstance(BigInteger price, BigInteger expiry, short[] tickets, Token ticket, byte[] tradeData) {
        this.price = price;
        this.expiry = expiry;
        this.tickets = tickets;
        this.contractAddress = ticket.getAddress();
        this.tradeData = tradeData;
    }

    public TradeInstance addSignature(byte[] sig) {
        this.signature = sig;
        return this;
    }

    public byte[] getTradeData() {
        return tradeData;
    }
}
