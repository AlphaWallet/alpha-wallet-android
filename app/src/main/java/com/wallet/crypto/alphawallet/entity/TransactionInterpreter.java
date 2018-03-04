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

    private Map<String, FunctionData> functionList;

    public TransactionInterpreter()
    {
        setupKnownFunctions();
    }

    public TransactionData InterpretTransation(String input)
    {
        int parseState = 0;
        int parseIndex = 0;
        //1. check function
        thisData = new TransactionData();

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

        return thisData;
    }

    public int setFunction(String input)
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

        return input.length();
    }

    private int getParams(String input)
    {
        int parseIndex = 0;

        if (thisData.functionData.args != null)
        {
            for (String type : thisData.functionData.args)
            {
                switch (type.charAt(0))
                {
                    case 'a':
                        if (type.equals("address"))
                        {
                            //read 64 from input
                            String addr = input.substring(parseIndex, parseIndex+64);
                            parseIndex += 64;
                            thisData.addresses.add(addr);
                        }
                        break;
                    case 'b':
                        if (type.equals("bytes32"))
                        {
                            //read 64 from input
                            String data = input.substring(parseIndex, parseIndex+64);
                            thisData.sigData.add(data);
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
                                thisData.paramValues.add(new BigInteger(input.substring(parseIndex, parseIndex+64), 16));
                                parseIndex += 64;
                            }
                        }
                        else if (type.equals("uint256"))
                        {
                            thisData.paramValues.add(new BigInteger(input.substring(parseIndex, parseIndex+64), 16));
                            parseIndex += 64;
                        }
                        else if (type.equals("uint8")) //In the transaction input uint8 is still written as uint256
                        {
                            thisData.paramValues.add(new BigInteger(input.substring(parseIndex, parseIndex+64), 16));
                            parseIndex += 64;
                            thisData.sigData.add(input.substring(parseIndex, parseIndex+64));
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        return parseIndex;
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
        data.args = Arrays.asList(argArray);
        data.functionFullName = methodSig;

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