package com.alphawallet.app.repository.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import io.realm.kotlin.types.annotations.PersistedName

class RealmTokenTicker : RealmObject {
    @PrimaryKey
    @PersistedName("contract")
    var contractKey: String = ""

    var price: String = ""
    var percentChange24h: String = ""
    var createdTime: Long = 0
    var id: String = ""
    var image: String = ""
    var updatedTime: Long = 0
    var currencySymbol: String = ""

    fun getContract(): String = contractKey.split("-")[0]

    fun getChain(): Long = contractKey.split("-")[1].toLong()
}
