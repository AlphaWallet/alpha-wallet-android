package com.alphawallet.app.interact;


import android.text.TextUtils;

import com.alphawallet.app.entity.MessagePair;
import com.alphawallet.app.entity.SignaturePair;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.tools.Numeric;

import java.math.BigInteger;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class CreateTransactionInteract
{
    private final TransactionRepositoryType transactionRepository;

    public CreateTransactionInteract(TransactionRepositoryType transactionRepository)
    {
        this.transactionRepository = transactionRepository;
    }

    public Single<SignaturePair> sign(Wallet wallet, MessagePair messagePair, long chainId)
    {
        return transactionRepository.getSignature(wallet, messagePair, chainId)
                .map(sig -> new SignaturePair(messagePair.selection, sig.signature, messagePair.message));
    }

    public Single<SignatureFromKey> sign(Wallet wallet, Signable message, long chainId)
    {
        return transactionRepository.getSignature(wallet, message, chainId);
    }

    public Single<String> create(Wallet from, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, long chainId)
    {
        return transactionRepository.createTransactionWithSig(from, to, subunitAmount,
                gasPrice, gasLimit, -1,
                data, chainId)
                .map(txData -> txData.txHash)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<TransactionData> createWithSig(Wallet from, Web3Transaction web3Tx, long chainId)
    {
        if (web3Tx.isLegacyTransaction())
        {
            return transactionRepository.createTransactionWithSig(from, web3Tx.recipient.toString(), web3Tx.value,
                    web3Tx.gasPrice, web3Tx.gasLimit, web3Tx.nonce,
                    !TextUtils.isEmpty(web3Tx.payload) ? Numeric.hexStringToByteArray(web3Tx.payload) : new byte[0], chainId)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread());
        }
        else
        {
            return create1559WithSig(from, web3Tx, chainId);
        }
    }

    /**
     * Used for LiFi Swap.
     * This uses the `contract` from Web3Transaction instead of `recipient`.
     * TODO: Consolidate with createWithSig()
     */
    public Single<TransactionData> createWithSig2(Wallet from, Web3Transaction web3Tx, long chainId)
    {
        return transactionRepository.createTransactionWithSig(
                from,
                web3Tx.contract.toString(),
                web3Tx.value,
                web3Tx.gasPrice,
                web3Tx.gasLimit,
                web3Tx.nonce,
                !TextUtils.isEmpty(web3Tx.payload) ? Numeric.hexStringToByteArray(web3Tx.payload) : new byte[0],
                chainId
        )
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<TransactionData> create1559WithSig(Wallet from, Web3Transaction web3Tx, long chainId)
    {
        return transactionRepository.create1559TransactionWithSig(from, web3Tx.recipient.toString(), web3Tx.value, web3Tx.gasLimit,
                web3Tx.maxFeePerGas, web3Tx.maxPriorityFeePerGas, web3Tx.nonce,
                !TextUtils.isEmpty(web3Tx.payload) ? Numeric.hexStringToByteArray(web3Tx.payload) : new byte[0], chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<TransactionData> createWithSig(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, long chainId)
    {
        return transactionRepository.createTransactionWithSig(from, "", BigInteger.ZERO,
                gasPrice, gasLimit, -1,
                !TextUtils.isEmpty(data) ? Numeric.hexStringToByteArray(data) : new byte[0], chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> resend(Wallet from, BigInteger nonce, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, long chainId)
    {
        return transactionRepository.resendTransaction(from, to, subunitAmount, nonce, gasPrice, gasLimit, data, chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<TransactionData> signTransaction(Wallet from, Web3Transaction web3Tx, long chainId)
    {
        return transactionRepository.getSignatureForTransaction(from, web3Tx.recipient.toString(), web3Tx.value,
                web3Tx.gasPrice, web3Tx.gasLimit, web3Tx.nonce,
                !TextUtils.isEmpty(web3Tx.payload) ? Numeric.hexStringToByteArray(web3Tx.payload) : new byte[0], chainId);
    }
}