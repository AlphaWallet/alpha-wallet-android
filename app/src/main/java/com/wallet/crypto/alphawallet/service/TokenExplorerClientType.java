package com.wallet.crypto.alphawallet.service;

import com.wallet.crypto.alphawallet.entity.TokenInfo;

import io.reactivex.Observable;

public interface TokenExplorerClientType {
    Observable<TokenInfo[]> fetch(String walletAddress);
}