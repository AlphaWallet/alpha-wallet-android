package com.alphawallet.app.interact;

import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.util.ens.AWEnsResolver;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.service.KeyService;

public class ImportWalletInteract {

    private final WalletRepositoryType walletRepository;

    public ImportWalletInteract(WalletRepositoryType walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Single<Wallet> importKeystore(String keystore, String password, String newPassword) {
        return walletRepository
                        .importKeystoreToWallet(keystore, password, newPassword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Wallet> importPrivateKey(String privateKey, String newPassword) {
        return walletRepository
                        .importPrivateKeyToWallet(privateKey, newPassword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Wallet> storeHDWallet(String walletAddress, KeyService.AuthenticationLevel authLevel, AWEnsResolver ensResolver)
    {
        Wallet wallet = new Wallet(walletAddress);
        wallet.type = WalletType.HDKEY;
        wallet.authLevel = authLevel;
        wallet.lastBackupTime = System.currentTimeMillis();
        return walletRepository.storeWallet(wallet);
    }

    public Single<Wallet> storeWatchWallet(String address, AWEnsResolver ensResolver)
    {
        Wallet wallet = new Wallet(address);
        wallet.type = WalletType.WATCH;
        wallet.lastBackupTime = System.currentTimeMillis();
        return walletRepository.storeWallet(wallet);
                /* return ensResolver.resolveEnsName(wallet.address)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .map(name -> { wallet.ENSname = name; return wallet; })
                .flatMap(walletRepository::storeWallet);*/
    }

    public Single<Wallet> storeKeystoreWallet(Wallet wallet, KeyService.AuthenticationLevel level, AWEnsResolver ensResolver)
    {
        wallet.authLevel = level;
        wallet.type = WalletType.KEYSTORE;
        wallet.lastBackupTime = System.currentTimeMillis();
        return walletRepository.storeWallet(wallet);

        /*return ensResolver.resolveEnsName(wallet.address)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .map(name -> { wallet.ENSname = name; return wallet; })
                .flatMap(walletRepository::storeWallet);*/
    }

    public boolean keyStoreExists(String address)
    {
        return walletRepository.keystoreExists(address);
    }
}
