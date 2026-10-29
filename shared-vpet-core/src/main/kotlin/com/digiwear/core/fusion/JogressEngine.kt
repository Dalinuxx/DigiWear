package com.digiwear.core.fusion

import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import kotlin.math.max

/**
 * Categorization of fusion mutations.
 */
enum class FusionType(val displayName: String, val badgeColorHex: Long) {
    DNA_JOGRESS("DNA Jogress", 0xFF64B5F6),
    DIGIXROS("DigiXros", 0xFFFFB74D),
    MODE_CHANGE("Mode Change", 0xFFBA68C8)
}

/**
 * Definition of a canonical fusion or Jogress rule between two species.
 */
data class JogressRecipe(
    val primarySpeciesId: Int,
    val secondarySpeciesId: Int,
    val targetSpeciesId: Int,
    val fusionType: FusionType,
    val description: String
)

/**
 * Engine handling DNA Digivolution (Jogress) and DigiXros mechanics.
 * Guarantees 100% backward and forward compatibility with Digimon 0nl1ne, D-Com, and A-Com:
 * The resulting fusion is a canonical [PetSpecies] that transmits via standard 128-bit Comm Codes
 * without altering protocol version or binary payload structure.
 */
object JogressEngine {

    /**
     * Canonical Jogress, DigiXros, and Mode Change catalog covering the 205-species roster.
     */
    val BASE_RECIPES: List<JogressRecipe> = listOf(
        // ==========================================
        // Ultimate & Super Ultimate Iconic Fusions
        // ==========================================
        // Omnimon (510) = WarGreymon (501) + MetalGarurumon (503)
        JogressRecipe(
            primarySpeciesId = 501,
            secondarySpeciesId = 503,
            targetSpeciesId = 510,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Holy Knight synthesis of Courage and Friendship into Omnimon."
        ),
        // Omnimon Zwart (536) = BlackWarGreymon (525) + BlackWereGarurumon (430) / BlackGarurumon (340)
        JogressRecipe(
            primarySpeciesId = 525,
            secondarySpeciesId = 430,
            targetSpeciesId = 536,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Black vaccine darkness infusion yielding Omnimon Zwart."
        ),
        JogressRecipe(
            primarySpeciesId = 525,
            secondarySpeciesId = 340,
            targetSpeciesId = 536,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Black warrior and wolf fusion into Omnimon Zwart."
        ),
        // Omnimon Alter-S (535) = WarGreymon (501) + MetalGarurumon (503) with Alter weapons
        JogressRecipe(
            primarySpeciesId = 501,
            secondarySpeciesId = 502, // WarGreymon + Machinedramon cannon tech
            targetSpeciesId = 535,
            fusionType = FusionType.DIGIXROS,
            description = "DigiXros with Heavy Armaments producing Omnimon Alter-S."
        ),
        // Mastemon (537) = Angewomon (427) + LadyDevimon (428)
        JogressRecipe(
            primarySpeciesId = 427,
            secondarySpeciesId = 428,
            targetSpeciesId = 537,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Light and Darkness harmonic equilibrium yielding Angel Mastemon."
        ),
        // Paildramon (423) = Flamedramon (332) or Veedramon (331) + Stingmon (333)
        JogressRecipe(
            primarySpeciesId = 332,
            secondarySpeciesId = 333,
            targetSpeciesId = 423,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Dragon and Insectoid DNA Jogress awakening Paildramon."
        ),
        JogressRecipe(
            primarySpeciesId = 331,
            secondarySpeciesId = 333,
            targetSpeciesId = 423,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Veedramon and Stingmon synergy awakening Paildramon."
        ),
        // Imperialdramon Dragon Mode (512) = Paildramon (423) + MetalGreymon (401) / Megadramon (416)
        JogressRecipe(
            primarySpeciesId = 423,
            secondarySpeciesId = 401,
            targetSpeciesId = 512,
            fusionType = FusionType.MODE_CHANGE,
            description = "Ancient dragon data ignition evolving Paildramon to Imperialdramon."
        ),
        JogressRecipe(
            primarySpeciesId = 423,
            secondarySpeciesId = 416,
            targetSpeciesId = 512,
            fusionType = FusionType.MODE_CHANGE,
            description = "Cyborg dragon wings ascension to Imperialdramon Dragon Mode."
        ),
        // Imperialdramon Fighter Mode (513) -> Paladin Mode (514) with Omnimon (510)
        JogressRecipe(
            primarySpeciesId = 513,
            secondarySpeciesId = 510,
            targetSpeciesId = 514,
            fusionType = FusionType.MODE_CHANGE,
            description = "Omni Sword inheritance crowning Imperialdramon Paladin Mode."
        ),
        // Alphamon Ouryuken (524) = Alphamon (523) + Cyberdramon (431) / WarGrowlmon (424)
        JogressRecipe(
            primarySpeciesId = 523,
            secondarySpeciesId = 431,
            targetSpeciesId = 524,
            fusionType = FusionType.DIGIXROS,
            description = "Ultimate blade DigiXros forging Alphamon Ouryuken."
        ),
        JogressRecipe(
            primarySpeciesId = 523,
            secondarySpeciesId = 424,
            targetSpeciesId = 524,
            fusionType = FusionType.DIGIXROS,
            description = "Dragon blade resonance forging Alphamon Ouryuken."
        ),
        // Gallantmon Crimson Mode (516) = Gallantmon (515) + AeroVeedramon (422) / Phoenixmon (506)
        JogressRecipe(
            primarySpeciesId = 515,
            secondarySpeciesId = 506,
            targetSpeciesId = 516,
            fusionType = FusionType.MODE_CHANGE,
            description = "Crimson feather ascension unlocking Gallantmon Crimson Mode."
        ),
        JogressRecipe(
            primarySpeciesId = 515,
            secondarySpeciesId = 422,
            targetSpeciesId = 516,
            fusionType = FusionType.MODE_CHANGE,
            description = "Winged steed fusion releasing Gallantmon Crimson Mode."
        ),
        // Beelzemon Blast Mode (520) = Beelzemon (519) + Andromon (418) / Megadramon (416)
        JogressRecipe(
            primarySpeciesId = 519,
            secondarySpeciesId = 418,
            targetSpeciesId = 520,
            fusionType = FusionType.DIGIXROS,
            description = "Heavy cyber cannon DigiXros granting Beelzemon Blast Mode."
        ),
        JogressRecipe(
            primarySpeciesId = 519,
            secondarySpeciesId = 416,
            targetSpeciesId = 520,
            fusionType = FusionType.DIGIXROS,
            description = "Dragon cannon DigiXros granting Beelzemon Blast Mode."
        ),
        // Millenniummon (546) = Machinedramon (502) + DarkTyrannomon (316) / Megadramon (416)
        JogressRecipe(
            primarySpeciesId = 502,
            secondarySpeciesId = 316,
            targetSpeciesId = 546,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Chrono-fusion of mechanical terror creating Millenniummon."
        ),
        JogressRecipe(
            primarySpeciesId = 502,
            secondarySpeciesId = 416,
            targetSpeciesId = 546,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Dark dragon cyborg fusion creating Millenniummon."
        ),
        // MoonMillenniummon (547) = Millenniummon (546) + Diaboromon (527)
        JogressRecipe(
            primarySpeciesId = 546,
            secondarySpeciesId = 527,
            targetSpeciesId = 547,
            fusionType = FusionType.MODE_CHANGE,
            description = "Spiritual matrix collapse awakening MoonMillenniummon."
        ),
        // ZeedMillenniummon (548) = MoonMillenniummon (547) + Apocalymon (529)
        JogressRecipe(
            primarySpeciesId = 547,
            secondarySpeciesId = 529,
            targetSpeciesId = 548,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Chrono-chains bind breaking into ZeedMillenniummon."
        ),
        // Apocalymon (529) = Piedmon (509) + Machinedramon (502)
        JogressRecipe(
            primarySpeciesId = 509,
            secondarySpeciesId = 502,
            targetSpeciesId = 529,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Dark Masters convergence forging Apocalymon."
        ),
        // Susanoomon (549) = WarGreymon (501) + Magnamon (521) / HerculesKabuterimon (507)
        JogressRecipe(
            primarySpeciesId = 501,
            secondarySpeciesId = 521,
            targetSpeciesId = 549,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Legendary warrior spirit synthesis into God of Destruction Susanoomon."
        ),

        // ==========================================
        // Classic Pendulum Adult & Perfect Jogress
        // ==========================================
        // Greymon (301) + Garurumon (306) -> MetalGreymon (401)
        JogressRecipe(
            primarySpeciesId = 301,
            secondarySpeciesId = 306,
            targetSpeciesId = 401,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Courage and Friendship early Jogress into MetalGreymon."
        ),
        // Angemon (307) + Devimon (303) -> Lucemon Chaos Mode (434)
        JogressRecipe(
            primarySpeciesId = 307,
            secondarySpeciesId = 303,
            targetSpeciesId = 434,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Holy Angel and Fallen Demon convergence into Lucemon Chaos Mode."
        ),
        // Gatomon (338) + Wizardmon (337) -> Angewomon (427)
        JogressRecipe(
            primarySpeciesId = 338,
            secondarySpeciesId = 337,
            targetSpeciesId = 427,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Sorcery and holy bond Jogress into Angewomon."
        ),
        // Devimon (303) + Ogremon (312) -> SkullGreymon (403)
        JogressRecipe(
            primarySpeciesId = 303,
            secondarySpeciesId = 312,
            targetSpeciesId = 403,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Corrupted dark malice fusion into SkullGreymon."
        ),
        // Growlmon (334) + BaoHackmon (341) -> WarGrowlmon (424)
        JogressRecipe(
            primarySpeciesId = 334,
            secondarySpeciesId = 341,
            targetSpeciesId = 424,
            fusionType = FusionType.DNA_JOGRESS,
            description = "Dragon warrior resonance Jogress into WarGrowlmon."
        )
    )

