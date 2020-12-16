package com.alphawallet.app.walletconnect.entity

import androidx.annotation.Keep

private const val JSONRPC_VERSION = "2.0"

@Keep
data class JsonRpcRequest<T>(
        val id: Long,
        val jsonrpc: String = JSONRPC_VERSION,
        val method: WCMethod?,
        val params: T
)

@Keep
data class JsonRpcResponse<T>(
        val jsonrpc: String = JSONRPC_VERSION,
        val id: Long,
        val result: T
)

@Keep
data class JsonRpcErrorResponse(
        val jsonrpc: String = JSONRPC_VERSION,
        val id: Long,
        val error: JsonRpcError
)

@Keep
data class JsonRpcError(
        val code: Int,
        val message: String
) {
    companion object {
        fun serverError(message: String) = JsonRpcError(-32000, message)
        fun invalidParams(message: String) = JsonRpcError(-32602, message)
        fun invalidRequest(message: String) = JsonRpcError(-32600, message)
        fun parseError(message: String) = JsonRpcError(-32700, message)
        fun methodNotFound(message: String) = JsonRpcError(-32601, message)
    }
}