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
    int nonce;
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

    private static TransactionDecoder decoder = null;

    public Transaction createTransaction(String walletAddress, Context ctx, int chainId)
    {
        Transaction tx = new Transaction(hash, isError, blockNumber, timeStamp, nonce, from, to, value, gas, gasPrice, input,
                                         gasUsed, chainId, contractAddress);

        if (tx.operations.length > 0)
        {
            TransactionOperation op = tx.operations[0];
            if (op.contract != null) op.contract.completeSetup(walletAddress, tx);
        }

        if (walletAddress != null && !walletInvolvedInTransaction(tx, walletAddress.toLowerCase()))
        {
            tx = null; //this transaction is not relevant to the wallet we're scanning for
        }

        return tx;
    }

    private boolean walletInvolvedInTransaction(Transaction trans, String walletAddr)
    {
        if (decoder == null) decoder = new TransactionDecoder();
        TransactionInput data = decoder.decodeInput(input);
        boolean involved = false;
        if ((data != null && data.functionData != null) && data.containsAddress(walletAddr)) return true;
        if (trans.from.equalsIgnoreCase(walletAddr)) return true;
        if (trans.to.equalsIgnoreCase(walletAddr)) return true;
        if (input != null && input.length() > 40 && input.contains(Numeric.cleanHexPrefix(walletAddr))) return true;
        if (trans.operations != null && trans.operations.length > 0 && trans.operations[0].walletInvolvedWithTransaction(walletAddr))
            involved = true;
        return involved;
    }

    public String getHash() { return hash; }
}
