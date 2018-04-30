package io.stormbird.token.entity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 10/02/2018.
 */

/**
 * This should purely be a container class of NonFungibleToken
 *
 */
public class TicketRange
{
    //public final int seatStart;
    //public int seatCount;
    public boolean isChecked;
    public String contractAddress; // Should this be address or actual token?
    public final boolean isBurned;

    public List<BigInteger> tokenIds;

    public TicketRange(BigInteger tokenId, String contractAddress)
    {
        this.contractAddress = contractAddress;
        tokenIds = new ArrayList<>();
        tokenIds.add(tokenId);
        this.isChecked = false;
        this.isBurned = false;
    }

    public TicketRange(BigInteger tokenId, String contractAddress, boolean isBurned)
    {
        this.contractAddress = contractAddress;
        tokenIds = new ArrayList<>();
        tokenIds.add(tokenId);
        this.isChecked = false;
        this.isBurned = isBurned;
    }

    public void selectSubRange(int count)
    {
        if (count < tokenIds.size())
        {
            tokenIds = tokenIds.subList(0, count);
        }
    }
}
