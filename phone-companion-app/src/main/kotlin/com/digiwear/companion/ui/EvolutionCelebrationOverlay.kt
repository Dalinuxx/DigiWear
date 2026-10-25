package com.digiwear.companion.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digiwear.companion.render.PortraitAssetCache
import com.digiwear.companion.render.SpriteCanvasRenderer
import com.digiwear.companion.sound.BuzzerManager
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.SpriteFrame
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dramatic canonical Digivolution & Egg Hatch celebration sequence for Phone Companion.
 * Features:
 * 1. Rapid piezo siren and strobe flash.
 * 2. Glitch data-stream sprite alternation (old vs new form).
 * 3. High-resolution Bandai Reference Book illustrated character artwork cut-in reveal with victory fanfare.
 */
@Composable
fun EvolutionCelebrationOverlay(
    previousSpeciesId: Int,
    newSpeciesId: Int,
    buzzerManager: BuzzerManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isFromEgg = previousSpeciesId == 0
    val prevSpecies = remember(previousSpeciesId) {
        if (isFromEgg) null else PetSpecies.getById(previousSpeciesId)
    }
    val newSpecies = remember(newSpeciesId) { PetSpecies.getById(newSpeciesId) }

    // Phase: 0 = Strobe Alert, 1 = Glitch Alternation, 2 = High-Res Reveal Card
    var phase by remember { mutableIntStateOf(0) }
    var glitchFrameSpeciesId by remember { mutableIntStateOf(previousSpeciesId) }

    // High-resolution Reference Book character portrait
    val highResBitmap: ImageBitmap? = remember(newSpeciesId) {
        PortraitAssetCache.getPortrait(context, newSpeciesId)
    }

    // Sequence orchestrator
    LaunchedEffect(Unit) {
        // Phase 0: Alert Strobe (0 - 600ms)
        buzzerManager.playAttentionBeep()
        delay(600L)

        // Phase 1: Glitch Silhouette Alternation (600ms - 2200ms)
        phase = 1
        for (i in 0 until 14) {
            glitchFrameSpeciesId = if (i % 2 == 0) previousSpeciesId else newSpeciesId
            buzzerManager.playEvolutionGlitchTone()
            delay(100L + (i * 5L))
        }

        // Phase 2: Dramatic Cut-In Card Reveal & Victory Fanfare
        phase = 2
        buzzerManager.playEvolutionAlert()
    }

    // Infinite Strobe / Digital Grid Pulse Transition
    val infiniteTransition = rememberInfiniteTransition(label = "EvolutionStrobe")
    val strobeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StrobeAlpha"
    )

    val scaleAnim = remember { Animatable(0.4f) }
    LaunchedEffect(phase) {
        if (phase == 2) {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(400, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable {
                if (phase == 2) onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        // Background Matrix / Warning Rays Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val rayCount = 12
            val radius = size.maxDimension / 1.5f

            if (phase < 2) {
                // Emergency Strobe Flash
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = strobeAlpha * 0.25f),
                    radius = size.minDimension / 1.8f,
                    center = center
                )
            } else {
                // High-Voltage Emergence Radial Rays
                for (i in 0 until rayCount) {
                    val angle = (i * (360f / rayCount) + (strobeAlpha * 20f)) * (Math.PI / 180f).toFloat()
                    val endX = center.x + radius * cos(angle)
                    val endY = center.y + radius * sin(angle)
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 2.5f
                    )
                }
            }
        }

        when (phase) {
            0, 1 -> {
                // Strobe & Glitch Phase
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isFromEgg) "DIGI-EGG HATCH ALERT" else "EVOLUTION ALERT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .background(Color(0xFF09101A), RoundedCornerShape(12.dp))
                            .border(2.dp, Color(0xFF00E5FF).copy(alpha = strobeAlpha), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (glitchFrameSpeciesId == 0) {
                            DigiVectorIcon(type = DigiIconType.EGG, size = 72.dp)
                        } else {
                            SpriteCanvasRenderer(
                                speciesId = glitchFrameSpeciesId,
                                frame = SpriteFrame.IDLE_A,
                                size = 96.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isFromEgg) "INCUBATING DIGITAL EGG..." else "MUTATING DIGITAL DNA...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80CBC4),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            2 -> {
                // Phase 2: High-Resolution Illustrated Reference Book Character Card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(scaleAnim.value)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = if (isFromEgg) "DIGI-EGG HATCHED" else "EVOLUTION COMPLETE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(Color(0xFF0A0F1A), RoundedCornerShape(16.dp))
                            .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (highResBitmap != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = highResBitmap,
                                    dstOffset = IntOffset.Zero,
                                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                    filterQuality = FilterQuality.Low
                                )
                            }
                        } else {
                            SpriteCanvasRenderer(
                                speciesId = newSpecies.id,
                                frame = SpriteFrame.IDLE_A,
                                size = 160.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = newSpecies.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StageBadge(newSpecies.stage.displayName)
                        AttributeBadge(newSpecies.attribute.displayName)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFromEgg) "PRIOR PARTNER: DIGI-EGG" else "PRIOR PARTNER: #${prevSpecies?.id} ${prevSpecies?.name?.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "HP ${newSpecies.baseHp}  PWR ${newSpecies.basePower}  WT ${newSpecies.baselineWeight}G",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CLAIM PARTNER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
