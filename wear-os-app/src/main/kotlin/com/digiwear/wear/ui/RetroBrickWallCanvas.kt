package com.digiwear.wear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.CustomThemeConfig

/**
 * Palette colors resolved for the brick wall based on the active user-selected theme.
 */
data class BrickWallPalette(
    val brickBody: Color,
    val brickHighlight: Color,
    val brickShadow: Color,
    val mortarColor: Color,
    val crackColor: Color,
    val grainColor: Color
)

/**
 * Authentic Bandai Digital Monster Virtual Pet Destructible Brick Wall.
 * Renders a 5-course stone masonry fortification with 3D chiseled bevels,
 * mortar seams, stone grain texture, and progressive structural fracturing.
 * Dynamically resolves colors to harmonize with ANY selected background theme.
 */
@Composable
fun RetroBrickWallCanvas(
    hitsLanded: Int, // 0 to 5
    shakeOffset: Offset = Offset.Zero,
    isMegaHit: Boolean = false,
    theme: BackgroundTheme = BackgroundTheme.CLASSIC_VPET_LCD,
    customConfig: CustomThemeConfig? = null,
    modifier: Modifier = Modifier
) {
    val palette = remember(theme, customConfig) {
        resolveBrickWallPalette(theme, customConfig)
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val totalCourses = 5
        val courseH = (h * 0.82f) / totalCourses
        val mortarW = 1.75f

        val ox = shakeOffset.x
        val oy = shakeOffset.y

        // --- 1. RENDER BRICK COURSES (Top = 0 down to Base = 4) ---
        for (course in 0 until totalCourses) {
            val isDestroyed = when (course) {
                0 -> hitsLanded >= 2
                1 -> hitsLanded >= 3
                2 -> hitsLanded >= 4
                3 -> hitsLanded >= 5
                4 -> hitsLanded >= 5 && isMegaHit
                else -> false
            }

            val isCracked = when (course) {
                0 -> hitsLanded == 1
                1 -> hitsLanded == 2
                2 -> hitsLanded == 3
                3 -> hitsLanded == 4
                4 -> hitsLanded >= 4 && !isDestroyed
                else -> false
            }

            if (isDestroyed) continue

            val courseY = oy + course * courseH

            // Staggered masonry layout: Even courses have 2 full bricks, Odd have 3 (half, full, half)
            if (course % 2 == 0) {
                val brickW = (w - mortarW) / 2f
                drawChiseledBrick(ox, courseY, brickW, courseH - mortarW, palette)
                drawChiseledBrick(ox + brickW + mortarW, courseY, brickW, courseH - mortarW, palette)
            } else {
                val halfW = (w - mortarW * 2f) * 0.25f
                val fullW = (w - mortarW * 2f) * 0.50f
                drawChiseledBrick(ox, courseY, halfW, courseH - mortarW, palette)
                drawChiseledBrick(ox + halfW + mortarW, courseY, fullW, courseH - mortarW, palette)
                drawChiseledBrick(ox + halfW + fullW + mortarW * 2f, courseY, halfW, courseH - mortarW, palette)
            }

            // Draw jagged zigzag stress fractures across damaged bricks
            if (isCracked) {
                val crackPath = Path().apply {
                    val startX = ox + w * (if (course % 2 == 0) 0.42f else 0.58f)
                    moveTo(startX, courseY)
                    lineTo(startX - 4.5f, courseY + courseH * 0.28f)
                    lineTo(startX + 5.5f, courseY + courseH * 0.58f)
                    lineTo(startX - 2.5f, courseY + courseH * 0.85f)
                    lineTo(startX + 1f, courseY + courseH - mortarW)
                }
                drawPath(crackPath, palette.crackColor, style = Stroke(width = 2.0f))

                // Fine secondary stress hairline
                val subCrack = Path().apply {
                    val sx = ox + w * (if (course % 2 == 0) 0.65f else 0.30f)
                    moveTo(sx, courseY + 2f)
                    lineTo(sx + 3f, courseY + courseH * 0.45f)
                }
                drawPath(subCrack, palette.crackColor.copy(alpha = 0.75f), style = Stroke(width = 1.0f))
            }
        }

        // --- 2. RENDER RUBBLE MOUND AT BASE ---
        val baseGroundY = oy + h * 0.84f
        if (hitsLanded > 0) {
            val rubbleCount = (hitsLanded * 4).coerceAtMost(22)
            for (i in 0 until rubbleCount) {
                val rx = ox + ((i * 6.7f) % (w - 4f))
                val ry = baseGroundY + ((i * 3.1f) % (h * 0.13f))
                val rw = 3.5f + (i % 3) * 2.5f
                val rh = 2.5f + (i % 2) * 2.0f

                // Rubble stone shadow
                drawRoundRect(
                    color = palette.brickShadow,
                    topLeft = Offset(rx, ry),
                    size = Size(rw, rh),
                    cornerRadius = CornerRadius(0.75f, 0.75f)
                )
                // Rubble stone highlight
                drawRoundRect(
                    color = palette.brickHighlight,
                    topLeft = Offset(rx, ry),
                    size = Size(rw * 0.7f, rh * 0.6f),
                    cornerRadius = CornerRadius(0.75f, 0.75f)
                )
            }
        }

        // Ground stone foundation line
        drawLine(
            color = palette.mortarColor,
            start = Offset(ox - 3f, oy + h * 0.98f),
            end = Offset(ox + w + 3f, oy + h * 0.98f),
            strokeWidth = 2.2f
        )
    }
}

