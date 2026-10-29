package com.digiwear.wear.receiver

import android.content.Context
import android.content.Intent
import com.digiwear.core.engine.PetSimulationEngine
import com.digiwear.core.model.PetState
import com.digiwear.wear.worker.PetDecayWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying NotificationQuickCareReceiver action routing,
 * unknown action safety, notification cancellation logic, and constant integrity.
 */
class NotificationQuickCareReceiverTest {

    @Test
    fun testActionRoutingForFeedAndClean() {
        assertEquals(
            "ACTION_QUICK_FEED must map to QuickCareAction.FEED",
            QuickCareAction.FEED,
            QuickCareAction.fromAction(NotificationQuickCareReceiver.ACTION_QUICK_FEED)
        )
        assertEquals(
            "ACTION_QUICK_CLEAN must map to QuickCareAction.CLEAN",
            QuickCareAction.CLEAN,
            QuickCareAction.fromAction(NotificationQuickCareReceiver.ACTION_QUICK_CLEAN)
        )
        assertEquals(
            "ACTION_QUICK_LIGHTS must map to QuickCareAction.LIGHTS",
            QuickCareAction.LIGHTS,
            QuickCareAction.fromAction(NotificationQuickCareReceiver.ACTION_QUICK_LIGHTS)
        )
        assertEquals(
            "ACTION_QUICK_MEDS must map to QuickCareAction.MEDS",
            QuickCareAction.MEDS,
            QuickCareAction.fromAction(NotificationQuickCareReceiver.ACTION_QUICK_MEDS)
        )
    }

    @Test
    fun testInvalidAndUnknownActionsAreSafelyIgnored() {
        assertEquals(
            "null action must map to QuickCareAction.UNKNOWN",
            QuickCareAction.UNKNOWN,
            QuickCareAction.fromAction(null)
        )
        assertEquals(
            "Empty string action must map to QuickCareAction.UNKNOWN",
            QuickCareAction.UNKNOWN,
            QuickCareAction.fromAction("")
        )
        assertEquals(
            "Bogus package action must map to QuickCareAction.UNKNOWN",
            QuickCareAction.UNKNOWN,
            QuickCareAction.fromAction("com.digiwear.wear.action.BOGUS_ACTION")
        )
        assertEquals(
            "Raw command 'FEED' without package prefix must not match receiver action",
            QuickCareAction.UNKNOWN,
            QuickCareAction.fromAction("FEED")
        )
        assertEquals(
            "Raw command 'CLEAN' without package prefix must not match receiver action",
            QuickCareAction.UNKNOWN,
            QuickCareAction.fromAction("CLEAN")
        )
    }

    @Test
    fun testOnReceiveSafeWithUnknownActionsWithoutExceptions() {
        val receiver = NotificationQuickCareReceiver()
        val dummyContext = DummyContext()
        val dummyIntent = Intent("com.digiwear.wear.action.UNKNOWN")

        // Must complete without throwing any uncaught exceptions
        receiver.onReceive(dummyContext, dummyIntent)
        receiver.onReceive(dummyContext, null)
    }

    @Test
    fun testNotificationDismissalLogicMatrix() {
        // 1. Healthy satisfied pet -> should dismiss
        val satisfiedState = PetState(
            hunger = 4,
            strength = 4,
            wasteCount = 0,
            isInjured = false,
            attentionCallActive = false
        )
        assertTrue(
            "Satisfied pet must dismiss notification",
            NotificationQuickCareReceiver.shouldDismissNotification(satisfiedState)
        )

        // 2. Starving pet (hunger == 0) -> should NOT dismiss
        val hungryState = satisfiedState.copy(hunger = 0)
        assertFalse(
            "Hungry pet must not dismiss notification",
            NotificationQuickCareReceiver.shouldDismissNotification(hungryState)
        )

        // 3. Dirty pet (wasteCount >= 4) -> should NOT dismiss
        val dirtyState = satisfiedState.copy(wasteCount = 4)
        assertFalse(
            "Dirty pet must not dismiss notification",
            NotificationQuickCareReceiver.shouldDismissNotification(dirtyState)
        )

        // 4. Injured pet -> should NOT dismiss
        val injuredState = satisfiedState.copy(isInjured = true)
        assertFalse(
            "Injured pet must not dismiss notification",
            NotificationQuickCareReceiver.shouldDismissNotification(injuredState)
        )

        // 5. Active attention call -> should NOT dismiss
        val attentionState = satisfiedState.copy(attentionCallActive = true)
        assertFalse(
            "Active attention call must not dismiss notification",
            NotificationQuickCareReceiver.shouldDismissNotification(attentionState)
        )
    }

    @Test
    fun testCareEngineStateTransformations() {
        val now = 1000000L
        val distressedState = PetState(
            hunger = 0,
            wasteCount = 4,
            attentionCallActive = true
        )

        // Feed meal clears hunger and resolves attention call
        val fedState = PetSimulationEngine.feedMeal(distressedState, now)
        assertEquals(1, fedState.hunger)
        assertFalse("Feeding meal must resolve attention call", fedState.attentionCallActive)

        // Clean waste resets waste count
        val cleanedState = PetSimulationEngine.cleanWaste(fedState)
        assertEquals(0, cleanedState.wasteCount)
        assertTrue(
            "Feeding and cleaning fully resolves distress",
            NotificationQuickCareReceiver.shouldDismissNotification(cleanedState)
        )
    }

    @Test
    fun testConstantsIntegrity() {
        assertEquals("com.digiwear.wear.action.QUICK_FEED", NotificationQuickCareReceiver.ACTION_QUICK_FEED)
        assertEquals("com.digiwear.wear.action.QUICK_CLEAN", NotificationQuickCareReceiver.ACTION_QUICK_CLEAN)
        assertEquals("com.digiwear.wear.action.QUICK_LIGHTS", NotificationQuickCareReceiver.ACTION_QUICK_LIGHTS)
        assertEquals("com.digiwear.wear.action.QUICK_MEDS", NotificationQuickCareReceiver.ACTION_QUICK_MEDS)
        assertEquals(1001, NotificationQuickCareReceiver.NOTIFICATION_ID_ATTENTION)
        assertEquals(PetDecayWorker.NOTIFICATION_ID_ATTENTION, NotificationQuickCareReceiver.NOTIFICATION_ID_ATTENTION)
    }

    private class DummyContext : android.content.ContextWrapper(null)
}
