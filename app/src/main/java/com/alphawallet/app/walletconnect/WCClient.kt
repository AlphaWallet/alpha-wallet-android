package com.alphawallet.app.walletconnect

import com.alphawallet.app.C
import com.alphawallet.app.walletconnect.entity.*
import com.alphawallet.app.walletconnect.util.WCCipher
import com.alphawallet.app.walletconnect.util.toByteArray
import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject
import com.alphawallet.token.tools.Numeric
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.github.salomonbrys.kotson.typeToken
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonSyntaxException
import okhttp3.*
import okio.ByteString
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

open class WCClient : WebSocketListener() {

    private val TAG = WCClient::class.java.simpleName

    private val gson = GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(ethTransactionSerializer)
        .create()

    private var socket: WebSocket? = null

    private val listeners: MutableSet<WebSocketListener> = mutableSetOf()

    var session: WCSession? = null
        private set

    var peerMeta: WCPeerMeta? = null
        private set

    var peerId: String? = null
        private set

    var remotePeerId: String? = null
        private set

    var isConnected: Boolean = false
        private set

    fun sessionId(): String? {
        if (session != null) return session!!.topic
        else return null
    }

    private var handshakeId: Long = -1

    var accounts: List<String>? = null
        private set

    private var chainId: String? = null

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .connectTimeout(C.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(C.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .pingInterval(C.PING_INTERVAL, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build();

    fun chainIdVal(): Long {
        return chainId?.toLong() ?: 0
    }

    var onFailure: (Throwable) -> Unit = { _ -> Unit }
    var onDisconnect: (code: Int, reason: String) -> Unit = { _, _ -> Unit }
    var onSessionRequest: (id: Long, peer: WCPeerMeta) -> Unit = { _, _ -> Unit }
    var onEthSign: (id: Long, message: WCEthereumSignMessage) -> Unit = { _, _ -> Unit }
    var onEthSignTransaction: (id: Long, transaction: WCEthereumTransaction) -> Unit =
        { _, _ -> Unit }
    var onEthSendTransaction: (id: Long, transaction: WCEthereumTransaction) -> Unit =
        { _, _ -> Unit }
    var onCustomRequest: (id: Long, payload: String) -> Unit = { _, _ -> Unit }
    var onGetAccounts: (id: Long) -> Unit = { _ -> Unit }
    var onWCOpen: (peerId: String) -> Unit = { _ -> Unit }
    var onPong: (peerId: String) -> Unit = { _ -> Unit }
    var onSwitchEthereumChain: (requestId: Long, chainId: Long) -> Unit = { _, _ -> Unit }
    var onAddEthereumChain: (requestId: Long, chainObj: WalletAddEthereumChainObject) -> Unit =
        { _, _ -> }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Timber.d("<< websocket opened >>")
        isConnected = true

        listeners.forEach { it.onOpen(webSocket, response) }

        val session =
            this.session ?: throw IllegalStateException("session can't be null on connection open")
        val peerId =
            this.peerId ?: throw IllegalStateException("peerId can't be null on connection open")
        // The Session.topic channel is used to listen session request messages only.
        subscribe(session.topic)
        // The peerId channel is used to listen to all messages sent to this httpClient.
        subscribe(peerId)

        onWCOpen(peerId)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        if (text.startsWith("Missing or invalid")) { return }
        var decrypted: String? = null
        try {
            Timber.d("<== message $text")
            decrypted = decryptMessage(text)
            Timber.d("<== decrypted $decrypted")
            handleMessage(decrypted)
        } catch (e: JsonSyntaxException) {
            //...
        } catch (e: Exception) {
            onFailure(e)
        } finally {
            listeners.forEach { it.onMessage(webSocket, decrypted ?: text) }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Timber.d("<< websocket closed >>")
        //resetState()
        onFailure(t)

        listeners.forEach { it.onFailure(webSocket, t, response) }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Timber.d("<< websocket closed >>")

        listeners.forEach { it.onClosed(webSocket, code, reason) }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Timber.d("<== Received: $bytes")
        listeners.forEach { it.onMessage(webSocket, bytes) }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Timber.d("<< closing socket >>")

        //resetState()
        onDisconnect(code, reason)

        listeners.forEach { it.onClosing(webSocket, code, reason) }
    }

    fun connect(
        session: WCSession,
        peerMeta: WCPeerMeta,
        peerId: String = UUID.randomUUID().toString(),
        remotePeerId: String? = null
    ) {
        if (this.session != null && this.session?.topic != session.topic) {
            killSession()
        }

        this.session = session
        this.peerMeta = peerMeta
        this.peerId = peerId
        this.remotePeerId = remotePeerId

        val request = Request.Builder()
            .url(session.bridge)
            .build()

        socket = httpClient.newWebSocket(request, this)
    }

    fun setupSession(accounts: List<String>, _chainId: Long) {
        this.chainId = _chainId.toString();
        this.accounts = accounts;
    }

    fun approveSession(accounts: List<String>, _chainId: Long): Boolean {
        if (handshakeId <= 0) {
            onFailure(Throwable("handshakeId must be greater than 0 on session approve"))
        }
        var useChainId: Long = _chainId
        if (this.chainId?.toIntOrNull() != 1) useChainId = _chainId
        chainId = useChainId.toString()
        this.accounts = accounts;

        val result = WCApproveSessionResponse(
            chainId = useChainId,
            accounts = accounts,
            peerId = peerId,
            peerMeta = peerMeta
        )
        val response = JsonRpcResponse(
            id = handshakeId,
            result = result
        )

        return encryptAndSend(gson.toJson(response))
    }

    fun sendPing(): Boolean {
        Timber.d("==> ping")
        return socket?.send("ping") ?: false
    }

    fun updateSession(
        accounts: List<String>? = null,
        chainId: Long? = null,
        approved: Boolean = true
    ): Boolean {
        val request = JsonRpcRequest(
            id = Date().time,
            method = WCMethod.SESSION_UPDATE,
            params = listOf(
                WCSessionUpdate(
                    approved = approved,
                    chainId = this.chainId?.toLongOrNull() ?: chainId,
                    accounts = accounts
                )
            )
        )
        return encryptAndSend(gson.toJson(request))
    }

    fun rejectSession(message: String = "Session rejected"): Boolean {
        check(handshakeId > 0) { "handshakeId must be greater than 0" }
        val response = JsonRpcErrorResponse(
            id = handshakeId,
            error = JsonRpcError.serverError(
                message = message
            )
        )
        return encryptAndSend(gson.toJson(response))
    }

    fun killSession(): Boolean {
        updateSession(approved = false)
        return disconnect()
    }

    fun <T> approveRequest(id: Long, result: T): Boolean {
        val response = JsonRpcResponse(
            id = id,
            result = result
        )
        return encryptAndSend(gson.toJson(response))
    }

    fun rejectRequest(id: Long, message: String = "Rejected by the user"): Boolean {
        val response = JsonRpcErrorResponse(
            id = id,
            error = JsonRpcError.serverError(
                message = message
            )
        )
        return encryptAndSend(gson.toJson(response))
    }

    fun switchChain(
        requestId: Long,
        chainId: Long,
        success: Boolean,
        chainAvailable: Boolean = true
    ): Boolean {
        Timber.tag(TAG).d(
            "switchChain: id: %s, chainId: %s, success: %s, chainAvailable: %s",
            requestId,
            chainId,
            success,
            chainAvailable
        );
        var response:String;
        if (!chainAvailable) {
            val errorResponse = JsonRpcErrorResponse(
                id = requestId,
                error = JsonRpcError.unrecognisedChain(
                    message = "Unrecognized chain ID"
                )
            )
            return encryptAndSend(gson.toJson(errorResponse))
        }
        if (success) {
            this.chainId = chainId.toString();
            updateSession()     // will use updated chainId
            response = gson.toJson(JsonRpcResponse<Any>(
                id = requestId,     // json rpc request id
                result = null
            ))
        } else {
            response = gson.toJson(JsonRpcErrorResponse(
                id = requestId,
                error = JsonRpcError.serverError(
                    message = "Rejected by user"
                ))
            )
        }
        return encryptAndSend(response)
    }

    fun addChain(
        requestId: Long,
        chainObj: WalletAddEthereumChainObject,
        success: Boolean
    ): Boolean {
        Timber.tag(TAG).d(
            "addChain: id: %s, chainId: %s, success: %s",
            requestId,
            chainObj.getChainId(),
            success
        );
        return if (success) {
            this.chainId = chainObj.getChainId().toString()
            updateSession()  // updated session with new chain Id
            encryptAndSend(
                gson.toJson(
                    JsonRpcResponse<Any>(id = requestId, result = null)
                )
            )
        } else {
            val errorResponse = JsonRpcErrorResponse(
                id = requestId,
                error = JsonRpcError.serverError("Rejected by user")
            )
            encryptAndSend(gson.toJson(errorResponse))
        }
    }

    private fun decryptMessage(text: String): String {
        val message = gson.fromJson<WCSocketMessage>(text)
        val encrypted = gson.fromJson<WCEncryptionPayload>(message.payload)
        val session = this.session
            ?: throw IllegalStateException("Session is null")
        return String(WCCipher.decrypt(encrypted, session.key.toByteArray()), Charsets.UTF_8)
    }

    private fun invalidParams(id: Long): Boolean {
        val response = JsonRpcErrorResponse(
            id = id,
            error = JsonRpcError.invalidParams(
                message = "Invalid parameters"
            )
        )
        return encryptAndSend(gson.toJson(response))
    }

    private fun handleMessage(payload: String) {
        try {
            val request = gson.fromJson<JsonRpcRequest<JsonArray>>(
                payload,
                typeToken<JsonRpcRequest<JsonArray>>()
            )
            val method = request.method
            if (method != null) {
                handleRequest(request)
            } else {
                onCustomRequest(request.id, payload)
            }
        } catch (e: InvalidJsonRpcParamsException) {
            Timber.d("handleMessage: InvalidJsonRpcParamsException")
            invalidParams(e.requestId)
        }
    }

    private fun handleRequest(request: JsonRpcRequest<JsonArray>) {
        Timber.tag(TAG).d("handleRequest: %s", request.toString())
        when (request.method) {
            WCMethod.SESSION_REQUEST -> {
                val param = gson.fromJson<List<WCSessionRequest>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                handshakeId = request.id
                remotePeerId = param.peerId
                chainId = param.chainId
                onSessionRequest(request.id, param.peerMeta)
            }
            WCMethod.SESSION_UPDATE -> {
                val param = gson.fromJson<List<WCSessionUpdate>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                if (!param.approved) {
                    killSession()
                }
            }
            WCMethod.ETH_SIGN -> {
                signRequest(request, WCEthereumSignMessage.WCSignType.MESSAGE)
            }
            WCMethod.ETH_PERSONAL_SIGN -> {
                signRequest(request, WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE)
            }
            WCMethod.ETH_SIGN_TYPE_DATA -> {
                signRequest(request, WCEthereumSignMessage.WCSignType.TYPED_MESSAGE)
            }
            WCMethod.ETH_SIGN_TRANSACTION -> {
                val param = gson.fromJson<List<WCEthereumTransaction>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                onEthSignTransaction(request.id, param)
            }
            WCMethod.ETH_SEND_TRANSACTION -> {
                val param = gson.fromJson<List<WCEthereumTransaction>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                onEthSendTransaction(request.id, param)
            }
            WCMethod.GET_ACCOUNTS -> {
                onGetAccounts(request.id)
            }
            WCMethod.SWITCH_ETHEREUM_CHAIN -> {
                handleSwitchChain(request)
            }
            WCMethod.ADD_ETHEREUM_CHAIN -> {
                handleAddChain(request)
            }
            else -> {}
        }
    }

    private fun handleAddChain(request: JsonRpcRequest<JsonArray>)
    {
        Timber.d("WCMethod: addEthereumChain")
        val param: WCAddEthChain = gson.fromJson<List<WCAddEthChain>>(request.params)
                .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
        Timber.tag(TAG).d("addChainRequest: $param")
        onAddEthereumChain(request.id, param.toWalletAddEthereumObject())
    }

    private fun handleSwitchChain(request: JsonRpcRequest<JsonArray>)
    {
        Timber.d("WCMethod: switchEthereumChain")
        val param = gson.fromJson<List<WCSwitchEthChain>>(request.params)
                .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
        val newChainId: Long = Numeric.toBigInt(param.chainId).toLong();
        if (newChainId != chainIdVal()) {
            onSwitchEthereumChain(request.id, newChainId)
        } else {
            // auto accept if same chain
            switchChain(request.id, chainIdVal(), true);
        }
    }

    private fun signRequest(request: JsonRpcRequest<JsonArray>, signType: WCEthereumSignMessage.WCSignType)
    {
        val params = gson.fromJson<List<String>>(request.params)
        if (params.size < 2)
            throw InvalidJsonRpcParamsException(request.id)
        onEthSign(
                request.id,
                WCEthereumSignMessage(params, signType)
        )
    }

    private fun subscribe(topic: String): Boolean {
        val message = WCSocketMessage(
            topic = topic,
            type = MessageType.SUB,
            payload = ""
        )
        val json = gson.toJson(message)
        Timber.d("==> Subscribe: $json")

        return socket?.send(gson.toJson(message)) ?: false
    }

    private fun encryptAndSend(result: String): Boolean {
        Timber.d("==> message $result")
        val session = this.session
            ?: throw IllegalStateException("Session is null")
        val payload = gson.toJson(
            WCCipher.encrypt(
                result.toByteArray(Charsets.UTF_8),
                session.key.toByteArray()
            )
        )
        val message = WCSocketMessage(
            topic = remotePeerId ?: session.topic,
            type = MessageType.PUB,
            payload = payload
        )

        val rpId = remotePeerId ?: session.topic
        Timber.d("E&Send: $rpId")

        val json = gson.toJson(message)
        Timber.d("==> encrypted $json")
        return socket?.send(json) ?: false
    }

    fun disconnect(): Boolean {
        return socket?.close(1000, null) ?: false
    }

    fun addSocketListener(listener: WebSocketListener) {
        listeners.add(listener)
    }

    fun removeSocketListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }

    fun resetState() {
        handshakeId = -1
        isConnected = false
        session = null
        peerId = null
        remotePeerId = null
        peerMeta = null
    }
}