/**
 * Draws an authentic 3D chiseled stone brick with beveled highlights, shadows, and subtle grain.
 */
private fun DrawScope.drawChiseledBrick(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    palette: BrickWallPalette
) {
    if (w <= 0f || h <= 0f) return

    // Mortar underlay
    drawRect(
        color = palette.mortarColor,
        topLeft = Offset(x, y),
        size = Size(w, h)
    )

    // Brick core body (inset by 1px)
    val inset = 1f
    val bodyW = (w - inset * 2f).coerceAtLeast(1f)
    val bodyH = (h - inset * 2f).coerceAtLeast(1f)
    drawRect(
        color = palette.brickBody,
        topLeft = Offset(x + inset, y + inset),
        size = Size(bodyW, bodyH)
    )

    // Subtle stone grain line
    if (bodyW > 6f && bodyH > 4f) {
        drawLine(
            color = palette.grainColor,
            start = Offset(x + inset + 2f, y + h * 0.5f),
            end = Offset(x + w - inset - 2f, y + h * 0.5f),
            strokeWidth = 0.75f
        )
    }

    // Bevel highlights: Top & Left edges (Light source from top-left)
    drawLine(
        color = palette.brickHighlight,
        start = Offset(x + inset, y + inset),
        end = Offset(x + w - inset, y + inset),
        strokeWidth = 1.25f
    )
    drawLine(
        color = palette.brickHighlight,
        start = Offset(x + inset, y + inset),
        end = Offset(x + inset, y + h - inset),
        strokeWidth = 1.25f
    )

    // Bevel shadows: Bottom & Right edges
    drawLine(
        color = palette.brickShadow,
        start = Offset(x + inset, y + h - inset),
        end = Offset(x + w - inset, y + h - inset),
        strokeWidth = 1.25f
    )
    drawLine(
        color = palette.brickShadow,
        start = Offset(x + w - inset, y + inset),
        end = Offset(x + w - inset, y + h - inset),
        strokeWidth = 1.25f
    )
}

/**
 * Harmonizes stone brick colors to match ANY user-selected background theme or custom matrix.
 */
