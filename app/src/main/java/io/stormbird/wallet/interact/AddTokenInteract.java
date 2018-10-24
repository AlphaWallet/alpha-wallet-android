package io.stormbird.wallet.interact;

import io.reactivex.Single;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class AddTokenInteract {
    private final TokenRepositoryType tokenRepository;
    private final WalletRepositoryType walletRepository;

    public AddTokenInteract(
            WalletRepositoryType walletRepository, TokenRepositoryType tokenRepository) {
        this.walletRepository = walletRepository;
        this.tokenRepository = tokenRepository;
    }

    public Observable<Token> add(TokenInfo tokenInfo) {
        return walletRepository
                .getDefaultWallet()
                .flatMap(wallet -> tokenRepository
                        .addToken(wallet, tokenInfo))
                .toObservable();
    }

    /**
     * Add Token to respository process which is a single not an observable
     * @param tokenInfo
     * @return
     */
    public Single<Token> addS(TokenInfo tokenInfo) {
        return walletRepository
                    .getDefaultWallet()
                    .flatMap(wallet -> tokenRepository
                            .addToken(wallet, tokenInfo));
    }

    public Observable<Token> add(TokenInfo tokenInfo, Wallet wallet) {
        return tokenRepository
                        .addToken(wallet, tokenInfo).toObservable();
    }

    public Observable<Token[]> add(TokenInfo[] tokenInfos) {
        return walletRepository.getDefaultWallet()
                .flatMap(wallet -> tokenRepository.addTokens(wallet, tokenInfos))
                .toObservable();

//        return walletRepository
//                .getDefaultWallet()
//                .map(wallet -> tokenRepository
//                        .addTokens(wallet, tokenInfos))
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Token[]> addERC721(Wallet wallet, Token[] tokens)
    {
        return tokenRepository.addERC721(wallet, tokens);
    }
}
