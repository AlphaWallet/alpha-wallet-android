package com.alphawallet.app.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.alphawallet.app.entity.ContractType.*;

/**
 * Created by James on 2/02/2018.
 */

public class FunctionData
{
    public String functionName;
    public String functionFullName;
    public List<String> args;
    public boolean hasSig;
    public List<ContractType> contractType;

    public FunctionData(String methodSig, ContractType type, boolean hasSingature)
    {
        int b1Index = methodSig.indexOf("(");
        int b2Index = methodSig.lastIndexOf(")");

        functionName = methodSig.substring(0, b1Index);
        String args = methodSig.substring(b1Index + 1, b2Index);
        String[] argArray = args.split(",");
        List<String> temp = Arrays.asList(argArray);
        this.args = new ArrayList<>();
        this.args.addAll(temp);
        functionFullName = methodSig;
        contractType = new ArrayList<>();
        contractType.add(type);
        hasSig  = hasSingature;

        for (int i = 0; i < temp.size(); i++)//String arg : data.args)
        {
            String arg = temp.get(i);
            if (arg.contains("[]") || arg.equals("string"))
            {
                //rearrange to end, no need to store this arg
                this.args.add(arg);
                String argPlaceholder = "nodata";
                this.args.set(i, argPlaceholder);
            }
        }
    }

    public void addType(ContractType type)
    {
        if (contractType == null) contractType = new ArrayList<>();
        contractType.add(type);
    }

    public boolean isERC20()
    {
        return (contractType.contains(ERC20));
    }

    public boolean isERC875()
    {
        return (contractType.contains(ERC875) || contractType.contains(ERC875LEGACY));
    }

    public boolean isConstructor()
    {
        return (contractType.contains(CREATION));
    }
}
