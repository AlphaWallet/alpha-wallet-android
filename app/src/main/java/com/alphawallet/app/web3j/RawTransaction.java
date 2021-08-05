package com.alphawallet.app.web3j;

import com.alphawallet.app.web3j.transactiontype.ITransaction;
import com.alphawallet.app.web3j.transactiontype.LegacyTransaction;
import com.alphawallet.app.web3j.transactiontype.Transaction1559;
import com.alphawallet.app.web3j.transactiontype.TransactionType;

import java.math.BigInteger;

/**
 * Created by JB on 5/08/2021.
 */
public class RawTransaction
{
    private final ITransaction transaction;

    protected RawTransaction(final ITransaction transaction) {
        this.transaction = transaction;
    }

    protected RawTransaction(
            BigInteger nonce,
            BigInteger gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data) {
        this(new LegacyTransaction(nonce, gasPrice, gasLimit, to, value, data));
    }

    public static RawTransaction createContractTransaction(
            BigInteger nonce,
            BigInteger gasPrice,
            BigInteger gasLimit,
            BigInteger value,
            String init) {
        return new RawTransaction(
                LegacyTransaction.createContractTransaction(
                        nonce, gasPrice, gasLimit, value, init));
    }

    public static RawTransaction createEtherTransaction(
            BigInteger nonce,
            BigInteger gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger value) {

        return new RawTransaction(
                LegacyTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, to, value));
    }

    public static RawTransaction createEtherTransaction(
            long chainId,
            BigInteger nonce,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas) {
        return new RawTransaction(
                Transaction1559.createEtherTransaction(
                        chainId, nonce, gasLimit, to, value, maxPriorityFeePerGas, maxFeePerGas));
    }

    public static RawTransaction createTransaction(
            BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to, String data) {
        return createTransaction(nonce, gasPrice, gasLimit, to, BigInteger.ZERO, data);
    }

    public static RawTransaction createTransaction(
            BigInteger nonce,
            BigInteger gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data) {

        return new RawTransaction(
                LegacyTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data));
    }

    public static RawTransaction createTransaction(
            long chainId,
            BigInteger nonce,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas) {

        return new RawTransaction(
                Transaction1559.createTransaction(
                        chainId,
                        nonce,
                        gasLimit,
                        to,
                        value,
                        data,
                        maxPriorityFeePerGas,
                        maxFeePerGas));
    }

    public BigInteger getNonce() {
        return transaction.getNonce();
    }

    public BigInteger getGasPrice() {
        return transaction.getGasPrice();
    }

    public BigInteger getGasLimit() {
        return transaction.getGasLimit();
    }

    public String getTo() {
        return transaction.getTo();
    }

    public BigInteger getValue() {
        return transaction.getValue();
    }

    public String getData() {
        return transaction.getData();
    }

    public TransactionType getType() {
        return transaction.getType();
    }

    public ITransaction getTransaction() {
        return transaction;
    }
}
