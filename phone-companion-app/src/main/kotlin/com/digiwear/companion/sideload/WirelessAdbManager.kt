package com.digiwear.companion.sideload

import android.content.Context
import android.net.wifi.WifiManager
import dadb.Dadb
import dadb.InstallResult
import dadb.UninstallResult
import io.github.muntashirakon.adb.AdbPairingRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SideloadConnectionState(val label: String) {
    IDLE("Disconnected"),
    CONNECTING("Connecting to Watch..."),
    AUTHENTICATING("Connecting to Watch..."),
    CONNECTED("Connected to Watch"),
    TRANSFERRING("Streaming APK to Watch..."),
    INSTALLING("Installing Package (-r -d -g)..."),
    GRANTING_PERMISSIONS("Granting Sensor & Notification Permissions..."),
    SUCCESS("Installation Successful!"),
    ERROR("Error")
}

data class ConnectedWatchDetails(
    val model: String = "Unknown Watch",
    val manufacturer: String = "Unknown",
    val androidVersion: String = "Unknown",
    val sdkVersion: Int = 0,
    val wearPlatform: String = "",
    val isDigiWearInstalled: Boolean = false,
    val installedVersionName: String = "",
    val batteryPercent: String = ""
)

class WirelessAdbManager(private val context: Context) {

    private val keyManager = AdbKeyManager(context)
    private var activeLibadb: DigiWearAdbConnectionManager? = null
    private var activeDadb: Dadb? = null

    var activePort: Int = 0
        private set

    private val _connectionState = MutableStateFlow(SideloadConnectionState.IDLE)
    val connectionState: StateFlow<SideloadConnectionState> = _connectionState.asStateFlow()

    private val _watchDetails = MutableStateFlow<ConnectedWatchDetails?>(null)
    val watchDetails: StateFlow<ConnectedWatchDetails?> = _watchDetails.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _transferProgress = MutableStateFlow(0f)
    val transferProgress: StateFlow<Float> = _transferProgress.asStateFlow()

    fun log(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val entry = "[$time] $message"
        _consoleLogs.value = (_consoleLogs.value + entry).takeLast(250)
    }

    fun clearLogs() {
        _consoleLogs.value = emptyList()
    }

    fun getPublicKeyFingerprint(): String = keyManager.getPublicKeyFingerprintSha256()

    fun isConnected(): Boolean = (activeLibadb?.isConnected == true || activeDadb != null) &&
        _connectionState.value != SideloadConnectionState.IDLE &&
        _connectionState.value != SideloadConnectionState.ERROR &&
        _connectionState.value != SideloadConnectionState.CONNECTING

    /**
     * Performs SPAKE2 pairing over TLS 1.3 with a Wear OS 3/4/5 smartwatch (Android 11+).
     *
     * @param host Watch IP address.
     * @param port Dynamic pairing port displayed on the watch's "Pair new device" screen.
     * @param pairingCode 6-digit Wi-Fi pairing code displayed on the watch screen.
     */
    suspend fun pair(host: String, port: Int, pairingCode: String): Boolean = withContext(Dispatchers.IO) {
        val cleanHost = host.trim()
        val cleanCode = pairingCode.trim()
        if (cleanHost.isEmpty()) {
            _lastError.value = "Watch IP address is required for pairing"
            log("Pairing Error: Watch IP address is empty")
            return@withContext false
        }
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            _lastError.value = "Pairing code must be exactly 6 digits"
            log("Pairing Error: Pairing code must be a 6-digit number (e.g. 123456)")
            return@withContext false
        }
        if (port <= 0 || port > 65535) {
            _lastError.value = "Invalid pairing port"
            log("Pairing Error: Invalid port $port (must be 1-65535)")
            return@withContext false
        }

        log("--------------------------------------------------")
        log("INITIATING SPAKE2 WIRELESS PAIRING (WEAR OS 3/4/5)...")
        log("Target: $cleanHost:$port | Code: $cleanCode")
        log("Loading RSA-2048 key & self-signed X.509 certificate...")

