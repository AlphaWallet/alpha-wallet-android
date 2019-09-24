package com.alphawallet.app.interact;

import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.util.BalanceUtils;

import io.reactivex.disposables.Disposable;
import com.alphawallet.app.entity.Wallet;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

import java.math.BigDecimal;

public class GenericWalletInteract
{
	private final WalletRepositoryType walletRepository;

	public GenericWalletInteract(WalletRepositoryType walletRepository) {
		this.walletRepository = walletRepository;
	}

	public Single<Wallet> find() {
		return walletRepository
				.getDefaultWallet()
				.onErrorResumeNext(walletRepository
						.fetchWallets()
						.to(single -> Flowable.fromArray(single.blockingGet()))
						.firstOrError()
						.flatMapCompletable(walletRepository::setDefaultWallet)
						.andThen(walletRepository.getDefaultWallet()))
				.observeOn(AndroidSchedulers.mainThread());
	}

	/**
	 * Called when wallet marked as backed up.
	 * Update the wallet date
	 *
	 * @param walletAddr
	 */
	public Disposable updateBackupTime(String walletAddr)
	{
		return walletRepository.updateBackupTime(walletAddr);
	}

	/**
	 * Called when wallet has had its backup warning dismissed
	 * Update the wallet date
	 *
	 * @param walletAddr
	 */
	public Disposable updateWarningTime(String walletAddr)
	{
		return walletRepository.updateWarningTime(walletAddr);
	}

	public Single<String> getWalletNeedsBackup(String walletAddr)
	{
		return walletRepository.getWalletRequiresBackup(walletAddr);
	}

	public Single<String> setIsDismissed(String walletAddr, boolean isDismissed)
	{
		return walletRepository.setIsDismissed(walletAddr, isDismissed);
	}

	/**
	 * Check if a wallet needs to be backed up.
	 * @param walletAddr
	 * @return
	 */
	public Single<Boolean> getBackupWarning(String walletAddr)
	{
		return walletRepository.getWalletBackupWarning(walletAddr);
	}

    public Single<Wallet> getWallet(String keyAddress)
    {
    	return walletRepository.findWallet(keyAddress);
    }

	private boolean hasBalance(Wallet wallet)
	{
		String balance = wallet.balance;
		if (balance == null || balance.length() == 0 || !BalanceUtils.isDecimalValue(balance)) return false;
		BigDecimal b = new BigDecimal(balance);
		return b.compareTo(BigDecimal.ZERO) > 0;
	}

	public enum BackupLevel
	{
		BACKUP_NOT_REQUIRED, WALLET_HAS_LOW_VALUE, WALLET_HAS_HIGH_VALUE
	}
}
