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
private const val MIN_PRIORITY_FEE = 100000000L       // Minimum priority fee in Wei, 0.1 Gwei

fun SuggestEIP1559(gasService: GasService, feeHistory: FeeHistory): Single<MutableMap<Int, EIP1559FeeOracleResult>> {
    return suggestPriorityFee(parseLong(feeHistory.oldestBlock.removePrefix("0x"), 16), feeHistory, gasService)
            .flatMap { priorityFee -> calculateResult(priorityFee, feeHistory) }
}

private fun calculateResult(priorityFee: BigInteger, feeHistory: FeeHistory): Single<MutableMap<Int, EIP1559FeeOracleResult>>
{
    return Single.fromCallable{
        val baseFee: Array<BigInteger> = feeHistory.baseFeePerGas.map {
            Numeric.toBigInt(it)
        }.toTypedArray()

        var usePriorityFee = priorityFee;

        var consistentBaseFee = false;

        //spot consistent base fees
        if (checkConsistentFees(baseFee)) {
            consistentBaseFee = true
            baseFee[baseFee.size - 1] = baseFee[0];
            if (priorityFee.toLong() == fallbackPriorityFee && priorityFee > baseFee[0]) {
                usePriorityFee = baseFee[0];
            }
        } else {
            baseFee[baseFee.size - 1] = (baseFee[baseFee.size - 1].toBigDecimal() * BigDecimal(9 / 8.0)).toBigInteger()
        }

        ((feeHistory.gasUsedRatio.size - 1) downTo 0).forEach { i ->
            if (feeHistory.gasUsedRatio[i] > 0.9) {
                baseFee[i] = baseFee[i + 1]
            }
        }

        val order = (0..feeHistory.gasUsedRatio.size).map { it }.sortedBy { baseFee[it] }

        var maxBaseFee = ZERO
        val result = mutableMapOf<Int, EIP1559FeeOracleResult>()
        (maxTimeFactor downTo 0).forEach { timeFactor ->
            var bf: BigInteger
            if (timeFactor < 1e-6) {
                bf = baseFee.last()
            } else {
                bf = predictMinBaseFee(baseFee, order, timeFactor.toDouble(), consistentBaseFee)
            }
            var t = BigDecimal(usePriorityFee)
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

fun checkConsistentFees(baseFee: Array<BigInteger>): Boolean {
    if (baseFee.isEmpty()) {
        return false
    }

    val firstVal: BigInteger = baseFee[0]
    var isConsistent = true;

    baseFee.forEach { element ->
        run {
            if (element != firstVal) {
                isConsistent = false;
            }
        }
    }

    return isConsistent;
}

internal fun predictMinBaseFee(baseFee: Array<BigInteger>, order: List<Int>, timeFactor: Double, consistentBaseFee: Boolean): BigInteger {
    val pendingWeight = (1 - exp(-1 / timeFactor)) / (1 - exp(-baseFee.size / timeFactor))
    var sumWeight = .0
    var result = ZERO
    var samplingCurveLast = .0
    if (consistentBaseFee) {
        result = baseFee[0]
    } else {
        order.indices.forEach { i ->
            sumWeight += pendingWeight * exp((order[i] - baseFee.size + 1) / timeFactor)
            val samplingCurveValue = samplingCurve(sumWeight * 100.0)
            result += ((samplingCurveValue - samplingCurveLast) * baseFee[order[i]].toDouble()).toBigDecimal().toBigInteger()
            if (samplingCurveValue >= 1) {
                return result
            }
            samplingCurveLast = samplingCurveValue
        }
    }
    return result
}

internal fun suggestPriorityFee(firstBlock: Long, feeHistory: FeeHistory, gasService: GasService): Single<BigInteger> {
    return Single.fromCallable {
        val gasUsedRatio = feeHistory.gasUsedRatio
        var ptr = gasUsedRatio.size - 1
        var needBlocks = 5
        val rewards = mutableListOf<BigInteger>()
        while (needBlocks > 0 && ptr >= 0) {
            val blockCount = maxBlockCount(gasUsedRatio, ptr, needBlocks)
            if (blockCount > 0) {
                // feeHistory API call with reward percentile specified is expensive and therefore is only requested for a few non-full recent blocks.
                val feeHistoryFetch = gasService.getChainFeeHistory(blockCount,
                    Numeric.prependHexPrefix((firstBlock + ptr).toString(16)),
                    rewardPercentile.toString()).blockingGet();

                val rewardSize = feeHistoryFetch?.reward?.size ?: 0
                (0 until rewardSize).forEach {
                    rewards.add(BigInteger(Numeric.cleanHexPrefix(feeHistoryFetch.reward[it][0].removePrefix("0x")),
                        16))
                }
                if (rewardSize < blockCount) break
                needBlocks -= blockCount
            }
            ptr -= blockCount + 1
        }

        if (rewards.isEmpty()) {
            return@fromCallable calculatePriorityFee(feeHistory)
        }
        rewards.sort()
        return@fromCallable rewards[floor((rewards.size - 1) * rewardBlockPercentile / 100.0).toInt()]
    }
}

fun calculatePriorityFee(feeHistory: FeeHistory): BigInteger? {
    var priorityFee = BigInteger.valueOf(MIN_PRIORITY_FEE)

    feeHistory.baseFeePerGas.forEach { element ->
        run {
            val elementVal = Numeric.toBigInt(element)
            if (elementVal > priorityFee && elementVal <= BigInteger.valueOf(fallbackPriorityFee)) {
                priorityFee = elementVal
            }
        }
    }

    return priorityFee
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
