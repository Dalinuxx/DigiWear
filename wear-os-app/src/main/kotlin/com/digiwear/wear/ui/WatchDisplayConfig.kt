package com.digiwear.wear.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Adaptive configuration catering to Round and Square/Rectangular Wear OS & AOSP watch displays.
 * Guarantees zero content clipping on circular displays while maximizing screen real-estate
 * on square, rectangular, and high-DPI Full Android Watch form factors.
 */
data class WatchDisplayConfig(
    val isRound: Boolean,
    val isWearOsSystem: Boolean,
    val runtimePlatformLabel: String,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val contentPadding: PaddingValues,
    val mainSpriteSize: Dp,
    val battleSpriteSize: Dp,
    val headerFontSize: TextUnit,
    val buttonSize: Dp,
    val actionRowSpacing: Dp,
    // Right-side authentic Bandai button rail & compact LCD metrics
    val rightRailWidth: Dp,
    val shellButtonSize: Dp,
    val shellButtonSpacing: Dp,
    val lcdIconWidth: Dp,
    val lcdIconHeight: Dp,
    val lcdIconFontSize: TextUnit,
    val telemetryFontSize: TextUnit
) {
    companion object {
        @Composable
        fun current(): WatchDisplayConfig {
            val config = LocalConfiguration.current
            val context = androidx.compose.ui.platform.LocalContext.current
            val shapeOverride by remember(context) {
                com.digiwear.wear.data.PetRepository.getInstance(context).displayShapeOverrideFlow
            }.collectAsState()
            val isRound = when (shapeOverride) {
                "ROUND" -> true
                "SQUARE" -> false
                else -> config.isScreenRound
            }
            val width = config.screenWidthDp
            val height = config.screenHeightDp
            val isWearOs = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WATCH)
            val platformLabel = if (isWearOs) "WEAR OS" else "AOSP WATCH"
            val shapeSuffix = if (shapeOverride != "AUTO") " ($shapeOverride)" else ""

            return if (isRound) {
                // Round watches (Wear OS or Round AOSP Full Android Watches): apply circular insets
                val horizPad = when {
                    width >= 300 -> 14.dp
                    width >= 240 -> 10.dp
                    else -> 8.dp
                }
                val vertPad = when {
                    height >= 300 -> 20.dp
                    height >= 240 -> 18.dp
                    else -> 16.dp
                }
                val rightRail = when {
                    width >= 300 -> 40.dp
                    width >= 240 -> 34.dp
                    else -> 28.dp
                }
                val btnSize = when {
                    width >= 300 -> 34.dp
                    width >= 240 -> 30.dp
                    else -> 26.dp
                }
                val btnSpacing = when {
                    height >= 300 -> 8.dp
                    height >= 240 -> 6.dp
                    else -> 5.dp
                }
                val iconW = when {
                    width >= 300 -> 28.dp
                    width >= 240 -> 25.dp
                    else -> 22.dp
                }
                val iconH = when {
                    height >= 300 -> 22.dp
                    height >= 240 -> 20.dp
                    else -> 17.dp
                }
                val iconFont = when {
                    width >= 300 -> 12.sp
                    width >= 240 -> 11.sp
                    else -> 9.5.sp
                }
                val telemetryFont = when {
                    width >= 300 -> 9.sp
                    width >= 240 -> 8.sp
                    else -> 7.sp
                }
                val spriteSize = when {
                    width >= 300 -> 100.dp
                    width >= 240 -> 86.dp
                    else -> 74.dp
                }

                WatchDisplayConfig(
                    isRound = true,
                    isWearOsSystem = isWearOs,
                    runtimePlatformLabel = "$platformLabel • ROUND$shapeSuffix",
                    screenWidthDp = width,
                    screenHeightDp = height,
                    horizontalPadding = horizPad,
                    verticalPadding = 6.dp,
                    contentPadding = PaddingValues(horizontal = horizPad, vertical = vertPad),
                    mainSpriteSize = spriteSize,
                    battleSpriteSize = when {
                        width >= 300 -> 72.dp
                        width >= 250 -> 62.dp
                        else -> 56.dp
                    },
                    headerFontSize = if (width >= 300) 12.sp else 10.5.sp,
                    buttonSize = if (width >= 300) 44.dp else 40.dp,
                    actionRowSpacing = if (width >= 300) 4.dp else 2.dp,
                    rightRailWidth = rightRail,
                    shellButtonSize = btnSize,
                    shellButtonSpacing = btnSpacing,
                    lcdIconWidth = iconW,
                    lcdIconHeight = iconH,
                    lcdIconFontSize = iconFont,
                    telemetryFontSize = telemetryFont
                )
            } else {
                // Square & Rectangular watches (AOSP Full Android Watches, Oppo Watch, square form factors)
                val horizPad = if (width >= 300) 8.dp else 6.dp
                val vertPad = if (height >= 300) 10.dp else 8.dp
                val rightRail = when {
                    width >= 300 -> 46.dp
                    width >= 240 -> 40.dp
                    else -> 36.dp
                }
                val btnSize = when {
                    width >= 300 -> 40.dp
                    width >= 240 -> 36.dp
                    else -> 32.dp
                }
                val btnSpacing = when {
                    height >= 300 -> 12.dp
                    height >= 240 -> 10.dp
                    else -> 8.dp
                }
                val iconW = when {
                    width >= 300 -> 34.dp
                    width >= 240 -> 30.dp
                    else -> 26.dp
                }
                val iconH = when {
                    height >= 300 -> 26.dp
                    height >= 240 -> 24.dp
                    else -> 21.dp
                }
                val iconFont = when {
                    width >= 300 -> 14.sp
                    width >= 240 -> 12.sp
                    else -> 11.sp
                }
                val telemetryFont = when {
                    width >= 300 -> 10.sp
                    width >= 240 -> 9.sp
                    else -> 8.sp
                }
                val spriteSize = when {
                    width >= 300 -> 116.dp
                    width >= 240 -> 100.dp
                    else -> 86.dp
                }

                WatchDisplayConfig(
                    isRound = false,
                    isWearOsSystem = isWearOs,
                    runtimePlatformLabel = "$platformLabel • RECT/SQ$shapeSuffix",
                    screenWidthDp = width,
                    screenHeightDp = height,
                    horizontalPadding = horizPad,
                    verticalPadding = 6.dp,
                    contentPadding = PaddingValues(horizontal = horizPad, vertical = vertPad),
                    mainSpriteSize = spriteSize,
                    battleSpriteSize = when {
                        width >= 300 -> 78.dp
                        width >= 260 -> 70.dp
                        else -> 64.dp
                    },
                    headerFontSize = if (width >= 300) 13.sp else 11.sp,
                    buttonSize = if (width >= 300) 48.dp else 44.dp,
                    actionRowSpacing = if (width >= 300) 6.dp else 4.dp,
                    rightRailWidth = rightRail,
                    shellButtonSize = btnSize,
                    shellButtonSpacing = btnSpacing,
                    lcdIconWidth = iconW,
                    lcdIconHeight = iconH,
                    lcdIconFontSize = iconFont,
                    telemetryFontSize = telemetryFont
                )
            }
        }
    }
}
