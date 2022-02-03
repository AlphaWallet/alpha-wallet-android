package com.alphawallet.app.entity;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.web3.entity.Web3Transaction;

import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphawallet.app.entity.TransactionDecoder.ReadState.ARGS;
import static org.web3j.crypto.Keys.ADDRESS_LENGTH_IN_HEX;

import timber.log.Timber;

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
    public static final int FUNCTION_LENGTH = 10;
    private final static List<String> endContractSignatures = new ArrayList<>();

    private int parseIndex;
    private Map<String, FunctionData> functionList;

    private ReadState state = ARGS;
    private int sigCount = 0;

    private FunctionData getUnknownFunction()
    {
        return new FunctionData("N/A", ContractType.OTHER);
    }

    public TransactionDecoder()
    {
        setupKnownFunctions();
    }

    public TransactionInput decodeInput(String input)
    {
        ParseStage parseState = ParseStage.PARSE_FUNCTION;
        parseIndex = 0;
        //1. check function
        TransactionInput thisData = new TransactionInput();
        if (input == null || input.length() < 10)
        {
            thisData.functionData = getUnknownFunction();
            return thisData;
        }

        try {
            while (parseIndex < input.length() && !(parseState == ParseStage.FINISH)) {
                switch (parseState) {
                    case PARSE_FUNCTION: //get function
                        parseState = setFunction(thisData, readBytes(input, FUNCTION_LENGTH), input.length());
                        break;
                    case PARSE_ARGS: //now get params
                        parseState = getParams(thisData, input);
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
            Timber.e(e);
        }

        thisData.setOperationType(null, null); //works for most cases; for magiclink requires tx and wallet data - but we don't see many of these now
        return thisData;
    }

    public TransactionInput decodeInput(Transaction tx, String walletAddress)
    {
        TransactionInput thisData = decodeInput(tx.input);
        thisData.setOperationType(tx, walletAddress);
        return thisData;
    }

    public TransactionInput decodeInput(Web3Transaction web3Tx, long chainId, String walletAddress)
    {
        TransactionInput thisData = decodeInput(web3Tx.payload);
        Transaction tx = new Transaction(web3Tx, chainId, walletAddress);
        thisData.setOperationType(tx, walletAddress);
        return thisData;
    }

    private ParseStage setFunction(TransactionInput thisData, String input, int length) {
        //first get expected arg list:
        FunctionData data = functionList.get(input);

        if (data != null)
        {
            thisData.functionData = data;
            thisData.arrayValues.clear();
            thisData.addresses.clear();
            thisData.sigData.clear();
            thisData.miscData.clear();
            thisData.functionData.functionRawHex = input;
        }
        else
        {
            thisData.functionData = getUnknownFunction();
            thisData.functionData.functionRawHex = input;
            return ParseStage.ERROR;
        }

        return ParseStage.PARSE_ARGS;
    }

    enum ReadState
    {
        ARGS,
        SIGNATURE
    }

    private ParseStage getParams(TransactionInput thisData, String input) {
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
                    case "bytes":
                        sb.setLength(0);
                        argData = read256bits(input);
                        BigInteger dataCount = Numeric.toBigInt(argData);
                        String stuff = readBytes(input, dataCount.intValue());
                        thisData.miscData.add(stuff);
                        break;
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
                        thisData.miscData.add(Numeric.cleanHexPrefix(sb.toString()));
                        break;
                    case "address":
                        if (argData.length() >= 64 - ADDRESS_LENGTH_IN_HEX)
                        {
                            thisData.addresses.add("0x" + argData.substring(64 - ADDRESS_LENGTH_IN_HEX));
                        }
                        break;
                    case "bytes32":
                        addArg(thisData, argData);
                        break;
                    case "bytes32[]":
                    case "uint16[]":
                    case "uint256[]":
                        count = new BigInteger(argData, 16);
                        for (int i = 0; i < count.intValue(); i++) {
                            String inputData = read256bits(input);
                            thisData.arrayValues.add(new BigInteger(inputData, 16));
                            if (inputData.equals("0")) break;
                        }
                        break;
                    case "uint256":
                        addArg(thisData, argData);
                        break;
                    case "uint8": //In our standards, we will put uint8 as the signature marker
                        if (thisData.functionData.hasSig) {
                            state = ReadState.SIGNATURE;
                            sigCount = 0;
                        }
                        addArg(thisData, argData);
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

    private void addArg(TransactionInput thisData, String input)
    {
        switch (state)
        {
            case ARGS:
                thisData.miscData.add(Numeric.cleanHexPrefix(input));
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

    private void addFunction(String method, ContractType type, boolean hasSig)
    {
        String methodId = buildMethodId(method);
        FunctionData data = functionList.get(methodId);
        if (data != null)
        {
            data.addType(type);
        }
        else
        {
            data = new FunctionData(method, type, hasSig);
            functionList.put(buildMethodId(method), data);
        }
    }

    private void setupKnownFunctions()
    {
        functionList = new HashMap<>();
        addFunction("transferFrom(address,address,uint16[])", ContractType.ERC875_LEGACY, false);
        addFunction("transfer(address,uint16[])", ContractType.ERC875_LEGACY, false);
        addFunction("trade(uint256,uint16[],uint8,bytes32,bytes32)", ContractType.ERC875_LEGACY, true);
        addFunction("passTo(uint256,uint16[],uint8,bytes32,bytes32,address)", ContractType.ERC875_LEGACY, true);
        addFunction("loadNewTickets(bytes32[])", ContractType.ERC875_LEGACY, false);
        addFunction("balanceOf(address)", ContractType.ERC875_LEGACY, false);

        addFunction("transfer(address,uint256)", ContractType.ERC20, false);
        addFunction("transfer(address,uint)", ContractType.ERC20, false);
        addFunction("transferFrom(address,address,uint256)", ContractType.ERC20, false);
        addFunction("approve(address,uint256)", ContractType.ERC20, false);
        addFunction("approve(address,uint)", ContractType.ERC20, false);
        addFunction("allocateTo(address,uint256)", ContractType.ERC20, false);
        addFunction("allowance(address,address)", ContractType.ERC20, false);
        addFunction("transferFrom(address,address,uint)", ContractType.ERC20, false);
        addFunction("approveAndCall(address,uint,bytes)", ContractType.ERC20, false);
        addFunction("balanceOf(address)", ContractType.ERC20, false);
        addFunction("transferAnyERC20Token(address,uint)", ContractType.ERC20, false);
        addFunction("delegate(address)", ContractType.ERC20, false);
        addFunction("mint(address,uint)", ContractType.ERC20, false);
        addFunction("swapExactTokensForTokens(uint256,uint256,address[],address,uint256)", ContractType.ERC20, false);
        addFunction("withdraw(address,uint256,address)", ContractType.ERC20, false);
        addFunction("deposit(address,uint256,address,uint16)", ContractType.ERC20, false);
        addFunction("deposit()", ContractType.ERC20, false);

        addFunction("transferFrom(address,address,uint256[])", ContractType.ERC875, false);
        addFunction("transfer(address,uint256[])", ContractType.ERC875, false);
        addFunction("trade(uint256,uint256[],uint8,bytes32,bytes32)", ContractType.ERC875, true);
        addFunction("passTo(uint256,uint256[],uint8,bytes32,bytes32,address)", ContractType.ERC875, true);
        addFunction("loadNewTickets(uint256[])", ContractType.ERC875, false);
        addFunction("balanceOf(address)", ContractType.ERC875, false);

        addFunction("endContract()", ContractType.CREATION, false);
        addFunction("selfdestruct()", ContractType.CREATION, false);
        addFunction("kill()", ContractType.CREATION, false);

        addFunction("safeTransferFrom(address,address,uint256,bytes)", ContractType.ERC721, false);
        addFunction("safeTransferFrom(address,address,uint256)", ContractType.ERC721, false);
        addFunction("transferFrom(address,address,uint256)", ContractType.ERC721, false);
        addFunction("approve(address,uint256)", ContractType.ERC721, false);
        addFunction("setApprovalForAll(address,bool)", ContractType.ERC721, false);
        addFunction("getApproved(address,address,uint256)", ContractType.ERC721, false);
        addFunction("isApprovedForAll(address,address)", ContractType.ERC721, false);
        addFunction("transfer(address,uint256)", ContractType.ERC721_LEGACY, false);
        addFunction("giveBirth(uint256,uint256)", ContractType.ERC721, false);
        addFunction("breedWithAuto(uint256,uint256)", ContractType.ERC721, false);
        addFunction("ownerOf(uint256)", ContractType.ERC721, false);
        addFunction("createSaleAuction(uint256,uint256,uint256,uint256)", ContractType.ERC721, false);
        addFunction("mixGenes(uint256,uint256,uint256)", ContractType.ERC721, false);
        addFunction("tokensOfOwner(address)", ContractType.ERC721, false);
        addFunction("store(uint256)", ContractType.ERC721, false);
        addFunction("remix(uint256,bytes)", ContractType.ERC721, false);

        addFunction("safeTransferFrom(address,address,uint256,uint256,bytes)", ContractType.ERC1155, false);
        addFunction("safeBatchTransferFrom(address,address,uint256[],uint256[],bytes)", ContractType.ERC1155, false);

        addFunction("dropCurrency(uint32,uint32,uint32,uint8,bytes32,bytes32,address)", ContractType.CURRENCY, true);
        addFunction("withdraw(uint256)", ContractType.CURRENCY, false); //0x2e1a7d4d0000000000000000000000000000000000000000000000000000000000000001

        addFunctionImmediate("commitNFT()", "0x521d83f0", ContractType.ERC721, false);
    }

    private void addFunctionImmediate(String functionBody, String functionHash, ContractType type, boolean hasSig)
    {
        FunctionData data = functionList.get(functionHash);
        if (data != null)
        {
            data.addType(type);
        }
        else
        {
            data = new FunctionData(functionBody, type, hasSig);
            functionList.put(functionHash, data);
        }
    }

    public void addScanFunction(String methodSignature, boolean hasSig)
    {
        addFunction(methodSignature, ContractType.OTHER, hasSig);
    }

    public ContractType getContractType(String input)
    {
        if (input.length() < 10) return ContractType.OTHER;
        Map<ContractType, Integer> functionCount = new HashMap<>();
        ContractType highestType = ContractType.OTHER;
        int highestCount = 0;

        //improve heuristic:
        String balanceMethod = Numeric.cleanHexPrefix(buildMethodId("balanceOf(address)"));
        String isStormbird = Numeric.cleanHexPrefix(buildMethodId("isStormBirdContract()"));
        String isStormbird2 = Numeric.cleanHexPrefix(buildMethodId("isStormBird()"));
        String trade = Numeric.cleanHexPrefix(buildMethodId("trade(uint256,uint256[],uint8,bytes32,bytes32)"));
        String tradeLegacy = Numeric.cleanHexPrefix(buildMethodId("trade(uint256,uint16[],uint8,bytes32,bytes32)"));

        if (input.contains(balanceMethod))
        {
            if (input.contains(isStormbird) || input.contains(isStormbird2) || input.contains(tradeLegacy) || input.contains(trade))
            {
                if (input.contains(tradeLegacy))
                {
                    return ContractType.ERC875_LEGACY;
                }
                else
                {
                    return ContractType.ERC875;
                }
            }
        }
        else
        {
            return ContractType.OTHER;
        }

        //ERC721/x or ERC20

        for (String signature : functionList.keySet())
        {
            String cleanSig = Numeric.cleanHexPrefix(signature);
            int index = input.indexOf(cleanSig);
            if (index >= 0)
            {
                FunctionData data = functionList.get(signature);
                for (ContractType type : data.contractType)
                {
                    int count = 0;
                    if (functionCount.containsKey(type)) count = functionCount.get(type);
                    count++;
                    functionCount.put(type, count);
                    if (count > highestCount)
                    {
                        highestCount = count;
                        highestType = type;
                    }
                }
            }
        }

        if (highestType == ContractType.ERC721 && functionCount.containsKey(ContractType.ERC721_LEGACY))
        {
            highestType = ContractType.ERC721_LEGACY;
        }
        else if (functionCount.containsKey(ContractType.ERC20))
        {
            highestType = ContractType.ERC20;
        }

        return highestType;
    }

    enum ParseStage
    {
        PARSE_FUNCTION, PARSE_ARGS, FINISH, ERROR
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
        if (data != null && data.arrayValues != null)
        {
            indices = new int[data.arrayValues.size()];
            for (int i = 0; i < data.arrayValues.size() ; i++)
            {
                indices[i] = data.arrayValues.get(i).intValue();
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
}

