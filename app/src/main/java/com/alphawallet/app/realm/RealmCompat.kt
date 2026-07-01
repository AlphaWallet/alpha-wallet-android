package com.alphawallet.app.realm

import android.os.Handler
import android.os.Looper
import com.alphawallet.app.service.RealmManager
import io.realm.kotlin.MutableRealm
import io.realm.kotlin.Realm
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.Sort as RealmSort
import com.alphawallet.app.repository.entity.RealmAttestation
import com.alphawallet.app.repository.entity.RealmAuxData
import com.alphawallet.app.repository.entity.RealmKeyType
import com.alphawallet.app.repository.entity.RealmNFTAsset
import com.alphawallet.app.repository.entity.RealmToken
import com.alphawallet.app.repository.entity.RealmTokenTicker
import com.alphawallet.app.repository.entity.RealmTransaction
import com.alphawallet.app.repository.entity.RealmWalletData
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

object RealmCompat {
    enum class Case { SENSITIVE, INSENSITIVE }

    enum class Sort { ASCENDING, DESCENDING }

    fun interface Transaction {
        fun execute(realm: MutableRealm)
    }

    fun interface OnSuccess {
        fun onSuccess()
    }

    @JvmStatic
    fun init(@Suppress("UNUSED_PARAMETER") context: android.content.Context) {
        // No-op: Realm Kotlin does not require explicit initialization.
    }

    @JvmStatic
    fun <T : TypedRealmObject> where(realm: TypedRealm, clazz: Class<T>): RealmQueryBuilder<T> {
        @Suppress("UNCHECKED_CAST")
        return RealmQueryBuilder(realm, clazz.kotlin as KClass<T>)
    }

    @JvmStatic
    fun executeTransaction(realm: TypedRealm, transaction: Transaction) {
        executeTransaction(realm as Realm, transaction)
    }

    @JvmStatic
    fun executeTransactionAsync(realm: TypedRealm, transaction: Transaction) {
        executeTransactionAsync(realm as Realm, transaction)
    }

    @JvmStatic
    fun executeTransactionAsync(realm: TypedRealm, transaction: Transaction, onSuccess: OnSuccess?) {
        executeTransactionAsync(realm as Realm, transaction, onSuccess)
    }

    @JvmStatic
    fun executeTransaction(realm: Realm, transaction: Transaction) {
        realm.writeBlocking { transaction.execute(this) }
    }

    /**
     * Realm Kotlin objects queried outside a write block are frozen; resolve a live copy before modifying.
     */
    @JvmStatic
    fun <T : RealmObject> latest(mutableRealm: MutableRealm, obj: T): T {
        return mutableRealm.findLatest(obj) ?: obj
    }

    @JvmStatic
    fun executeTransactionAsync(realm: Realm, transaction: Transaction) {
        val config = realm.configuration
        observerScope.launch(Dispatchers.IO) {
            val writeRealm = RealmManager.open(config)
            try {
                writeRealm.write { transaction.execute(this) }
            } finally {
                writeRealm.close()
            }
        }
    }

    @JvmStatic
    fun executeTransactionAsync(realm: Realm, transaction: Transaction, onSuccess: OnSuccess?) {
        val config = realm.configuration
        observerScope.launch(Dispatchers.IO) {
            val writeRealm = RealmManager.open(config)
            try {
                writeRealm.write { transaction.execute(this) }
            } finally {
                writeRealm.close()
            }
            if (onSuccess != null) {
                mainHandler.post { onSuccess.onSuccess() }
            }
        }
    }

    @JvmStatic
    fun close(realm: TypedRealm) {
        (realm as? Realm)?.close()
    }

    @JvmStatic
    fun isClosed(realm: TypedRealm): Boolean = realm.isClosed()

    @JvmStatic
    fun refresh(@Suppress("UNUSED_PARAMETER") realm: TypedRealm) {
        // Frozen Realm instances are refreshed on each query; kept for API compatibility.
    }

    @JvmStatic
    fun refresh(@Suppress("UNUSED_PARAMETER") realm: Realm) {
        // Frozen Realm instances are refreshed on each query; kept for API compatibility.
    }

