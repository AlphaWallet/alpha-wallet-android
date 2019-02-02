package io.stormbird.wallet.repository;

import io.stormbird.wallet.entity.*;
import org.web3j.protocol.core.methods.response.Transaction;

import io.reactivex.SingleSource;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.stormbird.wallet.service.AssetDefinitionService;
import rx.functions.Action1;

public interface TokenRepositoryType {

    Observable<Token[]> fetchActiveStored(String walletAddress);
    Observable<Token[]> fetchActiveStoredPlusEth(NetworkInfo network, String walletAddress);
    Observable<Token> fetchActiveSingle(String walletAddress, Token token);
    Observable<Token> fetchCachedSingleToken(String walletAddress, String tokenAddress);
    Observable<Token> fetchActiveTokenBalance(String walletAddress, Token token);
    Observable<Token> fetchActiveTokenBalance(Token token, NetworkInfo network, Wallet wallet);
    Observable<Token[]> fetchAll(String walletAddress);
    Single<String>    getTokenName(String address, int chainId);
    Completable setEnable(Wallet wallet, Token token, boolean isEnabled);
    Observable<TokenInfo> update(String address);
    Single<TokenInfo[]> update(String[] address);
    rx.Subscription memPoolListener(SubscribeWrapper wrapper); //only listen to transactions relating to this address
    rx.Observable<TransferFromEventResponse> burnListenerObservable(String contractAddress);
    Single<Token> addToken(Wallet wallet, TokenInfo tokenInfo, ContractType interfaceSpec);
    Single<Token> callTokenFunctions(Token token, AssetDefinitionService service);
    Completable setBurnList(Wallet wallet, Token token, List<Integer> burnList);
    Single<Ticker> getEthTicker();
    Single<Token> getEthBalance(NetworkInfo network, Wallet wallet);
    Single<BigInteger> fetchLatestBlockNumber();

    void terminateToken(Token token, Wallet wallet, NetworkInfo network);

    Single<Token[]> addERC721(Wallet wallet, Token[] tokens);
    Single<String> callAddressMethod(String method, byte[] resultHash, String address);
}
