package com.alphawallet.app.realm

import io.realm.kotlin.Realm

class AutoCloseableRealm private constructor(private val realm: Realm) : AutoCloseable {
    fun getRealm(): Realm = realm

    override fun close() {
        realm.close()
    }

    companion object {
        @JvmStatic
        fun wrap(realm: Realm): AutoCloseableRealm = AutoCloseableRealm(realm)
    }
}
