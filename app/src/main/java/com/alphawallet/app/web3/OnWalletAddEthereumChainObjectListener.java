package com.alphawallet.app.web3;

import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject;

public interface OnWalletAddEthereumChainObjectListener
{
    void onWalletAddEthereumChainObject(long callbackId, WalletAddEthereumChainObject chainObject);
}
