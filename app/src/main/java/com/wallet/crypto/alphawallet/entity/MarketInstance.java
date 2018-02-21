package com.wallet.crypto.alphawallet.entity;

import android.util.Base64;

import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 21/02/2018.
 */

public class MarketInstance
{
    public final long expiry;
    public final double price;
    public final int[] tickets;
    public final int ticketStart;
    public final int ticketCount;
    public final String contractAddress;
    public final byte[] signature;
    public final byte[] message;

    public MarketInstance(double price, long expiry, int ticketStart, int ticketCount, String contractAddress, String sig, String msg) {
        this.price = price;
        this.expiry = expiry;
        this.ticketStart = ticketStart;
        this.ticketCount = ticketCount;

        this.tickets = new int[ticketCount];
        for (int i = 0; i < ticketCount; i++)
        {
            this.tickets[i] = ticketStart;
            ticketStart++;
        }

        this.contractAddress = contractAddress;
        this.signature = Base64.decode(sig, Base64.DEFAULT);
        this.message = Base64.decode(msg, Base64.DEFAULT);
    }
}
