package com.wallet.crypto.alphawallet.entity;

import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 4/03/2018.
 */

public class TransactionData
{
    public FunctionData functionData;
    public List<String> addresses;
    public List<BigInteger> paramValues;
    public List<String> sigData;

    public TransactionData()
    {
        paramValues = new ArrayList<>();
        addresses = new ArrayList<>();
        sigData = new ArrayList<>();
    }

    //Addresses are in 256bit format
    public boolean containsAddress(String address)
    {
        boolean hasAddr = false;
        //Scan addresses for this address
        address = Numeric.cleanHexPrefix(address);
        for (String thisAddr : addresses)
        {
            if (thisAddr.contains(address))
            {
                hasAddr = true;
                break;
            }
        }

        return hasAddr;
    }
}
