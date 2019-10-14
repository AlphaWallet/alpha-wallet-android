package com.alphawallet.app.entity;


import android.content.Context;

import com.alphawallet.app.C;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.TokensService;

import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.ParseMagicLink;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;

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
    private static TransactionDecoder ensDecoder = null;
    private static ParseMagicLink parser = null;

    public Transaction createTransaction(String walletAddress, Context ctx, int chainId)
    {
        boolean isConstructor = false;
        TransactionOperation[] o;
        TransactionInput f = null;
        if (decoder == null) decoder = new TransactionDecoder();
        if (parser == null) parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());

        if (contractAddress.length() > 0)
        {
            to = contractAddress;
            //add a constructor here
            o = generateERC875Op();
            TransactionContract ct = o[0].contract;
            ct.setOperation(TransactionType.CONSTRUCTOR);
            ct.address = contractAddress;
            ct.setType(-3);// indicate that we need to load the contract
            o[0].value = "";
            isConstructor = true;
            ContractType type = decoder.getContractType(input);
            ct.decimals = type.ordinal();

            if (type != ContractType.OTHER)
            {
                TokensService.setInterfaceSpec(chainId, contractAddress, type);
            }

            input = "Constructor"; //Placeholder - don't consume storage for the constructor
        }
        else
        {
            //Now perform as complete processing as we are able to here. This saves re-allocating and makes code far less brittle.
            o = new TransactionOperation[0];

            if (input != null && input.length() >= 10)
            {
                TransactionOperation op = null;
                TransactionContract ct = null;

                f = decoder.decodeInput(input);
                //is this a trade?
                if (f.functionData != null)
                {
                    //recover recipient
                    switch (f.functionData.functionFullName)
                    {
                        case "trade(uint256,uint16[],uint8,bytes32,bytes32)":
                        case "trade(uint256,uint256[],uint8,bytes32,bytes32)":
                            o = processTrade(f);
                            op = o[0];
                            setName(o, TransactionType.MAGICLINK_TRANSFER);
                            op.contract.address = to;
                            op.value = String.valueOf(f.paramValues.size());
                            break;
                        case "transferFrom(address,address,uint16[])":
                        case "transferFrom(address,address,uint256[])":
                            o = generateERC875Op();
                            op = o[0];
                            op.contract.setIndicies(f.paramValues);
                            if (f.containsAddress(C.BURN_ADDRESS))
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
                            op.value = String.valueOf(f.paramValues.size());
                            op.to = f.getAddress(1);
                            break;
                        case "transfer(address,uint16[])":
                        case "transfer(address,uint256[])":
                            o = generateERC875Op();
                            op = o[0];
                            op.contract.setOtherParty(f.getFirstAddress());
                            op.contract.setIndicies(f.paramValues);
                            setName(o, TransactionType.TRANSFER_TO);
                            op.value = String.valueOf(f.paramValues.size());
                            op.contract.address = to;
                            break;
                        case "transfer(address,uint256)":
                            o = generateERC20Op();
                            op = o[0];
                            op.from = from;
                            op.to = f.getFirstAddress();
                            op.value = String.valueOf(f.getFirstValue(ctx));
                            op.contract.address = to;
                            setName(o, TransactionType.TRANSFER_TO);
                            break;
                        case "transferFrom(address,address,uint256)":
                            o = generateERC20Op();
                            op = o[0];
                            op.from = f.getFirstAddress();
                            op.to = f.getAddress(1);
                            op.value = String.valueOf(f.getFirstValue(ctx));
                            op.contract.address = to;
                            setName(o, TransactionType.TRANSFER_FROM);
                            op.contract.setType(1);
                            break;
                        case "allocateTo(address,uint256)":
                            o = generateERC20Op();
                            op = o[0];
                            op.from = from;
                            op.to = f.getFirstAddress();
                            op.value = String.valueOf(f.getFirstValue(ctx));
                            op.contract.address = to;
                            setName(o, TransactionType.ALLOCATE_TO);
                            break;
                        case "approve(address,uint256)":
                            o = generateERC20Op();
                            op = o[0];
                            op.from = from;
                            op.to = f.getFirstAddress();
                            op.value = String.valueOf(f.getFirstValue(ctx));
                            op.contract.address = to;
                            setName(o, TransactionType.APPROVE);
                            break;
                        case "loadNewTickets(bytes32[])":
                        case "loadNewTickets(uint256[])":
                            o = generateERC875Op();
                            op = o[0];
                            op.from = from;
                            op.value = String.valueOf(f.paramValues.size());
                            op.contract.address = to;
                            setName(o, TransactionType.LOAD_NEW_TOKENS);
                            op.contract.setType(1);
                            break;
                        case "passTo(uint256,uint16[],uint8,bytes32,bytes32,address)":
                        case "passTo(uint256,uint256[],uint8,bytes32,bytes32,address)":
                            o = processPassTo(f);
                            op = o[0];
                            op.from = from;
                            op.to = f.getFirstAddress();
                            op.value = String.valueOf(f.paramValues.size());
                            op.contract.address = to;
                            setName(o, TransactionType.PASS_TO);
                            op.contract.setType(-1);
                            break;
                        case "endContract()":
                        case "selfdestruct()":
                        case "kill()":
                            o = generateERC875Op();
                            op = o[0];
                            ct = op.contract;
                            ct.setOperation(TransactionType.TERMINATE_CONTRACT);
                            ct.name = to;
                            ct.setType(-2);
                            setName(o, TransactionType.TERMINATE_CONTRACT);
                            op.value = "";
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
            gasUsed, chainId, o);

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
            o = generateERC875Op();
            TransactionOperation op = o[0];
            TransactionContract ct = op.contract;
            if (isError.equals("0")) //don't bother checking signature unless the transaction succeeded
            {
                byte[] tradeBytes = parser.getTradeBytes(ticketIndexArray, contractAddress, priceWei, expiry);
                //attempt ecrecover
                BigInteger key = Sign.signedMessageToKey(tradeBytes, sig);
                ct.setOtherParty("0x" + Keys.getAddress(key));
            }
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
            else
            {
                op.contract.name = "*" + String.valueOf(name.ordinal());
            }
        }
    }

    private boolean walletInvolvedInTransaction(Transaction trans, TransactionInput data, String walletAddr)
    {
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
