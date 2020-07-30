package com.alphawallet.app.walletconnect.entity

data class WCSessionRequest(
        val peerId: String,
        val peerMeta: WCPeerMeta,
        val chainId: String?
)

data class WCApproveSessionResponse(
        val approved: Boolean = true,
        val chainId: Int,
        val accounts: List<String>,
        val peerId: String?,
        val peerMeta: WCPeerMeta?
)

data class WCSessionUpdate(
        val approved: Boolean,
        val chainId: Int?,
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

data class WCPeerMeta (
        val name: String,
        val url: String,
        val description: String? = null,
        val icons: List<String> = listOf("")
)