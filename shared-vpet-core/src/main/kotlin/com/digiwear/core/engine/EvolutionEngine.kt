package com.digiwear.core.engine

import com.digiwear.core.model.EvolutionRule
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import com.digiwear.core.model.PetState
import kotlin.math.max

/**
 * Evolution Decision Engine evaluating multi-variable condition vectors against species branch tables,
 * plus lineage graph introspection for Watch and Phone DigiDex guides.
 */
object EvolutionEngine {

    /**
     * All 16 hatchable Baby I starter Digi-Eggs across the 500-species roster.
     */
    val STARTER_EGG_IDS: List<Int> = listOf(
        101, 103, 105, 107, 109, 111, 113, 130, 131, 132,
        1101, 1104, 1106, 1108, 1111, 1113
    )

    private val PRE_EVOLUTION_INDEX: Map<Int, List<Pair<PetSpecies, EvolutionRule>>> by lazy {
        val map = mutableMapOf<Int, MutableList<Pair<PetSpecies, EvolutionRule>>>()
        for (species in PetSpecies.SORTED_SPECIES) {
            for (rule in species.evolutionRules) {
                map.getOrPut(rule.targetSpeciesId) { mutableListOf() }.add(species to rule)
            }
        }
        map
    }

    private val EGG_LINEAGE_PATH_INDEX: Map<Int, List<Int>> by lazy {
        val paths = mutableMapOf<Int, List<Int>>()
        val visited = mutableSetOf<Int>()
        val parentMap = mutableMapOf<Int, Int>()
        val queue = ArrayDeque<Int>()

        for (eggId in STARTER_EGG_IDS) {
            if (PetSpecies.SPECIES_REGISTRY.containsKey(eggId)) {
                visited.add(eggId)
                paths[eggId] = listOf(eggId)
                queue.addLast(eggId)
            }
        }

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val species = PetSpecies.SPECIES_REGISTRY[currentId] ?: continue
            for (rule in species.evolutionRules) {
                val nextId = rule.targetSpeciesId
                if (nextId !in visited && PetSpecies.SPECIES_REGISTRY.containsKey(nextId)) {
                    visited.add(nextId)
                    parentMap[nextId] = currentId
                    val path = mutableListOf<Int>()
                    var cursor: Int? = nextId
                    while (cursor != null) {
                        path.add(0, cursor)
                        cursor = parentMap[cursor]
                    }
                    paths[nextId] = path
                    queue.addLast(nextId)
                }
            }
        }
        paths
    }

    /**
     * Evaluates whether the current [state] qualifies for an evolutionary mutation.
     * Returns the target [PetSpecies] if an evolution occurs, or null if no mutation conditions are met.
     */
    fun evaluateEvolution(state: PetState): PetSpecies? {
        val currentSpecies = PetSpecies.getById(state.speciesId)
        val candidateRules = currentSpecies.evolutionRules

        for (rule in candidateRules) {
            if (rule.matches(state)) {
                return PetSpecies.getById(rule.targetSpeciesId)
            }
        }
        return null
    }

    /**
     * Applies the evolutionary mutation to [state], adapting HP, baseline weight, and resetting stage counters.
     */
    fun evolve(state: PetState, targetSpecies: PetSpecies, currentTimestampUtc: Long): PetState {
        val newMaxHp = targetSpecies.baseHp + (state.traitedEggBonus.coerceAtLeast(0) * 5)
        val newWeight = max(state.weight, targetSpecies.baselineWeight)

        return state.copy(
            speciesId = targetSpecies.id,
            maxHp = newMaxHp,
            currentHp = newMaxHp,
            weight = newWeight,
            // Stage-specific care mistake and effort counters reset upon form mutation
            careMistakes = 0,
            effort = 0,
            overfeedCount = 0,
            lastProcessedTimestampUtc = currentTimestampUtc,
            attentionCallActive = false,
            attentionCallTimestampUtc = null
        )
    }

    /**
     * Returns all species and the specific [EvolutionRule] that can directly evolve into [targetSpeciesId].
     */
    fun getPreEvolutions(targetSpeciesId: Int): List<Pair<PetSpecies, EvolutionRule>> {
        return PRE_EVOLUTION_INDEX[targetSpeciesId] ?: emptyList()
    }

    /**
     * Finds the shortest canonical natural evolution path from one of the 16 Baby I starter eggs
     * to [targetSpeciesId]. Returns the list of species IDs from Baby I to [targetSpeciesId].
     */
    fun findEggLineagePath(targetSpeciesId: Int): List<Int> {
        return EGG_LINEAGE_PATH_INDEX[targetSpeciesId] ?: emptyList()
    }

    /**
     * Formats an [EvolutionRule] into a concise human-readable requirement summary for Watch/Phone UI.
     */
    fun describeRule(rule: EvolutionRule): String {
        val parts = mutableListOf<String>()
        if (rule.minAgeSeconds > 0L) {
            val hours = rule.minAgeSeconds / 3600L
            parts.add("Age ≥ ${hours}h")
        }
        if (rule.minCareMistakes > 0 || rule.maxCareMistakes < 999) {
            val cmText = when {
                rule.minCareMistakes == rule.maxCareMistakes -> "CM = ${rule.minCareMistakes}"
                rule.maxCareMistakes >= 999 -> "CM ≥ ${rule.minCareMistakes}"
                else -> "CM ${rule.minCareMistakes}–${rule.maxCareMistakes}"
            }
            parts.add(cmText)
        }
        if (rule.minEffort > 0) parts.add("Effort ≥ ${rule.minEffort}/4")
        if (rule.minBattles > 0) parts.add("Battles ≥ ${rule.minBattles}")
        if (rule.minWinRatio > 0f) parts.add("Win ≥ ${(rule.minWinRatio * 100).toInt()}%")
        if (rule.minWeight > 0) parts.add("Weight ≥ ${rule.minWeight}G")
        if (rule.minOverfeed > 0) parts.add("Overfeed ≥ ${rule.minOverfeed}")
        if (rule.minVitalPoints > 0) parts.add("Vitals ≥ ${rule.minVitalPoints} VP (Optional)")
        return if (parts.isEmpty()) "Natural Ascension" else parts.joinToString(" • ")
    }

    data class ConditionItem(
        val label: String,
        val current: String,
        val target: String,
        val isMet: Boolean
    )

    data class EvolutionReadiness(
        val targetSpecies: PetSpecies,
        val overallMet: Boolean,
        val conditions: List<ConditionItem>,
        val completionPercentage: Int
    )

    /**
     * Evaluates a fine-grained, condition-by-condition telemetry checklist of [rule] against [state],
     * returning specific numerical progress, completion percentage, and pass/fail status per parameter.
     */
    fun evaluateConditionDetails(state: PetState, rule: EvolutionRule): EvolutionReadiness {
        val items = mutableListOf<ConditionItem>()
        var metCount = 0
        var totalCount = 0

        if (rule.minAgeSeconds > 0L) {
            totalCount++
            val curH = state.ageSeconds / 3600L
            val requiredAge = rule.effectiveMinAgeSeconds(state)
            val reqH = requiredAge / 3600L
            val met = state.ageSeconds >= requiredAge
            if (met) metCount++
            items.add(ConditionItem("Age", "${curH}h", "${reqH}h", met))
        }

        if (rule.minCareMistakes > 0 || rule.maxCareMistakes < 999) {
            totalCount++
            val curCm = state.careMistakes
            val met = curCm >= rule.minCareMistakes && curCm <= rule.maxCareMistakes
            if (met) metCount++
            val tgt = if (rule.maxCareMistakes >= 999) "≥${rule.minCareMistakes}" else "${rule.minCareMistakes}–${rule.maxCareMistakes}"
            items.add(ConditionItem("Care Mistakes", "$curCm", tgt, met))
        }

        if (rule.minEffort > 0) {
            totalCount++
            val met = state.effort >= rule.minEffort
            if (met) metCount++
            items.add(ConditionItem("Effort", "${state.effort}/4", "≥${rule.minEffort}/4", met))
        }

        if (rule.minBattles > 0) {
            totalCount++
            val met = state.totalBattles >= rule.minBattles
            if (met) metCount++
            items.add(ConditionItem("Battles", "${state.totalBattles}", "≥${rule.minBattles}", met))
        }

        if (rule.minWinRatio > 0f) {
            totalCount++
            val curWin = (state.winRatio * 100).toInt()
            val reqWin = (rule.minWinRatio * 100).toInt()
            val met = state.winRatio >= rule.minWinRatio
            if (met) metCount++
            items.add(ConditionItem("Win Ratio", "$curWin%", "≥$reqWin%", met))
        }

        if (rule.minOverfeed > 0) {
            totalCount++
            val met = state.overfeedCount >= rule.minOverfeed
            if (met) metCount++
            items.add(ConditionItem("Overfeed", "${state.overfeedCount}", "≥${rule.minOverfeed}", met))
        }

        if (rule.minWeight > 0) {
            totalCount++
            val met = state.weight >= rule.minWeight
            if (met) metCount++
            items.add(ConditionItem("Weight", "${state.weight}g", "≥${rule.minWeight}g", met))
        }

        // Vital Points are an optional fast-track bonus: listed for visibility but never counted
        // toward the required-condition completion percentage.
        if (rule.minVitalPoints > 0) {
            val met = state.vitalPoints >= rule.minVitalPoints
            items.add(ConditionItem("Vital Points (Optional)", "${state.vitalPoints} VP", "≥${rule.minVitalPoints} VP", met))
        }

        val pct = if (totalCount == 0) 100 else ((metCount.toFloat() / totalCount.toFloat()) * 100).toInt()
        val isAllMet = rule.matches(state)
        val target = PetSpecies.getById(rule.targetSpeciesId)

        return EvolutionReadiness(
            targetSpecies = target,
            overallMet = isAllMet,
            conditions = items,
            completionPercentage = pct
        )
    }
}
