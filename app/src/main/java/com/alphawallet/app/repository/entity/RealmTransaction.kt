package com.alphawallet.app.repository.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class RealmTransaction : RealmObject {
    @PrimaryKey
    var hash: String = ""

    var blockNumber: String = ""
    var timeStamp: Long = 0
    var nonce: Int = 0
    var from: String = ""
    var to: String = ""
    var value: String = ""
    var gas: String = ""
    var gasPrice: String = ""
    var gasUsed: String = ""
    var input: String = ""
    var error: String = ""
    var maxFeePerGas: String = ""
    var maxPriorityFee: String = ""
    var chainId: Long = 0
    var expectedCompletion: Long = 0
    var contractAddress: String = ""

    fun getPriorityFee(): String = maxPriorityFee

    fun isPending(): Boolean {
        return blockNumber.isEmpty() || blockNumber == "0" || blockNumber == "-2"
    }
}
