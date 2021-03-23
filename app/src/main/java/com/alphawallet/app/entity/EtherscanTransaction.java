package com.alphawallet.app.entity;


import android.content.Context;

import com.alphawallet.token.tools.Numeric;

/**
 * Created by James on 26/03/2018.
 */

public class EtherscanTransaction
{
    public String blockNumber;
    long timeStamp;
    String hash;
    public int nonce;
    String blockHash;
    int transactionIndex;
    String from;
    String to;
    String value;
    String gas;
    String gasPrice;
    String isError;
    String txreceipt_status;
    String input;
    String contractAddress;
    String cumulativeGasUsed;
    String gasUsed;
    int confirmations;

    public Transaction createTransaction(String walletAddress, int chainId)
    {
        Transaction tx = new Transaction(hash, isError, blockNumber, timeStamp, nonce, from, to, value, gas, gasPrice, input,
                                         gasUsed, chainId, contractAddress);

        if (walletAddress != null)
        {
            if (!tx.getWalletInvolvedInTransaction(walletAddress))
            {
                tx = null;
            }
        }

        return tx;
    }

    public String getHash() { return hash; }
}
