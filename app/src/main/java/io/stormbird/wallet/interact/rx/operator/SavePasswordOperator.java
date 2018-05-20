package io.stormbird.wallet.interact.rx.operator;

import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.reactivex.SingleTransformer;

import static io.stormbird.wallet.interact.rx.operator.Operators.completableErrorProxy;

public class SavePasswordOperator implements SingleTransformer<Wallet, Wallet> {

    private final PasswordStore passwordStore;
    private final String password;
    private final WalletRepositoryType walletRepository;

    SavePasswordOperator(
            PasswordStore passwordStore, WalletRepositoryType walletRepository, String password) {
        this.passwordStore = passwordStore;
        this.password = password;
        this.walletRepository = walletRepository;
    }

    @Override
    public Single<Wallet> apply(Single<Wallet> upstream) {
        return upstream.flatMap(wallet ->
                passwordStore
                .setPassword(wallet, password)
                .onErrorResumeNext(err -> walletRepository
                        .deleteWallet(wallet.address, password)
                        .lift(completableErrorProxy(err)))
                .toSingle(() -> wallet));
    }
}
