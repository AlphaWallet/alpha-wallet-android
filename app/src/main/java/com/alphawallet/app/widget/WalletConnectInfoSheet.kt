package com.alphawallet.app.widget

import android.app.Activity
import android.text.Spannable
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.alphawallet.app.R
import com.alphawallet.app.entity.StandardFunctionInterface
import com.alphawallet.app.repository.EthereumNetworkBase
import com.alphawallet.app.walletconnect.entity.WCPeerMeta
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import timber.log.Timber
import java.util.*

class WalletConnectInfoSheet(
    private val activity: Activity,
    private val iconUrl: String?,
    private val peer: WCPeerMeta,
    private var chainIdOverride: Long,
    private val hostListener: WalletConnectSheetCallback,
    ) : BottomSheetDialog(activity, R.style.FullscreenBottomSheetDialogStyle), StandardFunctionInterface {

    init {
        val view: View = View.inflate(context, R.layout.dialog_wallet_connect_sheet, null)
        setContentView(view)
        val behaviour: BottomSheetBehavior<View> = BottomSheetBehavior.from(view.parent as View)
        behaviour.state = STATE_EXPANDED
        behaviour.skipCollapsed = true

        view.findViewById<ImageView>(R.id.logo)?.apply {
            Glide.with(activity)
                .load(iconUrl)
                .circleCrop()
                .into(this)
        }
        view.findViewById<TextView>(R.id.text_title)?.apply { text = peer.name }
        view.findViewById<ImageView>(R.id.image_close)?.apply { setOnClickListener { hostListener.onClickReject() } }
        view.findViewById<DialogInfoItem>(R.id.info_website)?.apply {
            setLabel("Website")
            setMessage(peer.url)
        }
        view.findViewById<DialogInfoItem>(R.id.info_network)?.apply {
            setLabel("Network")
            setMessage(EthereumNetworkBase.getShortChainName(chainIdOverride))
            setMessageTextColor(EthereumNetworkBase.getChainColour(chainIdOverride))
            setActionText(context.getString(R.string.edit))
            setActionListener {
                hostListener.onClickChainId()
            }
        }
        Timber.d("Peer url: %s\nPeer: %s",peer.url, peer.description)

        view.findViewById<Button>(R.id.button_approve)?.apply {
            setOnClickListener { hostListener.onClickApprove(chainIdOverride) }
        }
        view.findViewById<Button>(R.id.button_reject)?.apply {
            setOnClickListener { hostListener.onClickReject() }
        }

    }

    // on receiving result from chain activity
    fun onUpdateChain(chainId: Long) {
        chainIdOverride = chainId
        findViewById<DialogInfoItem>(R.id.info_network)?.apply {
            setMessage(EthereumNetworkBase.getShortChainName(chainId))
            setMessageTextColor(EthereumNetworkBase.getChainColour(chainId))
            setActionListener {
                hostListener.onClickChainId()
            }
        }
    }

    override fun handleClick(action: String?, actionId: Int) {
        when(action) {
            context.getString(R.string.approve) -> {
                hostListener.onClickApprove(chainIdOverride)
            }
            context.getString(R.string.dialog_reject) -> {
                hostListener.onClickReject()
            }
        }
    }
}
