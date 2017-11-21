package com.wallet.crypto.trustapp.repository;

import android.text.format.DateUtils;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.entity.Transaction;

import java.util.Map;

import io.reactivex.Single;

// TODO: Add pagination.
public class TransactionInMemorySource implements TransactionLocalSource {

	private static final long MAX_TIME_OUT = DateUtils.MINUTE_IN_MILLIS;
	private final Map<String, CacheUnit> cache = new java.util.concurrent.ConcurrentHashMap<>();

	@Override
	public Single<Transaction[]> fetchTransaction(Account account) {
		return Single.fromCallable(() -> {
			CacheUnit unit = cache.get(account.address);
			Transaction[] transactions = null;
			if (unit != null) {
				if (System.currentTimeMillis() - unit.create > MAX_TIME_OUT) {
					cache.remove(account.address);
				} else {
					transactions = unit.transactions;
				}

			}
			return transactions;
		});
	}

	@Override
	public void putTransactions(Account account, Transaction[] transactions) {
		cache.put(account.address, new CacheUnit(account.address, System.currentTimeMillis(), transactions));
	}

	private static class CacheUnit {
		final String accountAddress;
		final long create;
		final Transaction[] transactions;

		private CacheUnit(String accountAddress, long create, Transaction[] transactions) {
			this.accountAddress = accountAddress;
			this.create = create;
			this.transactions = transactions;
		}
	}
}
