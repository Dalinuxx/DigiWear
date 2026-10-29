package com.digiwear.wear.tile

import com.digiwear.core.model.SoundpackProfile
import com.digiwear.wear.receiver.NotificationQuickCareReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite validating TileQuickCareActivity constants,
 * action routing parity with NotificationQuickCareReceiver, and SoundpackProfile integration.
 */
class TileQuickCareActivityTest {

    @Test
    fun testTileQuickCareActionConstantsIntegrity() {
        assertEquals(
            "com.digiwear.wear.extra.CARE_ACTION",
            TileQuickCareActivity.EXTRA_CARE_ACTION
        )
        assertEquals(
            "com.digiwear.wear.action.TILE_FEED",
            TileQuickCareActivity.ACTION_FEED
        )
        assertEquals(
            "com.digiwear.wear.action.TILE_CLEAN",
            TileQuickCareActivity.ACTION_CLEAN
        )
        assertEquals(
            "com.digiwear.wear.action.TILE_LIGHTS",
            TileQuickCareActivity.ACTION_LIGHTS
        )
        assertEquals(
            "com.digiwear.wear.action.TILE_MEDS",
            TileQuickCareActivity.ACTION_MEDS
        )
    }

    @Test
    fun testActionsAreDistinct() {
        val actions = setOf(
            TileQuickCareActivity.ACTION_FEED,
            TileQuickCareActivity.ACTION_CLEAN,
            TileQuickCareActivity.ACTION_LIGHTS,
            TileQuickCareActivity.ACTION_MEDS
        )
        assertEquals("Each Tile quick care action must have a unique intent action string", 4, actions.size)

        for (action in actions) {
            assertTrue("Action must start with package prefix", action.startsWith("com.digiwear.wear.action."))
        }
    }

    @Test
    fun testNotificationAndTileActionsDoNotCollide() {
        assertNotEquals(
            TileQuickCareActivity.ACTION_FEED,
            NotificationQuickCareReceiver.ACTION_QUICK_FEED
        )
        assertNotEquals(
            TileQuickCareActivity.ACTION_CLEAN,
            NotificationQuickCareReceiver.ACTION_QUICK_CLEAN
        )
        assertNotEquals(
            TileQuickCareActivity.ACTION_LIGHTS,
            NotificationQuickCareReceiver.ACTION_QUICK_LIGHTS
        )
        assertNotEquals(
            TileQuickCareActivity.ACTION_MEDS,
            NotificationQuickCareReceiver.ACTION_QUICK_MEDS
        )
    }

    @Test
    fun testSoundpackProfileCoverage() {
        val profiles = SoundpackProfile.entries
        assertEquals("Must support 3 soundpack profiles", 3, profiles.size)
        assertTrue(profiles.contains(SoundpackProfile.ORIGINAL_1997))
        assertTrue(profiles.contains(SoundpackProfile.DIGIVICE_1999))
        assertTrue(profiles.contains(SoundpackProfile.CHIPTUNE_RETRO))

        assertEquals(SoundpackProfile.ORIGINAL_1997, SoundpackProfile.fromId("ORIGINAL_1997"))
        assertEquals(SoundpackProfile.DIGIVICE_1999, SoundpackProfile.fromId("DIGIVICE_1999"))
        assertEquals(SoundpackProfile.CHIPTUNE_RETRO, SoundpackProfile.fromId("CHIPTUNE_RETRO"))
        assertEquals("Unknown profile must fallback to ORIGINAL_1997", SoundpackProfile.ORIGINAL_1997, SoundpackProfile.fromId("UNKNOWN_PROFILE"))
        assertEquals("Null profile must fallback to ORIGINAL_1997", SoundpackProfile.ORIGINAL_1997, SoundpackProfile.fromId(null))
    }

    @Test
    fun testNotificationDismissalEvaluation() {
        val distressedState = com.digiwear.core.model.PetState(
            hunger = 0,
            wasteCount = 4,
            attentionCallActive = true
        )
        assertFalse(
            "Distressed and hungry pet notification must NOT be dismissed",
            NotificationQuickCareReceiver.shouldDismissNotification(distressedState)
        )

        val happyState = com.digiwear.core.model.PetState(
            hunger = 4,
            wasteCount = 0,
            attentionCallActive = false
        )
        assertTrue(
            "Care-resolved pet notification MUST be dismissed",
            NotificationQuickCareReceiver.shouldDismissNotification(happyState)
        )
    }
}
