package com.digiwear.core

import com.digiwear.core.model.PetSpecies
import com.digiwear.core.render.SpriteMatrixRegistry
import com.digiwear.core.model.SpriteFrame
import org.junit.Test
import org.junit.Assert.assertTrue

class FullRosterCanonAuditTest {

    @Test
    fun auditAllSpeciesForGenericBoxPatterns() {
        val allSpecies = PetSpecies.SPECIES_REGISTRY.values.sortedBy { it.id }
        println("Auditing all ${allSpecies.size} species for generic box patterns...")

        val genericBoxSignatures = listOf(
            "B##WW######WW##B",
            "B##EE######EE##B",
            "B#TT#SSSSSS#TT#B",
            "B#SSSSSSSSSS#B",
            "B##########B",
            "B##WW####WW#B",
            "B##EE####EE#B",
            "B#SSSS#SSSS#B",
            "B###SSSSSSSS###B",
            "B#TTSSSSSSSSTT#B"
        )

        val flaggedSpecies = mutableListOf<String>()

        for (species in allSpecies) {
            val matrix = SpriteMatrixRegistry.getMatrix(species.id, SpriteFrame.IDLE_A)
            var matchesGenericBox = false
            var matchReason = ""

            for ((rIdx, row) in matrix.withIndex()) {
                for (sig in genericBoxSignatures) {
                    if (row.contains(sig)) {
                        matchesGenericBox = true
                        matchReason = "Row $rIdx matches signature '$sig'"
                        break
                    }
                }
                if (matchesGenericBox) break
            }

            // Also check symmetry: if > 11 rows out of 16 are perfectly symmetric (left-right mirror)
            var symmetricRows = 0
            for (row in matrix) {
                if (row == row.reversed()) symmetricRows++
            }
            if (symmetricRows >= 11) {
                matchesGenericBox = true
                matchReason = if (matchReason.isEmpty()) "$symmetricRows/16 rows perfectly symmetric" else "$matchReason & $symmetricRows symmetric rows"
            }

            if (matchesGenericBox) {
                flaggedSpecies.add("[ID ${species.id}] ${species.name} (${species.stage}): $matchReason")
            }
        }

        println("=== FLAGGED SPECIES (${flaggedSpecies.size}) ===")
        flaggedSpecies.forEach { println(it) }
        println("================================================")
    }

    @Test
    fun auditDuplicateAndPlaceholderSprites() {
        val allSpecies = PetSpecies.SPECIES_REGISTRY.values.sortedBy { it.id }
        println("Auditing 550 species for duplicate matrices and aliases...")

        val matrixToSpecies = mutableMapOf<String, MutableList<PetSpecies>>()
        for (sp in allSpecies) {
            val matrix = SpriteMatrixRegistry.getMatrix(sp.id, SpriteFrame.IDLE_A)
            val key = matrix.joinToString("\n")
            matrixToSpecies.getOrPut(key) { mutableListOf() }.add(sp)
        }

        val duplicates = matrixToSpecies.filter { it.value.size > 1 }
        println("=== IDENTICAL SPRITE MATRICES SHARED BETWEEN SPECIES (${duplicates.size} groups) ===")
        for ((_, speciesList) in duplicates) {
            val names = speciesList.joinToString(" <-> ") { "#${it.id} ${it.name} (${it.stage})" }
            println("DUPLICATE: $names")
        }
        println("==================================================================================")
    }

    @Test
    fun auditTokenDistributionAndColorInversion() {
        val allSpecies = PetSpecies.SPECIES_REGISTRY.values.sortedBy { it.id }
        println("Auditing 550 species for color inversions, dimension errors, and token distribution...")

        val invertedSpecies = mutableListOf<String>()
        val dimensionErrors = mutableListOf<String>()
        val zeroPrimarySpecies = mutableListOf<String>()

        for (species in allSpecies) {
            val matrix = SpriteMatrixRegistry.getMatrix(species.id, SpriteFrame.IDLE_A)
            
            // Check dimensions
            if (matrix.size != 16) {
                dimensionErrors.add("[ID ${species.id}] ${species.name}: Expected 16 rows, got ${matrix.size}")
            }
            for ((rIdx, row) in matrix.withIndex()) {
                if (row.length != 16) {
                    dimensionErrors.add("[ID ${species.id}] ${species.name} (row $rIdx): Expected 16 chars, got ${row.length} ('$row')")
                }
            }

            // Count tokens
            var countHash = 0
            var countT = 0
            var countS = 0
            var countW = 0
            var countB = 0
            var countDot = 0

            for (row in matrix) {
                for (ch in row) {
                    when (ch) {
                        '#' -> countHash++
                        'T' -> countT++
                        'S' -> countS++
                        'W' -> countW++
                        'B' -> countB++
                        '.' -> countDot++
                    }
                }
            }

            // Known canonical species where Tertiary or White is legitimately dominant
            // (e.g. giant mushroom caps, cherry blossom canopies, golden wings, silver helmets, energy auras)
            val canonicalDominantTertiaryIds = setOf(
                238, 239, 241, 252, 254, 256, 310, 320, 327, 337,
                347, 349, 380, 382, 383, 405, 406, 425, 443, 449,
                509, 522, 532, 539, 546, 553, 568, 578, 592, 1221, 1539
            )

            // Check if Tertiary is used as dominant body color while Primary is tiny (Tertiary Inversion Bug)
            if (species.id !in canonicalDominantTertiaryIds) {
                if (countT > 30 && countHash < countT / 2) {
                    invertedSpecies.add("[ID ${species.id}] ${species.name} (${species.stage}): T=$countT vs #=$countHash (Tertiary dominates Primary!)")
                } else if (countHash == 0 && countT > 10) {
                    zeroPrimarySpecies.add("[ID ${species.id}] ${species.name} (${species.stage}): # is 0, T=$countT")
                }
            }
        }

        println("=== UNEXPECTED TERTIARY INVERSION SUSPECTS (${invertedSpecies.size}) ===")
        invertedSpecies.forEach { println(it) }
        println("=== ZERO PRIMARY SUSPECTS (${zeroPrimarySpecies.size}) ===")
        zeroPrimarySpecies.forEach { println(it) }
        println("=== DIMENSION ERRORS (${dimensionErrors.size}) ===")
        dimensionErrors.forEach { println(it) }
        println("==========================================================")

        assertTrue("Found unexpected tertiary inverted species: $invertedSpecies", invertedSpecies.isEmpty())
        assertTrue("Found species with 0 primary pixels: $zeroPrimarySpecies", zeroPrimarySpecies.isEmpty())
        assertTrue("Found dimension errors: $dimensionErrors", dimensionErrors.isEmpty())
    }
}
