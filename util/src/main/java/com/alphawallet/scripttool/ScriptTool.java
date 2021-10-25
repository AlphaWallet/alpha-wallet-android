package com.alphawallet.scripttool;

import com.alphawallet.scripttool.Entity.CachedResult;
import com.alphawallet.scripttool.Entity.ParseState;
import com.alphawallet.scripttool.Entity.TokenscriptFunction;
import com.alphawallet.scripttool.Ethereum.TransactionHandler;
import com.alphawallet.token.entity.Attribute;
import com.alphawallet.token.entity.AttributeInterface;
import com.alphawallet.token.entity.ContractAddress;
import com.alphawallet.token.entity.ContractInfo;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.entity.TransactionResult;
import com.alphawallet.token.tools.TokenDefinition;

import org.web3j.abi.datatypes.Address;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.alphawallet.scripttool.Entity.ParseState.ARG;
import static com.alphawallet.scripttool.Entity.ParseState.TS_FILE;
import static com.alphawallet.scripttool.Entity.ParseState.USER_ADDR;

public class ScriptTool implements AttributeInterface
{
    private File tokenScriptFile;
    private Address userAddress = Address.DEFAULT;

    private Map<Long, Map<String, Map<BigInteger, CachedResult>>> transactionResults = new ConcurrentHashMap<>();  //optimisation results


    private final TokenscriptFunction tokenscriptFunction = new TokenscriptFunction() { };

