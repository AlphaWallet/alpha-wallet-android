package com.wallet.crypto.alphawallet.entity;

/**
 * Created by James on 18/03/2018.
 */

import java.util.List;

/**
 * This stores a contract address -> owner address pair
 * It's used to optimise checking balances of market orders
 * A token owner could have a lot of different tokens for sale from the same contract
 * It's unnecessary to repeat a balance check, which is quite expensive in terms of list update time
 */
public class OrderContractAddressTrie
{
    public Token orderToken;
    public String owningAddress;
    public SalesOrder order;

    public boolean equals(Token t, String address)
    {
        return (this.orderToken.getAddress().equals(t.getAddress()) && this.owningAddress.equals(address));
    }

    public static boolean addPair(List<OrderContractAddressTrie> checkList, Token t, String address) {
        boolean foundPair = false;
        for (OrderContractAddressTrie pair : checkList)
        {
            if (pair.equals(t, address)) {
                foundPair = true;
                break;
            }
        }

        if (!foundPair)
        {
            OrderContractAddressTrie pair = new OrderContractAddressTrie();
            pair.orderToken = t;
            pair.owningAddress = address;
            checkList.add(pair);
            return true;
        }
        else
        {
            return false;
        }
    }
}
