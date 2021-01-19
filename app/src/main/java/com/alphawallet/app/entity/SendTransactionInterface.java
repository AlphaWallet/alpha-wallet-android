package com.alphawallet.app.entity;

import com.alphawallet.app.web3.entity.Web3Transaction;

/**
 * Created by James on 26/01/2019.
 * Stormbird in Singapore
 */
public interface SendTransactionInterface
{
    void transactionSuccess(Web3Transaction web3Tx, String hashData);
    void transactionError(long callbackId, Throwable error);
}
