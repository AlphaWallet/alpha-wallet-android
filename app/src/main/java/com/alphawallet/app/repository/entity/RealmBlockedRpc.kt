package com.alphawallet.app.repository.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Records RPC domains blocked by the local router/ISP for a given external IP.
 * Keyed by external IP + domain so a block on one network does not affect another.
 */
class RealmBlockedRpc : RealmObject {
    @PrimaryKey
    var key: String = ""

    var externalIp: String = ""
    var domain: String = ""
    var reason: String = ""
    var timeStamp: Long = 0

    companion object {
        @JvmStatic
        fun makeKey(externalIp: String, domain: String): String {
            return "${externalIp.lowercase()}|${domain.lowercase()}"
        }
    }
}
