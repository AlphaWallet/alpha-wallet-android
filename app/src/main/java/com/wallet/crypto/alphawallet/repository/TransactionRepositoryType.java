package com.wallet.crypto.alphawallet.repository;

import com.wallet.crypto.alphawallet.entity.TradeInstance;
import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.Wallet;

import java.math.BigInteger;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public interface TransactionRepositoryType {
	Observable<Transaction[]> fetchTransaction(Wallet wallet);
	Maybe<Transaction> findTransaction(Wallet wallet, String transactionHash);
	Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password);
	Single<byte[]> getSignature(Wallet wallet, byte[] message, String password);

	void ProcessMarketOrders(Disposable orderQueue);
	Consumer<? super TradeInstance[]> onOrdersCreated(TradeInstance[] trades);
	//Consumer<TradeInstance[]> onOrdersCreate(TradeInstance[] trades);
}
