package com.alphawallet.app.repository.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Created by James on 8/11/2018.
 * Stormbird in Singapore
 */
class RealmWalletData : RealmObject {
    @PrimaryKey
    var address: String = ""

    var ENSName: String = ""
    var balance: String = ""
    var name: String = ""
    var lastWarning: Long = 0
        set(value) {
            field = value and DISMISS_WARNING_IN_SETTINGS_MASK
        }
    var ENSAvatar: String = ""

    fun getIsDismissedInSettings(): Boolean = (lastWarning and 0x1L) == 1L

    fun setIsDismissedInSettings(isDismissed: Boolean) {
        lastWarning = (lastWarning and DISMISS_WARNING_IN_SETTINGS_MASK) + if (isDismissed) 1L else 0L
    }

    companion object {
        private const val DISMISS_WARNING_IN_SETTINGS_MASK = 0xFFFFFFFE
    }
}
