package com.alphawallet.app.repository;

import android.util.Log;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.entity.cryptokeys.SignatureReturnType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.service.AccountKeystoreService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.service.TransactionsNetworkClientType;
import com.alphawallet.app.service.TransactionsService;

import com.alphawallet.token.entity.Signable;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;
import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;
import static com.alphawallet.app.service.KeyService.FAILED_SIGNATURE;

public class TransactionRepository implements TransactionRepositoryType {

	private final String TAG = "TREPO";
	private final EthereumNetworkRepositoryType networkRepository;
	private final AccountKeystoreService accountKeystoreService;
    private final TransactionLocalSource inDiskCache;
    private final TransactionsService transactionsService;

	public TransactionRepository(
			EthereumNetworkRepositoryType networkRepository,
			AccountKeystoreService accountKeystoreService,
			TransactionLocalSource inDiskCache,
			TransactionsService transactionsService) {
		this.networkRepository = networkRepository;
		this.accountKeystoreService = accountKeystoreService;
		this.inDiskCache = inDiskCache;
		this.transactionsService = transactionsService;
	}

	@Override
	public Transaction fetchCachedTransaction(String walletAddr, String hash)
	{
		Wallet wallet = new Wallet(walletAddr);
		return inDiskCache.fetchTransaction(wallet, hash);
	}

	@Override
	public Single<String> resendTransaction(Wallet from, String to, BigInteger subunitAmount, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId)
	{
		final Web3j web3j = getWeb3jService(chainId);
		final BigInteger useGasPrice = gasPriceForNode(chainId, gasPrice);

		return accountKeystoreService.signTransaction(from, to, subunitAmount, useGasPrice, gasLimit, nonce.longValue(), data, chainId)
				.flatMap(signedMessage -> Single.fromCallable( () -> {
					if (signedMessage.sigType != SignatureReturnType.SIGNATURE_GENERATED)
					{
						throw new Exception(signedMessage.failMessage);
					}
					EthSendTransaction raw = web3j
							.ethSendRawTransaction(Numeric.toHexString(signedMessage.signature))
							.send();
					if (raw.hasError())
					{
						throw new Exception(raw.getError().getMessage());
					}
					return raw.getTransactionHash();
				}))
				.flatMap(txHash -> storeUnconfirmedTransaction(from, txHash, to, subunitAmount, nonce, useGasPrice, gasLimit, chainId, data != null ? Numeric.toHexString(data) : "0x"))
				.subscribeOn(Schedulers.io());
	}

