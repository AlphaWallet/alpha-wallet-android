package io.stormbird.wallet.service;

import android.util.SparseArray;
import io.stormbird.wallet.entity.ContractResult;
import io.stormbird.wallet.entity.ContractType;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static io.stormbird.wallet.C.ETHER_DECIMALS;

public class TokensService
{
    private final Map<String, SparseArray<Token>> tokenMap = new ConcurrentHashMap<>();
    private static final Map<String, SparseArray<ContractType>> interfaceSpecMap = new ConcurrentHashMap<>();
    private final Map<Integer, Token> currencies = new ConcurrentHashMap<>();
    private String currentAddress = null;
    private boolean loaded;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final List<Integer> networkFilter;
    private final ConcurrentLinkedQueue<Token> transactionUpdateQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Token> balanceUpdateQueue = new ConcurrentLinkedQueue<>();
    private Token focusToken;

    public TokensService(EthereumNetworkRepositoryType ethereumNetworkRepository) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        loaded = false;
        networkFilter = new ArrayList<>(10);
        setupFilter();
        focusToken = null;
    }

    /**
     * Add the token to the service map and return token in case we use this call in a reactive element
     * @param t
     * @return
     */
    public Token addToken(Token t)
    {
        if (t.checkTokenWallet(currentAddress))
        {
            if (t.equals(focusToken))
            {
                t.balanceUpdateWeight = focusToken.balanceUpdateWeight;
                focusToken = t;
            }

            if (t.requiresTransactionRefresh())
            {
                transactionUpdateQueue.add(t);
            }

            if (t.isEthereum())
            {
                currencies.put(t.tokenInfo.chainId, t);
                //if (t.tokenInfo.chainId == 1) addToken(1, t);
            }
            else
            {
                addToken(t.tokenInfo.chainId, t);
            }
            return t;
        }
        else
        {
            return null;
        }
    }

    private void addToken(int chainId, Token t)
    {
        SparseArray<Token> tokenAddr = tokenMap.get(t.getAddress());

        //conserve space; contracts with the same address are rare (but may become more common with Plasma).
        if (tokenAddr == null)
        {
            tokenAddr = new SparseArray<>(1);
        }
        else
        {
            SparseArray<Token> replacementArray = new SparseArray<>(tokenAddr.size() + 1);
            for (int i = 0; i < tokenAddr.size(); i++) replacementArray.put(tokenAddr.keyAt(i), tokenAddr.valueAt(i));
            tokenAddr = replacementArray;
        }

        tokenMap.put(t.getAddress(), tokenAddr);
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
            if (addr.equalsIgnoreCase(currentAddress))
            {
                return currencies.get(chainId);
            }
            else if (tokenMap.containsKey(addr.toLowerCase()))
            {
                return tokenMap.get(addr.toLowerCase()).get(chainId, null);
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

    public Token getRequiresTransactionUpdate()
    {
        return transactionUpdateQueue.poll();
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
        currencies.clear();
        tokenMap.clear();
        transactionUpdateQueue.clear();
        balanceUpdateQueue.clear();
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
                if (networkFilter.contains(locals.keyAt(i))) tokens.add(locals.valueAt(i));
            }
        }

        return tokens;
    }

    public List<Token> getAllLiveTokens()
    {
        List<Token> tokens = new ArrayList<>();
        for (Integer chainId : currencies.keySet())
        {
            if (networkFilter.contains(chainId)) tokens.add(currencies.get(chainId));
        }

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

    public List<ContractResult> reduceToUnknown(List<ContractResult> contracts)
    {
        List<ContractResult> unknowns = new ArrayList<>();

        for (ContractResult r : contracts)
        {
            Token check = getToken(r.chainId, r.name.toLowerCase());
            if (check == null)
            {
                unknowns.add(r);
            }
        }

        return unknowns;
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

    public void setupFilter()
    {
        networkFilter.clear();
        networkFilter.addAll(ethereumNetworkRepository.getFilterNetworkList());
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

    public List<Token> getAllClass(int chainId, Class<?> tokenClass)
    {
        List<Token> classTokens = new ArrayList<>();
        for (Token t : getAllTokens())
        {
            if (tokenClass.isInstance(t) && t.tokenInfo.chainId == chainId)
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
     * If a token was transferred out then it should no longer be displayed
     * This is needed because if a token has been transferred out and the balance is now zero, it will not be
     * in the list of tokens from opensea. The only way to determine zero balance is by comparing to previous token balance
     *
     * TODO: use balanceOf function to double check zeroised balance
     *
     * @param tokens array of tokens with active balance
     * @param tokenClass type of token to filter (eg erc721)
     */
    public Token[] zeroiseBalanceOfSpentTokens(int chainId, Token[] tokens, Class<?> tokenClass)
    {
        List<Token> existingTokens = getAllClass(chainId, tokenClass);
        List<Token> openSeaRefreshTokens = new ArrayList<>(Arrays.asList(tokens));

        for (Token newToken : openSeaRefreshTokens)
        {
            for (Token existingToken : existingTokens)
            {
                if (newToken.getAddress().equals(existingToken.getAddress()))
                {
                    existingTokens.remove(existingToken);
                    break;
                }
            }
        }

        //should only be left with a list of tokens with now zero balance
        for (Token existingToken : existingTokens)
        {
            existingToken.zeroiseBalance();
            openSeaRefreshTokens.add(existingToken);
        }

        return openSeaRefreshTokens.toArray(new Token[0]);
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

    public void updateTokenPressure(boolean isVisible)
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

            t.updateBalanceCheckPressure(isVisible);
        }

        if (highestPressure > 20.0f)
        {
            if (!balanceUpdateQueue.contains(highestPressureToken))
            {
                balanceUpdateQueue.add(highestPressureToken);
            }
            highestPressureToken.balanceUpdatePressure = 0.0f;
        }
    }

    public Token getNextInBalanceUpdateQueue()
    {
        Token queueToken = balanceUpdateQueue.poll();
        if (queueToken != null)
        {
            return getToken(queueToken.tokenInfo.chainId, queueToken.getAddress());
        }
        else
        {
            return null;
        }
    }

    public void setFocusToken(Token token)
    {
        focusToken = token;
        focusToken.setFocus(true);
        addToken(focusToken);
    }

    public void clearFocusToken()
    {
        if (focusToken != null) focusToken.setFocus(false);
        focusToken = null;
    }

    public boolean checkHasLoaded()
    {
        return loaded;
    }

    public void randomiseInitialPressure(Token[] cachedTokens)
    {
        for (Token t : cachedTokens)
        {
            t.balanceUpdatePressure += (float)(Math.random()*15.0f);
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
