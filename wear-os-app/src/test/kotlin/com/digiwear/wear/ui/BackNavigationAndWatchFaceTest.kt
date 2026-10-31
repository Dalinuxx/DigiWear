package com.digiwear.wear.ui

import com.digiwear.wear.WearScreen
import com.digiwear.wear.input.VPetButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. Back navigation stack behavior (subscreen pop order).
 * 2. VPetButton.C trigger when back gesture occurs on the MAIN screen.
 * 3. Horizontal back-swipe gesture detection logic.
 * 4. Watch face adventure telemetry cycling & formatting.
 */
class BackNavigationAndWatchFaceTest {

    @Test
    fun testNavigationBackStackOrder() {
        val stack = mutableListOf<WearScreen>()
        var currentScreen = WearScreen.MAIN

        fun navigateTo(screen: WearScreen) {
            if (currentScreen != screen) {
                stack.add(currentScreen)
                currentScreen = screen
            }
        }

        fun handleBack(): Pair<WearScreen, Boolean> {
            return if (stack.isNotEmpty()) {
                val prev = stack.removeAt(stack.size - 1)
                currentScreen = prev
                Pair(currentScreen, false) // Did not trigger C
            } else if (currentScreen != WearScreen.MAIN) {
                currentScreen = WearScreen.MAIN
                Pair(currentScreen, false)
            } else {
                Pair(currentScreen, true) // Triggered Button C!
            }
        }

        // Navigate MAIN -> STATUS -> DIGIDEX
        navigateTo(WearScreen.STATUS)
        assertEquals(WearScreen.STATUS, currentScreen)
        assertEquals(listOf(WearScreen.MAIN), stack)

        navigateTo(WearScreen.DIGIDEX)
        assertEquals(WearScreen.DIGIDEX, currentScreen)
        assertEquals(listOf(WearScreen.MAIN, WearScreen.STATUS), stack)

        // 1st back swipe: returns from DIGIDEX to STATUS
        val res1 = handleBack()
        assertEquals(WearScreen.STATUS, res1.first)
        assertFalse("Should not trigger C on subscreen pop", res1.second)

        // 2nd back swipe: returns from STATUS to MAIN
        val res2 = handleBack()
        assertEquals(WearScreen.MAIN, res2.first)
        assertFalse("Should not trigger C on subscreen pop", res2.second)

        // 3rd back swipe: on MAIN screen, triggers Button C!
        val res3 = handleBack()
        assertEquals(WearScreen.MAIN, res3.first)
        assertTrue("Back swipe on MAIN screen MUST act like the C button", res3.second)
    }

    @Test
    fun testBackSwipeGestureDetectionFormula() {
        fun isBackSwipe(
            dx: Float,
            dy: Float,
            durationMs: Long,
            startX: Float,
            screenWidth: Float,
            isVerticalDragging: Boolean
        ): Boolean {
            return !isVerticalDragging &&
                    dx > 65f &&
                    dy < dx * 0.75f &&
                    durationMs in 35..750 &&
                    startX < (screenWidth * 0.75f)
        }

        val screenW = 450f

        // Case 1: Standard rightward back swipe from left bezel
        assertTrue(isBackSwipe(dx = 120f, dy = 15f, durationMs = 250L, startX = 20f, screenWidth = screenW, isVerticalDragging = false))

        // Case 2: Vertical scroll (should NOT trigger back)
        assertFalse(isBackSwipe(dx = 80f, dy = 120f, durationMs = 200L, startX = 100f, screenWidth = screenW, isVerticalDragging = false))

        // Case 3: Flagged as vertical dragging
        assertFalse(isBackSwipe(dx = 150f, dy = 20f, durationMs = 300L, startX = 50f, screenWidth = screenW, isVerticalDragging = true))

        // Case 4: Leftward swipe (dx is negative, should NOT trigger back)
        assertFalse(isBackSwipe(dx = -100f, dy = 10f, durationMs = 200L, startX = 300f, screenWidth = screenW, isVerticalDragging = false))

        // Case 5: Tap in place (dx < 65f)
        assertFalse(isBackSwipe(dx = 5f, dy = 2f, durationMs = 50L, startX = 100f, screenWidth = screenW, isVerticalDragging = false))
    }

