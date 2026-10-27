package com.digiwear.core.engine

import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import com.digiwear.core.model.PetState
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.max
import kotlin.math.min

/**
 * High-precision physiological simulation engine conforming to V1-V6 virtual pet specifications.
 * All updates are calculated strictly via UTC timestamp deltas:
 * DeltaT = T_current - T_last_processed
 * Eliminating active CPU polling loops for optimal battery conservation.
 */
object PetSimulationEngine {

    /**
     * Minimum sleep duration for virtual pets adhering to 1997 classic benchmarks (6 hours / 360 minutes).
     */
    const val MIN_SLEEP_HOURS = 6
    const val MIN_SLEEP_MINUTES = 6 * 60

    /**
     * Calculates the sleep duration in minutes for given bed and wake times in 24h format.
     */
    fun calculateSleepDurationMinutes(bedHour: Int, bedMinute: Int, wakeHour: Int, wakeMinute: Int): Int {
        val bedTotal = bedHour * 60 + bedMinute
        val wakeTotal = wakeHour * 60 + wakeMinute
        return if (wakeTotal > bedTotal) {
            wakeTotal - bedTotal
        } else if (wakeTotal < bedTotal) {
            (1440 - bedTotal) + wakeTotal
        } else {
            0
        }
    }

    /**
     * Calculates the sleep duration in minutes for a schedule string formatted as HH:mm_HH:mm.
     */
    fun calculateSleepDurationMinutes(schedule: String): Int? {
        val delimiter = if (schedule.contains("_")) "_" else if (schedule.contains("-")) "-" else return null
        val parts = schedule.split(delimiter)
        val bedParts = parts.getOrNull(0)?.trim()?.split(":")
        val wakeParts = parts.getOrNull(1)?.trim()?.split(":")
        val bedHour = bedParts?.getOrNull(0)?.toIntOrNull() ?: return null
        val bedMin = bedParts?.getOrNull(1)?.toIntOrNull() ?: 0
        val wakeHour = wakeParts?.getOrNull(0)?.toIntOrNull() ?: return null
        val wakeMin = wakeParts?.getOrNull(1)?.toIntOrNull() ?: 0
        return calculateSleepDurationMinutes(bedHour, bedMin, wakeHour, wakeMin)
    }

    /**
     * Validates whether a custom sleep schedule satisfies the OG V-Pet minimum sleep duration (at least 6 hours).
     */
    fun isValidSleepDuration(bedHour: Int, bedMinute: Int, wakeHour: Int, wakeMinute: Int): Boolean {
        return calculateSleepDurationMinutes(bedHour, bedMinute, wakeHour, wakeMinute) >= MIN_SLEEP_MINUTES
    }

    fun isValidSleepDuration(schedule: String): Boolean {
        val duration = calculateSleepDurationMinutes(schedule) ?: return false
        return duration >= MIN_SLEEP_MINUTES
    }

