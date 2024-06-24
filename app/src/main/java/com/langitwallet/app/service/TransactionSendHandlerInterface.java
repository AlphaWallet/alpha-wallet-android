package com.langitwallet.app.service;

import com.alphawallet.hardware.SignatureFromKey;
import com.langitwallet.app.entity.TransactionReturn;
import com.langitwallet.app.web3.entity.Web3Transaction;

/**
 * Created by JB on 2/02/2023.
 */
public interface TransactionSendHandlerInterface
{
    void transactionFinalised(TransactionReturn txData);

    void transactionError(TransactionReturn txError);

    default void transactionSigned(SignatureFromKey sigData, Web3Transaction w3Tx)
    {
    } //Not always required, only WalletConnect
}
