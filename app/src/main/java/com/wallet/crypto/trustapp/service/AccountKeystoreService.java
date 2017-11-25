package com.wallet.crypto.trustapp.service;

import com.wallet.crypto.trustapp.entity.Account;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface AccountKeystoreService {
	/**
	 * Create account in keystore
	 * @param password account password
	 * @return new {@link Account}
	 */
	Single<Account> createAccount(String password);

	/**
	 * Include new existing keystore
	 * @param store store to include
	 * @param password store password
	 * @return included {@link Account} if success
	 */
	Single<Account> importStore(String store, String password);

	/**
	 * Export account to keystore
	 * @param account account to export
	 * @param password password from account
	 * @param newPassword new password to store
	 * @return store data
	 */
	Single<String> exportAccount(Account account, String password, String newPassword);

	/**
	 * Delete account from keystore
	 * @param address account address
	 * @param password account password
	 */
	Completable deleteAccount(String address, String password);

	/**
	 * Sign transaction
	 * @param signer {@link Account}
	 * @param signerPassword password from {@link Account}
	 * @param toAddress transaction destination address
	 * @param wei
	 * @param nonce
	 * @return sign data
	 */
	Single<byte[]> signTransaction(
			Account signer,
			String signerPassword,
			String toAddress,
			String wei,
			long nonce,
			long chainId);

	/**
	 * Check if there is an address in the keystore
	 * @param address {@link Account} address
	 */
	boolean hasAccount(String address);

	/**
	 * Return all {@link Account} from keystore
	 * @return accounts
	 */
	Single<Account[]> fetchAccounts();
}