    /**
     * Advances the pet simulation to [currentTimestampUtc].
     */
    fun processDelta(
        currentState: PetState,
        currentTimestampUtc: Long,
        timeZoneOffset: ZoneOffset = ZoneOffset.UTC,
        isCanonicalLifespanEnabled: Boolean = false,
        sleepScheduleMode: String = "CANONICAL"
    ): PetState {
        if (currentState.isDeceased) return currentState

        val lastTime = if (currentState.lastProcessedTimestampUtc <= 0L) {
            currentTimestampUtc
        } else {
            currentState.lastProcessedTimestampUtc
        }

        val deltaSeconds = max(0L, currentTimestampUtc - lastTime)
        if (deltaSeconds == 0L) return currentState

        // Cold Sleep / Freezer Mode: complete physiological stasis (all decay & aging paused)
        if (currentState.isFrozen) {
            return currentState.copy(lastProcessedTimestampUtc = currentTimestampUtc)
        }

        val species = PetSpecies.getById(currentState.speciesId)
        val stage = species.stage

        // Update accumulated live age
        var updatedAge = currentState.ageSeconds + deltaSeconds

        // Check canonical lifespan limit
        val maxLifespanSec = currentState.lifespanDays * 86400L
        val isPerished = isCanonicalLifespanEnabled && (updatedAge >= maxLifespanSec || (currentState.careMistakes >= 20 && currentState.isInjured))
        if (isPerished) {
            return currentState.copy(
                ageSeconds = updatedAge,
                isDeceased = true,
                lastProcessedTimestampUtc = currentTimestampUtc
            )
        }

        // Calculate sleep window status based on wall clock time and user schedule preference
        val zonedDateTime = Instant.ofEpochSecond(currentTimestampUtc).atZone(timeZoneOffset)
        val hour = zonedDateTime.hour
        val minute = zonedDateTime.minute
        val inSleepWindow = isSleepWindowWithSchedule(stage, hour, minute, sleepScheduleMode)

        // Compute awake seconds across [lastTime, currentTimestampUtc] so sleep windows pause metabolic decay
        val awakeSeconds = computeAwakeSeconds(lastTime, currentTimestampUtc, stage, timeZoneOffset, sleepScheduleMode)

        // Automatically turn room lights back on when waking up in the morning (including multi-hour overnight jumps)
        val effectiveLightsOff = if (!inSleepWindow && (currentState.isSleeping || awakeSeconds < deltaSeconds)) false else currentState.lightsOff

        // Accumulate decay seconds strictly for awake time
        var accHungerSec = currentState.hungerAccumulatedSeconds + awakeSeconds
        var accStrengthSec = currentState.strengthAccumulatedSeconds + awakeSeconds
        var accWasteSec = currentState.wasteAccumulatedSeconds + awakeSeconds

        val hungerIntervalSec = stage.hungerLossMinutes * 60L
        val strengthIntervalSec = stage.strengthLossMinutes * 60L
        val wasteIntervalSec = stage.wasteIntervalMinutes * 60L

        var currentHunger = currentState.hunger
        var currentStrength = currentState.strength
        var currentWaste = currentState.wasteCount

        // Process hunger decay when awake (or for awake portion of multi-hour delta)
        if (awakeSeconds > 0L) {
            val hungerDecayUnits = (accHungerSec / hungerIntervalSec).toInt()
            if (hungerDecayUnits > 0) {
                currentHunger = max(0, currentHunger - hungerDecayUnits)
                accHungerSec %= hungerIntervalSec
            }

            // Process strength decay
            val strengthDecayUnits = (accStrengthSec / strengthIntervalSec).toInt()
            if (strengthDecayUnits > 0) {
                currentStrength = max(0, currentStrength - strengthDecayUnits)
                accStrengthSec %= strengthIntervalSec
            }

            // Process waste generation (up to 4 uncleaned units)
            val wasteUnits = (accWasteSec / wasteIntervalSec).toInt()
            if (wasteUnits > 0) {
                currentWaste = min(4, currentWaste + wasteUnits)
                accWasteSec %= wasteIntervalSec
            }
        }

        // Check for injury due to uncleaned waste accumulation (4 waste units triggers injury)
        var isInjured = currentState.isInjured
        if (currentWaste >= 4) {
            isInjured = true
        }

        // Attention Call logic:
        // Triggered by: empty hunger/strength meter when awake, or unextinguished lights during sleep window
        val needsAttention = (!inSleepWindow && (currentHunger == 0 || currentStrength == 0)) || (inSleepWindow && !effectiveLightsOff)

        var callActive = currentState.attentionCallActive
        var callTimestamp = currentState.attentionCallTimestampUtc
        var careMistakes = currentState.careMistakes

        if (stage.careMistakesEnabled) {
            val graceWindowSec = stage.graceWindowMinutes * 60L

            if (needsAttention) {
                if (!callActive) {
                    // Signal just triggered
                    callActive = true
                    callTimestamp = currentTimestampUtc
                } else if (callTimestamp != null) {
                    // Check if elapsed time beyond designated grace window
                    val timeInCall = currentTimestampUtc - callTimestamp
                    if (timeInCall >= graceWindowSec) {
                        // Care Mistake recorded!
                        careMistakes++
                        // Reset call so subsequent mistake requires re-evaluating or waiting next window
                        callActive = false
                        callTimestamp = null
                    }
                }
            } else {
                // Needs were addressed before grace window expired
                callActive = false
                callTimestamp = null
            }
        } else {
            // Baby I is exempt from care mistakes
            callActive = needsAttention
            callTimestamp = null
        }

        val perishedFromNeglect = isCanonicalLifespanEnabled && (careMistakes >= 20 && isInjured)

        return currentState.copy(
            hunger = currentHunger,
            strength = currentStrength,
            wasteCount = currentWaste,
            ageSeconds = updatedAge,
            careMistakes = careMistakes,
            isSleeping = inSleepWindow,
            lightsOff = effectiveLightsOff,
            isInjured = isInjured,
            isDeceased = perishedFromNeglect,
            lastProcessedTimestampUtc = currentTimestampUtc,
            attentionCallActive = callActive,
            attentionCallTimestampUtc = callTimestamp,
            hungerAccumulatedSeconds = accHungerSec,
            strengthAccumulatedSeconds = accStrengthSec,
            wasteAccumulatedSeconds = accWasteSec
        )
    }

