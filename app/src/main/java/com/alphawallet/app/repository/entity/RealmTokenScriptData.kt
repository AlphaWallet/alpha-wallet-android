package com.alphawallet.app.repository.entity

import android.text.TextUtils
import com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Created by JB on 17/08/2020.
 */
class RealmTokenScriptData : RealmObject {
    @PrimaryKey
    var instanceKey: String = ""

    var fileHash: String = ""
    var filePath: String = ""
    var names: String = ""
    @PersistedName("viewList")
    var viewListCsv: String = ""
    var ipfsPath: String = ""
    var hasEvents: Boolean = false
    var schemaUID: String = ""

    fun getChainId(): Long {
        val chainId = instanceKey.split("-")[1]
        return if (Character.isDigit(chainId[0])) chainId.toLong() else MAINNET_ID
    }

    fun getOriginTokenAddress(): String = instanceKey.split("-")[0]

    fun getName(count: Int): String? {
        val nameMap = getValueMap(names)
        var value: String? = null
        when (count) {
            1 -> if (nameMap.containsKey("one")) value = nameMap["one"]
            else -> {
                value = nameMap["other"]
                if (value == null) value = nameMap["two"]
                if (value == null) value = getName(1)
            }
        }

        if (value == null && nameMap.values.isNotEmpty()) {
            value = nameMap.values.iterator().next()
        }

        return value
    }

    private fun getValueMap(values: String): Map<String, String> {
        val nameMap = HashMap<String, String>()
        if (TextUtils.isEmpty(values)) return nameMap

        val nameList = values.split(",")
        var state = true
        var key: String? = null
        for (s in nameList) {
            if (state) {
                key = s
                state = false
            } else {
                nameMap[key!!] = s
                state = true
            }
        }

        return nameMap
    }

    fun setViewList(viewList: String) {
        this.viewListCsv = viewList
    }

    fun getViewList(): List<String> {
        val viewNames = ArrayList<String>()
        if (TextUtils.isEmpty(viewListCsv)) return viewNames

        val views = viewListCsv.split(",")
        viewNames.addAll(views)
        return viewNames
    }
}
