package com.alphawallet.app.widget

interface WalletConnectSheetCallback {
    fun onClickApprove(chainId: Long)
    fun onClickReject()
    fun onClickChainId()
}