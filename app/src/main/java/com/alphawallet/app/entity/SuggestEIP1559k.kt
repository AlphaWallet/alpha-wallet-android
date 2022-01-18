package com.alphawallet.app.entity

/**
 * Created by JB on 18/01/2022.
 */

/*
Economical Fee Oracle
EIP-1559 transaction fee parameter suggestion algorithm based on the eth_feeHistory API
based on: https://github.com/zsfelfoldi/feehistory
*/

import com.alphawallet.app.BuildConfig
import com.alphawallet.token.tools.Numeric
import com.google.gson.Gson
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.web3j.protocol.http.HttpService
import java.lang.Integer.parseInt
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

data class EIP1559FeeOracleResult(var maxFeePerGas: BigInteger, var maxPriorityFeePerGas: BigInteger)

public fun SuggestEIP1559k(info: NetworkInfo, httpClient: OkHttpClient): Map<Int, EIP1559FeeOracleResult> {

    //val baseFee: MutableList<BigInteger> = ArrayList()

    /*for (fee in feeHistory.baseFeePerGas) {
        baseFee.add(BigInteger(fee))
    }*/

    //private const val FEE_HISTORY_CALL = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_feeHistory\",\"params\":[100, \"latest\", []],\"id\":1}"

    val feeHistory: FeeHistory? = getChainFeeHistory(info, httpClient, 100, "latest", "").blockingGet();

    val baseFee: Array<BigInteger> = feeHistory!!.baseFeePerGas.map {
        Numeric.toBigInt(it)
    }.toTypedArray()

    baseFee[baseFee.size - 1] = (baseFee[baseFee.size - 1].toBigDecimal() * BigDecimal(9 / 8.0)).toBigInteger()

    ((feeHistory.gasUsedRatio.size - 1) downTo 0).forEach { i ->
        if (feeHistory.gasUsedRatio[i] > 0.9) {
            baseFee[i] = baseFee[i + 1]
        }
    }

    val order = (0..feeHistory.gasUsedRatio.size).map { it }.sortedBy { baseFee[it] }

    val priorityFee = suggestPriorityFee(parseInt(feeHistory.oldestBlock.removePrefix("0x"), 16), feeHistory.gasUsedRatio, info, httpClient)

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
        result[timeFactor] = EIP1559FeeOracleResult(bf + t.toBigInteger(), t.toBigInteger())
    }

    return result
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

internal fun suggestPriorityFee(firstBlock: Int, gasUsedRatio: DoubleArray, info: NetworkInfo, httpClient: OkHttpClient): BigInteger {
    var ptr = gasUsedRatio.size - 1
    var needBlocks = 5
    val rewards = mutableListOf<BigInteger>()
    while (needBlocks > 0 && ptr >= 0) {
        val blockCount = maxBlockCount(gasUsedRatio, ptr, needBlocks)
        if (blockCount > 0) {
            // feeHistory API call with reward percentile specified is expensive and therefore is only requested for a few non-full recent blocks.
            val feeHistory = getChainFeeHistory(info, httpClient, blockCount, "0x" + (firstBlock + ptr).toString(16), rewardPercentile.toString()).blockingGet() //rpc.getFeeHistory(blockCount, "0x" + (firstBlock + ptr).toString(16), rewardPercentile.toString())

            //getChainFeeHistory(info, httpClient, 100, "latest", "").blockingGet();


            (0 until feeHistory!!.reward.size).forEach {
                //rewards.add(HexString(feeHistory.reward[it].first().removePrefix("0x")).hexToBigInteger())
                rewards.add(BigInteger(Numeric.cleanHexPrefix(feeHistory.reward[it][0].removePrefix("0x")), 16))
            }
            if (feeHistory.reward.size < blockCount) break
            needBlocks -= blockCount
        }
        ptr -= blockCount + 1
    }

    if (rewards.isEmpty()) {
        return BigInteger.valueOf(fallbackPriorityFee)
    }
    rewards.sort()
    return rewards[floor((rewards.size - 1) * rewardBlockPercentile / 100.0).toInt()]
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

private const val FEE_HISTORY_CALL = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_feeHistory\",\"params\":[100, \"latest\", []],\"id\":1}"

fun getChainFeeHistory(info: NetworkInfo, httpClient: OkHttpClient, blocks: Int, lastBlock: String, percentiles: String): Single<FeeHistory?> {
    val requestBody: RequestBody = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_feeHistory\",\"params\":[\"0x${blocks.toString(16)}\", \"$lastBlock\",[$percentiles]],\"id\":1}"
            .toRequestBody(HttpService.JSON_MEDIA_TYPE)
    val sss: String = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_feeHistory\",\"params\":[\"0x${blocks.toString(16)}\", \"$lastBlock\",[$percentiles]],\"id\":1}"
    //val requestBody: RequestBody = FEE_HISTORY_CALL.toRequestBody(HttpService.JSON_MEDIA_TYPE)
    return Single.fromCallable {
        val request = Request.Builder()
                .url(info.rpcServerUrl)
                .post(requestBody)
                .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.code / 200 == 1) {
                    val jsonData = JSONObject(response.body?.string())
                    return@fromCallable Gson().fromJson(jsonData.getJSONObject("result").toString(), FeeHistory::class.java)
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
        }
        FeeHistory()
    }
}
