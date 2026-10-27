package com.digiwear.core

import com.digiwear.core.battle.DeterministicBattleSolver
import com.digiwear.core.battle.SignatureAttackRegistry
import com.digiwear.core.model.BattleFighter
import com.digiwear.core.model.ElementalAttribute
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicBattleSolverTest {

    @Test
    fun testBattleSymmetryAndDeterminism() {
        val fighterA = BattleFighter(
            id = "fighter_a",
            speciesId = 301, // Greymon
            name = "Greymon",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.VACCINE,
            maxHp = 240,
            currentHp = 240,
            basePower = 65,
            victories = 10,
            effort = 3,
            battleSeed = 0x1337
        )

        val fighterB = BattleFighter(
            id = "fighter_b",
            speciesId = 303, // Devimon
            name = "Devimon",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.VIRUS,
            maxHp = 230,
            currentHp = 230,
            basePower = 68,
            victories = 8,
            effort = 2,
            battleSeed = 0xBEEF
        )

        val result1 = DeterministicBattleSolver.resolveBattle(fighterA, fighterB)
        val result2 = DeterministicBattleSolver.resolveBattle(fighterA, fighterB)

        // Multiple runs with same seeds must produce identical turn count, damage, and winner
        assertEquals(result1.winnerIndex, result2.winnerIndex)
        assertEquals(result1.totalRounds, result2.totalRounds)
        assertEquals(result1.turns.size, result2.turns.size)

        for (i in result1.turns.indices) {
            val t1 = result1.turns[i]
            val t2 = result2.turns[i]
            assertEquals(t1.damage, t2.damage)
            assertEquals(t1.defenderRemainingHp, t2.defenderRemainingHp)
        }
    }

    @Test
    fun testVaccineOvercomesVirusAdvantage() {
        // Vaccine vs Virus multiplier is 1.2x
        val mult = ElementalAttribute.VACCINE.damageMultiplierAgainst(ElementalAttribute.VIRUS)
        assertEquals(1.2f, mult, 0.001f)

        // Virus vs Data multiplier is 1.2x
        val mult2 = ElementalAttribute.VIRUS.damageMultiplierAgainst(ElementalAttribute.DATA)
        assertEquals(1.2f, mult2, 0.001f)

        // Data vs Vaccine multiplier is 1.2x
        val mult3 = ElementalAttribute.DATA.damageMultiplierAgainst(ElementalAttribute.VACCINE)
        assertEquals(1.2f, mult3, 0.001f)
    }

    @Test
    fun testMax10RoundsEnforced() {
        // High HP, very low power fighters
        val tankA = BattleFighter(
            id = "tank_a",
            speciesId = 301,
            name = "Tank A",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.FREE,
            maxHp = 9999,
            currentHp = 9999,
            basePower = 1,
            victories = 0,
            effort = 0,
            battleSeed = 100
        )
        val tankB = BattleFighter(
            id = "tank_b",
            speciesId = 302,
            name = "Tank B",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.FREE,
            maxHp = 9999,
            currentHp = 9999,
            basePower = 1,
            victories = 0,
            effort = 0,
            battleSeed = 200
        )

        val result = DeterministicBattleSolver.resolveBattle(tankA, tankB)
        assertTrue("Rounds must not exceed 10", result.totalRounds <= 10)
    }

    @Test
    fun testAttackerCritBoostIncreasesPlayerDamageBy25Percent() {
        val fighterA = BattleFighter(
            id = "player",
            speciesId = 301,
            name = "Greymon",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.VACCINE,
            maxHp = 500,
            currentHp = 500,
            basePower = 100,
            victories = 10,
            effort = 4,
            battleSeed = 0x1234
        )
        val fighterB = BattleFighter(
            id = "opponent",
            speciesId = 303,
            name = "Devimon",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.VIRUS,
            maxHp = 500,
            currentHp = 500,
            basePower = 80,
            victories = 5,
            effort = 2,
            battleSeed = 0x5678
        )

        val normalResult = DeterministicBattleSolver.resolveBattle(fighterA, fighterB, attackerCritBoost = false)
        val critResult = DeterministicBattleSolver.resolveBattle(fighterA, fighterB, attackerCritBoost = true)

        val firstPlayerTurnNormal = normalResult.turns.first { it.attackerIndex == 0 }
        val firstPlayerTurnCrit = critResult.turns.first { it.attackerIndex == 0 }

        assertTrue(
            "Crit boost damage (${firstPlayerTurnCrit.damage}) must exceed normal damage (${firstPlayerTurnNormal.damage})",
            firstPlayerTurnCrit.damage > firstPlayerTurnNormal.damage
        )
        assertEquals(
            firstPlayerTurnNormal.damage * 1.25f,
            firstPlayerTurnCrit.damage.toFloat(),
            2.0f
        )
    }

    @Test
    fun testEnvironmentalWeatherHazardsModifyElementalDamage() {
        val virusFighter = BattleFighter(
            id = "virus",
            speciesId = 303,
            name = "Devimon",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.VIRUS,
            maxHp = 999,
            currentHp = 999,
            basePower = 100,
            victories = 10,
            effort = 4,
            battleSeed = 0x1111
        )
        val vaccineFighter = BattleFighter(
            id = "vaccine",
            speciesId = 301,
            name = "Greymon",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.VACCINE,
            maxHp = 999,
            currentHp = 999,
            basePower = 100,
            victories = 10,
            effort = 4,
            battleSeed = 0x2222
        )

        val clearRes = DeterministicBattleSolver.resolveBattle(virusFighter, vaccineFighter, environmentEffect = "none")
        val glitchFogRes = DeterministicBattleSolver.resolveBattle(virusFighter, vaccineFighter, environmentEffect = "glitch_fog")
        val cosmicRes = DeterministicBattleSolver.resolveBattle(virusFighter, vaccineFighter, environmentEffect = "cosmic_nihilism")

        val virusDmgClear = clearRes.turns.first { it.attackerIndex == 0 }.damage
        val virusDmgGlitch = glitchFogRes.turns.first { it.attackerIndex == 0 }.damage
        val virusDmgCosmic = cosmicRes.turns.first { it.attackerIndex == 0 }.damage

        assertTrue("glitch_fog must boost Virus damage ($virusDmgGlitch > $virusDmgClear)", virusDmgGlitch > virusDmgClear)
        assertTrue("cosmic_nihilism must boost damage ($virusDmgCosmic > $virusDmgClear)", virusDmgCosmic > virusDmgClear)
    }

    @Test
    fun testAll550SpeciesHaveCanonicalSignatureAttacks() {
        val roster = PetSpecies.SPECIES_REGISTRY
        assertEquals(550, roster.size)
        for ((speciesId, species) in roster) {
            assertTrue(
                "Species #$speciesId (${species.name}) must have an explicit canonical signature attack",
                SignatureAttackRegistry.hasCanonicalAttack(speciesId)
            )
            val attack = SignatureAttackRegistry.getAttackName(speciesId)
            assertTrue(
                "Species #$speciesId (${species.name}) must not use generic fallback",
                attack.isNotBlank()
            )
        }
        // Specific canonical checks
        assertEquals("Transcendent Sword", SignatureAttackRegistry.getAttackName(510))
        assertEquals("Trump Sword", SignatureAttackRegistry.getAttackName(509))
        assertEquals("Quo Vadis", SignatureAttackRegistry.getAttackName(516))
        assertEquals("Giga Death", SignatureAttackRegistry.getAttackName(513))
    }

    @Test
    fun testStrengthHeartsAndColosseumSpaceSeparatedEnvironmentEffects() {
        val weakFighter = BattleFighter(
            id = "weak",
            speciesId = 301,
            name = "Greymon",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.VACCINE,
            maxHp = 500,
            currentHp = 500,
            basePower = 100,
            victories = 0,
            effort = 0,
            strength = 0,
            battleSeed = 0x1111
        )
        val strongFighter = weakFighter.copy(strength = 4)
        val dummy = BattleFighter(
            id = "dummy",
            speciesId = 303,
            name = "Devimon",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.DATA,
            maxHp = 500,
            currentHp = 500,
            basePower = 100,
            victories = 0,
            effort = 0,
            strength = 0,
            battleSeed = 0x2222
        )

        val resWeak = DeterministicBattleSolver.resolveBattle(weakFighter, dummy)
        val resStrong = DeterministicBattleSolver.resolveBattle(strongFighter, dummy)
        val dmgWeak = resWeak.turns.first { it.attackerIndex == 0 }.damage
        val dmgStrong = resStrong.turns.first { it.attackerIndex == 0 }.damage
        assertTrue("Full strength hearts must increase combat damage ($dmgStrong > $dmgWeak)", dmgStrong > dmgWeak)

        val resDataClear = DeterministicBattleSolver.resolveBattle(dummy, weakFighter, environmentEffect = "none")
        val resDataStorm = DeterministicBattleSolver.resolveBattle(dummy, weakFighter, environmentEffect = "Data Storm")
        val dmgDataClear = resDataClear.turns.first { it.attackerIndex == 0 }.damage
        val dmgDataStorm = resDataStorm.turns.first { it.attackerIndex == 0 }.damage
        assertTrue("Space-separated 'Data Storm' must boost DATA attribute damage ($dmgDataStorm > $dmgDataClear)", dmgDataStorm > dmgDataClear)
    }

    @Test
    fun testAll550SpritesHaveFullColorFillAndVisibility() {
        val roster = PetSpecies.SORTED_SPECIES
        val sparseSprites = mutableListOf<String>()
        for (sp in roster) {
            val mat = com.digiwear.core.render.SpriteMatrixRegistry.getMatrix(sp.id, com.digiwear.core.model.SpriteFrame.IDLE_A)
            val pal = com.digiwear.core.render.SpriteMatrixRegistry.getPalette(sp.id)
            var totalLit = 0
            var coloredLit = 0
            var visibleBrightPixels = 0
            for (r in 0 until 16) {
                for (c in 0 until 16) {
                    val ch = mat[r][c]
                    if (ch != '.') {
                        totalLit++
                        if (ch != 'B' && ch != 'K') {
                            coloredLit++
                            val argb = when (ch) {
                                '#' -> pal.primary
                                'S' -> pal.secondary
                                'T' -> pal.tertiary
                                'E' -> pal.eye
                                'W' -> (0xFF shl 24) or (250 shl 16) or (250 shl 8) or 250
                                'R' -> (0xFF shl 24) or (244 shl 16) or (67 shl 8) or 54
                                'G' -> (0xFF shl 24) or (67 shl 16) or (185 shl 8) or 85
                                'C' -> (0xFF shl 24) or (0 shl 16) or (229 shl 8) or 255
                                else -> pal.primary
                            }
                            val red = (argb shr 16) and 0xFF
                            val green = (argb shr 8) and 0xFF
                            val blue = argb and 0xFF
                            val luma = 0.299 * red + 0.587 * green + 0.114 * blue
                            if (luma > 38.0) visibleBrightPixels++
                        }
                    }
                }
            }
            if (coloredLit < 30 || totalLit < 45 || visibleBrightPixels < 25) {
                println("SPARSE_OR_DARK_SPRITE: #${sp.id} ${sp.name} (${sp.stage}) -> totalLit=$totalLit, coloredLit=$coloredLit, bright=$visibleBrightPixels")
                sparseSprites.add("#${sp.id} ${sp.name} (total=$totalLit, colored=$coloredLit, bright=$visibleBrightPixels)")
            }
            // Verify all 12 animation frames render cleanly
            for (frame in com.digiwear.core.model.SpriteFrame.entries) {
                val frameMat = com.digiwear.core.render.SpriteMatrixRegistry.getMatrix(sp.id, frame)
                assertEquals("Frame $frame for #${sp.id} must have 16 rows", 16, frameMat.size)
                assertTrue("Frame $frame for #${sp.id} must have 16 cols per row", frameMat.all { it.length == 16 })
                val frameLit = frameMat.sumOf { row -> row.count { it != '.' } }
                assertTrue("Frame $frame for #${sp.id} must retain visible pixels ($frameLit)", frameLit >= 35)
            }
        }
        assertTrue("All 550 sprites must have full color fill and visibility! Sparse: $sparseSprites", sparseSprites.isEmpty())
    }

    @Test
    fun testBattleTowerHazardsAndCampaignBossSpeciesIds() {
        val vaccineFighter = BattleFighter(
            id = "vaccine",
            speciesId = 301,
            name = "Greymon",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.VACCINE,
            maxHp = 999,
            currentHp = 999,
            basePower = 100,
            victories = 10,
            effort = 4,
            battleSeed = 0x1111
        )
        val virusFighter = BattleFighter(
            id = "virus",
            speciesId = 303,
            name = "Devimon",
            stage = PetStage.ADULT,
            attribute = ElementalAttribute.VIRUS,
            maxHp = 999,
            currentHp = 999,
            basePower = 100,
            victories = 10,
            effort = 4,
            battleSeed = 0x2222
        )

        val clearRes = DeterministicBattleSolver.resolveBattle(vaccineFighter, virusFighter, environmentEffect = "NONE")
        val solarRes = DeterministicBattleSolver.resolveBattle(vaccineFighter, virusFighter, environmentEffect = "SOLAR_FLARE")
        assertTrue(
            "SOLAR_FLARE must boost VACCINE damage",
            solarRes.turns.first { it.attackerIndex == 0 }.damage > clearRes.turns.first { it.attackerIndex == 0 }.damage
        )

        // Verify canonical CampaignStoryRegistry boss species IDs match their BossEncounter names
        val allNodes = com.digiwear.core.adventure.CampaignStoryRegistry.ALL_CAMPAIGNS.flatMap { it.nodes }.associateBy { it.nodeId }
        val fixedNodes = mapOf(
            212 to 596,
            214 to 1563,
            215 to 1562,
            221 to 1539,
            225 to 1557,
            319 to 1421,
            416 to 1560,
            417 to 1561,
            421 to 1555
        )
        for ((nodeId, expectedSpeciesId) in fixedNodes) {
            val boss = allNodes.getValue(nodeId).bossEncounter!!
            assertEquals("Node $nodeId bossMonsterId", expectedSpeciesId, boss.bossMonsterId)
            assertEquals("Node $nodeId boss species name", boss.bossName, PetSpecies.getById(boss.bossMonsterId).name)
        }

        // Verify that a fighter with hitRatePercent >= 50% (e.g. 50% or 62%) can still roll GRAZE on non-crit turns
        val tankA = vaccineFighter.copy(maxHp = 5000, currentHp = 5000, basePower = 40)
        val tankB = virusFighter.copy(maxHp = 5000, currentHp = 5000, basePower = 40)
        val longBattle = DeterministicBattleSolver.resolveBattle(tankA, tankB)
        val nonCritAdvantagedTurns = longBattle.turns.filter { it.attackerIndex == 0 && !it.isCritical }
        assertTrue("Expected advantaged hitRate >= 50%", nonCritAdvantagedTurns.first().hitRatePercent >= 50)
        assertTrue(
            "Fighter with hitRate >= 50% (< 100%) must be able to roll GRAZE on some non-crit rounds",
            nonCritAdvantagedTurns.any { it.attackType == "GRAZE" }
        )
        assertTrue(
            "Fighter with hitRate >= 50% must also land direct hits on non-crit rounds",
            nonCritAdvantagedTurns.any { it.attackType != "GRAZE" }
        )
    }
}
