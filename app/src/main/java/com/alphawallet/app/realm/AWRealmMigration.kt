package com.alphawallet.app.realm

import com.alphawallet.app.BuildConfig
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.migration.AutomaticSchemaMigration

/**
 * Data migrations for schema changes that Realm Kotlin automatic migration cannot handle alone.
 * Field additions and new model types are handled automatically when [BuildConfig.DB_VERSION] increases.
 */
object AWRealmMigration : AutomaticSchemaMigration {
    override fun migrate(migrationContext: AutomaticSchemaMigration.MigrationContext) {
        var oldVersion = migrationContext.oldRealm.schemaVersion()

        if (oldVersion == 34L) {
            migrateChainIdToLong(migrationContext, "RealmToken")
            migrateChainIdToLong(migrationContext, "RealmAuxData")
            migrateChainIdToLong(migrationContext, "RealmGasSpread")
            migrateChainIdToLong(migrationContext, "RealmTransaction")
            migrateChainIdToLong(migrationContext, "RealmWCSession")
            oldVersion++
        }

        if (oldVersion == 51L) {
            migrationContext.enumerateIfPresent("RealmAttestation") { oldObject, newObject ->
                newObject?.run {
                    oldObject.readString("hash")?.let { set("collectionId", it) }
                }
            }
            oldVersion = 52
        }

        if (oldVersion == 52L) {
            migrationContext.enumerateIfPresent("RealmAttestation") { _, newObject ->
                newObject?.run {
                    readString("schemaUID")?.let { set("collectionId", it) }
                }
            }
        }

        // Realm Java -> Realm Kotlin: ensure wallet key rows keep a valid primary key
        migrationContext.enumerateIfPresent("RealmKeyType") { oldObject, newObject ->
            newObject?.run {
                val address = oldObject.readString("address")?.trim().orEmpty()
                if (address.isNotEmpty()) {
                    set("address", address)
                }
            }
        }

        migrationContext.enumerateIfPresent("RealmWalletData") { oldObject, newObject ->
            newObject?.run {
                val address = oldObject.readString("address")?.trim().orEmpty()
                if (address.isNotEmpty()) {
                    set("address", address)
                }
            }
        }

        migrateRealmAuxDataInstanceKeys(migrationContext)
        migrateRealmNFTAssetKeys(migrationContext)
        migrateRealmTokens(migrationContext)
        backfillRealmTokenFields(migrationContext)
        migratePrimaryKeyField(migrationContext, "RealmTransaction", "hash") { _, counter ->
            "migrated-tx-$counter"
        }
        migratePrimaryKeyField(migrationContext, "RealmTokenTicker", "contract") { _, counter ->
            "migrated-ticker-$counter"
        }
        migratePrimaryKeyField(migrationContext, "RealmAttestation", "address") { _, counter ->
            "migrated-attestation-$counter"
        }
        migratePrimaryKeyField(migrationContext, "RealmCertificateData", "instanceKey") { _, counter ->
            "migrated-certificate-$counter"
        }
        migratePrimaryKeyField(migrationContext, "RealmTokenScriptData", "instanceKey") { _, counter ->
            "migrated-tokenscript-$counter"
        }
        migratePrimaryKeyField(migrationContext, "RealmTokenMapping", "address") { _, counter ->
            "migrated-mapping-$counter"
        }
        migratePrimaryKeyField(migrationContext, "RealmWCSession", "sessionId") { _, counter ->
            "migrated-wc-$counter"
        }
        migratePrimaryKeyField(migrationContext, "RealmRPCEndpoint", "key") { _, counter ->
            "migrated-rpc-$counter"
        }
        migrateLongPrimaryKeyField(migrationContext, "RealmGasSpread", "chainId")
        migrateLongPrimaryKeyField(migrationContext, "Realm1559Gas", "chainId")
    }

