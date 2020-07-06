package com.alphawallet.app.repository;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;

import java.util.List;

import io.reactivex.Single;
import io.realm.Realm;

public interface TransactionLocalSource {
	Single<ActivityMeta[]> fetchActivityMetas(Wallet wallet, List<Integer> networkFilters, long fetchTime, int fetchLimit);
	Transaction fetchTransaction(Wallet wallet, String hash);
	void putTransaction(Wallet wallet, Transaction tx);
    void deleteTransaction(Wallet wallet, String oldTxHash);

	Realm getRealmInstance(Wallet wallet);
	Single<ActivityMeta[]> fetchActivityMetas(Wallet wallet, int chainId, String tokenAddress, int historyCount);
}
