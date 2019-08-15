package io.stormbird.wallet.interact;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.WalletType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.util.AWEnsResolver;

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

    public Single<Wallet> storeHDWallet(String walletAddress, KeyService.AuthenticationLevel authLevel, AWEnsResolver ensResolver)
    {
        Wallet wallet = new Wallet(walletAddress);
        wallet.type = WalletType.HDKEY;
        wallet.authLevel = authLevel;
        wallet.lastBackupTime = System.currentTimeMillis();
        return ensResolver.resolveWalletEns(wallet).subscribeOn(Schedulers.io())
               .flatMap(walletRepository::storeWallet);
    }

    public Single<Wallet> storeWatchWallet(String address, AWEnsResolver ensResolver)
    {
        Wallet wallet = new Wallet(address);
        wallet.type = WalletType.WATCH;
        wallet.lastBackupTime = System.currentTimeMillis();
        return ensResolver.resolveWalletEns(wallet).subscribeOn(Schedulers.io())
                .flatMap(walletRepository::storeWallet);
    }

    public Single<Wallet> storeKeystoreWallet(Wallet wallet, KeyService.AuthenticationLevel level, AWEnsResolver ensResolver)
    {
        wallet.authLevel = level;
        wallet.type = WalletType.KEYSTORE;
        wallet.lastBackupTime = System.currentTimeMillis();
        return ensResolver.resolveWalletEns(wallet).subscribeOn(Schedulers.io())
                .flatMap(walletRepository::storeWallet);
    }

    public boolean keyStoreExists(String address)
    {
        return walletRepository.keystoreExists(address);
    }
}