    /**
     * Realm Java -> Realm Kotlin automatic migration can leave PK strings empty on new objects,
     * producing duplicate "" keys. Re-copy from the old realm and assign unique fallbacks.
     */
    private fun migrateRealmAuxDataInstanceKeys(
        migrationContext: AutomaticSchemaMigration.MigrationContext,
    ) {
        val seenKeys = mutableSetOf<String>()
        var orphanCounter = 0

        migrationContext.enumerateIfPresent("RealmAuxData") { oldObject, newObject ->
            newObject?.run {
                copyLongField(oldObject, "chainId")
                copyStringField(oldObject, "tokenAddress")
                copyStringField(oldObject, "tokenId")
                copyStringField(oldObject, "functionId")
                copyStringField(oldObject, "result")
                copyLongField(oldObject, "resultTime")
                copyLongField(oldObject, "resultReceivedTime")

                var key = oldObject.readString("instanceKey")?.trim().orEmpty()
                if (key.isEmpty()) {
                    orphanCounter++
                    val chainId = readLong("chainId") ?: 0L
                    val tokenAddress = readString("tokenAddress").orEmpty()
                    val tokenId = readString("tokenId").orEmpty()
                    val functionId = readString("functionId").orEmpty()
                    val resultTime = readLong("resultTime") ?: 0L
                    val resultReceivedTime = readLong("resultReceivedTime") ?: 0L
                    key = "migrated-$chainId-$tokenAddress-$tokenId-$functionId-" +
                        "$resultTime-$resultReceivedTime-$orphanCounter"
                }

                key = deduplicateKey(key, seenKeys) { suffix ->
                    orphanCounter++
                    "$key-dup-$suffix"
                }
                set("instanceKey", key)
            }
        }
    }

    private fun backfillRealmTokenFields(
        migrationContext: AutomaticSchemaMigration.MigrationContext,
    ) {
        migrationContext.enumerateIfPresent("RealmToken") { oldObject, newObject ->
            newObject?.run {
                val key = readString("address")?.trim().orEmpty()
                if (key.isEmpty()) {
                    return@run
                }

                if ((readLong("chainId") ?: 0L) == 0L) {
                    chainIdFromDatabaseKey(key)?.let { set("chainId", it) }
                }

                val spec = readInt("interfaceSpec") ?: 0
                if (spec == 0) {
                    when {
                        key.regionMatches(0, "eth-", 0, 4, ignoreCase = true) -> set("interfaceSpec", 1L)
                        key.startsWith("0x", ignoreCase = true) -> set("interfaceSpec", 2L)
                    }
                }

                // Re-copy fields in case an earlier migration pass left defaults
                copyStringField(oldObject, "name")
                copyStringField(oldObject, "symbol")
                copyStringField(oldObject, "balance")
                copyBooleanField(oldObject, "isEnabled")
            }
        }
    }

    fun chainIdFromDatabaseKey(databaseKey: String): Long? {
        if (databaseKey.regionMatches(0, "eth-", 0, 4, ignoreCase = true)) {
            return databaseKey.substring(4).toLongOrNull()
        }
        val separatorIndex = databaseKey.indexOf('-', startIndex = 2).takeIf { it >= 0 }
            ?: databaseKey.indexOf('.', startIndex = 2).takeIf { it >= 0 }
            ?: return null
        var suffix = databaseKey.substring(separatorIndex + 1)
        val nextDash = suffix.indexOf('-')
        if (nextDash > 0) {
            suffix = suffix.substring(0, nextDash)
        }
        return suffix.toLongOrNull()
    }

    private fun migrateRealmTokens(
        migrationContext: AutomaticSchemaMigration.MigrationContext,
    ) {
        val seenKeys = mutableSetOf<String>()

        migrationContext.enumerateIfPresent("RealmToken") { oldObject, newObject ->
            newObject?.run {
                val sourceAddress = oldObject.readString("address")?.trim().orEmpty()
                if (sourceAddress.isEmpty()) {
                    return@run
                }

                copyStringField(oldObject, "name")
                copyStringField(oldObject, "symbol")
                copyStringField(oldObject, "balance")
                copyStringField(oldObject, "auxData")
                copyStringField(oldObject, "erc1155BlockRead")
                copyStringField(oldObject, "address")
                copyIntField(oldObject, "decimals")
                copyIntField(oldObject, "tokenId")
                copyIntField(oldObject, "interfaceSpec")
                copyLongField(oldObject, "addedTime")
                copyLongField(oldObject, "updatedTime")
                copyLongField(oldObject, "lastTxTime")
                copyLongField(oldObject, "lastBlockRead")
                copyLongField(oldObject, "chainId")
                copyLongField(oldObject, "earliestTxBlock")
                copyBooleanField(oldObject, "isEnabled")
                copyBooleanField(oldObject, "visibilityChanged")

                var key = sourceAddress
                key = deduplicateKey(key, seenKeys) { suffix ->
                    "$key-dup-$suffix"
                }
                set("address", key)
            }
        }
    }

