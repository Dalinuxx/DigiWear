package com.digiwear.core.workout

import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * Workout discipline classification.
 */
@Serializable
enum class WorkoutType(val displayName: String, val description: String) {
    FREE_TRAINING("Free Training", "Gym, weights & sparring session to burn weight and max effort"),
    EXPEDITION_TREK("Expedition Trek", "Jogging or fast-walking to accelerate Adventure Mode sector progress"),
    SHADOW_BOXING("Shadow Boxing", "High-cadence HIIT wrist movement to sharpen battle reflex and attack power"),
    CARDIO_RUN("Cardio Run", "Outdoor or treadmill running for high-intensity calorie burn & evolution acceleration"),
    POWER_WALK("Power Walk", "Steady brisk walking to build vital points and clear adventure sectors")
}

/**
 * Biometric Heart Rate zones with associated Adventure step and effort multipliers.
 */
@Serializable
enum class HeartRateZone(
    val minBpm: Int,
    val maxBpm: Int,
    val label: String,
    val stepMultiplier: Float,
    val hexColor: Long
) {
    RESTING(0, 99, "WARMUP", 1.0f, 0xFF90A4AE),
    AEROBIC(100, 139, "AEROBIC", 1.5f, 0xFF00E676),
    ANAEROBIC(140, 169, "ANAEROBIC", 2.0f, 0xFFFFB300),
    PEAK(170, 250, "PEAK", 2.5f, 0xFFFF1744);

    companion object {
        fun fromBpm(bpm: Int): HeartRateZone = when {
            bpm >= 170 -> PEAK
            bpm >= 140 -> ANAEROBIC
            bpm >= 100 -> AEROBIC
            else -> RESTING
        }
    }
}

/**
 * Result summary of a completed physical workout session.
 */
@Serializable
data class WorkoutSummary(
    val type: WorkoutType,
    val durationSeconds: Long,
    val averageBpm: Int,
    val peakBpm: Int,
    val estimatedCalories: Int,
    val rawSteps: Int,
    val effectiveAdventureSteps: Int,
    val weightGramsBurned: Int,
    val effortGained: Int,
    val energizedBuffGranted: Boolean,
    val vitalPointsGained: Int = 0,
    val evolutionMinutesAccelerated: Long = 0L
)

/**
 * Deterministic engine for processing physical workout sessions and applying
 * canonical virtual pet rewards without altering core simulation decay loops.
 */
object WorkoutEngine {

    /**
     * Estimates active calorie burn from average heart rate, duration, and baseline pet weight.
     */
    fun computeEstimatedCalories(avgBpm: Int, durationSeconds: Long): Int {
        if (durationSeconds <= 0L) return 0
        val baseBpm = max(70, avgBpm)
        // Standard MET estimation: ~0.08 kcal per second at moderate aerobic zone (~130 BPM)
        val intensityFactor = (baseBpm.toFloat() / 120f).coerceIn(0.5f, 2.5f)
        val calories = (durationSeconds * 0.08f * intensityFactor).toInt()
        return max(1, calories)
    }

    /**
     * Computes Adventure sector steps with cardio multiplier bonuses.
     */
    fun computeEffectiveSteps(rawSteps: Int, avgBpm: Int): Int {
        val zone = HeartRateZone.fromBpm(avgBpm)
        return (rawSteps.coerceAtLeast(0) * zone.stepMultiplier).toInt()
    }