    fun isSleepWindowWithSchedule(
        stage: PetStage,
        hour: Int,
        minute: Int,
        sleepScheduleMode: String = "CANONICAL"
    ): Boolean {
        val baseStart = stage.sleepStartHour ?: return false
        val baseEnd = stage.sleepEndHour ?: return false
        val trimmed = sleepScheduleMode.trim().uppercase()
        val (startMinutes, endMinutes) = when (trimmed) {
            "NIGHT_OWL" -> (0 * 60) to (8 * 60)
            "EARLY_BIRD" -> (21 * 60) to (6 * 60)
            else -> {
                val delimiter = if (trimmed.contains("_")) "_" else if (trimmed.contains("-")) "-" else null
                if (delimiter != null) {
                    val parts = trimmed.split(delimiter)
                    val bedParts = parts.getOrNull(0)?.trim()?.split(":")
                    val wakeParts = parts.getOrNull(1)?.trim()?.split(":")
                    val bedHour = bedParts?.getOrNull(0)?.toIntOrNull()
                    val bedMin = bedParts?.getOrNull(1)?.toIntOrNull() ?: 0
                    val wakeHour = wakeParts?.getOrNull(0)?.toIntOrNull()
                    val wakeMin = wakeParts?.getOrNull(1)?.toIntOrNull() ?: 0
                    if (bedHour != null && wakeHour != null) {
                        (bedHour * 60 + bedMin) to (wakeHour * 60 + wakeMin)
                    } else {
                        (baseStart * 60) to (baseEnd * 60)
                    }
                } else {
                    (baseStart * 60) to (baseEnd * 60)
                }
            }
        }
        val currentMinutes = hour * 60 + minute
        return if (startMinutes > endMinutes) {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            currentMinutes in startMinutes until endMinutes
        }
    }

    private fun computeAwakeSeconds(
        startUtc: Long,
        endUtc: Long,
        stage: PetStage,
        timeZoneOffset: ZoneOffset,
        sleepScheduleMode: String
    ): Long {
        if (endUtc <= startUtc) return 0L
        if (stage.sleepStartHour == null || stage.sleepEndHour == null) {
            return endUtc - startUtc
        }
        val totalDelta = endUtc - startUtc
        if (totalDelta <= 1800L) {
            val endZdt = Instant.ofEpochSecond(endUtc).atZone(timeZoneOffset)
            return if (isSleepWindowWithSchedule(stage, endZdt.hour, endZdt.minute, sleepScheduleMode)) 0L else totalDelta
        }
        var awake = 0L
        var cursor = startUtc
        while (cursor < endUtc) {
            val zdt = Instant.ofEpochSecond(cursor).atZone(timeZoneOffset)
            val secsIntoHour = zdt.minute * 60L + zdt.second
            val nextHourBoundary = cursor + (3600L - secsIntoHour).coerceAtLeast(60L)
            val stepEnd = min(endUtc, nextHourBoundary)
            val mid = (cursor + stepEnd) / 2L
            val midZdt = Instant.ofEpochSecond(mid).atZone(timeZoneOffset)
            if (!isSleepWindowWithSchedule(stage, midZdt.hour, midZdt.minute, sleepScheduleMode)) {
                awake += (stepEnd - cursor)
            }
            cursor = stepEnd
        }
        return awake
    }

