package com.alphawallet.app.interact;


import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;

import android.util.Pair;

import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.MessagePair;
import com.alphawallet.app.entity.SignaturePair;
import com.alphawallet.app.entity.TransactionReturn;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.KeystoreAccountService;
import com.alphawallet.app.service.TransactionSendHandlerInterface;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.token.entity.Signable;

import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.transaction.type.Transaction1559;

import java.math.BigInteger;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CreateTransactionInteract
{
    private final TransactionRepositoryType transactionRepository;
    private final AnalyticsServiceType analyticsService;
    private TransactionSendHandlerInterface txInterface;
    private Disposable disposable;
    /**
     * Cache the nonce to avoid needing to recalculate it. Hardware blocks all functionality until success or cancel so it's safe to do this
     * NOTE: if the wallet is upgraded to sign multiple transactions simultaneously this would need to be looked at again
     */
    private long nonceForHardwareSign;

    public CreateTransactionInteract(TransactionRepositoryType transactionRepository,
                                     AnalyticsServiceType analyticsService)
    {
        this.transactionRepository = transactionRepository;
        this.analyticsService = analyticsService;
    }

    public Single<SignaturePair> sign(Wallet wallet, MessagePair messagePair)
    {
        return transactionRepository.getSignature(wallet, messagePair)
            .map(sig -> new SignaturePair(messagePair.selection, sig, messagePair.message));
    }

    public Single<SignatureFromKey> sign(Wallet wallet, Signable message)
    {
        return transactionRepository.getSignature(wallet, message);
    }

    public void requestSignature(Web3Transaction w3Tx, Wallet wallet, long chainId, TransactionSendHandlerInterface txInterface)
    {
        this.txInterface = txInterface;
        disposable = createWithSigId(wallet, w3Tx, chainId)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(signaturePackage -> completeSendTransaction(wallet, chainId, signaturePackage, w3Tx),
                error -> handleTransactionError(error, w3Tx));
    }

    private void handleTransactionError(Throwable error, Web3Transaction w3Tx)
    {
        txInterface.transactionError(new TransactionReturn(error, w3Tx));
        trackTransactionError(error.getMessage());
    }

    public void requestSignTransaction(Web3Transaction w3Tx, Wallet wallet, long chainId, TransactionSendHandlerInterface txInterface)
    {
        this.txInterface = txInterface;
        disposable = createWithSigId(wallet, w3Tx, chainId)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(signaturePackage -> completeSignTransaction(chainId, signaturePackage, w3Tx),
                error -> handleTransactionError(error, w3Tx));
    }

    public Single<Pair<SignatureFromKey, RawTransaction>> createWithSigId(Wallet from, Web3Transaction w3Tx, long chainId)
    {
        return transactionRepository.signTransaction(from, w3Tx, chainId)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Return from hardware sign
     *
     * @param wallet
     * @param chainId
     * @param w3Tx
     * @param sigData
     */
    public void sendTransaction(Wallet wallet, long chainId, Web3Transaction w3Tx, SignatureFromKey sigData)
    {
        RawTransaction rtx = transactionRepository.formatRawTransaction(w3Tx, nonceForHardwareSign, chainId);
        sendTransaction(wallet, chainId, rtx, sigData, w3Tx);
    }

    public void sendTransaction(Wallet wallet, long chainId, RawTransaction rtx, SignatureFromKey sigData, Web3Transaction w3Tx)
    {
        //format transaction
        disposable = transactionRepository.sendTransaction(wallet, rtx, rlpEncodeSignature(rtx, sigData, chainId), chainId)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                txHash ->
                {
                    txInterface.transactionFinalised(new TransactionReturn(txHash, w3Tx));

                    trackTransactionCount(chainId);
                    AnalyticsProperties props = new AnalyticsProperties();
                    props.put(Analytics.PROPS_TRANSACTION_TYPE, "send");
                    props.put(Analytics.PROPS_TRANSACTION_CHAIN_ID, String.valueOf(chainId));
                    analyticsService.track(Analytics.Navigation.ACTION_SHEET_FOR_TRANSACTION_CONFIRMATION_SUCCESSFUL.getValue(), props);
                },
                error -> handleTransactionError(error, w3Tx)
            );
    }

    public void signTransaction(long chainId, Web3Transaction w3Tx, SignatureFromKey sigData)
    {
        RawTransaction rtx = transactionRepository.formatRawTransaction(w3Tx, nonceForHardwareSign, chainId);
        txInterface.transactionSigned(rlpEncodeSignature(rtx, sigData, chainId), w3Tx);

        trackTransactionCount(chainId);
        AnalyticsProperties props = new AnalyticsProperties();
        props.put(Analytics.PROPS_TRANSACTION_TYPE, "sign");
        props.put(Analytics.PROPS_TRANSACTION_CHAIN_ID, String.valueOf(chainId));
        analyticsService.track(Analytics.Navigation.ACTION_SHEET_FOR_TRANSACTION_CONFIRMATION_SUCCESSFUL.getValue(), props);
    }

    public Single<String> resend(Wallet from, BigInteger nonce, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, long chainId)
    {
        return transactionRepository.resendTransaction(from, to, subunitAmount, nonce, gasPrice, gasLimit, data, chainId)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread());
    }

    private void completeSendTransaction(Wallet wallet, long chainId, Pair<SignatureFromKey, RawTransaction> signaturePackage, Web3Transaction w3Tx)
    {
        switch (signaturePackage.first.sigType)
        {
            case SIGNATURE_GENERATED:
                sendTransaction(wallet, chainId, signaturePackage.second, signaturePackage.first, w3Tx);
                break;
            case SIGNING_POSTPONED:
                //record nonce
                nonceForHardwareSign = signaturePackage.second.getNonce().longValue();
                break;
            case KEY_FILE_ERROR:
            case KEY_AUTHENTICATION_ERROR:
            case KEY_CIPHER_ERROR:
                String errorMessage = signaturePackage.first.failMessage;
                handleTransactionError(new Throwable(errorMessage), w3Tx);
                break;
            default:
                String message = "Unimplemented sign type";
                trackTransactionError(message);
                throw new RuntimeException(message);
        }
    }

    private void completeSignTransaction(long chainId, Pair<SignatureFromKey, RawTransaction> signaturePackage, Web3Transaction w3Tx)
    {
        switch (signaturePackage.first.sigType)
        {
            case SIGNATURE_GENERATED:
                signTransaction(chainId, w3Tx, signaturePackage.first);
                break;
            case SIGNING_POSTPONED:
                //record nonce
                nonceForHardwareSign = signaturePackage.second.getNonce().longValue();
                break;
            case KEY_FILE_ERROR:
            case KEY_AUTHENTICATION_ERROR:
            case KEY_CIPHER_ERROR:
                String errorMessage = signaturePackage.first.failMessage;
                handleTransactionError(new Throwable(errorMessage), w3Tx);
                break;
            default:
                String message = "Unimplemented sign type";
                trackTransactionError(message);
                throw new RuntimeException(message);
        }
    }

    private SignatureFromKey rlpEncodeSignature(RawTransaction rtx, SignatureFromKey sigData, long chainId)
    {
        if (rtx.getTransaction() instanceof Transaction1559)
        {
            sigData.signature = KeystoreAccountService.encode(rtx, sigFromByteArray(sigData.signature));
        }
        else
        {
            Sign.SignatureData sig = TransactionEncoder.createEip155SignatureData(sigFromByteArray(sigData.signature), chainId);
            sigData.signature = KeystoreAccountService.encode(rtx, sig);
        }

        return sigData;
    }

    private void trackTransactionError(String message)
    {
        AnalyticsProperties props = new AnalyticsProperties();
        props.put(Analytics.PROPS_ERROR_MESSAGE, message);
        analyticsService.track(Analytics.Navigation.ACTION_SHEET_FOR_TRANSACTION_CONFIRMATION_FAILED.getValue());
    }

    private void trackTransactionCount(long chainId)
    {
        analyticsService.increment(EthereumNetworkRepository.hasRealValue(chainId) ?
            Analytics.UserProperties.TRANSACTION_COUNT.getValue() :
            Analytics.UserProperties.TRANSACTION_COUNT_TESTNET.getValue());
    }
}
