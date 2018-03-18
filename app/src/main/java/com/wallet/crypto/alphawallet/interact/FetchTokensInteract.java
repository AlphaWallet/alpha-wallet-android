package com.wallet.crypto.alphawallet.interact;

import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TokenInfo;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FetchTokensInteract {

    private final TokenRepositoryType tokenRepository;

    public FetchTokensInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Observable<Token[]> fetch(Wallet wallet) {
        return tokenRepository.fetchActive(wallet.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token[]> fetchCache(Wallet wallet) {
        return tokenRepository.fetchActiveCache(wallet.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token[]> fetchStored(Wallet wallet) {
        return tokenRepository.fetchActiveStored(wallet.address)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }

    public Observable<Token> fetchSingle(Wallet wallet, Token token) {
        return tokenRepository.fetchActiveSingle(wallet.address, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable updateBalance(Wallet wallet, Token token, List<Integer> burnList) {
        return tokenRepository
                        .setBurnList(wallet, token, burnList)
                        .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token> updateBalance(Wallet wallet, Token token)
    {
        return tokenRepository.fetchActiveTokenBalance(wallet.address, token)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }
}
