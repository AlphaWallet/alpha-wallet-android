package com.alphawallet.app.repository.entity

import android.content.Context
import android.text.TextUtils
import com.alphawallet.app.R
import com.alphawallet.app.entity.Transaction
import com.alphawallet.app.entity.tokens.Token
import com.alphawallet.app.realm.RealmCompat
import com.alphawallet.app.realm.RealmQueryBuilder
import com.alphawallet.app.repository.EventResult
import com.alphawallet.app.repository.TokensRealmSource.EVENT_CARDS
import com.alphawallet.app.ui.widget.entity.ENSHandler
import com.alphawallet.app.ui.widget.entity.StatusType
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.math.BigInteger
import java.util.regex.Pattern

/**
 * Created by James on 6/05/2019.
 * Stormbird in Sydney
 */
class RealmAuxData : RealmObject {
    @PrimaryKey
    var instanceKey: String = ""

    var chainId: Long = 0
    var tokenAddress: String = ""
    @get:JvmName("getTokenIdStr")
    var tokenId: String = ""
    var functionId: String = ""
    var result: String = ""
    var resultTime: Long = 0
    var resultReceivedTime: Long = 0

    fun getTransactionHash(): String {
        val split = instanceKey.split("-")
        return if (split.isNotEmpty()) split[0] else ""
    }

    fun getEventName(): String {
        val split = instanceKey.split("-")
        return if (split.size > 1) split[1] else ""
    }

    fun getExtendId(): Long {
        val split = instanceKey.split("-")
        if (split.size > 3) {
            val extendId = split[3]
            if (extendId.isNotEmpty() && Character.isDigit(extendId[0])) {
                return extendId.toLong()
            }
        }
        return 0
    }

    fun getTokenId(): BigInteger {
        return try {
            BigInteger(tokenId, Character.MAX_RADIX)
        } catch (e: Exception) {
            BigInteger.ZERO
        }
    }

    fun getAddress(): String = instanceKey.split("-")[0]

    fun getEventStatusType(): StatusType {
        return when (functionId) {
            "sent" -> StatusType.SENT
            "received" -> StatusType.RECEIVE
            "mint" -> StatusType.RECEIVE
            "burn" -> StatusType.SENT
            "ownerApproved", "approvalObtained" -> StatusType.NONE
            else -> StatusType.NONE
        }
    }

    fun getDetailAddress(): String {
        val resultMap = getEventResultMap()
        return when (functionId) {
            "sent" -> if (resultMap.containsKey("to")) resultMap["to"]!!.value else getEventName()
            "received" -> if (resultMap.containsKey("from")) resultMap["from"]!!.value else getEventName()
            "ownerApproved" -> if (resultMap.containsKey("spender")) resultMap["spender"]!!.value else getEventName()
            "approvalObtained" -> if (resultMap.containsKey("owner")) resultMap["owner"]!!.value else getEventName()
            else -> getEventName()
        }
    }

    fun getDetail(ctx: Context, tx: Transaction?, itemView: String): String {
        val resultMap = getEventResultMap()
        if (tx != null) tx.addTransactionElements(resultMap)

        if (!TextUtils.isEmpty(itemView)) {
            var eventDesc = itemView
            val m = Pattern.compile("\\$\\{([^}]+)\\}").matcher(itemView)
            while (m.find()) {
                val match = m.group(1)
                val replacement = if (resultMap.containsKey(match)) resultMap[match]!!.value else null
                if (replacement != null) {
                    val index = eventDesc.indexOf(m.group(0))
                    eventDesc = eventDesc.substring(0, index) + replacement +
                        eventDesc.substring(index + m.group(0).length)
                }
            }
            return eventDesc
        }

        return when (functionId) {
            "sent" -> if (resultMap.containsKey("to")) {
                ctx.getString(R.string.sent_to, ENSHandler.displayAddressOrENS(ctx, resultMap["to"]!!.value))
            } else {
                "$tokenId $result"
            }
            "received" -> if (resultMap.containsKey("from")) {
                ctx.getString(R.string.from, ENSHandler.displayAddressOrENS(ctx, resultMap["from"]!!.value))
            } else {
                "$tokenId $result"
            }
            "ownerApproved" -> if (resultMap.containsKey("spender")) {
                ctx.getString(
                    R.string.approval_granted_to,
                    ENSHandler.displayAddressOrENS(ctx, resultMap["spender"]!!.value)
                )
            } else {
                "$tokenId $result"
            }
            "approvalObtained" -> if (resultMap.containsKey("owner")) {
                ctx.getString(
                    R.string.approval_obtained_from,
                    ENSHandler.displayAddressOrENS(ctx, resultMap["owner"]!!.value)
                )
            } else {
                "$tokenId $result"
            }
            else -> getEventName()
        }
    }

    fun getTitle(ctx: Context): String {
        return when (functionId) {
            "sent" -> ctx.getString(R.string.activity_sent)
            "received" -> ctx.getString(R.string.activity_received)
            "ownerApproved" -> ctx.getString(R.string.activity_approved)
            "approvalObtained" -> ctx.getString(R.string.activity_approval_granted)
            else -> functionId
        }
    }

    private enum class ResultState {
        NAME,
        TYPE,
        RESULT
    }

    fun getEventResultMap(): Map<String, EventResult> {
        val split = result.split(",")
        var state = ResultState.NAME
        val resultMap = HashMap<String, EventResult>()
        var name: String? = null
        var type: String? = null
        for (r in split) {
            when (state) {
                ResultState.NAME -> {
                    name = r
                    state = ResultState.TYPE
                }
                ResultState.TYPE -> {
                    type = r
                    state = ResultState.RESULT
                }
                ResultState.RESULT -> {
                    if (name != null && type != null) {
                        resultMap[name] = EventResult(type, r)
                        name = null
                        type = null
                    }
                    state = ResultState.NAME
                }
            }
        }
        return resultMap
    }

    fun setBaseChainBlock(blockRead: Long) {
        this.functionId = blockRead.toString()
    }

    fun getBaseChainBlock(): Long {
        return try {
            if (TextUtils.isEmpty(functionId)) 0 else functionId.toLong()
        } catch (e: NumberFormatException) {
            0
        }
    }

    companion object {
        @JvmStatic
        fun getEventListener(
            realm: TypedRealm,
            token: Token,
            tokenId: BigInteger,
            historyCount: Int,
            timeLimit: Long
        ): RealmCompat.Results<RealmAuxData> {
            return getEventQuery(realm, token, tokenId, historyCount, timeLimit).findAllAsync()
        }

        @JvmStatic
        fun getEventQuery(
            realm: TypedRealm,
            token: Token,
            tokenId: BigInteger,
            historyCount: Int,
            timeLimit: Long
        ): RealmQueryBuilder<RealmAuxData> {
            val tokenIdHex = tokenId.toString(16)
            return RealmQueryBuilder(realm, RealmAuxData::class)
                .endsWith("instanceKey", EVENT_CARDS)
                .sort("resultTime", RealmCompat.Sort.DESCENDING)
                .greaterThan("resultTime", timeLimit)
                .equalTo("chainId", token.tokenInfo.chainId)
                .beginGroup()
                .equalTo("tokenId", "0")
                .or()
                .equalTo("tokenId", tokenIdHex)
                .endGroup()
                .equalTo("tokenAddress", token.getAddress())
                .limit(historyCount)
        }
    }
}