    public static void main(String[] args) {
        new ScriptTool(args);
    }
    public ScriptTool(String[] args)
    {
        ParseState p = ARG;

        if (args == null || args.length == 0)
        {
            showUsage();
            return;
        }

        for (String arg : args)
        {
            switch (p)
            {
                case ARG:
                    switch (arg)
                    {
                        case "-tokenscript":
                            p = TS_FILE;
                            break;
                        case "-address":
                            p = USER_ADDR;
                            break;
                        case "-help":
                            showUsage();
                            break;
                    }
                    break;
                case USER_ADDR:
                    try
                    {
                        userAddress = new Address(arg);
                    }
                    catch (Exception e)
                    {
                        System.out.println("ERROR: " + arg + " is not a valid Ethereum address");
                    }
                    p = ARG;
                    break;
                case TS_FILE:
                    try
                    {
                        File thisDirectory = new File(".");
                        File[] files = thisDirectory.listFiles();
                        for (File file : files)
                        {
                            if (file.getAbsolutePath().contains(arg))
                            {
                                tokenScriptFile = file;
                                break;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    if (tokenScriptFile == null)
                    {
                        System.out.println("ERROR: " + arg + " File not found.");
                    }
                    p = ARG;
                    break;
            }
        }

        if (tokenScriptFile != null)
        {
            dumpTokenInfo();
        }
    }

    private void showUsage()
    {
        System.out.println("Usage:");
        System.out.println("scripttool -tokenscript <TokenScript File> -address <Ethereum address>");
    }

    private void dumpTokenInfo()
    {
        try
        {
            TokenDefinition definition = getTokenDefinition();
            if (!checkValidity(definition)) return;

            ContractInfo holdingContract = definition.contracts.get(definition.holdingToken);
            System.out.println("Holding Token Name: " + definition.holdingToken);
            System.out.println("Token Addresses: ");
            for (Long chainId : holdingContract.addresses.keySet())
            {
                for (String addr : holdingContract.addresses.get(chainId))
                {
                    System.out.println(addr + " ChainID: " + chainId);
                }
            }

            //list actions:
            if (definition.actions != null && definition.actions.size() > 0)
            {
                System.out.println("Actions:");
                for (String actionName : definition.actions.keySet())
                {
                    TSAction action = definition.actions.get(actionName);
                    System.out.println(actionName);
                }
            }

            System.out.println();

            for (Long chainId : holdingContract.addresses.keySet())
            {
                for (String addr : holdingContract.addresses.get(chainId))
                {
                    System.out.println("Contract: " + addr + " ChainID: " + chainId + " Type: " + holdingContract.contractInterface);

                    System.out.println("{\n" + tokenAttributesToJson(holdingContract, new ContractAddress(chainId, addr), definition) + "\n}");
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("ERROR in TokenScript definition:");
            e.printStackTrace();
        }

        Runtime.getRuntime().exit(0);
    }

    private String tokenAttributesToJson(ContractInfo holdingContract, ContractAddress cAddr, TokenDefinition definition) throws java.lang.Exception {
        StringBuilder      tokenData = new StringBuilder();
        TransactionHandler txHandler = new TransactionHandler(cAddr.chainId);
        List<BigInteger>   balanceArray = null;

        String tokenName = txHandler.getNameOnly(cAddr.address);
        String symbol = txHandler.getSymbolOnly(cAddr.address);
        String nameWithSymbol = tokenName + "(" + symbol + ")";

        System.out.println("Contract Name (Eth): " + nameWithSymbol);

        System.out.println("JSON dictionary for token attributes:");
        System.out.println("-----------");

        if (holdingContract.contractInterface != null && holdingContract.contractInterface.equalsIgnoreCase("ERC875"))
        {
            //fetch balance array
            balanceArray = fetchArrayBalance(cAddr.address, userAddress, txHandler);
        }

        if (balanceArray == null || balanceArray.size() == 0){
            balanceArray = new ArrayList<>();
            balanceArray.add(BigInteger.ZERO);
        }

        tokenscriptFunction.resolveAttributes(userAddress.toString(), balanceArray.get(0), this, cAddr, definition)
                .forEach(attr -> TokenScriptResult.addPair(tokenData, "\"" + attr.id + "\"", attr.text))
                .isDisposed();

        return tokenData.toString() + "\"ownerAddress\": \"" + userAddress + "\"";
    }

    private boolean checkValidity(TokenDefinition definition)
    {
        if (definition == null)
        {
            System.out.println("ERROR: " + tokenScriptFile.getAbsolutePath() + " Invalid definition file");
            return false;
        }
        else if (definition.holdingToken == null)
        {
            System.out.println("ERROR: " + tokenScriptFile.getAbsolutePath() + " Has no <ts:ethereum contract=...> element");
            return false;
        }
        else return true;
    }

    private TokenDefinition getTokenDefinition() throws IOException, SAXException
    {
        TokenDefinition definition = null;
        try(FileInputStream in = new FileInputStream(tokenScriptFile)) {
            definition = new TokenDefinition(in, new Locale("en"), null);
        }

        return definition;
    }

    @Override
    public TransactionResult getFunctionResult(ContractAddress contract, Attribute attr,
                                               BigInteger tokenId)
    {
        String addressFunctionKey = contract.address + "-" + attr.name;
        TransactionResult tr = new TransactionResult(contract.chainId, contract.address, tokenId, attr);
        //existing entry in map?
        if (transactionResults.containsKey(contract.chainId))
        {
            Map<BigInteger, CachedResult> contractResult = transactionResults.get(contract.chainId).get(addressFunctionKey);
            if (contractResult != null && contractResult.containsKey(tokenId))
            {
                tr.resultTime = contractResult.get(tokenId).resultTime;
                tr.result = contractResult.get(tokenId).result;
            }
        }

        return tr;
    }

    @Override
    public TransactionResult storeAuxData(String wallet, TransactionResult tResult)
    {
        String addressFunctionKey = tResult.contractAddress + "-" + tResult.attrId;
        if (!transactionResults.containsKey(tResult.contractChainId)) transactionResults.put(tResult.contractChainId, new HashMap<>());
        if (!transactionResults.get(tResult.contractChainId).containsKey(addressFunctionKey)) transactionResults.get(tResult.contractChainId).put(addressFunctionKey, new HashMap<>());
        Map<BigInteger, CachedResult> tokenResultMap = transactionResults.get(tResult.contractChainId).get(addressFunctionKey);
        tokenResultMap.put(tResult.tokenId, new CachedResult(tResult.resultTime, tResult.result));
        transactionResults.get(tResult.contractChainId).put(addressFunctionKey, tokenResultMap);

        return tResult;
    }

    @Override
    public boolean resolveOptimisedAttr(ContractAddress contract, Attribute attr,
                                        TransactionResult transactionResult)
    {
        return false;
    }

    @Override
    public String getWalletAddr()
    {
        return userAddress.toString();
    }

    private List<BigInteger> fetchArrayBalance(
            String contractAddress, Address address,
            TransactionHandler txHandler) throws Exception {
        return txHandler.getBalanceArray(address.toString(), contractAddress);
    }
}
