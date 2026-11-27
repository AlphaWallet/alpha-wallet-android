package com.alphawallet.app.repository.entity

import android.text.TextUtils
import com.alphawallet.app.entity.ContractType
import com.alphawallet.app.entity.tokens.TokenInfo
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.math.BigInteger

class RealmToken : RealmObject {
    @PrimaryKey
    var address: String = ""

    var name: String = ""
    var symbol: String = ""
    var decimals: Int = 0
    var addedTime: Long = 0
    var updatedTime: Long = 0
    var lastTxTime: Long = 0
    var balance: String = ""
        get() = if (TextUtils.isEmpty(field)) "0" else field
        set(value) {
            if (value == field) {
                return
            }
            updatedTime = System.currentTimeMillis()
            field = value
            addedTime = System.currentTimeMillis()
        }
    var isEnabled: Boolean = false
    var tokenId: Int = 0
    var interfaceSpec: Int = 0
    var auxData: String = ""
    var lastBlockRead: Long = 0
    var chainId: Long = 0
    var earliestTxBlock: Long = 0
    var visibilityChanged: Boolean = false
    var erc1155BlockRead: String = ""

    fun getTokenAddress(): String {
        val tAddress = address
        // Legacy base-chain keys: 0x + 40 hex + '.' + chainId
        val dotIndex = tAddress.indexOf('.')
        if (dotIndex == 42) {
            return tAddress.substring(0, dotIndex)
        }
        // Standard keys: {address}-{chainId} including native "eth-{chainId}"
        val dashIndex = tAddress.indexOf('-')
        if (dashIndex > 0) {
            return tAddress.substring(0, dashIndex)
        }
        return tAddress
    }

    fun getUpdateTime(): Long = addedTime

    fun setUpdateTime(addedTime: Long) {
        this.updatedTime = addedTime
    }

    fun getBalanceUpdateTime(): Long = updatedTime

    fun isVisibilityChanged(): Boolean = visibilityChanged

    fun getEnabled(): Boolean = isEnabled

    fun getContractType(): ContractType {
        var typeOrdinal = interfaceSpec
        if (typeOrdinal > ContractType.CREATION.ordinal) typeOrdinal = ContractType.NOT_SET.ordinal
        return ContractType.values()[typeOrdinal]
    }

    fun setLastBlock(lastBlockCheck: Long) {
        this.lastBlockRead = lastBlockCheck
    }

    fun getLastBlock(): Long = lastBlockRead

    fun getEarliestTransactionBlock(): Long = earliestTxBlock

    fun setEarliestTransactionBlock(earliestTransactionBlock: Long) {
        this.earliestTxBlock = earliestTransactionBlock
    }

    fun updateTokenInfoIfRequired(tokenInfo: TokenInfo) {
        if (tokenInfo.decimals != decimals && (tokenInfo.decimals > 0 && (decimals == 0 || decimals == 18))
            || (!TextUtils.isEmpty(tokenInfo.name) && tokenInfo.name != name)
            || (!TextUtils.isEmpty(tokenInfo.symbol) && tokenInfo.symbol != symbol)
        ) {
            name = tokenInfo.name
            symbol = tokenInfo.symbol
            decimals = tokenInfo.decimals
        }

        if (!isEnabled && tokenInfo.isEnabled) {
            isEnabled = true
            visibilityChanged = false
        }
    }

    fun readErc1155BlockRead(): BigInteger {
        return if (erc1155BlockRead.isNotEmpty()) {
            BigInteger(erc1155BlockRead, Character.MAX_RADIX)
        } else {
            BigInteger.ZERO
        }
    }

    fun writeErc1155BlockRead(erc1155BlockRead: BigInteger) {
        this.erc1155BlockRead = erc1155BlockRead.toString(Character.MAX_RADIX)
    }
}
