package com.alphawallet.app.service;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.token.entity.Signable;

import java.math.BigInteger;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface AccountKeystoreService {
	/**
	 * Create account in keystore
	 * @param password account password
	 * @return new {@link Wallet}
	 */
	Single<Wallet> createAccount(String password);

	/**
	 * Include new existing keystore
	 * @param store store to include
	 * @param password store password
	 * @return included {@link Wallet} if success
	 */
	Single<Wallet> importKeystore(String store, String password, String newPassword);
    Single<Wallet> importPrivateKey(String privateKey, String newPassword);

	/**
	 * Export wallet to keystore
	 * @param wallet wallet to export
	 * @param password password from wallet
	 * @param newPassword new password to store
	 * @return store data
	 */
	Single<String> exportAccount(Wallet wallet, String password, String newPassword);

	/**
	 * Delete account from keystore
	 * @param address account address
	 * @param password account password
	 */
	Completable deleteAccount(String address, String password);

	/**
	 * Sign transaction
	 * @param signer {@link Wallet}
	 * @param toAddress transaction destination address
	 * @param nonce
	 * @return sign data
	 */
	Single<SignatureFromKey> signTransaction(
			Wallet signer,
			String toAddress,
			BigInteger amount,
			BigInteger gasPrice,
			BigInteger gasLimit,
			long nonce,
			byte[] data,
			long chainId);

	Single<SignatureFromKey> signMessage(
			Wallet signer,
			Signable message,
			long chainId);

	Single<SignatureFromKey> signTransaction(
			Wallet signer,
			byte[] rawTx,
			long chainId);

	Single<byte[]> signTransactionFast(
			Wallet signer,
			String password,
			byte[] message,
			long chainId);

	/**
	 * Check if there is an address in the keystore
	 * @param address {@link Wallet} address
	 */
	boolean hasAccount(String address);

	/**
	 * Return all {@link Wallet} from keystore
	 * @return wallets
	 */
	Single<Wallet[]> fetchAccounts();
}
