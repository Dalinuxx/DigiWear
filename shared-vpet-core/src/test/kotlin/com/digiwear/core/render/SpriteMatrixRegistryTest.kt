package com.digiwear.core.render

import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.SpriteFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpriteMatrixRegistryTest {

    private val validChars = setOf('.', '#', 'B', 'K', 'E', 'W', 'T', 'S', 'R', 'G', 'C')

    @Test
    fun testAll550SpeciesHaveValidNonHollow16x16Matrices() {
        assertEquals(550, PetSpecies.SPECIES_REGISTRY.size)
        for ((speciesId, species) in PetSpecies.SPECIES_REGISTRY) {
            val matrix = SpriteMatrixRegistry.getMatrix(speciesId, SpriteFrame.IDLE_A)
            assertEquals("Matrix row count for species $speciesId (${species.name}) must be 16", 16, matrix.size)
            var outlineCount = 0
            var fillCount = 0
            var dotCount = 0
            for (r in 0 until 16) {
                val row = matrix[r]
                assertEquals("Row $r in species $speciesId (${species.name}) must be 16 cols wide", 16, row.length)
                for (c in 0 until 16) {
                    val ch = row[c]
                    assertTrue(
                        "Char '$ch' at ($r,$c) in species $speciesId (${species.name}) must be valid dot character",
                        ch in validChars
                    )
                    if (ch != '.') dotCount++
                    if (ch == 'B' || ch == 'K') outlineCount++
                    if (ch in setOf('#', 'S', 'T', 'R', 'G', 'C', 'W')) fillCount++
                }
            }
            assertTrue("Species $speciesId (${species.name}) must have at least 20 active pixels (had $dotCount)", dotCount >= 20)
            assertTrue("Species $speciesId (${species.name}) must not be a solid 256-pixel box (had $dotCount)", dotCount <= 245)
            assertTrue("Species $speciesId (${species.name}) must have outline pixels", outlineCount > 0)
            assertTrue("Species $speciesId (${species.name}) must have multi-tone interior fill pixels", fillCount > 0)
        }
    }

    @Test
    fun testZeroDuplicateMatricesAcrossAll550Species() {
        val seen = mutableMapOf<String, Int>()
        val duplicates = mutableListOf<String>()
        for ((speciesId, species) in PetSpecies.SPECIES_REGISTRY) {
            val sig = SpriteMatrixRegistry.getMatrix(speciesId, SpriteFrame.IDLE_A).joinToString("\n")
            val existing = seen[sig]
            if (existing != null) {
                duplicates.add("Species $speciesId (${species.name}) shares matrix with species $existing (${PetSpecies.getById(existing).name})")
            }
            seen[sig] = speciesId
        }
        if (duplicates.isNotEmpty()) {
            println("DUPLICATES FOUND (${duplicates.size}):\n" + duplicates.joinToString("\n"))
        }
        assertTrue("Found ${duplicates.size} duplicate matrices:\n" + duplicates.joinToString("\n"), duplicates.isEmpty())
        assertEquals(550, seen.size)
    }

    @Test
    fun testZeroDuplicatePalettesAcrossAll550Species() {
        val seen = mutableMapOf<SpritePalette, Int>()
        for ((speciesId, species) in PetSpecies.SPECIES_REGISTRY) {
            val palette = SpriteMatrixRegistry.getPalette(speciesId)
            assertNotNull("Palette for species $speciesId must not be null", palette)
            assertEquals("Alpha channel of primary must be 0xFF", 0xFF, (palette.primary ushr 24) and 0xFF)
            assertEquals("Alpha channel of secondary must be 0xFF", 0xFF, (palette.secondary ushr 24) and 0xFF)
            assertEquals("Alpha channel of tertiary must be 0xFF", 0xFF, (palette.tertiary ushr 24) and 0xFF)
            val existing = seen[palette]
            assertTrue(
                "Species $speciesId (${species.name}) shares identical palette with species $existing",
                existing == null
            )
            seen[palette] = speciesId
        }
        assertEquals(550, seen.size)
    }

    @Test
    fun testAll12FramesProduceValidAndDifferentiatedAnimations() {
        for ((speciesId, species) in PetSpecies.SPECIES_REGISTRY) {
            val frames = SpriteFrame.entries.map { frame ->
                val m = SpriteMatrixRegistry.getMatrix(speciesId, frame)
                assertEquals(16, m.size)
                m.forEach { assertEquals(16, it.length) }
                m.joinToString("\n")
            }
            assertNotEquals("IDLE_A and IDLE_B must differ for ${species.name}", frames[0], frames[1])
            assertNotEquals("WALK_A and WALK_B must differ for ${species.name}", frames[2], frames[3])
            assertNotEquals("FEED_START and ATTACK_RELEASE must differ for ${species.name}", frames[4], frames[10])
            assertNotEquals("SLEEP_A and SLEEP_B must differ for ${species.name}", frames[6], frames[7])
            assertNotEquals("IDLE_A and REFUSAL must differ for ${species.name}", frames[0], frames[8])
            assertNotEquals("REFUSAL and ATTACK_CHARGE must differ for ${species.name}", frames[8], frames[9])
            assertNotEquals("ATTACK_CHARGE and ATTACK_RELEASE must differ for ${species.name}", frames[9], frames[10])
        }
        // Verify Botamon (#101) INJURED and SLEEP frames close both upper ('W') and lower ('T') eye pixels
        val botamonInjured = SpriteMatrixRegistry.getMatrix(101, SpriteFrame.INJURED).joinToString("\n")
        val botamonIdleB = SpriteMatrixRegistry.getMatrix(101, SpriteFrame.IDLE_B).joinToString("\n")
        assertNotEquals("Botamon INJURED must differ from IDLE_B", botamonIdleB, botamonInjured)
        assertTrue("Botamon INJURED should squint 'W' and 'T' eye pixels", !botamonInjured.contains('W') && !botamonInjured.contains('T'))
    }

    @Test
    fun testSleepAndInjuredFramesPreserveWhiteBodyFill() {
        val whiteBodiedIds = listOf(102, 105, 106, 232, 307, 338, 427, 510)
        for (speciesId in whiteBodiedIds) {
            val idleW = SpriteMatrixRegistry.getMatrix(speciesId, SpriteFrame.IDLE_A)
                .sumOf { row -> row.count { it == 'W' } }
            if (idleW >= 6) {
                val sleepW = SpriteMatrixRegistry.getMatrix(speciesId, SpriteFrame.SLEEP_A)
                    .sumOf { row -> row.count { it == 'W' } }
                val injuredW = SpriteMatrixRegistry.getMatrix(speciesId, SpriteFrame.INJURED)
                    .sumOf { row -> row.count { it == 'W' } }
                assertTrue(
                    "Species $speciesId should preserve white body fill during SLEEP_A (idleW=$idleW, sleepW=$sleepW)",
                    sleepW >= idleW / 2
                )
                assertTrue(
                    "Species $speciesId should preserve white body fill during INJURED (idleW=$idleW, injuredW=$injuredW)",
                    injuredW >= idleW / 2
                )
            }
        }
    }

    @Test
    fun testAmbientModeOprUnder15PercentForAll550Species() {
        for ((speciesId, species) in PetSpecies.SPECIES_REGISTRY) {
            for (frame in SpriteFrame.entries) {
                val opr = SpriteMatrixRegistry.calculateAmbientOpr(speciesId, frame)
                assertTrue(
                    "Ambient OPR for $speciesId (${species.name}) frame $frame was $opr (must be <= 0.15f)",
                    opr in 0.01f..0.15f
                )
            }
        }
    }
}