    /** Base catalog plus the expanded-roster recipes. */
    val RECIPES: List<JogressRecipe> by lazy { BASE_RECIPES + com.digiwear.core.model.ExpandedRoster.RECIPES }

    private val RECIPES_BY_SPECIES: Map<Int, List<JogressRecipe>> by lazy {
        val map = mutableMapOf<Int, MutableList<JogressRecipe>>()
        for (recipe in RECIPES) {
            map.getOrPut(recipe.primarySpeciesId) { mutableListOf() }.add(recipe)
            if (recipe.secondarySpeciesId != recipe.primarySpeciesId) {
                map.getOrPut(recipe.secondarySpeciesId) { mutableListOf() }.add(recipe)
            }
        }
        map
    }

    private val RECIPES_BY_TARGET: Map<Int, List<JogressRecipe>> by lazy {
        val map = mutableMapOf<Int, MutableList<JogressRecipe>>()
        for (recipe in RECIPES) {
            map.getOrPut(recipe.targetSpeciesId) { mutableListOf() }.add(recipe)
        }
        map
    }

    private val RECIPES_BY_ANY_SPECIES: Map<Int, List<JogressRecipe>> by lazy {
        val map = mutableMapOf<Int, MutableList<JogressRecipe>>()
        for (recipe in RECIPES) {
            map.getOrPut(recipe.primarySpeciesId) { mutableListOf() }.add(recipe)
            if (recipe.secondarySpeciesId != recipe.primarySpeciesId) {
                map.getOrPut(recipe.secondarySpeciesId) { mutableListOf() }.add(recipe)
            }
            if (recipe.targetSpeciesId != recipe.primarySpeciesId && recipe.targetSpeciesId != recipe.secondarySpeciesId) {
                map.getOrPut(recipe.targetSpeciesId) { mutableListOf() }.add(recipe)
            }
        }
        map
    }

