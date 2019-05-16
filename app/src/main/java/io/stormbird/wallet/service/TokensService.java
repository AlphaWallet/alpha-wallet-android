package io.stormbird.wallet.service;

import android.util.Log;
import android.util.SparseArray;
import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.realm.Realm;
import io.realm.RealmResults;
import io.stormbird.token.entity.TransactionResult;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TransactionsRealmCache;
import io.stormbird.wallet.repository.entity.RealmAuxData;

import java.math.BigInteger;
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
    private String currentAddress = null;
    private boolean loaded;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final RealmManager realmManager;
    private final List<Integer> networkFilter;
    private final ConcurrentLinkedQueue<Token> transactionUpdateQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Token> balanceUpdateQueue = new ConcurrentLinkedQueue<>();
    private Token focusToken;

    public TokensService(EthereumNetworkRepositoryType ethereumNetworkRepository, RealmManager realmManager) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.realmManager = realmManager;
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

    public void addTokens(Token[] tokens)
    {
        int i = 0;
        for (Token t : tokens)
        {
            i++;
            t.setRequireAuxRefresh();
            addToken(t);

            Token test = getToken(5, currentAddress);
            if (test != null && test.getInterfaceSpec() != ContractType.ETHEREUM)
            {
                System.out.println("yoless");
            }
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

        //after a filter change, ensure the base currency is checked and displayed first
        List<Token> baseCurrencies = getAllAtAddress(currentAddress);
        for (Token base : baseCurrencies) base.balanceUpdatePressure += 20.0f;
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

    public void updateTokenPressure(boolean isVisible)
    {
        //get all tokens
        List<Token> allTokens = getAllLiveTokens();
        Token highestPressureToken = null;
        float highestPressure = 0.0f;

        for (Token t : allTokens)
        {
            if (t == null) continue;
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
            if (t.isEthereum()) t.balanceUpdatePressure += 20.0f;
        }
    }

    public Disposable loadAuxData()
    {
        return Completable.complete()
                .subscribeWith(new DisposableCompletableObserver()
                {
                    Realm realm;

                    @Override
                    public void onStart()
                    {
                        realm = realmManager.getAuxRealmInstance(currentAddress);

                        RealmResults<RealmAuxData> realmTokens = realm.where(RealmAuxData.class)
                                .findAll();

                        for (RealmAuxData aux : realmTokens)
                        {
                            Token t = getToken(aux.getChainId(), aux.getAddress());
                            //now add the Aux data
                            if (!t.addAuxData(aux.getTokenId(), aux.getFunctionId(), aux.getResult(), aux.getResultTime()))
                            {
                                if (!realm.isInTransaction()) realm.beginTransaction();
                                aux.deleteFromRealm();
                            }
                        }
                    }

                    @Override
                    public void onComplete()
                    {
                        if (realm != null)
                        {
                            if (realm.isInTransaction())
                            {
                                realm.commitTransaction();
                            }

                            if (!realm.isClosed()) realm.close();
                        }
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        if (realm != null && !realm.isClosed()) realm.close();
                    }
                });
    }

    Disposable storeAuxData(TransactionResult tResult)
    {
        return Completable.complete()
                .subscribeWith(new DisposableCompletableObserver()
                {
                    Realm realm;

                    @Override
                    public void onStart()
                    {
                        if (tResult.result == null) return;
                        //tResult.token.addAuxData(tResult.tokenId, tResult.method, tResult.result, tResult.resultTime);
                        realm = realmManager.getAuxRealmInstance(currentAddress);
                        RealmAuxData realmToken = realm.where(RealmAuxData.class)
                                .equalTo("instanceKey", functionKey(tResult.contractChainId, tResult.contractAddress, tResult.tokenId, tResult.method))
                                .equalTo("chainId", tResult.contractChainId)
                                .findFirst();

                        TransactionsRealmCache.addRealm();
                        realm.beginTransaction();
                        if (realmToken == null)
                        {
                            createAuxData(realm, tResult);
                        }
                        else
                        {
                            realmToken.setResult(tResult.result);
                            realmToken.setResultTime(tResult.resultTime);
                        }
                    }

                    @Override
                    public void onComplete()
                    {
                        if (realm != null)
                        {
                            if (realm.isInTransaction())
                                realm.commitTransaction();
                            TransactionsRealmCache.subRealm();
                            if (!realm.isClosed()) realm.close();
                        }
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        if (realm != null && !realm.isClosed())
                        {
                            TransactionsRealmCache.subRealm();
                            realm.close();
                        }
                    }
                });
    }

    private void createAuxData(Realm realm, TransactionResult tResult)
    {
        //Log.d("TokensService", "Save Aux: " + tResult.token.getFullName() + " :" + tResult.token.tokenInfo.address + " :" + tResult.tokenId.toString());

        RealmAuxData realmData = realm.createObject(RealmAuxData.class, functionKey(tResult.contractChainId, tResult.contractAddress, tResult.tokenId, tResult.method));
        realmData.setResultTime(tResult.resultTime);
        realmData.setResult(tResult.result);
        realmData.setChainId(tResult.contractChainId);
        realmData.setFunctionId(tResult.method);
        realmData.setTokenId(tResult.tokenId.toString(Character.MAX_RADIX));
    }

    private String functionKey(int chainId, String address, BigInteger tokenId, String method)
    {
        //produce a unique key for this. token address, token Id, chainId
        return address + "-" + tokenId.toString(Character.MAX_RADIX) + "-" + chainId + "-" + method;
    }
}