    @JvmStatic
    fun <T : RealmObject> createObject(mutableRealm: Any, clazz: Class<T>): T {
        return createObject(mutableRealm, clazz, null)
    }

    @JvmStatic
    fun <T : RealmObject> createObject(mutableRealm: Any, clazz: Class<T>, primaryKey: Any?): T {
        val realm = mutableRealm as MutableRealm
        if (primaryKey != null) {
            val instance = newRealmObjectWithPrimaryKey(clazz, primaryKey)
            @Suppress("UNCHECKED_CAST")
            return realm.copyToRealm(instance, updatePolicy = io.realm.kotlin.UpdatePolicy.ALL) as T
        }
        @Suppress("UNCHECKED_CAST")
        return realm.copyToRealm(clazz.kotlin.createInstance()) as T
    }

    @JvmStatic
    fun insertOrUpdate(mutableRealm: Any, obj: RealmObject) {
        val realm = mutableRealm as MutableRealm
        val latest = realm.findLatest(obj)
        if (latest != null) {
            return
        }
        realm.copyToRealm(obj, updatePolicy = io.realm.kotlin.UpdatePolicy.ALL)
    }

    /**
     * Old Realm Java used [Realm.createObject] which set the PK atomically.
     * Realm Kotlin needs the PK on the object before [MutableRealm.copyToRealm].
     */
    private fun <T : RealmObject> newRealmObjectWithPrimaryKey(clazz: Class<T>, primaryKey: Any): T {
        val key = primaryKey.toString()
        @Suppress("UNCHECKED_CAST")
        val instance = when (clazz) {
            RealmToken::class.java -> RealmToken().apply { address = key }
            RealmWalletData::class.java -> RealmWalletData().apply { address = key }
            RealmKeyType::class.java -> RealmKeyType().apply { address = key }
            RealmAuxData::class.java -> RealmAuxData().apply { instanceKey = key }
            RealmNFTAsset::class.java -> RealmNFTAsset().apply { tokenIdAddr = key }
            RealmTokenTicker::class.java -> RealmTokenTicker().apply { contractKey = key }
            RealmTransaction::class.java -> RealmTransaction().apply { hash = key }
            RealmAttestation::class.java -> RealmAttestation().apply { address = key }
            else -> clazz.kotlin.createInstance().also { setPrimaryKey(it, primaryKey) }
        } as T
        verifyPrimaryKey(clazz, instance, key)
        return instance
    }

    private fun verifyPrimaryKey(clazz: Class<*>, instance: RealmObject, expectedKey: String) {
        val actualKey = readPrimaryKey(instance) ?: return
        if (actualKey != expectedKey) {
            Timber.w("RealmCompat.createObject: PK mismatch for %s (expected=%s actual=%s)",
                clazz.simpleName, expectedKey, actualKey)
        }
    }

    private fun readPrimaryKey(instance: RealmObject): String? {
        return when (instance) {
            is RealmToken -> instance.address
            is RealmWalletData -> instance.address
            is RealmKeyType -> instance.address
            is RealmAuxData -> instance.instanceKey
            is RealmNFTAsset -> instance.tokenIdAddr
            is RealmTokenTicker -> instance.contractKey
            is RealmTransaction -> instance.hash
            is RealmAttestation -> instance.address
            else -> {
                val pkProperty = instance::class.memberProperties.firstOrNull {
                    it.annotations.any { ann -> ann.annotationClass.simpleName == "PrimaryKey" }
                }
                pkProperty?.getter?.call(instance)?.toString()
            }
        }
    }

    @JvmStatic
    fun deleteFromRealm(mutableRealm: Any, obj: RealmObject) {
        val realm = mutableRealm as MutableRealm
        val latest = realm.findLatest(obj)
        if (latest != null) {
            realm.delete(latest)
        }
    }

    @JvmStatic
    fun deleteAll(mutableRealm: Any) {
        (mutableRealm as MutableRealm).deleteAll()
    }

    @JvmStatic
    fun <T : TypedRealmObject> deleteAllFromRealm(mutableRealm: Any, results: Results<T>) {
        (mutableRealm as MutableRealm).delete(results.query)
    }

