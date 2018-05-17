package io.awallet.crypto.alphawallet.repository;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenTransaction;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.Wallet;

import java.math.BigInteger;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface TransactionRepositoryType {
	public Observable<Transaction[]> fetchCachedTransactions(Wallet wallet);
	Observable<Transaction[]> fetchTransaction(Wallet wallet);
	Observable<Transaction[]> fetchNetworkTransaction(Wallet wallet, long lastBlock);
	Observable<Transaction[]> fetchInternalTransactionsNetwork(Wallet wallet, String feemaster);
	Observable<TokenTransaction[]> fetchTokenTransaction(Wallet wallet, Token token);
	Maybe<Transaction> findTransaction(Wallet wallet, String transactionHash);
	Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password);
	Single<byte[]> getSignature(Wallet wallet, byte[] message, String password);
	Single<byte[]> getSignatureFast(Wallet wallet, byte[] message, String password);
	void unlockAccount(Wallet signer, String signerPassword) throws Exception;
	void lockAccount(Wallet signer, String signerPassword) throws Exception;
	Single<Transaction[]> storeTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] txList);
}
