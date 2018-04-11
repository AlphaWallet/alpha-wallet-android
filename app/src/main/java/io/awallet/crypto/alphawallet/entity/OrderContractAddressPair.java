package io.awallet.crypto.alphawallet.entity;

/**
 * Created by James on 18/03/2018.
 */

import org.web3j.abi.datatypes.generated.Bytes32;

import java.math.BigInteger;
import java.util.List;

/**
 * This stores a contract address -> owner address pair plus the received balance of the owner
 * Note that the pair is contained within the SalesOrder. For any given contract address there could be many owner addresses in the order map
 * It's used to optimise checking balances of market orders
 * A token owner could have a lot of different tokens for sale from the same contract
 * It's unnecessary to repeat a balance check, which is quite expensive in terms of list update time
 */
public class OrderContractAddressPair
{
    public SalesOrder order;
    public List<BigInteger> balance;

    public boolean equals(String tokenAddr, String address)
    {
        return (this.order.tokenInfo.address.equals(tokenAddr) && this.order.ownerAddress.equals(address));
    }

    public static boolean addPair(List<OrderContractAddressPair> checkList, SalesOrder o) {
        boolean foundPair = false;
        for (OrderContractAddressPair pair : checkList)
        {
            if (pair.equals(o.tokenInfo.address, o.ownerAddress)) {
                foundPair = true;
                break;
            }
        }

        if (!foundPair)
        {
            OrderContractAddressPair pair = new OrderContractAddressPair();
            pair.order = o;
            checkList.add(pair);
            return true;
        }
        else
        {
            return false;
        }
    }
}
