@file:Suppress("UnusedPrivateMember")

package com.alphawallet.shadows

import com.walletconnect.android.internal.common.exception.WalletConnectException
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
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
