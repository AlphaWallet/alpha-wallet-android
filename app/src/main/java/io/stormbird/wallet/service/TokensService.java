package io.stormbird.wallet.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.util.SparseLongArray;
import io.reactivex.Observable;
import io.stormbird.wallet.entity.ContractType;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.Token;

import static io.stormbird.wallet.C.ETHER_DECIMALS;

public class TokensService
{
    private final Map<String, Token> tokenMap = new ConcurrentHashMap<>();
    private static final Map<String, ContractType> interfaceSpecMap = new ConcurrentHashMap<>();
    private final Map<Integer, Token> currencies = new ConcurrentHashMap<>();
    private String currentAddress = null;
    private boolean tokenTerminated = false;

    public TokensService() {

    }

    /**
     * Add the token to the service map and return token in case we use this call in a reactive element
     * @param t
     * @return
     */
    public Token addToken(Token t)
    {
        boolean added = false;
        if (t.isEthereum())
        {
            if (!currencies.containsKey(t.tokenInfo.chainId)) added = true;
            currencies.put(t.tokenInfo.chainId, t);
            if (t.tokenInfo.chainId == 1) tokenMap.put(t.getAddress(), t);
        }
        else if (t.checkTokenWallet(currentAddress))
        {
            if (!tokenMap.containsKey(currentAddress)) added = true;
            tokenMap.put(t.getAddress(), t);
            setSpec(t);
        }

        return t;
    }

    private void setSpec(Token t)
    {
        if (interfaceSpecMap.get(t.getAddress()) != null)
        {
            if (t.getInterfaceSpec() == null || t.getInterfaceSpec() == ContractType.NOT_SET || t.getInterfaceSpec() == ContractType.OTHER)
            {
                t.setInterfaceSpec(interfaceSpecMap.get(t.getAddress()));
            }
        }
    }

    public Token getToken(int chainId, String addr)
    {
        if (addr != null)
        {
            if (addr.equals(currentAddress))
            {
                return currencies.get(chainId);
            }
            else
            {
                return tokenMap.get(addr);
            }
        }
        else return null;
    }

    public String getTokenName(String addr)
    {
        if (addr == null) return "[Unknown contract]";
        String name = addr;
        Token token = tokenMap.get(addr);
        if (token != null)
        {
            if (token.isTerminated())
            {
                name = "[deleted contract]";
            }
            else if (!token.isBad())
            {
                name = token.getFullName();
            }
        }

        return name;
    }

    public String getTokenSymbol(String addr)
    {
        String symbol = "TOK";
        if (addr == null) return symbol;
        Token token = tokenMap.get(addr);
        if (token != null)
        {
            symbol = token.tokenInfo.symbol;
        }

        return symbol;
    }

    public int getTokenDecimals(String addr)
    {
        int decimals = ETHER_DECIMALS;
        if (addr == null) return decimals;
        Token token = tokenMap.get(addr);
        if (token != null)
        {
            decimals = token.tokenInfo.decimals;
        }

        return decimals;
    }

    public void clearTokens()
    {
        currentAddress = "";
        tokenMap.clear();
    }

    public List<Token> getAllTokens()
    {
        return new ArrayList<Token>(tokenMap.values());
    }

    public List<Token> getAllLiveTokens()
    {
        List<Token> tokens = new ArrayList<>(new ArrayList(currencies.values()));

        for (Token t : tokenMap.values())
        {
            if (!t.isEthereum() && !t.isTerminated() && t.tokenInfo.name != null && !tokens.contains(t)) tokens.add(t);
        }

        return tokens;
    }

    public boolean getTerminationReport()
    {
        boolean terminated = tokenTerminated;
        tokenTerminated = false;
        return terminated;
    }

    public void setTerminationFlag()
    {
        tokenTerminated = true; //walletView picks this up to perform a refresh
    }

    public void addTokens(Token[] tokens)
    {
        for (Token t : tokens)
        {
            t.setRequireAuxRefresh();
            addToken(t);
        }
    }

