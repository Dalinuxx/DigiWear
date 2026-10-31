package com.digiwear.companion.sideload

import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.PrivateKey
import java.security.cert.Certificate
import java.util.concurrent.TimeUnit

/**
 * Custom ADB Connection Manager supporting Android 11+ and Wear OS 3/4/5 Wireless Debugging
 * over TLS 1.3 with SPAKE2 pairing authentication.
 *
 * Extends [AbsAdbConnectionManager] from MuntashirAkon/libadb-android, supplying
 * the on-device RSA-2048 private key and X.509 self-signed certificate.
 */
class DigiWearAdbConnectionManager(
    private val keyManager: AdbKeyManager
) : AbsAdbConnectionManager() {

    private var deviceName: String = "DigiWear-Companion"

    init {
        val api = try {
            val sdk = android.os.Build.VERSION.SDK_INT
            if (sdk > 0) sdk else 35
        } catch (_: Throwable) {
            35
        }
        setApi(api)
        setDeviceName("DigiWear-Companion")
        setTimeout(180, TimeUnit.SECONDS)
    }

    fun setDeviceName(name: String) {
        deviceName = name
    }

    public override fun getPrivateKey(): PrivateKey = keyManager.getPrivateKey()

    public override fun getCertificate(): Certificate = keyManager.getCertificate()

    public override fun getDeviceName(): String = deviceName

    /**
     * Executes SPAKE2 pairing with a smartwatch running Wear OS 3/4/5 (Android 11+).
     *
     * @param host Target watch IP address.
     * @param port Dynamic pairing port shown on the watch's "Pair new device" screen.
     * @param pairingCode 6-digit Wi-Fi pairing code displayed on the watch.
     */
    fun pairWatch(host: String, port: Int, pairingCode: String): Result<Boolean> {
        return runCatching {
            pair(host, port, pairingCode)
        }
    }

    /**
     * Establishes a TLS 1.3 or TCP connection to the smartwatch.
     *
     * @param host Target watch IP address.
     * @param port Operational debugging port displayed on the watch's Wireless Debugging screen.
     */
    fun connectWatch(host: String, port: Int): Result<Boolean> {
        return runCatching {
            connect(host, port)
        }
    }

    /**
     * Automatically discovers the smartwatch over mDNS (_adb-tls-connect._tcp and _adb._tcp)
     * and establishes an ADB TLS 1.3 or TCP connection.
     *
     * @param context Application context for NsdManager.
     * @param timeoutMs Maximum discovery wait time in milliseconds (default 4000ms).
     */
    fun autoConnectWatch(context: android.content.Context, timeoutMs: Long = 4000L): Result<Boolean> {
        return runCatching {
            autoConnect(context, timeoutMs)
        }
    }

    /**
     * Executes an arbitrary shell command on the connected smartwatch and returns its standard output.
     */
    fun execShell(command: String): String {
        val stream = openStream("shell:$command")
        return try {
            val inStream = stream.openInputStream()
            val buffer = ByteArray(4096)
            val outputStream = ByteArrayOutputStream()
            try {
                var bytesRead: Int
                while (inStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            } catch (e: java.io.IOException) {
                // In libadb-android, AdbStream.read() throws IOException("Stream closed.")
                // when the remote process exits and read queue is exhausted, rather than returning -1.
                // Treat this as EOF.
                if (e.message?.contains("Stream closed", ignoreCase = true) != true) {
                    throw e
                }
            }
            outputStream.toString("UTF-8").trim()
        } finally {
            try { stream.close() } catch (_: Exception) {}
        }
    }

    fun pairDevice(host: String, port: Int, pairingCode: String): Result<Boolean> = pairWatch(host, port, pairingCode)

    /**
     * Installs an APK onto the connected smartwatch.
     *
     * Uses direct native package installer streaming (`exec:cmd package install -S <size>`),
     * falling back to `exec:pm install -S <size>` and staged file install.
     * Correctly handles EOF semantics in libadb-android (where Stream closed signals stream completion).
     *
     * @param file Local APK file.
     * @param onProgress Optional progress callback emitting a float between 0.0 and 1.0.
     */
    fun installApk(file: File, onProgress: ((Float) -> Unit)? = null): Result<String> {
        return runCatching {
            if (!file.exists() || file.length() <= 0) {
                throw IllegalArgumentException("APK file does not exist or is empty: ${file.name}")
            }

            val totalLength = file.length()

            // 1. Direct native streaming via cmd package install (standard Android 7+ / Dadb protocol)
            val cmdOutput = tryStreamSession(
                execCmd = "exec:cmd package install -r -d -g -S $totalLength",
                file = file,
                totalLength = totalLength,
                onProgress = onProgress
            )
            if (cmdOutput.contains("Success", ignoreCase = true)) {
                return@runCatching cmdOutput
            }

            // 1b. Retry without -g in case Knox / Wear OS restricts runtime grant at install time
            if (cmdOutput.contains("Failure", ignoreCase = true) && !cmdOutput.contains("Stream closed", ignoreCase = true)) {
                val cmdNoGOutput = tryStreamSession(
                    execCmd = "exec:cmd package install -r -d -S $totalLength",
                    file = file,
                    totalLength = totalLength,
                    onProgress = onProgress
                )
                if (cmdNoGOutput.contains("Success", ignoreCase = true)) {
                    return@runCatching cmdNoGOutput
                }
            }

            // 2. Direct streaming via pm install
            val pmOutput = tryStreamSession(
                execCmd = "exec:pm install -r -d -g -S $totalLength",
                file = file,
                totalLength = totalLength,
                onProgress = onProgress
            )
            if (pmOutput.contains("Success", ignoreCase = true)) {
                return@runCatching pmOutput
            }

            // 3. Fallback: Stage to /data/local/tmp/digiwear_install.apk and execute pm install
            val stageOutput = tryStageAndInstall(file, totalLength, onProgress)
            if (stageOutput.contains("Success", ignoreCase = true)) {
                return@runCatching stageOutput
            }

            throw IllegalStateException("Installation failed: cmd=[$cmdOutput], pm=[$pmOutput], stage=[$stageOutput]")
        }
    }

    private fun tryStreamSession(
        execCmd: String,
        file: File,
        totalLength: Long,
        onProgress: ((Float) -> Unit)?
    ): String {
        return try {
            val stream = openStream(execCmd)
            try {
                val outStream = stream.openOutputStream()
                file.inputStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var totalWritten = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        outStream.write(buffer, 0, read)
                        totalWritten += read
                        onProgress?.invoke(totalWritten.toFloat() / totalLength.toFloat())
                    }
                    try {
                        outStream.flush()
                    } catch (e: java.io.IOException) {
                        // Remote process may close stream after receiving exact -S bytes
                        if (e.message?.contains("Stream closed", ignoreCase = true) != true) {
                            throw e
                        }
                    }
                }

                // Read response from remote (e.g. "Success\n" or "Failure [INSTALL_FAILED...]")
                val inStream = stream.openInputStream()
                val resultBuf = ByteArray(4096)
                val resultOut = ByteArrayOutputStream()
                try {
                    var readBytes: Int
                    while (inStream.read(resultBuf).also { readBytes = it } != -1) {
                        resultOut.write(resultBuf, 0, readBytes)
                    }
                } catch (e: java.io.IOException) {
                    // AdbStream.read() throws IOException("Stream closed.") on EOF
                    if (e.message?.contains("Stream closed", ignoreCase = true) != true) {
                        throw e
                    }
                }
                resultOut.toString("UTF-8").trim()
            } finally {
                try { stream.close() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            "Stream install error: ${e.message}"
        }
    }

    private fun tryStageAndInstall(
        file: File,
        totalLength: Long,
        onProgress: ((Float) -> Unit)?
    ): String {
        val tmpPath = "/data/local/tmp/digiwear_install.apk"

        return try {
            execShell("rm -f $tmpPath")
            val uploadStream = openStream("exec:sh -c \"cat > $tmpPath\"")
            try {
                val outStream = uploadStream.openOutputStream()
                file.inputStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var totalWritten = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        outStream.write(buffer, 0, read)
                        totalWritten += read
                        onProgress?.invoke(totalWritten.toFloat() / totalLength.toFloat())
                    }
                    try {
                        outStream.flush()
                    } catch (e: java.io.IOException) {
                        if (e.message?.contains("Stream closed", ignoreCase = true) != true) {
                            throw e
                        }
                    }
                }
            } finally {
                try { uploadStream.close() } catch (_: Exception) {}
            }

            // Execute pm install on the staged file
            var output = execShell("pm install -r -d -g $tmpPath")
            if (!output.contains("Success", ignoreCase = true)) {
                // Fallback without -g in case of restricted permissions
                val fallbackOut = execShell("pm install -r -d $tmpPath")
                if (fallbackOut.contains("Success", ignoreCase = true)) {
                    output = fallbackOut
                }
            }
            execShell("rm -f $tmpPath")
            output
        } catch (e: Exception) {
            "Stage install error: ${e.message}"
        }
    }

    /**
     * Queries connected watch hardware, OS, and package details over a SINGLE consolidated ADB shell command
     * to avoid opening and tearing down multiple streams in rapid succession (which exhausts adbd stream slots).
     */
    fun queryDetails(): ConnectedWatchDetails {
        val delimiter = "===DW_DATA==="
        val combinedCmd = "echo '$delimiter'; " +
            "getprop ro.product.model; echo '$delimiter'; " +
            "getprop ro.product.manufacturer; echo '$delimiter'; " +
            "getprop ro.build.version.release; echo '$delimiter'; " +
            "getprop ro.build.version.sdk; echo '$delimiter'; " +
            "getprop ro.wearable.platform.version; echo '$delimiter'; " +
            "pm list packages com.digiwear.wear; echo '$delimiter'; " +
            "dumpsys battery | grep -i level"

        val rawOutput = runCatching { execShell(combinedCmd) }.getOrDefault("")
        val parts = rawOutput.split(delimiter).map { it.trim() }

        val model = parts.getOrNull(1)?.ifEmpty { "Android Watch" } ?: "Android Watch"
        val manufacturer = parts.getOrNull(2)?.ifEmpty { "Generic" } ?: "Generic"
        val androidVer = parts.getOrNull(3)?.ifEmpty { "Unknown" } ?: "Unknown"
        val sdkVer = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val wearVer = parts.getOrNull(5) ?: ""
        val isInstalled = parts.getOrNull(6)?.contains("package:com.digiwear.wear") == true

        val installedVersion = if (isInstalled) {
            val dumpsys = runCatching { execShell("dumpsys package com.digiwear.wear | grep -i versionName") }.getOrDefault("")
            dumpsys.substringAfter("versionName=").substringBefore("\n").trim().ifEmpty { "v0.1.0" }
        } else ""

        val batteryOutput = parts.getOrNull(7) ?: ""
        val battery = batteryOutput.substringAfter("level:").trim().takeIf { it.isNotEmpty() }?.let { "$it%" } ?: ""

        return ConnectedWatchDetails(
            model = model,
            manufacturer = manufacturer,
            androidVersion = androidVer,
            sdkVersion = sdkVer,
            wearPlatform = if (wearVer.isNotEmpty()) "Wear OS $wearVer" else "",
            isDigiWearInstalled = isInstalled,
            installedVersionName = installedVersion,
            batteryPercent = battery
        )
    }
}
