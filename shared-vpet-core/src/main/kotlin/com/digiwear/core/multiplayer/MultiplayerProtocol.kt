package com.digiwear.core.multiplayer

import com.digiwear.core.model.BattleFighter
import kotlinx.serialization.Serializable

/**
 * Single source of truth for the DigiWear Live Multiplayer Protocol.
 * Supports zero-trust, end-to-end encrypted peer-to-peer real-time duels.
 */
object MultiplayerProtocol {

    /**
     * Default placeholder URL for the self-hostable relay server.
     * Tamers can override this via local.properties, build-time environment variables,
     * or at runtime through the Companion App Live Duel Hub settings.
     */
    const val DEFAULT_PLACEHOLDER_RELAY_URL = "wss://relay.digiwear-placeholder.net:8080"
    const val DEFAULT_PLACEHOLDER_SERVER_KEY = ""
    const val DEFAULT_AOSP_LAN_PORT = 45825

    // Envelope Action Types
    const val TYPE_CONNECTED = "CONNECTED"
    const val TYPE_CREATE_ROOM = "CREATE_ROOM"
    const val TYPE_ROOM_CREATED = "ROOM_CREATED"
    const val TYPE_JOIN_ROOM = "JOIN_ROOM"
    const val TYPE_ROOM_JOINED = "ROOM_JOINED"
    const val TYPE_PEER_JOINED = "PEER_JOINED"
    const val TYPE_PEER_DISCONNECTED = "PEER_DISCONNECTED"
    const val TYPE_KEY_EXCHANGE = "KEY_EXCHANGE"
    const val TYPE_FIGHTER_VERIFY = "FIGHTER_VERIFY"
    const val TYPE_COMMIT_ACTION = "COMMIT_ACTION"
    const val TYPE_REVEAL_ACTION = "REVEAL_ACTION"
    const val TYPE_RELAY = "RELAY"
    const val TYPE_LEAVE_ROOM = "LEAVE_ROOM"
    const val TYPE_ERROR = "ERROR"

    // Error Codes
    const val ERR_ROOM_NOT_FOUND = "ROOM_NOT_FOUND"
    const val ERR_ROOM_FULL = "ROOM_FULL"
    const val ERR_AUTH_FAILED = "AUTH_FAILED"
    const val ERR_STAT_TAMPERING = "STAT_TAMPERING"
    const val ERR_COMMITMENT_MISMATCH = "COMMITMENT_MISMATCH"
    const val ERR_TEMPORAL_VIOLATION = "TEMPORAL_VIOLATION"
}

@Serializable
data class DuelPacket(
    val type: String,
    val roomId: String? = null,
    val role: String? = null,
    val fromRole: String? = null,
    val payload: String? = null,
    val code: String? = null,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class KeyExchangePayload(
    val publicKeyHex: String,
    val tamerTag: String,
    val speciesId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class EncryptedDuelPayload(
    val ivHex: String,
    val ciphertextHex: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class FighterHandshakePayload(
    val fighter: BattleFighter,
    val integritySignatureHex: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DuelCommitmentPayload(
    val round: Int,
    val commitmentHashHex: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DuelRevealPayload(
    val round: Int,
    val choice: Int, // 0 = Speed Attack, 1 = Heavy Burst, 2 = Guard/Counter
    val reflexMs: Long,
    val saltHex: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DuelReceipt(
    val duelId: String,
    val winnerTamer: String,
    val loserTamer: String,
    val winnerSpeciesId: Int,
    val loserSpeciesId: Int,
    val totalRounds: Int,
    val duelHash: String,
    val hostSignature: String,
    val guestSignature: String,
    val timestamp: Long = System.currentTimeMillis()
)
