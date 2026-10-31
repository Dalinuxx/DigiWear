package com.digiwear.companion.sideload

import android.content.Context
import android.net.Uri
import android.os.Build
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
    val lastModifiedMs: Long = System.currentTimeMillis(),
    val versionName: String = "",
    val versionCode: Long = 0L
) {
    val sizeFormatted: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val mb = sizeBytes / (1024.0 * 1024.0)
            return "${DecimalFormat("#,##0.00").format(mb)} MB"
        }

    /** Human readable "v0.9.0 (code 22)" or empty when the APK version could not be read. */
    val versionLabel: String
        get() = if (versionName.isNotEmpty()) "v$versionName (code $versionCode)" else ""
}

enum class ApkSourceType(val label: String) {
    PHONE_STORAGE("Phone Storage"),
    GITHUB_DOWNLOAD("GitHub release_bin"),
    EMBEDDED_ASSET("Embedded Asset"),
    LOCAL_CACHE("Local Cache")
}

class WatchApkSourceManager(private val context: Context) {

    companion object {
        const val WATCH_PACKAGE_NAME = "com.digiwear.wear"

        /** Always the newest commit on main: release_bin is rebuilt and committed with every release. */
        const val DEFAULT_RELEASE_DOWNLOAD_URL =
            "https://raw.githubusercontent.com/Dalinuxx/DigiWear/main/release_bin/DigiWear-WearOS-release.apk"
        const val AOSP_RELEASE_DOWNLOAD_URL =
            "https://raw.githubusercontent.com/Dalinuxx/DigiWear/main/release_bin/digiwear-aosp-standard-watch-release.apk"

        /** Secondary source if raw.githubusercontent.com is unreachable. */
        private const val FALLBACK_RELEASE_DOWNLOAD_URL =
            "https://github.com/Dalinuxx/DigiWear/releases/latest/download/DigiWear-WearOS-release.apk"

        private const val CACHE_DIR_NAME = "sideload_apk"
        private const val PICKED_FILENAME = "picked_watch.apk"
        private const val DOWNLOADED_FILENAME = "downloaded_watch.apk"
        private const val ASSET_FILENAME = "asset_watch.apk"
        private const val MAX_REDIRECTS = 5
    }

    private val cacheDir: File
        get() = File(context.cacheDir, CACHE_DIR_NAME).apply { if (!exists()) mkdirs() }

    /**
     * Reads packageName / versionName / versionCode straight from the APK archive.
     * Returns null when the file is not a valid APK.
     */
    @Suppress("DEPRECATION")
    fun readApkVersion(file: File): Triple<String, String, Long>? {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(file.absolutePath, 0) ?: return null
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            }
            Triple(info.packageName ?: "", info.versionName ?: "", code)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildInfo(file: File, displayName: String, type: ApkSourceType): WatchApkInfo {
        val v = readApkVersion(file)
        return WatchApkInfo(
            file = file,
            displayName = displayName,
            sizeBytes = file.length(),
            sourceType = type,
            lastModifiedMs = file.lastModified(),
            versionName = v?.second ?: "",
            versionCode = v?.third ?: 0L
        )
    }

    /** True when the APK is a readable DigiWear watch package. */
    private fun isValidWatchApk(file: File): Boolean {
        if (!file.exists() || file.length() <= 1024) return false
        val v = readApkVersion(file) ?: return false
        return v.first == WATCH_PACKAGE_NAME
    }

