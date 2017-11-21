package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.entity.Transaction;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public interface TransactionRepositoryType {
	Single<Transaction[]> fetchTransaction(Account account);
	Maybe<Transaction> findTransaction(Account account, String transactionHash);
	Completable createTransaction(Account from, String toAddress, String wei, String password);
}
