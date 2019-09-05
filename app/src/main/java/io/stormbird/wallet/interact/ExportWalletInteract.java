package io.stormbird.wallet.interact;

import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ExportWalletInteract {

    private final WalletRepositoryType walletRepository;

    public ExportWalletInteract(WalletRepositoryType walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Single<String> export(Wallet wallet, String keystorePassword, String backupPassword) {
        return walletRepository
                    .exportWallet(wallet, keystorePassword, backupPassword)
                .observeOn(AndroidSchedulers.mainThread());
    }
}
