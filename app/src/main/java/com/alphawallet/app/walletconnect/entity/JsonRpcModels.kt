package com.alphawallet.app.walletconnect.entity

private const val JSONRPC_VERSION = "2.0"

data class JsonRpcRequest<T>(
        val id: Long,
        val jsonrpc: String = JSONRPC_VERSION,
        val method: WCMethod?,
        val params: T
)

data class JsonRpcResponse<T>(
        val jsonrpc: String = JSONRPC_VERSION,
        val id: Long,
        val result: T
)

data class JsonRpcErrorResponse(
        val jsonrpc: String = JSONRPC_VERSION,
        val id: Long,
        val error: JsonRpcError
)

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