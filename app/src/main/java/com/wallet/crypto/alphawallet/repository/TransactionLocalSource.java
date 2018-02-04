package com.wallet.crypto.alphawallet.repository;

import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.Wallet;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface TransactionLocalSource {
	Single<Transaction[]> fetchTransaction(NetworkInfo networkInfo, Wallet wallet);

	Completable putTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] transactions);

    Single<Transaction> findLast(NetworkInfo networkInfo, Wallet wallet);
}
