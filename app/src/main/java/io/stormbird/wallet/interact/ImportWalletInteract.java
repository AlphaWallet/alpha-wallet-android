package io.stormbird.wallet.interact;

import io.reactivex.Completable;
import io.reactivex.SingleSource;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.WalletType;
import io.stormbird.wallet.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.stormbird.wallet.service.HDKeyService;

public class ImportWalletInteract {

    private final WalletRepositoryType walletRepository;

    public ImportWalletInteract(WalletRepositoryType walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Single<Wallet> importKeystore(String keystore, String password, String newPassword) {
        return walletRepository
                        .importKeystoreToWallet(keystore, password, newPassword)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Wallet> importPrivateKey(String privateKey, String newPassword) {
        return walletRepository
                        .importPrivateKeyToWallet(privateKey, newPassword)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Wallet> storeHDWallet(String walletAddress, HDKeyService.AuthenticationLevel authLevel)
    {
        Wallet wallet = new Wallet(walletAddress);
        wallet.type = WalletType.HDKEY;
        wallet.authLevel = authLevel;
        wallet.lastBackupTime = System.currentTimeMillis();
        return walletRepository.storeWallet(wallet);
    }

    public Single<Wallet> storeWatchWallet(String address)
    {
        Wallet wallet = new Wallet(address);
        wallet.type = WalletType.WATCH;
        wallet.lastBackupTime = System.currentTimeMillis();
        return walletRepository.storeWallet(wallet);
    }

    public Single<Wallet> storeKeystoreWallet(Wallet wallet, HDKeyService.AuthenticationLevel level)
    {
        wallet.authLevel = level;
        wallet.type = WalletType.KEYSTORE;
        wallet.lastBackupTime = System.currentTimeMillis();
        return walletRepository.storeWallet(wallet);
    }
}