    @Test
    fun testWatchFaceAdventureTelemetryFormatting() {
        fun formatTelemetry(
            isSleeping: Boolean,
            isBossReady: Boolean,
            adventureOverlayEnabled: Boolean,
            advNodeTitle: String,
            advNodeIndex: Int,
            currentNodeSteps: Int,
            targetNodeSteps: Int,
            dailySteps: Int,
            dailyStepGoal: Int,
            cycleAdv: Boolean
        ): String {
            return when {
                isSleeping -> {
                    if (adventureOverlayEnabled && targetNodeSteps > 0) {
                        "ZZZ • 🗺️ N${advNodeIndex + 1} (${currentNodeSteps}/${targetNodeSteps})"
                    } else {
                        "ZZZ • 👟 ${dailySteps} STEPS"
                    }
                }
                isBossReady && adventureOverlayEnabled -> {
                    "⚔️ BOSS READY! (${currentNodeSteps}/${targetNodeSteps})"
                }
                adventureOverlayEnabled && cycleAdv -> {
                    val areaLabel = if (advNodeTitle.isNotBlank()) advNodeTitle.take(10).uppercase() else "AREA ${advNodeIndex + 1}"
                    "🗺️ $areaLabel • ${currentNodeSteps}/${targetNodeSteps}"
                }
                else -> {
                    "👟 ${dailySteps} / ${dailyStepGoal} STEPS"
                }
            }
        }

        // 1. Walking during adventure cycle A (shows Adventure progress)
        val textAdv = formatTelemetry(
            isSleeping = false,
            isBossReady = false,
            adventureOverlayEnabled = true,
            advNodeTitle = "Native Forest",
            advNodeIndex = 0,
            currentNodeSteps = 150,
            targetNodeSteps = 500,
            dailySteps = 3200,
            dailyStepGoal = 5000,
            cycleAdv = true
        )
        assertEquals("🗺️ NATIVE FOR • 150/500", textAdv)

        // 2. Walking during adventure cycle B (shows Daily pedometer steps)
        val textDaily = formatTelemetry(
            isSleeping = false,
            isBossReady = false,
            adventureOverlayEnabled = true,
            advNodeTitle = "Native Forest",
            advNodeIndex = 0,
            currentNodeSteps = 150,
            targetNodeSteps = 500,
            dailySteps = 3200,
            dailyStepGoal = 5000,
            cycleAdv = false
        )
        assertEquals("👟 3200 / 5000 STEPS", textDaily)

        // 3. Boss Ready
        val textBoss = formatTelemetry(
            isSleeping = false,
            isBossReady = true,
            adventureOverlayEnabled = true,
            advNodeTitle = "Native Forest",
            advNodeIndex = 0,
            currentNodeSteps = 500,
            targetNodeSteps = 500,
            dailySteps = 3550,
            dailyStepGoal = 5000,
            cycleAdv = true
        )
        assertEquals("⚔️ BOSS READY! (500/500)", textBoss)

        // 4. Sleeping
        val textSleep = formatTelemetry(
            isSleeping = true,
            isBossReady = false,
            adventureOverlayEnabled = true,
            advNodeTitle = "Native Forest",
            advNodeIndex = 1,
            currentNodeSteps = 220,
            targetNodeSteps = 600,
            dailySteps = 4100,
            dailyStepGoal = 5000,
            cycleAdv = false
        )
        assertEquals("ZZZ • 🗺️ N2 (220/600)", textSleep)
    }

    @Test
    fun testModalDismissalPriorityOverScreenPop() {
        var selectedModalId: Int? = 42
        var screenPopped = false

        fun onButtonC() {
            if (selectedModalId != null) {
                selectedModalId = null // Closes modal without popping screen
            } else {
                screenPopped = true
            }
        }

        // 1st press/swipe with modal open: modal dismissed, screen remains intact
        onButtonC()
        assertEquals("Modal must be dismissed first", null, selectedModalId)
        assertFalse("Screen must NOT be popped while dismissing an active modal", screenPopped)

        // 2nd press/swipe with modal closed: triggers screen pop
        onButtonC()
        assertTrue("Screen must pop when no modal is open", screenPopped)
    }

    @Test
    fun testWatchFacePartnerSleepStateConsistency() {
        fun computePartnerState(
            isSleeping: Boolean,
            lightsOff: Boolean,
            isInjured: Boolean,
            isBouncing: Boolean
        ): Pair<Boolean, Boolean> {
            val isPartnerAsleep = isSleeping || lightsOff
            val isWalkingActive = !isPartnerAsleep && !isInjured && !isBouncing
            return Pair(isPartnerAsleep, isWalkingActive)
        }

        // 1. Natural sleep: asleep, not walking
        val (asleep1, walking1) = computePartnerState(isSleeping = true, lightsOff = false, isInjured = false, isBouncing = false)
        assertTrue(asleep1)
        assertFalse(walking1)

        // 2. Lights toggled OFF (V-Pet artificial sleep): must also be considered asleep and NOT walking!
        val (asleep2, walking2) = computePartnerState(isSleeping = false, lightsOff = true, isInjured = false, isBouncing = false)
        assertTrue(asleep2)
        assertFalse(walking2)

        // 3. Healthy, awake, lights ON: walking active!
        val (asleep3, walking3) = computePartnerState(isSleeping = false, lightsOff = false, isInjured = false, isBouncing = false)
        assertFalse(asleep3)
        assertTrue(walking3)

        // 4. Injured: not asleep, not walking
        val (asleep4, walking4) = computePartnerState(isSleeping = false, lightsOff = false, isInjured = true, isBouncing = false)
        assertFalse(asleep4)
        assertFalse(walking4)
    }

    @Test
    fun testAmbientAdventureTelemetryFormat() {
        fun formatAmbientBottom(
            adventureOverlayEnabled: Boolean,
            advTargetNodeSteps: Int,
            advNodeIndex: Int,
            currentSteps: Int,
            batteryPct: Int
        ): String {
            return if (adventureOverlayEnabled && advTargetNodeSteps > 0) {
                "${currentSteps}st • ADV N${advNodeIndex + 1} • ${batteryPct}%"
            } else {
                "${currentSteps}st • ${batteryPct}%"
            }
        }

        val ambientAdv = formatAmbientBottom(
            adventureOverlayEnabled = true,
            advTargetNodeSteps = 500,
            advNodeIndex = 2,
            currentSteps = 1250,
            batteryPct = 85
        )
        assertEquals("1250st • ADV N3 • 85%", ambientAdv)

        val ambientNoAdv = formatAmbientBottom(
            adventureOverlayEnabled = false,
            advTargetNodeSteps = 0,
            advNodeIndex = 0,
            currentSteps = 1250,
            batteryPct = 85
        )
        assertEquals("1250st • 85%", ambientNoAdv)
    }
}
