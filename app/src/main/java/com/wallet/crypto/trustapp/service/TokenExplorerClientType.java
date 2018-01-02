package com.wallet.crypto.trustapp.service;

import com.wallet.crypto.trustapp.entity.Token;

import io.reactivex.Observable;

public interface TokenExplorerClientType {
    Observable<Token[]> fetch(String walletAddress);
}
