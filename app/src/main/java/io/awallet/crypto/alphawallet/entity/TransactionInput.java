package io.awallet.crypto.alphawallet.entity;

import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 4/03/2018.
 *
 * A data structure that consists part of the transaction: it has the
 * parameters for a function call (currently, only transactions to a
 * contract is dealt in this class) and it is only returned by using
 * TransactionDecoder. Note that the address of the contract, the name
 * of the function called and the signature from transaction sender
 * are all not in this class.
 *
 */

public class TransactionInput
{
    public FunctionData functionData;
    public List<String> addresses;
    public List<BigInteger> paramValues;
    public List<String> sigData;
    public List<String> miscData;

    public TransactionInput()
    {
        paramValues = new ArrayList<>();
        addresses = new ArrayList<>();
        sigData = new ArrayList<>();
        miscData = new ArrayList<>();
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

    public String getFirstAddress() {
        String address = "";
        if (addresses.size() > 0) {
            address = "0x" + addresses.get(0).substring(64 - 40);
        }
        return address;
    }
}
