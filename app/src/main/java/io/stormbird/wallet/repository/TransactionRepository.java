package io.stormbird.wallet.repository;

import android.util.Log;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Bytes;
import org.web3j.utils.Numeric;

import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenTransaction;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.service.AccountKeystoreService;
import io.stormbird.wallet.service.TransactionsNetworkClientType;

import static io.stormbird.wallet.service.MarketQueueService.sigFromByteArray;
import static org.web3j.crypto.TransactionEncoder.encode;

public class TransactionRepository implements TransactionRepositoryType {

	private final String TAG = "TREPO";
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
	public Observable<Transaction[]> fetchCachedTransactions(NetworkInfo network, Wallet wallet) {
		Log.d(TAG, "Fetching Cached TX: " + network.name + " : " + wallet.address);
		return fetchFromCache(network, wallet)
				.observeOn(Schedulers.newThread())
				.toObservable();
	}

    @Override
	public Observable<Transaction[]> fetchTransaction(Wallet wallet) {
        NetworkInfo networkInfo = networkRepository.getDefaultNetwork();
	    return Single.merge(
	            fetchFromCache(networkInfo, wallet),
	            fetchAndCacheFromNetwork(networkInfo, wallet))
				.observeOn(Schedulers.newThread())
                .toObservable();
    }

	@Override
	public Observable<Transaction[]> fetchNetworkTransaction(Wallet wallet, long lastBlock) {
		NetworkInfo networkInfo = networkRepository.getDefaultNetwork();
		return fetchFromNetwork(networkInfo, wallet, lastBlock)
				.observeOn(Schedulers.newThread())
				.toObservable();
	}

	/**
	 * Either fetches a list of network transactions for this token address,
	 * or just a blank list is the token is dead
	 * TODO: we should be using a map/reduce instead
	 * @param wallet
	 * @param token
	 * @return
	 */
	@Override
	public Observable<TokenTransaction[]> fetchTokenTransaction(Wallet wallet, Token token, long lastBlock) {
		NetworkInfo networkInfo = networkRepository.getDefaultNetwork();

		return fetchFromNetwork(networkInfo, wallet, lastBlock+1) //+1 because we already have the transactions in the last block
					.observeOn(Schedulers.io())
					.map(txs -> mapToTokenTransactions(txs, token))
					.toObservable();
	}

//	@Override
//	public Observable<TokenTransaction[]> fetchTokenTransaction(Wallet wallet, Token token) {
//		NetworkInfo networkInfo = networkRepository.getDefaultNetwork();
//		return fetchFromCache(networkInfo, wallet)
//				.mergeWith(fetchAndCacheFromNetwork(networkInfo, wallet))
//					.observeOn(Schedulers.io())
//					.map(txs -> mapToTokenTransactions(txs, token))
//					.toObservable();
//	}

//	@Override
//	public void fetchTransaction2(Wallet wallet, TransactionsCallback txCallback) {
//		NetworkInfo networkInfo = networkRepository.getDefaultNetwork();
//
//		Transaction lastTx = inDiskCache.findLast(networkInfo, wallet).blockingGet();
//				blockExplorerClient
//						.fetchTransactions2(wallet, lastTx, txCallback);
//	}

