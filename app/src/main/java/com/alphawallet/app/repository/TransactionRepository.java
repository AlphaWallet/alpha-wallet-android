package com.alphawallet.app.repository;

import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.entity.cryptokeys.SignatureReturnType;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.service.AccountKeystoreService;
import com.alphawallet.app.service.TransactionsService;
import com.alphawallet.token.entity.Signable;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

public class TransactionRepository implements TransactionRepositoryType
{

    private final String TAG = "TREPO";
    private final EthereumNetworkRepositoryType networkRepository;
    private final AccountKeystoreService accountKeystoreService;
    private final TransactionLocalSource inDiskCache;
    private final TransactionsService transactionsService;

    public TransactionRepository(
            EthereumNetworkRepositoryType networkRepository,
            AccountKeystoreService accountKeystoreService,
            TransactionLocalSource inDiskCache,
            TransactionsService transactionsService)
    {
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
    public long fetchTxCompletionTime(String walletAddr, String hash)
    {
        Wallet wallet = new Wallet(walletAddr);
        return inDiskCache.fetchTxCompletionTime(wallet, hash);
    }

    @Override
    public Single<String> resendTransaction(Wallet from, String to, BigInteger subunitAmount, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, byte[] data, long chainId)
    {
        final Web3j web3j = getWeb3jService(chainId);
        final BigInteger useGasPrice = gasPriceForNode(chainId, gasPrice);

        return accountKeystoreService.signTransaction(from, to, subunitAmount, useGasPrice, gasLimit, nonce.longValue(), data, chainId)
                .flatMap(signedMessage -> Single.fromCallable(() -> {
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
    public Single<TransactionData> create1559TransactionWithSig(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasLimit, BigInteger maxFeePerGas,
                                                                BigInteger maxPriorityFee, long nonce, byte[] data, long chainId)
    {
        final Web3j web3j = getWeb3jService(chainId);

        TransactionData txData = new TransactionData();

        return getNonceForTransaction(web3j, from.address, nonce)
                .flatMap(txNonce -> {
                    txData.nonce = txNonce;
                    return accountKeystoreService.signTransactionEIP1559(from, toAddress, subunitAmount, gasLimit, maxFeePerGas, maxPriorityFee, txNonce.longValue(), data, chainId);
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
                .flatMap(tx -> storeUnconfirmedTransaction(from, tx, toAddress, subunitAmount, tx.nonce, maxFeePerGas, maxPriorityFee, gasLimit, chainId,
                        data != null ? Numeric.toHexString(data) : "0x", ""))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<TransactionData> createTransactionWithSig(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, long nonce, byte[] data, long chainId)
    {
        final Web3j web3j = getWeb3jService(chainId);
        final BigInteger useGasPrice = gasPriceForNode(chainId, gasPrice);

        TransactionData txData = new TransactionData();

        return getNonceForTransaction(web3j, from.address, nonce)
                .flatMap(txNonce -> {
                    txData.nonce = txNonce;
                    return accountKeystoreService.signTransaction(from, toAddress, subunitAmount, useGasPrice, gasLimit, txNonce.longValue(), data, chainId);
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
                .flatMap(tx -> storeUnconfirmedTransaction(from, tx, toAddress, subunitAmount, tx.nonce, useGasPrice, gasLimit, chainId,
                        data != null ? Numeric.toHexString(data) : "0x", ""))
                .subscribeOn(Schedulers.io());
    }

    /**
     * * Given a Web3Transaction, return a signature. Note that we can't fix up nonce, gas price or limit;
     * * This is a request to sign a transaction from an external source -
     * * presumably that external source will broadcast the transaction; together with this signature
     *
     * @param from
     * @param toAddress
     * @param subunitAmount
     * @param gasPrice
     * @param gasLimit
     * @param nonce
     * @param data
     * @param chainId
     * @return
     */
    @Override
    public Single<TransactionData> getSignatureForTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, long nonce,
                                                              byte[] data, long chainId)
    {
        final Web3j web3j = getWeb3jService(chainId);
        final BigInteger useGasPrice = gasPriceForNode(chainId, gasPrice);
        TransactionData txData = new TransactionData();

        return getNonceForTransaction(web3j, from.address, nonce)
                .flatMap(txNonce -> {
                    txData.nonce = txNonce;
                    return accountKeystoreService.signTransaction(from, toAddress, subunitAmount, useGasPrice, gasLimit, txNonce.longValue(), data, chainId);
                })
                .flatMap(signedMessage -> Single.fromCallable(() -> {
                    if (signedMessage.sigType != SignatureReturnType.SIGNATURE_GENERATED)
                    {
                        throw new Exception(signedMessage.failMessage);
                    }
                    txData.signature = Numeric.toHexString(signedMessage.signature);
                    return txData;
                }));
    }

    private BigInteger gasPriceForNode(long chainId, BigInteger gasPrice)
    {
        if (EthereumNetworkRepository.hasGasOverride(chainId)) return EthereumNetworkRepository.gasOverrideValue(chainId);
        else return gasPrice;
    }

    //EIP1559
    private Single<TransactionData> storeUnconfirmedTransaction(Wallet from, TransactionData txData, String toAddress, BigInteger value, BigInteger nonce, BigInteger maxFeePerGas,
                                                                BigInteger maxPriorityFee, BigInteger gasLimit, long chainId, String data, String contractAddr)
    {
        return Single.fromCallable(() -> {
            Transaction newTx = new Transaction(txData.txHash, "0", "0", System.currentTimeMillis() / 1000, nonce.intValue(), from.address, toAddress,
                    value.toString(10), "0", "0", maxFeePerGas.toString(10),
                    maxPriorityFee.toString(10), data,
                    gasLimit.toString(10), chainId, contractAddr);
            inDiskCache.putTransaction(from, newTx);
            transactionsService.markPending(newTx);

            return txData;
        });
    }

    private Single<TransactionData> storeUnconfirmedTransaction(Wallet from, TransactionData txData, String toAddress, BigInteger value, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit,
                                                                long chainId, String data, String contractAddr)
    {
        return Single.fromCallable(() -> {
            Transaction newTx = new Transaction(txData.txHash, "0", "0", System.currentTimeMillis() / 1000, nonce.intValue(), from.address, toAddress,
                    value.toString(10), "0", gasPrice.toString(10), data,
                    gasLimit.toString(10), chainId, contractAddr);
            //newTx.completeSetup(from.address);
            inDiskCache.putTransaction(from, newTx);
            transactionsService.markPending(newTx);

            return txData;
        });
    }

    private Single<String> storeUnconfirmedTransaction(Wallet from, String txHash, String toAddress, BigInteger value, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit,
                                                       long chainId, String data)
    {
        return Single.fromCallable(() -> {

            Transaction newTx = new Transaction(txHash, "0", "0", System.currentTimeMillis() / 1000, nonce.intValue(), from.address,
                    toAddress, value.toString(10), "0", gasPrice.toString(10), data,
                    gasLimit.toString(10), chainId, "");
            //newTx.completeSetup(from.address);
            inDiskCache.putTransaction(from, newTx);
            transactionsService.markPending(newTx);

            return txHash;
        });
    }

    @Override
    public Single<SignatureFromKey> getSignature(Wallet wallet, Signable message)
    {
        return accountKeystoreService.signMessage(wallet, message);
    }

    @Override
    public Single<byte[]> getSignatureFast(Wallet wallet, String password, byte[] message)
    {
        return accountKeystoreService.signMessageFast(wallet, password, message);
    }

    @Override
    public Single<ActivityMeta[]> fetchCachedTransactionMetas(Wallet wallet, List<Long> networkFilters, long fetchTime, int fetchLimit)
    {
        return inDiskCache.fetchActivityMetas(wallet, networkFilters, fetchTime, fetchLimit);
    }

    @Override
    public Single<ActivityMeta[]> fetchCachedTransactionMetas(Wallet wallet, long chainId, String tokenAddress, int historyCount)
    {
        return inDiskCache.fetchActivityMetas(wallet, chainId, tokenAddress, historyCount);
    }

    @Override
    public Single<ActivityMeta[]> fetchEventMetas(Wallet wallet, List<Long> networkFilters)
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

    @Override
    public void restartService()
    {
        transactionsService.startUpdateCycle();
    }

    private Single<BigInteger> getNonceForTransaction(Web3j web3j, String wallet, long nonce)
    {
        if (nonce != -1) //use supplied nonce
        {
            return Single.fromCallable(() -> BigInteger.valueOf(nonce));
        }
        else
        {
            return networkRepository.getLastTransactionNonce(web3j, wallet);
        }
    }
}
