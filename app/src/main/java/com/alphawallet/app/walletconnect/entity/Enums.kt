package com.alphawallet.app.walletconnect.entity

import com.google.gson.annotations.SerializedName

enum class MessageType {
    @SerializedName("pub")
    PUB,
    @SerializedName("sub")
    SUB
}

enum class WCMethod {
    @SerializedName("wc_sessionRequest")
    SESSION_REQUEST,

    @SerializedName("wc_sessionUpdate")
    SESSION_UPDATE,

    @SerializedName("eth_sign")
    ETH_SIGN,

    @SerializedName("personal_sign")
    ETH_PERSONAL_SIGN,

    @SerializedName("eth_signTypedData")
    ETH_SIGN_TYPE_DATA,

    @SerializedName("eth_signTypedData_v3")
    ETH_SIGN_TYPE_DATA_V3,

    @SerializedName("eth_signTypedData_v4")
    ETH_SIGN_TYPE_DATA_V4,

    @SerializedName("eth_signTransaction")
    ETH_SIGN_TRANSACTION,

    @SerializedName("eth_sendTransaction")
    ETH_SEND_TRANSACTION,

    @SerializedName("get_accounts")
    GET_ACCOUNTS,

    @SerializedName("wallet_switchEthereumChain")
    SWITCH_ETHEREUM_CHAIN,

    @SerializedName("wallet_addEthereumChain")
    ADD_ETHEREUM_CHAIN,

}