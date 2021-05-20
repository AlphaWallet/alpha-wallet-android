package com.alphawallet.app.entity;

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
    public String tokenID;
    public String value;
    public String tokenName;
    public String tokenSymbol;
    public String tokenDecimal;
    public String input;
    String gas;
    String gasPrice;
    String gasUsed;

    public Transaction createTransaction(@NotNull NetworkInfo networkInfo)
    {
        BigInteger valueBI = BigInteger.ZERO;
        if (value != null && value.length() > 0 && Character.isDigit(value.charAt(0)))
        {
            valueBI = new BigInteger(value);
        }

        String input = Numeric.toHexString(TokenRepository.createTokenTransferData(to, valueBI)); //write the input to the transaction to ensure this is correctly handled elsewhere in the wallet

        return new Transaction(hash, "0", blockNumber, timeStamp, nonce, from, contractAddress, "0", gas, gasPrice, input,
                gasUsed, networkInfo.chainId, false);
    }

    public Transaction createNFTTransaction(@NotNull NetworkInfo networkInfo)
    {
        BigInteger tokenId = BigInteger.ONE;
        try
        {
            tokenId = new BigInteger(tokenID);
        }
        catch (Exception e)
        {
            //no action, default to '1'
        }

        String input = Numeric.toHexString(TokenRepository.createERC721TransferFunction(from, to, contractAddress, tokenId)); //write the input to the transaction to ensure this is correctly handled elsewhere in the wallet

        return new Transaction(hash, "0", blockNumber, timeStamp, nonce, from, contractAddress, "0", gas, gasPrice, input,
                gasUsed, networkInfo.chainId, false);
    }
}
