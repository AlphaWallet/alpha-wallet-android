package com.alphawallet.app.interact;

import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.entity.Wallet;

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
