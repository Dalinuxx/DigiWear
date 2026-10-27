package com.digiwear.wear.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SpriteCanvasRenderer(
    speciesId: Int,
    isAmbient: Boolean = false,
    overrideFrame: SpriteFrame? = null,
    isSleeping: Boolean = false,
    isInjured: Boolean = false,
    flipHorizontal: Boolean = false,
    enableIdleRoaming: Boolean = false,
    isSilhouette: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    roamAmplitude: Float = 1f,
    roamLimitLeft: Dp = Dp.Infinity,
    roamLimitRight: Dp = Dp.Infinity
) {
    // 2 Hz V-Pet LCD frame & roaming cycle in Interactive Mode; paused whenever frame is static
    var currentIdleFrame by remember { mutableStateOf(SpriteFrame.IDLE_A) }
    var roamStep by remember { mutableIntStateOf(0) }
    val isStaticFrame = isAmbient || isInjured || overrideFrame != null

    LaunchedEffect(isStaticFrame, enableIdleRoaming, isSleeping) {
        if (isStaticFrame) {
            roamStep = 0
            return@LaunchedEffect
        }

        while (true) {
            delay(500L)
            currentIdleFrame = if (currentIdleFrame == SpriteFrame.IDLE_A) {
                SpriteFrame.IDLE_B
            } else {
                SpriteFrame.IDLE_A
            }
            if (enableIdleRoaming && !isSleeping) {
                roamStep = (roamStep + 1) % 8
            } else {
                roamStep = 0
            }
        }
    }

    val activeFrame = overrideFrame ?: when {
        isInjured -> SpriteFrame.INJURED
        isSleeping -> if (currentIdleFrame == SpriteFrame.IDLE_A) SpriteFrame.SLEEP_A else SpriteFrame.SLEEP_B
        enableIdleRoaming -> when (roamStep) {
            2, 5 -> SpriteFrame.WALK_A
            3, 6 -> SpriteFrame.WALK_B
            0, 4 -> SpriteFrame.IDLE_A
            else -> SpriteFrame.IDLE_B
        }
        else -> currentIdleFrame
    }

    val baseRoamOffsetDp = if (enableIdleRoaming && !isStaticFrame && !isSleeping) {
        when (roamStep) {
            2 -> (-3).dp
            3, 4 -> (-5).dp
            5 -> (-1).dp
            6 -> 4.dp
            7 -> 2.dp
            else -> 0.dp
        }
    } else 0.dp
    // Walking range is scaled by roamAmplitude and clamped to the boundaries supplied by the layout
    // (e.g. the A/B/C button rail on one side), so the pet never walks under controls or off the bezel.
    val roamOffsetDp = (baseRoamOffsetDp * roamAmplitude).coerceIn(-roamLimitLeft, roamLimitRight)

    val effectiveFlip = if (enableIdleRoaming && !isStaticFrame && !isSleeping && roamStep in 5..6) {
        !flipHorizontal
    } else {
        flipHorizontal
    }

    val imageBitmap = remember(speciesId, activeFrame, isAmbient) {
        PixelArtMatrices.getFrameBitmap(speciesId, activeFrame, isAmbient)
    }

    // OLED Burn-in margin (minimum 10px padding enforced in ambient mode)
    val burnInPadding = if (isAmbient) 10.dp else 0.dp

    Box(
        modifier = modifier
            .padding(burnInPadding)
            .offset(x = roamOffsetDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val dstPx = size.roundToPx()
            withTransform({
                if (effectiveFlip) {
                    scale(scaleX = -1f, scaleY = 1f)
                }
            }) {
                // Draw 64x64 pixel sprite scaled with FilterQuality.None to avoid bilinear blurring
                drawImage(
                    image = imageBitmap,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(64, 64),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(dstPx, dstPx),
                    filterQuality = FilterQuality.None,
                    colorFilter = if (isSilhouette) SilhouetteColorFilter else null
                )
            }
        }
    }
}

/** Solid slate tint used to render undiscovered species as pixel-exact silhouettes. */
private val SilhouetteColorFilter = ColorFilter.tint(Color(0xFF37474F), BlendMode.SrcIn)
