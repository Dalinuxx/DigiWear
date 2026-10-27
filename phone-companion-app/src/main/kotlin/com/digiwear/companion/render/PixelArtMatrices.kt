package com.digiwear.companion.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.digiwear.core.model.SpriteFrame
import com.digiwear.core.render.SpriteMatrixRegistry

/**
 * Authentic 16x16 dot-matrix pixel art renderer for the Phone Companion App.
 * Reads canonical 1997 Bandai V-Pet matrices from SpriteMatrixRegistry (shared-vpet-core).
 * Renders with crisp pixel steps onto 64x64 canvas (FilterQuality.None) and supports low-power monochrome ambient mode.
 */
object PixelArtMatrices {

    private const val MAX_CACHE_ENTRIES = 512
    private const val CUSTOM_SPRITES_PREFS = "custom_sprites_prefs"
    private val cache = android.util.LruCache<Int, ImageBitmap>(MAX_CACHE_ENTRIES)
    private val androidBitmapCache = android.util.LruCache<Int, Bitmap>(MAX_CACHE_ENTRIES)
    private val customMatrixOverrides = java.util.concurrent.ConcurrentHashMap<String, Array<String>>()

    fun loadCustomMatrices(context: android.content.Context) {
        val prefs = context.getSharedPreferences(CUSTOM_SPRITES_PREFS, android.content.Context.MODE_PRIVATE)
        for ((key, value) in prefs.all) {
            val str = value as? String ?: continue
            val rows = str.lines().filter { it.isNotEmpty() }.toTypedArray()
            if (rows.isNotEmpty()) {
                customMatrixOverrides[key] = rows
                // Drop any stock bitmap rendered before this async load finished.
                key.substringBefore('_').toIntOrNull()?.let { invalidateSpeciesCache(it) }
            }
        }
    }

