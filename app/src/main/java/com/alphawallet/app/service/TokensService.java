package com.alphawallet.app.service;

import android.util.SparseArray;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;

public class TokensService
{
    private final Map<String, SparseArray<Token>> tokenMap = new ConcurrentHashMap<>();
    private static final Map<String, SparseArray<ContractType>> interfaceSpecMap = new ConcurrentHashMap<>();
    private static String currentAddress = null;
    private boolean loaded;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokenRepositoryType tokenRepository;
    private final List<Integer> networkFilter;
    private ContractLocator focusToken;
    private final OkHttpClient okHttpClient;
    private int currencyCheckCount;

    public TokensService(EthereumNetworkRepositoryType ethereumNetworkRepository,
                         TokenRepositoryType tokenRepository,
                         OkHttpClient client,
                         PreferenceRepositoryType preferenceRepository) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokenRepository = tokenRepository;
        loaded = false;
        networkFilter = new ArrayList<>();
        setupFilter();
        focusToken = null;
        okHttpClient = client;
        currencyCheckCount = 0;
        setCurrentAddress(preferenceRepository.getCurrentWalletAddress()); //set current wallet address at service startup
    }

    /**
     * Add the token to the service map and return token in case we use this call in a reactive element
     * @param
     * @return
     */
    public Token addToken(Token t)
    {
        if (t.checkTokenWallet(currentAddress))
        {
            if (focusToken != null && focusToken.equals(t))
            {
                t.setFocus(true);
            }

            if (!t.isEthereum()) t.ticker = ethereumNetworkRepository.getTokenTicker(t);
            addToken(t.tokenInfo.chainId, t);
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
            tokenMap.put(t.getAddress(), tokenAddr);
        }
        else if (tokenAddr.get(chainId) == null)
        {
            SparseArray<Token> replacementArray = new SparseArray<>(tokenAddr.size() + 1);
            for (int i = 0; i < tokenAddr.size(); i++) replacementArray.put(tokenAddr.keyAt(i), tokenAddr.valueAt(i));
            tokenAddr = replacementArray;
            tokenMap.put(t.getAddress(), tokenAddr);
        }

        if (updatedSpec(t))
        {
            //store new spec in DB
            t = tokenRepository.updateTokenType(t, new Wallet(currentAddress), t.getInterfaceSpec());
        }

        tokenAddr.put(chainId, t);
    }

    public Token getToken(int chainId, String addr)
    {
        Token token = null;
        if (addr != null)
        {
            if (tokenMap.containsKey(addr.toLowerCase()))
            {
                token = tokenMap.get(addr.toLowerCase()).get(chainId, null);
            }

            if (token == null)
            {
                token = tokenRepository.fetchToken(chainId, currentAddress, addr);
            }
        }

        return token;
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
            symbol = token.getSymbol();
        }

        return symbol;
    }

    public Token getRequiresTransactionUpdate(Token pending)
    {
        int chainToUpdate = pending != null ? pending.tokenInfo.chainId : 0;
        for (Token check : getAllLiveTokens())
        {
            if (check.requiresTransactionRefresh(chainToUpdate))
            {
                return check;
            }
        }

        return null;
    }

    public int getTokenDecimals(int chainId, String addr)
    {
        int decimals = C.ETHER_DECIMALS;
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
        currencyCheckCount = 0;
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

    public List<Token> getAllAtAddress(String addr)
    {
        List<Token> tokens = new ArrayList<>();
        if (addr == null) return tokens;
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

    /**
     * Return all tokens after applying the networkId filters
     *
     * @return list of network filtered tokens
     */
    public List<Token> getAllLiveTokens()
    {
        List<Token> tokens = new ArrayList<>();
        for (String addr : tokenMap.keySet())
        {
            List<Token> chainTokens = getAllAtAddress(addr);
            for (Token t : chainTokens)
            {
                if (!t.isTerminated() && t.tokenInfo.name != null)
                    tokens.add(t);
            }
        }

        return tokens;
    }

    /**
     * Add these new tokens, return a list of new tokens or balance change tokens
     * @param tokens
     * @return
     */
    public Token[] addTokens(Token[] tokens)
    {
        List<Token> changedTokens = new ArrayList<>();
        for (Token t : tokens)
        {
            if (t.isBad() || t.getInterfaceSpec() == ContractType.OTHER) continue;
            Token check = getToken(t.tokenInfo.chainId, t.tokenInfo.address);
            if (check == null || t.checkBalanceChange(check) || t.checkTickerChange(check))
            {
                changedTokens.add(t);
            }
            addToken(t);
        }

        loaded = true;
        return changedTokens.toArray(new Token[0]);
    }

    public List<ContractLocator> reduceToUnknown(List<ContractLocator> contracts)
    {
        List<ContractLocator> unknowns = new ArrayList<>();

        for (ContractLocator r : contracts)
        {
            Token check = getToken(r.chainId, r.address.toLowerCase());
            if (check == null)
            {
                unknowns.add(r);
            }
        }

        return unknowns;
    }

    public void setCurrentAddress(String newWalletAddr)
    {
        if (newWalletAddr != null) this.currentAddress = newWalletAddr.toLowerCase();
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
        ContractType type = types != null ? types.get(chainId) : null;
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

    private boolean updatedSpec(Token token)
    {
        String normalisedAddr = token.getAddress().toLowerCase();
        SparseArray<ContractType> types = interfaceSpecMap.get(normalisedAddr);
        if (types != null)
        {
            ContractType updatedType = types.get(token.tokenInfo.chainId);
            if (updatedType != null)
            {
                token.setInterfaceSpec(updatedType);
                //clean up the revise spec map
                interfaceSpecMap.get(normalisedAddr).delete(token.tokenInfo.chainId);
                if (interfaceSpecMap.get(normalisedAddr).size() == 0)
                {
                    interfaceSpecMap.remove(normalisedAddr);
                }
                return true;
            }
        }

        return false;
    }

    private List<Token> getAllClass(int chainId, ContractType[] filter)
    {
        List<ContractType> filterList = Arrays.asList(filter);
        List<Token> classTokens = new ArrayList<>();
        for (Token t : getAllTokens())
        {
            if (t.tokenInfo.chainId == chainId && filterList.contains(t.getInterfaceSpec()))
            {
                classTokens.add(t);
            }
        }
        return classTokens;
    }


    public Token getNextInBalanceUpdateQueue()
    {
        //calculate update based on last update time & importance
        float highestWeighting = 0;
        Token highestToken = checkCurrencies(); //do an initial check of the base currencies eg on refresh or wallet init.
        long currentTime = System.currentTimeMillis();

        if (highestToken != null) return highestToken;

        for (Token check : getAllLiveTokens())
        {
            long lastUpdateDiff = currentTime - check.updateBlancaTime;
            float weighting = check.balanceUpdateWeight;

            //simply multiply the weighting by the last diff.
            float updateFactor = weighting * (float) lastUpdateDiff;
            long cutoffCheck = 30*1000; //maximum update time given by formula (30 seconds / weighting)
            if (check.isERC721()) continue; //no need to check ERC721
            if (!check.isEthereum() && !check.tokenInfo.isEnabled) continue; //don't check disabled tokens

            if (updateFactor > highestWeighting && (updateFactor > (float)cutoffCheck || check.refreshCheck))
            {
                highestWeighting = updateFactor;
                highestToken = check;
            }
        }

        if (highestToken != null)
        {
            highestToken.updateBlancaTime = System.currentTimeMillis();
            highestToken.refreshCheck = false;
        }

        return highestToken;
    }

    private Token checkCurrencies()
    {
        if (currencyCheckCount >= networkFilter.size()) return null;
        int chainId;
        try
        {
            chainId = networkFilter.get(currencyCheckCount);
        }
        catch (IndexOutOfBoundsException e)
        {
            return null;
        }
        currencyCheckCount++;
        return getToken(chainId, currentAddress);
    }

    public void setFocusToken(Token token)
    {
        focusToken = new ContractLocator(token.getAddress(), token.tokenInfo.chainId);
        addToken(token);
    }

    public void clearFocusToken()
    {
        if (focusToken != null)
        {
            Token fToken = getToken(focusToken.chainId, focusToken.address);
            if (fToken != null) fToken.setFocus(false);
        }
        focusToken = null;
    }

    public boolean checkHasLoaded()
    {
        return loaded;
    }

    public List<Integer> getNetworkFilters()
    {
        return networkFilter;
    }

    public void requireTokensRefresh()
    {
        for (Token t : getAllLiveTokens()) t.refreshCheck = true;
    }

    /**
     * Fetch the inverse of the intersection between displayed tokens and the balance received from Opensea
     * If a token was transferred out then it should no longer be displayed
     * This is needed because if a token has been transferred out and the balance is now zero, it will not be
     * in the list of tokens from opensea. The only way to determine zero balance is by comparing to previous token balance
     *
     * TODO: use balanceOf function to double check zeroised balance
     *
     */
    public List<Token> getChangedTokenBalance(int chainId, Token[] tokens, ContractType[] filterTypes)
    {
        List<Token> existingTokens = getAllClass(chainId, filterTypes);
        List<Token> openSeaRefreshedTokens = new ArrayList<>(Arrays.asList(tokens));
        List<Token> balanceChangedTokens = new ArrayList<>();
        List<String> refreshedTokenAddrs = new ArrayList<>();

        for (Token newToken : openSeaRefreshedTokens)
        {
            refreshedTokenAddrs.add(newToken.getAddress().toLowerCase());
            Token existingToken = getToken(newToken.tokenInfo.chainId, newToken.tokenInfo.address);
            if (existingToken == null || existingToken.checkBalanceChange(newToken))
            {
                //opensea is potentially unreliable for ERC721 Ticket class
                if (existingToken != null && existingToken.isERC721Ticket()) continue; //don't take the opensea balance for ERC721 ticket if the token is already known (use contract's getBalance instead).
                balanceChangedTokens.add(newToken);
                addToken(newToken);
            }
        }

        for (Token existingToken : existingTokens)
        {
            if (!refreshedTokenAddrs.contains(existingToken.tokenInfo.address.toLowerCase()))
            {
                existingToken.zeroiseBalance();
                balanceChangedTokens.add(existingToken);
                addToken(existingToken);
            }
        }

        return balanceChangedTokens;
    }

    public void markTokenUpdated(Token toUpdate)
    {
        Token t = getToken(toUpdate.tokenInfo.chainId, toUpdate.tokenInfo.address);
        if (t != null && t.walletUIUpdateRequired && t.getInterfaceSpec() == toUpdate.getInterfaceSpec() && t.getFullBalance().equals(toUpdate.getFullBalance()))
        {
            t.walletUIUpdateRequired = false;
        }
    }

    public TokenTicker getTokenTicker(Token token)
    {
        return ethereumNetworkRepository.getTokenTicker(token);
    }

    public static String getCurrentWalletAddress()
    {
        return currentAddress;
    }

    /**
     * Get all tokens of type. This exists mainly because we can't trust the balance returned for ERC721 Ticket from opensea
     *
     * @param filterTypes
     * @return
     */
    public Token[] getAllTokens(ContractType[] filterTypes)
    {
        List<ContractType> filterList = Arrays.asList(filterTypes);
        List<Token> classTokens = new ArrayList<>();
        for (Token t : getAllTokens())
        {
            if (filterList.contains(t.getInterfaceSpec()))
            {
                classTokens.add(t);
            }
        }

        return classTokens.toArray(new Token[0]);
    }

    public void updateTokenViewSizes(Token updatedToken)
    {
        Token cachedToken = getToken(updatedToken.tokenInfo.chainId, updatedToken.getAddress());
        if (cachedToken != null)
        {
            cachedToken.iconifiedWebviewHeight = updatedToken.iconifiedWebviewHeight;
            cachedToken.nonIconifiedWebviewHeight = updatedToken.nonIconifiedWebviewHeight;
        }
    }

    public String getNetworkName(int chainId)
    {
        NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (info != null) return info.getShortName();
        else return "";
    }
}
