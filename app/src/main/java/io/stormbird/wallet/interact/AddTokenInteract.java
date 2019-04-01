package io.stormbird.wallet.interact;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.service.AssetDefinitionService;

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

    public Observable<Token> addTokenFunctionData(Token token, AssetDefinitionService service)
    {
        if (token.hasPositiveBalance()) return tokenRepository.callTokenFunctions(token, service).toObservable();
        else return Observable.fromCallable(() -> token);
    }

    public Disposable updateBlockRead(Token token, Wallet wallet)
    {
        return tokenRepository.updateBlockRead(token, wallet);
    }

    public Single<BigInteger> getLatestBlockNumber(int chainId)
    {
        return tokenRepository.fetchLatestBlockNumber(chainId);
    }
}
