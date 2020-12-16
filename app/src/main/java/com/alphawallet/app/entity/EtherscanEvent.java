package com.alphawallet.app.entity;

import android.text.TextUtils;

import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.token.tools.Numeric;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 * Created by JB on 21/10/2020.
 */
public class EtherscanEvent
{
    public String blockNumber;
    public long timeStamp;
    public String hash;
    public int nonce;
    String blockHash;
    public String from;
    public String contractAddress;
    public String to;
    String tokenID;
    public String value;
    public String tokenName;
    public String tokenSymbol;
    public String tokenDecimal;
    String gas;
    String gasPrice;
    String gasUsed;

    public Transaction createTransaction(String walletAddress, @NotNull NetworkInfo networkInfo)
    {
        TransactionOperation[] operations = createOperation(walletAddress);

        String input = Numeric.toHexString(TokenRepository.createTokenTransferData(to, new BigInteger(value))); //write the input to the transaction to ensure this is correctly handled elsewhere in the wallet

        return new Transaction(hash, "0", blockNumber, timeStamp, nonce, from, contractAddress, "0", gas, gasPrice, input,
                gasUsed, networkInfo.chainId, operations);
    }

    private TransactionOperation[] createOperation(String walletAddress)
    {
        TransactionOperation[] o = generateOp();
        TransactionOperation op = o[0];
        op.from = from;
        op.to = to;
        if (!TextUtils.isEmpty(value) && !value.equals("null"))
        {
            op.value = value;
        }
        else
        {
            op.value = tokenID;
            if (tokenID.length() > 10)
            {
                op.value = "1";
            }
        }
        op.contract.address = contractAddress;
        if (from.equalsIgnoreCase(walletAddress))
        {
            setName(o, TransactionType.TRANSFER_TO);
        }
        else
        {
            setName(o, TransactionType.RECEIVED);
        }

        op.transactionId = hash;
        return o;
    }

    private TransactionOperation[] generateOp()
    {
        TransactionOperation[] o = new TransactionOperation[1];
        TransactionOperation op = new TransactionOperation();
        TransactionContract ct = new TransactionContract();
        o[0] = op;
        op.contract = ct;
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
}
