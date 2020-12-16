package com.alphawallet.app.walletconnect.entity

import androidx.annotation.Keep

@Keep
data class WCSessionRequest(
        val peerId: String,
        val peerMeta: WCPeerMeta,
        val chainId: String?
)

@Keep
data class WCApproveSessionResponse(
        val approved: Boolean = true,
        val chainId: Int,
        val accounts: List<String>,
        val peerId: String?,
        val peerMeta: WCPeerMeta?
)

@Keep
data class WCSessionUpdate(
        val approved: Boolean,
        val chainId: Int?,
        val accounts: List<String>?
)

@Keep
data class WCEncryptionPayload(
        val data: String,
        val hmac: String,
        val iv: String
)

@Keep
data class WCSocketMessage(
        val topic: String,
        val type: MessageType,
        val payload: String
)

@Keep
data class WCPeerMeta (
        val name: String,
        val url: String,
        val description: String? = null,
        val icons: List<String> = listOf("")
)