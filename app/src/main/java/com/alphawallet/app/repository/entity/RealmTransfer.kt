package com.alphawallet.app.repository.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PersistedName

/**
 * Created by JB on 17/12/2020.
 */
class RealmTransfer : RealmObject {
    @PersistedName("hash")
    var hashKey: String = ""
    var tokenAddress: String = ""
    var eventName: String = ""
    var transferDetail: String = ""

    fun getHash(): String {
        val split = hashKey.split("-")
        return if (split.isNotEmpty()) split[0] else ""
    }

    fun setHashKey(chainId: Long, hash: String) {
        this.hashKey = databaseKey(chainId, hash)
    }

    fun getChain(): Long {
        val split = hashKey.split("-")
        return if (split.size > 1) split[1].toLong() else 0L
    }

    companion object {
        @JvmStatic
        fun databaseKey(chainId: Long, hash: String): String {
            return "$hash-$chainId"
        }
    }
}