    /**
     * Feeding meat/meal increments hunger by 1 heart (max 4) and adds 1g weight.
     * Feeding when hunger is full records an Overfeed instance.
     */
    fun feedMeal(state: PetState, currentTimestampUtc: Long): PetState {
        val isFull = state.hunger >= 4
        val newOverfeed = if (isFull) state.overfeedCount + 1 else state.overfeedCount
        val newHunger = min(4, state.hunger + 1)
        val newWeight = state.weight + 1

        val updatedState = state.copy(
            hunger = newHunger,
            overfeedCount = newOverfeed,
            weight = newWeight,
            lastProcessedTimestampUtc = currentTimestampUtc
        )
        return resolveAttentionCall(updatedState, currentTimestampUtc)
    }

    /**
     * Feeding vitamin/protein increments strength by 1 heart (max 4) and adds 2g weight.
     */
    fun feedProtein(state: PetState, currentTimestampUtc: Long): PetState {
        val newStrength = min(4, state.strength + 1)
        val newWeight = state.weight + 2

        val updatedState = state.copy(
            strength = newStrength,
            weight = newWeight,
            lastProcessedTimestampUtc = currentTimestampUtc
        )
        return resolveAttentionCall(updatedState, currentTimestampUtc)
    }

    /**
     * Cleans all waste units and prevents injury buildup.
     */
    fun cleanWaste(state: PetState, currentTimestampUtc: Long = state.lastProcessedTimestampUtc): PetState {
        return state.copy(
            wasteCount = 0,
            wasteAccumulatedSeconds = 0L,
            lastProcessedTimestampUtc = if (currentTimestampUtc > 0L) currentTimestampUtc else state.lastProcessedTimestampUtc
        )
    }

    /**
     * Toggles Cold Sleep (Rest Mode / Freezer Mode).
     * Pauses all aging, hunger decay, strength decay, and care mistake accumulation.
     */
    fun toggleColdSleep(state: PetState, currentTimestampUtc: Long = System.currentTimeMillis() / 1000L): PetState {
        val nextFrozen = !state.isFrozen
        return state.copy(
            isFrozen = nextFrozen,
            attentionCallActive = if (nextFrozen) false else state.attentionCallActive,
            attentionCallTimestampUtc = if (nextFrozen) null else state.attentionCallTimestampUtc,
            lastProcessedTimestampUtc = currentTimestampUtc
        )
    }

    fun toggleLights(state: PetState, currentTimestampUtc: Long): PetState {
        val newLightsOff = !state.lightsOff
        val updated = state.copy(
            lightsOff = newLightsOff,
            lastProcessedTimestampUtc = currentTimestampUtc
        )
        return resolveAttentionCall(updated, currentTimestampUtc)
    }

    /**
     * Executes mini-game training session.
     * Win grants strength +1, effort +1, +25 Vital Points, reduces weight by 1g.
     */
    fun train(state: PetState, success: Boolean, currentTimestampUtc: Long): PetState {
        val newStrength = if (success) min(4, state.strength + 1) else state.strength
        val newEffort = if (success) min(4, state.effort + 1) else state.effort
        val newWeight = max(PetSpecies.getById(state.speciesId).baselineWeight, state.weight - 1)
        val vpGain = if (success) 25 else 5

        val updated = state.copy(
            strength = newStrength,
            effort = newEffort,
            weight = newWeight,
            vitalPoints = state.vitalPoints + vpGain,
            lastProcessedTimestampUtc = currentTimestampUtc
        )
        return resolveAttentionCall(updated, currentTimestampUtc)
    }

