package com.alphawallet.token.entity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MagicLinkData
{
    public long expiry;
    public byte[] prefix;
    public BigInteger nonce;
    public double price;
    public BigInteger priceWei;
    public List<BigInteger> tokenIds;
    public int[] indices;
    public BigInteger amount;
    public int ticketStart;
    public int ticketCount;
    public String contractAddress;
    public byte[] signature = new byte[65];
    public byte[] message;
    public String ownerAddress;
    public String contractName;
    public byte contractType;
    public long chainId;

    public List<BigInteger> balanceInfo = null;

    public boolean isValidOrder()
    {
        //check this order is not corrupt
        //first check the owner address - we should already have called getOwnerKey
        boolean isValid = true;

        if (this.ownerAddress == null || this.ownerAddress.length() < 20) isValid = false;
        if (this.contractAddress == null || this.contractAddress.length() < 20) isValid = false;
        if (this.message == null) isValid = false;

        return isValid;
    }

    public boolean balanceChange(List<BigInteger> balance)
    {
        //compare two balances
        //quick return, if sizes are different there's a change
        if (balanceInfo == null)
        {
            balanceInfo = new ArrayList<>(); //initialise the balance list
            return true;
        }
        if (balance.size() != balanceInfo.size()) return true;

        List<BigInteger> oldBalance = new ArrayList<>(balanceInfo);
        List<BigInteger> newBalance = new ArrayList<>(balance);

        oldBalance.removeAll(balanceInfo);
        newBalance.removeAll(balance);

        return (oldBalance.size() != 0 || newBalance.size() != 0);
    }
}
