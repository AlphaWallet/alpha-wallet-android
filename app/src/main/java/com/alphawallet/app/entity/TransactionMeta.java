package com.alphawallet.app.entity;

/**
 * Created by James on 3/12/2018.
 * Stormbird in Singapore
 */

/**
 * Cut down version of transaction which is used to populate the Transaction view adapter data.
 * The actual transaction data is retrieved just-in-time from the database when the user looks at the transaction
 * This saves a lot of memory - especially for a contract with a huge amount of transactions.
 */

public class TransactionMeta
{
    public final String hash;
    public final long timeStamp;

    public TransactionMeta(String hash, long timeStamp)
    {
        this.hash = hash;
        this.timeStamp = timeStamp;
    }
}
