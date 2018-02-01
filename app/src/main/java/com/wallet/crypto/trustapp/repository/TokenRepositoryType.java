package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.SubscribeWrapper;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.entity.TokenInfo;
import com.wallet.crypto.trustapp.entity.Wallet;

import io.reactivex.Completable;
import io.reactivex.Observable;
import rx.Subscription;
import rx.functions.Action1;

public interface TokenRepositoryType {

    Observable<Token[]> fetch(String walletAddress);
    Observable<TokenInfo> update(String address);
    void memPoolListener(SubscribeWrapper wrapper); //only listen to transactions relating to this address
    Completable addToken(Wallet wallet, TokenInfo tokenInfo);
}
