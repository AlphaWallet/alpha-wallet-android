package io.stormbird.wallet.repository;

import android.net.Network;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenTransaction;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;

import java.math.BigInteger;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface TransactionRepositoryType {
	public Observable<Transaction[]> fetchCachedTransactions(NetworkInfo network, Wallet wallet);
	Observable<Transaction[]> fetchTransaction(Wallet wallet);
	Observable<Transaction[]> fetchNetworkTransaction(Wallet wallet, long lastBlock);
	Observable<TokenTransaction[]> fetchTokenTransaction(Wallet wallet, Token token, long lastBlock);
	Maybe<Transaction> findTransaction(Wallet wallet, String transactionHash);
	Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password);
	Single<String> createTransaction(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, String password);
	Single<byte[]> getSignature(Wallet wallet, byte[] message, String password);
	Single<byte[]> getSignatureFast(Wallet wallet, byte[] message, String password);
	void unlockAccount(Wallet signer, String signerPassword) throws Exception;
	void lockAccount(Wallet signer, String signerPassword) throws Exception;
	Single<Transaction[]> storeTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] txList);
}
