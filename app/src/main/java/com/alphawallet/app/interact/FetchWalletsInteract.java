package com.alphawallet.app.interact;

import android.text.TextUtils;

import com.alphawallet.app.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.alphawallet.app.entity.Wallet;

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

    public Single<Wallet> getWallet(String keyAddress)
    {
        return accountRepository.findWallet(keyAddress);
    }

    public Single<Wallet> storeWallet(Wallet wallet) {
        return accountRepository.storeWallet(wallet);
    }

    public Single<Wallet> updateWalletData(Wallet wallet) {
        return accountRepository.updateWalletData(wallet);
    }

    /**
     * Called when wallet marked as backed up.
     * Update the wallet backup date
     *
     * @param walletAddr
     */
    public void updateBackupTime(String walletAddr)
    {
        accountRepository.updateBackupTime(walletAddr);
    }

    public Single<Wallet> updateENS(Wallet wallet)
    {
        if (TextUtils.isEmpty(wallet.ENSname)) return Single.fromCallable(() -> wallet);
        return storeWallet(wallet);
    }
}