        try {
            val manager = DigiWearAdbConnectionManager(keyManager)
            log("Transmitting pairing credentials to watch...")
            val result = manager.pairWatch(cleanHost, port, cleanCode)
            if (result.isSuccess && result.getOrDefault(false)) {
                log("SUCCESS: Pairing authenticated!")
                log("Smartwatch has trusted this companion app's credentials.")
                log("Pairing complete. Establishing ADB session with watch...")
                true
            } else {
                val ex = result.exceptionOrNull()
                val rawErr = ex?.message ?: "Pairing request was rejected by the watch."
                val isPeerInfoErr = rawErr.contains("peer info", ignoreCase = true)
                val userMsg = if (isPeerInfoErr) {
                    "Could not exchange peer info: Pairing dialog expired or timed out on watch. Keep watch screen awake on 'Pair new device' and check that the 6-digit PIN and pairing port match."
                } else {
                    rawErr
                }
                _lastError.value = userMsg
                log("Pairing failed: $userMsg")
                if (isPeerInfoErr) {
                    log("CRITICAL NOTE: Wear OS invalidates the pairing port and 6-digit code the moment the watch display dims or sleeps.")
                    log("Please wake the watch, tap 'Pair new device' again, and enter the updated port & code.")
                }
                false
            }
        } catch (e: Exception) {
            val rawErr = e.message ?: e.javaClass.simpleName
            val isPeerInfoErr = rawErr.contains("peer info", ignoreCase = true)
            val userMsg = if (isPeerInfoErr) {
                "Could not exchange peer info: Pairing dialog expired or timed out on watch. Keep watch screen awake on 'Pair new device' and check that the 6-digit PIN and pairing port match."
            } else {
                "Pairing failed: $rawErr"
            }
            _lastError.value = userMsg
            log("SPAKE2 Pairing Error: $userMsg")
            log("Troubleshooting:")
            log("1. Ensure watch display stays awake on 'Pair new device' (do not let screen dim/sleep).")
            log("2. Verify that the 6-digit code and pairing port match what is shown on watch.")
            log("3. Ensure phone and watch are connected to the same Wi-Fi / Hotspot.")
            false
        }
    }

    /**
     * Connects to a smartwatch listening on [host]:[port] over wireless ADB.
     * Tries modern TLS 1.3 (Wear OS 3/4/5) first; falls back to legacy unencrypted ADB (AOSP / port 5555).
     */
    suspend fun connect(host: String, port: Int = 0): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        val cleanHost = host.trim()
        if (cleanHost.isEmpty()) {
            _lastError.value = "Watch IP address is required"
            log("Error: Watch IP address is empty")
            return@withContext false
        }

        if (port <= 0) {
            log("No port specified. Initiating mDNS discovery on $cleanHost...")
            return@withContext autoDiscoverAndConnect(context, cleanHost)
        }

        _connectionState.value = SideloadConnectionState.CONNECTING
        _lastError.value = null
        log("Initiating Wireless ADB connection to $cleanHost:$port...")

        // 1. Attempt modern TLS 1.3 connection (Wear OS 3/4/5 / Android 11+)
        try {
            log("Attempting TLS 1.3 ADB connection to $cleanHost:$port...")
            val manager = DigiWearAdbConnectionManager(keyManager)
            _connectionState.value = SideloadConnectionState.AUTHENTICATING

            val connResult = manager.connectWatch(cleanHost, port)
            if (connResult.isSuccess && manager.isConnected) {
                activeLibadb = manager
                activePort = port
                log("TLS 1.3 ADB Handshake OK! Querying smartwatch hardware details...")
                val details = manager.queryDetails()
                _watchDetails.value = details

                _connectionState.value = SideloadConnectionState.CONNECTED
                log("SUCCESS: Connected to ${details.model} (${details.manufacturer}) via TLS 1.3")
                log("Watch OS: Android ${details.androidVersion} (API ${details.sdkVersion}) ${details.wearPlatform}")
                if (details.isDigiWearInstalled) {
                    log("Existing DigiWear installation detected: ${details.installedVersionName}")
                } else {
                    log("DigiWear is not yet installed on this watch")
                }
                return@withContext true
            } else {
                val ex = connResult.exceptionOrNull()
                val isPairingReq = ex is AdbPairingRequiredException ||
                    ex?.cause is AdbPairingRequiredException ||
                    ex?.message?.contains("pairing required", ignoreCase = true) == true
                if (isPairingReq) {
                    throw (ex as? AdbPairingRequiredException
                        ?: (ex?.cause as? AdbPairingRequiredException)
                        ?: AdbPairingRequiredException(ex?.message ?: "Pairing required"))
                }
                log("TLS connection to port $port not accepted: ${ex?.message ?: "Refused"}")
            }
        } catch (e: AdbPairingRequiredException) {
            _connectionState.value = SideloadConnectionState.ERROR
            val msg = "Watch requires 6-digit pairing code before connecting! Use 'PAIR NEW DEVICE' below first."
            _lastError.value = msg
            log("PAIRING REQUIRED: $msg")
            disconnect()
            return@withContext false
        } catch (e: Exception) {
            log("TLS connection attempt failed: ${e.message ?: e.javaClass.simpleName}")
        }

        // 2. Attempt legacy unencrypted TCP ADB fallback (dadb) if port is 5555
        if (port == 5555) {
            log("Attempting legacy ADB fallback (AOSP / Port 5555)...")
            try {
                val keyPair = keyManager.getOrCreateKeyPair()
                val fp = keyManager.getPublicKeyFingerprintSha256()
                log("Using RSA-2048 key (SHA-256: $fp)")
                log("Connecting to watch...")

                _connectionState.value = SideloadConnectionState.AUTHENTICATING
                val dadb = Dadb.create(cleanHost, port, keyPair)
                activeDadb = dadb
                activePort = port

                log("ADB Handshake OK! Querying smartwatch hardware details...")
                val details = queryWatchDetails(dadb)
                _watchDetails.value = details

                _connectionState.value = SideloadConnectionState.CONNECTED
                log("SUCCESS: Connected to ${details.model} (${details.manufacturer}) via legacy ADB")
                log("Watch OS: Android ${details.androidVersion} (API ${details.sdkVersion}) ${details.wearPlatform}")
                if (details.isDigiWearInstalled) {
                    log("Existing DigiWear installation detected: ${details.installedVersionName}")
                } else {
                    log("DigiWear is not yet installed on this watch")
                }
                return@withContext true
            } catch (e: Exception) {
                _connectionState.value = SideloadConnectionState.ERROR
                val err = e.message ?: e.javaClass.simpleName
                _lastError.value = err
                log("Connection failed: $err")
                disconnect()
                return@withContext false
            }
        }

        _connectionState.value = SideloadConnectionState.ERROR
        val failMsg = "Could not connect to watch at $cleanHost:$port. Please verify IP & Port."
        _lastError.value = failMsg
        log("Connection failed: $failMsg")
        disconnect()
        return@withContext false
    }

    /**
     * Connects directly via TLS 1.3 without falling back to legacy dadb.
     * Used for modern Wear OS 3/4/5 watches to avoid hanging or false "Always Allow" prompts.
     */
    suspend fun connectTlsOnly(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty() || port <= 0) return@withContext false

        disconnect()
        _connectionState.value = SideloadConnectionState.CONNECTING
        _lastError.value = null

        try {
            val manager = DigiWearAdbConnectionManager(keyManager)
            val connResult = manager.connectWatch(cleanHost, port)
            if (connResult.isSuccess && manager.isConnected) {
                activeLibadb = manager
                activePort = port
                val details = manager.queryDetails()
                _watchDetails.value = details
                _connectionState.value = SideloadConnectionState.CONNECTED
                log("SUCCESS: Connected to ${details.model} (${details.manufacturer}) via TLS 1.3 on port $port")
                return@withContext true
            } else {
                val ex = connResult.exceptionOrNull()
                val err = ex?.message ?: "Handshake refused"
                log("TLS 1.3 direct connection to $cleanHost:$port failed: $err")
                _lastError.value = err
            }
        } catch (e: Exception) {
            val err = e.message ?: e.javaClass.simpleName
            log("TLS 1.3 direct connection to $cleanHost:$port error: $err")
            _lastError.value = err
        }

        _connectionState.value = SideloadConnectionState.IDLE
        false
    }

    private var multicastLock: WifiManager.MulticastLock? = null

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wifi?.createMulticastLock("DigiWearAdbMdns")?.apply {
                    setReferenceCounted(false)
                }
            }
            multicastLock?.acquire()
        } catch (_: Exception) {}
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.release()
        } catch (_: Exception) {}
    }

    /**
     * Fast-probes the smartwatch for the active Wear OS ADB port across the standard
     * ephemeral range and verifies connection by performing TLS 1.3 handshake.
     */
    suspend fun scanForAdbPort(
        host: String,
        startPort: Int = 36000,
        endPort: Int = 46000,
        timeoutMs: Int = 90,
        concurrency: Int = 100,
        skipPort: Int = 0
    ): Int? = withContext(Dispatchers.IO) {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty()) return@withContext null

        log("Probing watch ($cleanHost) for active Wear OS ADB port ($startPort..$endPort)...")
        val ports = (startPort..endPort).filter { it != skipPort }

        for (chunk in ports.chunked(concurrency)) {
            val candidatePorts = chunk.map { port ->
                async {
                    var s: Socket? = null
                    try {
                        s = Socket()
                        s.connect(InetSocketAddress(cleanHost, port), timeoutMs)
                        port
                    } catch (_: Throwable) {
                        null
                    } finally {
                        try { s?.close() } catch (_: Throwable) {}
                    }
                }
            }.awaitAll().filterNotNull()

            for (openPort in candidatePorts) {
                if (openPort == skipPort) continue
                log("Found open socket on port $openPort on $cleanHost. Verifying TLS 1.3 ADB handshake...")
                val testManager = DigiWearAdbConnectionManager(keyManager)
                val testResult = testManager.connectWatch(cleanHost, openPort)
                if (testResult.isSuccess && testManager.isConnected) {
                    log("SUCCESS: Verified Wear OS ADB TLS session on port $openPort!")
                    activeLibadb = testManager
                    activePort = openPort
                    val details = testManager.queryDetails()
                    _watchDetails.value = details
                    _connectionState.value = SideloadConnectionState.CONNECTED
                    return@withContext openPort
                } else {
                    log("Port $openPort is not an ADB TLS service. Continuing scan...")
                }
            }
        }
        null
    }

    /**
     * Automatically discovers the operational ADB port on the smartwatch via:
     * 1. Direct TLS if port is provided
     * 2. MulticastLock-enabled mDNS discovery (_adb-tls-connect._tcp)
     * 3. Fast LAN port scan verifying TLS 1.3 handshake
     */
    suspend fun autoDiscoverAndConnect(
        context: Context,
        host: String,
        nearPort: Int = 0,
        skipPort: Int = 0
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty()) return@withContext false

        log("Discovering Wear OS ADB service on $cleanHost...")
        _connectionState.value = SideloadConnectionState.CONNECTING

        // 1. Direct TLS if a specific port was provided
        if (nearPort in 1024..65535 && nearPort != skipPort) {
            log("Attempting direct TLS 1.3 connection to $cleanHost:$nearPort...")
            if (connectTlsOnly(cleanHost, nearPort)) {
                return@withContext true
            }
        }

        // 2. MulticastLock-enabled mDNS discovery of Wear OS TLS service
        acquireMulticastLock()
        try {
            log("Searching local network via mDNS (_adb-tls-connect._tcp)...")
            val manager = DigiWearAdbConnectionManager(keyManager)
            val mdnsResult = manager.autoConnectWatch(context, 2500L)
            if (mdnsResult.isSuccess && manager.isConnected) {
                activeLibadb = manager
                val details = manager.queryDetails()
                _watchDetails.value = details
                _connectionState.value = SideloadConnectionState.CONNECTED
                activePort = runCatching { manager.hostAddress.substringAfter(":", "").toIntOrNull() ?: 0 }.getOrDefault(0)
                log("SUCCESS: Connected to ${details.model} (${details.manufacturer}) via mDNS on port $activePort")
                return@withContext true
            }
        } catch (e: Exception) {
            log("mDNS search notice: ${e.message ?: "No service found"}")
        } finally {
            releaseMulticastLock()
        }

        // 3. Fast LAN port scan across primary Wear OS ephemeral range (36000..46000)
        val discoveredPort = scanForAdbPort(cleanHost, startPort = 36000, endPort = 46000, skipPort = skipPort)
        if (discoveredPort != null) {
            return@withContext true
        }

        // 4. Secondary fallback scan across lower range (32768..35999) if not found
        val secondaryPort = scanForAdbPort(cleanHost, startPort = 32768, endPort = 35999, skipPort = skipPort)
        if (secondaryPort != null) {
            return@withContext true
        }

        _connectionState.value = SideloadConnectionState.IDLE
        val msg = "Could not automatically detect watch port on $cleanHost. Please enter the Port displayed on your watch screen under Wireless Debugging."
        _lastError.value = msg
        log(msg)
        false
    }

    suspend fun autoConnect(context: Context, targetHost: String? = null): Boolean {
        return autoDiscoverAndConnect(context, targetHost ?: "", nearPort = 0)
    }

    private fun queryWatchDetails(dadb: Dadb): ConnectedWatchDetails {
        val model = dadb.shell("getprop ro.product.model").output.trim().ifEmpty { "Android Watch" }
        val manufacturer = dadb.shell("getprop ro.product.manufacturer").output.trim().ifEmpty { "Generic" }
        val androidVer = dadb.shell("getprop ro.build.version.release").output.trim().ifEmpty { "Unknown" }
        val sdkVer = dadb.shell("getprop ro.build.version.sdk").output.trim().toIntOrNull() ?: 0

        val wearVer = dadb.shell("getprop ro.wearable.platform.version").output.trim()
            .ifEmpty { dadb.shell("getprop ro.build.version.wear").output.trim() }

        val pkgCheck = dadb.shell("pm list packages com.digiwear.wear").output
        val isInstalled = pkgCheck.contains("package:com.digiwear.wear")

        val installedVersion = if (isInstalled) {
            val dumpsys = dadb.shell("dumpsys package com.digiwear.wear | grep -i versionName").output.trim()
            dumpsys.substringAfter("versionName=").substringBefore("\n").trim().ifEmpty { "v0.1.0" }
        } else ""

        val batteryOutput = dadb.shell("dumpsys battery | grep -i level").output.trim()
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

    private fun execShell(cmd: String): String {
        activeLibadb?.let { return it.execShell(cmd) }
        activeDadb?.let { return it.shell(cmd).output.trim() }
        return ""
    }

    /**
     * Installs the target APK file onto the connected watch.
     * Alias for [sideloadApk] using the active connection engine.
     */
    suspend fun installApk(
        apkFile: File,
        grantPermissions: Boolean = true,
        reinstallKeepData: Boolean = true,
        autoLaunch: Boolean = true
    ): Boolean = sideloadApk(apkFile, grantPermissions, reinstallKeepData, autoLaunch)

    /**
     * Sideloads the target APK file to the connected watch using the active ADB connection engine.
     */
    suspend fun sideloadApk(
        apkFile: File,
        grantPermissions: Boolean = true,
        reinstallKeepData: Boolean = true,
        autoLaunch: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        val libadb = activeLibadb
        val dadb = activeDadb
        if (libadb == null && dadb == null) {
            _lastError.value = "Watch is not connected"
            log("Cannot sideload: No active ADB connection")
            return@withContext false
        }

        if (!apkFile.exists() || apkFile.length() <= 0) {
            _lastError.value = "APK file not found or empty: ${apkFile.name}"
            log("Error: ${apkFile.absolutePath} not found")
            return@withContext false
        }

        val sizeMb = String.format(Locale.US, "%.2f MB", apkFile.length() / (1024.0 * 1024.0))
        log("--------------------------------------------------")
        log("SIDELOADING: ${apkFile.name} ($sizeMb)")
        log("Target: ${_watchDetails.value?.model ?: "Connected Watch"}")
        log("Engine: ${if (libadb != null) "TLS 1.3 (Wear OS 3/4/5)" else "Legacy TCP ADB"}")
        log("Options: Reinstall=$reinstallKeepData, GrantPermissions=$grantPermissions, AutoLaunch=$autoLaunch")

        _connectionState.value = SideloadConnectionState.TRANSFERRING
        var installSuccess = false

        try {
            if (libadb != null) {
                // Install via TLS connection manager
                _connectionState.value = SideloadConnectionState.INSTALLING
                log("Streaming APK to PackageInstaller session...")
                var result = libadb.installApk(apkFile) { progress ->
                    _transferProgress.value = progress
                }

                // If stream was closed due to exhausted slots, reconnect with a clean socket session and retry
                if (result.isFailure && result.exceptionOrNull()?.message?.contains("Stream closed", ignoreCase = true) == true) {
                    val currentHost = runCatching { libadb.hostAddress.substringBefore(":") }.getOrNull()
                    val currentPort = activePort
                    if (!currentHost.isNullOrEmpty() && currentPort > 0) {
                        log("Stream socket closed by watch daemon. Reconnecting clean TLS session to $currentHost:$currentPort...")
                        val freshManager = DigiWearAdbConnectionManager(keyManager)
                        val reconn = freshManager.connectWatch(currentHost, currentPort)
                        if (reconn.isSuccess && freshManager.isConnected) {
                            activeLibadb = freshManager
                            log("Connected clean! Retrying PackageInstaller stream...")
                            result = freshManager.installApk(apkFile) { progress ->
                                _transferProgress.value = progress
                            }
                        }
                    }
                }

                if (result.isSuccess) {
                    installSuccess = true
                    log("Package installer session committed successfully!")
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Installation failed"
                    _lastError.value = err
                    log("TLS Install Error: $err")
                }
            } else if (dadb != null) {
                // Install via Dadb
                val installOptions = mutableListOf<String>()
                if (reinstallKeepData) {
                    installOptions.add("-r")
                    installOptions.add("-d")
                }
                if (grantPermissions) {
                    installOptions.add("-g")
                }

                _connectionState.value = SideloadConnectionState.INSTALLING
                val installResult: InstallResult = try {
                    dadb.install(apkFile, *installOptions.toTypedArray())
                } catch (e: Exception) {
                    InstallResult.Failure(e.message ?: "Streaming install failed")
                }

                if (installResult is InstallResult.Success) {
                    installSuccess = true
                    log("Package installer session committed successfully!")
                } else {
                    val failureReason = (installResult as InstallResult.Failure).reason
                    log("Direct streaming failed ($failureReason). Falling back to push + pm install...")

                    val remotePath = "/data/local/tmp/digiwear_install.apk"
                    _connectionState.value = SideloadConnectionState.TRANSFERRING
                    log("Pushing APK to $remotePath...")
                    dadb.push(apkFile, remotePath)
                    log("Push complete. Executing pm install...")

                    _connectionState.value = SideloadConnectionState.INSTALLING
                    val flags = if (reinstallKeepData) "-r -d" else ""
                    val grantFlag = if (grantPermissions) "-g" else ""
                    val pmCmd = "pm install $flags $grantFlag $remotePath"
                    log("Running: $pmCmd")
                    var pmOutput = dadb.shell(pmCmd).output.trim()
                    log("pm output: $pmOutput")

                    if (!pmOutput.contains("Success", ignoreCase = true)) {
                        log("Retrying pm install without -g flag in case of platform restrictions...")
                        pmOutput = dadb.shell("pm install $flags $remotePath").output.trim()
                        log("pm fallback output: $pmOutput")
                    }

                    // Clean up remote temp file
                    dadb.shell("rm -f $remotePath")

                    if (pmOutput.contains("Success", ignoreCase = true)) {
                        installSuccess = true
                        log("pm install succeeded!")
                    } else {
                        _connectionState.value = SideloadConnectionState.ERROR
                        _lastError.value = "pm install failed: $pmOutput"
                        log("INSTALLATION FAILED: $pmOutput")
                        return@withContext false
                    }
                }
            }

            if (!installSuccess) {
                _connectionState.value = SideloadConnectionState.ERROR
                return@withContext false
            }

            // Post-install permission grants
            if (grantPermissions) {
                _connectionState.value = SideloadConnectionState.GRANTING_PERMISSIONS
                log("Ensuring required runtime permissions are granted...")
                val permissions = listOf(
                    "android.permission.ACTIVITY_RECOGNITION",
                    "android.permission.BODY_SENSORS",
                    "android.permission.POST_NOTIFICATIONS"
                )
                for (perm in permissions) {
                    val out = execShell("pm grant com.digiwear.wear $perm")
                    if (out.isNotEmpty()) {
                        log("Grant $perm: $out")
                    } else {
                        log("Granted: ${perm.substringAfterLast('.')}")
                    }
                }
            }

            // Refresh watch details
            if (libadb != null) {
                _watchDetails.value = libadb.queryDetails()
            } else if (dadb != null) {
                _watchDetails.value = queryWatchDetails(dadb)
            }

            // Auto-launch if requested
            if (autoLaunch) {
                log("Waking watch screen and launching DigiWear...")
                execShell("input keyevent 224") // KEYCODE_WAKEUP
                val startOutput = execShell("am start --user 0 -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10000000 -n com.digiwear.wear/.MainActivity")
                log("Launch: $startOutput")
            }

            _connectionState.value = SideloadConnectionState.SUCCESS
            log("==================================================")
            log("ALL OPERATIONS COMPLETED SUCCESSFULLY!")
            log("DigiWear is now running on your smartwatch!")
            log("==================================================")
            true
        } catch (e: Exception) {
            _connectionState.value = SideloadConnectionState.ERROR
            val err = e.message ?: e.javaClass.simpleName
            _lastError.value = err
            log("Sideload error: $err")
            false
        }
    }

    /**
     * Launches the DigiWear app on the watch.
     */
    suspend fun launchOnWatch(): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext false
        log("Waking watch screen and launching DigiWear...")
        execShell("input keyevent 224") // KEYCODE_WAKEUP
        val out = execShell("am start --user 0 -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10000000 -n com.digiwear.wear/.MainActivity")
        log("Launch result: $out")
        out.contains("Starting") || out.contains("Warning: Activity not started") || out.contains("Status: ok")
    }

    /**
     * Uninstalls any existing DigiWear package from the watch (resolves signature mismatch issues).
     */
    suspend fun uninstallFromWatch(): Boolean = withContext(Dispatchers.IO) {
        val libadb = activeLibadb
        val dadb = activeDadb
        if (libadb == null && dadb == null) return@withContext false

        log("Uninstalling com.digiwear.wear from watch...")
        var success = false

        if (libadb != null) {
            val out = libadb.execShell("pm uninstall com.digiwear.wear")
            log("pm uninstall output: $out")
            success = out.contains("Success", ignoreCase = true)
            if (success) {
                _watchDetails.value = libadb.queryDetails()
            }
        } else if (dadb != null) {
            val result = try {
                dadb.uninstall("com.digiwear.wear")
            } catch (_: Exception) {
                null
            }
            success = if (result is UninstallResult.Success) {
                true
            } else {
                val out = dadb.shell("pm uninstall com.digiwear.wear").output.trim()
                log("pm uninstall output: $out")
                out.contains("Success", ignoreCase = true)
            }
            if (success) {
                _watchDetails.value = queryWatchDetails(dadb)
            }
        }

        if (success) {
            log("com.digiwear.wear uninstalled.")
        } else {
            log("Uninstall did not find package or failed.")
        }
        success
    }

    /**
     * Refreshes the watch's status and installed package info.
     */
    suspend fun refreshWatchStatus() = withContext(Dispatchers.IO) {
        val libadb = activeLibadb
        val dadb = activeDadb
        if (libadb == null && dadb == null) return@withContext
        log("Refreshing watch diagnostic status...")
        if (libadb != null) {
            _watchDetails.value = libadb.queryDetails()
        } else if (dadb != null) {
            _watchDetails.value = queryWatchDetails(dadb)
        }
        log("Status updated.")
    }

    /**
     * Disconnects the active ADB session.
     */
    fun disconnect() {
        activePort = 0
        try {
            activeLibadb?.disconnect()
        } catch (_: Exception) {}
        try {
            activeLibadb?.close()
        } catch (_: Exception) {}
        activeLibadb = null

        try {
            activeDadb?.close()
        } catch (_: Exception) {}
        activeDadb = null

        _connectionState.value = SideloadConnectionState.IDLE
        _watchDetails.value = null
    }
}
