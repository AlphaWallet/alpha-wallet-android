package io.stormbird.wallet.repository;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface TransactionLocalSource {
	Single<Transaction[]> fetchTransaction(NetworkInfo networkInfo, Wallet wallet);
	Transaction fetchTransaction(NetworkInfo networkInfo, Wallet wallet, String hash);

	Completable putTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] transactions);

    Single<Transaction> findLast(NetworkInfo networkInfo, Wallet wallet);
	Single<Transaction[]> putAndReturnTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] txList);
}
