package com.alphawallet.shadows

import com.walletconnect.walletconnectv2.client.WalletConnect
import com.walletconnect.walletconnectv2.client.WalletConnectClient
import com.walletconnect.walletconnectv2.core.exceptions.WalletConnectException
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(WalletConnectClient::class)
class ShadowWalletConnectClient {
    constructor() {}

    @Implementation
    @Throws(IllegalStateException::class)
    fun setWalletDelegate(delegate: WalletConnectClient.WalletDelegate) {}

    @Implementation
    fun initialize(
        initial: WalletConnect.Params.Init,
        onError: (WalletConnectException) -> Unit = {}
    ) = with(initial) {

    }
}