    /**
     * Treats illness or combat injury.
     */
    fun cureInjury(state: PetState, currentTimestampUtc: Long = state.lastProcessedTimestampUtc): PetState {
        return state.copy(
            isInjured = false,
            lastProcessedTimestampUtc = if (currentTimestampUtc > 0L) currentTimestampUtc else state.lastProcessedTimestampUtc
        )
    }

    private fun resolveAttentionCall(state: PetState, currentTimestampUtc: Long): PetState {
        val inSleep = state.isSleeping
        val needsAttention = (!inSleep && (state.hunger == 0 || state.strength == 0)) || (inSleep && !state.lightsOff)

        return if (!needsAttention && state.attentionCallActive) {
            state.copy(
                attentionCallActive = false,
                attentionCallTimestampUtc = null,
                lastProcessedTimestampUtc = currentTimestampUtc
            )
        } else {
            state
        }
    }

    /**
     * Hatches a Generation N+1 Traited Digitama inheriting stat and lifespan bonuses from the departed partner.
     */
    fun createTraitedEggState(
        departedState: PetState,
        starterSpeciesId: Int = 101,
        nowUtc: Long = System.currentTimeMillis() / 1000L
    ): PetState {
        val validEggId = if (starterSpeciesId in EvolutionEngine.STARTER_EGG_IDS) starterSpeciesId else 101
        val species = PetSpecies.getById(validEggId)
        val nextGen = departedState.generation + 1
        val bonus = (departedState.traitedEggBonus + 1).coerceAtMost(5)
        val startHp = species.baseHp + (bonus * 5)
        return PetState(
            speciesId = validEggId,
            currentHp = startHp,
            maxHp = startHp,
            hunger = 4,
            strength = 4,
            weight = species.baselineWeight,
            ageSeconds = 0L,
            careMistakes = 0,
            effort = 0,
            overfeedCount = 0,
            totalBattles = 0,
            victories = 0,
            wasteCount = 0,
            isSleeping = false,
            lightsOff = false,
            isInjured = false,
            lastProcessedTimestampUtc = nowUtc,
            generation = nextGen,
            lifespanDays = 14 + (bonus * 2),
            isDeceased = false,
            traitedEggBonus = bonus
        )
    }

    /**
     * Creates an immortalized Memorial Graveyard record of a departed partner.
     */
    fun createMemorialRecord(
        state: PetState,
        causeOfDeath: String = "OLD_AGE",
        epitaph: String = "Beloved partner of Generation ${state.generation}."
    ): com.digiwear.core.model.MemorialRecord {
        val species = PetSpecies.getById(state.speciesId)
        return com.digiwear.core.model.MemorialRecord(
            speciesId = state.speciesId,
            speciesName = species.name,
            generation = state.generation,
            finalStage = species.stage.displayName,
            ageDays = (state.ageSeconds / 86400L).toInt(),
            battlesWon = state.victories,
            battlesTotal = state.totalBattles,
            causeOfDeath = causeOfDeath,
            epitaph = epitaph,
            timestampUtc = System.currentTimeMillis() / 1000L,
            traitedEggBonusGiven = (state.traitedEggBonus + 1).coerceAtMost(5)
        )
    }

    /**
     * Authentic Bandai combat result processing:
     * Increments total battles, and increments victories on win.
     * On defeat (or knockout), rolls a canonical ~25% chance of battle injury if [combatInjuriesEnabled] is true.
     * Returns Pair<UpdatedState, WasInjuredInBattle>.
     */
    fun processBattleOutcome(
        state: PetState,
        won: Boolean,
        combatInjuriesEnabled: Boolean = true,
        injuryRoll: Int = (0..99).random()
    ): Pair<PetState, Boolean> {
        val newBattles = state.totalBattles + 1
        val newWins = if (won) state.victories + 1 else state.victories
        val getsInjured = !won && combatInjuriesEnabled && (injuryRoll < 25)
        val finalInjured = state.isInjured || getsInjured
        val vpGain = if (won) 50 else 10

        val updated = state.copy(
            totalBattles = newBattles,
            victories = newWins,
            isInjured = finalInjured,
            vitalPoints = state.vitalPoints + vpGain
        )
        return Pair(updated, getsInjured)
    }
}
