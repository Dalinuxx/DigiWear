package com.digiwear.core

import com.digiwear.core.engine.TrainingEngine
import com.digiwear.core.model.ElementalAttribute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingEngineTest {

    @Test
    fun testVaccineOptimalShakeRange() {
        val range = TrainingEngine.getOptimalShakeRange(ElementalAttribute.VACCINE)
        assertEquals(5..7, range)

        val mega = TrainingEngine.evaluateShakeTraining(ElementalAttribute.VACCINE, 6)
        assertTrue(mega.won)
        assertTrue(mega.isMegaHit)

        val goodLow = TrainingEngine.evaluateShakeTraining(ElementalAttribute.VACCINE, 4)
        assertTrue(goodLow.won)
        assertFalse(goodLow.isMegaHit)

        val missLow = TrainingEngine.evaluateShakeTraining(ElementalAttribute.VACCINE, 2)
        assertFalse(missLow.won)
    }

    @Test
    fun testVirusOptimalShakeRange() {
        val range = TrainingEngine.getOptimalShakeRange(ElementalAttribute.VIRUS)
        assertEquals(7..9, range)

        val mega = TrainingEngine.evaluateShakeTraining(ElementalAttribute.VIRUS, 8)
        assertTrue(mega.won)
        assertTrue(mega.isMegaHit)

        val missLow = TrainingEngine.evaluateShakeTraining(ElementalAttribute.VIRUS, 5)
        assertFalse(missLow.won)
    }

    @Test
    fun testDataOptimalShakeRange() {
        val range = TrainingEngine.getOptimalShakeRange(ElementalAttribute.DATA)
        assertEquals(6..8, range)

        val mega = TrainingEngine.evaluateShakeTraining(ElementalAttribute.DATA, 7)
        assertTrue(mega.won)
        assertTrue(mega.isMegaHit)
    }

    @Test
    fun testQtePunchEvaluation() {
        val mega = TrainingEngine.evaluateQtePunch(0.85f)
        assertTrue(mega.won)
        assertTrue(mega.isMegaHit)

        val good = TrainingEngine.evaluateQtePunch(0.50f)
        assertTrue(good.won)
        assertFalse(good.isMegaHit)

        val miss = TrainingEngine.evaluateQtePunch(0.20f)
        assertFalse(miss.won)
        assertFalse(miss.isMegaHit)
    }

    @Test
    fun testDynamicTargetCenterQtePunchEvaluation() {
        val target = 0.50f
        // Direct hit inside sweet spot (±0.09f)
        val mega = TrainingEngine.evaluateQtePunch(0.53f, targetCenter = target)
        assertTrue(mega.won)
        assertTrue(mega.isMegaHit)
        assertEquals(5, mega.hits)

        // Near hit inside good tolerance (±0.20f)
        val good = TrainingEngine.evaluateQtePunch(0.65f, targetCenter = target)
        assertTrue(good.won)
        assertFalse(good.isMegaHit)
        assertEquals(3, good.hits)

        // Far miss (>0.20f)
        val miss = TrainingEngine.evaluateQtePunch(0.85f, targetCenter = target)
        assertFalse(miss.won)
        assertFalse(miss.isMegaHit)
    }
}
