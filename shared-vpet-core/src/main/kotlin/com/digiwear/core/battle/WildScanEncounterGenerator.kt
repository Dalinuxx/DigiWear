package com.digiwear.core.battle

import com.digiwear.core.codec.CommCodeCodec
import com.digiwear.core.model.BattleFighter
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Result of scanning a physical NFC Tag UID, UPC/EAN Barcode, QR Code, or RF Signal string.
 */
@Serializable
data class WildEncounter(
    val rawCode: String,
    val signalType: String,         // "BARCODE_EAN_UPC", "NFC_TAG_UID", "QR_MATRIX", "RF_SIGNAL"
    val frequencyLabel: String,     // e.g., "13.56 MHz ISO-14443A" or "850 nm LASER EAN-13"
    val habitatZone: String,        // e.g., "File Island — Gear Savannah"
    val environmentHazard: String,  // "none", "glitch_fog", "subzero_frost", "pyroclastic_flow", "dark_gravity"
    val isRareAnomaly: Boolean,     // True if high-tier Boss anomaly triggered
    val wildFighter: BattleFighter,
    val digiRomCode: String,        // V1-XXXX-XXXX-XXXX code for Watch/Phone relay
    val commCode128: String         // 26-char Base32 CommCode for NFC NDEF relay
) {
    val species: PetSpecies
        get() = PetSpecies.getById(wildFighter.speciesId) ?: PetSpecies.SPECIES_REGISTRY.values.first()

    val fighter: BattleFighter
        get() = wildFighter
}

typealias WildScanEncounter = WildEncounter

/**
 * Deterministic Barcode / NFC Tag Wild Digimon Generator (inspired by Bandai D-Scanner & Barcode Battler).
 * Any household barcode (UPC-A, EAN-13, EAN-8), QR string, or NFC Tag UID deterministically spawns a
 * specific Wild Digimon encounter scaled to the player's stage, with a 15% chance of an anomalous Boss encounter.
 */
object WildScanEncounterGenerator {

    private val HABITAT_ZONES = listOf(
        "File Island — Native Forest" to "none",
        "File Island — Gear Savannah" to "none",
        "File Island — Freeze Land" to "subzero_frost",
        "File Island — Lava Caldera" to "pyroclastic_flow",
        "Folder Continent — Ancient Ruins" to "glitch_fog",
        "Net Ocean — Abyssal Trench" to "subzero_frost",
        "Dark Area — Nightmare Citadel" to "dark_gravity",
        "Kernel Core — Yggdrasil Node" to "cosmic_nihilism"
    )

    /**
     * Curated sample barcodes/NFC tags for one-tap testing or quick scanning in the UI.
     */
    val PRESET_SAMPLE_CODES: List<Pair<String, String>> = listOf(
        "4902425615042" to "Bandai 1997 Original V-Pet Box (EAN-13)",
        "4902425719818" to "Digimon Pendulum Nature Spirits (EAN-13)",
        "4543112085627" to "D-Scanner Barcode Matrix V1.0 (EAN-13)",
        "04:A3:7F:92:C1:88:00" to "Transit / Keycard NFC UID (ISO-14443A)",
        "04:E9:11:5B:3D:40:80" to "Arcade IC Card NFC Tag (FeliCa/MIFARE)",
        "QR:DIGIWEAR-OMEGA-CORE-99" to "Kernel Anomaly QR Beacon"
    )

    val PRESET_BARCODES: List<Pair<String, String>> = PRESET_SAMPLE_CODES

    fun generateFromScan(
        rawScanData: String,
        scanSource: String,
        playerStage: PetStage = PetStage.ADULT
    ): WildEncounter = generateFromScan(
        rawScanInput = rawScanData,
        playerStage = playerStage
    )

