package io.stormbird.wallet.entity;

import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.stormbird.wallet.entity.TransactionDecoder.ContractType.CREATION;
import static io.stormbird.wallet.entity.TransactionDecoder.ContractType.ERC20;
import static io.stormbird.wallet.entity.TransactionDecoder.ContractType.ERC875;
import static io.stormbird.wallet.entity.TransactionDecoder.ReadState.ARGS;
import static org.web3j.crypto.Keys.ADDRESS_LENGTH_IN_HEX;

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

    private static List<String> endContractSignatures = new ArrayList<>();

    private int parseIndex;
    private Map<String, FunctionData> functionList;

    private ReadState state = ARGS;
    private int sigCount = 0;

    private FunctionData unknownFunction = new FunctionData();

    public TransactionDecoder()
    {
        setupKnownFunctions();
        setupUnknownFunction();
    }

    public TransactionInput decodeInput(String input)
    {
        ParseStage parseState = ParseStage.PARSE_FUNCTION;
        parseIndex = 0;
        //1. check function
        thisData = new TransactionInput();
        if (input == null || input.length() < 10)
        {
            thisData.functionData = unknownFunction;
            return thisData;
        }

        try {
            while (parseIndex < input.length() && !(parseState == ParseStage.FINISH)) {
                switch (parseState) {
                    case PARSE_FUNCTION: //get function
                        parseState = setFunction(readBytes(input, 10), input.length());
                        break;
                    case PARSE_ARGS: //now get params
                        parseState = getParams(input);
                        break;
                    case FINISH:
                        break;
                    case ERROR:
                        //Perform any future error handling here
                        parseState = ParseStage.FINISH;
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

    private ParseStage setFunction(String input, int length) throws Exception
    {
        //first get expected arg list:
        FunctionData data = functionList.get(input);

        if (data != null)
        {
            thisData.functionData = data;
            thisData.paramValues.clear();
            thisData.addresses.clear();
            thisData.sigData.clear();
            thisData.miscData.clear();
        }
        else
        {
            thisData.functionData = unknownFunction;
            return ParseStage.ERROR;
        }

        return ParseStage.PARSE_ARGS;
    }

    enum ReadState
    {
        ARGS,
        SIGNATURE
    };

    private ParseStage getParams(String input) throws Exception
    {
        state = ARGS;
        BigInteger count;
        StringBuilder sb = new StringBuilder();
        if (thisData.functionData != null && thisData.functionData.args != null)
        {
            for (String type : thisData.functionData.args)
            {
                String argData = read256bits(input);
                if (argData.equals("0")) break;
                switch (type)
                {
                    case "string":
                        count = new BigInteger(argData, 16);
                        sb.setLength(0);
                        argData = read256bits(input);
                        if (count.intValue() > argData.length()) count = BigInteger.valueOf(argData.length());
                        for (int index = 0; index < (count.intValue()*2); index += 2)
                        {
                            int v = Integer.parseInt(argData.substring(index, index+2), 16);
                            char c = (char)v;
                            sb.append(c);
                        }
                        thisData.miscData.add(sb.toString());
                        break;
                    case "address":
                        if (argData.length() >= 64 - ADDRESS_LENGTH_IN_HEX)
                        {
                            thisData.addresses.add("0x" + argData.substring(64 - ADDRESS_LENGTH_IN_HEX));
                        }
                        break;
                    case "bytes32":
                        addArg(argData);
                        break;
                    case "bytes32[]":
                        count = new BigInteger(argData, 16);
                        for (int i = 0; i < count.intValue(); i++) {
                            thisData.paramValues.add(new BigInteger(read256bits(input), 16));
                        }
                        break;
                    case "uint16[]":
                        count = new BigInteger(argData, 16);
                        for (int i = 0; i < count.intValue(); i++) {
                            thisData.paramValues.add(new BigInteger(read256bits(input), 16));
                        }
                        break;
                    case "uint256":
                        addArg(argData);
                        break;
                    case "uint8": //In our standards, we will put uint8 as the signature marker
                        if (thisData.functionData.hasSig) {
                            state = ReadState.SIGNATURE;
                            sigCount = 0;
                        }
                        addArg(argData);
                        break;
                    case "nodata":
                        //no need to store this data - eg placeholder to indicate presence of a vararg
                        break;
                    default:
                        break;
                }
            }
        }
        else
        {
            return ParseStage.FINISH; //skip to end of read if there are no args in the spec
        }

        return ParseStage.FINISH;
    }

    private void addArg(String input)
    {
        switch (state)
        {
            case ARGS:
                thisData.miscData.add(input);
                break;
            case SIGNATURE:
                thisData.sigData.add(input);
                if (++sigCount == 3) state = ARGS;
                break;
        }
    }

    private String readBytes(String input, int bytes)
    {
        if ((parseIndex + bytes) <= input.length())
        {
            String value = input.substring(parseIndex, parseIndex+bytes);
            parseIndex += bytes;
            return value;
        }
        else
        {
            return "0";
        }
    }

    private String read256bits(String input)
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
        for (int index = 0; index < KNOWN_FUNCTIONS.length; index++)
        {
            String methodSignature = KNOWN_FUNCTIONS[index];
            FunctionData data = getArgs(methodSignature);
            data.hasSig = HAS_SIG[index];
            data.contractType = CONTRACT_TYPE[index];
            functionList.put(buildMethodId(methodSignature), data);
        }
    }

    public void addScanFunction(String methodSignature, boolean hasSig)
    {
        FunctionData data = getArgs(methodSignature);
        data.hasSig = hasSig;
        data.contractType = ContractType.OTHER;
        functionList.put(buildMethodId(methodSignature), data);
    }

    static final String[] KNOWN_FUNCTIONS = {
            "transferFrom(address,address,uint16[])",
            "transfer(address,uint16[])",
            "trade(uint256,uint16[],uint8,bytes32,bytes32)",
            //ERC20 functions
            "transfer(address,uint256)",
            "transferFrom(address,address,uint256)",
            "approve(address,uint256)",
            "allocateTo(address,uint256)",
            //ERC875
            "loadNewTickets(bytes32[])",
            "passTo(uint256,uint16[],uint8,bytes32,bytes32,address)",
            "transferFrom(address,address,uint256[])",
            "transfer(address,uint256[])",
            "trade(uint256,uint256[],uint8,bytes32,bytes32)",
            "passTo(uint256,uint256[],uint8,bytes32,bytes32,address)",
            "loadNewTickets(uint256[])",
            //end updated ERC875 sigs
            "endContract()",
            "selfdestruct()",
            "kill()"
            };

    static final boolean[] HAS_SIG = {
            false,  //transferFrom
            false,  //transfer
            true,
            false,  //ERC20
            false,
            false,
            false,
            false, //loadNewTickets
            true,  //passTo
            false, //updated transferFrom
            false,
            true, //updated trade
            true, //updated passTo
            false,
            false, //endContract
            false,  //selfdestruct()
            false
    };

    enum ContractType
    {
        ERC20, ERC875, CREATION, OTHER
    }

    enum ParseStage
    {
        PARSE_FUNCTION, PARSE_ARGS, FINISH, ERROR
    }

    static final ContractType[] CONTRACT_TYPE = {
            ERC875,  //transferFrom
            ERC875,
            ERC875,
            ERC20,   //transferFrom
            ERC20,
            ERC20,
            ERC20,
            ERC875,
            ERC875, //old loadnewtickets
            ERC875, //updated transferFrom
            ERC875,
            ERC875,
            ERC875,
            ERC875, //updated loadNewTickets
            CREATION, //endContract
            CREATION, //selfdestruct
            CREATION  //kill
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
            if (arg.contains("[]") || arg.equals("string"))
            {
                //rearrange to end, no need to store this arg
                data.args.add(arg);
                String argPlaceholder = "nodata";
                data.args.set(i, argPlaceholder);
            }
        }

        return data;
    }

    public Sign.SignatureData getSignatureData(TransactionInput data)
    {
        Sign.SignatureData sigData = null;
        if (data.functionData.hasSig && data.sigData != null && data.sigData.size() == 3)
        {
            BigInteger vBi = new BigInteger(data.sigData.get(0), 16);
            BigInteger rBi = new BigInteger(data.sigData.get(1), 16);
            BigInteger sBi = new BigInteger(data.sigData.get(2), 16);
            byte v = (byte) vBi.intValue();
            byte[] r = Numeric.toBytesPadded(rBi, 32);
            byte[] s = Numeric.toBytesPadded(sBi, 32);

            sigData = new Sign.SignatureData(v, r, s);
        }

        return sigData;
    }

    public int[] getIndices(TransactionInput data)
    {
        int[] indices = null;
        if (data != null && data.paramValues != null)
        {
            indices = new int[data.paramValues.size()];
            for (int i = 0; i < data.paramValues.size() ; i++)
            {
                indices[i] = data.paramValues.get(i).intValue();
            }
        }

        return indices;
    }

    public static String buildMethodId(String methodSignature) {
        byte[] input = methodSignature.getBytes();
        byte[] hash = Hash.sha3(input);
        return Numeric.toHexString(hash).substring(0, 10);
    }

    public static boolean isEndContract(String input)
    {
        if (input == null || input.length() != 10)
        {
            return false;
        }

        if (endContractSignatures.size() == 0)
        {
            buildEndContractSigs();
        }

        for (String sig : endContractSignatures)
        {
            if (input.equals(sig)) return true;
        }

        return false;
    }

    private static void buildEndContractSigs()
    {
        endContractSignatures.add(buildMethodId("endContract()"));
        endContractSignatures.add(buildMethodId("selfdestruct()"));
        endContractSignatures.add(buildMethodId("kill()"));
    }

    private void setupUnknownFunction()
    {
        unknownFunction.functionName = "N/A";
        unknownFunction.functionFullName = "N/A";
    }
}