    private fun setPrimaryKey(instance: RealmObject, primaryKey: Any) {
        val pkProperty = instance::class.memberProperties.firstOrNull {
            it.annotations.any { ann -> ann.annotationClass.simpleName == "PrimaryKey" }
        } ?: return
        @Suppress("UNCHECKED_CAST")
        (pkProperty as? kotlin.reflect.KMutableProperty1<RealmObject, Any?>)?.set(instance, primaryKey)
    }

    private val observerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    class Results<T : TypedRealmObject>(
        private val typedRealm: TypedRealm,
        val query: RealmQuery<T>,
        private val items: List<T> = query.find(),
    ) : Iterable<T> {
        private var observerJob: Job? = null

        fun size(): Int = items.size

        fun isEmpty(): Boolean = items.isEmpty()

        fun isValid(): Boolean = !typedRealm.isClosed()

        fun first(): T? = items.firstOrNull()

        fun get(index: Int): T = items[index]

        fun getRealm(): Realm = typedRealm as Realm

        fun deleteAllFromRealm(mutableRealm: Any): Boolean {
            val deleted = items.isNotEmpty()
            RealmCompat.deleteAllFromRealm(mutableRealm, this)
            return deleted
        }

        fun findAllAsync(): Results<T> = this

        fun addChangeListener(listener: RealmChangeListener<T>) {
            removeAllChangeListeners()
            observerJob = observerScope.launch {
                query.asFlow().collectLatest { change ->
                    listener.onChange(Results(typedRealm, query, change.list))
                }
            }
        }

        fun removeAllChangeListeners() {
            observerJob?.cancel()
            observerJob = null
        }

        override fun iterator(): Iterator<T> = items.iterator()
    }

    fun interface RealmChangeListener<T : TypedRealmObject> {
        fun onChange(results: Results<T>)
    }
}

