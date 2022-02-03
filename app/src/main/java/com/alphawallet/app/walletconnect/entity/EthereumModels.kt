package com.alphawallet.app.walletconnect.entity

import com.github.salomonbrys.kotson.jsonDeserializer

data class WCEthereumSignMessage(
        val raw: List<String>,
        val type: WCSignType
) {
    enum class WCSignType {
        MESSAGE, PERSONAL_MESSAGE, TYPED_MESSAGE
    }

    val data
        get() = when (type) {
            WCSignType.MESSAGE -> raw[1]
            WCSignType.TYPED_MESSAGE -> raw[1]
            WCSignType.PERSONAL_MESSAGE -> raw[0]
        }
}

data class WCEthereumTransaction(
        val from: String,
        val to: String?,
        val nonce: String?,
        val gasPrice: String?,
        val maxFeePerGas: String?,
        val maxPriorityFeePerGas: String?,
        val gas: String?,
        val gasLimit: String?,
        val value: String?,
        val data: String
)

val ethTransactionSerializer = jsonDeserializer<List<WCEthereumTransaction>> {
    val array = mutableListOf<WCEthereumTransaction>()
    it.json.asJsonArray.forEach { tx ->
        if (tx.isJsonObject) {
            array.add(it.context.deserialize(tx))
        }
    }
    array
}