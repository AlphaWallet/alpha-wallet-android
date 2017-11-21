package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.entity.Transaction;

import io.reactivex.Single;

public class TransactionInDiskSource implements TransactionLocalSource {
	@Override
	public Single<Transaction[]> fetchTransaction(Account account) {
		return null;
	}

	@Override
	public void putTransactions(Account account, Transaction[] transactions) {

	}
}
