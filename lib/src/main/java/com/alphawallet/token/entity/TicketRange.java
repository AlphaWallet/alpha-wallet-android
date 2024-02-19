package com.alphawallet.token.entity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
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

    public final List<BigInteger> tokenIds;
    public final BigInteger balance; //in wei for ERC20
    public final boolean isERC20;

    public TicketRange(BigInteger balance)
    {
        this.balance = balance;
        this.isERC20 = true;
        this.tokenIds = new ArrayList<>(Collections.singleton(BigInteger.ZERO));
        this.contractAddress = null;
    }

    public TicketRange(BigInteger tokenId, String contractAddress)
    {
        this.contractAddress = contractAddress;
        tokenIds = new ArrayList<>();
        tokenIds.add(tokenId);
        this.isChecked = false;
        this.exposeRadio = false;
        this.balance = BigInteger.ONE;
        this.isERC20 = false;
    }

    public TicketRange(List<BigInteger> tokenIds, String contractAddress, boolean isChecked)
    {
        this.contractAddress = contractAddress;
        this.tokenIds = tokenIds;
        this.isChecked = isChecked;
        this.exposeRadio = false;
        this.balance = BigInteger.valueOf(tokenIds.size());
        this.isERC20 = false;
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
