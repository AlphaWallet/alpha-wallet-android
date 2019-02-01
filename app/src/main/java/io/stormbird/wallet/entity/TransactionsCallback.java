package io.stormbird.wallet.entity;

/**
 * Created by James on 19/03/2018.
 */

public interface TransactionsCallback
{
    void recieveTransactions(Transaction[] txList);
}
