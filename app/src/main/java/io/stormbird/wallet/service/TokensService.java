package io.stormbird.wallet.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.util.SparseArray;
import android.util.SparseLongArray;
import io.reactivex.Observable;
import io.stormbird.wallet.entity.ContractType;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.Token;

import static io.stormbird.wallet.C.ETHER_DECIMALS;

public class TokensService
{
    private final Map<String, SparseArray<Token>> tokenMap = new ConcurrentHashMap<>();
    private static final Map<String, SparseArray<ContractType>> interfaceSpecMap = new ConcurrentHashMap<>();
    private final Map<Integer, Token> currencies = new ConcurrentHashMap<>();
    private String currentAddress = null;
    private Token nextSelection;
    private boolean loaded;

    public TokensService() {
        nextSelection = null;
        loaded = false;
    }

    /**
     * Add the token to the service map and return token in case we use this call in a reactive element
     * @param t
     * @return
     */
    public Token addToken(Token t)
    {
        if (t.isEthereum())
        {
            currencies.put(t.tokenInfo.chainId, t);
            if (t.tokenInfo.chainId == 1) addToken(1, t);
        }
        else if (t.checkTokenWallet(currentAddress))
        {
            addToken(t.tokenInfo.chainId, t);
        }

        return t;
    }

    private void addToken(int chainId, Token t)
    {
        SparseArray<Token> tokenAddr = tokenMap.get(t.getAddress());
        if (tokenAddr == null)
        {
            tokenAddr = new SparseArray<>();
            tokenMap.put(t.getAddress(), tokenAddr);
        }
        tokenAddr.put(chainId, t);
        setSpec(t);
    }

    private void setSpec(Token t)
    {
        SparseArray<ContractType> types = interfaceSpecMap.get(t.getAddress());
        if (types != null && types.get(t.tokenInfo.chainId, null) != null)
        {
            if (t.getInterfaceSpec() == null || t.getInterfaceSpec() == ContractType.NOT_SET || t.getInterfaceSpec() == ContractType.OTHER)
            {
                t.setInterfaceSpec(interfaceSpecMap.get(t.getAddress()).get(t.tokenInfo.chainId));
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
            else if (tokenMap.containsKey(addr))
            {
                return tokenMap.get(addr).get(chainId, null);
            }
        }

        return null;
    }

    public String getTokenName(int chainId, String addr)
    {
        if (addr == null) return "[Unknown contract]";
        String name = addr;
        Token token = getToken(chainId, addr);
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

    public String getTokenSymbol(int chainId, String addr)
    {
        String symbol = "TOK";
        if (addr == null) return symbol;
        Token token = getToken(chainId, addr);
        if (token != null)
        {
            symbol = token.tokenInfo.symbol;
        }

        return symbol;
    }

    public int getTokenDecimals(int chainId, String addr)
    {
        int decimals = ETHER_DECIMALS;
        if (addr == null) return decimals;
        Token token = getToken(chainId, addr);
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
        List<Token> tokens = new ArrayList<>();
        for (String address : tokenMap.keySet())
        {
            tokens.addAll(getAllAtAddress(address));
        }

        return tokens;
    }

    private List<Token> getAllAtAddress(String addr)
    {
        List<Token> tokens = new ArrayList<>();
        SparseArray<Token> locals = tokenMap.get(addr);
        if (locals != null)
        {
            for (int i = 0; i < locals.size(); i++)
            {
                tokens.add(locals.valueAt(i));
            }
        }

        return tokens;
    }

    public List<Token> getAllLiveTokens()
    {
        List<Token> tokens = new ArrayList<>(new ArrayList(currencies.values()));

        for (String addr : tokenMap.keySet())
        {
            List<Token> chainTokens = getAllAtAddress(addr);
            for (Token t : chainTokens)
            {
                if (!t.isTerminated() && t.tokenInfo.name != null && !tokens.contains(t))
                    tokens.add(t);
            }
        }

        return tokens;
    }

    public void addTokens(Token[] tokens)
    {
        for (Token t : tokens)
        {
            t.setRequireAuxRefresh();
            addToken(t);
        }

        loaded = true;
    }

    public List<String> reduceToUnknown(List<String> addrs)
    {
        List<Token> allTokens = getAllTokens();
        for (Token t : allTokens)
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

    public static void setInterfaceSpec(int chainId, String address, ContractType functionSpec)
    {
        SparseArray<ContractType> types = interfaceSpecMap.get(address);
        if (types == null)
        {
            types = new SparseArray<>();
            interfaceSpecMap.put(address, types);
        }
        types.put(chainId, functionSpec);
    }

    public static ContractType checkInterfaceSpec(int chainId, String address)
    {
        SparseArray<ContractType> types = interfaceSpecMap.get(address);
        ContractType type = types != null ? type = types.get(chainId) : null;
        if (type != null)
        {
            return type;
        }
        else
        {
            return ContractType.NOT_SET;
        }
    }

    public ContractType getInterfaceSpec(int chainId, String address)
    {
        SparseArray<ContractType> types = interfaceSpecMap.get(address);
        ContractType result = types != null ? result = types.get(chainId) : ContractType.OTHER;

        if (result == ContractType.OTHER && tokenMap.containsKey(address))
        {
            List<Token> tokens = getAllAtAddress(address);
            for (Token token : tokens)
            {
                if (token.tokenInfo.chainId == chainId && token.getInterfaceSpec() != null)
                {
                    result = token.getInterfaceSpec();
                    break;
                }
            }
        }

        return result;
    }

    public List<Token> getAllClass(Class<?> tokenClass)
    {
        List<Token> classTokens = new ArrayList<>();
        for (Token t : getAllTokens())
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
        for (Token t : getAllTokens())
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

    public void updateTokenPressure()
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
            nextSelection = highestPressureToken;
        }
        else
        {
            nextSelection = null;
        }
    }

    public Token getNextSelection()
    {
        Token next = nextSelection;
        if (next != null) next.balanceUpdatePressure = 0.0f;
        nextSelection = null;
        return next;
    }

    public boolean checkHasLoaded()
    {
        return loaded;
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