fun resolveBrickWallPalette(
    theme: BackgroundTheme,
    customConfig: CustomThemeConfig?
): BrickWallPalette {
    return when (theme) {
        BackgroundTheme.CLASSIC_VPET_LCD -> BrickWallPalette(
            brickBody = Color(0xFF1E2D1B),      // Deep LCD olive
            brickHighlight = Color(0xFF3B4D36), // Lighter matrix accent
            brickShadow = Color(0xFF0E170C),    // Dark LCD ink
            mortarColor = Color(0xFF6E7A52),    // Ghost dot matrix
            crackColor = Color(0xFF6E7A52),
            grainColor = Color(0xFF283824)
        )
        BackgroundTheme.OLED_MINIMAL -> BrickWallPalette(
            brickBody = Color(0xFF383838),      // Stone slate
            brickHighlight = Color(0xFFE0E0E0), // Pure silver highlight
            brickShadow = Color(0xFF121212),    // OLED black shadow
            mortarColor = Color(0xFF242424),
            crackColor = Color(0xFFFFFFFF),
            grainColor = Color(0xFF4A4A4A)
        )
        BackgroundTheme.CYBER_FOREST -> BrickWallPalette(
            brickBody = Color(0xFF183827),      // Deep forest jade
            brickHighlight = Color(0xFF00E676), // Neon green accent
            brickShadow = Color(0xFF0B1B12),
            mortarColor = Color(0xFF2A523A),
            crackColor = Color(0xFF00E676),
            grainColor = Color(0xFF204833)
        )
        BackgroundTheme.DIGITAL_OCEAN -> BrickWallPalette(
            brickBody = Color(0xFF1A2D44),      // Oceanic navy
            brickHighlight = Color(0xFF00E5FF), // Electric cyan
            brickShadow = Color(0xFF0A131F),
            mortarColor = Color(0xFF415A77),
            crackColor = Color(0xFF00E5FF),
            grainColor = Color(0xFF243D5C)
        )
        BackgroundTheme.NEON_CORE_CITY -> BrickWallPalette(
            brickBody = Color(0xFF351752),      // Synthwave violet
            brickHighlight = Color(0xFFFFD54F), // Gold neon
            brickShadow = Color(0xFF140822),
            mortarColor = Color(0xFF6C227E),
            crackColor = Color(0xFFFFD54F),
            grainColor = Color(0xFF4A2072)
        )
        BackgroundTheme.DATA_WASTELAND -> BrickWallPalette(
            brickBody = Color(0xFF823C24),      // Terracotta rust
            brickHighlight = Color(0xFFE07A5F), // Burnished copper
            brickShadow = Color(0xFF3D1A10),
            mortarColor = Color(0xFF5C3D28),
            crackColor = Color(0xFFE07A5F),
            grainColor = Color(0xFF9E482C)
        )
        BackgroundTheme.DARK_NETWORK -> BrickWallPalette(
            brickBody = Color(0xFF381218),      // Crimson obsidian
            brickHighlight = Color(0xFFFF1744), // Crimson plasma
            brickShadow = Color(0xFF140508),
            mortarColor = Color(0xFF521822),
            crackColor = Color(0xFFFF5252),
            grainColor = Color(0xFF4D1820)
        )
        BackgroundTheme.GLITCH_VOID -> BrickWallPalette(
            brickBody = Color(0xFF20122B),      // Dark void obsidian
            brickHighlight = Color(0xFFE040FB), // Electric neon magenta
            brickShadow = Color(0xFF0C0512),
            mortarColor = Color(0xFF381C4F),
            crackColor = Color(0xFF00E5FF),     // Glitch cyan fracture
            grainColor = Color(0xFF331647)
        )
        BackgroundTheme.CUSTOM_WALLPAPER -> BrickWallPalette(
            brickBody = Color(0xFF263238),      // Neutral slate stone
            brickHighlight = Color(0xFF80CBC4), // Teal highlight
            brickShadow = Color(0xFF10171A),
            mortarColor = Color(0xFF37474F),
            crackColor = Color(0xFFFFD54F),
            grainColor = Color(0xFF37474F)
        )
        BackgroundTheme.CUSTOM_MATRIX -> {
            val primary = Color(customConfig?.primaryColorHex ?: 0xFF8B956DL)
            val secondary = Color(customConfig?.secondaryColorHex ?: 0xFF7A845CL)
            val grid = Color(customConfig?.gridColorHex ?: 0xFF9CA67EL)
            BrickWallPalette(
                brickBody = secondary,
                brickHighlight = primary,
                brickShadow = secondary.copy(alpha = 0.5f),
                mortarColor = grid,
                crackColor = primary,
                grainColor = grid.copy(alpha = 0.7f)
            )
        }
    }
}
