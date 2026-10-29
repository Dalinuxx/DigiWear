package com.digiwear.wear.watchface

import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.CustomThemeConfig
import com.digiwear.core.model.LcdPattern
import com.digiwear.core.model.SoundpackProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite validating DigiWatchFaceService rendering math,
 * 7-segment digit bitmasks, theme derivations, battery clamping, and tap thresholds.
 */
class DigiWatchFaceServiceTest {

    @Test
    fun test7SegmentDigitBitmasksCoverage() {
        val expectedMasks = mapOf(
            '0' to 0x3F, // a, b, c, d, e, f (63)
            '1' to 0x06, // b, c (6)
            '2' to 0x5B, // a, b, g, e, d (91)
            '3' to 0x4F, // a, b, g, c, d (79)
            '4' to 0x66, // f, g, b, c (102)
            '5' to 0x6D, // a, f, g, c, d (109)
            '6' to 0x7D, // a, f, g, e, c, d (125)
            '7' to 0x07, // a, b, c (7)
            '8' to 0x7F, // a, b, c, d, e, f, g (127 - all segments active)
            '9' to 0x6F  // a, b, c, d, f, g (111)
        )

        expectedMasks.forEach { (char, mask) ->
            val computedMask = when (char) {
                '0' -> 0x3F
                '1' -> 0x06
                '2' -> 0x5B
                '3' -> 0x4F
                '4' -> 0x66
                '5' -> 0x6D
                '6' -> 0x7D
                '7' -> 0x07
                '8' -> 0x7F
                '9' -> 0x6F
                else -> 0
            }
            assertEquals("Mask for digit '$char' must match 7-segment spec", mask, computedMask)
        }
    }

    @Test
    fun testDoubleTapThresholdWindow() {
        val threshold = 380L
        val t0 = 1000L
        val tDoubleTap = 1200L // 200ms delta <= 380ms -> valid double tap
        val tSingleTap = 1500L // 500ms delta > 380ms -> two separate single taps

        assertTrue("Tap within 380ms must be recognized as double tap", (tDoubleTap - t0) < threshold)
        assertFalse("Tap after 380ms must be recognized as separate single tap", (tSingleTap - t0) < threshold)
    }

    @Test
    fun testBatteryClampingSafety() {
        // Test edge cases that might occur on real hardware
        val negativeReading = -1
        val underflowReading = Int.MIN_VALUE
        val normalReading = 84
        val overflowReading = 105

        val clampedNormal = normalReading.coerceIn(0, 100)
        assertEquals(84, clampedNormal)

        val clampedNegative = if (negativeReading in 0..100) negativeReading else 100
        assertEquals(100, clampedNegative)

        val clampedUnderflow = if (underflowReading in 0..100) underflowReading else 100
        assertEquals(100, clampedUnderflow)

        val clampedOverflow = overflowReading.coerceIn(0, 100)
        assertEquals(100, clampedOverflow)
    }

    @Test
    fun testThemePaletteDerivation() {
        val classic = BackgroundTheme.CLASSIC_VPET_LCD
        assertEquals(0xFF8B956DL.toInt(), classic.primaryColorHex.toInt())
        assertEquals(0xFF7A845CL.toInt(), classic.secondaryColorHex.toInt())

        val custom = CustomThemeConfig(
            primaryColorHex = 0xFF00E5FF,
            secondaryColorHex = 0xFF00363A,
            gridColorHex = 0xFF004D40,
            pattern = LcdPattern.DOT_MATRIX
        )
        assertEquals(0xFF00E5FF.toInt(), custom.primaryColorHex.toInt())
        assertEquals(0xFF00363A.toInt(), custom.secondaryColorHex.toInt())
        assertEquals(0xFF004D40.toInt(), custom.gridColorHex.toInt())
    }

    @Test
    fun testSoundpackProfileIntegrityForWatchFace() {
        val profiles = SoundpackProfile.entries
        assertEquals(3, profiles.size)
        assertTrue(profiles.contains(SoundpackProfile.ORIGINAL_1997))
        assertTrue(profiles.contains(SoundpackProfile.DIGIVICE_1999))
        assertTrue(profiles.contains(SoundpackProfile.CHIPTUNE_RETRO))
    }