	@Override
	public Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId)
	{
		final Web3j web3j = getWeb3jService(chainId);
		final BigInteger useGasPrice = gasPriceForNode(chainId, gasPrice);

		TransactionData txData = new TransactionData();

		return networkRepository.getLastTransactionNonce(web3j, from.address)
				.flatMap(nonce -> {
					txData.nonce = nonce;
					return accountKeystoreService.signTransaction(from, toAddress, subunitAmount, useGasPrice, gasLimit, nonce.longValue(), data, chainId);
				})
				.flatMap(signedMessage -> Single.fromCallable(() -> {
					if (signedMessage.sigType != SignatureReturnType.SIGNATURE_GENERATED)
					{
						throw new Exception(signedMessage.failMessage);
					}
					txData.signature = Numeric.toHexString(signedMessage.signature);
					EthSendTransaction raw = web3j
							.ethSendRawTransaction(Numeric.toHexString(signedMessage.signature))
							.send();
					if (raw.hasError())
					{
						throw new Exception(raw.getError().getMessage());
					}
					txData.txHash = raw.getTransactionHash();
					return txData;
				}))
				.flatMap(tx -> storeUnconfirmedTransaction(from, tx.txHash, toAddress, subunitAmount, tx.nonce, useGasPrice, gasLimit, chainId, data != null ? Numeric.toHexString(data) : "0x"))
				.subscribeOn(Schedulers.io());
	}

	@Override
	public Single<TransactionData> createTransactionWithSig(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId) {
		final Web3j web3j = getWeb3jService(chainId);
		final BigInteger useGasPrice = gasPriceForNode(chainId, gasPrice);

		TransactionData txData = new TransactionData();

		return networkRepository.getLastTransactionNonce(web3j, from.address)
				.flatMap(nonce -> {
					txData.nonce = nonce;
					return accountKeystoreService.signTransaction(from, toAddress, subunitAmount, useGasPrice, gasLimit, nonce.longValue(), data, chainId);
				})
				.flatMap(signedMessage -> Single.fromCallable( () -> {
					if (signedMessage.sigType != SignatureReturnType.SIGNATURE_GENERATED)
					{
						throw new Exception(signedMessage.failMessage);
					}
					txData.signature = Numeric.toHexString(signedMessage.signature);
					EthSendTransaction raw = web3j
							.ethSendRawTransaction(Numeric.toHexString(signedMessage.signature))
							.send();
					if (raw.hasError()) {
						throw new Exception(raw.getError().getMessage());
					}
					txData.txHash = raw.getTransactionHash();
					return txData;
				}))
				.flatMap(tx -> storeUnconfirmedTransaction(from, tx, toAddress, subunitAmount, tx.nonce, useGasPrice, gasLimit, chainId, data != null ? Numeric.toHexString(data) : "0x", ""))
				.subscribeOn(Schedulers.io());
	}

	// Called for constructors from web3 Dapp transaction
	@Override
	public Single<TransactionData> createTransactionWithSig(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, int chainId) {
		final Web3j web3j = getWeb3jService(chainId);
		final BigInteger useGasPrice = gasPriceForNode(chainId, gasPrice);

		TransactionData txData = new TransactionData();

		return networkRepository.getLastTransactionNonce(web3j, from.address)
				.flatMap(nonce -> {
					txData.nonce = nonce;
					return getRawTransaction(nonce, useGasPrice, gasLimit, BigInteger.ZERO, data);
				})
				.flatMap(rawTx -> signEncodeRawTransaction(rawTx, from, chainId))
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
				}))
				.flatMap(tx -> storeUnconfirmedTransaction(from, tx, "", BigInteger.ZERO, txData.nonce, useGasPrice, gasLimit, chainId, data, C.BURN_ADDRESS))
				.subscribeOn(Schedulers.io());
	}

	private BigInteger gasPriceForNode(int chainId, BigInteger gasPrice)
	{
		if (EthereumNetworkRepository.hasGasOverride(chainId)) return EthereumNetworkRepository.gasOverrideValue(chainId);
		else return gasPrice;
	}

	private Single<TransactionData> storeUnconfirmedTransaction(Wallet from, TransactionData txData, String toAddress, BigInteger value, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, int chainId, String data, String contractAddr)
	{
		return Single.fromCallable(() -> {
			Transaction newTx = new Transaction(txData.txHash, "0", "0", System.currentTimeMillis()/1000, nonce.intValue(), from.address, toAddress, value.toString(10), "0", gasPrice.toString(10), data,
					gasLimit.toString(10), chainId, contractAddr);
			inDiskCache.putTransaction(from, newTx);
			transactionsService.markPending(newTx);

			return txData;
		});
	}

	private Single<String> storeUnconfirmedTransaction(Wallet from, String txHash, String toAddress, BigInteger value, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, int chainId, String data)
	{
		return Single.fromCallable(() -> {

			Transaction newTx = new Transaction(txHash, "0", "0", System.currentTimeMillis()/1000, nonce.intValue(), from.address, toAddress, value.toString(10), "0", gasPrice.toString(10), data,
					gasLimit.toString(10), chainId, "");
			inDiskCache.putTransaction(from, newTx);
			transactionsService.markPending(newTx);

			return txHash;
		});
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

	private Single<byte[]> signEncodeRawTransaction(RawTransaction rtx, Wallet wallet, int chainId)
	{
		return Single.fromCallable(() -> TransactionEncoder.encode(rtx))
				.flatMap(encoded -> accountKeystoreService.signTransaction(wallet, encoded, chainId))
				.flatMap(signatureReturn -> {
						 	if (signatureReturn.sigType != SignatureReturnType.SIGNATURE_GENERATED)
							{
								throw new Exception(signatureReturn.failMessage);
							}
						 	return encodeTransaction(signatureReturn.signature, rtx);
						 });
	}

	private Single<byte[]> encodeTransaction(byte[] signatureBytes, RawTransaction rtx)
	{
		return Single.fromCallable(() -> {
			Sign.SignatureData sigData = sigFromByteArray(signatureBytes);
			if (sigData == null) return FAILED_SIGNATURE.getBytes();
			return encode(rtx, sigData);
		});
	}

	@Override
	public Single<SignatureFromKey> getSignature(Wallet wallet, Signable message, int chainId) {
		return accountKeystoreService.signMessage(wallet, message, chainId);
	}

	@Override
	public Single<byte[]> getSignatureFast(Wallet wallet, String password, byte[] message, int chainId) {
		return accountKeystoreService.signTransactionFast(wallet, password, message, chainId);
	}

	/**
	 * From Web3j to encode a constructor
	 * @param rawTransaction
	 * @param signatureData
	 * @return
	 */
	private static byte[] encode(RawTransaction rawTransaction, Sign.SignatureData signatureData) {
		List<RlpType> values = TransactionEncoder.asRlpValues(rawTransaction, signatureData);
		RlpList rlpList = new RlpList(values);
		return RlpEncoder.encode(rlpList);
	}

	@Override
	public void removeOldTransaction(Wallet wallet, String oldTxHash)
	{
		inDiskCache.deleteTransaction(wallet, oldTxHash);
	}

	@Override
	public Single<ActivityMeta[]> fetchCachedTransactionMetas(Wallet wallet, List<Integer> networkFilters, long fetchTime, int fetchLimit)
	{
		return inDiskCache.fetchActivityMetas(wallet, networkFilters, fetchTime, fetchLimit);
	}

	@Override
	public Single<ActivityMeta[]> fetchCachedTransactionMetas(Wallet wallet, int chainId, String tokenAddress, int historyCount)
	{
		return inDiskCache.fetchActivityMetas(wallet, chainId, tokenAddress, historyCount);
	}

	@Override
	public Single<ActivityMeta[]> fetchEventMetas(Wallet wallet, List<Integer> networkFilters)
	{
		return inDiskCache.fetchEventMetas(wallet, networkFilters);
	}

	@Override
	public Realm getRealmInstance(Wallet wallet)
	{
		return inDiskCache.getRealmInstance(wallet);
	}

	@Override
	public RealmAuxData fetchCachedEvent(String walletAddress, String eventKey)
	{
		return inDiskCache.fetchEvent(walletAddress, eventKey);
	}
}
