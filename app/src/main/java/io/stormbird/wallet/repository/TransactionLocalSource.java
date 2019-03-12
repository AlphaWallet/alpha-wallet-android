package io.stormbird.wallet.repository;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface TransactionLocalSource {
	Single<Transaction[]> fetchTransaction(Wallet wallet);
	Transaction fetchTransaction(Wallet wallet, String hash);

	Completable putTransactions(Wallet wallet, Transaction[] transactions);

    Single<Transaction> findLast(Wallet wallet);
	Single<Transaction[]> putAndReturnTransactions(Wallet wallet, Transaction[] txList);
}
