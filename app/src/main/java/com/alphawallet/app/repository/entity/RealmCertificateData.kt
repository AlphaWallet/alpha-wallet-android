package com.alphawallet.app.repository.entity

import com.alphawallet.token.entity.SigReturnType
import com.alphawallet.token.entity.XMLDsigDescriptor
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Created by JB on 4/02/2020.
 */
class RealmCertificateData : RealmObject {
    @PrimaryKey
    var instanceKey: String = ""

    var result: String = ""
    var subject: String = ""
    var keyName: String = ""
    var keyType: String = ""
    var issuer: String = ""
    var certificateName: String = ""
    @get:JvmName("getTypeOrdinal")
    @set:JvmName("setTypeOrdinal")
    var type: Int = 0

    fun setFromSig(sig: XMLDsigDescriptor) {
        this.result = sig.result
        this.subject = sig.subject
        this.keyName = sig.keyName
        this.keyType = sig.keyType
        this.issuer = sig.issuer
        this.certificateName = sig.certificateName
        this.type = if (sig.type == null) {
            if (sig.result != null && sig.result == "pass") SigReturnType.SIGNATURE_PASS.ordinal
            else SigReturnType.SIGNATURE_INVALID.ordinal
        } else {
            sig.type.ordinal
        }
    }

    fun getDsigObject(): XMLDsigDescriptor {
        val sig = XMLDsigDescriptor()
        sig.issuer = issuer
        sig.certificateName = certificateName
        sig.keyName = keyName
        sig.keyType = keyType
        sig.result = result
        sig.subject = subject
        sig.type = SigReturnType.values()[type]
        return sig
    }

    fun getType(): SigReturnType = SigReturnType.values()[type]

    fun setType(type: SigReturnType?) {
        if (type == null && result == "pass") {
            this.type = type!!.ordinal
        }
    }
}
