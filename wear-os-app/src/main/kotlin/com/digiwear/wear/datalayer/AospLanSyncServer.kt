package com.digiwear.wear.datalayer

import android.content.Context
import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.PetState
import com.digiwear.wear.complication.DigiWearComplicationService
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.tile.DigiWearTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * Lightweight LAN / Hotspot Sync Server for AOSP & standard Android smartwatches
 * that do not have Google Play Services or Wear OS DataLayer pairing.
 *
 * Exposes the exact same command protocol as [WearDataSyncService] over TCP port [TCP_PORT]
 * and responds to UDP auto-discovery broadcasts on port [UDP_DISCOVERY_PORT].
 */
object AospLanSyncServer {

    const val TCP_PORT = 45820
    const val UDP_DISCOVERY_PORT = 45821
    private const val DISCOVERY_PROBE = "DIGIWEAR_DISCOVER"
    private const val DISCOVERY_REPLY_PREFIX = "DIGIWEAR_AOSP_NODE:"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    /** Maximum bytes accepted for a single request line (path or payload). Prevents memory exhaustion while allowing Base64 454x454 PNG wallpapers. */
    private const val MAX_LINE_CHARS = 512 * 1024
    private const val MAX_CONCURRENT_CLIENTS = 4
    private val clientPermits = java.util.concurrent.Semaphore(MAX_CONCURRENT_CLIENTS)

    /** Allows loopback, link-local, RFC1918 site-local and CGNAT (100.64.0.0/10, e.g. Tailscale) peers only. */
    private fun isTrustedLanAddress(address: java.net.InetAddress?): Boolean {
        if (address == null) return false
        if (address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress) return true
        val raw = address.address
        return raw.size == 4 && (raw[0].toInt() and 0xFF) == 100 && (raw[1].toInt() and 0xC0) == 0x40
    }

