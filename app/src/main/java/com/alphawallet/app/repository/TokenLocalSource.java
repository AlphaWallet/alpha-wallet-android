package com.alphawallet.app.repository;

import io.reactivex.disposables.Disposable;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.service.AssetDefinitionService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.realm.Realm;

public interface TokenLocalSource {
    Single<Token> saveToken(Wallet wallet, Token token);
    Single<Token[]> saveTokens(Wallet wallet, Token[] items);
    void updateTokenBalance(NetworkInfo network, Wallet wallet, Token token);
    boolean updateTokenBalance(Wallet wallet, int chainId, String tokenAddress, BigDecimal balance, List<BigInteger> balanceArray, ContractType type);
    void markBalanceChecked(Wallet wallet, int chainId, String tokenAddress);
    Token fetchToken(int chainId, Wallet wallet, String address);
    void setEnable(NetworkInfo network, Wallet wallet, Token token, boolean isEnabled);

    Single<Token[]> saveERC20Tokens(Wallet wallet, Token[] tokens);
    void deleteRealmToken(int chainId, Wallet wallet, String address);

    Token updateTokenType(Token token, Wallet wallet, ContractType type);
    Disposable storeTokenUrl(int networkId, String address, String imageUrl);

    Single<TokenCardMeta[]> fetchTokenMetas(Wallet wallet, List<Integer> networkFilters,
                                            AssetDefinitionService svs);

    Single<TokenCardMeta[]> fetchAllTokenMetas(Wallet wallet, List<Integer> networkFilters,
                                             String seachTerm);

    TokenCardMeta[] fetchTokenMetasForUpdate(Wallet wallet, List<Integer> networkFilters);

    Single<Integer> fixFullNames(Wallet wallet, AssetDefinitionService svs);

    Disposable updateEthTickers(Map<Integer, TokenTicker> ethTickers);
    Disposable updateERC20Tickers(Map<String, TokenTicker> erc20Tickers);
    Disposable removeOutdatedTickers();

    Realm getRealmInstance(Wallet wallet);
    Realm getTickerRealmInstance();

    TokenTicker getCurrentTicker(Token token);

    void setVisibilityChanged(Wallet wallet, Token token);

    boolean hasVisibilityBeenChanged(Token token);
    boolean getEnabled(Token token);
}
