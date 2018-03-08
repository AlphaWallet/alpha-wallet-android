package com.wallet.crypto.alphawallet.repository;

import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TokenTransaction;
import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.Wallet;

import java.math.BigInteger;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface TransactionRepositoryType {
	Observable<Transaction[]> fetchTransaction(Wallet wallet);
	Observable<TokenTransaction[]> fetchTokenTransaction(Wallet wallet, Token token);
	Maybe<Transaction> findTransaction(Wallet wallet, String transactionHash);
	Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password);
	Single<byte[]> getSignature(Wallet wallet, byte[] message, String password);
	Single<byte[]> getSignatureFast(Wallet wallet, byte[] message, String password);
	void unlockAccount(Wallet signer, String signerPassword) throws Exception;
	void lockAccount(Wallet signer, String signerPassword) throws Exception;
}
