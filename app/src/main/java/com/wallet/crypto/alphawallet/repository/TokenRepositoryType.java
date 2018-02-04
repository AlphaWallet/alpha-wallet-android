package com.wallet.crypto.alphawallet.repository;

import com.wallet.crypto.alphawallet.entity.SubscribeWrapper;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TokenInfo;
import com.wallet.crypto.alphawallet.entity.Wallet;

import io.reactivex.Completable;
import io.reactivex.Observable;

public interface TokenRepositoryType {

    Observable<Token[]> fetchActive(String walletAddress);

    Observable<Token[]> fetchAll(String walletAddress);

    //Completable addToken(Wallet wallet, String address, String symbol, int decimals);

    Completable setEnable(Wallet wallet, Token token, boolean isEnabled);
    //Observable<Token[]> fetch(String walletAddress);
    Observable<TokenInfo> update(String address);
    void memPoolListener(SubscribeWrapper wrapper); //only listen to transactions relating to this address
    Completable addToken(Wallet wallet, TokenInfo tokenInfo);
}
