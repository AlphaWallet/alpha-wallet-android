package io.stormbird.wallet.repository;

import org.web3j.protocol.core.methods.response.Transaction;

import io.reactivex.SingleSource;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.SubscribeWrapper;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.TransferFromEventResponse;
import io.stormbird.wallet.entity.Wallet;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import rx.functions.Action1;

public interface TokenRepositoryType {

    Observable<Token[]> fetchActive(String walletAddress);
    Observable<Token[]> fetchActiveCache(String walletAddress);
    Observable<Token[]> fetchActiveStored(String walletAddress);
    Observable<Token[]> fetchActiveStoredPlusEth(NetworkInfo network, String walletAddress);
    Observable<Token> fetchActiveStoredSequential(String walletAddress);
    Observable<Token> fetchActiveStoredSequentialNoEth(String walletAddress);
    Observable<Token> fetchActiveSingle(String walletAddress, Token token);
    Observable<Token> fetchCachedSingleToken(String walletAddress, String tokenAddress);
    Observable<Token> fetchActiveTokenBalance(String walletAddress, Token token);
    Observable<Token> fetchActiveDefaultTokenBalance(Token token);
    Observable<Token> fetchActiveTokenBalance(Token token, NetworkInfo network, Wallet wallet);
    Observable<Token[]> fetchAll(String walletAddress);
    Completable setEnable(Wallet wallet, Token token, boolean isEnabled);
    Observable<TokenInfo> update(String address);
    Single<TokenInfo[]> update(String[] address);
    rx.Subscription memPoolListener(SubscribeWrapper wrapper); //only listen to transactions relating to this address
    rx.Observable<TransferFromEventResponse> burnListenerObservable(String contractAddress);
    Single<Token> addToken(Wallet wallet, TokenInfo tokenInfo);
    Completable setBurnList(Wallet wallet, Token token, List<Integer> burnList);
    Single<Token[]> addTokens(Wallet wallet, TokenInfo[] tokenInfos);
    Single<Ticker> getEthTicker();
    Single<Token> getEthBalance(NetworkInfo network, Wallet wallet);

    void terminateToken(Token token, Wallet wallet, NetworkInfo network);

    Single<Token[]> addERC721(Wallet wallet, Token[] tokens);
}
