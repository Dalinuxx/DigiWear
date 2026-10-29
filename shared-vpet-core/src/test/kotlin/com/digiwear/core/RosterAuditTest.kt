package com.digiwear.core

import com.digiwear.core.codec.CommCodeCodec
import com.digiwear.core.fusion.FusionType
import com.digiwear.core.fusion.JogressEngine
import com.digiwear.core.model.ElementalAttribute
import com.digiwear.core.model.ExpandedRoster
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RosterAuditTest {

    @Test
    fun auditAllSpeciesAndEvolutions() {
        val registry = PetSpecies.SPECIES_REGISTRY
        println("=== TOTAL REGISTERED SPECIES: ${registry.size} ===")

        val stageCounts = registry.values.groupBy { it.stage }.mapValues { it.value.size }
        println("Stage distribution: $stageCounts")

        val invalidTargets = mutableListOf<String>()
        val stageSkips = mutableListOf<String>()
        val backwardEvolutions = mutableListOf<String>()
        val lateralEvolutions = mutableListOf<String>()

        for (sp in registry.values) {
            assertTrue("Species ${sp.name} has empty spriteKey", sp.spriteKey.isNotBlank())
            assertTrue("Species ${sp.name} baseHp must be > 0", sp.baseHp > 0)
            assertTrue("Species ${sp.name} basePower must be > 0", sp.basePower > 0)
            assertTrue("Species ${sp.name} baselineWeight must be > 0", sp.baselineWeight > 0)

            // Verify strict ID-to-stage range mapping across all 550 species
            when (sp.id) {
                in 100..199, in 1100..1199 -> assertTrue("${sp.name} (${sp.id}) must be Baby", sp.stage == PetStage.BABY_I || sp.stage == PetStage.BABY_II)
                in 200..299, in 1200..1299 -> assertEquals("${sp.name} (${sp.id}) must be Child", PetStage.CHILD, sp.stage)
                in 300..399, in 1300..1399 -> assertEquals("${sp.name} (${sp.id}) must be Adult", PetStage.ADULT, sp.stage)
                in 400..499, in 1400..1499 -> assertEquals("${sp.name} (${sp.id}) must be Perfect", PetStage.PERFECT, sp.stage)
                in 500..599, in 1500..1599 -> assertEquals("${sp.name} (${sp.id}) must be Ultimate", PetStage.ULTIMATE, sp.stage)
                else -> throw AssertionError("Unknown ID range for species: ${sp.id} ${sp.name}")
            }

            for (rule in sp.evolutionRules) {
                val target = registry[rule.targetSpeciesId]
                if (target == null) {
                    invalidTargets.add("${sp.id} (${sp.name}) -> ${rule.targetSpeciesId} (MISSING)")
                    continue
                }

                val stageDiff = target.stage.stageIndex - sp.stage.stageIndex
                when {
                    stageDiff < 0 -> backwardEvolutions.add("${sp.name} (${sp.stage}) -> ${target.name} (${target.stage})")
                    stageDiff == 0 -> lateralEvolutions.add("${sp.name} (${sp.stage}) -> ${target.name} (${target.stage})")
                    stageDiff > 1 -> {
                        // Allow only canonical exceptions: Lucemon (Child->Perfect) and Flamedramon (Adult->Magnamon Ultimate)
                        val isAllowedSkip = (sp.id == 221 && target.id == 434) || (sp.id == 332 && target.id == 521)
                        if (!isAllowedSkip) {
                            stageSkips.add("${sp.name} (${sp.stage}) -> ${target.name} (${target.stage}) [jump: +$stageDiff]")
                        }
                    }
                }
            }
        }

        println("=== EVOLUTION AUDIT RESULTS ===")
        println("Invalid targets (${invalidTargets.size}): $invalidTargets")
        println("Stage skips (${stageSkips.size}): $stageSkips")
        println("Backward evolutions (${backwardEvolutions.size}): $backwardEvolutions")
        println("Lateral evolutions (${lateralEvolutions.size}): $lateralEvolutions")

        assertTrue("Found invalid evolution targets: $invalidTargets", invalidTargets.isEmpty())
        assertTrue("Found uncanonical stage skips: $stageSkips", stageSkips.isEmpty())
        assertTrue("Found backward evolutions: $backwardEvolutions", backwardEvolutions.isEmpty())
    }

    @Test
    fun auditCanonicalAdventure02JogressAndAscensions() {
        val registry = PetSpecies.SPECIES_REGISTRY

        // 1. Gatomon (338) + Aquilamon (344) -> Silphymon (437, PERFECT)
        val silphyRecipe = JogressEngine.findFusionRecipe(338, 344)
        assertNotNull("Silphymon Jogress recipe missing", silphyRecipe)
        assertEquals(437, silphyRecipe!!.targetSpeciesId)
        assertEquals(PetStage.PERFECT, registry[437]!!.stage)

        // 2. Angemon (307) + Ankylomon (345) -> Shakkoumon (441, PERFECT)
        val shakkouRecipe = JogressEngine.findFusionRecipe(307, 345)
        assertNotNull("Shakkoumon Jogress recipe missing", shakkouRecipe)
        assertEquals(441, shakkouRecipe!!.targetSpeciesId)
        assertEquals(PetStage.PERFECT, registry[441]!!.stage)

        // 3. Veedramon/Flamedramon + Stingmon -> Paildramon (423, PERFECT)
        val paildraRecipe = JogressEngine.findFusionRecipe(331, 333)
        assertNotNull("Paildramon Jogress recipe missing", paildraRecipe)
        assertEquals(423, paildraRecipe!!.targetSpeciesId)
        assertEquals(PetStage.PERFECT, registry[423]!!.stage)

        // 4. Shakkoumon (441) ascends into Vikemon (553)
        val shakkoumon = registry[441]!!
        val shakkouTargets = shakkoumon.evolutionRules.map { it.targetSpeciesId }
        assertTrue("Shakkoumon must evolve into Vikemon (553)", 553 in shakkouTargets)

        // 5. Angewomon (427) evolves into Magnadramon (556) and Ophanimon (552)
        val angewomon = registry[427]!!
        val angewomonTargets = angewomon.evolutionRules.map { it.targetSpeciesId }
        assertTrue("Angewomon must evolve into Magnadramon (556)", 556 in angewomonTargets)
        assertTrue("Angewomon must evolve into Ophanimon (552)", 552 in angewomonTargets)

        // 6. Magnadramon (556) must be ULTIMATE
        val magnadramon = registry[556]!!
        assertEquals("Magnadramon must be ULTIMATE", PetStage.ULTIMATE, magnadramon.stage)
        assertEquals("Magnadramon must be Vaccine", ElementalAttribute.VACCINE, magnadramon.attribute)
    }

    @Test
    fun auditDigimon0nl1neCompatibilityForAllSpecies() {
        val registry = PetSpecies.SPECIES_REGISTRY
        assertEquals(550, registry.size)

        for (sp in registry.values) {
            // Guarantee 12-bit monster ID for Digimon 0nl1ne / D-Com wire packets (0x001..0xFFF)
            assertTrue("Species ${sp.name} (${sp.id}) exceeds 12-bit limit", sp.id in 1..0x0FFF)
            assertTrue("Base power must fit 12-bit limit", sp.basePower in 1..0x0FFF)

            // DigiROM test: encode to V1-XXXX-XXXX-XXXX and decode
            val digiRom = CommCodeCodec.encodeDigiRom(
                monsterId = sp.id,
                stage = sp.stage,
                attribute = sp.attribute,
                basePowerHp = sp.basePower,
                battleRngSeed = 0xABCD
            )
            val romPayload = CommCodeCodec.decodeDigiRom(digiRom)
            assertEquals("DigiROM ID roundtrip mismatch for ${sp.name}", sp.id, romPayload.monsterId)
            assertEquals("DigiROM Stage roundtrip mismatch for ${sp.name}", sp.stage, romPayload.stage)
            assertEquals("DigiROM Attribute roundtrip mismatch for ${sp.name}", sp.attribute, romPayload.attribute)

            // 128-bit Base32 test: encode and decode
            val b32 = CommCodeCodec.encode128(
                monsterId = sp.id,
                stage = sp.stage,
                attribute = sp.attribute,
                basePowerHp = sp.basePower,
                historicalVictories = 10,
                trainingEffort = 4,
                battleRngSeed = 0x4321
            )
            val b32Payload = CommCodeCodec.decode128(b32)
            assertEquals("Base32 ID roundtrip mismatch for ${sp.name}", sp.id, b32Payload.monsterId)
            assertEquals("Base32 Stage roundtrip mismatch for ${sp.name}", sp.stage, b32Payload.stage)
            assertEquals("Base32 Attribute roundtrip mismatch for ${sp.name}", sp.attribute, b32Payload.attribute)

            // 16-character compact code test: encode and decode
            val b16 = CommCodeCodec.encode16(
                monsterId = sp.id,
                stage = sp.stage,
                attribute = sp.attribute,
                basePowerHp = sp.basePower,
                battleRngSeed = 0x5678
            )
            val b16Payload = CommCodeCodec.decode16(b16)
            assertEquals("Base16 ID roundtrip mismatch for ${sp.name}", sp.id, b16Payload.monsterId)
            assertEquals("Base16 Stage roundtrip mismatch for ${sp.name}", sp.stage, b16Payload.stage)
            assertEquals("Base16 Attribute roundtrip mismatch for ${sp.name}", sp.attribute, b16Payload.attribute)
        }
    }

    @Test
    fun auditPerfectToUltimateRequirementsAndAll92JogressRecipes() {
        val registry = PetSpecies.SPECIES_REGISTRY
        val perfectSpecies = registry.values.filter { it.stage == PetStage.PERFECT && it.evolutionRules.isNotEmpty() }
        assertTrue("Must have Perfect species with evolution rules", perfectSpecies.isNotEmpty())

        for (sp in perfectSpecies) {
            for (rule in sp.evolutionRules) {
                val target = registry[rule.targetSpeciesId]!!
                assertEquals("Perfect species ${sp.name} must evolve to ULTIMATE", PetStage.ULTIMATE, target.stage)
                assertTrue(
                    "Perfect -> Ultimate rule for ${sp.name} -> ${target.name} must require >= 3 days (259200s), had ${rule.minAgeSeconds}",
                    rule.minAgeSeconds >= 3 * 86400L
                )
                assertTrue(
                    "Perfect -> Ultimate rule for ${sp.name} -> ${target.name} must require >= 20 battles, had ${rule.minBattles}",
                    rule.minBattles >= 20
                )
            }
        }

        // Verify all 92 Jogress / DigiXros / Mode Change recipes reference valid species
        assertEquals(92, JogressEngine.RECIPES.size)
        for (recipe in JogressEngine.RECIPES) {
            assertTrue("Missing primary species ${recipe.primarySpeciesId}", registry.containsKey(recipe.primarySpeciesId))
            assertTrue("Missing secondary species ${recipe.secondarySpeciesId}", registry.containsKey(recipe.secondarySpeciesId))
            assertTrue("Missing target species ${recipe.targetSpeciesId}", registry.containsKey(recipe.targetSpeciesId))
        }
    }

    @Test
    fun audit100PercentReachabilityFromStarterEggsAndZeroShadowedRules() {
        val registry = PetSpecies.SPECIES_REGISTRY
        val unreachableSpecies = mutableListOf<String>()

        for (sp in registry.values.sortedBy { it.id }) {
            val path = com.digiwear.core.engine.EvolutionEngine.findEggLineagePath(sp.id)
            if (path.isEmpty()) {
                unreachableSpecies.add("#${sp.id} ${sp.name} (${sp.stage})")
            } else {
                assertTrue("Lineage path for ${sp.name} must start with a starter egg", path.first() in com.digiwear.core.engine.EvolutionEngine.STARTER_EGG_IDS)
                assertEquals("Lineage path for ${sp.name} must end at ${sp.id}", sp.id, path.last())
            }
        }
        assertTrue(
            "All 550 species must be reachable from the 16 starter eggs! Unreachable: $unreachableSpecies",
            unreachableSpecies.isEmpty()
        )

        // Verify that every single EvolutionRule in every species' evolutionRules list can be triggered
        // as the FIRST matching rule by at least one concrete PetState (zero shadowed rules).
        val shadowedRules = mutableListOf<String>()
        for (sp in registry.values.sortedBy { it.id }) {
            for ((index, rule) in sp.evolutionRules.withIndex()) {
                var canTrigger = false
                val targetAge = rule.minAgeSeconds.coerceAtLeast(300_000L)
                val cmCandidates = listOf(0, 1, 2, 3, 4, 5, 6, 7)
                val effortCandidates = listOf(0, 1, 2, 3, 4)
                val overfeedCandidates = listOf(0, 2, 3, 4)
                val battleCandidates = listOf(0, 10, 12, 15, 20, 25, 30, 35, 40)
                val winRatioCandidates = listOf(0.0f, 0.60f, 0.65f, 0.70f, 0.75f, 0.80f, 0.85f, 0.90f, 1.0f)
                val weightCandidates = listOf(10, 20, 25, 28, 30, 40)

                outer@ for (cm in cmCandidates) {
                    for (effort in effortCandidates) {
                        for (overfeed in overfeedCandidates) {
                            for (battles in battleCandidates) {
                                for (winRatio in winRatioCandidates) {
                                    val wins = kotlin.math.ceil(battles * winRatio).toInt()
                                    for (weight in weightCandidates) {
                                        val testState = com.digiwear.core.model.PetState(
                                            speciesId = sp.id,
                                            ageSeconds = targetAge,
                                            careMistakes = cm,
                                            effort = effort,
                                            overfeedCount = overfeed,
                                            totalBattles = battles,
                                            victories = wins,
                                            weight = weight
                                        )
                                        val evolved = com.digiwear.core.engine.EvolutionEngine.evaluateEvolution(testState)
                                        if (evolved?.id == rule.targetSpeciesId) {
                                            canTrigger = true
                                            break@outer
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (!canTrigger) {
                    val targetName = registry[rule.targetSpeciesId]?.name ?: "?"
                    shadowedRules.add("#${sp.id} ${sp.name} rule[$index] -> #${rule.targetSpeciesId} $targetName (${com.digiwear.core.engine.EvolutionEngine.describeRule(rule)})")
                }
            }
        }
        assertTrue(
            "Zero evolution rules may be shadowed by earlier looser rules! Shadowed: $shadowedRules",
            shadowedRules.isEmpty()
        )
    }

    @Test
    fun generateAuthoritativeDigivolutionGuideMarkdown() {
        if (System.getProperty("generateDocs") != "true") {
            // Fast CI path: skip heavy 160MB poster rendering during standard test passes
            return
        }
        val registry = PetSpecies.SPECIES_REGISTRY.values.sortedBy { it.id }
        val docsDir = java.io.File("../docs").apply { mkdirs() }
        val rosterSheetFile = java.io.File(docsDir, "digiwear_roster_spritesheet.png")
        val evoChartFile = java.io.File(docsDir, "digiwear_evolution_guide_chart.png")

        renderMasterRosterSpriteSheetPng(registry, rosterSheetFile)
        renderVisualEvolutionGuideChartPng(registry, evoChartFile)

        val sb = StringBuilder()

        sb.appendLine("# DigiWear Official Visual Sprite Sheet Digivolution & DNA Jogress Guide (550 Species)")
        sb.appendLine()
        sb.appendLine("> **100% Complete & Mathematically Verified Lineage Chart + Full Pixel-Art Sprite Sheets**")
        sb.appendLine("> Every single one of the **550 Digimon** in DigiWear is naturally reachable from the **16 Starter Digi-Eggs** via canonical Bandai 1997 V-Pet, Digimon Pendulum, and Digimon Color evolution rules, or via **${JogressEngine.RECIPES.size} DNA Jogress / DigiXros / Mode Change** combinations.")
        sb.appendLine()
        sb.appendLine("## Visual Sprite Sheet Evolution Chart & Master 550-Species Roster")
        sb.appendLine()
        sb.appendLine("### 1. Full Visual Sprite Sheet Evolution & DNA Jogress Guide Poster")
        sb.appendLine("![DigiWear Full Sprite Sheet Evolution Guide](docs/digiwear_evolution_guide_chart.png)")
        sb.appendLine()
        sb.appendLine("### 2. Complete 550-Species Master Pixel-Art Sprite Sheet")
        sb.appendLine("![DigiWear 550-Species Master Roster Sprite Sheet](docs/digiwear_roster_spritesheet.png)")
        sb.appendLine()
        sb.appendLine("## Roster & Evolution Summary")
        sb.appendLine()
        sb.appendLine("| Stage | Species Count | Canonical Evolution Window | Key Branching Factors |")
        sb.appendLine("| :--- | :---: | :--- | :--- |")
        for (stage in PetStage.entries) {
            val count = registry.count { it.stage == stage }
            val window = when (stage) {
                PetStage.BABY_I -> "Hatch → 10 Mins (`600s`)"
                PetStage.BABY_II -> "6 Hours (`21,600s`)"
                PetStage.CHILD -> "24 Hours (`86,400s`)"
                PetStage.ADULT -> "36 Hours (`129,600s`)"
                PetStage.PERFECT -> "72 Hours (`259,200s`)"
                PetStage.ULTIMATE -> "Apex / Mode Change / Jogress"
            }
            val factors = when (stage) {
                PetStage.BABY_I -> "Starter Digi-Egg Selection (16 Lineages)"
                PetStage.BABY_II -> "Care Mistakes (`CM`), Training Effort (`EFF`), Weight (`G`)"
                PetStage.CHILD -> "Care Mistakes (`CM`), Training Effort (`EFF`), Overfeed Count, Weight (`G`)"
                PetStage.ADULT -> "Battles (`≥ 10–15`), Win Ratio (`≥ 60%–80%`), Care Mistakes (`CM`), Effort (`EFF`)"
                PetStage.PERFECT -> "Battles (`≥ 20–35`), Win Ratio (`≥ 60%–85%`), Care Mistakes (`CM`), Effort (`EFF`)"
                PetStage.ULTIMATE -> "DNA Jogress, Mode Change, or Super-Ultimate Fusion"
            }
            sb.appendLine("| **${stage.displayName}** | **$count** | $window | $factors |")
        }
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("## 1. The 16 Starter Digi-Eggs (Baby I → Baby II)")
        sb.appendLine()
        sb.appendLine("You can hatch any of these **16 Starter Digi-Eggs** at any time from **Watch Settings → Hatch New Digi-Egg** or **Phone Companion → Remote Care Deck**:")
        sb.appendLine()
        sb.appendLine("| Egg ID | Baby I (Fresh) | Evolves Into (Baby II Targets & Conditions) | Version / Family Origin |")
        sb.appendLine("| :---: | :--- | :--- | :--- |")
        val eggOrigins = mapOf(
            101 to "Digital Monster Ver. 1",
            103 to "Digital Monster Ver. 2",
            105 to "Digital Monster Ver. 3",
            107 to "Digital Monster Ver. 4",
            109 to "Digital Monster Ver. 5",
            111 to "Digimon Pendulum 1.0 Nature Spirits",
            113 to "Armageddon Army / Virus Busters",
            130 to "Wind Guardians / Floramon Line",
            131 to "Deep Savers / Marine Line",
            132 to "Deep Savers II / Gomamon Line",
            1101 to "Pendulum Z / Deep Savers",
            1104 to "Pendulum Z / Nature Spirits",
            1106 to "Digimon Adventure: / Algomon Line",
            1108 to "Vital Bracelet / Pulsemon Line",
            1111 to "Digital Monster X / Metal Empire",
            1113 to "Digital Monster Color / Nature Spirits"
        )
        for (eggId in com.digiwear.core.engine.EvolutionEngine.STARTER_EGG_IDS) {
            val eggSp = PetSpecies.getById(eggId)
            val targetsDesc = eggSp.evolutionRules.joinToString("<br>") { r ->
                val nextSp = PetSpecies.getById(r.targetSpeciesId)
                "→ **`#${nextSp.id}` ${nextSp.name}**: `${com.digiwear.core.engine.EvolutionEngine.describeRule(r)}`"
            }
            sb.appendLine("| `#$eggId` | **${eggSp.name}** | $targetsDesc | ${eggOrigins[eggId] ?: "Classic V-Pet"} |")
        }

        for (stage in listOf(PetStage.BABY_II, PetStage.CHILD, PetStage.ADULT, PetStage.PERFECT, PetStage.ULTIMATE)) {
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
            sb.appendLine("## ${stage.displayName} Stage Digimon (${registry.count { it.stage == stage }} Species)")
            sb.appendLine()
            sb.appendLine("| ID | Digimon | Attr | HP / PWR | Starter Egg Lineage Path | Evolves Into (Target & Exact Requirements) |")
            sb.appendLine("| :---: | :--- | :---: | :---: | :--- | :--- |")
            for (sp in registry.filter { it.stage == stage }) {
                val lineage = com.digiwear.core.engine.EvolutionEngine.findEggLineagePath(sp.id)
                    .joinToString(" → ") { id -> PetSpecies.getById(id).name }
                val branches = if (sp.evolutionRules.isEmpty()) {
                    val fusions = JogressEngine.getCompatibleRecipesFor(sp.id)
                    if (fusions.isEmpty()) {
                        "_Apex Form_"
                    } else {
                        fusions.joinToString("<br>") { r ->
                            val otherId = if (r.primarySpeciesId == sp.id) r.secondarySpeciesId else r.primarySpeciesId
                            val other = PetSpecies.getById(otherId)
                            val target = PetSpecies.getById(r.targetSpeciesId)
                            "+#${other.id} ${other.name} → #${target.id} ${target.name} (${r.fusionType.displayName})"
                        }
                    }
                } else {
                    sp.evolutionRules.joinToString("<br>") { rule ->
                        val target = PetSpecies.getById(rule.targetSpeciesId)
                        "→ **`#${target.id}` ${target.name}** (${target.stage.displayName}): `${com.digiwear.core.engine.EvolutionEngine.describeRule(rule)}`"
                    }
                }
                sb.appendLine("| `#${sp.id}` | **${sp.name}** | ${sp.attribute.displayName} | `${sp.baseHp}HP / ${sp.basePower}P` | $lineage | $branches |")
            }
        }

        val totalJogress = JogressEngine.RECIPES.size
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("## DNA Digivolution, Mode Change & DigiXros Recipes (All $totalJogress Combinations)")
        sb.appendLine()
        sb.appendLine("Combine any two compatible Digimon via **Watch D-Com Battle Link**, **Phone Battle Command Center**, or **Phone DigiVault** to awaken these $totalJogress special forms:")
        sb.appendLine()
        sb.appendLine("| # | Primary Partner | Secondary Partner | Awakened Form | Fusion Type | Lore / Description |")
        sb.appendLine("| :---: | :--- | :--- | :--- | :---: | :--- |")
        JogressEngine.RECIPES.forEachIndexed { idx, recipe ->
            val spA = PetSpecies.getById(recipe.primarySpeciesId)
            val spB = PetSpecies.getById(recipe.secondarySpeciesId)
            val target = PetSpecies.getById(recipe.targetSpeciesId)
            sb.appendLine("| ${idx + 1} | `#${spA.id}` **${spA.name}** | `#${spB.id}` **${spB.name}** | `#${target.id}` **${target.name}** | `${recipe.fusionType.displayName}` | ${recipe.description} |")
        }

        val outFile = java.io.File("../DIGIVOLUTION_GUIDE.md")
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        assertTrue("DIGIVOLUTION_GUIDE.md must be written and non-empty", outFile.exists() && outFile.length() > 10_000L)
        assertTrue("Roster spritesheet PNG must exist", rosterSheetFile.exists() && rosterSheetFile.length() > 10_000L)
        assertTrue("Visual evolution guide chart PNG must exist", evoChartFile.exists() && evoChartFile.length() > 20_000L)
    }

    private fun drawSpeciesSprite(
        g: java.awt.Graphics2D,
        speciesId: Int,
        destX: Int,
        destY: Int,
        pixelScale: Int
    ) {
        val palette = com.digiwear.core.render.SpriteMatrixRegistry.getPalette(speciesId)
        val matrix = com.digiwear.core.render.SpriteMatrixRegistry.getMatrix(
            speciesId,
            com.digiwear.core.model.SpriteFrame.IDLE_A
        )
        for (r in 0 until 16) {
            val row = matrix[r]
            for (c in 0 until 16) {
                val ch = row[c]
                if (ch == '.') continue
                val argb = when (ch) {
                    '#' -> palette.primary
                    'S' -> palette.secondary
                    'T' -> palette.tertiary
                    'E' -> palette.eye
                    'K', 'B' -> (0xFF shl 24) or (18 shl 16) or (20 shl 8) or 26
                    'W' -> (0xFF shl 24) or (250 shl 16) or (250 shl 8) or 250
                    'R' -> (0xFF shl 24) or (244 shl 16) or (67 shl 8) or 54
                    'G' -> (0xFF shl 24) or (67 shl 16) or (185 shl 8) or 85
                    'C' -> (0xFF shl 24) or (0 shl 16) or (229 shl 8) or 255
                    else -> palette.primary
                }
                g.color = java.awt.Color(argb, true)
                g.fillRect(destX + c * pixelScale, destY + r * pixelScale, pixelScale, pixelScale)
            }
        }
    }

    private fun attributeAwtColor(attr: ElementalAttribute): java.awt.Color = when (attr) {
        ElementalAttribute.VACCINE -> java.awt.Color(0x00, 0xE5, 0xFF)
        ElementalAttribute.DATA -> java.awt.Color(0xFF, 0xD5, 0x4F)
        ElementalAttribute.VIRUS -> java.awt.Color(0xFF, 0x52, 0x52)
        ElementalAttribute.FREE -> java.awt.Color(0x69, 0xF0, 0xAE)
    }

    private fun renderMasterRosterSpriteSheetPng(
        registry: List<PetSpecies>,
        outFile: java.io.File
    ) {
        val cols = 10
        val rows = (registry.size + cols - 1) / cols
        val cellW = 148
        val cellH = 112
        val headerH = 72
        val width = cols * cellW + 24
        val height = headerH + rows * cellH + 20

        val img = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        g.color = java.awt.Color(0x0D, 0x11, 0x17)
        g.fillRect(0, 0, width, height)

        g.color = java.awt.Color(0x16, 0x1B, 0x24)
        g.fillRoundRect(10, 8, width - 20, 54, 12, 12)
        g.color = java.awt.Color(0x00, 0xE5, 0xFF)
        g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 20)
        g.drawString("DIGIWEAR v2.0 — COMPLETE 550-SPECIES FULL-COLOR PIXEL ART ROSTER", 24, 34)
        g.color = java.awt.Color(0xB0, 0xBE, 0xC5)
        g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12)
        g.drawString("16x16 Multi-Tone Bandai Color Palettes • Cyan = Vaccine • Gold = Data • Crimson = Virus • Emerald = Free", 24, 53)

        registry.forEachIndexed { idx, sp ->
            val col = idx % cols
            val row = idx / cols
            val x = 12 + col * cellW
            val y = headerH + row * cellH

            val attrCol = attributeAwtColor(sp.attribute)
            g.color = java.awt.Color(0x18, 0x1F, 0x2C)
            g.fillRoundRect(x + 3, y + 3, cellW - 6, cellH - 6, 10, 10)
            g.color = attrCol
            g.drawRoundRect(x + 3, y + 3, cellW - 6, cellH - 6, 10, 10)

            // LCD inner well for sprite
            val spriteBoxX = x + (cellW - 56) / 2
            val spriteBoxY = y + 8
            g.color = java.awt.Color(0x10, 0x15, 0x1E)
            g.fillRoundRect(spriteBoxX, spriteBoxY, 56, 56, 6, 6)
            drawSpeciesSprite(g, sp.id, spriteBoxX + 4, spriteBoxY + 4, 3)

            g.color = attrCol
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 10)
            g.drawString("#${sp.id} ${sp.stage.displayName.take(7).uppercase()}", x + 8, y + 76)

            g.color = java.awt.Color.WHITE
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 11)
            val shortName = if (sp.name.length > 15) sp.name.take(14) + "…" else sp.name
            g.drawString(shortName, x + 8, y + 90)

            g.color = java.awt.Color(0x90, 0xA4, 0xAE)
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 9)
            g.drawString("${sp.baseHp}HP / ${sp.basePower}PWR", x + 8, y + 102)
        }

        g.dispose()
        javax.imageio.ImageIO.write(img, "PNG", outFile)
    }

    private fun renderVisualEvolutionGuideChartPng(
        registry: List<PetSpecies>,
        outFile: java.io.File
    ) {
        val width = 2220
        val starterEggs = com.digiwear.core.engine.EvolutionEngine.STARTER_EGG_IDS
        val nonBabyISpecies = registry.filter { it.stage != PetStage.BABY_I }
        val cols = 3
        val colW = 724
        val cardH = 122
        val directoryRows = (nonBabyISpecies.size + cols - 1) / cols
        val jogressCols = 4
        val jogressColW = 540
        val jogressCardH = 74
        val jogressRows = (JogressEngine.RECIPES.size + jogressCols - 1) / jogressCols

        val section1Y = 92
        val section1H = starterEggs.size * 72 + 48
        val section2Y = section1Y + section1H + 16
        val section2H = directoryRows * cardH + 54
        val section3Y = section2Y + section2H + 16
        val section3H = jogressRows * jogressCardH + 64
        val totalHeight = section3Y + section3H + 24

        val img = java.awt.image.BufferedImage(width, totalHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Background
        g.color = java.awt.Color(0x0B, 0x0E, 0x14)
        g.fillRect(0, 0, width, totalHeight)

        // Top Title Header
        g.color = java.awt.Color(0x15, 0x1C, 0x28)
        g.fillRoundRect(16, 12, width - 32, 68, 14, 14)
        g.color = java.awt.Color(0x00, 0xE5, 0xFF)
        g.drawRoundRect(16, 12, width - 32, 68, 14, 14)
        g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 24)
        g.drawString("DIGIWEAR v2.0 — VISUAL SPRITE SHEET DIGIVOLUTION & DNA JOGRESS GUIDE (ALL 550 SPECIES)", 32, 42)
        g.color = java.awt.Color(0xCF, 0xD8, 0xDC)
        g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13)
        g.drawString("100% Reachable from 16 Starter Eggs • Key: Age = Min Time in Stage | CM = Care Mistakes | EFF = Effort (0-4) | W = Min Battles & Win% | G = Weight", 32, 66)

        // =========================================================================
        // SECTION 1: THE 16 STARTER DIGI-EGGS (BABY I ──▶ BABY II ──▶ CHILD ROOKIES)
        // =========================================================================
        g.color = java.awt.Color(0x1E, 0x29, 0x3B)
        g.fillRoundRect(16, section1Y, width - 32, 36, 10, 10)
        g.color = java.awt.Color(0xFF, 0xD5, 0x4F)
        g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 16)
        g.drawString("PART 1: THE 16 STARTER DIGI-EGG FAMILIES (BABY I ──10m──▶ BABY II ──6h──▶ CHILD ROOKIE BRANCHES)", 30, section1Y + 24)

        starterEggs.forEachIndexed { idx, eggId ->
            val rowY = section1Y + 44 + idx * 72
            val eggSp = PetSpecies.getById(eggId)
            val baby2Id = eggSp.evolutionRules.first().targetSpeciesId
            val baby2Sp = PetSpecies.getById(baby2Id)

            g.color = java.awt.Color(0x13, 0x18, 0x22)
            g.fillRoundRect(20, rowY, width - 40, 66, 10, 10)
            g.color = java.awt.Color(0x2D, 0x37, 0x48)
            g.drawRoundRect(20, rowY, width - 40, 66, 10, 10)

            // Draw Baby I Box
            drawSpeciesSprite(g, eggSp.id, 32, rowY + 9, 3)
            g.color = java.awt.Color(0xCE, 0x93, 0xD8)
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 11)
            g.drawString("EGG #${eggSp.id}", 86, rowY + 26)
            g.color = java.awt.Color.WHITE
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 13)
            g.drawString(eggSp.name, 86, rowY + 44)

            // Arrow to Baby II
            g.color = java.awt.Color(0x64, 0xB5, 0xF6)
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 12)
            g.drawString("──10m──▶", 185, rowY + 36)

            // Draw Baby II Box
            drawSpeciesSprite(g, baby2Sp.id, 255, rowY + 9, 3)
            g.color = java.awt.Color(0x80, 0xCB, 0xC4)
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 11)
            g.drawString("BABY II #${baby2Sp.id}", 308, rowY + 26)
            g.color = java.awt.Color.WHITE
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 13)
            g.drawString(baby2Sp.name, 308, rowY + 44)

            // Arrow to Child Branches
            g.color = java.awt.Color(0xFF, 0xD5, 0x4F)
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 12)
            g.drawString("──6h──▶", 422, rowY + 36)

            // Child branches
            baby2Sp.evolutionRules.forEachIndexed { bIdx, rule ->
                val childSp = PetSpecies.getById(rule.targetSpeciesId)
                val bx = 488 + bIdx * 282
                if (bx + 274 < width - 24) {
                    g.color = java.awt.Color(0x1B, 0x24, 0x32)
                    g.fillRoundRect(bx, rowY + 6, 274, 54, 8, 8)
                    g.color = attributeAwtColor(childSp.attribute)
                    g.drawRoundRect(bx, rowY + 6, 274, 54, 8, 8)
                    drawSpeciesSprite(g, childSp.id, bx + 6, rowY + 9, 3)

                    g.color = java.awt.Color.WHITE
                    g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 12)
                    g.drawString("#${childSp.id} ${childSp.name}", bx + 58, rowY + 25)

                    g.color = java.awt.Color(0xB0, 0xBE, 0xC5)
                    g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 10)
                    val cond = compactRuleSummary(rule)
                    g.drawString(cond, bx + 58, rowY + 44)
                }
            }
        }

        // =========================================================================
        // SECTION 2: COMPLETE STAGE-BY-STAGE VISUAL DIGIVOLUTION DIRECTORY
        // =========================================================================
        g.color = java.awt.Color(0x1E, 0x29, 0x3B)
        g.fillRoundRect(16, section2Y, width - 32, 36, 10, 10)
        g.color = java.awt.Color(0x00, 0xE5, 0xFF)
        g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 16)
        g.drawString("PART 2: COMPLETE SPECIES-BY-SPECIES VISUAL DIGIVOLUTION TREE (WITH TARGET SPRITES & EXACT CONDITIONS)", 30, section2Y + 24)

        nonBabyISpecies.forEachIndexed { idx, sp ->
            val col = idx % cols
            val row = idx / cols
            val cx = 20 + col * colW
            val cy = section2Y + 44 + row * cardH

            val attrCol = attributeAwtColor(sp.attribute)
            g.color = java.awt.Color(0x13, 0x18, 0x22)
            g.fillRoundRect(cx, cy, colW - 10, cardH - 6, 10, 10)
            g.color = attrCol
            g.drawRoundRect(cx, cy, colW - 10, cardH - 6, 10, 10)

            // Left panel: Source Digimon Sprite + Metadata
            g.color = java.awt.Color(0x0D, 0x11, 0x18)
            g.fillRoundRect(cx + 8, cy + 8, 54, 54, 6, 6)
            drawSpeciesSprite(g, sp.id, cx + 11, cy + 11, 3)

            g.color = attrCol
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 10)
            g.drawString("#${sp.id} [${sp.stage.displayName.uppercase()}]", cx + 68, cy + 20)

            g.color = java.awt.Color.WHITE
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 13)
            val dispName = if (sp.name.length > 18) sp.name.take(17) + "…" else sp.name
            g.drawString(dispName, cx + 68, cy + 36)

            g.color = java.awt.Color(0x90, 0xA4, 0xAE)
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 10)
            g.drawString("${sp.attribute.displayName} • ${sp.baseHp}HP/${sp.basePower}P", cx + 68, cy + 51)

            val lineageIds = com.digiwear.core.engine.EvolutionEngine.findEggLineagePath(sp.id)
            val eggOrigin = lineageIds.firstOrNull()?.let { PetSpecies.getById(it).name } ?: "Jogress"
            g.color = java.awt.Color(0xCE, 0x93, 0xD8)
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 9)
            g.drawString("Origin: $eggOrigin", cx + 8, cy + 78)

            val prevForms = com.digiwear.core.engine.EvolutionEngine.getPreEvolutions(sp.id)
            if (prevForms.isNotEmpty()) {
                val prevText = "From: " + prevForms.take(2).joinToString(",") { "#${it.first.id}" }
                g.color = java.awt.Color(0x80, 0xDE, 0xEA)
                g.drawString(prevText, cx + 8, cy + 92)
            }

            // Right panel: Evolution Target Sprites + Conditions
            val targetX = cx + 228
            if (sp.evolutionRules.isNotEmpty()) {
                val rulesToShow = sp.evolutionRules.take(6)
                rulesToShow.forEachIndexed { rIdx, rule ->
                    val targetSp = PetSpecies.getById(rule.targetSpeciesId)
                    val subCol = rIdx % 2
                    val subRow = rIdx / 2
                    val tx = targetX + subCol * 240
                    val ty = cy + 6 + subRow * 36

                    g.color = java.awt.Color(0x1B, 0x22, 0x30)
                    g.fillRoundRect(tx, ty, 234, 33, 6, 6)
                    drawSpeciesSprite(g, targetSp.id, tx + 3, ty + 1, 2)

                    g.color = java.awt.Color(0xFF, 0xD5, 0x4F)
                    g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 10)
                    val tName = if (targetSp.name.length > 15) targetSp.name.take(14) + "…" else targetSp.name
                    g.drawString("→#${targetSp.id} $tName", tx + 37, ty + 13)

                    g.color = java.awt.Color(0xCF, 0xD8, 0xDC)
                    g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 9)
                    g.drawString(compactRuleSummary(rule), tx + 37, ty + 26)
                }
            } else {
                val fusions = JogressEngine.getCompatibleRecipesFor(sp.id).take(3)
                if (fusions.isNotEmpty()) {
                    fusions.forEachIndexed { fIdx, recipe ->
                        val partnerId = if (recipe.primarySpeciesId == sp.id) recipe.secondarySpeciesId else recipe.primarySpeciesId
                        val partnerSp = PetSpecies.getById(partnerId)
                        val targetSp = PetSpecies.getById(recipe.targetSpeciesId)
                        val ty = cy + 8 + fIdx * 34
                        g.color = java.awt.Color(0x23, 0x1B, 0x32)
                        g.fillRoundRect(targetX, ty, 470, 31, 6, 6)
                        drawSpeciesSprite(g, partnerSp.id, targetX + 4, ty, 2)
                        drawSpeciesSprite(g, targetSp.id, targetX + 215, ty, 2)

                        g.color = java.awt.Color(0xEA, 0x80, 0xFC)
                        g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 10)
                        g.drawString("+ #${partnerSp.id} ${partnerSp.name.take(13)} ══▶", targetX + 38, ty + 19)
                        g.color = java.awt.Color(0xFF, 0xD5, 0x4F)
                        g.drawString("#${targetSp.id} ${targetSp.name.take(16)} (${recipe.fusionType.displayName.take(8)})", targetX + 250, ty + 19)
                    }
                } else {
                    g.color = java.awt.Color(0x69, 0xF0, 0xAE)
                    g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 11)
                    g.drawString("[ APEX ULTIMATE FORM (FINAL STAGE) ]", targetX + 12, cy + 56)
                }
            }
        }

        // =========================================================================
        // SECTION 3: ALL 65 DNA DIGIVOLUTION (JOGRESS), DIGIXROS & MODE CHANGE VISUAL RECIPES
        // =========================================================================
        g.color = java.awt.Color(0x1E, 0x29, 0x3B)
        g.fillRoundRect(16, section3Y, width - 32, 36, 10, 10)
        g.color = java.awt.Color(0xEA, 0x80, 0xFC)
        g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 16)
        g.drawString("PART 3: ALL 65 DNA DIGIVOLUTION (JOGRESS), DIGIXROS & MODE CHANGE VISUAL RECIPES", 30, section3Y + 24)

        JogressEngine.RECIPES.forEachIndexed { idx, recipe ->
            val col = idx % jogressCols
            val row = idx / jogressCols
            val jx = 20 + col * jogressColW
            val jy = section3Y + 44 + row * jogressCardH

            val spA = PetSpecies.getById(recipe.primarySpeciesId)
            val spB = PetSpecies.getById(recipe.secondarySpeciesId)
            val target = PetSpecies.getById(recipe.targetSpeciesId)

            g.color = java.awt.Color(0x16, 0x14, 0x26)
            g.fillRoundRect(jx, jy, jogressColW - 10, jogressCardH - 6, 8, 8)
            g.color = java.awt.Color(recipe.fusionType.badgeColorHex.toInt())
            g.drawRoundRect(jx, jy, jogressColW - 10, jogressCardH - 6, 8, 8)

            drawSpeciesSprite(g, spA.id, jx + 8, jy + 8, 2)
            g.color = java.awt.Color.WHITE
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 10)
            g.drawString("#${spA.id} ${spA.name.take(11)}", jx + 42, jy + 22)

            g.color = java.awt.Color(0xFF, 0xD5, 0x4F)
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 12)
            g.drawString("+", jx + 145, jy + 26)

            drawSpeciesSprite(g, spB.id, jx + 160, jy + 8, 2)
            g.color = java.awt.Color.WHITE
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 10)
            g.drawString("#${spB.id} ${spB.name.take(11)}", jx + 194, jy + 22)

            g.color = java.awt.Color(0x00, 0xE5, 0xFF)
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 11)
            g.drawString("══▶", jx + 298, jy + 25)

            drawSpeciesSprite(g, target.id, jx + 328, jy + 8, 2)
            g.color = java.awt.Color(0x69, 0xF0, 0xAE)
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 11)
            g.drawString("#${target.id} ${target.name.take(15)}", jx + 364, jy + 22)

            g.color = java.awt.Color(0xCE, 0x93, 0xD8)
            g.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 9)
            g.drawString("[${recipe.fusionType.displayName}] ${recipe.description.take(45)}", jx + 42, jy + 54)
        }

        g.dispose()
        javax.imageio.ImageIO.write(img, "PNG", outFile)
    }

    private fun compactRuleSummary(rule: com.digiwear.core.model.EvolutionRule): String {
        val parts = mutableListOf<String>()
        val hours = rule.minAgeSeconds / 3600L
        if (hours > 0) parts.add("${hours}h") else parts.add("${rule.minAgeSeconds / 60L}m")
        if (rule.minCareMistakes > 0 || rule.maxCareMistakes < 99) {
            val maxStr = if (rule.maxCareMistakes >= 99) "+" else "-${rule.maxCareMistakes}"
            parts.add("CM:${rule.minCareMistakes}$maxStr")
        }
        if (rule.minEffort > 0) parts.add("EFF≥${rule.minEffort}")
        if (rule.minBattles > 0) parts.add("B≥${rule.minBattles}")
        if (rule.minWinRatio > 0f) parts.add("W≥${(rule.minWinRatio * 100).toInt()}%")
        if (rule.minOverfeed > 0) parts.add("OF≥${rule.minOverfeed}")
        if (rule.minWeight > 0) parts.add("G≥${rule.minWeight}")
        return parts.joinToString(" ")
    }
}