    /**
     * Completes a workout session and applies natural V-Pet rewards to [currentState]:
     * - Reduces excess weight down toward canonical baseline.
     * - Fills training effort hearts in proportion to session duration (up to 4).
     * - Cleanses stress / distressed state.
     * - Returns the updated [PetState] and [WorkoutSummary].
     */
    fun finishSession(
        currentState: PetState,
        type: WorkoutType,
        durationSeconds: Long,
        rawSteps: Int,
        bpmSamples: List<Int>,
        currentTimestampUtc: Long = System.currentTimeMillis() / 1000L
    ): Pair<PetState, WorkoutSummary> {
        val safeDuration = durationSeconds.coerceAtLeast(0L)
        val safeRawSteps = rawSteps.coerceAtLeast(0)
        val validSamples = bpmSamples.filter { it > 40 }
        val avgBpm = if (validSamples.isNotEmpty()) validSamples.average().toInt() else 0
        val peakBpm = if (validSamples.isNotEmpty()) validSamples.maxOrNull() ?: avgBpm else 0

        val calories = computeEstimatedCalories(avgBpm, safeDuration)
        val effectiveSteps = computeEffectiveSteps(safeRawSteps, avgBpm)

        val species = PetSpecies.getById(currentState.speciesId)
        val baseline = species.baselineWeight

        // 1g burned per 3 minutes of workout or 25+ calories (minimum 30s active session), capped at baseline
        val weightBurnUnits = if (safeDuration < 30L && safeRawSteps < 15) {
            0
        } else {
            max(1, (safeDuration / 180L).toInt() + (calories / 25))
        }
        val newWeight = max(baseline, currentState.weight - weightBurnUnits)
        val weightBurned = currentState.weight - newWeight

        // Scale training effort proportionally to workout duration/activity (preventing 0s instant max-effort exploit)
        val effortDelta = when {
            safeDuration < 30L && safeRawSteps < 15 -> 0
            safeDuration < 120L -> 1
            safeDuration < 240L -> 2
            safeDuration < 360L -> 3
            else -> 4
        }
        val newEffort = min(4, currentState.effort + effortDelta)
        val strengthDelta = if (safeDuration >= 60L || safeRawSteps >= 25) 1 else 0
        val newStrength = min(4, currentState.strength + strengthDelta)
        val effortGained = max(0, newEffort - currentState.effort)

        // Sessions of 3+ minutes grant an energized buff for battles
        val energized = safeDuration >= 180L || avgBpm >= 120

        // Vital Points calculation: 10 VP per minute + 1 VP per 10 effective steps
        val vpGained = if (safeDuration < 15L && effectiveSteps == 0) {
            0
        } else {
            max(5, ((safeDuration / 60L) * 10 + (effectiveSteps / 10)).toInt())
        }
        val newVitalPoints = currentState.vitalPoints + vpGained

        // Evolution timer acceleration from calories burned: 15 min (900s) growth per 50 kcal, up to 120 min (7200s)
        val evoMinutesBonus = if (safeDuration >= 30L && calories >= 20) {
            min(120L, ((calories.toLong() * 15L) / 50L))
        } else {
            0L
        }
        val evoSecondsBonus = evoMinutesBonus * 60L
        val newAgeSeconds = currentState.ageSeconds + evoSecondsBonus

        val needsAttentionStill = currentState.hunger == 0 || (currentState.isSleeping && !currentState.lightsOff)

        val updatedState = currentState.copy(
            strength = newStrength,
            weight = newWeight,
            effort = newEffort,
            vitalPoints = newVitalPoints,
            ageSeconds = newAgeSeconds,
            attentionCallActive = if (!needsAttentionStill) false else currentState.attentionCallActive,
            attentionCallTimestampUtc = if (!needsAttentionStill) null else currentState.attentionCallTimestampUtc,
            lastProcessedTimestampUtc = currentTimestampUtc
        )

        val summary = WorkoutSummary(
            type = type,
            durationSeconds = safeDuration,
            averageBpm = avgBpm,
            peakBpm = peakBpm,
            estimatedCalories = calories,
            rawSteps = safeRawSteps,
            effectiveAdventureSteps = effectiveSteps,
            weightGramsBurned = weightBurned,
            effortGained = effortGained,
            energizedBuffGranted = energized,
            vitalPointsGained = vpGained,
            evolutionMinutesAccelerated = evoMinutesBonus
        )

        return Pair(updatedState, summary)
    }
}
