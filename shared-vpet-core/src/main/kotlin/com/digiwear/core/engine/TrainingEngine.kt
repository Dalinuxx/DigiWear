package com.digiwear.core.engine

import com.digiwear.core.model.ElementalAttribute

/**
 * Result of a training session (classic QTE or Bandai Pendulum shake).
 */
data class TrainingResult(
    val won: Boolean,
    val isMegaHit: Boolean,
    val feedbackMessage: String,
    val hits: Int = if (isMegaHit) 5 else if (won) 3 else 1
)

/**
 * Authentic Bandai V-Pet and Digimon Pendulum training logic engine.
 * Computes attribute-specific shake count sweet spots and QTE hit evaluations.
 */
object TrainingEngine {

    /**
     * Authentic Bandai Digimon Pendulum optimal shake targets:
     * - Vaccine: 5–7 shakes (Swift Heroic rhythm)
     * - Data: 6–8 shakes (Balanced Strategic rhythm)
     * - Virus: 7–9 shakes (Ferocious Aggressive rhythm)
     * - Free: 5–8 shakes (Adaptable rhythm)
     */
    fun getOptimalShakeRange(attribute: ElementalAttribute): IntRange {
        return when (attribute) {
            ElementalAttribute.VACCINE -> 5..7
            ElementalAttribute.DATA -> 6..8
            ElementalAttribute.VIRUS -> 7..9
            ElementalAttribute.FREE -> 5..8
        }
    }

    /**
     * Evaluates a physical or tap shake count against the partner's attribute range.
     */
    fun evaluateShakeTraining(attribute: ElementalAttribute, shakeCount: Int): TrainingResult {
        val range = getOptimalShakeRange(attribute)
        return when {
            shakeCount in range -> TrainingResult(
                won = true,
                isMegaHit = true,
                feedbackMessage = "MEGA HIT! $shakeCount SHAKES! (+1 Effort)",
                hits = 5
            )
            shakeCount == range.first - 1 || shakeCount == range.last + 1 -> TrainingResult(
                won = true,
                isMegaHit = false,
                feedbackMessage = "GOOD HIT! $shakeCount SHAKES! (+1 Effort)",
                hits = 3
            )
            shakeCount > 0 -> TrainingResult(
                won = false,
                isMegaHit = false,
                feedbackMessage = "MISS! $shakeCount Shakes (Target: ${range.first}-${range.last})",
                hits = 1
            )
            else -> TrainingResult(
                won = false,
                isMegaHit = false,
                feedbackMessage = "MISS! 0 Shakes (Target: ${range.first}-${range.last})",
                hits = 0
            )
        }
    }

    /**
     * Evaluates timing gauge punch in classic QTE mode.
     */
    fun evaluateQtePunch(gauge: Float): TrainingResult {
        return when {
            gauge >= 0.75f -> TrainingResult(
                won = true,
                isMegaHit = true,
                feedbackMessage = "MEGA HIT! 5/5 SMASHED! (+1 Effort)",
                hits = 5
            )
            gauge >= 0.40f -> TrainingResult(
                won = true,
                isMegaHit = false,
                feedbackMessage = "GOOD HIT! 3/5 SMASHED! (+1 Effort)",
                hits = 3
            )
            gauge >= 0.15f -> TrainingResult(
                won = false,
                isMegaHit = false,
                feedbackMessage = "MISS! 1/5 Hit (Try Again)",
                hits = 1
            )
            else -> TrainingResult(
                won = false,
                isMegaHit = false,
                feedbackMessage = "MISS! 0/5 Hit (Try Again)",
                hits = 0
            )
        }
    }
}
