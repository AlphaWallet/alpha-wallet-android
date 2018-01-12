package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.Wallet;

import io.reactivex.Single;

public interface TransactionLocalSource {
	Single<Transaction[]> fetchTransaction(NetworkInfo networkInfo, Wallet wallet);

	void putTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] transactions);

    Single<Transaction> findLast(NetworkInfo networkInfo, Wallet wallet);
}
