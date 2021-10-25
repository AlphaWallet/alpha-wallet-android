package com.alphawallet.app.entity;

/**
 * Created by James on 3/12/2018.
 * Stormbird in Singapore
 */

import java.util.UUID;

/**
 * Cut down version of transaction which is used to populate the Transaction view adapter data.
 * The actual transaction data is retrieved just-in-time from the database when the user looks at the transaction
 * This saves a lot of memory - especially for a contract with a huge amount of transactions.
 */

public class TransactionMeta extends ActivityMeta
{
    public final boolean isPending;
    public final String contractAddress;
    public final long chainId;

    public TransactionMeta(String hash, long timeStamp, String contractAddress, long chainId, String blockNumber)
    {
        super(timeStamp, hash);
        this.isPending = blockNumber.equals("0") || blockNumber.equals("-2");
        this.contractAddress = contractAddress;
        this.chainId = chainId;
    }

    public long getUID()
    {
        return UUID.nameUUIDFromBytes((this.hash + "t").getBytes()).getMostSignificantBits();
    }
}
