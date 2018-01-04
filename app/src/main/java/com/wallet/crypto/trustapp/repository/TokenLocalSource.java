package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.TokenInfo;
import com.wallet.crypto.trustapp.entity.Wallet;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface TokenLocalSource {
    Completable put(Wallet wallet, TokenInfo tokenInfo);
    Single<TokenInfo[]> fetch(Wallet wallet);
}
