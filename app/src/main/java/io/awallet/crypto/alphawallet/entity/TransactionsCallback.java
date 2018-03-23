package io.awallet.crypto.alphawallet.entity;

/**
 * Created by James on 19/03/2018.
 */

public interface TransactionsCallback
{
    public void recieveTransactions(Transaction[] txList);
}
