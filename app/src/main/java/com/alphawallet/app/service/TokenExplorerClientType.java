package com.alphawallet.app.service;

import com.alphawallet.app.entity.tokens.TokenInfo;

import io.reactivex.Observable;

public interface TokenExplorerClientType {
    Observable<TokenInfo[]> fetch(String walletAddress);
}