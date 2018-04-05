package io.awallet.crypto.alphawallet.repository;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.Wallet;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface TransactionLocalSource {
	Single<Transaction[]> fetchTransaction(NetworkInfo networkInfo, Wallet wallet);

	Completable putTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] transactions);

    Single<Transaction> findLast(NetworkInfo networkInfo, Wallet wallet);
	Single<Transaction[]> putAndReturnTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] txList);
}