    /**
     * Deterministically generates a [WildEncounter] from any scanned barcode, QR code, or NFC UID string.
     */
    fun generateFromScan(
        rawScanInput: String,
        playerStage: PetStage = PetStage.ADULT
    ): WildEncounter {
        val cleaned = rawScanInput.trim().ifEmpty { "4902425615042" }

        // Compute deterministic 64-bit FNV-1a style hash
        var hash = -0x340d631b7bdddcdbL
        for (ch in cleaned) {
            hash = hash xor ch.code.toLong()
            hash *= 0x100000001b3L
        }
        val positiveHash = hash ushr 1

        val signalType = when {
            cleaned.all { it.isDigit() } && cleaned.length in 8..14 -> "BARCODE_EAN_UPC"
            cleaned.contains(":") && cleaned.length >= 11 -> "NFC_TAG_UID"
            cleaned.startsWith("QR:", ignoreCase = true) || cleaned.startsWith("http", ignoreCase = true) -> "QR_MATRIX"
            else -> "RF_SIGNAL"
        }

        val frequencyLabel = when (signalType) {
            "BARCODE_EAN_UPC" -> "650nm LASER • EAN/UPC (${cleaned.length}-DIGIT)"
            "NFC_TAG_UID" -> "13.56 MHz RF • NFC UID"
            "QR_MATRIX" -> "OPTICAL MATRIX • 2D QR PAYLOAD"
            else -> "SUB-GHZ DIGITAL SIGNAL"
        }

        // 18% chance of a Rare Boss Anomaly (+1 Stage)
        val isRareAnomaly = (positiveHash % 100L) < 18L || cleaned.contains("OMEGA", ignoreCase = true)

        val targetStage = when {
            isRareAnomaly -> when (playerStage) {
                PetStage.BABY_I, PetStage.BABY_II -> PetStage.CHILD
                PetStage.CHILD -> PetStage.ADULT
                PetStage.ADULT -> PetStage.PERFECT
                PetStage.PERFECT, PetStage.ULTIMATE -> PetStage.ULTIMATE
            }
            else -> when (playerStage) {
                PetStage.BABY_I, PetStage.BABY_II -> PetStage.CHILD
                else -> playerStage
            }
        }

        val candidates = PetSpecies.SPECIES_BY_STAGE[targetStage]
            ?.ifEmpty { null }
            ?: PetSpecies.SORTED_SPECIES

        val chosenSpecies = candidates[(positiveHash % candidates.size).toInt()]
        val zonePair = HABITAT_ZONES[((positiveHash shr 8) % HABITAT_ZONES.size).toInt()]

        val effort = if (isRareAnomaly) 4 else ((positiveHash shr 16) % 5).toInt()
        val victories = if (isRareAnomaly) 25 else ((positiveHash shr 20) % 18).toInt()
        val battleSeed = ((positiveHash shr 24) and 0xFFFFL).toInt()
        val hpBoost = if (isRareAnomaly) 1.15f else 1.0f
        val wildHp = (chosenSpecies.baseHp * hpBoost).toInt()
        val wildPower = (chosenSpecies.basePower * (if (isRareAnomaly) 1.10f else 1.0f)).toInt()

        val fighter = BattleFighter(
            id = "wild_${chosenSpecies.id}_${battleSeed}",
            speciesId = chosenSpecies.id,
            name = if (isRareAnomaly) "Wild ${chosenSpecies.name} α" else "Wild ${chosenSpecies.name}",
            stage = chosenSpecies.stage,
            attribute = chosenSpecies.attribute,
            maxHp = wildHp,
            currentHp = wildHp,
            basePower = wildPower,
            victories = victories,
            effort = effort,
            battleSeed = battleSeed,
            strength = if (isRareAnomaly) 4 else 3,
            weight = chosenSpecies.baselineWeight + (effort * 2)
        )

        val digiRom = CommCodeCodec.encodeDigiRom(
            monsterId = chosenSpecies.id,
            stage = chosenSpecies.stage,
            attribute = chosenSpecies.attribute,
            basePowerHp = wildPower,
            battleRngSeed = battleSeed
        )

        val commCode = CommCodeCodec.encode128(
            monsterId = chosenSpecies.id,
            stage = chosenSpecies.stage,
            attribute = chosenSpecies.attribute,
            basePowerHp = wildPower,
            historicalVictories = victories,
            trainingEffort = effort,
            battleRngSeed = battleSeed
        )

        return WildEncounter(
            rawCode = cleaned,
            signalType = signalType,
            frequencyLabel = frequencyLabel,
            habitatZone = zonePair.first,
            environmentHazard = zonePair.second,
            isRareAnomaly = isRareAnomaly,
            wildFighter = fighter,
            digiRomCode = digiRom,
            commCode128 = commCode
        )
    }
}
