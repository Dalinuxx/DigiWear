package com.digiwear.core.multiplayer

import com.digiwear.core.model.BattleFighter
import com.digiwear.core.model.ElementalAttribute
import com.digiwear.core.model.PetStage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuelSecurityEngineTest {

    @Test
    fun testValidFighterPassesIntegrityCheck() {
        // Agumon (ID 201, Child, Vaccine, BaseHp: 130, BasePower: 35)
        val legitFighter = BattleFighter(
            id = "tamer_1",
            speciesId = 201,
            name = "Agumon",
            stage = PetStage.CHILD,
            attribute = ElementalAttribute.VACCINE,
            maxHp = 140,
            currentHp = 140,
            basePower = 35,
            victories = 12,
            effort = 3,
            battleSeed = 0x4101
        )
        val result = DuelSecurityEngine.validateFighterIntegrity(legitFighter)
        assertTrue(result.violationReason ?: "", result.isValid)
    }

    @Test
    fun testHackedStatsFailIntegrityCheck() {
        // Agumon with 9999 HP (hacked)
        val hackedHp = BattleFighter(
            id = "cheater_1",
            speciesId = 201,
            name = "Agumon",
            stage = PetStage.CHILD,
            attribute = ElementalAttribute.VACCINE,
            maxHp = 9999,
            currentHp = 9999,
            basePower = 35,
            victories = 0,
            effort = 0,
            battleSeed = 0x4101
        )
        val hpResult = DuelSecurityEngine.validateFighterIntegrity(hackedHp)
        assertFalse(hpResult.isValid)
        assertTrue(hpResult.violationReason!!.contains("Illegal Max HP"))

        // Agumon with hacked Base Power (500 instead of 35)
        val hackedPower = hackedHp.copy(maxHp = 130, currentHp = 130, basePower = 500)
        val powerResult = DuelSecurityEngine.validateFighterIntegrity(hackedPower)
        assertFalse(powerResult.isValid)
        assertTrue(powerResult.violationReason!!.contains("Base power mismatch"))

        // Agumon with hacked Stage (Ultimate instead of Child)
        val hackedStage = hackedHp.copy(maxHp = 130, currentHp = 130, stage = PetStage.ULTIMATE)
        val stageResult = DuelSecurityEngine.validateFighterIntegrity(hackedStage)
        assertFalse(stageResult.isValid)
        assertTrue(stageResult.violationReason!!.contains("Stage mismatch"))
    }

    @Test
    fun testCommitRevealFairnessProtocol() {
        val salt = DuelSecurityEngine.generateRandomSalt()
        val choice = 1 // Heavy Burst
        val reflexMs = 340L

        val commitment = DuelSecurityEngine.computeCommitment(choice, reflexMs, salt)

        // Valid reveal matches commitment
        assertTrue(DuelSecurityEngine.verifyCommitment(commitment, choice, reflexMs, salt))

        // Tampered choice fails
        assertFalse(DuelSecurityEngine.verifyCommitment(commitment, 2, reflexMs, salt))

        // Tampered reflex timing fails
        assertFalse(DuelSecurityEngine.verifyCommitment(commitment, choice, 150L, salt))

        // Tampered salt fails
        val fakeSalt = DuelSecurityEngine.generateRandomSalt()
        assertFalse(DuelSecurityEngine.verifyCommitment(commitment, choice, reflexMs, fakeSalt))
    }

    @Test
    fun testTemporalReflexAntiCheat() {
        // Impossible superhuman reflex (< 120ms) is blocked
        assertFalse(DuelSecurityEngine.validateReflexTime(45L))
        assertFalse(DuelSecurityEngine.validateReflexTime(0L))
        assertFalse(DuelSecurityEngine.validateReflexTime(-10L))

        // Legitimate human reflex (120ms - 6000ms) passes
        assertTrue(DuelSecurityEngine.validateReflexTime(140L))
        assertTrue(DuelSecurityEngine.validateReflexTime(320L))
        assertTrue(DuelSecurityEngine.validateReflexTime(1200L))

        // AFK / timeout (> 6000ms) is blocked
        assertFalse(DuelSecurityEngine.validateReflexTime(7500L))
    }

    @Test
    fun testEphemeralEcdhAndAesGcmEncryptionRoundTrip() {
        // Host and Guest generate independent keypairs
        val hostKeyPair = DuelSecurityEngine.generateEcKeyPair()
        val guestKeyPair = DuelSecurityEngine.generateEcKeyPair()

        // Derive shared AES keys via ECDH
        val hostSharedKey = DuelSecurityEngine.deriveSharedAesKey(
            hostKeyPair.private,
            guestKeyPair.public.encoded
        )
        val guestSharedKey = DuelSecurityEngine.deriveSharedAesKey(
            guestKeyPair.private,
            hostKeyPair.public.encoded
        )

        // Keys derived on both sides must match identically
        assertArrayEquals(hostSharedKey.encoded, guestSharedKey.encoded)

        // Host encrypts a confidential battle command
        val plaintext = "ATTACK_ROUND:1:SPECIAL_BLAST".toByteArray(Charsets.UTF_8)
        val encrypted = DuelSecurityEngine.encryptAesGcm(hostSharedKey, plaintext)

        // Guest decrypts the ciphertext
        val decrypted = DuelSecurityEngine.decryptAesGcm(guestSharedKey, encrypted)
        assertEquals(String(plaintext, Charsets.UTF_8), String(decrypted, Charsets.UTF_8))
    }
}
