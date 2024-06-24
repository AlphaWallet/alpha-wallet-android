package com.langitwallet.app.ui.widget.entity;

import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.token.entity.Signable;
import com.langitwallet.app.entity.SignAuthenticationCallback;
import com.langitwallet.app.entity.WalletType;
import com.langitwallet.app.entity.tokens.Token;
import com.langitwallet.app.service.GasService;
import com.langitwallet.app.web3.entity.Web3Transaction;

import java.math.BigInteger;

/**
 * Created by JB on 27/11/2020.
 */
public interface ActionSheetCallback
{
    void getAuthorisation(SignAuthenticationCallback callback);

    void sendTransaction(Web3Transaction tx);

    //For Hardware wallet
    void completeSendTransaction(Web3Transaction tx, SignatureFromKey signature); //return from hardware signing

    default void completeSignTransaction(Web3Transaction tx, SignatureFromKey signature) //return from hardware signing - sign tx only
    {

    }

    void dismissed(String txHash, long callbackId, boolean actionCompleted);

    void notifyConfirm(String mode);

    ActivityResultLauncher<Intent> gasSelectLauncher();

    default void signTransaction(Web3Transaction tx)
    {
    } // only WalletConnect uses this so far

    default void buttonClick(long callbackId, Token baseToken)
    {
    }

    default void notifyWalletConnectApproval(long chainId)
    {
    }

    default void denyWalletConnect()
    {
    }

    default void openChainSelection()
    {
    }

    default void signingComplete(SignatureFromKey signature, Signable message)
    {
    }

    default void signingFailed(Throwable error, Signable message)
    {
    }

    WalletType getWalletType();

    default BigInteger getTokenId()
    {
        return BigInteger.ZERO;
    }

    GasService getGasService();
}
