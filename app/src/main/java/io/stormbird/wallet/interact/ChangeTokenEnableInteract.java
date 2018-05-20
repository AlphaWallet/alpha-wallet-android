package io.stormbird.wallet.interact;

import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.TokenRepositoryType;

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
