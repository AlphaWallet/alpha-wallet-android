package com.alphawallet.app.entity;

import com.alphawallet.app.web3.entity.Web3Transaction;

/**
 * Created by James on 26/01/2019.
 * Stormbird in Singapore
 */
public interface SignTransactionInterface
{
    void signTransaction(Web3Transaction transaction, String txHex, boolean success);
}
