package com.digiwear.wear.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.CustomThemeConfig
import com.digiwear.core.model.LcdPattern
import com.digiwear.wear.R
import java.io.File

/**
 * Renders procedural, battery-optimized backgrounds tailored for virtual pets.
 * Uses [drawWithCache] and batched GPU point/line primitives for zero per-frame allocations.
 * In Ambient Mode, it renders strictly pure black (#000000) for OLED power saving (OPR <= 15%).
 * In Interactive Mode, it renders vintage LCD dot matrices, custom procedural themes,
 * or user-imported custom photo wallpapers with recessed Bandai V-Pet inner screen bezels.
 */
@Composable
fun BackgroundCanvas(
    theme: BackgroundTheme,
    isAmbient: Boolean,
    modifier: Modifier = Modifier,
    lightsOff: Boolean = false,
    contrastLevel: String = "MEDIUM",
    ambientStyle: String = "MINIMAL_DOT",
    customThemeConfig: CustomThemeConfig? = null,
    wallpaperVersion: Long = 0L,
    customWallpaperFile: File? = null
) {
    val context = LocalContext.current
    val wallpaperFile = remember(customWallpaperFile) {
        customWallpaperFile ?: File(context.filesDir, "custom_wallpaper.png")
    }

    val wallpaperBitmap = remember(wallpaperVersion, wallpaperFile) {
        if (wallpaperFile.exists()) {
            try {
                BitmapFactory.decodeFile(wallpaperFile.absolutePath)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else {
            try {
                BitmapFactory.decodeResource(context.resources, R.drawable.wp_cyber_metropolis)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    val contrastMultiplier = when (contrastLevel.uppercase()) {
        "LOW" -> 0.65f
        "HIGH" -> 1.45f
        else -> 1.0f
    }

    val isCustomMatrix = theme == BackgroundTheme.CUSTOM_MATRIX
    val isCustomWallpaper = theme == BackgroundTheme.CUSTOM_WALLPAPER

    val effectivePrimaryHex = if (isCustomMatrix && customThemeConfig != null) customThemeConfig.primaryColorHex else theme.primaryColorHex
    val effectiveSecondaryHex = if (isCustomMatrix && customThemeConfig != null) customThemeConfig.secondaryColorHex else theme.secondaryColorHex
    val effectiveGridHex = if (isCustomMatrix && customThemeConfig != null) customThemeConfig.gridColorHex else theme.gridColorHex
    val effectivePattern = if (isCustomMatrix && customThemeConfig != null) customThemeConfig.pattern else LcdPattern.DOT_MATRIX

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val primaryColor = Color(effectivePrimaryHex)
                val secondaryColor = Color(effectiveSecondaryHex).copy(alpha = (0.35f * contrastMultiplier).coerceIn(0.1f, 0.8f))
                val gridColor25 = Color(effectiveGridHex).copy(alpha = (0.25f * contrastMultiplier).coerceIn(0.1f, 0.7f))
                val gridColor20 = Color(effectiveGridHex).copy(alpha = (0.20f * contrastMultiplier).coerceIn(0.08f, 0.6f))
                val gridColor15 = Color(effectiveGridHex).copy(alpha = (0.15f * contrastMultiplier).coerceIn(0.06f, 0.5f))
                val gridColor35 = Color(effectiveGridHex).copy(alpha = (0.35f * contrastMultiplier).coerceIn(0.15f, 0.85f))
                val bezelHighlight = Color(effectiveGridHex).copy(alpha = (0.45f * contrastMultiplier).coerceIn(0.2f, 0.95f))

                // Precompute LCD dot-matrix coordinates once per size/theme change
                val needsDots = theme == BackgroundTheme.CLASSIC_VPET_LCD || (isCustomMatrix && effectivePattern == LcdPattern.DOT_MATRIX) || (isCustomWallpaper && wallpaperBitmap == null)
                val lcdDots = if (needsDots) {
                    val pts = ArrayList<Offset>(1600)
                    val dotSpacing = 7f
                    var x = 3.5f
                    while (x < size.width) {
                        var y = 3.5f
                        while (y < size.height) {
                            pts.add(Offset(x, y))
                            y += dotSpacing
                        }
                        x += dotSpacing
                    }
                    pts
                } else {
                    emptyList()
                }

                // Precompute subtle brick-pattern inner bezel tick marks for Classic V-Pet or custom dot-matrix
                val needsBrickTicks = theme == BackgroundTheme.CLASSIC_VPET_LCD || (isCustomMatrix && effectivePattern == LcdPattern.DOT_MATRIX)
                val brickTicks = if (needsBrickTicks) {
                    val ticks = ArrayList<Offset>(64)
                    val step = 18f
                    var x = 12f
                    while (x < size.width - 12f) {
                        ticks.add(Offset(x, 0f))
                        ticks.add(Offset(x, 5f))
                        ticks.add(Offset(x, size.height - 5f))
                        ticks.add(Offset(x, size.height))
                        x += step
                    }
                    ticks
                } else {
                    emptyList()
                }

                // Precompute corner rivet positions once per size change to avoid 60 Hz allocations
                val cornerRivets = listOf(
                    Offset(10f, 10f),
                    Offset(size.width - 10f, 10f),
                    Offset(10f, size.height - 10f),
                    Offset(size.width - 10f, size.height - 10f)
                )

                onDrawBehind {
                    if (isAmbient || lightsOff) {
                        // OLED Power Saving: Pure pitch black base
                        drawRect(color = Color.Black)
                        if (isAmbient && ambientStyle == "DIMMED_LCD") {
                            drawRoundRect(
                                color = Color(0xFF222830),
                                topLeft = Offset(4f, 4f),
                                size = Size(size.width - 8f, size.height - 8f),
                                cornerRadius = CornerRadius(12f, 12f),
                                style = Stroke(width = 1f)
                            )
                        }
                        return@onDrawBehind
                    }

                    if (isCustomWallpaper) {
                        if (wallpaperBitmap != null) {
                            // Draw scaled custom gallery wallpaper
                            drawImage(
                                image = wallpaperBitmap,
                                dstSize = IntSize(size.width.toInt(), size.height.toInt())
                            )
                            // Authentic recessed V-Pet screen bezel
                            drawRoundRect(
                                color = Color(0x55000000),
                                topLeft = Offset(3f, 3f),
                                size = Size(size.width - 6f, size.height - 6f),
                                cornerRadius = CornerRadius(12f, 12f),
                                style = Stroke(width = 3f)
                            )
                        } else {
                            // Fallback if wallpaper not yet loaded
                            drawRect(color = Color(0xFF121814))
                            if (lcdDots.isNotEmpty()) {
                                drawPoints(
                                    points = lcdDots,
                                    pointMode = PointMode.Points,
                                    color = Color(0x3380D8FF),
                                    strokeWidth = 1.5f
                                )
                            }
                        }
                        return@onDrawBehind
                    }

                    // Draw solid background base
                    drawRect(color = primaryColor)

                    if (isCustomMatrix) {
                        when (effectivePattern) {
                            LcdPattern.DOT_MATRIX -> {
                                drawPoints(
                                    points = lcdDots,
                                    pointMode = PointMode.Points,
                                    color = gridColor25,
                                    strokeWidth = 1.5f
                                )
                                drawRoundRect(
                                    color = gridColor35,
                                    topLeft = Offset(3f, 3f),
                                    size = Size(size.width - 6f, size.height - 6f),
                                    cornerRadius = CornerRadius(12f, 12f),
                                    style = Stroke(width = 3f)
                                )
                                if (brickTicks.isNotEmpty()) {
                                    drawPoints(
                                        points = brickTicks,
                                        pointMode = PointMode.Lines,
                                        color = bezelHighlight,
                                        strokeWidth = 1.5f
                                    )
                                }
                                val rivetRadius = 2.5f
                                cornerRivets.forEach { pt ->
                                    drawCircle(color = bezelHighlight, radius = rivetRadius, center = pt, style = Stroke(width = 1f))
                                    drawLine(color = bezelHighlight, start = Offset(pt.x - 1.5f, pt.y), end = Offset(pt.x + 1.5f, pt.y), strokeWidth = 0.8f)
                                }
                            }
                            LcdPattern.GRID -> {
                                val gridSize = 22f
                                var x = 0f
                                while (x < size.width) {
                                    drawLine(
                                        color = gridColor20,
                                        start = Offset(x, 0f),
                                        end = Offset(x, size.height),
                                        strokeWidth = 1f
                                    )
                                    x += gridSize
                                }
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(
                                        color = gridColor20,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1f
                                    )
                                    y += gridSize
                                }
                                drawRoundRect(
                                    color = bezelHighlight,
                                    topLeft = Offset(2f, 2f),
                                    size = Size(size.width - 4f, size.height - 4f),
                                    cornerRadius = CornerRadius(16f, 16f),
                                    style = Stroke(width = 1.5f)
                                )
                            }
                            LcdPattern.SCANLINES -> {
                                val lineSpacing = 12f
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(
                                        color = secondaryColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1f
                                    )
                                    y += lineSpacing
                                }
                                drawLine(
                                    color = bezelHighlight,
                                    start = Offset(0f, size.height * 0.72f),
                                    end = Offset(size.width, size.height * 0.72f),
                                    strokeWidth = 1.5f
                                )
                            }
                            LcdPattern.OLED -> {
                                drawRect(color = primaryColor)
                            }
                        }
                        return@onDrawBehind
                    }

                    when (theme) {
                        BackgroundTheme.CLASSIC_VPET_LCD -> {
                            // Single batched GPU call for authentic 1997 LCD dot-matrix pixel grid
                            drawPoints(
                                points = lcdDots,
                                pointMode = PointMode.Points,
                                color = gridColor25,
                                strokeWidth = 1.5f
                            )
                            // Recessed Bandai V-Pet LCD screen cage border
                            drawRoundRect(
                                color = gridColor35,
                                topLeft = Offset(3f, 3f),
                                size = Size(size.width - 6f, size.height - 6f),
                                cornerRadius = CornerRadius(12f, 12f),
                                style = Stroke(width = 3f)
                            )
                            if (brickTicks.isNotEmpty()) {
                                drawPoints(
                                    points = brickTicks,
                                    pointMode = PointMode.Lines,
                                    color = bezelHighlight,
                                    strokeWidth = 1.5f
                                )
                            }
                            // Authentic corner Digivice shell rivets
                            val rivetRadius = 2.5f
                            listOf(
                                Offset(10f, 10f),
                                Offset(size.width - 10f, 10f),
                                Offset(10f, size.height - 10f),
                                Offset(size.width - 10f, size.height - 10f)
                            ).forEach { pt ->
                                drawCircle(color = bezelHighlight, radius = rivetRadius, center = pt, style = Stroke(width = 1f))
                                drawLine(color = bezelHighlight, start = Offset(pt.x - 1.5f, pt.y), end = Offset(pt.x + 1.5f, pt.y), strokeWidth = 0.8f)
                            }
                        }
                        BackgroundTheme.CYBER_FOREST, BackgroundTheme.DATA_WASTELAND -> {
                            val lineSpacing = 12f
                            var y = 0f
                            while (y < size.height) {
                                drawLine(
                                    color = secondaryColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f
                                )
                                y += lineSpacing
                            }
                            // Subtle Digivice radar horizon line
                            drawLine(
                                color = bezelHighlight,
                                start = Offset(0f, size.height * 0.72f),
                                end = Offset(size.width, size.height * 0.72f),
                                strokeWidth = 1.5f
                            )
                        }
                        BackgroundTheme.DIGITAL_OCEAN, BackgroundTheme.NEON_CORE_CITY -> {
                            val gridSize = 22f
                            var x = 0f
                            while (x < size.width) {
                                drawLine(
                                    color = gridColor20,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 1f
                                )
                                x += gridSize
                            }
                            var y = 0f
                            while (y < size.height) {
                                drawLine(
                                    color = gridColor20,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f
                                )
                                y += gridSize
                            }
                            drawRoundRect(
                                color = bezelHighlight,
                                topLeft = Offset(2f, 2f),
                                size = Size(size.width - 4f, size.height - 4f),
                                cornerRadius = CornerRadius(16f, 16f),
                                style = Stroke(width = 1.5f)
                            )
                        }
                        BackgroundTheme.GLITCH_VOID -> {
                            val step = 16f
                            var y = 0f
                            var flip = false
                            while (y < size.height) {
                                drawLine(
                                    color = if (flip) gridColor15 else gridColor35,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 2f
                                )
                                flip = !flip
                                y += step
                            }
                        }
                        BackgroundTheme.DARK_NETWORK, BackgroundTheme.OLED_MINIMAL, BackgroundTheme.CUSTOM_MATRIX, BackgroundTheme.CUSTOM_WALLPAPER -> {
                            drawRect(color = primaryColor)
                        }
                    }
                }
            }
    )
}
