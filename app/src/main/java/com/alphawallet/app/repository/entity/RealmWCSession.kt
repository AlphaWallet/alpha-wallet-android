package com.alphawallet.app.repository.entity

import com.alphawallet.app.walletconnect.WCSession
import com.alphawallet.app.walletconnect.entity.WCPeerMeta
import com.google.gson.Gson
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Created by JB on 4/09/2020.
 */
class RealmWCSession : RealmObject {
    @PrimaryKey
    var sessionId: String = ""

    var peerId: String = ""
    var sessionData: String = ""
    @PersistedName("remotePeerData")
    var remotePeerDataJson: String = ""
    var remotePeerId: String = ""
    var usageCount: Int = 0
    var lastUsageTime: Long = 0
    var walletAccount: String = ""
    var chainId: Long = 0

    fun getSession(): WCSession = Gson().fromJson(sessionData, WCSession::class.java)

    fun setRemotePeerData(peerData: String) {
        remotePeerDataJson = peerData
    }

    fun getRemotePeerData(): WCPeerMeta = Gson().fromJson(remotePeerDataJson, WCPeerMeta::class.java)
}
