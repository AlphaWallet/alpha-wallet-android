package com.alphawallet.app.interact;

import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.util.BalanceUtils;

import io.reactivex.disposables.Disposable;
import com.alphawallet.app.entity.Wallet;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import java.math.BigDecimal;

import static com.alphawallet.app.C.ETHER_DECIMALS;
import static com.alphawallet.app.entity.tokens.Token.TOKEN_BALANCE_PRECISION;

public class GenericWalletInteract
{
	private final WalletRepositoryType walletRepository;

	public GenericWalletInteract(WalletRepositoryType walletRepository) {
		this.walletRepository = walletRepository;
	}

	public Single<Wallet> find() {
		return walletRepository.getDefaultWallet()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread());
	}

	/**
	 * Called when wallet marked as backed up.
	 * Update the wallet date
	 *
	 * @param walletAddr
	 */
	public void updateBackupTime(String walletAddr)
	{
		walletRepository.updateBackupTime(walletAddr);
	}

	/**
	 * Called when wallet has had its backup warning dismissed
	 * Update the wallet date
	 *
	 * @param walletAddr
	 */
	public void updateWarningTime(String walletAddr)
	{
		walletRepository.updateWarningTime(walletAddr);
	}

	public Single<String> getWalletNeedsBackup(String walletAddr)
	{
		return walletRepository.getWalletRequiresBackup(walletAddr);
	}

	public void setIsDismissed(String walletAddr, boolean isDismissed)
	{
		walletRepository.setIsDismissed(walletAddr, isDismissed);
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

	private boolean hasBalance(Wallet wallet)
	{
		String balance = wallet.balance;
		if (balance == null || balance.length() == 0 || !BalanceUtils.isDecimalValue(balance)) return false;
		BigDecimal b = new BigDecimal(balance);
		return b.compareTo(BigDecimal.ZERO) > 0;
	}

	public Single<Wallet> updateBalanceIfRequired(Wallet wallet, BigDecimal newBalance)
	{
		String newBalanceStr = BalanceUtils.getScaledValueFixed(newBalance, ETHER_DECIMALS, TOKEN_BALANCE_PRECISION);
		if (!newBalance.equals(BigDecimal.valueOf(-1)) && !wallet.balance.equals(newBalanceStr))
		{
			wallet.balance = newBalanceStr;
			return walletRepository.updateWalletData(wallet);
		}
		else
		{
			return Single.fromCallable(() -> wallet);
		}
	}

	public Realm getWalletRealm()
	{
		return walletRepository.getWalletRealm();
	}

    public enum BackupLevel
	{
		BACKUP_NOT_REQUIRED, WALLET_HAS_LOW_VALUE, WALLET_HAS_HIGH_VALUE
	}
}
