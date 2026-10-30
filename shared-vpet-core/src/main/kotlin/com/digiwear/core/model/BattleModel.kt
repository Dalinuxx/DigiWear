package com.digiwear.core.model

import kotlinx.serialization.Serializable

/**
 * Distinct canonical battle modes supported across DigiWear and Companion App.
 */
@Serializable
enum class BattleMode(
    val code: String,
    val displayName: String,
    val shortLabel: String,
    val description: String
) {
    ADVENTURE(
        code = "adventure",
        displayName = "Adventure Colosseum",
        shortLabel = "ADVENTURE",
        description = "30-Sector Pedometer Campaign & Boss Encounters"
    ),
    WILD_SCAN(
        code = "wild_scan",
        displayName = "Wild Signal Scanner",
        shortLabel = "SCAN WILD",
        description = "Scan NFC Tags or Barcodes/QR to materialize Wild Digimon"
    ),
    NFC_TAP_DUEL(
        code = "nfc_duel",
        displayName = "NFC Tap-to-Duel",
        shortLabel = "NFC DUEL",
        description = "Watch-to-Watch or Watch-to-Phone P2P NDEF/HCE Duel"
    ),
    ACOM_DCOM_WIFICOM(
        code = "acom_wificom",
        displayName = "A-Com / D-Com / WiFiCom",
        shortLabel = "A-COM / WIFI",
        description = "DigiROM V1-XXXX / 0nl1ne Transceiver & NFC Phone Relay"
    ),
    ONLINE_DUEL_LOBBY(
        code = "online_duel",
        displayName = "Live Real-Time Multiplayer Duel Hub",
        shortLabel = "LIVE DUEL",
        description = "Encrypted Real-Time WebSocket / LAN Duel Matchmaking"
    );

    companion object {
        fun fromCode(code: String): BattleMode =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: NFC_TAP_DUEL
    }
}

/**
 * Combatant representation for deterministic offline battle simulation.
 */
@Serializable
data class BattleFighter(
    val id: String,
    val speciesId: Int,
    val name: String,
    val stage: PetStage,
    val attribute: ElementalAttribute,
    val maxHp: Int,
    val currentHp: Int,
    val basePower: Int,
    val victories: Int,
    val effort: Int,
    val battleSeed: Int,
    val strength: Int = 4,
    val weight: Int = 20
)

/**
 * Individual turn outcome within a combat round.
 */
@Serializable
data class BattleTurn(
    val round: Int,
    val attackerIndex: Int,
    val attackerName: String,
    val defenderIndex: Int,
    val defenderName: String,
    val damage: Int,
    val defenderRemainingHp: Int,
    val isAdvantage: Boolean,
    val isCritical: Boolean,
    val attackType: String = "SINGLE", // SINGLE, DOUBLE, MEGA_HIT
    val hitRatePercent: Int = 75,
    val effectivePower: Int = damage,
    val moveName: String = ""
)

/**
 * Complete outcome of a deterministic 10-round combat simulation.
 */
@Serializable
data class BattleResult(
    val fighterA: BattleFighter,
    val fighterB: BattleFighter,
    val turns: List<BattleTurn>,
    val totalRounds: Int,
    val winnerIndex: Int, // 0 = Fighter A, 1 = Fighter B, -1 = Draw
    val fighterAFinalHp: Int,
    val fighterBFinalHp: Int,
    val battleMode: BattleMode = BattleMode.NFC_TAP_DUEL,
    val fighterAEffectivePower: Int = fighterA.basePower,
    val fighterBEffectivePower: Int = fighterB.basePower,
    val attributeMultiplierA: Float = 1.0f,
    val attributeMultiplierB: Float = 1.0f,
    val formulaSummary: String = ""
) {
    val isDraw: Boolean
        get() = winnerIndex == -1

    val winnerName: String?
        get() = when (winnerIndex) {
            0 -> fighterA.name
            1 -> fighterB.name
            else -> null
        }
}
