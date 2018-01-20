package com.wallet.crypto.trustapp.interact;

/**
 * Created by James on 16/01/2018.
 */

import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.entity.TokenInfo;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.TokenRepositoryType;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SetupTokensInteract {

    private final TokenRepositoryType tokenRepository;

    public SetupTokensInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Observable<TokenInfo> update(String address) {
        return tokenRepository.update(address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
