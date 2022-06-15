package com.alphawallet.app.entity

/*
Economical Fee Oracle
Uses code from the KEthereum library: https://github.com/komputing/KEthereum
EIP-1559 transaction fee parameter suggestion algorithm based on the eth_feeHistory API
based on: https://github.com/zsfelfoldi/feehistory
*/

import com.alphawallet.app.service.GasService
import com.alphawallet.token.tools.Numeric
import io.reactivex.Single
import java.lang.Long.parseLong
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ZERO
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor

private const val sampleMinPercentile = 10          // sampled percentile range of exponentially weighted baseFee history
private const val sampleMaxPercentile = 30

private const val rewardPercentile = 10             // effective reward value to be selected from each individual block
private const val rewardBlockPercentile = 40        // suggested priority fee to be selected from sorted individual block reward percentiles

private const val maxTimeFactor = 15                // highest timeFactor index in the returned list of suggestion
private const val extraPriorityFeeRatio = 0.25      // extra priority fee offered in case of expected baseFee rise
private const val fallbackPriorityFee = 2000000000L // priority fee offered when there are no recent transactions

fun SuggestEIP1559(gasService: GasService, feeHistory: FeeHistory): Single<MutableMap<Int, EIP1559FeeOracleResult>> {
    return suggestPriorityFee(parseLong(feeHistory.oldestBlock.removePrefix("0x"), 16), feeHistory.gasUsedRatio, gasService)
            .flatMap { priorityFee -> calculateResult(priorityFee, feeHistory) }
}

private fun calculateResult(priorityFee: BigInteger, feeHistory: FeeHistory): Single<MutableMap<Int, EIP1559FeeOracleResult>>
{
    return Single.fromCallable{
        val baseFee: Array<BigInteger> = feeHistory.baseFeePerGas.map {
            Numeric.toBigInt(it)
        }.toTypedArray()

        baseFee[baseFee.size - 1] = (baseFee[baseFee.size - 1].toBigDecimal() * BigDecimal(9 / 8.0)).toBigInteger()

        ((feeHistory.gasUsedRatio.size - 1) downTo 0).forEach { i ->
            if (feeHistory.gasUsedRatio[i] > 0.9) {
                baseFee[i] = baseFee[i + 1]
            }
        }

        val order = (0..feeHistory.gasUsedRatio.size).map { it }.sortedBy { baseFee[it] }

        var maxBaseFee = ZERO
        val result = mutableMapOf<Int, EIP1559FeeOracleResult>()
        (maxTimeFactor downTo 0).forEach { timeFactor ->
            var bf = predictMinBaseFee(baseFee, order, timeFactor.toDouble())
            var t = BigDecimal(priorityFee)
            if (bf > maxBaseFee) {
                maxBaseFee = bf
            } else {
                // If a narrower time window yields a lower base fee suggestion than a wider window then we are probably in a price dip.
                // In this case getting included with a low priority fee is not guaranteed; instead we use the higher base fee suggestion
                // and also offer extra priority fee to increase the chance of getting included in the base fee dip.
                t += BigDecimal(maxBaseFee - bf) * BigDecimal.valueOf(extraPriorityFeeRatio)
                bf = maxBaseFee
            }
            result[timeFactor] = EIP1559FeeOracleResult(bf + t.toBigInteger(), t.toBigInteger(), bf)
        }

        return@fromCallable result
    }
}

internal fun predictMinBaseFee(baseFee: Array<BigInteger>, order: List<Int>, timeFactor: Double): BigInteger {
    if (timeFactor < 1e-6) {
        return baseFee.last()
    }
    val pendingWeight = (1 - exp(-1 / timeFactor)) / (1 - exp(-baseFee.size / timeFactor))
    var sumWeight = .0
    var result = ZERO
    var samplingCurveLast = .0
    order.indices.forEach { i ->
        sumWeight += pendingWeight * exp((order[i] - baseFee.size + 1) / timeFactor)
        val samplingCurveValue = samplingCurve(sumWeight * 100.0)
        result += ((samplingCurveValue - samplingCurveLast) * baseFee[order[i]].toDouble()).toBigDecimal().toBigInteger()
        if (samplingCurveValue >= 1) {
            return result
        }
        samplingCurveLast = samplingCurveValue
    }
    return result
}

internal fun suggestPriorityFee(firstBlock: Long, gasUsedRatio: DoubleArray, gasService: GasService): Single<BigInteger> {
    return Single.fromCallable {
        var ptr = gasUsedRatio.size - 1
        var needBlocks = 5
        val rewards = mutableListOf<BigInteger>()
        while (needBlocks > 0 && ptr >= 0) {
            val blockCount = maxBlockCount(gasUsedRatio, ptr, needBlocks)
            if (blockCount > 0) {
                // feeHistory API call with reward percentile specified is expensive and therefore is only requested for a few non-full recent blocks.
                val feeHistory = gasService.getChainFeeHistory(blockCount, "0x" + (firstBlock + ptr).toString(16), rewardPercentile.toString()).blockingGet();

                val rewardSize = feeHistory?.reward?.size ?: 0
                (0 until rewardSize).forEach {
                    rewards.add(BigInteger(Numeric.cleanHexPrefix(feeHistory.reward[it][0].removePrefix("0x")), 16))
                }
                if (rewardSize < blockCount) break
                needBlocks -= blockCount
            }
            ptr -= blockCount + 1
        }

        if (rewards.isEmpty()) {
            return@fromCallable BigInteger.valueOf(fallbackPriorityFee)
        }
        rewards.sort()
        return@fromCallable rewards[floor((rewards.size - 1) * rewardBlockPercentile / 100.0).toInt()]
    }
}

internal fun maxBlockCount(gasUsedRatio: DoubleArray, _ptr: Int, _needBlocks: Int): Int {
    var blockCount = 0
    var ptr = _ptr
    var needBlocks = _needBlocks
    while (needBlocks > 0 && ptr >= 0) {
        if (gasUsedRatio[ptr] == 0.0 || gasUsedRatio[ptr] > 0.9) {
            break
        }
        ptr--
        needBlocks--
        blockCount++
    }
    return blockCount
}

internal fun samplingCurve(percentile: Double): Double = when {
    (percentile <= sampleMinPercentile) -> 0.0
    (percentile >= sampleMaxPercentile) -> 1.0
    else -> (1 - cos((percentile - sampleMinPercentile) * 2 * Math.PI / (sampleMaxPercentile - sampleMinPercentile))) / 2
}