package io.awallet.crypto.alphawallet.entity;


import static io.awallet.crypto.alphawallet.interact.SetupTokensInteract.CONTRACT_CONSTRUCTOR;

/**
 * Created by James on 26/03/2018.
 */

public class EtherscanTransaction
{
    //[{"blockNumber":"1671277","timeStamp":"1505373215","hash":"0x1b1717b6d32387041f7053a5ce3426e3c030ba557fcc458c3829abc8ad0601a9","nonce":"5","blockHash":"0x4389a76b07d5b6b82737aebb182b81758adb839431cf49669bf0c234201cdced","transactionIndex":"3",
    // "from":"0xfde7b48f097102e736b45296d1ac6cb8a51426eb","to":"0x007bee82bdd9e866b2bd114780a47f2261c684e3","value":"500000000000000000",
    // "gas":"31501","gasPrice":"4000000000","isError":"0","txreceipt_status":"","input":"0x","contractAddress":"",
    // "cumulativeGasUsed":"184451","gasUsed":"21000","confirmations":"1236861"},
    String blockNumber;
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

    public Transaction createTransaction()
    {
        TransactionOperation[] o;

        //TODO: Do full interpretation here, to avoid needing to reallocate.
        //Then in the transaction parsing we only need to fill in token information.
        //Further work would make a master Token list in 'HomeViewModel' and the transaction holder just populates the name
        //from there. That way we don't need to hold much information and we don't need to re-parse after this
        if (contractAddress.length() > 0)
        {
            to = contractAddress;
            //add a constructor here
            o = new TransactionOperation[1];
            TransactionOperation op = new TransactionOperation();
            ERC875ContractTransaction ct = new ERC875ContractTransaction();
            o[0] = op;
            op.contract = ct;
            ct.operation = CONTRACT_CONSTRUCTOR;
        }
        else
        {
            o = new TransactionOperation[0];
        }

        Transaction tx = new Transaction(hash, isError, blockNumber, timeStamp, nonce, from, to, value, gas, gasPrice, input,
            gasUsed, o);

        return tx;
    }
}
