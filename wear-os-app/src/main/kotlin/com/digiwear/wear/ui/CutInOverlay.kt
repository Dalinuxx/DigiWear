package com.digiwear.wear.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.wear.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.render.CutInPortraitData
import com.digiwear.core.render.CutInPortraitEngine
import com.digiwear.wear.render.SpriteCanvasRenderer
import com.digiwear.wear.render.SpriteFrame
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Authentic Color V-Pet (Digital Monster Color / Pendulum Color) battle cut-in view.
 * Renders dynamic manga speedlines, glowing attribute energy aura,
 * official Reference Book character artwork portrait with eye glints, and critical hit battle banners.
 */
@Composable
fun CutInOverlay(
    speciesId: Int,
    modifier: Modifier = Modifier,
    title: String = "CRITICAL HIT",
    autoDismissMs: Long? = 750L,
    onDismiss: (() -> Unit)? = null
) {
    val cutIn = remember(speciesId) { CutInPortraitEngine.getCutInPortrait(speciesId) }
    val attrColor = Color(cutIn.attributeColorArgb)
    val secColor = Color(cutIn.secondaryAuraArgb)
    val context = LocalContext.current

    val highResBitmap = remember(speciesId) {
        com.digiwear.wear.render.PortraitAssetCache.getPortrait(context, speciesId)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "CutInAnim")
    val speedlinePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Speedlines"
    )

    LaunchedEffect(speciesId, autoDismissMs) {
        if (autoDismissMs != null && autoDismissMs > 0L) {
            delay(autoDismissMs)
            onDismiss?.invoke()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040810))
            .clickable { onDismiss?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        // 1. Dynamic Canvas Layer: Speedlines, Aura, High-Res Portrait & Manga Slash Action Frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f - 10.dp.toPx())
            val boxPx = 140.dp.toPx()
            drawActionSpeedlines(center, speedlinePhase, attrColor, secColor)
            drawAttributeAura(center, attrColor, secColor)
            drawMangaActionFrame(center, boxPx, attrColor, secColor)
            if (highResBitmap != null) {
                drawImage(
                    image = highResBitmap,
                    dstOffset = IntOffset(
                        (center.x - boxPx / 2f).toInt(),
                        (center.y - boxPx / 2f).toInt()
                    ),
                    dstSize = IntSize(boxPx.toInt(), boxPx.toInt())
                )
            } else {
                drawMagnifiedHeadshot(cutIn, center, boxPx)
            }
        }

        // 2. HUD Overlay Layer: Title, Name, Attribute & Battle Cry
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Critical Hit Banner
            Row(
                modifier = Modifier
                    .background(Color(0xFF140808), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DigiVectorIcon(type = DigiIconType.BOLT, size = 10.dp, tint = Color(0xFFFFD54F))
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                DigiVectorIcon(type = DigiIconType.BOLT, size = 10.dp, tint = Color(0xFFFFD54F))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Digimon Details Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .background(Color(0xF50A121E), RoundedCornerShape(6.dp))
                    .border(0.5.dp, attrColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(vertical = 3.dp, horizontal = 6.dp)
            ) {
                Text(
                    text = cutIn.name.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[${cutIn.attribute.displayName.take(3).uppercase()}]",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = attrColor,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "•  ${cutIn.stage.displayName.uppercase()}",
                        fontSize = 7.5.sp,
                        color = Color(0xFFB0BEC5),
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "\"${cutIn.battleCry}\"",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secColor,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Draws radiating anime/manga action speedlines from the focal center to the screen perimeter.
 */
private fun DrawScope.drawActionSpeedlines(
    center: Offset,
    phase: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val rayCount = 32
    val maxRadius = size.maxDimension
    val innerRadius = 45.dp.toPx()

    for (i in 0 until rayCount) {
        val angleDeg = (i * (360f / rayCount) + phase) % 360f
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val isAlt = i % 2 == 0

        val startX = center.x + innerRadius * cos(angleRad).toFloat()
        val startY = center.y + innerRadius * sin(angleRad).toFloat()
        val endX = center.x + maxRadius * cos(angleRad).toFloat()
        val endY = center.y + maxRadius * sin(angleRad).toFloat()

        val color = if (isAlt) primaryColor.copy(alpha = 0.22f) else secondaryColor.copy(alpha = 0.12f)
        val strokeWidth = if (isAlt) 2.5f else 1.2f

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Draws glowing concentric energy rings behind the Digimon's head.
 */
private fun DrawScope.drawAttributeAura(
    center: Offset,
    primaryColor: Color,
    secondaryColor: Color
) {
    drawCircle(
        color = primaryColor.copy(alpha = 0.15f),
        radius = 70.dp.toPx(),
        center = center
    )
    drawCircle(
        color = secondaryColor.copy(alpha = 0.08f),
        radius = 85.dp.toPx(),
        center = center
    )
}

/**
 * Renders the 10x10 cropped headshot matrix scaled onto the canvas with crisp pixel steps.
 */
private fun DrawScope.drawMagnifiedHeadshot(
    cutIn: CutInPortraitData,
    center: Offset,
    renderBoxSize: Float
) {
    val matrix = cutIn.headCropMatrix
    val rows = matrix.size
    val cols = matrix[0].length
    val cellSize = renderBoxSize / cols.toFloat()

    val startX = center.x - (cols * cellSize) / 2f
    val startY = center.y - (rows * cellSize) / 2f
    val palette = cutIn.palette

    for (r in 0 until rows) {
        val rowStr = matrix[r]
        val py = startY + r * cellSize
        for (c in 0 until cols) {
            val ch = rowStr[c]
            if (ch == '.') continue
            val px = startX + c * cellSize

            val pixelColor = when (ch) {
                '#' -> Color(palette.primary)
                'S' -> Color(palette.secondary)
                'T' -> Color(palette.tertiary)
                'E' -> Color(palette.eye)
                'K' -> Color(0xFF12141A)
                'W' -> Color(0xFFFAFAFA)
                'B' -> Color(0xFF10121A)
                'R' -> Color(0xFFF44336)
                'G' -> Color(0xFF43B955)
                'C' -> Color(0xFF00E5FF)
                else -> Color(palette.primary)
            }

            drawRect(
                color = pixelColor,
                topLeft = Offset(px, py),
                size = Size(cellSize, cellSize)
            )
        }
    }
}

/**
 * Draws an authentic diagonal manga action panel frame inspired by official Color V-Pets
 * (Digital Monster Color & Pendulum Color battle cut-ins). Replaces tactical HUD reticles and eye dots
 * with bold diagonal anime slashes and attribute energy borders.
 */
private fun DrawScope.drawMangaActionFrame(
    center: Offset,
    boxPx: Float,
    attrColor: Color,
    secColor: Color
) {
    val slant = 12.dp.toPx()
    val topY = center.y - (boxPx * 0.52f)
    val bottomY = center.y + (boxPx * 0.52f)
    val insetX = size.width * 0.12f

    // Top primary diagonal slash
    drawLine(
        color = attrColor.copy(alpha = 0.95f),
        start = Offset(insetX, topY - slant),
        end = Offset(size.width - insetX, topY + slant),
        strokeWidth = 2.5.dp.toPx()
    )
    // Top highlight line
    drawLine(
        color = Color.White.copy(alpha = 0.6f),
        start = Offset(insetX, topY - slant - 2.dp.toPx()),
        end = Offset(size.width - insetX, topY + slant - 2.dp.toPx()),
        strokeWidth = 1.dp.toPx()
    )

    // Bottom primary diagonal slash
    drawLine(
        color = attrColor.copy(alpha = 0.95f),
        start = Offset(insetX, bottomY - slant),
        end = Offset(size.width - insetX, bottomY + slant),
        strokeWidth = 2.5.dp.toPx()
    )
    // Bottom highlight line
    drawLine(
        color = Color.White.copy(alpha = 0.6f),
        start = Offset(insetX, bottomY - slant + 2.dp.toPx()),
        end = Offset(size.width - insetX, bottomY + slant + 2.dp.toPx()),
        strokeWidth = 1.dp.toPx()
    )

    // Flanking diagonal energy slashes
    val wingLen = 14.dp.toPx()
    val left = center.x - (boxPx * 0.52f)
    val right = center.x + (boxPx * 0.52f)

    drawLine(
        color = secColor.copy(alpha = 0.8f),
        start = Offset(left, center.y - wingLen),
        end = Offset(left - 5.dp.toPx(), center.y + wingLen),
        strokeWidth = 2.dp.toPx()
    )
    drawLine(
        color = secColor.copy(alpha = 0.8f),
        start = Offset(right, center.y - wingLen),
        end = Offset(right + 5.dp.toPx(), center.y + wingLen),
        strokeWidth = 2.dp.toPx()
    )
}

/**
 * Authentic battle encounter flash sequence.
 * Strobe flashes pure white -> dim invert -> second strobe -> tactical enemy card.
 */
@Composable
fun EncounterFlashOverlay(
    enemySpeciesId: Int,
    isBoss: Boolean = false,
    bossTitle: String? = null,
    onFinished: () -> Unit
) {
    var flashStep by remember { mutableIntStateOf(0) }
    val enemySpecies = remember(enemySpeciesId) { PetSpecies.getById(enemySpeciesId) }

    LaunchedEffect(enemySpeciesId) {
        // Step 0: White Strobe 1
        flashStep = 0
        delay(60L)
        // Step 1: Inverted Strobe
        flashStep = 1
        delay(60L)
        // Step 2: White Strobe 2
        flashStep = 2
        delay(70L)
        // Step 3: Enemy Reveal Card
        flashStep = 3
        delay(850L)
        // Step 4: Finished -> transition to combat arena
        onFinished()
    }

    when (flashStep) {
        0, 2 -> {
            // Intense Pure White Flash
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
        1 -> {
            // Inverted Red / Navy Strobe
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isBoss) Color(0xFF880000) else Color(0xFF001933))
            )
        }
        3 -> {
            // Tactical Enemy Encounter Reveal Card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080E18))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .background(if (isBoss) Color(0xFF4A0E17) else Color(0xFF0D253A), RoundedCornerShape(4.dp))
                            .border(1.dp, if (isBoss) Color(0xFFFF5252) else Color(0xFF40C4FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DigiVectorIcon(
                            type = if (isBoss) DigiIconType.ATTENTION else DigiIconType.BATTLE,
                            size = 10.dp,
                            tint = if (isBoss) Color(0xFFFF8A80) else Color(0xFF80D8FF)
                        )
                        Text(
                            text = if (isBoss) "APEX BOSS DETECTED!" else "WILD ENCOUNTER!",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isBoss) Color(0xFFFF8A80) else Color(0xFF80D8FF),
                            fontFamily = FontFamily.Monospace
                        )
                        DigiVectorIcon(
                            type = if (isBoss) DigiIconType.ATTENTION else DigiIconType.BATTLE,
                            size = 10.dp,
                            tint = if (isBoss) Color(0xFFFF8A80) else Color(0xFF80D8FF)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    SpriteCanvasRenderer(
                        speciesId = enemySpeciesId,
                        isAmbient = false,
                        overrideFrame = SpriteFrame.ATTACK_CHARGE,
                        size = 64.dp,
                        flipHorizontal = false
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = (bossTitle ?: enemySpecies.name).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "${enemySpecies.stage.displayName} • ${enemySpecies.attribute.displayName} • HP ${enemySpecies.baseHp}",
                        fontSize = 7.5.sp,
                        color = Color(0xFFFFD166),
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "INITIALIZING COMBAT...",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
