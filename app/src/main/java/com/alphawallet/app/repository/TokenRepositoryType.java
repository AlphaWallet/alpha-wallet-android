package com.alphawallet.app.repository;

import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SubscribeWrapper;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.entity.TransferFromEventResponse;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.service.AssetDefinitionService;

import io.reactivex.disposables.Disposable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.realm.Realm;

public interface TokenRepositoryType {

    Observable<Token> fetchActiveTokenBalance(String walletAddress, Token token);
    Single<Boolean> updateTokenBalance(String walletAddress, int chainId, String tokenAddress, ContractType type);
    Single<ContractLocator> getTokenResponse(String address, int chainId, String method);
    Single<Token[]> checkInterface(Token[] tokens, Wallet wallet);
    Completable setEnable(Wallet wallet, Token token, boolean isEnabled);
    Single<TokenInfo> update(String address, int chainId);
    Disposable memPoolListener(int chainId, SubscribeWrapper wrapper); //only listen to transactions relating to this address
    Observable<TransferFromEventResponse> burnListenerObservable(String contractAddress);
    Single<Token> addToken(Wallet wallet, TokenInfo tokenInfo, ContractType interfaceSpec);
    Single<TokenTicker> getEthTicker(int chainId);
    TokenTicker getTokenTicker(Token token);
    Single<BigInteger> fetchLatestBlockNumber(int chainId);
    Token fetchToken(int chainId, String walletAddress, String address);

    Single<Token[]> storeTokens(Wallet wallet, Token[] tokens);
    Single<String> resolveENS(int chainId, String address);

    Single<ContractType> determineCommonType(TokenInfo tokenInfo);
    Single<Token[]> addERC20(Wallet wallet, Token[] tokens);

    Token updateTokenType(Token token, Wallet wallet, ContractType type);

    Single<Boolean> fetchIsRedeemed(Token token, BigInteger tokenId);

    Disposable addImageUrl(int networkId, String address, String imageUrl);

    Single<TokenCardMeta[]> fetchTokenMetas(Wallet wallet, List<Integer> networkFilters,
                                            AssetDefinitionService svs);

    Single<TokenCardMeta[]> fetchAllTokenMetas(Wallet wallet, List<Integer> networkFilters,
                                            AssetDefinitionService svs);

    Realm getRealmInstance(Wallet wallet);
    Realm getTickerRealmInstance();

    Single<BigDecimal> fetchChainBalance(String walletAddress, int chainId);
}
