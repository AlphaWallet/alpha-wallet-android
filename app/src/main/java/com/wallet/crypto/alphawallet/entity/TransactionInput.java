package com.wallet.crypto.alphawallet.entity;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James on 2/02/2018.
 */

public class TransactionInput
{
    public String functionName;
    public String function;
    public List<String> params;
    public List<String> addresses;
    public List<BigInteger> paramValues;

    private Map<String, FunctionData> functionList;

    public TransactionInput(String input)
    {
        setupKnownFunctions();

        int parseState = 0;
        int parseIndex = 0;
        //1. check function

        while (parseIndex < input.length())
        {
            switch (parseState)
            {
                case 0: //get function
                    parseIndex += setFunction(input.substring(0, 10));
                    parseState = 1;
                    break;
                case 1: //now get params
                    parseIndex += getParams(input.substring(parseIndex));
                    parseState = 2;
                    break;
                case 2:
                    break;
            }

            if (parseIndex < 0) break; //error
        }
    }

    public int setFunction(String input)
    {
        //first get expected arg list:
        FunctionData data = functionList.get(input);

        if (data != null)
        {
            params = data.args;
            functionName = data.functionName;
            paramValues = new ArrayList<>();
            addresses = new ArrayList<>();
        }

        return input.length();
    }

    private int getParams(String input)
    {
        int parseIndex = 0;

        if (params != null)
        {
            for (String type : params)
            {
                switch (type.charAt(0))
                {
                    case 'a':
                        if (type.equals("address"))
                        {
                            //read 64 from input
                            String addr = input.substring(parseIndex, parseIndex+64);
                            parseIndex += 64;
                            addresses.add(addr);
                        }
                        break;
                    case 'u':
                        if (type.equals("uint16[]")) //TODO: handle dynamic types separately
                        {
                            //read in dynamic array
                            String offset = input.substring(parseIndex, parseIndex+64);
                            parseIndex += 64;
                            BigInteger count = new BigInteger(input.substring(parseIndex, parseIndex+64), 16);
                            parseIndex += 64;
                            for (int i = 0; i < count.intValue(); i++)
                            {
                                paramValues.add(new BigInteger(input.substring(parseIndex, parseIndex+64), 16));
                                parseIndex += 64;
                            }
                        }
                        break;
                }
            }
        }

        return parseIndex;
    }


    private void setupKnownFunctions()
    {
        functionList = new HashMap<>();
        String methodSignature = "transferFrom(address,address,uint16[])";
        FunctionData data = getArgs(methodSignature);
        functionList.put(buildMethodId(methodSignature), data);
    }

    private FunctionData getArgs(String methodSig)
    {
        int b1Index = methodSig.indexOf("(");
        int b2Index = methodSig.lastIndexOf(")");

        FunctionData data = new FunctionData();
        data.functionName = methodSig.substring(0, b1Index);
        String args = methodSig.substring(b1Index + 1, b2Index);
        String[] argArray = args.split(",");
        data.args = Arrays.asList(argArray);

        return data;
    }

    private String buildMethodId(String methodSignature) {
        byte[] input = methodSignature.getBytes();
        byte[] hash = Hash.sha3(input);
        return Numeric.toHexString(hash).substring(0, 10);
    }
}
//0xa6fb475f000000000000000000000000951c19daead668bfa8391c94286f8ce7cbda2fe3000000000000000000000000879230570f360424bc5baa99906d5f640a75551e000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000004

//000000000000000000000000cb53390d32495163936ee451fee7089cd30be33c000000000000000000000000000000000000000000000000000000000000dead000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001