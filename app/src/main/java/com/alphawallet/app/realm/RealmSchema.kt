package com.alphawallet.app.realm

import com.alphawallet.app.repository.entity.Realm1559Gas
import com.alphawallet.app.repository.entity.RealmAttestation
import com.alphawallet.app.repository.entity.RealmAuxData
import com.alphawallet.app.repository.entity.RealmBlockedRpc
import com.alphawallet.app.repository.entity.RealmCertificateData
import com.alphawallet.app.repository.entity.RealmGasSpread
import com.alphawallet.app.repository.entity.RealmKeyType
import com.alphawallet.app.repository.entity.RealmNFTAsset
import com.alphawallet.app.repository.entity.RealmRPCEndpoint
import com.alphawallet.app.repository.entity.RealmToken
import com.alphawallet.app.repository.entity.RealmTokenMapping
import com.alphawallet.app.repository.entity.RealmTokenScriptData
import com.alphawallet.app.repository.entity.RealmTokenTicker
import com.alphawallet.app.repository.entity.RealmTransaction
import com.alphawallet.app.repository.entity.RealmTransfer
import com.alphawallet.app.repository.entity.RealmWalletData
import com.alphawallet.app.repository.entity.RealmWCSession
import com.alphawallet.app.repository.entity.RealmWCSignElement
import io.realm.kotlin.types.RealmObject
import kotlin.reflect.KClass

object RealmSchema {
    @JvmField
    val ALL: Set<KClass<out RealmObject>> = setOf(
        Realm1559Gas::class,
        RealmAttestation::class,
        RealmAuxData::class,
        RealmBlockedRpc::class,
        RealmCertificateData::class,
        RealmGasSpread::class,
        RealmKeyType::class,
        RealmNFTAsset::class,
        RealmRPCEndpoint::class,
        RealmToken::class,
        RealmTokenMapping::class,
        RealmTokenScriptData::class,
        RealmTokenTicker::class,
        RealmTransaction::class,
        RealmTransfer::class,
        RealmWalletData::class,
        RealmWCSession::class,
        RealmWCSignElement::class,
    )
}
