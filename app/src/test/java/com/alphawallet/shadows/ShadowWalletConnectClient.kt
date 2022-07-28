package com.alphawallet.shadows

import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import com.walletconnect.sign.core.exceptions.client.WalletConnectException
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(SignClient::class)
class ShadowWalletConnectClient {
    constructor() {}

    @Implementation
    @Throws(IllegalStateException::class)
    fun setWalletDelegate(delegate: SignClient.WalletDelegate) {}

    @Implementation
    fun initialize(
        initial: Sign.Params.Init,
        onError: (WalletConnectException) -> Unit = {}
    ) = with(initial) {

    }
}