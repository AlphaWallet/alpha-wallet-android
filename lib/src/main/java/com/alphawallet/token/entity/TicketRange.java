package com.alphawallet.token.entity;

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
    public boolean isChecked;
    public boolean exposeRadio;
    public String contractAddress; // Should this be address or actual token?

    public List<BigInteger> tokenIds;

    public TicketRange(BigInteger tokenId, String contractAddress)
    {
        this.contractAddress = contractAddress;
        tokenIds = new ArrayList<>();
        tokenIds.add(tokenId);
        this.isChecked = false;
        this.exposeRadio = false;
    }

    public TicketRange(List<BigInteger> tokenIds, String contractAddress, boolean isChecked)
    {
        this.contractAddress = contractAddress;
        this.tokenIds = tokenIds;
        this.isChecked = isChecked;
        this.exposeRadio = false;
    }

    public void selectSubRange(int count)
    {
        if (count < tokenIds.size())
        {
            tokenIds = tokenIds.subList(0, count);
        }
    }

    public boolean equals(TicketRange compare)
    {
        if (compare == null || compare.tokenIds.size() != tokenIds.size()) return false;
        for (int i = 0; i < tokenIds.size(); i++)
        {
            BigInteger id = tokenIds.get(i);
            if (!id.equals(compare.tokenIds.get(i))) return false;
        }

        return true;
    }
}
