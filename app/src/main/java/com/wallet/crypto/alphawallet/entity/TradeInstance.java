package com.wallet.crypto.alphawallet.entity;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by James on 5/02/2018.
 */

//TradeInstance((price, expiryTimestamp, tickets, ticket, tradeData, sig)
public class TradeInstance
{
    public final BigInteger expiry;
    public final BigInteger price;
    public final short[] tickets;
    public final String contractAddress;
    public final byte[] tradeData;
    byte[] signature;

    public TradeInstance(BigInteger price, BigInteger expiry, short[] tickets, Token ticket, byte[] tradeData) {
        this.price = price;
        this.expiry = expiry;
        this.tickets = tickets;
        this.contractAddress = ticket.getAddress();
        this.tradeData = tradeData;
    }

    public TradeInstance(TradeInstance t, byte[] sig) {
        this.price = t.price;
        this.expiry = t.expiry;
        this.tickets = t.tickets;
        this.contractAddress = t.contractAddress;
        this.tradeData = t.tradeData;
        this.signature = sig;
    }

    public TradeInstance addSignature(byte[] sig) {
        this.signature = sig;
        return this;
    }

    public byte[] getTradeData() {
        return tradeData;
    }

    public String getStringSig() {
        String sigStr = new String(signature);
        return sigStr;
    }

    public String getExpiryString() {
        long expire = expiry.longValue();
        Date date = new Date(expire*1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm z");
        //sdf.setTimeZone(TimeZone.getTimeZone("GMT-4"));
        String formattedDate = sdf.format(date);
        return formattedDate;
    }
}
