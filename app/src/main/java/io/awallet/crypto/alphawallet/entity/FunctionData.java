package io.awallet.crypto.alphawallet.entity;

import java.util.List;

import static io.awallet.crypto.alphawallet.entity.TransactionDecoder.CREATION;
import static io.awallet.crypto.alphawallet.entity.TransactionDecoder.ERC20;
import static io.awallet.crypto.alphawallet.entity.TransactionDecoder.ERC875;

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

    public boolean isConstructor()
    {
        return (contractType == CREATION);
    }
}
