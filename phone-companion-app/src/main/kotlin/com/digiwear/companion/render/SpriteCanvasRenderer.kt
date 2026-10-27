package com.digiwear.companion.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.digiwear.core.model.SpriteFrame

@Composable
fun SpriteCanvasRenderer(
    speciesId: Int,
    frame: SpriteFrame = SpriteFrame.IDLE_A,
    flipHorizontal: Boolean = false,
    isAmbient: Boolean = false,
    isSilhouette: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val imageBitmap = remember(speciesId, frame, isAmbient) {
        PixelArtMatrices.getFrameBitmap(speciesId, frame, isAmbient)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val dstPx = size.toPx().toInt()
            withTransform({
                if (flipHorizontal) {
                    scale(scaleX = -1f, scaleY = 1f)
                }
            }) {
                drawImage(
                    image = imageBitmap,
                    srcOffset = IntOffset(0, 0),
                    srcSize = IntSize(64, 64),
                    dstOffset = IntOffset(0, 0),
                    dstSize = IntSize(dstPx, dstPx),
                    filterQuality = FilterQuality.None, // Crisp pixel art without blur
                    colorFilter = if (isSilhouette) SilhouetteColorFilter else null
                )
            }
        }
    }
}

/** Solid slate tint used to render undiscovered species as pixel-exact silhouettes. */
private val SilhouetteColorFilter = ColorFilter.tint(Color(0xFF37474F), BlendMode.SrcIn)
