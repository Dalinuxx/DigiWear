package com.digiwear.wear.ui

import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.SpriteFrame
import com.digiwear.wear.haptic.HapticManager
import com.digiwear.wear.render.PixelArtMatrices
import com.digiwear.wear.render.SpriteCanvasRenderer
import com.digiwear.wear.sound.BuzzerManager
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dramatic canonical Digivolution & Egg Hatch celebration sequence.
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
    hapticManager: HapticManager,
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
        com.digiwear.wear.render.PortraitAssetCache.getPortrait(context, newSpeciesId)
    }

    // Sequence orchestrator
    LaunchedEffect(Unit) {
        // Phase 0: Alert Strobe (0 - 600ms)
        buzzerManager.playAttentionBeep()
        hapticManager.playStepMilestone()
        delay(600L)

        // Phase 1: Glitch Silhouette Alternation (600ms - 2200ms)
        phase = 1
        for (i in 0 until 14) {
            glitchFrameSpeciesId = if (i % 2 == 0) newSpeciesId else previousSpeciesId
            hapticManager.playStepMilestone()
            delay(110L)
        }

        // Phase 2: Reveal & Victory Fanfare (2200ms+)
        phase = 2
        glitchFrameSpeciesId = newSpeciesId
        buzzerManager.playVictoryFanfare()
        hapticManager.playStepMilestone()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "EvoAura")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050811))
            .clickable {
                if (phase == 2) onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        when (phase) {
            0 -> {
                // Flash Strobe Alert
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DigiVectorIcon(type = if (isFromEgg) DigiIconType.EGG else DigiIconType.BOLT, size = 14.dp, tint = Color.Black)
                        Text(
                            text = if (isFromEgg) "DIGI-EGG HATCHING!" else "DIGIVOLVING!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                        DigiVectorIcon(type = if (isFromEgg) DigiIconType.EGG else DigiIconType.BOLT, size = 14.dp, tint = Color.Black)
                    }
                }
            }

            1 -> {
                // Glitch Sprite Alternation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isFromEgg) "DIGI-EGG INCUBATION STREAM" else "DATA STREAM EVOLUTION",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.3f),
                                radius = size.minDimension / 2f * auraScale,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                        if (glitchFrameSpeciesId == 0) {
                            DigiVectorIcon(
                                type = DigiIconType.EGG,
                                size = 56.dp
                            )
                        } else {
                            SpriteCanvasRenderer(
                                speciesId = glitchFrameSpeciesId,
                                isAmbient = false,
                                overrideFrame = SpriteFrame.IDLE_A,
                                size = 80.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isFromEgg) "HATCHING NEW PARTNER..." else "RECONFIGURING DIGICORE...",
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            2 -> {
                // Final Reveal Card with High-Res Character Artwork
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 22.dp, horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DigiVectorIcon(type = if (isFromEgg) DigiIconType.EGG else DigiIconType.STAR_FULL, size = 9.dp, tint = Color(0xFFFFD54F))
                        Text(
                            text = if (isFromEgg) "DIGI-EGG HATCHED!" else "EVOLUTION COMPLETE!",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFD54F),
                            textAlign = TextAlign.Center
                        )
                        DigiVectorIcon(type = if (isFromEgg) DigiIconType.EGG else DigiIconType.STAR_FULL, size = 9.dp, tint = Color(0xFFFFD54F))
                    }

                    // Portrait viewport
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(Color(0xFF0D1822), RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (highResBitmap != null) {
                            Canvas(modifier = Modifier.size(76.dp)) {
                                drawImage(
                                    image = highResBitmap,
                                    srcOffset = IntOffset.Zero,
                                    srcSize = IntSize(highResBitmap.width, highResBitmap.height),
                                    dstOffset = IntOffset.Zero,
                                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                    filterQuality = FilterQuality.High
                                )
                            }
                        } else {
                            SpriteCanvasRenderer(
                                speciesId = newSpeciesId,
                                isAmbient = false,
                                overrideFrame = SpriteFrame.IDLE_A,
                                size = 60.dp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "#${newSpecies.id} ${newSpecies.name.uppercase()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = if (isFromEgg) {
                                "${newSpecies.stage.displayName} • PRIOR: DIGI-EGG"
                            } else {
                                "${newSpecies.stage.displayName} • PRIOR: #${prevSpecies?.id} ${prevSpecies?.name?.uppercase()}"
                            },
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFD54F)
                        )
                        Text(
                            text = "${newSpecies.attribute.displayName} • HP ${newSpecies.baseHp}",
                            fontSize = 6.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF80D8FF)
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth(0.76f)
                            .height(26.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text(
                            text = "CONTINUE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}
