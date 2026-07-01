package com.alphawallet.app.repository.entity

import com.alphawallet.app.entity.tokens.Token
import com.alphawallet.app.repository.TokensRealmSource
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Created by JB on 10/07/2021.
 */
class RealmNFTAsset : RealmObject {
    @PrimaryKey
    var tokenIdAddr: String = ""

    var metaData: String = ""
    @get:JvmName("getBalanceStr")
    @set:JvmName("setBalanceStr")
    var balance: String = ""

    fun getTokenId(): String {
        val str = tokenIdAddr.split("-")
        return str[str.size - 1]
    }

    fun setBalance(balance: BigDecimal) {
        this.balance = balance.toString()
    }

    fun getBalance(): BigDecimal {
        return if (balance.isNotEmpty()) {
            BigDecimal(balance)
        } else {
            BigDecimal.ZERO
        }
    }

    companion object {
        @JvmStatic
        fun databaseKey(token: Token, tokenId: BigInteger): String {
            return TokensRealmSource.databaseKey(token) + "-" + tokenId.toString()
        }
    }
}
