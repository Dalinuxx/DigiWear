package com.digiwear.companion

import com.digiwear.companion.data.SyncedPetEntity
import com.digiwear.companion.ui.extractCommCodeFromText
import com.digiwear.core.codec.CommCodeCodec
import com.digiwear.core.model.ElementalAttribute
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import com.digiwear.core.model.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DComReorganizationAndStasisTest {

    @Test
    fun testColdSleepStasisPersistenceAcrossDomainAndEntity() {
        // Given a pet frozen in cold sleep stasis
        val frozenPet = PetState(
            speciesId = 201, // Agumon
            currentHp = 100,
            maxHp = 100,
            hunger = 4,
            strength = 4,
            weight = 20,
            isFrozen = true, // Cold Sleep Active!
            isSleeping = false,
            lightsOff = false
        )

        // Convert to Room SyncedPetEntity
        val entity = SyncedPetEntity.fromDomain(frozenPet)
        assertTrue("Entity must preserve isFrozen stasis flag", entity.isFrozen)

        // Convert back from Room SyncedPetEntity to domain PetState
        val restored = entity.toDomain()
        assertTrue("Restored PetState must retain isFrozen stasis across tab switches", restored.isFrozen)
        assertEquals(frozenPet.speciesId, restored.speciesId)
        assertEquals(frozenPet.weight, restored.weight)
    }

    @Test
    fun testCameraScannerExtractsDigiRomFromText() {
        val validRom = CommCodeCodec.encodeDigiRom(
            monsterId = 401, // MetalGreymon
            stage = PetStage.PERFECT,
            attribute = ElementalAttribute.VACCINE,
            basePowerHp = 220,
            battleRngSeed = 0x4101
        )
        // A DigiROM string embedded inside plain text from Discord / chat / forum
        val rawChatText = "Hey bro, here is my MetalGreymon DigiROM: $validRom let's duel!"
        val extracted = extractCommCodeFromText(rawChatText)
        assertNotNull("Should extract valid DigiROM pattern from surrounding chat text", extracted)
        assertEquals(validRom, extracted)

        val decoded = CommCodeCodec.decodeAuto(extracted!!)
        assertEquals(401, decoded.monsterId)
    }

    @Test
    fun testCameraScannerExtractsBase32CommCodeFromText() {
        val base32 = CommCodeCodec.encode128(
            monsterId = 301,
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.VACCINE,
            basePowerHp = 200,
            historicalVictories = 5,
            trainingEffort = 4,
            battleRngSeed = 0x1234
        )
        val formatted = CommCodeCodec.formatWithHyphens(base32)
        val sampleText = "Scanned screen text: Code [$formatted] match ready."

        val extracted = extractCommCodeFromText(sampleText)
        assertNotNull("Should extract 26-char base32 code from text", extracted)
        val decoded = CommCodeCodec.decodeAuto(extracted!!)
        assertEquals(301, decoded.monsterId)
    }
}
