package com.digiwear.core.model

import kotlinx.serialization.Serializable

/**
 * Immutable snapshot of the virtual pet's physiological state.
 * All state decays and progressions are calculated via UTC timestamp deltas.
 */
@Serializable
data class PetState(
    val speciesId: Int = 201,
    val currentHp: Int = 100,
    val maxHp: Int = 100,
    val hunger: Int = 4,              // 0 to 4 hearts
    val strength: Int = 4,            // 0 to 4 hearts
    val weight: Int = 5,              // in grams
    val ageSeconds: Long = 0L,        // accumulated live age
    val careMistakes: Int = 0,        // accumulated care mistakes
    val effort: Int = 0,              // 0 to 4 training hearts / bonus
    val overfeedCount: Int = 0,       // feeds when hunger was 4
    val totalBattles: Int = 0,
    val victories: Int = 0,
    val wasteCount: Int = 0,          // 0 to 4 uncleaned waste units
    val isSleeping: Boolean = false,
    val lightsOff: Boolean = false,
    val isFrozen: Boolean = false,
    val isInjured: Boolean = false,
    val lastProcessedTimestampUtc: Long = 0L,
    val attentionCallActive: Boolean = false,
    val attentionCallTimestampUtc: Long? = null,
    val hungerAccumulatedSeconds: Long = 0L,
    val strengthAccumulatedSeconds: Long = 0L,
    val wasteAccumulatedSeconds: Long = 0L,
    val generation: Int = 1,
    val lifespanDays: Int = 14,
    val isDeceased: Boolean = false,
    val traitedEggBonus: Int = 0,
    val vitalPoints: Int = 0
) {
    val ageDays: Int
        get() = (ageSeconds / 86400L).toInt()
    val winRatio: Float
        get() = if (totalBattles > 0) victories.toFloat() / totalBattles else 0f

    val isHungry: Boolean
        get() = hunger == 0

    val isWeak: Boolean
        get() = strength == 0

    val isDistressed: Boolean
        get() = !lightsOff && !isSleeping && (attentionCallActive || isInjured || wasteCount >= 4)
}
