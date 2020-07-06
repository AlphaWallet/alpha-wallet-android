package com.alphawallet.app.interact;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.TokenRepositoryType;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

import java.math.BigInteger;

public class AddTokenInteract {
    private final TokenRepositoryType tokenRepository;

    public AddTokenInteract(
            TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Observable<Token> add(TokenInfo tokenInfo, ContractType type, Wallet wallet) {
        return tokenRepository
                        .addToken(wallet, tokenInfo, type).toObservable();
    }
}
