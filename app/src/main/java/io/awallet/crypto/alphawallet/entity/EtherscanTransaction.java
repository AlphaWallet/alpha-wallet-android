package io.awallet.crypto.alphawallet.entity;


import static io.awallet.crypto.alphawallet.interact.SetupTokensInteract.CONTRACT_CONSTRUCTOR;
import static io.awallet.crypto.alphawallet.interact.SetupTokensInteract.RECEIVE_FROM_UNIVERSAL_LINK;

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
    public boolean internal = false;

    public Transaction createTransaction()
    {
        TransactionOperation[] o;
        // Parse internal transaction - this is a RECEIVE_FROM_UNIVERSAL_LINK transaction.
        /* 'operations' member is used in a lot of places. However,
	 * I'd say a good refactor will sort this out, I think Scoff &
	 * co had to make the unwieldy nested class set there to read
	 * in data from their server. 'Operations' should really hold
	 * an object that defines the transaction. You should have an
	 * object for each type of contract - ERC20/875 etc. The
	 * places where operations is used then can be moved inside
	 * these classes. We don't use his transaction server now
	 * anyway.
         */

        if (internal)
        {
            o = new TransactionOperation[1];
            TransactionOperation op = new TransactionOperation();
            ERC875ContractTransaction ct = new ERC875ContractTransaction();
            o[0] = op;
            op.contract = ct;
            ct.address = contractAddress;
            op.from = contractAddress;
            ct.type = 2; // indicate that we need to load the contract
            ct.operation = RECEIVE_FROM_UNIVERSAL_LINK;

            //fix up the received params to make parsing simple
            from = to;
            to = contractAddress;
        }

        //TODO: Do full interpretation here, to avoid needing to reallocate.
        //Then in the transaction parsing we only need to fill in token information.
        //Further work would make a master Token list in 'HomeViewModel' and the transaction holder just populates the name
        //from there. That way we don't need to hold much information and we don't need to re-parse after this
        else if (contractAddress.length() > 0)
        {
            to = contractAddress;
            //add a constructor here
            o = new TransactionOperation[1];
            TransactionOperation op = new TransactionOperation();
            ERC875ContractTransaction ct = new ERC875ContractTransaction();
            o[0] = op;
            op.contract = ct;
            ct.operation = CONTRACT_CONSTRUCTOR;
            ct.address = contractAddress;
            ct.type = -5; // indicate that we need to load the contract
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
