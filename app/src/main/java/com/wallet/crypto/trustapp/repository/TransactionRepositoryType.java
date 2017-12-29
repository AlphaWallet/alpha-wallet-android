package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.entity.Transaction;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;

public interface TransactionRepositoryType {
	Observable<Transaction[]> fetchTransaction(Wallet wallet);
	Maybe<Transaction> findTransaction(Wallet wallet, String transactionHash);
	Completable createTransaction(Wallet from, String toAddress, String wei, String password);
}
