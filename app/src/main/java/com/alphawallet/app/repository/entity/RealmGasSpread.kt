package com.alphawallet.app.repository.entity

import com.alphawallet.app.entity.GasPriceSpread
import com.alphawallet.app.entity.TXSpeed
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.math.BigInteger

/**
 * Created by JB on 19/11/2020.
 */
class RealmGasSpread : RealmObject {
    @PrimaryKey
    var chainId: Long = 0

    var rapid: String = ""
    var fast: String = ""
    var standard: String = ""
    var slow: String = ""
    var baseFee: String = ""
    var timeStamp: Long = 0

    fun setGasSpread(spread: GasPriceSpread, time: Long) {
        rapid = addGasPrice(TXSpeed.RAPID, spread)
        fast = addGasPrice(TXSpeed.FAST, spread)
        standard = addGasPrice(TXSpeed.STANDARD, spread)
        slow = addGasPrice(TXSpeed.SLOW, spread)
        timeStamp = time
    }

    private fun addGasPrice(speed: TXSpeed, spread: GasPriceSpread): String {
        return if (spread.getSelectedGasFee(speed) != null) {
            spread.getSelectedGasFee(speed).gasPrice.maxFeePerGas.toString()
        } else {
            "0"
        }
    }

    fun getGasFee(speed: TXSpeed): BigInteger {
        return try {
            when (speed) {
                TXSpeed.RAPID -> BigInteger(rapid)
                TXSpeed.FAST -> BigInteger(fast)
                TXSpeed.STANDARD -> BigInteger(standard)
                TXSpeed.SLOW -> BigInteger(slow)
                TXSpeed.CUSTOM -> BigInteger.ZERO
            }
        } catch (e: Exception) {
            BigInteger.ZERO
        }
    }

    fun getGasFees(): Map<TXSpeed, BigInteger> {
        var slowGas = slow
        if (slow.contains(",")) {
            val gasBreakdown = slow.split(",")
            slowGas = gasBreakdown[0]
        }

        val fees = HashMap<TXSpeed, BigInteger>()
        fees[TXSpeed.RAPID] = BigInteger(rapid)
        fees[TXSpeed.FAST] = BigInteger(fast)
        fees[TXSpeed.STANDARD] = BigInteger(standard)
        fees[TXSpeed.SLOW] = BigInteger(slowGas)
        return fees
    }

    fun getGasPrice(): BigInteger {
        var gasPrice = getGasFee(TXSpeed.STANDARD)
        if (gasPrice > BigInteger.ZERO) return gasPrice

        for (txs in TXSpeed.values()) {
            gasPrice = getGasFee(txs)
            if (gasPrice > BigInteger.ZERO) return gasPrice
        }

        return BigInteger.ZERO
    }

    fun isLocked(): Boolean {
        var gasLocked = false
        if (slow.contains(",")) {
            val gasBreakdown = slow.split(",")
            gasLocked = gasBreakdown[1][0] == '0'
        }
        return gasLocked
    }
}