    /** Reads one line but never more than [MAX_LINE_CHARS]; returns null on EOF, throws on oversize. */
    private fun readBoundedLine(reader: BufferedReader): String? {
        val sb = StringBuilder()
        while (true) {
            val c = reader.read()
            if (c == -1) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) return sb.toString()
            if (c != '\r'.code) sb.append(c.toChar())
            if (sb.length > MAX_LINE_CHARS) throw java.io.IOException("Request line too long")
        }
    }

    private fun isKnownSpecies(speciesId: Int): Boolean =
        com.digiwear.core.model.PetSpecies.SPECIES_REGISTRY.containsKey(speciesId)

    @Volatile
    private var tcpJob: Job? = null

    @Volatile
    private var udpJob: Job? = null

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var udpSocket: DatagramSocket? = null

    @Synchronized
    fun start(context: Context) {
        val appContext = context.applicationContext
        if (tcpJob?.isActive != true) {
            tcpJob = scope.launch {
                try {
                    val server = ServerSocket().apply {
                        reuseAddress = true
                        bind(java.net.InetSocketAddress(TCP_PORT))
                    }
                    serverSocket = server
                    while (isActive && !server.isClosed) {
                        val client = try {
                            server.accept()
                        } catch (_: Exception) {
                            break
                        }
                        // Security: only serve local / private networks and cap concurrent sessions
                        if (!isTrustedLanAddress(client.inetAddress) || !clientPermits.tryAcquire()) {
                            try { client.close() } catch (_: Exception) {}
                            continue
                        }
                        launch {
                            try {
                                handleClient(appContext, client)
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                // Never let a single misbehaving client crash the process or the accept loop.
                                try { client.close() } catch (_: Exception) {}
                            } finally {
                                clientPermits.release()
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }

        if (udpJob?.isActive != true) {
            udpJob = scope.launch {
                try {
                    val socket = DatagramSocket(null).apply {
                        reuseAddress = true
                        broadcast = true
                        bind(java.net.InetSocketAddress(UDP_DISCOVERY_PORT))
                    }
                    udpSocket = socket
                    val buf = ByteArray(256)
                    while (isActive && !socket.isClosed) {
                        val packet = DatagramPacket(buf, buf.size)
                        try {
                            socket.receive(packet)
                            val msg = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                            if (msg.startsWith(DISCOVERY_PROBE)) {
                                val model = android.os.Build.MODEL ?: "AOSP-Watch"
                                val replyBytes = "$DISCOVERY_REPLY_PREFIX$model".toByteArray(Charsets.UTF_8)
                                val reply = DatagramPacket(
                                    replyBytes,
                                    replyBytes.size,
                                    packet.address,
                                    packet.port
                                )
                                socket.send(reply)
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        udpSocket = null
        tcpJob?.cancel()
        udpJob?.cancel()
        tcpJob = null
        udpJob = null
    }

    fun isConnectedToCharger(context: Context): Boolean {
        return try {
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.applicationContext.registerReceiver(null, filter)
            val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Pauses (fully stops) the LAN sync server — both the TCP command listener and the UDP
     * discovery listener — during ambient mode / onStop / onDestroy when not connected to a
     * charger, so the Wi-Fi chipset can enter low-power sleep. Use [resumeIfNeeded] to restart.
     */
    @Synchronized
    fun pauseIfUnplugged(context: Context) {
        if (!isConnectedToCharger(context)) {
            stop()
        }
    }

    /**
     * Resumes the LAN server upon waking or connecting to charger.
     */
    fun resumeIfNeeded(context: Context) {
        start(context)
    }

    private suspend fun handleClient(context: Context, socket: Socket) {
        socket.use { client ->
            client.soTimeout = 4000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
            // PrintWriter(OutputStream, Boolean, Charset) is API 33+; wrap a Writer for minSdk 28 AOSP watches.
            val writer = PrintWriter(java.io.OutputStreamWriter(client.getOutputStream(), Charsets.UTF_8), true)

            val path = try { readBoundedLine(reader)?.trim() } catch (_: Exception) { null } ?: return
            val payload = try { readBoundedLine(reader) } catch (_: Exception) { return } ?: ""
            val repo = PetRepository.getInstance(context)

            val response = when (path) {
                "/sync/request_pet_state" -> {
                    val state = repo.getPetState()
                    json.encodeToString(state)
                }
                "/sync/update_pet_state" -> {
                    try {
                        val incoming = json.decodeFromString<PetState>(payload)
                        if (!isKnownSpecies(incoming.speciesId)) {
                            "ERR"
                        } else {
                            repo.savePetState(incoming)
                            "OK"
                        }
                    } catch (_: Exception) {
                        "ERR"
                    }
                }
                "/sync/request_settings" -> {
                    json.encodeToString(repo.getSettingsMap())
                }
                "/sync/request_memorials" -> {
                    val list = repo.getMemorials()
                    json.encodeToString(list)
                }
                "/sync/update_settings" -> {
                    try {
                        val map = json.decodeFromString<Map<String, String>>(payload)
                        map["theme"]?.let { repo.setThemeId(it) }
                        map["vibration"]?.let { repo.setVibrationEnabled(it.toBoolean()) }
                        map["vpet_3button_mode"]?.let { repo.setThreeButtonMode(it.toBoolean()) }
                        map["vpet_button_placement"]?.let { repo.setButtonPlacement(it) }
                        map["pendulum_shake_mode"]?.let { repo.setPendulumShakeMode(it.toBoolean()) }
                        map["sound_enabled"]?.let { repo.setSoundEnabled(it.toBoolean()) }
                        map["nfc_bridge_mode"]?.let { repo.setNfcBridgeMode(it) }
                        map["display_shape_override"]?.let { repo.setDisplayShapeOverride(it) }
                        map["daily_step_goal"]?.toIntOrNull()?.let { repo.setDailyStepGoal(it) }
                        map["lcd_contrast_level"]?.let { repo.setContrastLevel(it) }
                        map["sleep_schedule"]?.let { repo.setSleepSchedule(it) }
                        map["ambient_display_style"]?.let { repo.setAmbientStyle(it) }
                        map["hazard_alerts_enabled"]?.let { repo.setHazardAlertsEnabled(it.toBoolean()) }
                        map["clock_format_24h"]?.let { repo.setClockFormat24h(it.toBoolean()) }
                        map["lifespan_canonical_enabled"]?.let { repo.setLifespanCanonicalEnabled(it.toBoolean()) }
                        map["combat_injuries_enabled"]?.let { repo.setCombatInjuriesEnabled(it.toBoolean()) }
                        map["mask_undiscovered_species"]?.let { repo.setMaskUndiscoveredSpecies(it.toBoolean()) }
                        map["soundpack"]?.let { repo.setSoundpack(com.digiwear.core.model.SoundpackProfile.fromId(it)) }
                        "OK"
                    } catch (_: Exception) {
                        "ERR"
                    }
                }
                "/sync/remote_care_action" -> {
                    try {
                        val action = payload.trim()
                        val updatedState = when {
                            action == "FEED_MEAL" || action == "FEED" -> repo.feedMeal()
                            action == "FEED_PROTEIN" -> repo.feedProtein()
                            action == "CLEAN_WASTE" || action == "CLEAN" -> repo.cleanWaste()
                            action == "TOGGLE_LIGHTS" -> repo.toggleLights()
                            action == "TOGGLE_COLD_SLEEP" -> repo.toggleColdSleep()
                            action == "DEPOSIT_VAULT" || action == "FREEZE_AND_DEPOSIT" -> {
                                repo.clearActivePet()
                                repo.getPetState()
                            }
                            action == "CURE_INJURY" -> repo.cureInjury()
                            action == "TRAIN_WIN" -> repo.train(won = true)
                            action.startsWith("HATCH_EGG:") -> {
                                val eggId = action.substringAfter("HATCH_EGG:").toIntOrNull()
                                    ?.takeIf { isKnownSpecies(it) } ?: 101
                                repo.hatchNewEgg(eggId)
                            }
                            action.startsWith("JOGRESS:") -> {
                                val partnerId = action.substringAfter("JOGRESS:").toIntOrNull() ?: 0
                                repo.performRemoteJogress(partnerId) ?: repo.getPetState()
                            }
                            else -> repo.getPetState()
                        }
                        if (action == "FEED_MEAL" || action == "FEED" || action == "CLEAN_WASTE" || action == "CLEAN" || action == "TOGGLE_LIGHTS" || action.startsWith("JOGRESS:")) {
                            DigiWearComplicationService.requestUpdate(context)
                            DigiWearTileService.requestUpdate(context)
                        }
                        json.encodeToString(updatedState)
                    } catch (_: Exception) {
                        ""
                    }
                }
                "/sync/jogress_trigger" -> {
                    try {
                        val partnerId = payload.trim().toIntOrNull() ?: 0
                        val updatedState = repo.performRemoteJogress(partnerId) ?: repo.getPetState()
                        json.encodeToString(updatedState)
                    } catch (_: Exception) {
                        ""
                    }
                }
                "/sync/battle_code" -> {
                    val code = payload.trim()
                    if (code.isNotEmpty()) {
                        repo.setBeamedCommCode(code)
                    }
                    "OK"
                }
                "/sync/campaign_data" -> {
                    val campaignJson = payload.trim()
                    if (campaignJson.isNotEmpty()) {
                        repo.saveCustomCampaignJson(campaignJson)
                        scope.launch {
                            try {
                                val parsed = com.digiwear.core.adventure.AdventureEngine.parseCampaign(campaignJson)
                                repo.switchCampaign(parsed.campaignId)
                            } catch (_: Exception) {}
                        }
                    }
                    "OK"
                }
                "/sync/upload_wallpaper" -> {
                    try {
                        val bytes = android.util.Base64.decode(payload.trim(), android.util.Base64.DEFAULT)
                        if (bytes.isNotEmpty()) {
                            val written = repo.wallpaperWriteMutex.withLock {
                                repo.writeWallpaperAtomically(bytes).also { ok ->
                                    if (ok) {
                                        repo.setThemeId(com.digiwear.core.model.CustomThemeConfig.WALLPAPER_THEME_ID)
                                        repo.notifyWallpaperUpdated()
                                    }
                                }
                            }
                            if (written) "OK" else "ERR"
                        } else {
                            "ERR"
                        }
                    } catch (_: Exception) {
                        "ERR"
                    }
                }
                else -> "UNKNOWN"
            }

            writer.println(response)
            writer.flush()
        }
    }

    fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "OFFLINE"
            for (intf in interfaces.iterator()) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses.iterator()) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (!ip.startsWith("169.254.")) {
                            return ip
                        }
                    }
                }
            }
            "OFFLINE"
        } catch (_: Exception) {
            "OFFLINE"
        }
    }
}
