package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.entity.Transaction;

import io.reactivex.Single;

public interface TransactionLocalSource {
	Single<Transaction[]> fetchTransaction(Account account);

	void putTransactions(Account account, Transaction[] transactions);
}
