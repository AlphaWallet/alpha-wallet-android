package io.stormbird.wallet.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.stormbird.wallet.entity.Token;

public class TokensService
{
    private Map<String, Token> tokenMap = new ConcurrentHashMap<>();
    private List<String> terminationList = new ArrayList<>();
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
        }
        return t;
    }

    public Token getToken(String addr)
    {
        return tokenMap.get(addr);
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
        for (Token t : tokenMap.values())
        {
            if (!t.isTerminated() && t.tokenInfo.name != null) tokens.add(t);
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
            if (t.checkTokenNetwork(currentNetwork) && t.checkTokenWallet(currentAddress))
            {
                tokenMap.put(t.getAddress(), t);
            }
        }
    }


    public Observable<List<String>> reduceToUnknown(List<String> addrs)
    {
        return Observable.fromCallable(() -> {
            List<String> addresses = new ArrayList<>();
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

    public void tokenContractUpdated(Token token, long blockNumber)
    {
        updateMap.put(token.getAddress(), blockNumber);
    }

    public long getLastBlock(Token token)
    {
        if (updateMap.get(token.getAddress()) != null)
        {
            return updateMap.get(token.getAddress());
        }
        else
        {
            return 0;
        }
    }

    public void setCurrentAddress(String currentAddress)
    {
        this.currentAddress = currentAddress;
    }

    public void setCurrentNetwork(int currentNetwork)
    {
        this.currentNetwork = currentNetwork;
    }
}
