package com.alphawallet.app.repository;

import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SubscribeWrapper;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.entity.TransferFromEventResponse;
import com.alphawallet.app.entity.Wallet;

import io.reactivex.disposables.Disposable;

import java.math.BigInteger;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface TokenRepositoryType {

    Observable<Token[]> fetchStored(String walletAddress);
    Observable<Token[]> fetchActiveStoredPlusEth(String walletAddress);
    Observable<Token> fetchActiveSingle(String walletAddress, Token token);
    Observable<Token> fetchCachedSingleToken(int chainId, String walletAddress, String tokenAddress);
    Observable<Token> fetchActiveTokenBalance(String walletAddress, Token token);
    Single<ContractLocator> getTokenResponse(String address, int chainId, String method);
    Completable setEnable(Wallet wallet, Token token, boolean isEnabled);
    Observable<TokenInfo> update(String address, int chainId);
    Single<TokenInfo[]> update(String[] address, NetworkInfo network);
    Disposable memPoolListener(int chainId, SubscribeWrapper wrapper); //only listen to transactions relating to this address
    Observable<TransferFromEventResponse> burnListenerObservable(String contractAddress);
    Single<Token> addToken(Wallet wallet, TokenInfo tokenInfo, ContractType interfaceSpec);
    Single<TokenTicker> getEthTicker(int chainId);
    Single<TokenTicker> getTokenTicker(Token token);
    Single<Token> getEthBalance(NetworkInfo network, Wallet wallet);
    Single<BigInteger> fetchLatestBlockNumber(int chainId);
    Token fetchToken(int chainId, String walletAddress, String address);

    Disposable terminateToken(Token token, Wallet wallet, NetworkInfo network);

    Single<Token[]> storeTokens(Wallet wallet, Token[] tokens);
    Single<String> resolveENS(int chainId, String address);

    Disposable updateBlockRead(Token token, Wallet wallet);
    Single<String> resolveProxyAddress(TokenInfo tokenInfo);
    Single<ContractType> determineCommonType(TokenInfo tokenInfo);
    Single<Token[]> addERC20(Wallet wallet, Token[] tokens);

    Token updateTokenType(Token token, Wallet wallet, ContractType type);
    Single<Token[]> storeTickers(Wallet wallet, Token[] tokens);

    Single<Boolean> fetchIsRedeemed(Token token, BigInteger tokenId);
}