    /**
     * Returns the freshest cached APK: highest versionCode first, then newest modification time.
     * A stale picked/asset file can therefore never shadow a newer download.
     */
    fun getCachedApk(): WatchApkInfo? {
        val candidates = listOf(
            File(cacheDir, PICKED_FILENAME) to ApkSourceType.PHONE_STORAGE,
            File(cacheDir, DOWNLOADED_FILENAME) to ApkSourceType.GITHUB_DOWNLOAD,
            File(cacheDir, ASSET_FILENAME) to ApkSourceType.EMBEDDED_ASSET
        )
        return candidates
            .filter { (file, _) -> file.exists() && file.length() > 1024 }
            .map { (file, type) -> buildInfo(file, file.name, type) }
            .maxWithOrNull(compareBy<WatchApkInfo> { it.versionCode }.thenBy { it.lastModifiedMs })
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

    private fun deleteOthers(keep: String) {
        listOf(PICKED_FILENAME, DOWNLOADED_FILENAME, ASSET_FILENAME)
            .filter { it != keep }
            .forEach { runCatching { File(cacheDir, it).delete() } }
    }

    /**
     * Copies an APK selected by the user via Storage Access Framework (SAF) URI into local cache.
     */
    suspend fun importApkFromUri(uri: Uri): Result<WatchApkInfo> = withContext(Dispatchers.IO) {
        runCatching {
            var fileName = "DigiWear-Watch.apk"

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        fileName = cursor.getString(nameIdx) ?: fileName
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

            if (targetFile.length() <= 0) error("Imported APK is empty")
            if (!isValidWatchApk(targetFile)) {
                targetFile.delete()
                error("Selected file is not a valid DigiWear watch APK ($WATCH_PACKAGE_NAME)")
            }

            deleteOthers(PICKED_FILENAME)
            buildInfo(targetFile, fileName, ApkSourceType.PHONE_STORAGE)
        }
    }

    private fun openFollowingRedirects(urlStr: String): HttpURLConnection {
        var current = urlStr
        var redirects = 0
        while (true) {
            val url = URL(current)
            require(url.protocol.equals("https", ignoreCase = true)) { "Only HTTPS downloads are allowed" }
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("User-Agent", "DigiWear-PhoneCompanion-Sideload/2.5")
                setRequestProperty("Cache-Control", "no-cache")
            }
            val code = conn.responseCode
            if (code in 300..399 && redirects < MAX_REDIRECTS) {
                val next = conn.getHeaderField("Location")
                conn.disconnect()
                if (next.isNullOrEmpty()) error("Redirect without Location header")
                current = URL(URL(current), next).toString()
                redirects++
                continue
            }
            return conn
        }
    }

    private fun downloadOnce(
        urlStr: String,
        onProgress: (Long, Long) -> Unit
    ): WatchApkInfo {
        // Cache-buster so no intermediate cache can serve an older build.
        val busted = if (urlStr.contains("?")) "$urlStr&t=${System.currentTimeMillis()}"
        else "$urlStr?t=${System.currentTimeMillis()}"

        val conn = openFollowingRedirects(busted)
        val tmpFile = File(cacheDir, "$DOWNLOADED_FILENAME.part")
        val targetFile = File(cacheDir, DOWNLOADED_FILENAME)
        try {
            if (conn.responseCode !in 200..299) {
                error("HTTP ${conn.responseCode}: ${conn.responseMessage}")
            }
            val totalBytes = conn.contentLengthLong
            var downloaded = 0L
            tmpFile.delete()
            conn.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(32768)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, totalBytes)
                    }
                    output.flush()
                }
            }
        } finally {
            conn.disconnect()
        }

        if (tmpFile.length() <= 0) {
            tmpFile.delete()
            error("Downloaded file is empty")
        }
        if (!isValidWatchApk(tmpFile)) {
            tmpFile.delete()
            error("Downloaded file is not a valid DigiWear watch APK")
        }
        if (targetFile.exists()) targetFile.delete()
        if (!tmpFile.renameTo(targetFile)) {
            tmpFile.copyTo(targetFile, overwrite = true)
            tmpFile.delete()
        }
        deleteOthers(DOWNLOADED_FILENAME)

        val suggestedName = urlStr.substringAfterLast("/").substringBefore("?")
            .ifEmpty { "DigiWear-WearOS-release.apk" }
        return buildInfo(targetFile, suggestedName, ApkSourceType.GITHUB_DOWNLOAD)
    }

    /**
     * Downloads the newest watch APK from GitHub (release_bin on main), validating that it is a real
     * DigiWear package. Falls back to the latest-release asset URL if the primary source fails.
     */
    suspend fun downloadReleaseApk(
        urlStr: String = DEFAULT_RELEASE_DOWNLOAD_URL,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<WatchApkInfo> = withContext(Dispatchers.IO) {
        val primary = runCatching { downloadOnce(urlStr, onProgress) }
        if (primary.isSuccess) return@withContext primary
        if (urlStr == DEFAULT_RELEASE_DOWNLOAD_URL) {
            val fallback = runCatching { downloadOnce(FALLBACK_RELEASE_DOWNLOAD_URL, onProgress) }
            if (fallback.isSuccess) return@withContext fallback
        }
        primary
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
            if (!isValidWatchApk(targetFile)) {
                targetFile.delete()
                return@withContext null
            }
            buildInfo(targetFile, candidate, ApkSourceType.EMBEDDED_ASSET)
        } catch (_: Exception) {
            null
        }
    }
}
