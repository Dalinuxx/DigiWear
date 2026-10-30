package com.digiwear.companion.datalayer

import android.content.Context
import android.net.Uri
import com.digiwear.core.engine.PetSimulationEngine
import com.digiwear.core.model.PetState
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * Dual-Transport Watch Sync Manager supporting:
 * 1. Wear OS DataLayer (`com.google.android.gms.wearable`) for paired Wear OS smartwatches.
 * 2. AOSP / Standard Android Watch LAN & Hotspot Bridge (TCP port 45820 + UDP auto-discovery on 45821)
 *    for standalone Full Android Watches (Kospet, Zeblaze, Lokmat, Lemfo, Rogbid, etc.) and de-Googled devices.
 */
class PhoneWearableManager(private val context: Context) {

    companion object {
        const val AOSP_TCP_PORT = 45820
        const val AOSP_UDP_DISCOVERY_PORT = 45821
        private const val DISCOVERY_PROBE = "DIGIWEAR_DISCOVER"
        private const val DISCOVERY_REPLY_PREFIX = "DIGIWEAR_AOSP_NODE:"
        private const val AOSP_NODE_PREFIX = "aosp://"
    }

    private val prefs = context.getSharedPreferences("digiwear_companion_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val nodeClient by lazy {
        runCatching { Wearable.getNodeClient(context) }.getOrNull()
    }
    private val messageClient by lazy {
        runCatching { Wearable.getMessageClient(context) }.getOrNull()
    }

    @Volatile
    private var lastLanReachabilityTimestampMs: Long = 0L

    @Volatile
    private var lastReachableIp: String? = null

    fun getConfiguredAospWatchIp(): String =
        prefs.getString("aosp_watch_ip", "")?.trim() ?: ""

    fun setConfiguredAospWatchIp(ip: String) {
        val cleaned = ip.trim().substringBefore(":")
        prefs.edit().putString("aosp_watch_ip", cleaned).apply()
    }

    fun getMaskUndiscoveredSpecies(): Boolean =
        prefs.getBoolean("mask_undiscovered_species", true)

    /** Mirrors the watch's Battle Injuries setting (from the last known settings snapshot). Defaults to canon `true`. */
    fun isCombatInjuriesEnabled(): Boolean =
        getCachedSettings()?.get("combat_injuries_enabled")?.toBooleanStrictOrNull() ?: true

    fun setMaskUndiscoveredSpecies(masked: Boolean) {
        val editor = prefs.edit().putBoolean("mask_undiscovered_species", masked)
        // Keep the cached watch-settings snapshot consistent so an offline settings reload
        // (which falls back to this cache) doesn't revert the user's choice.
        getCachedSettings()?.let { cached ->
            val patched = cached + ("mask_undiscovered_species" to masked.toString())
            runCatching { editor.putString("cached_watch_settings", json.encodeToString(patched)) }
        }
        editor.apply()
    }

