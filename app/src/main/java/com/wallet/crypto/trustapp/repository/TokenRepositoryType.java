package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.entity.TokenInfo;
import com.wallet.crypto.trustapp.entity.Wallet;

import io.reactivex.Completable;
import io.reactivex.Observable;

public interface TokenRepositoryType {

    Observable<Token[]> fetch(String walletAddress);
    Observable<TokenInfo> update(String address);

    Completable addToken(Wallet wallet, TokenInfo tokenInfo);
}
