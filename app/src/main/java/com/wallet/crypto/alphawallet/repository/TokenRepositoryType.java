package com.wallet.crypto.alphawallet.repository;

import com.wallet.crypto.alphawallet.entity.SubscribeWrapper;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TokenInfo;
import com.wallet.crypto.alphawallet.entity.Wallet;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;

public interface TokenRepositoryType {

    Observable<Token[]> fetchActive(String walletAddress);
    Observable<Token[]> fetchActiveCache(String walletAddress);
    Observable<Token[]> fetchActiveStored(String walletAddress);
    Observable<Token[]> fetchActiveStoredPlusEth(String walletAddress);
    Observable<Token> fetchActiveSingle(String walletAddress, Token token);
    Observable<Token> fetchActiveTokenBalance(String walletAddress, Token token);
    Observable<Token[]> fetchAll(String walletAddress);
    Completable setEnable(Wallet wallet, Token token, boolean isEnabled);
    Observable<TokenInfo> update(String address);
    void memPoolListener(SubscribeWrapper wrapper); //only listen to transactions relating to this address
    Completable addToken(Wallet wallet, TokenInfo tokenInfo);
    Completable setBurnList(Wallet wallet, Token token, List<Integer> burnList);
}