    /**
     * Broadcasts a UDP discovery probe on local Wi-Fi / Hotspot interfaces to auto-discover
     * an AOSP / standard Android watch running [com.digiwear.wear.datalayer.AospLanSyncServer].
     */
    suspend fun discoverAospWatchOnLan(timeoutMs: Int = 450): String? = withContext(Dispatchers.IO) {
        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = timeoutMs
                val probeBytes = DISCOVERY_PROBE.toByteArray(Charsets.UTF_8)

                val targets = mutableSetOf<InetAddress>()
                runCatching { targets.add(InetAddress.getByName("255.255.255.255")) }
                // Also add directed subnet broadcast addresses (e.g. 192.168.43.255 for Android Hotspot)
                runCatching {
                    val interfaces = NetworkInterface.getNetworkInterfaces()
                    if (interfaces != null) {
                        for (intf in interfaces.iterator()) {
                            if (!intf.isUp || intf.isLoopback) continue
                            for (ia in intf.interfaceAddresses) {
                                val bcast = ia.broadcast
                                if (bcast != null) targets.add(bcast)
                            }
                        }
                    }
                }

                for (target in targets) {
                    runCatching {
                        val packet = DatagramPacket(probeBytes, probeBytes.size, target, AOSP_UDP_DISCOVERY_PORT)
                        socket.send(packet)
                    }
                }

                val buf = ByteArray(256)
                val replyPacket = DatagramPacket(buf, buf.size)
                socket.receive(replyPacket)
                val replyText = String(replyPacket.data, 0, replyPacket.length, Charsets.UTF_8).trim()
                if (replyText.startsWith(DISCOVERY_REPLY_PREFIX)) {
                    val discoveredIp = replyPacket.address.hostAddress ?: return@withContext null
                    setConfiguredAospWatchIp(discoveredIp)
                    return@withContext discoveredIp
                }
            }
        } catch (_: Exception) {
        }
        null
    }

    private suspend fun sendTcpCommandToAospWatch(ip: String, path: String, payload: String): String? =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, AOSP_TCP_PORT), 1200)
                    socket.soTimeout = 2500
                    val writer = PrintWriter(socket.getOutputStream(), true, Charsets.UTF_8)
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                    writer.println(path)
                    writer.println(payload.replace("\n", " "))
                    writer.flush()
                    val response = reader.readLine()
                    if (!response.isNullOrBlank() && response != "ERR") {
                        lastLanReachabilityTimestampMs = System.currentTimeMillis()
                        lastReachableIp = ip
                    }
                    response
                }
            } catch (_: Exception) {
                if (lastReachableIp == ip) {
                    lastReachableIp = null
                    lastLanReachabilityTimestampMs = 0L
                }
                null
            }
        }

    suspend fun getConnectedWatchNodes(): List<String> = withContext(Dispatchers.IO) {
        val result = mutableListOf<String>()
        // 1. Check Wear OS DataLayer nodes
        try {
            val nc = nodeClient
            if (nc != null) {
                val nodes = Tasks.await(nc.connectedNodes)
                result.addAll(nodes.map { it.id })
            }
        } catch (_: Exception) {
        }

        // 2. Check configured or auto-discovered AOSP / standard Android watch over Wi-Fi / Hotspot LAN
        val now = System.currentTimeMillis()
        val configuredIp = getConfiguredAospWatchIp()
        if (configuredIp.isNotEmpty()) {
            val isCached = (now - lastLanReachabilityTimestampMs < 30_000L) && (lastReachableIp == configuredIp)
            if (isCached) {
                result.add("$AOSP_NODE_PREFIX$configuredIp")
            } else {
                val ping = sendTcpCommandToAospWatch(configuredIp, "/sync/request_settings", "")
                if (!ping.isNullOrBlank() && ping != "ERR") {
                    result.add("$AOSP_NODE_PREFIX$configuredIp")
                } else if (result.isEmpty()) {
                    val discovered = discoverAospWatchOnLan(350)
                    if (!discovered.isNullOrBlank()) {
                        result.add("$AOSP_NODE_PREFIX$discovered")
                    }
                }
            }
        } else if (result.isEmpty()) {
            val isCached = (now - lastLanReachabilityTimestampMs < 30_000L) && !lastReachableIp.isNullOrBlank()
            if (isCached && lastReachableIp != null) {
                result.add("$AOSP_NODE_PREFIX$lastReachableIp")
            } else {
                val discovered = discoverAospWatchOnLan(350)
                if (!discovered.isNullOrBlank()) {
                    result.add("$AOSP_NODE_PREFIX$discovered")
                }
            }
        }

        result
    }

    /**
     * Reads the asynchronously replicated pet state DataItem from Google Play Services DataClient.
     */
    suspend fun getReplicatedPetStateFromDataClient(): PetState? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse("wear://*/sync/active_pet_state")
            val dataClient = Wearable.getDataClient(context)
            val dataItems = Tasks.await(dataClient.getDataItems(uri))
            dataItems?.use { buffer ->
                for (item in buffer) {
                    if (item.uri.path == "/sync/active_pet_state") {
                        val mapItem = DataMapItem.fromDataItem(item)
                        val jsonStr = mapItem.dataMap.getString("pet_state_json")
                        if (!jsonStr.isNullOrBlank()) {
                            return@withContext json.decodeFromString<PetState>(jsonStr)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Sends `/sync/request_pet_state` to the paired Wear OS or AOSP smartwatch and awaits `/sync/response_pet_state`.
     * Falls back to replicated DataClient state if RPC message is delayed.
     */
    suspend fun requestPetStateFromWatch(): PetState? = withContext(Dispatchers.IO) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) {
            return@withContext getReplicatedPetStateFromDataClient()
        }

        // Check AOSP LAN node first if present
        val aospNode = nodes.firstOrNull { it.startsWith(AOSP_NODE_PREFIX) }
        if (aospNode != null) {
            val ip = aospNode.removePrefix(AOSP_NODE_PREFIX)
            val resp = sendTcpCommandToAospWatch(ip, "/sync/request_pet_state", "")
            if (!resp.isNullOrBlank()) {
                val parsed = runCatching { json.decodeFromString<PetState>(resp) }.getOrNull()
                if (parsed != null) return@withContext parsed
            }
        }

        val wearNodes = nodes.filterNot { it.startsWith(AOSP_NODE_PREFIX) }
        val mc = messageClient
        if (mc == null || wearNodes.isEmpty()) {
            return@withContext getReplicatedPetStateFromDataClient()
        }

        val deferred = CompletableDeferred<PetState?>()
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == "/sync/response_pet_state") {
                try {
                    val str = String(event.data, Charsets.UTF_8)
                    val state = json.decodeFromString<PetState>(str)
                    deferred.complete(state)
                } catch (_: Exception) {
                    deferred.complete(null)
                }
            }
        }

        try {
            Tasks.await(mc.addListener(listener))
            val emptyPayload = ByteArray(0)
            for (node in wearNodes) {
                try {
                    Tasks.await(mc.sendMessage(node, "/sync/request_pet_state", emptyPayload))
                } catch (_: Exception) {}
            }
            val liveState = withTimeoutOrNull(6500L) {
                deferred.await()
            }
            liveState ?: getReplicatedPetStateFromDataClient()
        } catch (_: Exception) {
            getReplicatedPetStateFromDataClient()
        } finally {
            try {
                Tasks.await(mc.removeListener(listener))
            } catch (_: Exception) {}
        }
    }

    suspend fun sendPetStateToWatch(state: PetState): Boolean = withContext(Dispatchers.IO) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) return@withContext false

        val jsonStr = json.encodeToString(state)
        val payload = jsonStr.toByteArray(Charsets.UTF_8)
        var success = false
        for (node in nodes) {
            if (node.startsWith(AOSP_NODE_PREFIX)) {
                val ip = node.removePrefix(AOSP_NODE_PREFIX)
                val resp = sendTcpCommandToAospWatch(ip, "/sync/update_pet_state", jsonStr)
                if (resp == "OK") success = true
            } else {
                try {
                    messageClient?.let { mc ->
                        Tasks.await(mc.sendMessage(node, "/sync/update_pet_state", payload))
                        success = true
                    }
                } catch (_: Exception) {}
            }
        }
        success
    }

    suspend fun requestSettingsFromWatch(): Map<String, String>? = withContext(Dispatchers.IO) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) return@withContext null

        val aospNode = nodes.firstOrNull { it.startsWith(AOSP_NODE_PREFIX) }
        if (aospNode != null) {
            val ip = aospNode.removePrefix(AOSP_NODE_PREFIX)
            val resp = sendTcpCommandToAospWatch(ip, "/sync/request_settings", "")
            if (!resp.isNullOrBlank()) {
                runCatching { return@withContext json.decodeFromString<Map<String, String>>(resp) }
            }
        }

        val wearNodes = nodes.filterNot { it.startsWith(AOSP_NODE_PREFIX) }
        val mc = messageClient ?: return@withContext null
        if (wearNodes.isEmpty()) return@withContext null

        val deferred = CompletableDeferred<Map<String, String>?>()
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == "/sync/response_settings") {
                try {
                    val str = String(event.data, Charsets.UTF_8)
                    val map = json.decodeFromString<Map<String, String>>(str)
                    deferred.complete(map)
                } catch (_: Exception) {
                    deferred.complete(null)
                }
            }
        }

        try {
            Tasks.await(mc.addListener(listener))
            val emptyPayload = ByteArray(0)
            for (node in wearNodes) {
                try {
                    Tasks.await(mc.sendMessage(node, "/sync/request_settings", emptyPayload))
                } catch (_: Exception) {}
            }
            val result = withTimeoutOrNull(6500L) {
                deferred.await()
            }
            if (result != null) {
                runCatching { prefs.edit().putString("cached_watch_settings", json.encodeToString(result)).apply() }
            }
            result
        } catch (_: Exception) {
            null
        } finally {
            try {
                Tasks.await(mc.removeListener(listener))
            } catch (_: Exception) {}
        }
    }

    fun getCachedSettings(): Map<String, String>? {
        val raw = prefs.getString("cached_watch_settings", null) ?: return null
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrNull()
    }

    suspend fun requestMemorialsFromWatch(): List<com.digiwear.core.model.MemorialRecord>? = withContext(Dispatchers.IO) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) return@withContext null

        val aospNode = nodes.firstOrNull { it.startsWith(AOSP_NODE_PREFIX) }
        if (aospNode != null) {
            val ip = aospNode.removePrefix(AOSP_NODE_PREFIX)
            val resp = sendTcpCommandToAospWatch(ip, "/sync/request_memorials", "")
            if (!resp.isNullOrBlank()) {
                runCatching { return@withContext json.decodeFromString<List<com.digiwear.core.model.MemorialRecord>>(resp) }
            }
        }

        val wearNodes = nodes.filterNot { it.startsWith(AOSP_NODE_PREFIX) }
        val mc = messageClient ?: return@withContext null
        if (wearNodes.isEmpty()) return@withContext null

        val deferred = CompletableDeferred<List<com.digiwear.core.model.MemorialRecord>?>()
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == "/sync/response_memorials") {
                try {
                    val str = String(event.data, Charsets.UTF_8)
                    val list = json.decodeFromString<List<com.digiwear.core.model.MemorialRecord>>(str)
                    deferred.complete(list)
                } catch (_: Exception) {
                    deferred.complete(null)
                }
            }
        }

        try {
            Tasks.await(mc.addListener(listener))
            for (node in wearNodes) {
                try {
                    mc.sendMessage(node, "/sync/request_memorials", ByteArray(0))
                } catch (_: Exception) {}
            }
            withTimeoutOrNull(6500L) {
                deferred.await()
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                Tasks.await(mc.removeListener(listener))
            } catch (_: Exception) {}
        }
    }

    suspend fun sendSettingsToWatch(
        themeId: String,
        vibrationEnabled: Boolean,
        threeButtonMode: Boolean,
        buttonPlacement: String = "RIGHT",
        pendulumShakeMode: Boolean = false,
        soundEnabled: Boolean = false,
        nfcBridgeMode: String = "NFC_DUEL",
        displayShapeOverride: String? = null,
        dailyStepGoal: Int = 5000,
        contrastLevel: String = "MEDIUM",
        sleepSchedule: String = "21:00_07:00",
        ambientStyle: String = "MINIMAL_DOT",
        hazardAlertsEnabled: Boolean = true,
        clockFormat24h: Boolean = false,
        lifespanCanonicalEnabled: Boolean = true,
        combatInjuriesEnabled: Boolean = true,
        soundpack: String = "ORIGINAL_1997",
        maskUndiscoveredSpecies: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) return@withContext false

        val sanitizedSleepSchedule = if (PetSimulationEngine.calculateSleepDurationMinutes(sleepSchedule) != null) {
            if (PetSimulationEngine.isValidSleepDuration(sleepSchedule)) sleepSchedule else "21:00_07:00"
        } else {
            sleepSchedule
        }

        val map = buildMap {
            put("theme", themeId)
            put("vibration", vibrationEnabled.toString())
            put("vpet_3button_mode", threeButtonMode.toString())
            put("vpet_button_placement", buttonPlacement)
            put("pendulum_shake_mode", pendulumShakeMode.toString())
            put("sound_enabled", soundEnabled.toString())
            put("nfc_bridge_mode", nfcBridgeMode)
            if (displayShapeOverride != null) {
                put("display_shape_override", displayShapeOverride)
            }
            put("daily_step_goal", dailyStepGoal.toString())
            put("lcd_contrast_level", contrastLevel)
            put("sleep_schedule", sanitizedSleepSchedule)
            put("ambient_display_style", ambientStyle)
            put("hazard_alerts_enabled", hazardAlertsEnabled.toString())
            put("clock_format_24h", clockFormat24h.toString())
            put("lifespan_canonical_enabled", lifespanCanonicalEnabled.toString())
            put("combat_injuries_enabled", combatInjuriesEnabled.toString())
            put("soundpack", soundpack)
            put("mask_undiscovered_species", maskUndiscoveredSpecies.toString())
        }
        val jsonStr = json.encodeToString(map)
        runCatching { prefs.edit().putString("cached_watch_settings", jsonStr).apply() }
        val payload = jsonStr.toByteArray(Charsets.UTF_8)
        var success = false
        for (node in nodes) {
            if (node.startsWith(AOSP_NODE_PREFIX)) {
                val ip = node.removePrefix(AOSP_NODE_PREFIX)
                val resp = sendTcpCommandToAospWatch(ip, "/sync/update_settings", jsonStr)
                if (resp == "OK") success = true
            } else {
                try {
                    messageClient?.let { mc ->
                        Tasks.await(mc.sendMessage(node, "/sync/update_settings", payload))
                        success = true
                    }
                } catch (_: Exception) {}
            }
        }
        success
    }

    suspend fun sendRemoteCareCommand(action: String): PetState? = withContext(Dispatchers.IO) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) return@withContext null

        val aospNode = nodes.firstOrNull { it.startsWith(AOSP_NODE_PREFIX) }
        if (aospNode != null) {
            val ip = aospNode.removePrefix(AOSP_NODE_PREFIX)
            val resp = sendTcpCommandToAospWatch(ip, "/sync/remote_care_action", action)
            if (!resp.isNullOrBlank()) {
                runCatching { return@withContext json.decodeFromString<PetState>(resp) }
            }
        }

        val wearNodes = nodes.filterNot { it.startsWith(AOSP_NODE_PREFIX) }
        val mc = messageClient ?: return@withContext null
        if (wearNodes.isEmpty()) return@withContext null

        val deferred = CompletableDeferred<PetState?>()
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == "/sync/response_pet_state") {
                try {
                    val str = String(event.data, Charsets.UTF_8)
                    val state = json.decodeFromString<PetState>(str)
                    deferred.complete(state)
                } catch (_: Exception) {
                    deferred.complete(null)
                }
            }
        }

        try {
            Tasks.await(mc.addListener(listener))
            val payload = action.toByteArray(Charsets.UTF_8)
            for (node in wearNodes) {
                try {
                    Tasks.await(mc.sendMessage(node, "/sync/remote_care_action", payload))
                } catch (_: Exception) {}
            }
            withTimeoutOrNull(6500L) {
                deferred.await()
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                Tasks.await(mc.removeListener(listener))
            } catch (_: Exception) {}
        }
    }

    suspend fun sendCommCodeToWatch(code: String): Boolean = withContext(Dispatchers.IO) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) return@withContext false

        val payload = code.toByteArray(Charsets.UTF_8)
        var success = false
        for (node in nodes) {
            if (node.startsWith(AOSP_NODE_PREFIX)) {
                val ip = node.removePrefix(AOSP_NODE_PREFIX)
                val resp = sendTcpCommandToAospWatch(ip, "/sync/battle_code", code)
                if (resp == "OK") success = true
            } else {
                try {
                    messageClient?.let { mc ->
                        Tasks.await(mc.sendMessage(node, "/sync/battle_code", payload))
                        success = true
                    }
                } catch (_: Exception) {}
            }
        }
        success
    }

    suspend fun sendCampaignToWatch(campaignJson: String): Boolean = withContext(Dispatchers.IO) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) return@withContext false

        val payload = campaignJson.toByteArray(Charsets.UTF_8)
        var success = false
        for (node in nodes) {
            if (node.startsWith(AOSP_NODE_PREFIX)) {
                val ip = node.removePrefix(AOSP_NODE_PREFIX)
                val resp = sendTcpCommandToAospWatch(ip, "/sync/campaign_data", campaignJson)
                if (resp == "OK") success = true
            } else {
                try {
                    messageClient?.let { mc ->
                        Tasks.await(mc.sendMessage(node, "/sync/campaign_data", payload))
                        success = true
                    }
                } catch (_: Exception) {}
            }
        }
        success
    }

    suspend fun sendWallpaperToWatch(imageBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val nodes = getConnectedWatchNodes()
        if (nodes.isEmpty()) return@withContext false

        var success = false
        // 1. Wear OS DataLayer via PutDataMapRequest with Asset
        val wearNodes = nodes.filterNot { it.startsWith(AOSP_NODE_PREFIX) }
        if (wearNodes.isNotEmpty()) {
            try {
                val asset = Asset.createFromBytes(imageBytes)
                val putDataMapReq = PutDataMapRequest.create("/custom_wallpaper").apply {
                    dataMap.putAsset("wallpaper_asset", asset)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
                val request = putDataMapReq.asPutDataRequest().setUrgent()
                Tasks.await(Wearable.getDataClient(context).putDataItem(request))
                success = true
            } catch (_: Exception) {}

            // Direct message delivery fallback
            val mc = messageClient
            if (mc != null) {
                for (node in wearNodes) {
                    try {
                        Tasks.await(mc.sendMessage(node, "/sync/upload_wallpaper", imageBytes))
                        success = true
                    } catch (_: Exception) {}
                }
            }
        }

        // 2. AOSP LAN Nodes via Base64 TCP Socket Bridge
        val aospNodes = nodes.filter { it.startsWith(AOSP_NODE_PREFIX) }
        if (aospNodes.isNotEmpty()) {
            val base64Str = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            for (node in aospNodes) {
                val ip = node.removePrefix(AOSP_NODE_PREFIX)
                val resp = sendTcpCommandToAospWatch(ip, "/sync/upload_wallpaper", base64Str)
                if (resp == "OK") success = true
            }
        }

        success
    }
}
