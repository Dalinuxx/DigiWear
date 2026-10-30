package com.digiwear.companion.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Image processing pipeline for custom LCD wallpapers:
 * 1. Center crops and scales to exactly 390x390 watch resolution.
 * 2. Applies brightness lift and contrast grading so black virtual pet sprites remain 100% visible and readable.
 * 3. Applies subtle retro LCD dot-matrix texture overlay.
 * 4. Compresses to PNG for DataLayer / AOSP LAN transmission and internal cache storage.
 */
object WallpaperProcessor {

    const val TARGET_DIMENSION = 390
    const val WALLPAPER_FILENAME = "custom_wallpaper.png"

    fun processWallpaper(context: Context, uri: Uri): Bitmap? {
        val original = try {
            // First pass: inspect bounds without allocating bitmap pixels
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

            // Sub-sample targeting ~2x target dimensions to conserve heap memory on 12-108MP camera captures
            var sampleSize = 1
            val minDim = minOf(boundsOptions.outWidth, boundsOptions.outHeight)
            while (minDim / (sampleSize * 2) >= TARGET_DIMENSION) {
                sampleSize *= 2
            }

            // Second pass: decode sub-sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (_: Throwable) {
            null
        } ?: return null

        // 1. Center crop to square
        val minDim = minOf(original.width, original.height)
        val startX = (original.width - minDim) / 2
        val startY = (original.height - minDim) / 2
        val cropped = try {
            Bitmap.createBitmap(original, startX, startY, minDim, minDim)
        } catch (_: Throwable) {
            original
        }

        // 2. Downscale to 390x390
        val scaled = try {
            Bitmap.createScaledBitmap(cropped, TARGET_DIMENSION, TARGET_DIMENSION, true)
        } catch (_: Throwable) {
            null
        }
        if (cropped != original && cropped != scaled) {
            cropped.recycle()
        }
        if (original != scaled) {
            original.recycle()
        }
        if (scaled == null) return null

        // 3. Contrast & Brightness Grading
        // Virtual pet sprites are pure black (#000000). To ensure high contrast against the photo,
        // we scale luminance by 0.70 and add a base offset (+65 R, +75 G, +60 B).
        // This ensures the darkest point in the wallpaper is lifted to a readable olive LCD tint,
        // preventing black sprites from ever getting lost in shadows.
        val gradedBitmap = try {
            Bitmap.createBitmap(TARGET_DIMENSION, TARGET_DIMENSION, Bitmap.Config.ARGB_8888)
        } catch (_: Throwable) {
            scaled.recycle()
            return null
        }
        val canvas = Canvas(gradedBitmap)

        val colorMatrix = ColorMatrix().apply {
            setScale(0.70f, 0.70f, 0.70f, 1.0f)
        }
        val biasMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, 65f,
                0f, 1f, 0f, 0f, 75f,
                0f, 0f, 1f, 0f, 60f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(biasMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        scaled.recycle()

        // 4. Subtle LCD dot-matrix overlay (dots spaced every 4px) via batched native drawPoints
        val dotPaint = Paint().apply {
            color = android.graphics.Color.argb(32, 20, 32, 20)
            strokeWidth = 1f
        }
        val steps = (TARGET_DIMENSION + 3) / 4
        val pts = FloatArray(steps * steps * 2)
        var idx = 0
        for (x in 0 until TARGET_DIMENSION step 4) {
            for (y in 0 until TARGET_DIMENSION step 4) {
                if (idx + 1 < pts.size) {
                    pts[idx++] = x.toFloat()
                    pts[idx++] = y.toFloat()
                }
            }
        }
        canvas.drawPoints(pts, 0, idx, dotPaint)

        return gradedBitmap
    }

    fun compressToPngBytes(bitmap: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }

    fun saveToInternalStorage(context: Context, bitmap: Bitmap): File {
        val file = File(context.filesDir, WALLPAPER_FILENAME)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    fun loadFromInternalStorage(context: Context): Bitmap? {
        val file = File(context.filesDir, WALLPAPER_FILENAME)
        return if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun loadPresetBitmap(context: Context, resId: Int): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, resId)
        } catch (_: Throwable) {
            null
        }
    }
}
