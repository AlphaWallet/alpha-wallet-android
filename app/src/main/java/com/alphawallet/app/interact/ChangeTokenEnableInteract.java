package com.alphawallet.app.interact;

import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Wallet;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ChangeTokenEnableInteract {
    private final TokenRepositoryType tokenRepository;

    public ChangeTokenEnableInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Completable setEnable(Wallet wallet, Token token) {
        return tokenRepository.setEnable(wallet, token, !token.tokenInfo.isEnabled)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