    private fun migrateRealmNFTAssetKeys(
        migrationContext: AutomaticSchemaMigration.MigrationContext,
    ) {
        val seenKeys = mutableSetOf<String>()
        var orphanCounter = 0

        migrationContext.enumerateIfPresent("RealmNFTAsset") { oldObject, newObject ->
            newObject?.run {
                copyStringField(oldObject, "metaData")
                copyStringField(oldObject, "balance")

                var key = oldObject.readString("tokenIdAddr")?.trim().orEmpty()
                if (key.isEmpty()) {
                    orphanCounter++
                    val metaData = readString("metaData").orEmpty()
                    val balance = readString("balance").orEmpty()
                    key = "migrated-nft-$metaData-$balance-$orphanCounter"
                }

                key = deduplicateKey(key, seenKeys) { suffix ->
                    orphanCounter++
                    "$key-dup-$suffix"
                }
                set("tokenIdAddr", key)
            }
        }
    }

    private fun migrateLongPrimaryKeyField(
        migrationContext: AutomaticSchemaMigration.MigrationContext,
        className: String,
        primaryKeyField: String,
    ) {
        val seenKeys = mutableSetOf<Long>()
        var orphanCounter = 0L

        migrationContext.enumerateIfPresent(className) { oldObject, newObject ->
            newObject?.run {
                var key = oldObject.readLong(primaryKeyField) ?: 0L
                if (key == 0L) {
                    orphanCounter++
                    key = -orphanCounter
                }
                while (!seenKeys.add(key)) {
                    orphanCounter++
                    key = -orphanCounter
                }
                set(primaryKeyField, key)
            }
        }
    }

    private fun migratePrimaryKeyField(
        migrationContext: AutomaticSchemaMigration.MigrationContext,
        className: String,
        primaryKeyField: String,
        emptyKeyFallback: (oldObject: io.realm.kotlin.dynamic.DynamicRealmObject, counter: Int) -> String,
    ) {
        val seenKeys = mutableSetOf<String>()
        var orphanCounter = 0

        migrationContext.enumerateIfPresent(className) { oldObject, newObject ->
            newObject?.run {
                var key = oldObject.readString(primaryKeyField)?.trim().orEmpty()
                if (key.isEmpty()) {
                    orphanCounter++
                    key = emptyKeyFallback(oldObject, orphanCounter)
                }
                key = deduplicateKey(key, seenKeys) { suffix ->
                    orphanCounter++
                    "$key-dup-$suffix"
                }
                set(primaryKeyField, key)
            }
        }
    }

    private fun AutomaticSchemaMigration.MigrationContext.enumerateIfPresent(
        className: String,
        block: (
            io.realm.kotlin.dynamic.DynamicRealmObject,
            io.realm.kotlin.dynamic.DynamicMutableRealmObject?,
        ) -> Unit,
    ) {
        if (!oldRealm.schema().classes.any { it.name == className }) {
            return
        }
        enumerate(className, block)
    }

    private fun deduplicateKey(
        key: String,
        seenKeys: MutableSet<String>,
        onDuplicate: (suffix: Int) -> String,
    ): String {
        var uniqueKey = key
        var duplicateCounter = 0
        while (!seenKeys.add(uniqueKey)) {
            duplicateCounter++
            uniqueKey = onDuplicate(duplicateCounter)
        }
        return uniqueKey
    }

    private fun io.realm.kotlin.dynamic.DynamicMutableRealmObject.copyStringField(
        oldObject: io.realm.kotlin.dynamic.DynamicRealmObject,
        field: String,
    ) {
        oldObject.readString(field)?.let { set(field, it) }
    }

