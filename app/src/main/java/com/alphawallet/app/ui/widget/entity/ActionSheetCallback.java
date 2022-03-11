package com.alphawallet.app.ui.widget.entity;

import android.app.Activity;

import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.alphawallet.app.web3.entity.Web3Transaction;

/**
 * Created by JB on 27/11/2020.
 */
public interface ActionSheetCallback
{
    void getAuthorisation(SignAuthenticationCallback callback);
    void sendTransaction(Web3Transaction tx);
    void dismissed(String txHash, long callbackId, boolean actionCompleted);
    void notifyConfirm(String mode);
    default void signTransaction(Web3Transaction tx) { } // only WalletConnect uses this so far

    default void buttonClick(long callbackId, Token baseToken) { }; //for message only actionsheet

    default void notifyWalletConnectApproval(long chainId) { };    // used by WalletConnectRequest
    default void denyWalletConnect() { };
    default void openChainSelection() { };      // used by WalletConnectRequest
}
