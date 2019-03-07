package io.stormbird.wallet.interact;

import java.util.Map;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.WalletUpdate;
import io.stormbird.wallet.repository.WalletRepositoryType;

public class FetchWalletsInteract {

    private final WalletRepositoryType accountRepository;

    public FetchWalletsInteract(WalletRepositoryType accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Single<Wallet[]> fetch() {
        return accountRepository
                .fetchWallets()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> getWalletName(String address) {
        return accountRepository.getName(address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<WalletUpdate> scanForNames(Wallet[] wallets, long lastBlockChecked) {
        return accountRepository.scanForNames(wallets, lastBlockChecked);
    }

    public Single<Integer> storeWallets(Wallet[] wallets, boolean isMainNet) {
        return accountRepository.storeWallets(wallets, isMainNet);
    }

    public Single<Integer> storeWallet(Wallet wallet) {
        return accountRepository.storeWallet(wallet);
    }
}
