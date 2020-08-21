package com.alphawallet.app.repository;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;

import java.util.List;

import io.reactivex.Single;

public interface TransactionLocalSource {
	Single<Transaction[]> fetchTransaction(Wallet wallet, int maxTransactions, List<Integer> networkFilters);
	Single<Transaction[]> fetchTransactions(Wallet wallet, Token token, int count);
	Transaction fetchTransaction(Wallet wallet, String hash);
	Single<Transaction[]> putAndReturnTransactions(Wallet wallet, Transaction[] txList);
	void putTransaction(Wallet wallet, Transaction tx);
    void deleteTransaction(Wallet wallet, String oldTxHash);

    Single<Transaction[]> markTransactionDropped(Wallet wallet, String hash);
}
