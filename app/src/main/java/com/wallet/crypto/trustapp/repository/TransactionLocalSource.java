package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.Wallet;

import io.reactivex.Single;

public interface TransactionLocalSource {
	Single<Transaction[]> fetchTransaction(Wallet wallet);

	void putTransactions(Wallet wallet, Transaction[] transactions);

    void clear();
}
