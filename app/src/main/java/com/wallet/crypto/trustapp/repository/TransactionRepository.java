package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.ServiceException;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.service.AccountKeystoreService;
import com.wallet.crypto.trustapp.service.BlockExplorerClientType;

import org.reactivestreams.Subscriber;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import io.reactivex.Completable;
import io.reactivex.FlowableOperator;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

public class TransactionRepository implements TransactionRepositoryType {

	private final EthereumNetworkRepositoryType networkRepository;
	private final AccountKeystoreService accountKeystoreService;
	private final TransactionLocalSource transactionLocalSource;
	private final BlockExplorerClientType blockExplorerClient;

	public TransactionRepository(
			EthereumNetworkRepositoryType networkRepository,
			AccountKeystoreService accountKeystoreService,
			TransactionLocalSource inMemoryCache,
			TransactionLocalSource inDiskCache,
			BlockExplorerClientType blockExplorerClient) {
		this.networkRepository = networkRepository;
		this.accountKeystoreService = accountKeystoreService;
		this.blockExplorerClient = blockExplorerClient;
		this.transactionLocalSource = inMemoryCache;

		this.networkRepository.addOnChangeDefaultNetwork(this::onNetworkChanged);
	}

    @Override
	public Observable<Transaction[]> fetchTransaction(Wallet wallet) {
        return Observable.create(e -> {
            Transaction[] transactions = transactionLocalSource.fetchTransaction(wallet).blockingGet();
            e.onNext(transactions == null ? new Transaction[0] : transactions);
            transactions = blockExplorerClient.fetchTransactions(wallet.address).blockingFirst();
            transactionLocalSource.clear();
            transactionLocalSource.putTransactions(wallet, transactions);
            e.onNext(transactions);
            e.onComplete();
        });
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
	public Completable createTransaction(Wallet from, String toAddress, String wei, String password) {
		final Web3j web3j = Web3jFactory
                .build(new HttpService(networkRepository.getDefaultNetwork().rpcServerUrl));

		return Single.fromCallable(() -> {
			EthGetTransactionCount ethGetTransactionCount = web3j
					.ethGetTransactionCount(from.address, DefaultBlockParameterName.LATEST)
					.send();
			return ethGetTransactionCount.getTransactionCount();
		})
		.flatMap(nonce -> accountKeystoreService.signTransaction(
		        from, password, toAddress, wei, nonce.longValue(),
                networkRepository.getDefaultNetwork().chainId))
		.flatMapCompletable(signedMessage -> Completable.fromAction(() -> {
			EthSendTransaction raw = web3j
					.ethSendRawTransaction(Numeric.toHexString(signedMessage))
					.send();
			if (raw.hasError()) {
				throw new ServiceException(raw.getError().getMessage());
			}
		})).observeOn(Schedulers.io());
	}

    private void onNetworkChanged(NetworkInfo networkInfo) {
        transactionLocalSource.clear();
    }
}