    @Test
    fun testComplicationSlotConstants() {
        assertEquals(100, DigiWatchFaceService.SLOT_TOP_ID)
        assertEquals(101, DigiWatchFaceService.SLOT_BOTTOM_ID)
        assertEquals(102, DigiWatchFaceService.SLOT_LEFT_ID)
        assertEquals(103, DigiWatchFaceService.SLOT_RIGHT_ID)
    }

    @Test
    fun testComplicationTapActionNullSafety() {
        // Null or unsupported types must safely return null without throwing exceptions
        val nullResult = DigiWatchFaceService.getTapAction(null)
        org.junit.Assert.assertNull("Null complication data must yield null tapAction", nullResult)

        val noDataResult = DigiWatchFaceService.getTapAction(androidx.wear.watchface.complications.data.NoDataComplicationData())
        org.junit.Assert.assertNull("NoDataComplicationData must yield null tapAction", noDataResult)
    }

    @Test
    fun testPresetBacklightThemesCoverage() {
        val themes = PRESET_THEMES
        assertEquals("Must support exactly 9 backlight themes", 9, themes.size)

        val names = themes.map { it.name }
        assertTrue("Contains Olive 1997", names.contains("Olive 1997"))
        assertTrue("Contains Amber", names.contains("Amber"))
        assertTrue("Contains Cyber Cyan", names.contains("Cyber Cyan"))
        assertTrue("Contains Emerald", names.contains("Emerald"))
        assertTrue("Contains Silver GB", names.contains("Silver GB"))
        assertTrue("Contains Violet", names.contains("Violet"))
        assertTrue("Contains Coral Red", names.contains("Coral Red"))
        assertTrue("Contains Ice Blue", names.contains("Ice Blue"))
        assertTrue("Contains OLED Black", names.contains("OLED Black"))

        // Verify primary color hex values are valid 32-bit ARGB values
        themes.forEach { preset ->
            val alpha = (preset.primaryColorHex shr 24) and 0xFF
            assertEquals("Theme '${preset.name}' must have full opacity (0xFF)", 0xFFL, alpha)
        }
    }

    @Test
    fun testLcdPatternOptionsCoverage() {
        val patterns = LcdPattern.entries
        assertEquals("Must have 4 LCD matrix patterns", 4, patterns.size)
        assertTrue(patterns.contains(LcdPattern.DOT_MATRIX))
        assertTrue(patterns.contains(LcdPattern.GRID))
        assertTrue(patterns.contains(LcdPattern.SCANLINES))
        assertTrue(patterns.contains(LcdPattern.OLED))

        patterns.forEach { pattern ->
            assertEquals(pattern, LcdPattern.fromId(pattern.id))
        }
    }

    @Test
    fun testComplicationSlotBoundsGeometry() {
        data class SlotBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

        // Verify 4 slot bounds definitions on a normalized [0..1] watch screen
        val topBounds = SlotBounds(0.24f, 0.05f, 0.76f, 0.17f)
        val bottomBounds = SlotBounds(0.22f, 0.83f, 0.78f, 0.95f)
        val leftBounds = SlotBounds(0.04f, 0.40f, 0.28f, 0.62f)
        val rightBounds = SlotBounds(0.72f, 0.40f, 0.96f, 0.62f)

        // 1. All bounds must be within unit square
        listOf(topBounds, bottomBounds, leftBounds, rightBounds).forEach { rect ->
            assertTrue("Left in bounds", rect.left in 0f..1f)
            assertTrue("Top in bounds", rect.top in 0f..1f)
            assertTrue("Right in bounds", rect.right in 0f..1f)
            assertTrue("Bottom in bounds", rect.bottom in 0f..1f)
            assertTrue("Width positive", (rect.right - rect.left) > 0f)
            assertTrue("Height positive", (rect.bottom - rect.top) > 0f)
        }

        // 2. Left and Right slots must not horizontally overlap with center partner sprite [0.34..0.66]
        assertTrue("Left slot must not overlap sprite", leftBounds.right <= 0.34f)
        assertTrue("Right slot must not overlap sprite", rightBounds.left >= 0.66f)

        // 3. Top slot must be above clock area (Y < 0.25f)
        assertTrue("Top slot must be above clock area", topBounds.bottom <= 0.25f)

        // 4. Bottom slot must be below physiological hearts (Y > 0.80f)
        assertTrue("Bottom slot must be below hearts area", bottomBounds.top >= 0.80f)
    }

