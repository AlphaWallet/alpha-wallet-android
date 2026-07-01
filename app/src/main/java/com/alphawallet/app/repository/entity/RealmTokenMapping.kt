package com.alphawallet.app.repository.entity

import android.text.TextUtils
import com.alphawallet.app.entity.tokendata.TokenGroup
import com.alphawallet.token.entity.ContractAddress
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class RealmTokenMapping : RealmObject {
    @PrimaryKey
    var address: String = ""

    var base: String = ""
    var group: Int = 0

    fun getBase(): ContractAddress? {
        return if (!TextUtils.isEmpty(base)) {
            ContractAddress(base)
        } else {
            null
        }
    }

    fun getGroup(): TokenGroup {
        return if (group >= 0 && group < TokenGroup.values().size) {
            TokenGroup.values()[group]
        } else {
            TokenGroup.ASSET
        }
    }
}
