package com.alphawallet.app.repository;

import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;

import io.reactivex.Single;

public interface TransactionLocalSource {
	Single<Transaction[]> fetchTransaction(Wallet wallet, int maxTransactions);
	Single<Transaction[]> fetchTransactions(Wallet wallet, Token token, int count);
	Transaction fetchTransaction(Wallet wallet, String hash);
	Single<Transaction[]> putAndReturnTransactions(Wallet wallet, Transaction[] txList);
	void putTransaction(Wallet wallet, Transaction tx);
}
