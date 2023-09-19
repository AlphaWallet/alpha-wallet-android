package com.alphawallet.app.entity.analytics;

/**
 * Created by JB on 12/01/2021.
 */
public enum ActionSheetMode
{
    SEND_TRANSACTION("Send Transaction"),
    SEND_TRANSACTION_DAPP("Send Transaction DApp"),
    SEND_TRANSACTION_WC("Send Transaction WalletConnect"),
    SIGN_MESSAGE("Sign Message"),
    SIGN_TRANSACTION("Sign Transaction"),
    SPEEDUP_TRANSACTION("Speed Up Transaction"),
    CANCEL_TRANSACTION("Cancel Transaction"),
    MESSAGE("Message"),
    WALLET_CONNECT_REQUEST("WalletConnect Request"),
    NODE_STATUS_INFO("Node Status Info");

    private final String mode;

    ActionSheetMode(String mode)
    {
        this.mode = mode;
    }

    public String getValue()
    {
        return mode;
    }
}