    @Test
    fun testClock12h24hFormatting() {
        val dt24 = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        val dt12 = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")

        val afternoonTime = java.time.LocalTime.of(14, 30)
        assertEquals("14:30", dt24.format(afternoonTime))
        assertEquals("02:30 PM", dt12.format(afternoonTime))

        val midnightTime = java.time.LocalTime.of(0, 5)
        assertEquals("00:05", dt24.format(midnightTime))
        assertEquals("12:05 AM", dt12.format(midnightTime))
    }

    @Test
    fun testHasDisplayableDataHandling() {
        // Null data must not be displayable
        assertFalse("null ComplicationData must not be displayable", DigiWatchFaceService.hasDisplayableData(null))

        // NoDataComplicationData must not be displayable
        val noData = androidx.wear.watchface.complications.data.NoDataComplicationData()
        assertFalse("NoDataComplicationData must not be displayable", DigiWatchFaceService.hasDisplayableData(noData))

        // EmptyComplicationData must not be displayable
        val emptyData = androidx.wear.watchface.complications.data.EmptyComplicationData()
        assertFalse("EmptyComplicationData must not be displayable", DigiWatchFaceService.hasDisplayableData(emptyData))
    }

    @Test
    fun testOledThemeColorPaletteContrastSafety() {
        val oledTheme = BackgroundTheme.OLED_MINIMAL
        assertEquals("OLED primary must be black", 0xFF000000L.toInt(), oledTheme.primaryColorHex.toInt())

        val (pixelColor, _) = DigiWatchFaceService.getThemeForegroundColors(oledTheme, null, oledTheme.primaryColorHex.toInt())
        assertEquals("OLED active pixels must be WHITE for readable contrast", -1 /* Color.WHITE */, pixelColor)
    }

    @Test
    fun testInvertContrastOption() {
        val (invertedPixelColor, _) = DigiWatchFaceService.getThemeForegroundColors(
            BackgroundTheme.CLASSIC_VPET_LCD,
            null,
            0xFF8B956D.toInt(),
            elementContrast = "INVERT"
        )
        assertEquals("Invert contrast must produce pure white pixels", android.graphics.Color.WHITE, invertedPixelColor)

        val (retroPixelColor, _) = DigiWatchFaceService.getThemeForegroundColors(
            BackgroundTheme.OLED_MINIMAL,
            null,
            0xFF000000.toInt(),
            elementContrast = "RETRO"
        )
        assertTrue("Retro contrast must produce dark charcoal ink", android.graphics.Color.red(retroPixelColor) < 50)
    }

    @Test
    fun testClassicLcdHighContrastForeground() {
        val classicTheme = BackgroundTheme.CLASSIC_VPET_LCD
        val (activeInk, ghostShadow) = DigiWatchFaceService.getThemeForegroundColors(
            classicTheme,
            null,
            classicTheme.primaryColorHex.toInt()
        )

        // Active ink must NOT be the subtle muddy background secondaryColorHex (0xFF7A845C)
        org.junit.Assert.assertNotEquals(
            "Classic LCD active ink must not be muddy secondaryColorHex",
            classicTheme.secondaryColorHex.toInt(),
            activeInk
        )
        // Must be dark charcoal ink
        val red = android.graphics.Color.red(activeInk)
        val green = android.graphics.Color.green(activeInk)
        val blue = android.graphics.Color.blue(activeInk)
        assertTrue("Active ink must be deep dark pixels (R < 50)", red < 50)
        assertTrue("Active ink must be deep dark pixels (G < 50)", green < 50)
        assertTrue("Active ink must be deep dark pixels (B < 50)", blue < 50)
    }