	private TokenTransaction[] mapToTokenTransactions(Transaction[] items, Token token)
	{
		TokenTransaction[] ttxList = new TokenTransaction[items.length];
		for (int i = 0; i < items.length; i++) {
			ttxList[i] = new TokenTransaction(token, items[i]);
		}

		return ttxList;
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
		final Web3j web3j = Web3jFactory.build(new HttpService(networkRepository.getActiveRPC()));

		return networkRepository.getLastTransactionNonce(web3j, from.address)
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
	public Single<String> createTransaction(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, String password) {
		final Web3j web3j = Web3jFactory.build(new HttpService(networkRepository.getActiveRPC()));

		return networkRepository.getLastTransactionNonce(web3j, from.address)
				.flatMap(nonce -> getRawTransaction(nonce, gasPrice, gasLimit, BigInteger.ZERO, data))
				.flatMap(rawTx -> signEncodeRawTransaction(rawTx, password, from, networkRepository.getDefaultNetwork().chainId))
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

	private Single<RawTransaction> getRawTransaction(BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, BigInteger value, String data)
	{
		return Single.fromCallable(() ->
			RawTransaction.createContractTransaction(
					nonce,
					gasPrice,
					gasLimit,
					value,
					data));
	}

	private Single<byte[]> signEncodeRawTransaction(RawTransaction rtx, String password, Wallet wallet, int chainId)
	{
		return Single.fromCallable(() -> TransactionEncoder.encode(rtx))
				.flatMap(encoded -> accountKeystoreService.signTransaction(wallet, password, encoded, chainId))
				.flatMap(signatureBytes -> encodeTransaction(signatureBytes, rtx));
	}

	private Single<byte[]> encodeTransaction(byte[] signatureBytes, RawTransaction rtx)
	{
		return Single.fromCallable(() -> {
			Sign.SignatureData sigData = sigFromByteArray(signatureBytes);
			return encode(rtx, sigData);
		});
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
                        .fetchLastTransactions(networkInfo, wallet, Long.valueOf(lastTransaction.blockNumber))))
                .onErrorResumeNext(throwable -> Single.fromObservable(blockExplorerClient
                        .fetchLastTransactions(networkInfo, wallet, 0)))
                .flatMapCompletable(transactions -> inDiskCache.putTransactions(networkInfo, wallet, transactions))
                .andThen(inDiskCache.fetchTransaction(networkInfo, wallet))
				.observeOn(Schedulers.io());
    }

	private Single<Transaction[]> fetchAllFromNetwork(NetworkInfo networkInfo, Wallet wallet)
	{
		return Single.fromObservable(blockExplorerClient.fetchLastTransactions(networkInfo, wallet, 0))
				.observeOn(Schedulers.io());
	}

	private Single<Transaction[]> fetchFromNetwork(NetworkInfo networkInfo, Wallet wallet, long lastBlock) {
		return Single.fromObservable(blockExplorerClient.fetchLastTransactions(networkInfo, wallet, lastBlock));
	}

	@Override
	public Single<Transaction[]> storeTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] txList)
	{
		return inDiskCache.putAndReturnTransactions(networkInfo, wallet, txList);
	}


	/**
	 * From Web3j to encode a constructor
	 * @param rawTransaction
	 * @param signatureData
	 * @return
	 */
	private static byte[] encode(RawTransaction rawTransaction, Sign.SignatureData signatureData) {
		List<RlpType> values = asRlpValues(rawTransaction, signatureData);
		RlpList rlpList = new RlpList(values);
		return RlpEncoder.encode(rlpList);
	}

	/**
	 * Taken from Web3j to encode RLP strings
	 * @param rawTransaction
	 * @param signatureData
	 * @return
	 */
	private static List<RlpType> asRlpValues(
			RawTransaction rawTransaction, Sign.SignatureData signatureData) {
		List<RlpType> result = new ArrayList<>();

		result.add(RlpString.create(rawTransaction.getNonce()));
		result.add(RlpString.create(rawTransaction.getGasPrice()));
		result.add(RlpString.create(rawTransaction.getGasLimit()));

		// an empty to address (contract creation) should not be encoded as a numeric 0 value
		String to = rawTransaction.getTo();
		if (to != null && to.length() > 0) {
			// addresses that start with zeros should be encoded with the zeros included, not
			// as numeric values
			result.add(RlpString.create(Numeric.hexStringToByteArray(to)));
		} else {
			result.add(RlpString.create(""));
		}

		result.add(RlpString.create(rawTransaction.getValue()));

		// value field will already be hex encoded, so we need to convert into binary first
		byte[] data = Numeric.hexStringToByteArray(rawTransaction.getData());
		result.add(RlpString.create(data));

		if (signatureData != null) {
			result.add(RlpString.create(signatureData.getV()));
			result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
			result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
		}

		return result;
	}
}
