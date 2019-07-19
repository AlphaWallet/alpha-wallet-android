package io.stormbird.wallet.interact;

import android.arch.lifecycle.MutableLiveData;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.rx.operator.Operators;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ImportWalletInteract {

    private final WalletRepositoryType walletRepository;
    private final PasswordStore passwordStore;

    public ImportWalletInteract(WalletRepositoryType walletRepository, PasswordStore passwordStore) {
        this.walletRepository = walletRepository;
        this.passwordStore = passwordStore;
    }

    public Single<Wallet> importKeystore(String keystore, String password) {
        return passwordStore
                .generatePassword()
                .flatMap(newPassword -> walletRepository
                        .importKeystoreToWallet(keystore, password, newPassword)
                        .compose(Operators.savePassword(passwordStore, walletRepository, newPassword)))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Wallet> importPrivateKey(String privateKey) {
        return passwordStore
                .generatePassword()
                .flatMap(newPassword -> walletRepository
                        .importPrivateKeyToWallet(privateKey, newPassword)
                        .compose(Operators.savePassword(passwordStore, walletRepository, newPassword)))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Wallet> storeHDWallet(String walletAddress)
    {
        Wallet wallet = new Wallet(walletAddress);
        wallet.type = Wallet.WalletType.HDKEY;
        wallet.lastBackupTime = System.currentTimeMillis();
        return walletRepository.storeWallet(wallet);
    }
}
