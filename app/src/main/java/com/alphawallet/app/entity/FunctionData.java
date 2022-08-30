package com.alphawallet.app.entity;

import static com.alphawallet.app.entity.ContractType.CREATION;
import static com.alphawallet.app.entity.ContractType.ERC20;
import static com.alphawallet.app.entity.ContractType.ERC875;
import static com.alphawallet.app.entity.ContractType.ERC875_LEGACY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by James on 2/02/2018.
 */

public class FunctionData
{
    public final String functionName;
    public final String functionFullName;
    public String functionRawHex;
    public final List<String> args;
    public final boolean hasSig;
    public final List<ContractType> contractType;

    public FunctionData(String fName, ContractType t)
    {
        functionName = fName;
        functionFullName = fName;
        args = new ArrayList<>();
        hasSig = false;
        contractType = new ArrayList<>();
    }

    public FunctionData(String methodSig, ContractType t, boolean hasSignature)
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
        contractType.add(t);
        hasSig  = hasSignature;

        for (int i = 0; i < temp.size(); i++)//String arg : data.args)
        {
            String arg = temp.get(i);
            if (arg.contains("[]") || arg.equals("string") || arg.equals("bytes"))
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
        contractType.add(type);
    }

    public boolean isERC20()
    {
        return (contractType.contains(ERC20));
    }

    public boolean isERC875()
    {
        return (contractType.contains(ERC875) || contractType.contains(ERC875_LEGACY));
    }

    public boolean isConstructor()
    {
        return (contractType.contains(CREATION));
    }
}