    fun setCustomMatrix(
        context: android.content.Context,
        speciesId: Int,
        frame: SpriteFrame,
        matrixRows: Array<String>
    ) {
        val prefKey = "${speciesId}_${frame.frameIndex}"
        customMatrixOverrides[prefKey] = matrixRows
        context.getSharedPreferences(CUSTOM_SPRITES_PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(prefKey, matrixRows.joinToString("\n"))
            .apply()
        invalidateSpeciesCache(speciesId)
    }

    fun clearCustomMatrix(
        context: android.content.Context,
        speciesId: Int,
        frame: SpriteFrame
    ) {
        val prefKey = "${speciesId}_${frame.frameIndex}"
        customMatrixOverrides.remove(prefKey)
        context.getSharedPreferences(CUSTOM_SPRITES_PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .remove(prefKey)
            .apply()
        invalidateSpeciesCache(speciesId)
    }

    fun getCustomMatrix(speciesId: Int, frame: SpriteFrame): Array<String>? {
        return customMatrixOverrides["${speciesId}_${frame.frameIndex}"]
    }

    fun invalidateSpeciesCache(speciesId: Int) {
        for (f in SpriteFrame.entries) {
            val activeKey = (speciesId shl 5) or (f.frameIndex shl 1)
            val ambientKey = activeKey or 1
            cache.remove(activeKey)
            cache.remove(ambientKey)
            synchronized(androidBitmapCache) {
                androidBitmapCache.remove(activeKey)
                androidBitmapCache.remove(ambientKey)
            }
        }
    }

    fun getFrameBitmap(speciesId: Int, frame: SpriteFrame, isAmbient: Boolean = false): ImageBitmap {
        val key = (speciesId shl 5) or (frame.frameIndex shl 1) or (if (isAmbient) 1 else 0)
        cache.get(key)?.let { return it }
        val img = getAndroidBitmap(speciesId, frame, isAmbient).asImageBitmap()
        cache.put(key, img)
        return img
    }

    fun getAndroidBitmap(speciesId: Int, frame: SpriteFrame = SpriteFrame.IDLE_A, isAmbient: Boolean = false): Bitmap {
        val key = (speciesId shl 5) or (frame.frameIndex shl 1) or (if (isAmbient) 1 else 0)
        androidBitmapCache.get(key)?.let { return it }
        synchronized(androidBitmapCache) {
            androidBitmapCache.get(key)?.let { return it }
            val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint().apply {
                isAntiAlias = false
                isDither = false
                isFilterBitmap = false
            }
            renderSpriteToCanvas(canvas, paint, speciesId, frame, isAmbient)
            if (!isAmbient) {
                applyVPetPixelOutline(bmp)
            }
            androidBitmapCache.put(key, bmp)
            return bmp
        }
    }

    /**
     * Post-processes a 64x64 sprite once at cache-creation time to add an authentic
     * 1-pixel Bandai Digimon Color / 0nl1ne dark pixel outline and top-edge pixel highlight.
     */
    private fun applyVPetPixelOutline(bmp: Bitmap) {
        val pixels = IntArray(64 * 64)
        val out = IntArray(64 * 64)
        bmp.getPixels(pixels, 0, 64, 0, 0, 64, 64)
        System.arraycopy(pixels, 0, out, 0, pixels.size)
        val outlineColor = Color.rgb(16, 18, 26)

        for (y in 1 until 63) {
            val row = y * 64
            for (x in 1 until 63) {
                val idx = row + x
                val c = pixels[idx]
                if ((c ushr 24) == 0) {
                    if ((pixels[idx - 1] ushr 24) != 0 ||
                        (pixels[idx + 1] ushr 24) != 0 ||
                        (pixels[idx - 64] ushr 24) != 0 ||
                        (pixels[idx + 64] ushr 24) != 0
                    ) {
                        out[idx] = outlineColor
                    }
                } else if ((pixels[idx - 64] ushr 24) == 0) {
                    val r = ((c shr 16) and 0xFF).let { (it + 24).coerceAtMost(255) }
                    val g = ((c shr 8) and 0xFF).let { (it + 24).coerceAtMost(255) }
                    val b = (c and 0xFF).let { (it + 24).coerceAtMost(255) }
                    out[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }
        bmp.setPixels(out, 0, 64, 0, 0, 64, 64)
    }

    private fun renderSpriteToCanvas(
        canvas: Canvas,
        paint: Paint,
        speciesId: Int,
        frame: SpriteFrame,
        isAmbient: Boolean
    ) {
        val palette = SpriteMatrixRegistry.getPalette(speciesId)
        val customOverride = getCustomMatrix(speciesId, frame) ?: getCustomMatrix(speciesId, SpriteFrame.IDLE_A)
        val matrix = if (customOverride != null) {
            customOverride
        } else if (isAmbient) {
            SpriteMatrixRegistry.getAmbientOutlineMatrix(speciesId, frame)
        } else {
            SpriteMatrixRegistry.getMatrix(speciesId, frame)
        }

        // Frame offsets aligned with SpriteMatrixRegistry directional transforms
        val xOffset = when (frame) {
            SpriteFrame.WALK_A -> -1.5f
            SpriteFrame.WALK_B -> 1.5f
            SpriteFrame.FEED_START -> -1f
            SpriteFrame.ATTACK_CHARGE -> 2f
            SpriteFrame.ATTACK_RELEASE -> -2f
            SpriteFrame.REFUSAL -> 1.5f
            SpriteFrame.INJURED -> 1f
            else -> 0f
        }

        val yBob = when (frame) {
            SpriteFrame.IDLE_B -> 0.5f
            SpriteFrame.WALK_A, SpriteFrame.WALK_B -> -0.5f
            SpriteFrame.SLEEP_A, SpriteFrame.SLEEP_B -> 1.5f
            SpriteFrame.JOY_EAT -> -2f
            else -> 0f
        }

        val rows = matrix.size
        val cols = matrix[0].length
        val cellSize = 3.5f
        val startX = (64f - cols * cellSize) / 2f + xOffset
        val startY = (64f - rows * cellSize) / 2f + yBob

        paint.style = Paint.Style.FILL

        for (r in 0 until rows) {
            val rowStr = matrix[r]
            val py = startY + r * cellSize
            for (c in 0 until cols) {
                // Tolerate ragged custom matrices (rows shorter than row 0) instead of crashing.
                val ch = rowStr.getOrNull(c) ?: break
                if (ch == '.') continue
                val px = startX + c * cellSize

                if (isAmbient) {
                    paint.color = Color.rgb(210, 215, 220)
                    canvas.drawRect(px + 0.5f, py + 0.5f, px + cellSize - 0.5f, py + cellSize - 0.5f, paint)
                } else {
                    paint.color = when (ch) {
                        '#' -> palette.primary
                        'S' -> palette.secondary
                        'T' -> palette.tertiary
                        'E' -> palette.eye
                        'K' -> Color.rgb(18, 20, 26)
                        'W' -> Color.rgb(250, 250, 250)
                        'B' -> Color.rgb(18, 20, 26)
                        'R' -> Color.rgb(244, 67, 54)
                        'G' -> Color.rgb(67, 185, 85)
                        'C' -> Color.rgb(0, 229, 255)
                        else -> palette.primary
                    }
                    canvas.drawRect(px, py, px + cellSize, py + cellSize, paint)
                }
            }
        }

        if (isAmbient) return

        // ==========================================
        // Universal Authentic V-Pet Action & Status Overlays
        // ==========================================
        when (frame) {
            SpriteFrame.FEED_START, SpriteFrame.JOY_EAT -> {
                // Classic V-Pet Meat-on-Bone pixel sprite in front (left side) of partner
                paint.color = Color.rgb(245, 240, 230)
                canvas.drawRect(2f, 26f + yBob, 12f, 29f + yBob, paint)
                paint.color = Color.rgb(205, 55, 45)
                if (frame == SpriteFrame.FEED_START) {
                    canvas.drawRect(4f, 23f + yBob, 10f, 32f + yBob, paint)
                } else {
                    // Half-eaten meat + Pixel Heart above head
                    canvas.drawRect(5f, 25f + yBob, 9f, 30f + yBob, paint)
                    paint.color = Color.rgb(255, 65, 110)
                    canvas.drawRect(29f + xOffset, 5f + yBob, 32f + xOffset, 8f + yBob, paint)
                    canvas.drawRect(33f + xOffset, 5f + yBob, 36f + xOffset, 8f + yBob, paint)
                    canvas.drawRect(30f + xOffset, 8f + yBob, 35f + xOffset, 11f + yBob, paint)
                }
            }
            SpriteFrame.SLEEP_A, SpriteFrame.SLEEP_B -> {
                // Classic V-Pet floating "Zzz" sleep glyph in top-right
                paint.color = Color.rgb(130, 205, 255)
                canvas.drawRect(46f, 14f, 52f, 16f, paint)
                canvas.drawRect(48f, 16f, 50f, 18f, paint)
                canvas.drawRect(46f, 18f, 52f, 20f, paint)
                if (frame == SpriteFrame.SLEEP_B) {
                    paint.color = Color.rgb(210, 240, 255)
                    canvas.drawRect(52f, 6f, 60f, 8f, paint)
                    canvas.drawRect(55f, 8f, 57f, 11f, paint)
                    canvas.drawRect(52f, 11f, 60f, 13f, paint)
                }
            }
            SpriteFrame.ATTACK_CHARGE -> {
                // Energy charge particles around front
                paint.color = Color.rgb(255, 215, 50)
                canvas.drawRect(6f, 18f, 9f, 21f, paint)
                canvas.drawRect(4f, 30f, 7f, 33f, paint)
                canvas.drawRect(7f, 42f, 10f, 45f, paint)
            }
            SpriteFrame.ATTACK_RELEASE -> {
                // Classic V-Pet Fireball / Energy Blast projectile
                paint.color = Color.rgb(245, 75, 35)
                canvas.drawRect(2f, 24f, 12f, 34f, paint)
                paint.color = Color.rgb(255, 220, 65)
                canvas.drawRect(4f, 26f, 10f, 32f, paint)
            }
            SpriteFrame.REFUSAL -> {
                // Classic V-Pet Refusal / Displeasure cross-vein mark in top-right
                paint.color = Color.rgb(244, 67, 54)
                canvas.drawRect(49f, 8f, 51f, 16f, paint)
                canvas.drawRect(53f, 8f, 55f, 16f, paint)
                canvas.drawRect(47f, 10f, 57f, 12f, paint)
                canvas.drawRect(47f, 14f, 57f, 16f, paint)
            }
            SpriteFrame.INJURED -> {
                // Classic V-Pet Two-Tone Medical Pill Capsule alert in top-right
                paint.color = Color.rgb(26, 28, 36)
                canvas.drawRect(46f, 7f, 60f, 15f, paint)
                paint.color = Color.rgb(239, 68, 68)
                canvas.drawRect(47f, 8f, 53f, 14f, paint)
                paint.color = Color.rgb(245, 247, 250)
                canvas.drawRect(53f, 8f, 59f, 14f, paint)
                paint.color = Color.rgb(255, 255, 255)
                canvas.drawRect(48f, 9f, 58f, 10f, paint)
            }
            else -> Unit
        }
    }
}
