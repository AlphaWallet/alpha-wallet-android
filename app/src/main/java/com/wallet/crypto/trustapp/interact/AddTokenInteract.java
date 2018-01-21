package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.TokenInfo;
import com.wallet.crypto.trustapp.repository.TokenRepositoryType;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class AddTokenInteract {
    private final TokenRepositoryType tokenRepository;
    private final WalletRepositoryType walletRepository;

    public AddTokenInteract(
            WalletRepositoryType walletRepository, TokenRepositoryType tokenRepository) {
        this.walletRepository = walletRepository;
        this.tokenRepository = tokenRepository;
    }

    public Completable add(TokenInfo tokenInfo) {
        return walletRepository
                .getDefaultWallet()
                .flatMapCompletable(wallet -> tokenRepository
                        .addToken(wallet, tokenInfo)
                        .observeOn(AndroidSchedulers.mainThread()));
    }
}
