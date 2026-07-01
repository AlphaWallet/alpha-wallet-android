package com.alphawallet.app.repository.entity

import com.alphawallet.app.entity.EIP1559FeeOracleResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Created by JB on 19/01/2022.
 */
class Realm1559Gas : RealmObject {
    @PrimaryKey
    var chainId: Long = 0

    var timeStamp: Long = 0
    var resultData: String = "" // JSON format string

    fun getResult(): Map<Int, EIP1559FeeOracleResult> {
        val entry = object : TypeToken<Map<Int, EIP1559FeeOracleResult>>() {}.type
        return Gson().fromJson(resultData, entry)
    }

    fun setResultData(result: Map<Int, EIP1559FeeOracleResult>, ts: Long) {
        resultData = Gson().toJson(result)
        timeStamp = ts
    }
}