    public List<String> reduceToUnknown(List<String> addrs)
    {
        for (Token t : tokenMap.values())
        {
            if (addrs.contains(t.getAddress()))
            {
                addrs.remove(t.getAddress());
            }
        }

        return addrs;
    }

    public void setCurrentAddress(String currentAddress)
    {
        this.currentAddress = currentAddress.toLowerCase();
    }
    public String getCurrentAddress() { return this.currentAddress; }

    public static void setInterfaceSpec(String address, ContractType functionSpec)
    {
        interfaceSpecMap.put(address.toLowerCase(), functionSpec);
    }

    public static ContractType checkInterfaceSpec(String address)
    {
        ContractType type = interfaceSpecMap.get(address.toLowerCase());
        if (type != null)
        {
            return type;
        }
        else
        {
            return ContractType.NOT_SET;
        }
    }

    public ContractType getInterfaceSpec(String address)
    {
        address = address.toLowerCase();
        ContractType result = ContractType.OTHER;
        if (interfaceSpecMap.containsKey(address)) return interfaceSpecMap.get(address);
        else if (tokenMap.containsKey(address))
        {
            Token token = tokenMap.get(address);
            if (token.getInterfaceSpec() != null)
            {
                result = tokenMap.get(address).getInterfaceSpec();
            }
        }

        return result;
    }

    public List<Token> getAllClass(Class<?> tokenClass)
    {
        List<Token> classTokens = new ArrayList<>();
        for (Token t : tokenMap.values())
        {
            if (tokenClass.isInstance(t))
            {
                classTokens.add(t);
            }
        }
        return classTokens;
    }

    public void clearBalanceOf(Class<?> tokenClass)
    {
        for (Token t : tokenMap.values())
        {
            if (tokenClass.isInstance(t))
            {
                ((ERC721Token)t).tokenBalance.clear();
            }
        }
    }

    /**
     * Fetch the inverse of the intersection between displayed tokens and the balance received from Opensea
     * If a token was transferred out then it will no longer be displayed
     *
     * @param tokens array of tokens with active balance
     * @param tokenClass type of token to filter (eg erc721)
     * @return
     */
    public List<String> getRemovedTokensOfClass(Token[] tokens, Class<?> tokenClass)
    {
        List<String> newTokens = getAddresses(tokens);
        List<Token> oldTokens = getAllClass(tokenClass);

        List<String> removedTokens = new ArrayList<>();

        for (Token s : oldTokens)
        {
            if (!newTokens.contains(s.getAddress())) removedTokens.add(s.getAddress());
        }

        return removedTokens;
    }

    private List<String> getAddresses(Token[] tokens)
    {
        List<String> addresses = new ArrayList<>();
        for (Token t : tokens)
        {
            addresses.add(t.getAddress());
        }

        return addresses;
    }

    public Token getNextUpdateToken()
    {
        //get all tokens
        List<Token> allTokens = getAllLiveTokens();
        Token highestPressureToken = null;
        float highestPressure = 0.0f;

        for (Token t : allTokens)
        {
            if (t.balanceUpdatePressure > highestPressure)
            {
                highestPressureToken = t;
                highestPressure = t.balanceUpdatePressure;
            }

            t.updateBalanceCheckPressure();
        }

        if (highestPressure > 10.0f)
        {
            highestPressureToken.balanceUpdatePressure = 0.0f;
            return highestPressureToken;
        }
        else
        {
            return null;
        }
    }

    //    public long getLastTransactionFetch(int chainId, String address)
//    {
//        SparseLongArray chainMap = transactionUpdateMap.get(address);
//        if (chainMap == null)
//        {
//            chainMap = new SparseLongArray();
//            transactionUpdateMap.put(address, chainMap);
//        }
//
//        long value = chainMap.valueAt(chainId);
//
//        return value;
//    }
//
//    public void setNextTransactionFetch(int chainId, String address, long updateTime)
//    {
//        SparseLongArray chainMap = transactionUpdateMap.get(address);
//        if (chainMap == null)
//        {
//            chainMap = new SparseLongArray();
//            transactionUpdateMap.put(address, chainMap);
//        }
//
//        chainMap.put(chainId, updateTime);
//    }
}