class RealmQueryBuilder<T : TypedRealmObject>(
    private val realm: TypedRealm,
    private val clazz: KClass<T>,
) {
    private val filters = mutableListOf<String>()
    private val args = mutableListOf<Any?>()
    private val sortFields = mutableListOf<Pair<String, RealmCompat.Sort>>()
    private var limitCount: Int? = null
    private var groupDepth = 0

    fun equalTo(field: String, value: Any?): RealmQueryBuilder<T> = equalTo(field, value, RealmCompat.Case.SENSITIVE)

    fun equalTo(field: String, value: Any?, case: RealmCompat.Case): RealmQueryBuilder<T> {
        appendFilter(
            if (case == RealmCompat.Case.INSENSITIVE) "$field ==[c] \$${args.size}"
            else "$field == \$${args.size}"
        )
        args.add(value)
        return this
    }

    fun notEqualTo(field: String, value: Any?): RealmQueryBuilder<T> {
        appendFilter("$field != \$${args.size}")
        args.add(value)
        return this
    }

    fun greaterThan(field: String, value: Any?): RealmQueryBuilder<T> {
        appendFilter("$field > \$${args.size}")
        args.add(value)
        return this
    }

    fun greaterThanOrEqualTo(field: String, value: Any?): RealmQueryBuilder<T> {
        appendFilter("$field >= \$${args.size}")
        args.add(value)
        return this
    }

    fun lessThan(field: String, value: Any?): RealmQueryBuilder<T> {
        appendFilter("$field < \$${args.size}")
        args.add(value)
        return this
    }

    fun lessThanOrEqualTo(field: String, value: Any?): RealmQueryBuilder<T> {
        appendFilter("$field <= \$${args.size}")
        args.add(value)
        return this
    }

    fun contains(field: String, value: String): RealmQueryBuilder<T> =
        contains(field, value, RealmCompat.Case.SENSITIVE)

    fun contains(field: String, value: String, case: RealmCompat.Case): RealmQueryBuilder<T> {
        appendFilter(
            if (case == RealmCompat.Case.INSENSITIVE) "$field CONTAINS[c] \$${args.size}"
            else "$field CONTAINS \$${args.size}"
        )
        args.add(value)
        return this
    }

    fun like(field: String, value: String): RealmQueryBuilder<T> =
        like(field, value, RealmCompat.Case.SENSITIVE)

    fun like(field: String, value: String, case: RealmCompat.Case): RealmQueryBuilder<T> {
        appendFilter(
            if (case == RealmCompat.Case.INSENSITIVE) "$field LIKE[c] \$${args.size}"
            else "$field LIKE \$${args.size}"
        )
        args.add(value)
        return this
    }

    fun and(): RealmQueryBuilder<T> {
        appendFilter("AND", force = true)
        return this
    }

    fun beginsWith(field: String, value: String, case: RealmCompat.Case = RealmCompat.Case.SENSITIVE): RealmQueryBuilder<T> {
        appendFilter(
            if (case == RealmCompat.Case.INSENSITIVE) "$field BEGINSWITH[c] \$${args.size}"
            else "$field BEGINSWITH \$${args.size}"
        )
        args.add(value)
        return this
    }

    fun endsWith(field: String, value: String): RealmQueryBuilder<T> {
        appendFilter("$field ENDSWITH \$${args.size}")
        args.add(value)
        return this
    }

    fun isEmpty(field: String): RealmQueryBuilder<T> {
        appendFilter("$field == \"\"")
        return this
    }

    fun isNotEmpty(field: String): RealmQueryBuilder<T> {
        appendFilter("$field != \"\"")
        return this
    }

    fun sort(field: String, order: RealmCompat.Sort): RealmQueryBuilder<T> {
        sortFields.add(field to order)
        return this
    }

    fun limit(count: Int): RealmQueryBuilder<T> {
        limitCount = count
        return this
    }

    fun beginGroup(): RealmQueryBuilder<T> {
        appendFilter("(")
        groupDepth++
        return this
    }

    fun endGroup(): RealmQueryBuilder<T> {
        appendFilter(")")
        groupDepth = (groupDepth - 1).coerceAtLeast(0)
        return this
    }

    fun or(): RealmQueryBuilder<T> {
        appendFilter("OR")
        return this
    }

    fun not(): RealmQueryBuilder<T> {
        appendFilter("NOT")
        return this
    }

    fun `in`(field: String, values: Array<String>): RealmQueryBuilder<T> {
        appendFilter("$field IN \$${args.size}")
        args.add(values)
        return this
    }

    fun findFirst(): T? = buildQuery().first().find()

    fun findAll(): RealmCompat.Results<T> {
        val query = buildQuery()
        return RealmCompat.Results(realm, query, query.find())
    }

    fun findFirstAsync(): RealmCompat.Results<T> {
        if (limitCount == null) {
            limitCount = 1
        }
        return findAllAsync()
    }

    fun findAllAsync(): RealmCompat.Results<T> = findAll()

    private fun appendFilter(part: String, force: Boolean = false) {
        if (!force && filters.isNotEmpty() && needsImplicitAnd(part)) {
            filters.add("AND")
        }
        filters.add(part)
    }

    private fun needsImplicitAnd(part: String): Boolean {
        if (part == ")" || part == "OR" || part == "AND" || part == "NOT") {
            return false
        }
        if (part == "(") {
            return when (filters.last()) {
                "(", "OR", "AND", "NOT" -> false
                else -> true
            }
        }
        return when (filters.last()) {
            "(", "OR", "AND", "NOT" -> false
            else -> true
        }
    }

    internal fun buildQuery(): RealmQuery<T> {
        var queryString = filters.joinToString(" ")
        limitCount?.let { queryString = if (queryString.isBlank()) "TRUEPREDICATE LIMIT($it)" else "$queryString LIMIT($it)" }
        var query: RealmQuery<T> = if (queryString.isBlank()) {
            realm.query(clazz)
        } else {
            realm.query(clazz, queryString, *args.toTypedArray())
        }
        sortFields.forEach { (field, order) ->
            query = query.sort(field, if (order == RealmCompat.Sort.ASCENDING) RealmSort.ASCENDING else RealmSort.DESCENDING)
        }
        return query
    }
}
