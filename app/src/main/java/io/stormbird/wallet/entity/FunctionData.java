package io.stormbird.wallet.entity;

import java.util.List;

import static io.stormbird.wallet.entity.TransactionDecoder.ContractType.CREATION;
import static io.stormbird.wallet.entity.TransactionDecoder.ContractType.ERC20;
import static io.stormbird.wallet.entity.TransactionDecoder.ContractType.ERC875;

/**
 * Created by James on 2/02/2018.
 */

public class FunctionData
{
    public String functionName;
    public String functionFullName;
    public List<String> args;
    public boolean hasSig;
    public TransactionDecoder.ContractType contractType;

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
