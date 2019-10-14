package com.alphawallet.app.service;

import android.util.SparseArray;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenFactory;
import com.alphawallet.app.entity.TokenInfo;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;

import io.reactivex.Observable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokensService
{
    private final Map<String, SparseArray<Token>> tokenMap = new ConcurrentHashMap<>();
    private static final Map<String, SparseArray<ContractType>> interfaceSpecMap = new ConcurrentHashMap<>();
    private String currentAddress = null;
    private boolean loaded;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final RealmManager realmManager;
    private final List<Integer> networkFilter;
    private Token focusToken;
    private final OkHttpClient okHttpClient;
    private int currencyCheckCount;

    public TokensService(EthereumNetworkRepositoryType ethereumNetworkRepository, RealmManager realmManager, OkHttpClient client) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.realmManager = realmManager;
        loaded = false;
        networkFilter = new ArrayList<>(10);
        setupFilter();
        focusToken = null;
        okHttpClient = client;
        currencyCheckCount = 0;
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
        if (addr != null && tokenMap.containsKey(addr.toLowerCase()))
        {
            return tokenMap.get(addr.toLowerCase()).get(chainId, null);
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
        for (Token check : getAllLiveTokens())
        {
            if (check.requiresTransactionRefresh())
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
                if (!t.isTerminated() && t.tokenInfo.name != null && !tokens.contains(t))
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
            if (t == null || t.isBad() || t.getInterfaceSpec() == ContractType.OTHER) continue;
            Token check = getToken(t.tokenInfo.chainId, t.tokenInfo.address);
            if (check == null || t.checkBalanceChange(check))
            {
                changedTokens.add(t);
            }
            addToken(t);
        }

        loaded = true;
        return changedTokens.toArray(new Token[0]);
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
        ContractType result = types != null ? types.get(chainId) : ContractType.OTHER;

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

    private List<Token> getAllClass(int chainId, Class<?> tokenClass)
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
            long cutoffCheck = check.isEthereum() ? 20*1000 : check.isERC875() ? 30*1000 : 40*1000; //ERC20's get updated from blockscout
            if (updateFactor > highestWeighting && (updateFactor > (float)cutoffCheck || check.refreshCheck)) // don't add to list if updated in the last 20 seconds
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
        int chainId = networkFilter.get(currencyCheckCount);
        currencyCheckCount++;
        return getToken(chainId, currentAddress);
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


    //Token balance service

    /**
     * returns a list of tokens found at this address
     * @return List of Token at current address
     */
    public Observable<Token[]> getTokensAtAddress()
    {
        //create filter list
        return Observable.fromIterable(networkFilter)
                .flatMap(this::getTokensOnNetwork);
    }

    private Observable<Token[]> getTokensOnNetwork(int chainId)
    {
        return Observable.fromCallable(() -> {
            NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
            String blockscoutURL = EthereumNetworkRepository.BLOCKSCOUT_API + info.blockscoutAPI + EthereumNetworkRepository.BLOCKSCOUT_TOKEN_ARGS + currentAddress;
            //make call to blockscout
            Request request = new Request.Builder()
                    .url(blockscoutURL)
                    .get()
                    .build();

            okhttp3.Response response = okHttpClient.newCall(request).execute();

            List<Token> tokenList = new ArrayList<>();

            if (response.code() == HttpURLConnection.HTTP_OK && response.body() != null)
            {
                handleTokenList(chainId, tokenList, response.body().string());
            }

            return tokenList.toArray(new Token[0]);
        });
    }

    private void handleTokenList(int chainId, List<Token> tokenList, String string)
    {
        //parse JSON
        try
        {
            JSONObject json = new JSONObject(string);
            JSONArray tokens = json.getJSONArray("result");
            TokenFactory tf = new TokenFactory();
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId);

            for (int i = 0; i < tokens.length(); i++)
            {
                JSONObject t = (JSONObject)tokens.get(i);
                String balanceStr = t.getString("balance");
                if (balanceStr.length() == 0 || balanceStr.equals("0")) continue;
                String decimalsStr = t.getString("decimals");
                int decimals = (decimalsStr.length() > 0) ? Integer.parseInt(decimalsStr) : 0;
                TokenInfo info = new TokenInfo(t.getString("contractAddress"), t.getString("name"), t.getString("symbol"), decimals, true, chainId);
                //now create token with balance info, only for ERC20 for now
                if (decimalsStr.length() > 0)
                {
                    BigDecimal balance = new BigDecimal(balanceStr);
                    Token newToken = tf.createToken(info, balance, null, System.currentTimeMillis(), ContractType.ERC20, network.getShortName(), System.currentTimeMillis());
                    newToken.setTokenWallet(currentAddress);
                    newToken.refreshCheck = false;
                    tokenList.add(newToken);
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    public List<Integer> getNetworkFilters()
    {
        return networkFilter;
    }

    public void requireTokensRefresh()
    {
        for (Token t : getAllLiveTokens()) t.refreshCheck = true;
    }
}