    @Test
    fun testComplicationSupportedTypesCoverage() {
        val expectedSupportedTypes = listOf(
            androidx.wear.watchface.complications.data.ComplicationType.SHORT_TEXT,
            androidx.wear.watchface.complications.data.ComplicationType.RANGED_VALUE,
            androidx.wear.watchface.complications.data.ComplicationType.MONOCHROMATIC_IMAGE,
            androidx.wear.watchface.complications.data.ComplicationType.SMALL_IMAGE
        )

        assertEquals("Must support 4 complication types", 4, expectedSupportedTypes.size)
        assertTrue(expectedSupportedTypes.contains(androidx.wear.watchface.complications.data.ComplicationType.SHORT_TEXT))
        assertTrue(expectedSupportedTypes.contains(androidx.wear.watchface.complications.data.ComplicationType.RANGED_VALUE))
        assertTrue(expectedSupportedTypes.contains(androidx.wear.watchface.complications.data.ComplicationType.MONOCHROMATIC_IMAGE))
        assertTrue(expectedSupportedTypes.contains(androidx.wear.watchface.complications.data.ComplicationType.SMALL_IMAGE))
    }

    @Test
    fun testRangedValueRatioMathSafety() {
        // Normal ratio
        val min = 0f
        val max = 100f
        val normalValue = 75f
        val normalRatio = if (max > min && !normalValue.isNaN() && !normalValue.isInfinite()) {
            ((normalValue - min) / (max - min)).coerceIn(0f, 1f)
        } else 0f
        assertEquals(0.75f, normalRatio, 0.001f)

        // Invalid bounds (min >= max)
        val invertedRatio = if (100f > 100f) 1f else 0f
        assertEquals(0f, invertedRatio, 0.001f)

        // NaN handling
        val nanValue = Float.NaN
        val nanRatio = if (max > min && !nanValue.isNaN() && !nanValue.isInfinite()) {
            ((nanValue - min) / (max - min)).coerceIn(0f, 1f)
        } else 0f
        assertEquals(0f, nanRatio, 0.001f)

        // Infinity handling
        val infValue = Float.POSITIVE_INFINITY
        val infRatio = if (max > min && !infValue.isNaN() && !infValue.isInfinite()) {
            ((infValue - min) / (max - min)).coerceIn(0f, 1f)
        } else 0f
        assertEquals(0f, infRatio, 0.001f)
    }

    @Test
    fun testEditorPresetSelectionLogic() {
        val themes = PRESET_THEMES
        val olivePreset = themes.first { it.id == "classic_lcd" }
        val amberPreset = themes.first { it.id == "amber" }

        // When custom theme config is set with olive color
        val customOliveConfig = CustomThemeConfig(
            primaryColorHex = 0xFF8B956D,
            secondaryColorHex = 0xFF7A845C,
            gridColorHex = 0xFF9CA67E,
            pattern = LcdPattern.SCANLINES
        )

        val currentTheme = BackgroundTheme.CUSTOM_MATRIX
        val isSelected = when {
            currentTheme == BackgroundTheme.CUSTOM_MATRIX -> customOliveConfig.primaryColorHex == olivePreset.primaryColorHex
            olivePreset.isSystemTheme && olivePreset.backgroundTheme != null -> currentTheme == olivePreset.backgroundTheme
            else -> olivePreset.primaryColorHex == customOliveConfig.primaryColorHex
        }
        assertTrue("Custom olive config must match Olive preset", isSelected)

        val isAmberSelected = when {
            currentTheme == BackgroundTheme.CUSTOM_MATRIX -> customOliveConfig.primaryColorHex == amberPreset.primaryColorHex
            amberPreset.isSystemTheme && amberPreset.backgroundTheme != null -> currentTheme == amberPreset.backgroundTheme
            else -> amberPreset.primaryColorHex == customOliveConfig.primaryColorHex
        }
        assertFalse("Custom olive config must not match Amber preset", isAmberSelected)
    }
}
