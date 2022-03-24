package com.alphawallet.app.walletconnect.entity

import com.alphawallet.app.web3.entity.NativeCurrency
import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject

data class WCSessionRequest(
    val peerId: String,
    val peerMeta: WCPeerMeta,
    val chainId: String?
)

data class WCApproveSessionResponse(
    val approved: Boolean = true,
    val chainId: Long,
    val accounts: List<String>,
    val peerId: String?,
    val peerMeta: WCPeerMeta?
)

data class WCSessionUpdate(
    val approved: Boolean,
    val chainId: Long?,
    val accounts: List<String>?
)

data class WCEncryptionPayload(
    val data: String,
    val hmac: String,
    val iv: String
)

data class WCSocketMessage(
    val topic: String,
    val type: MessageType,
    val payload: String
)

data class WCPeerMeta(
    val name: String,
    val url: String,
    val description: String? = null,
    val icons: List<String> = listOf("")
)

data class WCSwitchEthChain(
    val chainId: String
)

data class WCAddEthChain(
    val chainId: String,
    val chainName: String,
    val nativeCurrency: NativeCurrency,
    val rpcUrls: List<String>,
    val blockExplorerUrls: List<String>? = null,
    val iconUrls: List<String>? = null
) {
    fun toWalletAddEthereumObject(): WalletAddEthereumChainObject {
        val chainObject: WalletAddEthereumChainObject = WalletAddEthereumChainObject()
        chainObject.nativeCurrency = this.nativeCurrency
        chainObject.chainName = this.chainName
        chainObject.chainId = this.chainId
        chainObject.blockExplorerUrls = this.blockExplorerUrls?.toTypedArray()
        chainObject.rpcUrls = this.rpcUrls.toTypedArray()
        return chainObject
    }
}