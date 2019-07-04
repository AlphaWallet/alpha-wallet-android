package io.stormbird.token.entity;

import io.stormbird.token.tools.TokenDefinition;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by James on 10/11/2018.
 * Stormbird in Singapore
 */

public class FunctionDefinition
{
    public ContractInfo contract;
    public String method;
    public TokenDefinition.Syntax syntax;
    public TokenDefinition.As as;
    public List<MethodArg> parameters = new ArrayList<>();

    public String result;
    public long resultTime = 0;
    public BigInteger tokenId;
    public EthereumTransaction tx;

    public int getTokenRequirement()
    {
        int count = 0;
        for (MethodArg arg : parameters)
        {
            if (arg.isTokenId()) count++;
        }

        return count;
    }
}