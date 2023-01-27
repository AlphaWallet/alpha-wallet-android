package com.alphawallet.app.interact;


import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;

import android.util.Pair;

import com.alphawallet.app.entity.MessagePair;
import com.alphawallet.app.entity.SignaturePair;
import com.alphawallet.app.entity.TransactionReturn;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.TransactionRepositoryType;
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
    private TransactionSendHandlerInterface txInterface;
    private Disposable disposable;

    public CreateTransactionInteract(TransactionRepositoryType transactionRepository)
    {
        this.transactionRepository = transactionRepository;
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
                        error -> txInterface.transactionError(new TransactionReturn(error, w3Tx)));
    }

    public void requestSignatureOnly(Web3Transaction w3Tx, Wallet wallet, long chainId, TransactionSendHandlerInterface txInterface)
    {
        this.txInterface = txInterface;
        disposable = createWithSigId(wallet, w3Tx, chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(signaturePackage -> txInterface.transactionSigned(signaturePackage.first, w3Tx),
                        error -> txInterface.transactionError(new TransactionReturn(error, w3Tx)));
    }

    public Single<Pair<SignatureFromKey, RawTransaction>> createWithSigId(Wallet from, Web3Transaction w3Tx, long chainId)
    {
        return transactionRepository.signTransaction(from, w3Tx, chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Cache the nonce to avoid needing to recalculate it. Hardware blocks all functionality until success or cancel so it's safe to do this
     * NOTE: if the wallet is upgraded to sign multiple transactions simultaneously this would need to be looked at again
     */
    private long nonceForHardwareSign;

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
                txInterface.transactionError(new TransactionReturn(new Throwable(signaturePackage.first.failMessage), w3Tx));
                break;
        }
    }

    /**
     * Return from hardware sign
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
        if (rtx.getTransaction() instanceof Transaction1559)
        {
            sigData.signature = KeystoreAccountService.encode(rtx, sigFromByteArray(sigData.signature));
        }
        else
        {
            Sign.SignatureData sig = TransactionEncoder.createEip155SignatureData(sigFromByteArray(sigData.signature), chainId);
            sigData.signature = KeystoreAccountService.encode(rtx, sig);
        }

        disposable = transactionRepository.sendTransaction(wallet, rtx, sigData, chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(txHash -> txInterface.transactionFinalised(new TransactionReturn(txHash, w3Tx)),
                        error -> txInterface.transactionError(new TransactionReturn(error, w3Tx)));
    }

    public Single<String> resend(Wallet from, BigInteger nonce, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, long chainId)
    {
        return transactionRepository.resendTransaction(from, to, subunitAmount, nonce, gasPrice, gasLimit, data, chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
