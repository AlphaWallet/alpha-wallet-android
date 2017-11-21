package com.wallet.crypto.trustapp.service;

import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.entity.ServiceException;

import org.ethereum.geth.Accounts;
import org.ethereum.geth.Address;
import org.ethereum.geth.BigInt;
import org.ethereum.geth.Geth;
import org.ethereum.geth.KeyStore;
import org.ethereum.geth.Transaction;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class GethStoreAccountService implements AccountKeystoreService {

	private final KeyStore keyStore;

	public GethStoreAccountService(File keyStoreFile) {
		// TODO: filesDir + "/keystore"
		keyStore = new KeyStore(keyStoreFile.getAbsolutePath(), Geth.LightScryptN, Geth.LightScryptP);
	}

	@Override
	public Single<Account> createAccount(String password) {
		return Single.fromCallable(() -> new Account(
				keyStore.newAccount(password).getAddress().getHex().toLowerCase()))
			.subscribeOn(Schedulers.io());
	}

	@Override
	public Single<Account> importStore(String store, String password) {
		return Single.fromCallable(() -> {
			org.ethereum.geth.Account account = keyStore.importKey(store.getBytes(Charset.forName("UTF-8")), password, password);
			return new Account(account.getAddress().getHex().toLowerCase());
		})
		.subscribeOn(Schedulers.io());
	}

	@Override
	public Single<String> exportAccount(Account account, String password, String newPassword) {
		return Single
				.fromCallable(() -> findAccount(account.address))
				.flatMap(account1 -> Single.fromCallable(()
						-> new String(keyStore.exportKey(account1, password, newPassword))))
				.subscribeOn(Schedulers.io());
	}

	@Override
	public Completable deleteAccount(String address, String password) {
		return Single.fromCallable(() -> findAccount(address))
				.flatMapCompletable(account -> Completable.fromAction(
						() -> keyStore.deleteAccount(account, password)))
				.subscribeOn(Schedulers.io());
	}

	@Override
	public Single<byte[]> signTransaction(Account signer, String signerPassword, String toAddress, String wei, long nonce) {
		return Single.fromCallable(new Callable<byte[]>() {
			@Override
			public byte[] call() throws Exception {
				BigInt value = new BigInt(Long.decode(wei));
				BigInt gasPrice = new BigInt(0);
				gasPrice.setString("15000000000", 10); // price, base
				Transaction tx = new Transaction(
						nonce,
						new Address(toAddress),
						value,
						new BigInt(90000), // gas limit
						gasPrice,
						null); // data

				BigInt chain = new BigInt(Controller.get().getCurrentNetwork().getChainId()); // Chain identifier of the main net
				org.ethereum.geth.Account gethAccount = findAccount(signer.address);
				keyStore.unlock(gethAccount, signerPassword);
				Transaction signed = keyStore.signTx(gethAccount, tx, chain);
				keyStore.lock(gethAccount.getAddress());

				return signed.encodeRLP();
			}
		})
		.subscribeOn(Schedulers.io());
	}

	@Override
	public boolean hasAccount(String address) {
		return keyStore.hasAddress(new Address(address));
	}

	@Override
	public Single<Account[]> fetchAccounts() {
		return Single.fromCallable(() -> {
			Accounts accounts = keyStore.getAccounts();
			int len = (int) accounts.size();
			Account[] result = new Account[len];

			for (int i = 0; i < len; i++) {
				org.ethereum.geth.Account gethAccount = accounts.get(i);
				result[i] = new Account(gethAccount.getAddress().getHex().toLowerCase());
			}
			return result;
		}).subscribeOn(Schedulers.io());
	}

	private org.ethereum.geth.Account findAccount(String address) throws ServiceException {
		Accounts accounts = keyStore.getAccounts();
		int len = (int) accounts.size();
		for (int i = 0; i < len; i++) {
			try {
				if (accounts.get(i).getAddress().getHex().equals(address)) {
					return accounts.get(i);
				}
			} catch (Exception ex) {
				/* Quietly: interest only result, maybe next is ok. */
			}
		}
		throw new ServiceException("Account with address: " + address + " not found");
	}
}
