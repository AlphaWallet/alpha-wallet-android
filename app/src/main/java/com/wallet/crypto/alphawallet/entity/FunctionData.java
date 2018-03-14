package com.wallet.crypto.alphawallet.entity;

import java.util.List;

import static com.wallet.crypto.alphawallet.entity.TransactionDecoder.ERC20;
import static com.wallet.crypto.alphawallet.entity.TransactionDecoder.ERC875;

/**
 * Created by James on 2/02/2018.
 */

public class FunctionData
{
    public String functionName;
    public String functionFullName;
    public List<String> args;
    public boolean hasSig;
    public int contractType;

    public boolean isERC20()
    {
        return (contractType == ERC20);
    }

    public boolean isERC875()
    {
        return (contractType == ERC875);
    }
}
