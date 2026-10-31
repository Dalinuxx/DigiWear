package com.digiwear.companion.multiplayer

import android.util.Log
import com.digiwear.core.model.BattleFighter
import com.digiwear.core.multiplayer.DuelCommitmentPayload
import com.digiwear.core.multiplayer.DuelPacket
import com.digiwear.core.multiplayer.DuelRevealPayload
import com.digiwear.core.multiplayer.DuelSecurityEngine
import com.digiwear.core.multiplayer.EncryptedDuelPayload
import com.digiwear.core.multiplayer.FighterHandshakePayload
import com.digiwear.core.multiplayer.KeyExchangePayload
import com.digiwear.core.multiplayer.MultiplayerProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.security.KeyPair
import java.util.concurrent.TimeUnit
import javax.crypto.spec.SecretKeySpec

enum class LobbyState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    WAITING_FOR_OPPONENT,
    OPPONENT_FOUND,
    SECURITY_HANDSHAKE,
    DUEL_READY,
    ERROR
}

class LiveDuelLobbyManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "LiveDuelLobby"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep alive
        .build()

    private var webSocket: WebSocket? = null

    private val _lobbyState = MutableStateFlow(LobbyState.DISCONNECTED)
    val lobbyState: StateFlow<LobbyState> = _lobbyState.asStateFlow()

    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _opponentFighter = MutableStateFlow<BattleFighter?>(null)
    val opponentFighter: StateFlow<BattleFighter?> = _opponentFighter.asStateFlow()

    private val _opponentTamerTag = MutableStateFlow<String?>(null)
    val opponentTamerTag: StateFlow<String?> = _opponentTamerTag.asStateFlow()

    private val _statusMessage = MutableStateFlow("Disconnected")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Cryptographic Session State
    private var myKeyPair: KeyPair? = null
    private var sessionKey: SecretKeySpec? = null
    private var activeFighter: BattleFighter? = null
    private var tamerTag: String = "Tamer"

    // Callbacks for duel events
    var onOpponentActionReceived: ((choice: Int, reflexMs: Long) -> Unit)? = null
    var onOpponentCommitmentReceived: ((round: Int) -> Unit)? = null

    fun connect(serverUrl: String, serverKey: String, onConnected: (() -> Unit)? = null) {
        if (_lobbyState.value == LobbyState.CONNECTING || _lobbyState.value == LobbyState.CONNECTED) {
            return
        }

        _lobbyState.value = LobbyState.CONNECTING
        _errorMessage.value = null
        _statusMessage.value = "Connecting to relay server..."

        // Ensure proper websocket protocol scheme
        val normalizedUrl = when {
            serverUrl.startsWith("http://", ignoreCase = true) -> "ws://" + serverUrl.substring(7)
            serverUrl.startsWith("https://", ignoreCase = true) -> "wss://" + serverUrl.substring(8)
            serverUrl.startsWith("ws://", ignoreCase = true) || serverUrl.startsWith("wss://", ignoreCase = true) -> serverUrl
            else -> "wss://$serverUrl"
        }

        val requestBuilder = Request.Builder().url(normalizedUrl)
        if (serverKey.isNotBlank()) {
            val keyClean = serverKey.trim()
            requestBuilder.addHeader("X-DigiWear-Key", keyClean)
            requestBuilder.addHeader("Authorization", "Bearer $keyClean")

            // Replay-Proof HMAC-SHA256 App Attestation Token
            val timestamp = System.currentTimeMillis()
            val nonce = java.util.UUID.randomUUID().toString()
            val hmacKey = SecretKeySpec(keyClean.toByteArray(Charsets.UTF_8), "HmacSHA256")
            val token = DuelSecurityEngine.signDuelReceipt("$timestamp:$nonce", hmacKey)

            requestBuilder.addHeader("X-DigiWear-Timestamp", timestamp.toString())
            requestBuilder.addHeader("X-DigiWear-Nonce", nonce)
            requestBuilder.addHeader("X-DigiWear-Token", token)
        }

        try {
            webSocket = client.newWebSocket(requestBuilder.build(), object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.i(TAG, "Connected to Live Duel Relay: $normalizedUrl")
                    _lobbyState.value = LobbyState.CONNECTED
                    _statusMessage.value = "Connected to Live Relay Server"
                    onConnected?.invoke()
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleIncomingPacket(text)
                }

                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    ws.close(1000, null)
                    _lobbyState.value = LobbyState.DISCONNECTED
                    _statusMessage.value = "Disconnected from server"
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure: ${t.message}", t)
                    val errorDetail = when (response?.code) {
                        401 -> "401 Unauthorized: Invalid DigiWear Server Key."
                        429 -> "429 Too Many Requests: Rate limit exceeded."
                        404 -> "404 Not Found: Check relay server URL."
                        else -> t.message ?: "Connection failed"
                    }
                    _lobbyState.value = LobbyState.ERROR
                    _errorMessage.value = errorDetail
                    _statusMessage.value = "Failed: $errorDetail"
                }
            })
        } catch (e: Exception) {
            _lobbyState.value = LobbyState.ERROR
            _errorMessage.value = e.message ?: "Could not initiate connection"
            _statusMessage.value = "Error: ${_errorMessage.value}"
        }
    }

    fun createRoom(fighter: BattleFighter, tag: String) {
        activeFighter = fighter
        tamerTag = tag
        myKeyPair = DuelSecurityEngine.generateEcKeyPair()
        sessionKey = null
        _opponentFighter.value = null
        _opponentTamerTag.value = null

        val packet = DuelPacket(type = MultiplayerProtocol.TYPE_CREATE_ROOM)
        sendPacket(packet)
        _statusMessage.value = "Creating encrypted room..."
    }

    fun joinRoom(roomId: String, fighter: BattleFighter, tag: String) {
        val cleanRoomId = roomId.trim().uppercase()
        activeFighter = fighter
        tamerTag = tag
        myKeyPair = DuelSecurityEngine.generateEcKeyPair()
        sessionKey = null
        _opponentFighter.value = null
        _opponentTamerTag.value = null

        val packet = DuelPacket(
            type = MultiplayerProtocol.TYPE_JOIN_ROOM,
            roomId = cleanRoomId
        )
        sendPacket(packet)
        _statusMessage.value = "Joining room $cleanRoomId..."
    }

    fun leaveRoom() {
        val packet = DuelPacket(type = MultiplayerProtocol.TYPE_LEAVE_ROOM)
        sendPacket(packet)
        _currentRoomId.value = null
        _lobbyState.value = if (webSocket != null) LobbyState.CONNECTED else LobbyState.DISCONNECTED
        _statusMessage.value = "Left room"
        _opponentFighter.value = null
        _opponentTamerTag.value = null
        sessionKey = null
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "User disconnect")
        } catch (_: Exception) {}
        webSocket = null
        _lobbyState.value = LobbyState.DISCONNECTED
        _currentRoomId.value = null
        _statusMessage.value = "Disconnected"
    }

    private fun handleIncomingPacket(text: String) {
        scope.launch {
            try {
                val packet = json.decodeFromString<DuelPacket>(text)
                when (packet.type) {
                    MultiplayerProtocol.TYPE_ROOM_CREATED -> {
                        _currentRoomId.value = packet.roomId
                        _isHost.value = true
                        _lobbyState.value = LobbyState.WAITING_FOR_OPPONENT
                        _statusMessage.value = "Room ${packet.roomId} open! Waiting for challenger..."
                    }

                    MultiplayerProtocol.TYPE_ROOM_JOINED -> {
                        _currentRoomId.value = packet.roomId
                        _isHost.value = false
                        _lobbyState.value = LobbyState.OPPONENT_FOUND
                        _statusMessage.value = "Joined room ${packet.roomId}! Negotiating keys..."
                        // Guest immediately sends KeyExchange to Host
                        sendKeyExchange()
                    }

                    MultiplayerProtocol.TYPE_PEER_JOINED -> {
                        _lobbyState.value = LobbyState.OPPONENT_FOUND
                        _statusMessage.value = "Challenger joined! Exchanging encryption keys..."
                        // Host sends KeyExchange to Guest
                        sendKeyExchange()
                    }

                    MultiplayerProtocol.TYPE_PEER_DISCONNECTED -> {
                        _lobbyState.value = if (_isHost.value) LobbyState.WAITING_FOR_OPPONENT else LobbyState.CONNECTED
                        _statusMessage.value = "Opponent disconnected"
                        _opponentFighter.value = null
                        _opponentTamerTag.value = null
                        sessionKey = null
                    }

                    MultiplayerProtocol.TYPE_RELAY -> {
                        handleRelayedPayload(packet.payload)
                    }

                    MultiplayerProtocol.TYPE_ERROR -> {
                        val errMsg = packet.message ?: packet.code ?: "Relay error"
                        _errorMessage.value = errMsg
                        _statusMessage.value = "Error: $errMsg"
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse incoming packet: ${e.message}")
            }
        }
    }

    private fun sendKeyExchange() {
        val kp = myKeyPair ?: return
        val ft = activeFighter ?: return

        val kex = KeyExchangePayload(
            publicKeyHex = DuelSecurityEngine.toHex(kp.public.encoded),
            tamerTag = tamerTag,
            speciesId = ft.speciesId
        )
        val kexJson = json.encodeToString(kex)
        val relayPacket = DuelPacket(
            type = MultiplayerProtocol.TYPE_RELAY,
            payload = json.encodeToString(DuelPacket(type = MultiplayerProtocol.TYPE_KEY_EXCHANGE, payload = kexJson))
        )
        sendPacket(relayPacket)
    }

    private fun handleRelayedPayload(payloadStr: String?) {
        if (payloadStr.isNullOrBlank()) return
        try {
            val innerPacket = json.decodeFromString<DuelPacket>(payloadStr)
            when (innerPacket.type) {
                MultiplayerProtocol.TYPE_KEY_EXCHANGE -> {
                    val kex = json.decodeFromString<KeyExchangePayload>(innerPacket.payload ?: return)
                    _opponentTamerTag.value = kex.tamerTag

                    // Derive AES-256 session key
                    val peerKeyBytes = DuelSecurityEngine.fromHex(kex.publicKeyHex)
                    val derivedKey = DuelSecurityEngine.deriveSharedAesKey(myKeyPair!!.private, peerKeyBytes)
                    sessionKey = derivedKey

                    // Now send fighter profile for zero-trust validation
                    sendFighterVerification()
                }

                MultiplayerProtocol.TYPE_FIGHTER_VERIFY -> {
                    val encrypted = json.decodeFromString<EncryptedDuelPayload>(innerPacket.payload ?: return)
                    val key = sessionKey ?: return
                    val decryptedBytes = DuelSecurityEngine.decryptAesGcm(key, encrypted)
                    val handshake = json.decodeFromString<FighterHandshakePayload>(String(decryptedBytes, Charsets.UTF_8))

                    // Zero-Trust Validation Check
                    val validation = DuelSecurityEngine.validateFighterIntegrity(handshake.fighter)
                    if (!validation.isValid) {
                        _lobbyState.value = LobbyState.ERROR
                        val reason = "Security Rejection: ${validation.violationReason}"
                        _errorMessage.value = reason
                        _statusMessage.value = reason
                        leaveRoom()
                        return
                    }

                    _opponentFighter.value = handshake.fighter
                    _lobbyState.value = LobbyState.DUEL_READY
                    _statusMessage.value = "Match Ready! Verified #${handshake.fighter.speciesId} ${handshake.fighter.name}"
                }

                MultiplayerProtocol.TYPE_COMMIT_ACTION -> {
                    val encrypted = json.decodeFromString<EncryptedDuelPayload>(innerPacket.payload ?: return)
                    val key = sessionKey ?: return
                    val decryptedBytes = DuelSecurityEngine.decryptAesGcm(key, encrypted)
                    val commitPayload = json.decodeFromString<DuelCommitmentPayload>(String(decryptedBytes, Charsets.UTF_8))
                    onOpponentCommitmentReceived?.invoke(commitPayload.round)
                }

                MultiplayerProtocol.TYPE_REVEAL_ACTION -> {
                    val encrypted = json.decodeFromString<EncryptedDuelPayload>(innerPacket.payload ?: return)
                    val key = sessionKey ?: return
                    val decryptedBytes = DuelSecurityEngine.decryptAesGcm(key, encrypted)
                    val revealPayload = json.decodeFromString<DuelRevealPayload>(String(decryptedBytes, Charsets.UTF_8))

                    // Validate reflex time
                    if (DuelSecurityEngine.validateReflexTime(revealPayload.reflexMs)) {
                        onOpponentActionReceived?.invoke(revealPayload.choice, revealPayload.reflexMs)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing relayed payload: ${e.message}", e)
        }
    }

    private fun sendFighterVerification() {
        val ft = activeFighter ?: return
        val key = sessionKey ?: return

        val handshake = FighterHandshakePayload(
            fighter = ft,
            integritySignatureHex = DuelSecurityEngine.signDuelReceipt("${ft.speciesId}:${ft.maxHp}", key)
        )
        val rawBytes = json.encodeToString(handshake).toByteArray(Charsets.UTF_8)
        val encrypted = DuelSecurityEngine.encryptAesGcm(key, rawBytes)

        val relayPacket = DuelPacket(
            type = MultiplayerProtocol.TYPE_RELAY,
            payload = json.encodeToString(DuelPacket(type = MultiplayerProtocol.TYPE_FIGHTER_VERIFY, payload = json.encodeToString(encrypted)))
        )
        sendPacket(relayPacket)
    }

    fun sendRoundCommitment(round: Int, commitmentHash: String) {
        val key = sessionKey ?: return
        val payload = DuelCommitmentPayload(round = round, commitmentHashHex = commitmentHash)
        val encrypted = DuelSecurityEngine.encryptAesGcm(key, json.encodeToString(payload).toByteArray(Charsets.UTF_8))

        val relayPacket = DuelPacket(
            type = MultiplayerProtocol.TYPE_RELAY,
            payload = json.encodeToString(DuelPacket(type = MultiplayerProtocol.TYPE_COMMIT_ACTION, payload = json.encodeToString(encrypted)))
        )
        sendPacket(relayPacket)
    }

    fun sendRoundReveal(round: Int, choice: Int, reflexMs: Long, salt: ByteArray) {
        val key = sessionKey ?: return
        val payload = DuelRevealPayload(
            round = round,
            choice = choice,
            reflexMs = reflexMs,
            saltHex = DuelSecurityEngine.toHex(salt)
        )
        val encrypted = DuelSecurityEngine.encryptAesGcm(key, json.encodeToString(payload).toByteArray(Charsets.UTF_8))

        val relayPacket = DuelPacket(
            type = MultiplayerProtocol.TYPE_RELAY,
            payload = json.encodeToString(DuelPacket(type = MultiplayerProtocol.TYPE_REVEAL_ACTION, payload = json.encodeToString(encrypted)))
        )
        sendPacket(relayPacket)
    }

    private fun sendPacket(packet: DuelPacket) {
        val jsonStr = json.encodeToString(packet)
        webSocket?.send(jsonStr)
    }
}
