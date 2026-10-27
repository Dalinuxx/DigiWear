package com.digiwear.core

import com.digiwear.core.engine.EvolutionEngine
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EvolutionEngineTest {

    @Test
    fun testAgumonToGreymonEvolution() {
        val readyForGreymon = PetState(
            speciesId = 201, // Agumon
            ageSeconds = 90000L, // >= 86400s (24h)
            careMistakes = 1,    // <= 3
            effort = 3,          // >= 3
            weight = 32          // >= 30
        )

        val target = EvolutionEngine.evaluateEvolution(readyForGreymon)
        assertNotNull(target)
        assertEquals(301, target!!.id) // Greymon
        assertEquals("Greymon", target.name)

        val evolved = EvolutionEngine.evolve(readyForGreymon, target, 100000L)
        assertEquals(301, evolved.speciesId)
        assertEquals(0, evolved.careMistakes) // Reset on form change
        assertEquals(0, evolved.effort)
    }

    @Test
    fun testAgumonToNumemonOnHighCareMistakes() {
        val neglectedAgumon = PetState(
            speciesId = 201, // Agumon
            ageSeconds = 90000L,
            careMistakes = 7 // >= 6
        )

        val target = EvolutionEngine.evaluateEvolution(neglectedAgumon)
        assertNotNull(target)
        assertEquals(305, target!!.id) // Numemon!
        assertEquals("Numemon", target.name)
    }

    @Test
    fun testNumemonToMonzaemonRedemption() {
        val dedicatedNumemon = PetState(
            speciesId = 305, // Numemon
            ageSeconds = 180000L, // >= 172800s (48h)
            totalBattles = 20,
            victories = 16, // win ratio = 0.80 >= 0.70
            effort = 4
        )

        val target = EvolutionEngine.evaluateEvolution(dedicatedNumemon)
        assertNotNull(target)
        assertEquals(402, target!!.id) // Monzaemon!
        assertEquals("Monzaemon", target.name)
    }

    @Test
    fun testTooYoungDoesNotEvolve() {
        val babyAgumon = PetState(
            speciesId = 201,
            ageSeconds = 1000L // Not yet 24 hours
        )
        assertNull(EvolutionEngine.evaluateEvolution(babyAgumon))
    }

    @Test
    fun testVitalPointsRemainStrictlyOptional() {
        val rule = com.digiwear.core.model.EvolutionRule(
            targetSpeciesId = 301,
            minAgeSeconds = 86400L,
            minVitalPoints = 2500, // Optional Vital Points condition
            maxCareMistakes = 3
        )
        // Pet with 0 vital points should still evolve normally when standard conditions are met
        val petWithoutVitals = PetState(
            speciesId = 201,
            ageSeconds = 86400L,
            careMistakes = 1,
            vitalPoints = 0
        )
        org.junit.Assert.assertTrue("0 vital points must not block evolution", rule.matches(petWithoutVitals))
    }

    @Test
    fun testVitalPointsFastTracksEvolutionAgeRequirement() {
        val rule = com.digiwear.core.model.EvolutionRule(
            targetSpeciesId = 301,
            minAgeSeconds = 86400L,
            minVitalPoints = 2000,
            maxCareMistakes = 3
        )
        // At half age (43200s), standard pet cannot evolve yet
        val standardPetAtHalfAge = PetState(
            speciesId = 201,
            ageSeconds = 43200L,
            careMistakes = 1,
            vitalPoints = 500
        )
        org.junit.Assert.assertFalse("Below threshold must not fast-track before full age", rule.matches(standardPetAtHalfAge))

        // But pet meeting optional vital points threshold fast-tracks at half age
        val highVitalPetAtHalfAge = PetState(
            speciesId = 201,
            ageSeconds = 43200L,
            careMistakes = 1,
            vitalPoints = 2500
        )
        org.junit.Assert.assertTrue("Achieving vital points fast-tracks evolution by halving required age", rule.matches(highVitalPetAtHalfAge))
    }
}
