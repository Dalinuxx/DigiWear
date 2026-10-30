package com.digiwear.core

import com.digiwear.core.engine.PetSimulationEngine
import com.digiwear.core.model.PetStage
import com.digiwear.core.model.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class PetSimulationEngineTest {

    @Test
    fun testBabyIDecayAndNoSleep() {
        // Baby I: Hunger and Strength decay every 3 minutes (180s); waste every 15 minutes (900s)
        val initial = PetState(
            speciesId = 101, // Botamon (Baby I)
            hunger = 4,
            strength = 4,
            wasteCount = 0,
            lastProcessedTimestampUtc = 43200L // 12:00 PM UTC
        )

        // Advance 3 minutes (180s)
        val after3Min = PetSimulationEngine.processDelta(initial, 43380L, ZoneOffset.UTC)
        assertEquals(3, after3Min.hunger)
        assertEquals(3, after3Min.strength)
        assertEquals(0, after3Min.wasteCount)

        // Advance 15 minutes total (900s)
        val after15Min = PetSimulationEngine.processDelta(initial, 44100L, ZoneOffset.UTC)
        assertEquals(0, after15Min.hunger)
        assertEquals(0, after15Min.strength)
        assertEquals(1, after15Min.wasteCount)

        // Baby I never sleeps
        assertFalse(after15Min.isSleeping)
        // Baby I is exempt from care mistakes
        assertEquals(0, after15Min.careMistakes)
    }

    @Test
    fun testBabyIICareMistakeGraceWindow() {
        // Baby II (Koromon): Grace window is 10 minutes (600s)
        // Start at 12:00 PM UTC (awake)
        val initial = PetState(
            speciesId = 102, // Koromon
            hunger = 1,
            strength = 1,
            careMistakes = 0,
            lastProcessedTimestampUtc = 43200L // 12:00:00
        )

        // 10 minutes pass (12:10:00 = 43800L): hunger drops to 0, triggering attention call
        val atZeroHunger = PetSimulationEngine.processDelta(initial, 43800L, ZoneOffset.UTC)
        assertEquals(0, atZeroHunger.hunger)
        assertTrue(atZeroHunger.attentionCallActive)
        assertEquals(0, atZeroHunger.careMistakes)

        // 5 minutes later (12:15:00 = 44100L, still within 10 min grace window)
        val withinGrace = PetSimulationEngine.processDelta(atZeroHunger, 44100L, ZoneOffset.UTC)
        assertTrue(withinGrace.attentionCallActive)
        assertEquals(0, withinGrace.careMistakes)

        // 11 minutes after call triggered (12:21:00 = 44460L): Grace window expired! Care mistake recorded!
        val pastGrace = PetSimulationEngine.processDelta(atZeroHunger, 44460L, ZoneOffset.UTC)
        assertEquals(1, pastGrace.careMistakes)
    }

    @Test
    fun testSleepWindowPausesMetabolicDecay() {
        // Koromon sleeps 20:00 to 07:00
        // Timestamp at 22:00 UTC = 22 * 3600 = 79200L
        val sleepingKoromon = PetState(
            speciesId = 102,
            hunger = 2,
            strength = 2,
            lastProcessedTimestampUtc = 79200L
        )

        // 30 minutes pass during sleep (79200 + 1800 = 81000L)
        val after30Min = PetSimulationEngine.processDelta(sleepingKoromon, 81000L, ZoneOffset.UTC)
        assertTrue(after30Min.isSleeping)
        // Hunger remains paused during sleep
        assertEquals(2, after30Min.hunger)
    }

    @Test
    fun testOverfeedMechanism() {
        val initial = PetState(
            speciesId = 201, // Agumon
            hunger = 4,
            weight = 20,
            overfeedCount = 0
        )

        val afterFeed = PetSimulationEngine.feedMeal(initial, 43200L)
        assertEquals(4, afterFeed.hunger)
        assertEquals(21, afterFeed.weight)
        assertEquals(1, afterFeed.overfeedCount)

        val afterSecondFeed = PetSimulationEngine.feedMeal(afterFeed, 43201L)
        assertEquals(2, afterSecondFeed.overfeedCount)
        assertEquals(22, afterSecondFeed.weight)
    }

    @Test
    fun testWasteAccumulationTriggersInjury() {
        val initial = PetState(
            speciesId = 201, // Agumon (Child: waste every 60 min = 3600s)
            wasteCount = 3,
            isInjured = false,
            lastProcessedTimestampUtc = 43200L
        )

        // 1 hour passes -> 4th waste unit generated -> triggers injury!
        val afterHour = PetSimulationEngine.processDelta(initial, 46800L, ZoneOffset.UTC)
        assertEquals(4, afterHour.wasteCount)
        assertTrue(afterHour.isInjured)

        // Cleaning waste
        val cleaned = PetSimulationEngine.cleanWaste(afterHour)
        assertEquals(0, cleaned.wasteCount)

        // Curing injury
        val cured = PetSimulationEngine.cureInjury(cleaned)
        assertFalse(cured.isInjured)
    }

    @Test
    fun testColdSleepPausesAllDecay() {
        val initial = PetState(
            speciesId = 201, // Agumon
            hunger = 4,
            strength = 4,
            wasteCount = 0,
            isFrozen = true,
            lastProcessedTimestampUtc = 43200L
        )

        // 8 hours pass while frozen in cold sleep stasis
        val after8Hours = PetSimulationEngine.processDelta(initial, 43200L + 28800L, ZoneOffset.UTC)
        assertEquals(4, after8Hours.hunger)
        assertEquals(4, after8Hours.strength)
        assertEquals(0, after8Hours.wasteCount)
        assertEquals(0L, after8Hours.ageSeconds)
        assertFalse(after8Hours.attentionCallActive)
        assertEquals(0, after8Hours.careMistakes)
        assertTrue(after8Hours.isFrozen)

        // Waking up from cold sleep
        val awakened = PetSimulationEngine.toggleColdSleep(after8Hours, 43200L + 28800L)
        assertFalse(awakened.isFrozen)
    }

    @Test
    fun testBattleVictoryIncrementsWinsAndBattlesWithoutInjury() {
        val initial = PetState(totalBattles = 5, victories = 3, isInjured = false)
        val (updated, wasInjured) = PetSimulationEngine.processBattleOutcome(
            state = initial,
            won = true,
            combatInjuriesEnabled = true,
            injuryRoll = 10
        )
        assertEquals(6, updated.totalBattles)
        assertEquals(4, updated.victories)
        assertFalse(updated.isInjured)
        assertFalse(wasInjured)
    }

    @Test
    fun testBattleDefeatCanInflictCanonicalInjuryWhenRollBelow25() {
        val initial = PetState(totalBattles = 5, victories = 3, isInjured = false)
        // Roll of 15 (< 25) on defeat triggers canonical injury
        val (updated, wasInjured) = PetSimulationEngine.processBattleOutcome(
            state = initial,
            won = false,
            combatInjuriesEnabled = true,
            injuryRoll = 15
        )
        assertEquals(6, updated.totalBattles)
        assertEquals(3, updated.victories)
        assertTrue(updated.isInjured)
        assertTrue(wasInjured)
    }

    @Test
    fun testBattleDefeatDoesNotInflictInjuryWhenRoll25OrHigher() {
        val initial = PetState(totalBattles = 5, victories = 3, isInjured = false)
        // Roll of 40 (>= 25) on defeat does not trigger injury
        val (updated, wasInjured) = PetSimulationEngine.processBattleOutcome(
            state = initial,
            won = false,
            combatInjuriesEnabled = true,
            injuryRoll = 40
        )
        assertEquals(6, updated.totalBattles)
        assertEquals(3, updated.victories)
        assertFalse(updated.isInjured)
        assertFalse(wasInjured)
    }

    @Test
    fun testBattleDefeatDoesNotInflictInjuryWhenDisabled() {
        val initial = PetState(totalBattles = 5, victories = 3, isInjured = false)
        // Roll of 5 (< 25) but combatInjuriesEnabled = false
        val (updated, wasInjured) = PetSimulationEngine.processBattleOutcome(
            state = initial,
            won = false,
            combatInjuriesEnabled = false,
            injuryRoll = 5
        )
        assertEquals(6, updated.totalBattles)
        assertEquals(3, updated.victories)
        assertFalse(updated.isInjured)
        assertFalse(wasInjured)
    }

    @Test
    fun testOvernightDeltaOnlyDecaysDuringAwakeHoursAndResetsLights() {
        // Agumon (Child: sleeps 21:00..08:00, hunger/strength decay every 30 min = 1800s, waste every 60 min = 3600s)
        // Start at 20:50 UTC (10 minutes before 21:00 sleep window) with lights turned off during sleep
        val startUtc = 20L * 3600L + 50L * 60L // 75,000s
        val initial = PetState(
            speciesId = 201,
            hunger = 4,
            strength = 4,
            wasteCount = 0,
            lightsOff = true,
            isSleeping = false,
            lastProcessedTimestampUtc = startUtc
        )

        // Advance overnight to 09:00 UTC next day (24h + 9h = 33h = 118,800s)
        // Awake time should ONLY be 10m (20:50..21:00) + 60m (08:00..09:00) = 70 minutes (4,200s)
        // 4,200s / 1,800s = 2 hunger/strength lost (remaining = 2), 4,200s / 3,600s = 1 waste
        val morningUtc = 33L * 3600L // 118,800s
        val morningState = PetSimulationEngine.processDelta(initial, morningUtc, ZoneOffset.UTC)

        assertFalse("Pet should be awake at 09:00", morningState.isSleeping)
        assertFalse("Lights should auto-turn back on upon waking", morningState.lightsOff)
        assertEquals("Only 70 awake minutes elapsed -> 2 hunger lost", 2, morningState.hunger)
        assertEquals("Only 70 awake minutes elapsed -> 2 strength lost", 2, morningState.strength)
        assertEquals("Only 70 awake minutes elapsed -> 1 waste generated", 1, morningState.wasteCount)
        assertEquals("No care mistakes should occur overnight when full before bed", 0, morningState.careMistakes)
    }

    @Test
    fun testTraitedEggBonusHpPreservedAcrossEvolution() {
        val departed = PetState(
            speciesId = 501,
            generation = 2,
            traitedEggBonus = 1,
            victories = 25,
            isDeceased = true
        )
        val traitedBotamon = PetSimulationEngine.createTraitedEggState(
            departedState = departed,
            starterSpeciesId = 101,
            nowUtc = 1000L
        )
        // Botamon baseHp (50) + 2 * 5 = 60
        assertEquals(2, traitedBotamon.traitedEggBonus)
        assertEquals(60, traitedBotamon.maxHp)

        val agedBotamon = traitedBotamon.copy(ageSeconds = 700L)
        val koromonSpecies = com.digiwear.core.model.PetSpecies.getById(102)
        val evolvedKoromon = com.digiwear.core.engine.EvolutionEngine.evolve(agedBotamon, koromonSpecies, 1700L)
        // Koromon baseHp (80) + 2 * 5 = 90
        assertEquals(90, evolvedKoromon.maxHp)
        assertEquals(2, evolvedKoromon.traitedEggBonus)
    }

    @Test
    fun testRangeSleepSchedules() {
        // Child stage (e.g. Agumon): base sleep is 21 to 8
        val stage = PetStage.CHILD

        // Test 20:00_07:00 (Bed at 20, Wake at 7)
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 20, 0, "20:00_07:00"))
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 23, 30, "20:00_07:00"))
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 6, 59, "20:00_07:00"))
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 7, 0, "20:00_07:00"))
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 19, 59, "20:00_07:00"))

        // Test 23:00_08:00 (Bed at 23, Wake at 8)
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 22, 0, "23:00_08:00"))
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 23, 0, "23:00_08:00"))
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 3, 0, "23:00_08:00"))
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 7, 59, "23:00_08:00"))
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 8, 0, "23:00_08:00"))

        // Test minute-level precision 20:30_07:30
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 20, 29, "20:30_07:30"))
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 20, 30, "20:30_07:30"))
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 7, 29, "20:30_07:30"))
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 7, 30, "20:30_07:30"))

        // Test hyphen delimiter and whitespace trimming
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 21, 0, " 20:00 - 07:00 "))
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 19, 0, "20:00-07:00"))

        // Test same-day sleep window (e.g. nap 13:00 to 15:00)
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 12, 59, "13:00_15:00"))
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 13, 0, "13:00_15:00"))
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 14, 59, "13:00_15:00"))
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 15, 0, "13:00_15:00"))
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 22, 0, "13:00_15:00"))

        // Test legacy NIGHT_OWL (0 to 8)
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 0, 0, "NIGHT_OWL"))
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 7, 0, "NIGHT_OWL"))
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 8, 0, "NIGHT_OWL"))

        // Test invalid/empty fallback to canonical stage sleep (Child: 21:00 to 08:00)
        assertTrue(PetSimulationEngine.isSleepWindowWithSchedule(stage, 22, 0, "CORRUPTED_STRING"))
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(stage, 12, 0, "CORRUPTED_STRING"))

        // Test Baby I (never sleeps)
        assertFalse(PetSimulationEngine.isSleepWindowWithSchedule(PetStage.BABY_I, 22, 0, "20:00_07:00"))
    }

    @Test
    fun testEmptyHungerDuringSleepWithLightsOffDoesNotTriggerCareMistake() {
        // Agumon sleeps 21:00..08:00. At 22:00 UTC (79,200s) with lightsOff = true and hunger = 0:
        val sleepingEmpty = PetState(
            speciesId = 201,
            hunger = 0,
            strength = 0,
            lightsOff = true,
            isSleeping = true,
            careMistakes = 0,
            lastProcessedTimestampUtc = 79200L
        )
        val after2HoursSleep = PetSimulationEngine.processDelta(sleepingEmpty, 79200L + 7200L, ZoneOffset.UTC)
        assertTrue(after2HoursSleep.isSleeping)
        assertFalse("Empty hunger while sleeping with lights off must not trigger attention call", after2HoursSleep.attentionCallActive)
        assertEquals("No care mistakes should accrue while sleeping with lights off", 0, after2HoursSleep.careMistakes)
    }

    @Test
    fun testVitalPointsAwardedOnTrainingAndBattle() {
        val initial = PetState(speciesId = 201, vitalPoints = 100)
        val afterTrainWin = PetSimulationEngine.train(initial, success = true, currentTimestampUtc = 1000L)
        assertEquals(125, afterTrainWin.vitalPoints)
        val afterTrainLoss = PetSimulationEngine.train(afterTrainWin, success = false, currentTimestampUtc = 1010L)
        assertEquals(130, afterTrainLoss.vitalPoints)
        val (afterBattleWin, _) = PetSimulationEngine.processBattleOutcome(afterTrainLoss, won = true)
        assertEquals(180, afterBattleWin.vitalPoints)
        val (afterBattleLoss, _) = PetSimulationEngine.processBattleOutcome(afterBattleWin, won = false)
        assertEquals(190, afterBattleLoss.vitalPoints)
    }

    @Test
    fun testLightsOutClearsAndSuppressesAllAttentionCalls() {
        val callingState = PetState(
            speciesId = 201,
            hunger = 0,
            strength = 0,
            attentionCallActive = true,
            attentionCallTimestampUtc = 1000L,
            lightsOff = false
        )
        // Turning lights out must immediately extinguish active call signal
        val afterLightsOut = PetSimulationEngine.toggleLights(callingState, 1010L)
        assertTrue(afterLightsOut.lightsOff)
        assertFalse(afterLightsOut.attentionCallActive)
        assertNull(afterLightsOut.attentionCallTimestampUtc)

        // Further decay while lights are out must never trigger attention call
        val decayedInDark = PetSimulationEngine.processDelta(afterLightsOut, 1010L + 3600L, ZoneOffset.UTC)
        assertFalse(decayedInDark.attentionCallActive)
        assertNull(decayedInDark.attentionCallTimestampUtc)
    }
}
