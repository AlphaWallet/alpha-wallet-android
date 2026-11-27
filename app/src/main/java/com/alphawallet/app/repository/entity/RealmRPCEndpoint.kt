package com.alphawallet.app.repository.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Stores RPC endpoint data fetched from chainlist.org with ranking information.
 *
 * Ranking values:
 *   -1 = untested (freshly fetched or reset after weekly update)
 *    0 = non-responsive (failed during calibration)
 *   >0 = average response time in milliseconds (lower is better)
 *
 * A singleton record with key "__rpc_meta__" stores the lastRPCCheck timestamp.
 */
class RealmRPCEndpoint : RealmObject {
    @PrimaryKey
    var key: String = ""

    var chainId: Long = 0
    var url: String = ""
    var rank: Long = 0
    var lastRPCCheck: Long = 0

    companion object {
        @JvmField
        val META_KEY: String = "__rpc_meta__"

        @JvmStatic
        fun makeKey(chainId: Long, url: String): String {
            return "$chainId-$url"
        }
    }
}
