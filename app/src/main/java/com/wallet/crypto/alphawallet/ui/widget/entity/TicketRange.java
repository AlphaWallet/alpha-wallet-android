package com.wallet.crypto.alphawallet.ui.widget.entity;

/**
 * Created by James on 10/02/2018.
 */

public class TicketRange
{
    public final int seatStart;
    public int seatCount;

    public final int tokenId;

    public TicketRange(int tokenId, int seatStart)
    {
        this.tokenId = tokenId;
        this.seatStart = seatStart;
        this.seatCount = 1;
    }
}
