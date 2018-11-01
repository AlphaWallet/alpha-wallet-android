package io.stormbird.wallet.entity;


import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import java.math.BigInteger;
import io.stormbird.token.tools.ParseMagicLink;
import static io.stormbird.wallet.C.BURN_ADDRESS;

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

    private static TransactionDecoder decoder = null;
    private static ParseMagicLink parser = null;

    public Transaction createTransaction(String walletAddress)
    {
        boolean isConstructor = false;
        TransactionOperation[] o;
        TransactionInput f = null;

        if (contractAddress.length() > 0)
        {
            to = contractAddress;
            //add a constructor here
            o = generateERC875Op();
            TransactionContract ct = o[0].contract;
            ct.setOperation(TransactionType.CONSTRUCTOR);// R.string.ticket_contract_constructor);
            ct.address = contractAddress;
            ct.setType(-3);// indicate that we need to load the contract
            isConstructor = true;
        }
        else
        {
            //Now perform as complete processing as we are able to here. This saves re-allocating and makes code far less brittle.
            o = new TransactionOperation[0];

            if (input != null && input.length() >= 10)
            {
                TransactionOperation op = null;
                TransactionContract ct = null;

                if (decoder == null) decoder = new TransactionDecoder();
                if (parser == null) parser = new ParseMagicLink();
                f = decoder.decodeInput(input);
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
                            op = o[0];
                            setName(o, TransactionType.MAGICLINK_TRANSFER);
                            op.contract.address = to;
                            break;
                        case "transferFrom(address,address,uint16[])":
                            o = generateERC875Op();
                            op = o[0];
                            op.contract.setIndicies(f.paramValues);
                            if (f.containsAddress(BURN_ADDRESS))
                            {
                                setName(o, TransactionType.REDEEM);
                            }
                            else
                            {
                                setName(o, TransactionType.TRANSFER_FROM);
                            }
                            op.contract.setType(-1);
                            op.contract.address = to;
                            op.contract.setOtherParty(f.getFirstAddress());
                            op.to = f.getAddress(1);
                            break;
                        case "transfer(address,uint16[])":
                            o = generateERC875Op();
                            op = o[0];
                            op.contract.setOtherParty(f.getFirstAddress());
                            op.contract.setIndicies(f.paramValues);
                            setName(o, TransactionType.TRANSFER_TO);
                            op.contract.address = to;
                            break;
                        case "transfer(address,uint256)":
                            o = generateERC20Op();
                            op = o[0];
                            op.from = from;
                            op.to = f.getFirstAddress();
                            op.value = String.valueOf(f.getFirstValue());
                            op.contract.address = to;
                            setName(o, TransactionType.TRANSFER_TO);
                            break;
                        case "transferFrom(address,address,uint256)":
                            o = generateERC20Op();
                            op = o[0];
                            op.from = f.getFirstAddress();
                            op.to = f.getAddress(1);
                            op.value = String.valueOf(f.getFirstValue());
                            op.contract.address = to;
                            setName(o, TransactionType.TRANSFER_FROM);
                            op.contract.setType(1);
                            break;
                        case "loadNewTickets(bytes32[])":
                            o = generateERC875Op();
                            op = o[0];
                            op.from = from;
                            op.value = String.valueOf(f.paramValues.size());
                            op.contract.address = to;
                            setName(o, TransactionType.LOAD_NEW_TOKENS);
                            op.contract.setType(1);
                            break;
                        case "passTo(uint256,uint16[],uint8,bytes32,bytes32,address)":
                            o = processPassTo(f);
                            op = o[0];
                            op.from = from;
                            op.to = f.getFirstAddress();
                            //value in what?
                            op.value = String.valueOf(f.getFirstValue());
                            op.contract.address = to;
                            setName(o, TransactionType.PASS_TO);
                            op.contract.setType(-1);
                            break;
                        case "endContract()":
                            o = generateERC875Op();
                            op = o[0];
                            ct = op.contract;
                            ct.setOperation(TransactionType.TERMINATE_CONTRACT);
                            ct.name = to;
                            ct.setType(-2);
                            setName(o, TransactionType.TERMINATE_CONTRACT);
                            ct.address = to;
                            break;
                        default:
                            break;
                    }

                    if (op != null)
                    {
                        op.transactionId = hash;
                    }
                }
            }
        }

        Transaction tx = new Transaction(hash, isError, blockNumber, timeStamp, nonce, from, to, value, gas, gasPrice, input,
            gasUsed, o);

        if (o.length > 0)
        {
            TransactionOperation op = o[0];
            if (op.contract != null) op.contract.completeSetup(walletAddress, tx);
        }

        tx.isConstructor = isConstructor;

        if (walletAddress != null && !walletInvolvedInTransaction(tx, f, walletAddress))
        {
            tx = null; //this transaction is not relevant to the wallet we're scanning for
        }

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

    private void setName(TransactionOperation[] o, TransactionType name)
    {
        if (o.length > 0 && o[0] != null)
        {
            TransactionOperation op = o[0];
            TransactionContract ct = op.contract;
            if (ct instanceof ERC875ContractTransaction)
            {
                ((ERC875ContractTransaction) ct).operation = name;
            }
        }
    }

    private boolean walletInvolvedInTransaction(Transaction trans, TransactionInput data, String walletAddr)
    {
        boolean involved = false;
        if (data == null || data.functionData == null)
        {
            return (trans.from.equalsIgnoreCase(walletAddr) || trans.to.equalsIgnoreCase(walletAddr)); //early return
        }
        if (data.containsAddress(walletAddr)) return true;
        if (trans.from.equalsIgnoreCase(walletAddr)) return true;
        if (trans.to.equalsIgnoreCase(walletAddr)) return true;
        if (trans.operations != null && trans.operations.length > 0 && trans.operations[0].walletInvolvedWithTransaction(walletAddr)) involved = true;
        return involved;
    }
}
