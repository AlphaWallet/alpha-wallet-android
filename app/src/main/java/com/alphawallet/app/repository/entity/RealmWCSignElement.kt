package com.alphawallet.app.repository.entity

import io.realm.kotlin.types.RealmObject
import java.nio.charset.StandardCharsets

/**
 * Created by JB on 8/09/2020.
 */
class RealmWCSignElement : RealmObject {
    var sessionId: String = ""
    @get:JvmName("getSignMessageBytes")
    @set:JvmName("setSignMessageBytes")
    var signMessage: ByteArray = ByteArray(0)
    var signTime: Long = 0
    var signType: String = ""

    fun getSignMessage(): CharSequence = String(signMessage, StandardCharsets.UTF_8)

    fun setSignMessage(msg: CharSequence) {
        signMessage = msg.toString().toByteArray(StandardCharsets.UTF_8)
    }
}