    /**
     * Looks up any valid fusion recipe between two species (commutative: order independent).
     */
    fun findFusionRecipe(speciesAId: Int, speciesBId: Int): JogressRecipe? {
        val candidates = RECIPES_BY_SPECIES[speciesAId] ?: return null
        return candidates.firstOrNull { recipe ->
            (recipe.primarySpeciesId == speciesAId && recipe.secondarySpeciesId == speciesBId) ||
            (recipe.primarySpeciesId == speciesBId && recipe.secondarySpeciesId == speciesAId)
        }
    }

    /**
     * Returns all compatible partner species that can fuse with [speciesId].
     */
    fun getCompatibleRecipesFor(speciesId: Int): List<JogressRecipe> {
        return RECIPES_BY_SPECIES[speciesId] ?: emptyList()
    }

    /**
     * Returns all fusion recipes whose result is [targetSpeciesId] in O(1) time.
     */
    fun getRecipesByTarget(targetSpeciesId: Int): List<JogressRecipe> {
        return RECIPES_BY_TARGET[targetSpeciesId] ?: emptyList()
    }

    /**
     * Returns all fusion recipes where [speciesId] participates as primary, secondary, or target in O(1) time.
     */
    fun getRecipesForSpecies(speciesId: Int): List<JogressRecipe> {
        return RECIPES_BY_ANY_SPECIES[speciesId] ?: emptyList()
    }

