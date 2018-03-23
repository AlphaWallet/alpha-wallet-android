package io.awallet.crypto.alphawallet.service;

import io.awallet.crypto.alphawallet.entity.TokenInfo;

import io.reactivex.Observable;

public interface TokenExplorerClientType {
    Observable<TokenInfo[]> fetch(String walletAddress);
}