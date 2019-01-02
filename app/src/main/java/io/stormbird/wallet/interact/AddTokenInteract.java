package io.stormbird.wallet.interact;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.stormbird.wallet.entity.ContractType;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.service.AssetDefinitionService;

public class AddTokenInteract {
    private final TokenRepositoryType tokenRepository;
    private final WalletRepositoryType walletRepository;

    public AddTokenInteract(
            WalletRepositoryType walletRepository, TokenRepositoryType tokenRepository) {
        this.walletRepository = walletRepository;
        this.tokenRepository = tokenRepository;
    }

    public Observable<Token> add(TokenInfo tokenInfo, ContractType type) {
        return walletRepository
                .getDefaultWallet()
                .flatMap(wallet -> tokenRepository
                        .addToken(wallet, tokenInfo, type))
                .toObservable();
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
}