    private fun io.realm.kotlin.dynamic.DynamicMutableRealmObject.copyLongField(
        oldObject: io.realm.kotlin.dynamic.DynamicRealmObject,
        field: String,
    ) {
        oldObject.readLong(field)?.let { set(field, it) }
    }

    private fun io.realm.kotlin.dynamic.DynamicMutableRealmObject.copyIntField(
        oldObject: io.realm.kotlin.dynamic.DynamicRealmObject,
        field: String,
    ) {
        // Realm Kotlin's dynamic migration API always represents integral properties
        // (however they are declared - Byte/Short/Int/Long - on the actual model class)
        // as Long; passing a raw Int to set() throws IllegalArgumentException.
        oldObject.readInt(field)?.let { set(field, it.toLong()) }
    }

    private fun io.realm.kotlin.dynamic.DynamicMutableRealmObject.copyBooleanField(
        oldObject: io.realm.kotlin.dynamic.DynamicRealmObject,
        field: String,
    ) {
        oldObject.readBoolean(field)?.let { set(field, it) }
    }

    private inline fun <T> tryRead(block: () -> T): T? =
        try {
            block()
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun io.realm.kotlin.dynamic.DynamicRealmObject.readString(field: String): String? =
        tryRead { getNullableValue(field, String::class) }
            ?: tryRead { getValue(field, String::class) }

    private fun io.realm.kotlin.dynamic.DynamicMutableRealmObject.readString(field: String): String? =
        tryRead { getNullableValue(field, String::class) }
            ?: tryRead { getValue(field, String::class) }

    private fun io.realm.kotlin.dynamic.DynamicRealmObject.readLong(field: String): Long? =
        tryRead { getValue(field, Long::class) }
            ?: tryRead { getValue(field, Int::class).toLong() }
            ?: tryRead { getNullableValue(field, Long::class) }
            ?: tryRead { getNullableValue(field, Int::class)?.toLong() }

    private fun io.realm.kotlin.dynamic.DynamicMutableRealmObject.readLong(field: String): Long? =
        tryRead { getValue(field, Long::class) }
            ?: tryRead { getValue(field, Int::class).toLong() }
            ?: tryRead { getNullableValue(field, Long::class) }
            ?: tryRead { getNullableValue(field, Int::class)?.toLong() }

    private fun io.realm.kotlin.dynamic.DynamicRealmObject.readInt(field: String): Int? =
        tryRead { getValue(field, Int::class) }
            ?: tryRead { getValue(field, Long::class).toInt() }
            ?: tryRead { getNullableValue(field, Int::class) }
            ?: tryRead { getNullableValue(field, Long::class)?.toInt() }

    private fun io.realm.kotlin.dynamic.DynamicMutableRealmObject.readInt(field: String): Int? =
        tryRead { getValue(field, Int::class) }
            ?: tryRead { getValue(field, Long::class).toInt() }
            ?: tryRead { getNullableValue(field, Int::class) }
            ?: tryRead { getNullableValue(field, Long::class)?.toInt() }

    private fun io.realm.kotlin.dynamic.DynamicRealmObject.readBoolean(field: String): Boolean? =
        tryRead { getValue(field, Boolean::class) }
            ?: tryRead { getNullableValue(field, Boolean::class) }

    private fun io.realm.kotlin.dynamic.DynamicMutableRealmObject.readBoolean(field: String): Boolean? =
        tryRead { getValue(field, Boolean::class) }
            ?: tryRead { getNullableValue(field, Boolean::class) }

    private fun migrateChainIdToLong(
        migrationContext: AutomaticSchemaMigration.MigrationContext,
        className: String,
    ) {
        migrationContext.enumerateIfPresent(className) { oldObject, newObject ->
            newObject?.run {
                oldObject.readLong("chainId")?.let { set("chainId", it) }
            }
        }
    }
}

fun buildRealmConfiguration(name: String): RealmConfiguration {
    return RealmConfiguration.Builder(schema = RealmSchema.ALL)
        .name(name)
        .schemaVersion(BuildConfig.DB_VERSION.toLong())
        .migration(AWRealmMigration)
        .build()
}
