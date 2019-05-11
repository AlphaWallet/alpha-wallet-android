package io.stormbird.wallet.repository;

import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.service.AccountKeystoreService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.service.TransactionsNetworkClientType;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Bytes;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static io.stormbird.wallet.service.MarketQueueService.sigFromByteArray;

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
	public Observable<Transaction[]> fetchCachedTransactions(Wallet wallet) {
		Log.d(TAG, "Fetching Cached TX: " + wallet.address);
		return fetchFromCache(wallet)
				.observeOn(Schedulers.newThread())
				.toObservable();
	}

	@Override
	public Transaction fetchCachedTransaction(String walletAddr, String hash)
	{
		Wallet wallet = new Wallet(walletAddr);
		return inDiskCache.fetchTransaction(wallet, hash);
	}

	@Override
	public Observable<Transaction[]> fetchNetworkTransaction(NetworkInfo network, String tokenAddress, long lastBlock, String userAddress) {
		return fetchFromNetwork(network, tokenAddress, lastBlock, userAddress)
				.observeOn(Schedulers.newThread())
				.toObservable();
	}

	@Override
	public Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password, int chainId) {
		final Web3j web3j = Web3j.build(new HttpService(networkRepository.getNetworkByChain(chainId, false).rpcServerUrl));

		return networkRepository.getLastTransactionNonce(web3j, from.address)
		.flatMap(nonce -> accountKeystoreService.signTransaction(from, password, toAddress, subunitAmount, gasPrice, gasLimit, nonce.longValue(), data, chainId))
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
	public Single<TransactionData> createTransactionWithSig(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password, int chainId) {
		final Web3j web3j = Web3j.build(new HttpService(networkRepository.getNetworkByChain(chainId, false).rpcServerUrl));

		TransactionData txData = new TransactionData();

		return networkRepository.getLastTransactionNonce(web3j, from.address)
				.flatMap(nonce -> accountKeystoreService.signTransaction(from, password, toAddress, subunitAmount, gasPrice, gasLimit, nonce.longValue(), data, chainId))
				.flatMap(signedMessage -> Single.fromCallable( () -> {
					txData.signature = Numeric.toHexString(signedMessage);
					EthSendTransaction raw = web3j
							.ethSendRawTransaction(Numeric.toHexString(signedMessage))
							.send();
					if (raw.hasError()) {
						throw new Exception(raw.getError().getMessage());
					}
					txData.txHash = raw.getTransactionHash();
					return txData;
				})).subscribeOn(Schedulers.io());
	}

	@Override
	public Single<String> createTransaction(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, String password, int chainId) {
		final Web3j web3j = Web3j.build(new HttpService(networkRepository.getNetworkByChain(chainId, false).rpcServerUrl));

		return networkRepository.getLastTransactionNonce(web3j, from.address)
				.flatMap(nonce -> getRawTransaction(nonce, gasPrice, gasLimit, BigInteger.ZERO, data))
				.flatMap(rawTx -> signEncodeRawTransaction(rawTx, password, from, chainId))
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
	public Single<TransactionData> createTransactionWithSig(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, String password, int chainId) {
		final Web3j web3j = Web3j.build(new HttpService(networkRepository.getNetworkByChain(chainId, false).rpcServerUrl));

		TransactionData txData = new TransactionData();

		return networkRepository.getLastTransactionNonce(web3j, from.address)
				.flatMap(nonce -> getRawTransaction(nonce, gasPrice, gasLimit, BigInteger.ZERO, data))
				.flatMap(rawTx -> signEncodeRawTransaction(rawTx, password, from, chainId))
				.flatMap(signedMessage -> Single.fromCallable( () -> {
					txData.signature = Numeric.toHexString(signedMessage);
					EthSendTransaction raw = web3j
							.ethSendRawTransaction(Numeric.toHexString(signedMessage))
							.send();
					if (raw.hasError()) {
						throw new Exception(raw.getError().getMessage());
					}
					txData.txHash = raw.getTransactionHash();
					return txData;
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
	public Single<byte[]> getSignature(Wallet wallet, byte[] message, String password, int chainId) {
		return accountKeystoreService.signTransaction(wallet, password, message, chainId);
	}

	@Override
	public Single<byte[]> getSignatureFast(Wallet wallet, byte[] message, String password, int chainId) {
		return accountKeystoreService.signTransactionFast(wallet, password, message, chainId);
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

	private Single<Transaction[]> fetchFromCache(Wallet wallet) {
	    return inDiskCache.fetchTransaction(wallet);
    }

	private Single<Transaction[]> fetchFromNetwork(NetworkInfo networkInfo, String tokenAddress, long lastBlock, String userAddress) {
		return Single.fromObservable(blockExplorerClient.fetchLastTransactions(networkInfo, tokenAddress, lastBlock, userAddress));
	}

	@Override
	public Single<Transaction[]> fetchTransactionsFromStorage(Wallet wallet, Token token, int count)
	{
		return inDiskCache.fetchTransactions(wallet, token, count);
	}

	@Override
	public Single<Transaction[]> storeTransactions(Wallet wallet, Transaction[] txList)
	{
		if (txList.length == 0)
		{
			return noTransactions();
		}
		else
		{
			return inDiskCache.putAndReturnTransactions(wallet, txList);
		}
	}

	private Single<Transaction[]> noTransactions()
	{
		return Single.fromCallable(() -> new Transaction[0]);
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

	@Override
	public Single<ContractType> queryInterfaceSpec(String address, TokenInfo tokenInfo)
	{
		NetworkInfo networkInfo = networkRepository.getNetworkByChain(tokenInfo.chainId, false);
		ContractType checked = TokensService.checkInterfaceSpec(tokenInfo.chainId, tokenInfo.address);
		if (tokenInfo.name == null && tokenInfo.symbol == null)
		{
			return Single.fromCallable(() -> ContractType.NOT_SET);
		}
		else if (checked != null && checked != ContractType.NOT_SET && checked != ContractType.OTHER)
		{
			return Single.fromCallable(() -> checked);
		}
		else return blockExplorerClient.checkConstructorArgs(networkInfo, address);
	}
}
