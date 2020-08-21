package com.alphawallet.app.walletconnect.entity

class InvalidHmacException : Exception("Invalid HMAC")
class InvalidJsonRpcParamsException(val requestId: Long) : Exception("Invalid JSON RPC Request")
