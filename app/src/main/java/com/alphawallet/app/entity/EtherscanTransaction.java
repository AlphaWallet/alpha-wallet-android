package com.alphawallet.app.entity;


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

    public Transaction createTransaction(String walletAddress, long chainId)
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

    public EtherscanTransaction(CovalentTransaction transaction, Transaction tx)
    {
        blockNumber = tx.blockNumber;
        timeStamp = tx.timeStamp;
        hash = transaction.tx_hash;
        nonce = tx.nonce;
        blockHash = "";
        transactionIndex = transaction.tx_offset;
        from = transaction.from_address;
        to = transaction.to_address;
        value = transaction.value;
        gas = transaction.gas_spent;
        gasPrice = transaction.gas_price;
        isError = transaction.successful ? "0" : "1";
        txreceipt_status = "";
        input = tx.input;
        contractAddress = "";
        cumulativeGasUsed = "";
        gasUsed = transaction.gas_spent;
        confirmations = 0;

        if (tx.isConstructor)
        {
            to = tx.to;
            contractAddress = tx.to;
        }
    }
}
