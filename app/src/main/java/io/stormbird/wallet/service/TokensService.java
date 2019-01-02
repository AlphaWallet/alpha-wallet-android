package io.stormbird.wallet.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Observable;
import io.stormbird.wallet.entity.ContractType;
import io.stormbird.wallet.entity.ERC721Token;
import io.stormbird.wallet.entity.Token;

import static io.stormbird.wallet.C.ETHER_DECIMALS;

public class TokensService
{
    private Map<String, Token> tokenMap = new ConcurrentHashMap<>();
    private List<String> terminationList = new ArrayList<>();
    private static Map<String, ContractType> interfaceSpecMap = new ConcurrentHashMap<>();
    private Map<String, Long> updateMap = new ConcurrentHashMap<>();
    private String currentAddress = null;
    private int currentNetwork = 0;

    public TokensService() {

    }

    /**
     * Add the token to the service map and return token in case we use this call in a reactive element
     * @param t
     * @return
     */
    public Token addToken(Token t)
    {
        if (t.checkTokenNetwork(currentNetwork) && t.checkTokenWallet(currentAddress))
        {
            tokenMap.put(t.getAddress(), t);
            setSpec(t);
        }

        return t;
    }

    private void setSpec(Token t)
    {
        if (interfaceSpecMap.get(t.getAddress()) != null)
        {
            t.setInterfaceSpec(interfaceSpecMap.get(t.getAddress()));
        }
    }

    public Token getToken(String addr)
    {
        if (addr != null) return tokenMap.get(addr);
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
        currentNetwork = 0;
        tokenMap.clear();
    }

    public List<Token> getAllTokens()
    {
        return new ArrayList<Token>(tokenMap.values());
    }

    public List<Token> getAllLiveTokens()
    {
        List<Token> tokens = new ArrayList<>();
        tokens.add(tokenMap.get(currentAddress)); //currency token goes first
        for (Token t : tokenMap.values())
        {
            if (!t.isTerminated() && t.tokenInfo.name != null && !tokens.contains(t)) tokens.add(t);
        }

        return tokens;
    }

    public void scheduleForTermination(String address)
    {
        if (!terminationList.contains(address)) terminationList.add(address);
    }

    public List<String> getTerminationList()
    {
        return terminationList;
    }

    public void clearTerminationList()
    {
        terminationList.clear();
    }

    public void addTokens(Token[] tokens)
    {
        for (Token t : tokens)
        {
            t.setRequireAuxRefresh();
            if (t.checkTokenNetwork(currentNetwork) && t.checkTokenWallet(currentAddress))
            {
                tokenMap.put(t.getAddress(), t);
                setSpec(t);
            }
        }
    }

    public Observable<List<String>> reduceToUnknown(List<String> addrs)
    {
        return Observable.fromCallable(() -> {
            for (Token t : tokenMap.values())
            {
                if (addrs.contains(t.getAddress()))
                {
                    addrs.remove(t.getAddress());
                }
            }

            return addrs;
        });
    }

    public void setCurrentAddress(String currentAddress)
    {
        this.currentAddress = currentAddress;
    }
    public String getCurrentAddress() { return this.currentAddress; }

    public void setCurrentNetwork(int currentNetwork)
    {
        this.currentNetwork = currentNetwork;
    }

    public static void setInterfaceSpec(String address, ContractType functionSpec)
    {
        interfaceSpecMap.put(address, functionSpec);
    }

    public ContractType getInterfaceSpec(String address)
    {
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

    public void setLatestBlock(String address, long block)
    {
        updateMap.put(address, block);
    }

    public long getLatestBlock(String address)
    {
        if (updateMap.containsKey(address)) return updateMap.get(address);
        else return 0;
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
}
