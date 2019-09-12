package io.stormbird.wallet.repository;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface TransactionLocalSource {
	Single<Transaction[]> fetchTransaction(Wallet wallet, int maxTransactions);
	Single<Transaction[]> fetchTransactions(Wallet wallet, Token token, int count);
	Transaction fetchTransaction(Wallet wallet, String hash);
	Single<Transaction[]> putAndReturnTransactions(Wallet wallet, Transaction[] txList);
	void putTransaction(Wallet wallet, Transaction tx);
}
