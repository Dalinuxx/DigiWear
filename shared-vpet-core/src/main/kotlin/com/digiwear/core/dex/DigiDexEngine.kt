package com.digiwear.core.dex

import com.digiwear.core.engine.EvolutionEngine
import com.digiwear.core.fusion.JogressEngine
import com.digiwear.core.model.ElementalAttribute
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import kotlinx.serialization.Serializable

/**
 * Metadata record for a species in the DigiDex.
 */
@Serializable
data class SpeciesDiscoveryEntry(
    val speciesId: Int,
    val isDiscovered: Boolean,
    val firstDiscoveredTimestampUtc: Long? = null,
    val timesRaised: Int = 0,
    val timesDefeated: Int = 0
)

/**
 * Summary metrics of the Tamer's DigiDex progression.
 */
data class DigiDexSummary(
    val totalSpecies: Int,
    val discoveredCount: Int,
    val completionPercentage: Float,
    val babyCount: Int,
    val childCount: Int,
    val adultCount: Int,
    val perfectCount: Int,
    val ultimateCount: Int
)

/**
 * Canonical DigiDex engine managing collection tracking, mystery silhouette generation,
 * evolution lineage hints, and discovery progression across all 550 species.
 */
object DigiDexEngine {

    val TOTAL_SPECIES_COUNT: Int
        get() = PetSpecies.SPECIES_REGISTRY.size

    /**
     * Generates a canonical discovery hint for undiscovered species.
     */
    fun getDiscoveryHint(speciesId: Int): String {
        val species = PetSpecies.getById(speciesId)
        val preEvos = EvolutionEngine.getPreEvolutions(speciesId)

        return when {
            species.stage == PetStage.BABY_I -> "Hatchable from a canonical Starter Digi-Egg."
            preEvos.isNotEmpty() -> {
                val preSp = preEvos.first().first
                when (species.stage) {
                    PetStage.BABY_I -> "Hatchable from a canonical Starter Digi-Egg."
                    PetStage.BABY_II -> "Evolves from ${preSp.name} after 10 minutes of care."
                    PetStage.CHILD -> "Evolves from ${preSp.name} after 6 hours with tailored care mistakes."
                    PetStage.ADULT -> "Evolves from ${preSp.name} after 24 hours with battle training."
                    PetStage.PERFECT -> "Evolves from ${preSp.name} after 36 hours with >= 15 battles."
                    PetStage.ULTIMATE -> "Ascends from ${preSp.name} with >= 80% battle win ratio or Jogress."
                }
            }
            species.stage == PetStage.ULTIMATE -> {
                val recipes = JogressEngine.getRecipesByTarget(speciesId)
                if (recipes.isNotEmpty()) {
                    val r = recipes.first()
                    val spA = PetSpecies.getById(r.primarySpeciesId).name
                    val spB = PetSpecies.getById(r.secondarySpeciesId).name
                    "Achieved via ${r.fusionType.displayName} of $spA and $spB."
                } else {
                    "Apex digital entity discovered in deep digital sectors."
                }
            }
            else -> "Encountered in ancient Digital World sectors."
        }
    }

    /**
     * Calculates overall DigiDex progression metrics given a set of discovered species IDs.
     */
    fun calculateSummary(discoveredSpeciesIds: Set<Int>): DigiDexSummary {
        val allSpecies = PetSpecies.SPECIES_REGISTRY.values
        val total = allSpecies.size
        val validDiscovered = discoveredSpeciesIds.filter { PetSpecies.SPECIES_REGISTRY.containsKey(it) }.toSet()
        val count = validDiscovered.size
        val pct = if (total > 0) (count.toFloat() / total.toFloat()) * 100f else 0f

        var baby = 0
        var child = 0
        var adult = 0
        var perfect = 0
        var ultimate = 0

        for (id in validDiscovered) {
            val sp = PetSpecies.SPECIES_REGISTRY[id] ?: continue
            when (sp.stage) {
                PetStage.BABY_I, PetStage.BABY_II -> baby++
                PetStage.CHILD -> child++
                PetStage.ADULT -> adult++
                PetStage.PERFECT -> perfect++
                PetStage.ULTIMATE -> ultimate++
            }
        }

        return DigiDexSummary(
            totalSpecies = total,
            discoveredCount = count,
            completionPercentage = pct,
            babyCount = baby,
            childCount = child,
            adultCount = adult,
            perfectCount = perfect,
            ultimateCount = ultimate
        )
    }

    /**
     * Filters species in the catalog according to discovery status, stage, and attribute.
     */
    fun filterCatalog(
        discoveredSpeciesIds: Set<Int>,
        filterDiscoveredOnly: Boolean = false,
        stageFilter: PetStage? = null,
        attributeFilter: ElementalAttribute? = null,
        searchQuery: String = ""
    ): List<PetSpecies> {
        val query = searchQuery.trim().lowercase()
        return PetSpecies.SORTED_SPECIES.filter { sp ->
            val isDiscovered = sp.id in discoveredSpeciesIds
            if (filterDiscoveredOnly && !isDiscovered) return@filter false
            if (stageFilter != null && sp.stage != stageFilter) return@filter false
            if (attributeFilter != null && sp.attribute != attributeFilter) return@filter false
            if (query.isNotEmpty()) {
                val matchesName = isDiscovered && sp.name.lowercase().contains(query)
                val matchesId = "${sp.id}".contains(query)
                if (!matchesName && !matchesId) return@filter false
            }
            true
        }
    }
}
