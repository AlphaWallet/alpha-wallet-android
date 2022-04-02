package com.alphawallet.app.walletconnect.entity

private const val JSONRPC_VERSION = "2.0"
private const val SERVER_ERROR = -32000;
private const val INVALID_PARAMS = -32602;
private const val INVALID_REQUEST = -32600;
private const val PARSE_ERROR = -32700;
private const val NOT_FOUND = -32601;
private const val UNRECOGNISED = 4902;

data class JsonRpcRequest<T>(
        val id: Long,
        val jsonrpc: String = JSONRPC_VERSION,
        val method: WCMethod?,
        val params: T
)

data class JsonRpcResponse<T>(
        val jsonrpc: String = JSONRPC_VERSION,
        val id: Long,
        val result: T?
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
        fun serverError(message: String) = JsonRpcError(SERVER_ERROR, message)
        fun invalidParams(message: String) = JsonRpcError(INVALID_PARAMS, message)
        fun invalidRequest(message: String) = JsonRpcError(INVALID_REQUEST, message)
        fun parseError(message: String) = JsonRpcError(PARSE_ERROR, message)
        fun methodNotFound(message: String) = JsonRpcError(NOT_FOUND, message)
        fun unrecognisedChain(message: String) = JsonRpcError(UNRECOGNISED, message)
    }
}