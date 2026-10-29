package com.digiwear.wear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Authentic Bandai Digital Monster Virtual Pet Brick Wall target.
 * Renders an authentic 5-course stone masonry wall that dynamically fractures,
 * chips, and collapses into rubble as training punches land.
 */
@Composable
fun RetroBrickWallCanvas(
    hitsLanded: Int, // 0 to 5
    shakeOffset: Offset = Offset.Zero,
    isMegaHit: Boolean = false,
    isLcdTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val totalCourses = 5
        val courseH = (h * 0.82f) / totalCourses
        val mortarW = 1.5f

        // Theme-tuned palette
        val brickColor = if (isLcdTheme) Color(0xFF1B2C1A) else Color(0xFFB04A2F)
        val brickHighlight = if (isLcdTheme) Color(0xFF2E462C) else Color(0xFFD46A4B)
        val brickShadow = if (isLcdTheme) Color(0xFF101C0F) else Color(0xFF7A2E1C)
        val mortarColor = if (isLcdTheme) Color(0xFF6B7750) else Color(0xFF263238)
        val crackColor = if (isLcdTheme) Color(0xFF6B7750) else Color(0xFF1A1A1A)

        // Wall origin with dynamic impact shake
        val ox = shakeOffset.x
        val oy = shakeOffset.y

        // --- 1. RENDER BRICK COURSES (From Top = 0 down to Base = 4) ---
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

            if (isDestroyed) {
                // If destroyed, don't draw intact brick; debris at base represents it
                continue
            }

            val courseY = oy + course * courseH

            // Staggered brick patterns: Even courses have 2 equal bricks, Odd have 3 (half, full, half)
            if (course % 2 == 0) {
                val brickW = (w - mortarW) / 2f
                drawBeveledBrick(ox, courseY, brickW, courseH - mortarW, brickColor, brickHighlight, brickShadow, mortarColor)
                drawBeveledBrick(ox + brickW + mortarW, courseY, brickW, courseH - mortarW, brickColor, brickHighlight, brickShadow, mortarColor)
            } else {
                val halfW = (w - mortarW * 2f) * 0.25f
                val fullW = (w - mortarW * 2f) * 0.50f
                drawBeveledBrick(ox, courseY, halfW, courseH - mortarW, brickColor, brickHighlight, brickShadow, mortarColor)
                drawBeveledBrick(ox + halfW + mortarW, courseY, fullW, courseH - mortarW, brickColor, brickHighlight, brickShadow, mortarColor)
                drawBeveledBrick(ox + halfW + fullW + mortarW * 2f, courseY, halfW, courseH - mortarW, brickColor, brickHighlight, brickShadow, mortarColor)
            }

            // Draw stress fractures and jagged cracks on partially damaged courses
            if (isCracked) {
                val crackPath = Path().apply {
                    val startX = ox + w * (if (course % 2 == 0) 0.45f else 0.55f)
                    moveTo(startX, courseY)
                    lineTo(startX - 4f, courseY + courseH * 0.35f)
                    lineTo(startX + 5f, courseY + courseH * 0.65f)
                    lineTo(startX - 2f, courseY + courseH - mortarW)
                }
                drawPath(crackPath, crackColor, style = Stroke(width = 1.75f))
            }
        }

        // --- 2. RENDER RUBBLE & DEBRIS MOUND AT BASE ---
        val baseGroundY = oy + h * 0.85f
        if (hitsLanded > 0) {
            val rubbleCount = (hitsLanded * 3).coerceAtMost(16)
            for (i in 0 until rubbleCount) {
                val rx = ox + (i * 7.3f) % w
                val ry = baseGroundY + ((i * 3.7f) % (h * 0.12f))
                val rw = 3.5f + (i % 3) * 2f
                val rh = 2.5f + (i % 2) * 1.5f
                drawRoundRect(
                    color = brickShadow,
                    topLeft = Offset(rx, ry),
                    size = Size(rw, rh),
                    cornerRadius = CornerRadius(0.5f, 0.5f)
                )
                drawRoundRect(
                    color = brickHighlight,
                    topLeft = Offset(rx, ry),
                    size = Size(rw * 0.7f, rh * 0.6f),
                    cornerRadius = CornerRadius(0.5f, 0.5f)
                )
            }
        }

        // Foundation ground baseline
        drawLine(
            color = mortarColor,
            start = Offset(ox - 4f, oy + h * 0.98f),
            end = Offset(ox + w + 4f, oy + h * 0.98f),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawBeveledBrick(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    bodyColor: Color,
    highlightColor: Color,
    shadowColor: Color,
    mortarColor: Color
) {
    if (w <= 0f || h <= 0f) return

    // Mortar border
    drawRect(
        color = mortarColor,
        topLeft = Offset(x, y),
        size = Size(w, h)
    )

    // Brick core body
    val inset = 1f
    drawRect(
        color = bodyColor,
        topLeft = Offset(x + inset, y + inset),
        size = Size((w - inset * 2f).coerceAtLeast(1f), (h - inset * 2f).coerceAtLeast(1f))
    )

    // Bevel highlights (Top & Left edges)
    drawLine(
        color = highlightColor,
        start = Offset(x + inset, y + inset),
        end = Offset(x + w - inset, y + inset),
        strokeWidth = 1f
    )
    drawLine(
        color = highlightColor,
        start = Offset(x + inset, y + inset),
        end = Offset(x + inset, y + h - inset),
        strokeWidth = 1f
    )

    // Bevel shadows (Bottom & Right edges)
    drawLine(
        color = shadowColor,
        start = Offset(x + inset, y + h - inset),
        end = Offset(x + w - inset, y + h - inset),
        strokeWidth = 1f
    )
    drawLine(
        color = shadowColor,
        start = Offset(x + w - inset, y + inset),
        end = Offset(x + w - inset, y + h - inset),
        strokeWidth = 1f
    )
}
