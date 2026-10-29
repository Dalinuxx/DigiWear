package com.digiwear.core

import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import com.digiwear.core.workout.HeartRateZone
import com.digiwear.core.workout.WorkoutEngine
import com.digiwear.core.workout.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutEngineTest {

    @Test
    fun testHeartRateZones() {
        assertEquals(HeartRateZone.RESTING, HeartRateZone.fromBpm(85))
        assertEquals(HeartRateZone.AEROBIC, HeartRateZone.fromBpm(120))
        assertEquals(HeartRateZone.ANAEROBIC, HeartRateZone.fromBpm(155))
        assertEquals(HeartRateZone.PEAK, HeartRateZone.fromBpm(178))
    }

    @Test
    fun testEffectiveAdventureStepMultipliers() {
        val rawSteps = 1000
        val restingSteps = WorkoutEngine.computeEffectiveSteps(rawSteps, 80)
        assertEquals(1000, restingSteps)

        val aerobicSteps = WorkoutEngine.computeEffectiveSteps(rawSteps, 125)
        assertEquals(1500, aerobicSteps)

        val anaerobicSteps = WorkoutEngine.computeEffectiveSteps(rawSteps, 150)
        assertEquals(2000, anaerobicSteps)

        val peakSteps = WorkoutEngine.computeEffectiveSteps(rawSteps, 180)
        assertEquals(2500, peakSteps)
    }

    @Test
    fun testWorkoutFinishSessionRewards() {
        // Agumon baseline weight = 20g
        val agumon = PetSpecies.getById(201)
        val initialPet = PetState(
            speciesId = 201,
            weight = 30, // 10g overweight
            effort = 1
        )

        val bpmSamples = listOf(115, 125, 130, 140, 135)
        val (updatedPet, summary) = WorkoutEngine.finishSession(
            currentState = initialPet,
            type = WorkoutType.EXPEDITION_TREK,
            durationSeconds = 600L, // 10 minutes
            rawSteps = 1200,
            bpmSamples = bpmSamples
        )

        assertEquals(4, updatedPet.effort)
        assertTrue(updatedPet.weight < 30)
        assertTrue(updatedPet.weight >= agumon.baselineWeight)
        assertEquals(WorkoutType.EXPEDITION_TREK, summary.type)
        assertEquals(600L, summary.durationSeconds)
        assertTrue(summary.estimatedCalories > 0)
        assertTrue(summary.effectiveAdventureSteps > 1200)
        assertTrue(summary.energizedBuffGranted)
    }

    @Test
    fun testWeightNeverDropsBelowBaseline() {
        // Gabumon baseline weight = 20g
        val agumon = PetSpecies.getById(201)
        val exactWeightPet = PetState(
            speciesId = 201,
            weight = agumon.baselineWeight,
            effort = 4
        )

        val (updatedPet, summary) = WorkoutEngine.finishSession(
            currentState = exactWeightPet,
            type = WorkoutType.FREE_TRAINING,
            durationSeconds = 3600L, // 1 hour intense
            rawSteps = 5000,
            bpmSamples = listOf(160, 165, 170)
        )

        assertEquals(agumon.baselineWeight, updatedPet.weight)
        assertEquals(0, summary.weightGramsBurned)
    }

    @Test
    fun testZeroDurationWorkoutGrantsNoFreeRewardsAndZeroBpm() {
        val initialPet = PetState(
            speciesId = 201,
            weight = 30,
            effort = 1,
            strength = 1,
            vitalPoints = 0
        )
        val (updatedPet, summary) = WorkoutEngine.finishSession(
            currentState = initialPet,
            type = WorkoutType.FREE_TRAINING,
            durationSeconds = 0L,
            rawSteps = 0,
            bpmSamples = emptyList()
        )
        assertEquals(1, updatedPet.effort)
        assertEquals(1, updatedPet.strength)
        assertEquals(30, updatedPet.weight)
        assertEquals(0, updatedPet.vitalPoints)
        assertEquals(0, summary.averageBpm)
        assertEquals(0, summary.weightGramsBurned)
        assertEquals(0L, summary.evolutionMinutesAccelerated)
    }

    @Test
    fun testWorkoutCalorieEvolutionAcceleration() {
        val initialPet = PetState(
            speciesId = 201,
            ageSeconds = 10_000L
        )

        val (updatedPet, summary) = WorkoutEngine.finishSession(
            currentState = initialPet,
            type = WorkoutType.CARDIO_RUN,
            durationSeconds = 1000L,
            rawSteps = 1500,
            bpmSamples = listOf(140, 142, 138)
        )

        assertTrue("Must calculate calories", summary.estimatedCalories >= 50)
        assertTrue("Must accelerate evolution", summary.evolutionMinutesAccelerated > 0L)
        assertTrue("Age must increase by evolution bonus seconds", updatedPet.ageSeconds > initialPet.ageSeconds)
        assertEquals(
            initialPet.ageSeconds + (summary.evolutionMinutesAccelerated * 60L),
            updatedPet.ageSeconds
        )
    }
}
