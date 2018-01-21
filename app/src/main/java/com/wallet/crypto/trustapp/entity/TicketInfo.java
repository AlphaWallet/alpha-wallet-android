package com.wallet.crypto.trustapp.entity;


import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Created by James on 20/01/2018.
 */

public class TicketInfo extends TokenInfo {
    public final String venue;
    public final String date;
    public final double price;

    public TicketInfo(TokenInfo ti, String venue, String date, double price)
    {
        super(ti.address, ti.name, ti.symbol, ti.decimals);
        this.venue = venue;
        this.date = date;
        this.price = price;
    }
}
