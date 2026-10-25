package com.digiwear.companion.sideload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat

data class WatchApkInfo(
    val file: File,
    val displayName: String,
    val sizeBytes: Long,
    val sourceType: ApkSourceType,
    val lastModifiedMs: Long = System.currentTimeMillis()
) {
    val sizeFormatted: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val mb = sizeBytes / (1024.0 * 1024.0)
            return "${DecimalFormat("#,##0.00").format(mb)} MB"
        }
}

enum class ApkSourceType(val label: String) {
    PHONE_STORAGE("Phone Storage"),
    GITHUB_DOWNLOAD("GitHub Release"),
    EMBEDDED_ASSET("Embedded Asset"),
    LOCAL_CACHE("Local Cache")
}

class WatchApkSourceManager(private val context: Context) {

    companion object {
        const val DEFAULT_RELEASE_DOWNLOAD_URL =
            "https://github.com/Dalinuxx/DigiWear/releases/latest/download/DigiWear-WearOS-release.apk"
        const val AOSP_RELEASE_DOWNLOAD_URL =
            "https://github.com/Dalinuxx/DigiWear/releases/latest/download/digiwear-aosp-standard-watch-release.apk"
        private const val CACHE_DIR_NAME = "sideload_apk"
        private const val PICKED_FILENAME = "picked_watch.apk"
        private const val DOWNLOADED_FILENAME = "downloaded_watch.apk"
        private const val ASSET_FILENAME = "asset_watch.apk"
    }

    private val cacheDir: File
        get() = File(context.cacheDir, CACHE_DIR_NAME).apply { if (!exists()) mkdirs() }

    /**
     * Checks if a cached APK from a previous pick or download is available.
     */
    fun getCachedApk(): WatchApkInfo? {
        val candidates = listOf(
            File(cacheDir, PICKED_FILENAME) to ApkSourceType.PHONE_STORAGE,
            File(cacheDir, DOWNLOADED_FILENAME) to ApkSourceType.GITHUB_DOWNLOAD,
            File(cacheDir, ASSET_FILENAME) to ApkSourceType.EMBEDDED_ASSET
        )
        for ((file, type) in candidates) {
            if (file.exists() && file.length() > 1024) {
                return WatchApkInfo(
                    file = file,
                    displayName = file.name,
                    sizeBytes = file.length(),
                    sourceType = type,
                    lastModifiedMs = file.lastModified()
                )
            }
        }
        return null
    }

    /**
     * Clears all cached/downloaded watch APKs to ensure fresh downloads.
     */
    fun clearCache() {
        try {
            File(cacheDir, PICKED_FILENAME).delete()
            File(cacheDir, DOWNLOADED_FILENAME).delete()
            File(cacheDir, ASSET_FILENAME).delete()
        } catch (_: Exception) {}
    }

    /**
     * Copies an APK selected by the user via Storage Access Framework (SAF) URI into local cache.
     */
    suspend fun importApkFromUri(uri: Uri): Result<WatchApkInfo> = withContext(Dispatchers.IO) {
        runCatching {
            var fileName = "DigiWear-Watch.apk"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        fileName = cursor.getString(nameIdx) ?: fileName
                    }
                    if (sizeIdx >= 0) {
                        fileSize = cursor.getLong(sizeIdx)
                    }
                }
            }

            val targetFile = File(cacheDir, PICKED_FILENAME)
            if (targetFile.exists()) targetFile.delete()

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Failed to open input stream for selected APK: $uri")

            val actualSize = targetFile.length()
            if (actualSize <= 0) error("Imported APK is empty")

            WatchApkInfo(
                file = targetFile,
                displayName = fileName,
                sizeBytes = actualSize,
                sourceType = ApkSourceType.PHONE_STORAGE,
                lastModifiedMs = targetFile.lastModified()
            )
        }
    }

    /**
     * Downloads an APK from a remote HTTP/HTTPS URL with live progress callback.
     */
    suspend fun downloadReleaseApk(
        urlStr: String = DEFAULT_RELEASE_DOWNLOAD_URL,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<WatchApkInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "DigiWear-PhoneCompanion-Sideload/2.0")
            }

            // Follow HTTP -> HTTPS or 302 redirects if needed
            var finalConnection = connection
            var redirectCount = 0
            while (finalConnection.responseCode in 300..399 && redirectCount < 5) {
                val newUrl = finalConnection.getHeaderField("Location") ?: break
                finalConnection.disconnect()
                finalConnection = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "DigiWear-PhoneCompanion-Sideload/2.0")
                }
                redirectCount++
            }

            val targetFile = File(cacheDir, DOWNLOADED_FILENAME)
            if (targetFile.exists()) targetFile.delete()

            try {
                if (finalConnection.responseCode !in 200..299) {
                    error("HTTP Download failed with status ${finalConnection.responseCode}: ${finalConnection.responseMessage}")
                }

                val totalBytes = finalConnection.contentLengthLong

                var downloadedBytes = 0L
                finalConnection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(32768)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            onProgress(downloadedBytes, totalBytes)
                        }
                        output.flush()
                    }
                }
            } finally {
                finalConnection.disconnect()
            }

            val actualSize = targetFile.length()
            if (actualSize <= 0) error("Downloaded file is empty")

            val suggestedName = urlStr.substringAfterLast("/").substringBefore("?").ifEmpty { "DigiWear-WearOS-release.apk" }

            WatchApkInfo(
                file = targetFile,
                displayName = suggestedName,
                sizeBytes = actualSize,
                sourceType = ApkSourceType.GITHUB_DOWNLOAD,
                lastModifiedMs = targetFile.lastModified()
            )
        }
    }

    /**
     * Checks if an APK is bundled inside the app's assets and extracts it to cache if present.
     */
    suspend fun getEmbeddedAssetApk(): WatchApkInfo? = withContext(Dispatchers.IO) {
        try {
            val assetList = context.assets.list("") ?: return@withContext null
            val candidate = assetList.firstOrNull {
                it.equals("DigiWear-WearOS-release.apk", ignoreCase = true) ||
                    it.equals("wear-os-app-release.apk", ignoreCase = true) ||
                    it.equals("digiwear-aosp-standard-watch-release.apk", ignoreCase = true)
            } ?: return@withContext null

            val targetFile = File(cacheDir, ASSET_FILENAME)
            context.assets.open(candidate).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            WatchApkInfo(
                file = targetFile,
                displayName = candidate,
                sizeBytes = targetFile.length(),
                sourceType = ApkSourceType.EMBEDDED_ASSET,
                lastModifiedMs = targetFile.lastModified()
            )
        } catch (_: Exception) {
            null
        }
    }
}
