package com.alphawallet.app.interact;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenInfo;
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

    public Single<Token[]> addERC721(Wallet wallet, Token[] tokens)
    {
        return tokenRepository.addERC721(wallet, tokens);
    }

    public Disposable updateBlockRead(Token token, Wallet wallet)
    {
        return tokenRepository.updateBlockRead(token, wallet);
    }

    public Single<BigInteger> getLatestBlockNumber(int chainId)
    {
        return tokenRepository.fetchLatestBlockNumber(chainId);
    }

    public Single<Token[]> addERC20(Wallet wallet, Token[] tokens)
    {
        return tokenRepository.addERC20(wallet, tokens);
    }
}
