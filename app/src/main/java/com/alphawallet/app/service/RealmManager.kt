package com.alphawallet.app.service

import com.alphawallet.app.entity.Wallet
import com.alphawallet.app.realm.buildRealmConfiguration
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.Configuration
import timber.log.Timber
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

class RealmManager {
    private val realmConfigurations: MutableMap<String, RealmConfiguration> = HashMap()

    fun getRealmInstanceName(wallet: Wallet): String {
        return wallet.address.lowercase() + "-db.realm"
    }

    fun getRealmInstance(wallet: Wallet): Realm {
        return getRealmInstanceInternal(getRealmInstanceName(wallet))
    }

    fun getRealmInstance(walletAddress: String): Realm {
        return getRealmInstanceInternal(walletAddress.lowercase() + "-db.realm")
    }

    fun getRealmInstanceInternal(name: String): Realm {
        return openRealm(name)
    }

    private fun openRealm(name: String): Realm {
        synchronized(lockFor(name)) {
            var config = realmConfigurations[name]
            if (config == null) {
                config = buildRealmConfiguration(name)
                realmConfigurations[name] = config
            }
            return try {
                open(config)
            } catch (e: Exception) {
                Timber.e(e, "Failed to open Realm %s; refusing to delete existing database", name)
                throw e
            }
        }
    }

    fun getWalletDataRealmInstance(): Realm {
        return getRealmInstanceInternal("WalletData-db.realm")
    }

    fun getWalletTypeRealmInstance(): Realm {
        return getRealmInstanceInternal("WalletType-db.realm")
    }

    companion object {
        private val openLocks = ConcurrentHashMap<String, Any>()

        private fun lockFor(name: String): Any = openLocks.getOrPut(name) { Any() }

        /**
         * All opens for the same realm file must be serialized. Concurrent opens during
         * Java-to-Kotlin migration trigger RLM_ERR_MISMATCHED_CONFIG.
         */
        @JvmStatic
        fun open(config: Configuration): Realm {
            synchronized(lockFor(config.name)) {
                return Realm.open(config)
            }
        }
    }
}
