package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Token;

import io.reactivex.Single;

public interface TokenRepositoryType {

    Single<Token[]> fetch(String walletAddress);
}
