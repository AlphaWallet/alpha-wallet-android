package com.alphawallet.app.repository;

import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;

import android.text.TextUtils;
import android.util.Pair;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.service.AccountKeystoreService;
import com.alphawallet.app.service.TransactionsService;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.hardware.SignatureReturnType;
import com.alphawallet.token.entity.Signable;

import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.transaction.type.ITransaction;
import org.web3j.crypto.transaction.type.Transaction1559;
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
    public Single<Pair<SignatureFromKey, RawTransaction>> signTransaction(Wallet from, Web3Transaction w3Tx, long chainId)
    {
        return getNonceForTransaction(getWeb3jService(chainId), from.address, w3Tx.nonce) //Note here if the supplied nonce is zero or greater then simply pass that on
                .map(txNonce -> formatRawTransaction(w3Tx, txNonce.longValue(), chainId))
                .map(rtx -> new Pair<>(accountKeystoreService.signTransaction(from, chainId, rtx).blockingGet(),
                        rtx))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public RawTransaction formatRawTransaction(Web3Transaction w3Tx, long nonce, long chainId)
    {
        if (w3Tx.isLegacyTransaction())
        {
            return formatRawTransaction(w3Tx.getTransactionDestination().toString(), w3Tx.value, w3Tx.gasPrice, w3Tx.gasLimit, nonce,
                    !TextUtils.isEmpty(w3Tx.payload) ? Numeric.hexStringToByteArray(w3Tx.payload) : new byte[0]);
        }
        else
        {
            return formatRawTransaction(w3Tx.getTransactionDestination().toString(), chainId, w3Tx.value, w3Tx.gasLimit, w3Tx.maxPriorityFeePerGas,
                    w3Tx.maxFeePerGas, nonce, !TextUtils.isEmpty(w3Tx.payload) ? Numeric.hexStringToByteArray(w3Tx.payload) : new byte[0]);
        }
    }

    @Override
    public Single<String> sendTransaction(Wallet from, RawTransaction rtx, SignatureFromKey sigData, long chainId)
    {
        return Single.fromCallable(() -> {
            if (sigData.sigType != SignatureReturnType.SIGNATURE_GENERATED)
            {
                throw new Exception(sigData.failMessage);
            }
            EthSendTransaction raw = getWeb3jService(chainId)
                    .ethSendRawTransaction(Numeric.toHexString(sigData.signature))
                    .send();
            if (raw.hasError())
            {
                throw new Exception(raw.getError().getMessage());
            }
            return raw.getTransactionHash();
        }).flatMap(txHash -> storeUnconfirmedTransaction(from, txHash, rtx.getTransaction(), chainId, rtx.getData().length() > 2 ? rtx.getTo() : ""))
        .subscribeOn(Schedulers.io());
    }

    //TODO: Re-do this for separate signing
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

    private BigInteger gasPriceForNode(long chainId, BigInteger gasPrice)
    {
        if (EthereumNetworkRepository.hasGasOverride(chainId)) return EthereumNetworkRepository.gasOverrideValue(chainId);
        else return gasPrice;
    }

    private Single<String> storeUnconfirmedTransaction(Wallet from, String txHash, ITransaction itx, long chainId, String contractAddr)
    {
        return Single.fromCallable(() -> {
            Transaction newTx;
            if (itx instanceof Transaction1559)
            {
                newTx = new Transaction(txHash, "0", "0", System.currentTimeMillis() / 1000, itx.getNonce().intValue(), from.address,
                        itx.getTo(), itx.getValue().toString(10), "0", "0", ((Transaction1559) itx).getMaxFeePerGas().toString(10),
                        ((Transaction1559) itx).getMaxPriorityFeePerGas().toString(10), itx.getData(),
                        itx.getGasLimit().toString(10), chainId, contractAddr);
            }
            else
            {
                newTx = new Transaction(txHash, "0", "0", System.currentTimeMillis() / 1000, itx.getNonce().intValue(), from.address,
                        itx.getTo(), itx.getValue().toString(10), "0", itx.getGasPrice().toString(10), itx.getData(),
                        itx.getGasLimit().toString(10), chainId, contractAddr, ""); //TODO: Function Name
            }

            inDiskCache.putTransaction(from, newTx);
            transactionsService.markPending(newTx);

            return txHash;
        });
    }

    private Single<String> storeUnconfirmedTransaction(Wallet from, String txHash, String toAddress, BigInteger value, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit,
                                                       long chainId, String data)
    {
        return Single.fromCallable(() -> {

            Transaction newTx = new Transaction(txHash, "0", "0", System.currentTimeMillis() / 1000, nonce.intValue(), from.address,
                    toAddress, value.toString(10), "0", gasPrice.toString(10), data,
                    gasLimit.toString(10), chainId, "", ""); //TODO: Function Name
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

    public Single<Transaction> fetchTransactionFromNode(String walletAddress, long chainId, String hash)
    {
        return transactionsService.fetchTransaction(walletAddress, chainId, hash)
                .map(tx -> inDiskCache.putTransaction(new Wallet(walletAddress), tx));
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

    /**
     * Format a legacy transaction
     */
    private RawTransaction formatRawTransaction(String toAddress, BigInteger amount, BigInteger gasPrice, BigInteger gasLimit, long nonce, byte[] data)
    {
        String dataStr = data != null ? Numeric.toHexString(data) : "";

        if (TextUtils.isEmpty(toAddress)) //This transaction is a constructor
        {
            return RawTransaction.createContractTransaction(
                    BigInteger.valueOf(nonce),
                    gasPrice,
                    gasLimit,
                    amount,
                    dataStr);
        }
        else
        {
            return RawTransaction.createTransaction(
                    BigInteger.valueOf(nonce),
                    gasPrice,
                    gasLimit,
                    toAddress,
                    amount,
                    dataStr);
        }
    }

    /**
     * Format an ERC1559 Transaction
     */
    private RawTransaction formatRawTransaction(String toAddress,
                                                long chainId,
                                                BigInteger amount,
                                                BigInteger gasLimit,
                                                BigInteger maxPriorityFeePerGas,
                                                BigInteger maxFeePerGas,
                                                long nonce,
                                                byte[] data)
    {
        String dataStr = data != null ? Numeric.toHexString(data) : "";

        return RawTransaction.createTransaction(
                chainId,
                BigInteger.valueOf(nonce),
                gasLimit,
                toAddress,
                amount,
                dataStr,
                maxPriorityFeePerGas,
                maxFeePerGas
        );
    }
}
