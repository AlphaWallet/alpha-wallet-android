package com.alphawallet.app.repository.entity

import android.util.Base64
import com.alphawallet.app.util.Utils
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Created by JB on 19/01/2023.
 */
class RealmAttestation : RealmObject {
    @PrimaryKey
    var address: String = ""

    var name: String = ""
    @PersistedName("chains")
    var chainsCsv: String = ""
    var subTitle: String = ""
    var id: String = ""
    var collectionId: String = ""
    @get:JvmName("getAttestationStr")
    @set:JvmName("setAttestationStr")
    var attestation: String = ""
    var identifierHash: String = ""

    fun getTokenAddress(): String {
        return if (address.contains("-")) {
            address.split("-")[0]
        } else {
            address
        }
    }

    fun getAttestationID(): String {
        var secondIndex = address.indexOf("-")
        secondIndex = if (secondIndex != -1) address.indexOf("-", secondIndex + 1) else -1
        return if (secondIndex != -1) {
            address.substring(secondIndex + 1)
        } else {
            ""
        }
    }

    fun getAttestationKey(): String = address

    fun getChains(): List<Long> = Utils.longListToArray(chainsCsv)

    fun addChain(chainId: Long) {
        val knownChains = HashSet<Long>(Utils.longListToArray(chainsCsv))
        knownChains.add(chainId)
        this.chainsCsv = Utils.longArrayToString(knownChains.toTypedArray())
    }

    fun setChain(chainId: Long) {
        this.chainsCsv = chainId.toString()
    }

    fun setAttestation(attestation: ByteArray) {
        this.attestation = Base64.encodeToString(attestation, Base64.DEFAULT)
    }

    fun setAttestationLink(attestation: String) {
        this.attestation = attestation
    }

    fun getAttestationLink(): String = attestation

    fun getAttestation(): ByteArray = Base64.decode(attestation, Base64.DEFAULT)

    fun isValid(): Boolean = true

    fun supportsChain(networkFilters: List<Long>): Boolean {
        val knownChains = HashSet<Long>(Utils.longListToArray(chainsCsv))
        for (chainId in knownChains) {
            if (networkFilters.contains(chainId)) {
                return true
            }
        }
        return false
    }
}
