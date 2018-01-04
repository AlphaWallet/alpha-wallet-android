package com.wallet.crypto.trustapp.service;

import com.wallet.crypto.trustapp.entity.TokenInfo;

import io.reactivex.Observable;

public interface TokenExplorerClientType {
    Observable<TokenInfo[]> fetch(String walletAddress);
}