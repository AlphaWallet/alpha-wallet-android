package io.stormbird.token.entity;

import io.stormbird.token.tools.TokenDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 10/11/2018.
 * Stormbird in Singapore
 */

public class FunctionDefinition
{
    public ContractInfo contract;
    public String method;
    public TokenDefinition.Syntax syntax;
    public List<MethodArg> parameters = new ArrayList<>();

    public String result;
    public long resultTime;
}