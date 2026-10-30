package com.digiwear.wear.ui

import com.digiwear.core.engine.TrainingEngine
import com.digiwear.core.model.ElementalAttribute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingMinigameLogicTest {

    @Test
    fun testQteSweetSpotHits() {
        val mega = TrainingEngine.evaluateQtePunch(0.88f)
        assertTrue(mega.won)
        assertTrue(mega.isMegaHit)
        assertEquals(5, mega.hits)

        val good = TrainingEngine.evaluateQtePunch(0.55f)
        assertTrue(good.won)
        assertFalse(good.isMegaHit)
        assertEquals(3, good.hits)

        val miss = TrainingEngine.evaluateQtePunch(0.10f)
        assertFalse(miss.won)
        assertEquals(0, miss.hits)
    }

    @Test
    fun testDynamicQteSweetSpotHits() {
        val target = 0.35f
        val mega = TrainingEngine.evaluateQtePunch(0.38f, targetCenter = target)
        assertTrue(mega.won)
        assertTrue(mega.isMegaHit)
        assertEquals(5, mega.hits)

        val good = TrainingEngine.evaluateQtePunch(0.50f, targetCenter = target)
        assertTrue(good.won)
        assertFalse(good.isMegaHit)
        assertEquals(3, good.hits)

        val nearMiss = TrainingEngine.evaluateQtePunch(0.10f, targetCenter = target)
        assertFalse(nearMiss.won)
        assertEquals(1, nearMiss.hits)

        val farMiss = TrainingEngine.evaluateQtePunch(0.90f, targetCenter = target)
        assertFalse(farMiss.won)
        assertEquals(0, farMiss.hits)
    }

    @Test
    fun testPendulumShakeHits() {
        // Vaccine optimal is 5..7
        val vaccineMega = TrainingEngine.evaluateShakeTraining(ElementalAttribute.VACCINE, 6)
        assertTrue(vaccineMega.won)
        assertTrue(vaccineMega.isMegaHit)
        assertEquals(5, vaccineMega.hits)

        val vaccineGood = TrainingEngine.evaluateShakeTraining(ElementalAttribute.VACCINE, 8)
        assertTrue(vaccineGood.won)
        assertFalse(vaccineGood.isMegaHit)
        assertEquals(3, vaccineGood.hits)

        val vaccineMiss = TrainingEngine.evaluateShakeTraining(ElementalAttribute.VACCINE, 2)
        assertFalse(vaccineMiss.won)
        assertEquals(1, vaccineMiss.hits)
    }
}
