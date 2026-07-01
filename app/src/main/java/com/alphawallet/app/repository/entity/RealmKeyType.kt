package com.alphawallet.app.repository.entity

import com.alphawallet.app.entity.WalletType
import com.alphawallet.app.service.KeyService
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey

class RealmKeyType : RealmObject {
    @PrimaryKey
    var address: String = ""

    @get:JvmName("getTypeByte")
    @set:JvmName("setTypeByte")
    var type: Byte = 0
    @PersistedName("authLevel")
    var authLevelByte: Byte = 0
    var lastBackup: Long = 0
    var dateAdded: Long = 0
    var modulus: String = ""

    fun getType(): WalletType = WalletType.values()[type.toInt()]

    fun setType(type: WalletType) {
        this.type = type.ordinal.toByte()
    }

    fun getAuthLevel(): KeyService.AuthenticationLevel =
        KeyService.AuthenticationLevel.values()[authLevelByte.toInt()]

    fun setAuthLevel(authLevel: KeyService.AuthenticationLevel) {
        this.authLevelByte = authLevel.ordinal.toByte()
    }

    fun getKeyModulus(): String = modulus

    fun setKeyModulus(modulus: String) {
        this.modulus = modulus
    }
}