    /**
     * Executes the fusion mutation, combining battle experience, granting synergy stat bonuses,
     * resetting stage decay counters, and returning the updated [PetState].
     *
     * @param primary Current state of the active pet.
     * @param secondarySpeciesId ID of the secondary fusion partner.
     * @param secondaryVictories Victories accumulated by the partner.
     * @param secondaryEffort Training effort of the partner.
     * @param currentTimestampUtc Current UTC timestamp in seconds.
     */
    fun executeFusion(
        primary: PetState,
        secondarySpeciesId: Int,
        secondaryVictories: Int = 0,
        secondaryEffort: Int = 0,
        currentTimestampUtc: Long = System.currentTimeMillis() / 1000L
    ): PetState {
        require(!primary.isDeceased) { "Cannot execute Jogress fusion on a deceased partner" }
        val recipe = findFusionRecipe(primary.speciesId, secondarySpeciesId)
            ?: throw IllegalArgumentException("No valid Jogress/Xros recipe exists between species ${primary.speciesId} and $secondarySpeciesId")

        val targetSpecies = PetSpecies.getById(recipe.targetSpeciesId)

        // Fusion synergy calculations:
        // Max HP adapts to target species base HP + 10% bonus for DNA harmony + traited egg bonus
        val synergyHp = (targetSpecies.baseHp * 1.10f).toInt() + (primary.traitedEggBonus.coerceAtLeast(0) * 5)
        val combinedWins = primary.victories + secondaryVictories.coerceAtLeast(0)
        val combinedBattles = max(primary.totalBattles, combinedWins)
        val combinedEffort = max(primary.effort, secondaryEffort).coerceIn(0, 4)
        val targetWeight = max(primary.weight, targetSpecies.baselineWeight)

        return primary.copy(
            speciesId = targetSpecies.id,
            maxHp = synergyHp,
            currentHp = synergyHp,
            weight = targetWeight,
            victories = combinedWins,
            totalBattles = combinedBattles,
            effort = combinedEffort,
            // Reset lifecycle mistake counters upon form ascension
            careMistakes = 0,
            overfeedCount = 0,
            lastProcessedTimestampUtc = currentTimestampUtc,
            attentionCallActive = false,
            attentionCallTimestampUtc = null
        )
    }
}
