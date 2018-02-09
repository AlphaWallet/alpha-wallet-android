package com.wallet.crypto.alphawallet.repository;

import android.support.annotation.Nullable;

import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.TradeInstance;
import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.service.AccountKeystoreService;
import com.wallet.crypto.alphawallet.service.TransactionsNetworkClientType;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class TransactionRepository implements TransactionRepositoryType {

	private final EthereumNetworkRepositoryType networkRepository;
	private final AccountKeystoreService accountKeystoreService;
    private final TransactionLocalSource inDiskCache;
    private final TransactionsNetworkClientType blockExplorerClient;

	public TransactionRepository(
			EthereumNetworkRepositoryType networkRepository,
			AccountKeystoreService accountKeystoreService,
			TransactionLocalSource inDiskCache,
			TransactionsNetworkClientType blockExplorerClient) {
		this.networkRepository = networkRepository;
		this.accountKeystoreService = accountKeystoreService;
		this.blockExplorerClient = blockExplorerClient;
		this.inDiskCache = inDiskCache;
	}

    @Override
	public Observable<Transaction[]> fetchTransaction(Wallet wallet) {
        NetworkInfo networkInfo = networkRepository.getDefaultNetwork();
	    return Single.merge(
	            fetchFromCache(networkInfo, wallet),
	            fetchAndCacheFromNetwork(networkInfo, wallet))
                .toObservable();
    }

	@Override
	public Maybe<Transaction> findTransaction(Wallet wallet, String transactionHash) {
		return fetchTransaction(wallet)
				.firstElement()
                .flatMap(transactions -> {
					for (Transaction transaction : transactions) {
						if (transaction.hash.equals(transactionHash)) {
							return Maybe.just(transaction);
						}
					}
					return null;
				});
	}

	@Override
	public Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password) {
		final Web3j web3j = Web3jFactory.build(new HttpService(networkRepository.getDefaultNetwork().rpcServerUrl));

		return Single.fromCallable(() -> {
			EthGetTransactionCount ethGetTransactionCount = web3j
					.ethGetTransactionCount(from.address, DefaultBlockParameterName.LATEST)
					.send();
			return ethGetTransactionCount.getTransactionCount();
		})
		.flatMap(nonce -> accountKeystoreService.signTransaction(from, password, toAddress, subunitAmount, gasPrice, gasLimit, nonce.longValue(), data, networkRepository.getDefaultNetwork().chainId))
		.flatMap(signedMessage -> Single.fromCallable( () -> {
			EthSendTransaction raw = web3j
					.ethSendRawTransaction(Numeric.toHexString(signedMessage))
					.send();
			if (raw.hasError()) {
			    throw new Exception(raw.getError().getMessage());
			}
			return raw.getTransactionHash();
		})).subscribeOn(Schedulers.io());
	}

	@Override
	public Single<byte[]> getSignature(Wallet wallet, byte[] message, String password) {
		return accountKeystoreService.signTransaction(wallet, password, message, networkRepository.getDefaultNetwork().chainId);
	}

	@Override
	public Single<byte[]> getSignatureFast(Wallet wallet, byte[] message, String password) {
		return accountKeystoreService.signTransactionFast(wallet, password, message, networkRepository.getDefaultNetwork().chainId);
	}

	@Override
	public void unlockAccount(Wallet signer, String signerPassword) throws Exception
	{
		accountKeystoreService.unlockAccount(signer, signerPassword);
	}

	@Override
	public void lockAccount(Wallet signer, String signerPassword) throws Exception
	{
		accountKeystoreService.lockAccount(signer, signerPassword);
	}

	private Single<Transaction[]> fetchFromCache(NetworkInfo networkInfo, Wallet wallet) {
	    return inDiskCache.fetchTransaction(networkInfo, wallet);
    }

	private Single<Transaction[]> fetchAndCacheFromNetwork(NetworkInfo networkInfo, Wallet wallet) {
        return inDiskCache
                .findLast(networkInfo, wallet)
                .flatMap(lastTransaction -> Single.fromObservable(blockExplorerClient
                        .fetchLastTransactions(wallet, lastTransaction)))
                .onErrorResumeNext(throwable -> Single.fromObservable(blockExplorerClient
                        .fetchLastTransactions(wallet, null)))
                .flatMapCompletable(transactions -> inDiskCache.putTransactions(networkInfo, wallet, transactions))
                .andThen(inDiskCache.fetchTransaction(networkInfo, wallet));
    }
}
