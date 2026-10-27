package com.digiwear.wear.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kotlin.math.cos
import kotlin.math.sin

/**
 * Authentic Digital World Holographic Long-Range Scanner Radar.
 * Displays real-time sector navigation, rotating 360° radar sweep,
 * player trajectory, and approaching Boss milestone proximity beacon.
 */
@Composable
fun AdventureRadarView(
    currentSteps: Int,
    requiredSteps: Int,
    bossName: String?,
    environmentEffect: String,
    modifier: Modifier = Modifier
) {
    val progress = (currentSteps.toFloat() / requiredSteps.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val remSteps = (requiredSteps - currentSteps).coerceAtLeast(0)
    val isNearBoss = progress >= 0.80f

    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isNearBoss) 300 else 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BeaconPulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF091410), RoundedCornerShape(10.dp))
            .border(1.dp, if (isNearBoss) Color(0xFFFF5252) else Color(0x6626A69A), RoundedCornerShape(10.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Radar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ":: RADAR SCANNER // NET-LOC ::",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Black,
                color = if (isNearBoss) Color(0xFFFF5252) else Color(0xFF64FFDA),
                fontFamily = FontFamily.Monospace
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isNearBoss) {
                    DigiVectorIcon(type = DigiIconType.ATTENTION, size = 8.dp, tint = Color(0xFFFF5252))
                }
                Text(
                    text = if (isNearBoss) "BOSS CLOSE" else "TRACKING",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNearBoss) Color(0xFFFF5252) else Color(0xFF80CBC4),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Center Circular Radar Viewport
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(Color(0xFF050D0A), CircleShape)
                .border(1.dp, Color(0xFF26A69A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(76.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f - 4f

                // Concentric range rings
                drawCircle(color = Color(0x3326A69A), radius = maxRadius * 0.35f, center = center, style = Stroke(1f))
                drawCircle(color = Color(0x3326A69A), radius = maxRadius * 0.70f, center = center, style = Stroke(1f))
                drawCircle(color = Color(0x5526A69A), radius = maxRadius, center = center, style = Stroke(1f))

                // Crosshair axes
                drawLine(Color(0x2226A69A), Offset(center.x, 4f), Offset(center.x, size.height - 4f), 1f)
                drawLine(Color(0x2226A69A), Offset(4f, center.y), Offset(size.width - 4f, center.y), 1f)

                // Rotating radar sweep line
                val rad = Math.toRadians(sweepAngle.toDouble())
                val sweepEnd = Offset(
                    center.x + (cos(rad) * maxRadius).toFloat(),
                    center.y + (sin(rad) * maxRadius).toFloat()
                )
                drawLine(
                    color = Color(0xFF64FFDA),
                    start = center,
                    end = sweepEnd,
                    strokeWidth = 1.5f
                )

                // Player position dot (moves from center outwards as progress increases)
                val playerDist = maxRadius * progress * 0.85f
                val playerPos = Offset(center.x, center.y - playerDist)
                drawCircle(
                    color = Color(0xFF00E676),
                    radius = 3f,
                    center = playerPos
                )

                // Boss Beacon (pulsing red target at top of radar)
                val bossPos = Offset(center.x, 8f)
                drawCircle(
                    color = Color(0xFFFF1744).copy(alpha = beaconPulse),
                    radius = 4f,
                    center = bossPos
                )
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = beaconPulse * 0.8f),
                    radius = 7f * beaconPulse,
                    center = bossPos,
                    style = Stroke(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Distance & Status Readout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (remSteps == 0) {
                DigiVectorIcon(type = DigiIconType.SCAN, size = 9.dp, tint = Color(0xFFFFD54F))
            }
            Text(
                text = if (remSteps == 0) "DESTINATION REACHED - ENCOUNTER READY" else "LOCK: $remSteps STEPS TO ${bossName?.uppercase() ?: "SECTOR BOSS"}",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = if (isNearBoss) Color(0xFFFFD54F) else Color(0xFFB0BEC5),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
