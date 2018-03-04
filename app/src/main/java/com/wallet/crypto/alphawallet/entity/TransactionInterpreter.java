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

// TODO: Should be a factory class that emits an object containing transaction interpretation
public class TransactionInterpreter
{
    TransactionData thisData;

    private int parseIndex;
    private int parseState;
    private Map<String, FunctionData> functionList;

    public TransactionInterpreter()
    {
        setupKnownFunctions();
    }

    public TransactionData InterpretTransation(String input)
    {
        parseState = 0;
        parseIndex = 0;
        //1. check function
        thisData = new TransactionData();

        try {
            while (parseIndex < input.length()) {
                switch (parseState) {
                    case 0: //get function
                        parseIndex += setFunction(input.substring(0, 10));
                        parseState = 1;
                        break;
                    case 1: //now get params
                        parseIndex += getParams(input);
                        parseState = 2;
                        break;
                    case 2:
                        break;
                }

                if (parseIndex < 0) break; //error
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return thisData;
    }

    public int setFunction(String input) throws Exception
    {
        //first get expected arg list:
        FunctionData data = functionList.get(input);

        if (data != null)
        {
            thisData.functionData = data;
            thisData.paramValues.clear();
            thisData.addresses.clear();
            thisData.sigData.clear();
        }
        else
        {
            System.out.println("What?");
        }

        return input.length();
    }

    private int getParams(String input) throws Exception
    {
        if (thisData.functionData.args != null)
        {
            for (String type : thisData.functionData.args)
            {
                switch (type.charAt(0))
                {
                    case 'a':
                        if (type.equals("address"))
                        {
                            thisData.addresses.add(readNumber256(input));
                        }
                        break;
                    case 'b':
                        if (type.equals("bytes32"))
                        {
                            thisData.sigData.add(readNumber256(input));
                        }
                        break;
                    case 'u':
                        if (type.equals("uint16[]")) //TODO: handle dynamic types separately
                        {
                            BigInteger count = new BigInteger(readNumber256(input), 16);
                            for (int i = 0; i < count.intValue(); i++)
                            {
                                thisData.paramValues.add(new BigInteger(readNumber256(input), 16));
                            }
                        }
                        else if (type.equals("uint256"))
                        {
                            thisData.sigData.add(readNumber256(input));
                        }
                        else if (type.equals("uint8")) //In the transaction input uint8 is still written as uint256
                        {
                            String data = readNumber256(input);
                            thisData.sigData.add(data);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        return parseIndex;
    }

    private String readNumber256(String input)
    {
        if ((parseIndex + 64) <= input.length())
        {
            String value = input.substring(parseIndex, parseIndex+64);
            parseIndex += 64;
            return value;
        }
        else
        {
            return "0";
        }
    }


    private void setupKnownFunctions()
    {
        functionList = new HashMap<>();
        for (String methodSignature : KNOWN_FUNCTIONS)
        {
            FunctionData data = getArgs(methodSignature);
            functionList.put(buildMethodId(methodSignature), data);
        }
    }

    static final String[] KNOWN_FUNCTIONS = {
            "transferFrom(address,address,uint16[])",
            "transfer(address,uint16[])",
            "trade(uint256,uint16[],uint8,bytes32,bytes32)"
            };

    private FunctionData getArgs(String methodSig)
    {
        int b1Index = methodSig.indexOf("(");
        int b2Index = methodSig.lastIndexOf(")");

        FunctionData data = new FunctionData();
        data.functionName = methodSig.substring(0, b1Index);
        String args = methodSig.substring(b1Index + 1, b2Index);
        String[] argArray = args.split(",");
        List<String> temp = Arrays.asList(argArray);
        data.args = new ArrayList<>();
        data.args.addAll(temp);
        data.functionFullName = methodSig;

        for (int i = 0; i < temp.size(); i++)//String arg : data.args)
        {
            String arg = temp.get(i);
            if (arg.contains("[]"))
            {
                //rearrange to end, but read in arg decl
                data.args.add(arg);
                String argPlaceholder = "uint256";
                data.args.set(i, argPlaceholder);
            }
        }

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
//trade(uint,[],v,r,s)
/*
0x696ecc55
000000000000000000000000000000000000000000000000000000005a9a00e2 - expiry
00000000000000000000000000000000000000000000000000000000000000a0 - varargs decl
000000000000000000000000000000000000000000000000000000000000001b - 27: v
c59d6718734043600a49ec2419e566fa03676058e88326ff1161c579c6b8e799 - R
51d22461ab6f27bedd72d6b56c1fecf9f4cbb79a7728c510022617a9e42782ea - S
000000000000000000000000000000000000000000000000000000000000000a
0000000000000000000000000000000000000000000000000000000000000009
000000000000000000000000000000000000000000000000000000000000000a
000000000000000000000000000000000000000000000000000000000000000b
000000000000000000000000000000000000000000000000000000000000000c
000000000000000000000000000000000000000000000000000000000000000d
000000000000000000000000000000000000000000000000000000000000000e
000000000000000000000000000000000000000000000000000000000000000f
0000000000000000000000000000000000000000000000000000000000000010
0000000000000000000000000000000000000000000000000000000000000011
0000000000000000000000000000000000000000000000000000000000000012
 */