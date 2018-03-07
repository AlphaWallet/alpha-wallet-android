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
 *
 * TransactionDecoder currently only decode a transaction input in the
 * string format, which is strictly a string starting with "0x" and
 * with an even number of hex digits followed. (Probably should be
 * bytes but we work with string for now.) It is used only for one
 * thing at the moment: decodeInput(), which returns the decoded
 * input.
 */

// TODO: Should be a factory class that emits an object containing transaction interpretation
public class TransactionDecoder
{
    TransactionInput thisData;

    private int parseIndex;
    private Map<String, FunctionData> functionList;

    public TransactionDecoder()
    {
        setupKnownFunctions();
    }

    public TransactionInput decodeInput(String input)
    {
        int parseState = 0;
        parseIndex = 0;
        //1. check function
        thisData = new TransactionInput();

        try {
            while (parseIndex < input.length()) {
                switch (parseState) {
                    case 0: //get function
                        parseIndex += setFunction(input.substring(0, 10), input.length());
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

    public int setFunction(String input, int length) throws Exception
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
            System.out.println("Unhandled Transaction: " + input);
            //unknown
            return length;
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

