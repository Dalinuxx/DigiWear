package com.digiwear.wear.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

private enum class EnvTheme {
    GLITCH_FOG,
    FROST_BLIZZARD,
    INFERNO_EMBERS,
    COSMIC_VOID,
    HOLY_BLESSING,
    NONE
}

private data class ParticleDef(
    val initX: Float,
    val initY: Float,
    val speed: Float,
    val size: Float,
    val phaseOffset: Float
)

/**
 * High-performance ambient atmospheric environmental particle renderer.
 * Visualizes authentic Digital World hazardous battle zones (X-Program Fog, Cocytus Blizzard,
 * Pyroclastic Flow, Cosmic Nihilism, Seraphic Grace) via hardware-accelerated Compose Canvas.
 */
@Composable
fun EnvironmentParticleCanvas(
    environmentEffect: String,
    modifier: Modifier = Modifier
) {
    val theme = remember(environmentEffect) {
        val env = environmentEffect.lowercase().trim().replace(Regex("[\\s-]+"), "_")
        when {
            env in listOf("glitch_fog", "x_program_fog", "x_virus_rot", "dark_miasma", "miasma_fog") -> EnvTheme.GLITCH_FOG
            env in listOf("subzero_frost", "cocytus_blizzard", "absolute_aurora", "fenrir_frost", "tidal_current", "data_storm") -> EnvTheme.FROST_BLIZZARD
            env in listOf("pyroclastic_flow", "heat_wave", "magma_heat", "ancient_inferno", "hellfire_storm", "solar_flare", "gehenna_flames", "thunderstorm") -> EnvTheme.INFERNO_EMBERS
            env in listOf("cosmic_nihilism", "quantum_erasure", "quantum_singularity", "dark_gravity", "zero_gravity", "apocalymon_void", "dark_annihilation", "grb_cataclysm", "grb_wave_cataclysm", "seven_sins_maw", "dark_area_sin_gates", "temporal_singularity") -> EnvTheme.COSMIC_VOID
            env in listOf("seraphic_grace", "holy_shield_aura", "divine_seals", "merciful_aurora", "aurora_blessing") -> EnvTheme.HOLY_BLESSING
            else -> EnvTheme.NONE
        }
    }

    if (theme == EnvTheme.NONE) return

    val particles = remember(theme) {
        List(18) { i ->
            ParticleDef(
                initX = ((i * 37 + 13) % 100) / 100f,
                initY = ((i * 53 + 29) % 100) / 100f,
                speed = 0.5f + (i % 5) * 0.25f,
                size = 2f + (i % 4) * 1.5f,
                phaseOffset = (i * 0.35f)
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "EnvParticleTicker")
    val ticker by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "EnvTicker"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            when (theme) {
                EnvTheme.GLITCH_FOG -> {
                    // Digital glitch motes sliding horizontally with sudden scanline flicker
                    val progress = (ticker * p.speed + p.phaseOffset) % 1f
                    val posX = (p.initX + progress) % 1f * w
                    val posY = (p.initY + sin((ticker + p.phaseOffset) * 6.28f) * 0.05f) * h
                    val glitchColor = if (p.size > 4f) Color(0xFF00E676).copy(alpha = 0.20f) else Color(0xFFB388FF).copy(alpha = 0.22f)
                    drawRect(
                        color = glitchColor,
                        topLeft = Offset(posX, posY),
                        size = Size(p.size * 3.5f, 2.5f)
                    )
                }

                EnvTheme.FROST_BLIZZARD -> {
                    // Crystalline sub-zero frost snowflakes drifting downward with gentle wind sway
                    val progress = (ticker * p.speed + p.phaseOffset) % 1f
                    val posY = progress * h
                    val posX = ((p.initX + sin((ticker + p.phaseOffset) * 4f) * 0.08f) % 1f).coerceIn(0f, 1f) * w
                    val frostAlpha = 0.18f + (sin((ticker + p.phaseOffset) * 3f) * 0.12f).coerceIn(0f, 0.22f)
                    drawCircle(
                        color = Color(0xFFE0F7FA).copy(alpha = frostAlpha),
                        radius = p.size,
                        center = Offset(posX, posY)
                    )
                }

                EnvTheme.INFERNO_EMBERS -> {
                    // Rising volcanic embers floating upward with heat shimmer
                    val progress = 1f - ((ticker * p.speed + p.phaseOffset) % 1f)
                    val posY = progress * h
                    val posX = ((p.initX + sin((ticker + p.phaseOffset) * 5f) * 0.06f) % 1f).coerceIn(0f, 1f) * w
                    val emberColor = if (p.size > 3.5f) Color(0xFFFF3D00) else Color(0xFFFFD54F)
                    drawCircle(
                        color = emberColor.copy(alpha = 0.25f),
                        radius = p.size * 0.9f,
                        center = Offset(posX, posY)
                    )
                }

                EnvTheme.COSMIC_VOID -> {
                    // Pulsating graviton quantum distortions
                    val pulse = (sin((ticker * 2f + p.phaseOffset) * 6.28f) + 1f) / 2f
                    val posX = p.initX * w
                    val posY = p.initY * h
                    drawCircle(
                        color = Color(0xFF7C4DFF).copy(alpha = 0.15f * pulse),
                        radius = p.size * (1f + pulse * 1.2f),
                        center = Offset(posX, posY)
                    )
                }

                EnvTheme.HOLY_BLESSING -> {
                    // Celestial gold light sparkles ascending slowly
                    val progress = 1f - ((ticker * (p.speed * 0.6f) + p.phaseOffset) % 1f)
                    val posY = progress * h
                    val posX = ((p.initX + sin((ticker + p.phaseOffset) * 3f) * 0.04f) % 1f).coerceIn(0f, 1f) * w
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = 0.22f),
                        radius = p.size * 0.85f,
                        center = Offset(posX, posY)
                    )
                }

                EnvTheme.NONE -> {}
            }
        }
    }
}
