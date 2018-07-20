package io.stormbird.wallet.entity;


import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.security.SignatureException;

import io.stormbird.token.tools.ParseMagicLink;
import io.stormbird.wallet.R;

import static io.stormbird.wallet.interact.SetupTokensInteract.CONTRACT_CONSTRUCTOR;
import static io.stormbird.wallet.interact.SetupTokensInteract.RECEIVE_FROM_MAGICLINK;

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

    private static TransactionDecoder decoder = null;
    private static ParseMagicLink parser = null;

    public Transaction createTransaction()
    {
        boolean isConstructor = false;
        TransactionOperation[] o;
        // Parse internal transaction - this is a RECEIVE_FROM_MAGICLINK transaction.
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
            ct.operation = R.string.ticket_receive_from_magiclink;

            //fix up the received params to make parsing simple
            from = to;
            to = contractAddress;
        }
        else if (contractAddress.length() > 0)
        {
            to = contractAddress;
            //add a constructor here
            o = generateERC875Op();
            TransactionContract ct = o[0].contract;
            ct.setOperation(R.string.ticket_contract_constructor);
            ct.address = contractAddress;
            ct.setType(-5);// indicate that we need to load the contract
            isConstructor = true;
        }
        else
        {
            //Now perform as complete processing as we are able to here. This saves re-allocating and makes code far less brittle.
            o = new TransactionOperation[0];

            if (input != null && input.length() >= 10)
            {
                TransactionOperation op;
                TransactionContract ct;

                if (decoder == null) decoder = new TransactionDecoder();
                if (parser == null) parser = new ParseMagicLink();
                TransactionInput f = decoder.decodeInput(input);
                //is this a trade?
                if (f.functionData != null)
                {
                    //recover recipient
                    //no need for passTo: address is embedded in the tx Input.
                    //may be desirable for iOS though.
                    switch (f.functionData.functionFullName)
                    {
                        case "trade(uint256,uint16[],uint8,bytes32,bytes32)":
                            o = processTrade(f);
                            break;
                        case "transferFrom(address,address,uint16[])":
                            o = generateERC875Op();
                            o[0].contract.setIndicies(f.paramValues);
                            break;
                        case "transfer(address,uint16[])":
                            o = generateERC875Op();
                            o[0].contract.setOtherParty(from);
                            o[0].contract.setIndicies(f.paramValues);
                            break;
                        case "transfer(address,uint256)":
                            o = generateERC20Op();
                            op = o[0];
                            op.from = from;
                            op.to = f.getFirstAddress();
                            op.transactionId = hash;
                            op.value = String.valueOf(f.getFirstValue());
                            break;
                        case "loadNewTickets(bytes32[])":
                            o = generateERC875Op();
                            op = o[0];
                            op.from = from;
                            op.transactionId = hash;
                            op.value = String.valueOf(f.paramValues.size());
                            break;
                        case "passTo(uint256,uint16[],uint8,bytes32,bytes32,address)":
                            o = processPassTo(f);
                            op = o[0];
                            op.from = from;
                            op.to = f.getFirstAddress();
                            op.transactionId = hash;
                            //value in what?
                            op.value = String.valueOf(f.getFirstValue());
                            break;
                        case "endContract()":
                            o = generateERC875Op();
                            op = o[0];
                            ct = op.contract;
                            ct.setOperation(R.string.ticket_terminate_contract);
                            ct.name = to;
                            ct.setType(-2);
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        Transaction tx = new Transaction(hash, isError, blockNumber, timeStamp, nonce, from, to, value, gas, gasPrice, input,
            gasUsed, o);

        tx.isConstructor = isConstructor;

        return tx;
    }

    private TransactionOperation[] generateERC20Op()
    {
        TransactionOperation[] o = new TransactionOperation[1];
        TransactionOperation op = new TransactionOperation();
        TransactionContract ct = new TransactionContract();
        o[0] = op;
        op.contract = ct;
        return o;
    }

    private TransactionOperation[] generateERC875Op()
    {
        TransactionOperation[] o = new TransactionOperation[1];
        TransactionOperation op = new TransactionOperation();
        ERC875ContractTransaction ct = new ERC875ContractTransaction();
        o[0] = op;
        op.contract = ct;
        return o;
    }

    private TransactionOperation[] processPassTo(TransactionInput f)
    {
        TransactionOperation[] o = processTrade(f);
        if (o.length > 0)
        {
            o[0].contract.totalSupply = f.getFirstAddress(); //store destination address for this passTo. We don't use totalSupply for anything else in this case
        }

        return o;
    }

    private TransactionOperation[] processTrade(TransactionInput f)
    {
        TransactionOperation[] o;
        try
        {
            Sign.SignatureData sig = decoder.getSignatureData(f);
            //ecrecover the recipient of the ether
            int[] ticketIndexArray = decoder.getIndices(f);
            String expiryStr = f.miscData.get(0);
            long expiry = Long.valueOf(expiryStr, 16);
            BigInteger priceWei = new BigInteger(value);
            contractAddress = to;
            byte[] tradeBytes = parser.getTradeBytes(ticketIndexArray, contractAddress, priceWei, expiry);
            //attempt ecrecover
            BigInteger key = Sign.signedMessageToKey(tradeBytes, sig);

            o = generateERC875Op();
            TransactionOperation op = o[0];
            TransactionContract ct = op.contract;

            ct.setOtherParty("0x" + Keys.getAddress(key));
            ct.address = contractAddress;
            ct.setIndicies(f.paramValues);
            ct.name = contractAddress;
        }
        catch (Exception e)
        {
            o = generateERC875Op();
            e.printStackTrace();
        }

        return o;
    }
}
