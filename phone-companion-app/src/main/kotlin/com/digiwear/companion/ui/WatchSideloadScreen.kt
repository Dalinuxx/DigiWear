package com.digiwear.companion.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.sideload.ApkSourceType
import com.digiwear.companion.sideload.SideloadConnectionState
import com.digiwear.companion.sideload.WatchApkInfo
import com.digiwear.companion.sideload.WatchApkSourceManager
import com.digiwear.companion.sideload.WirelessAdbManager
import kotlinx.coroutines.launch

@Composable
fun WatchSideloadScreen(
    wearableManager: PhoneWearableManager,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val adbManager = remember { WirelessAdbManager(context) }
    val apkSourceManager = remember { WatchApkSourceManager(context) }

    DisposableEffect(adbManager) {
        onDispose {
            adbManager.disconnect()
        }
    }

    val connectionState by adbManager.connectionState.collectAsState()
    val watchDetails by adbManager.watchDetails.collectAsState()
    val consoleLogs by adbManager.consoleLogs.collectAsState()
    val lastError by adbManager.lastError.collectAsState()

    var watchIp by remember {
        val configured = wearableManager.getConfiguredAospWatchIp()
        mutableStateOf(if (configured.isNotEmpty()) configured else "")
    }
    var watchPort by remember { mutableStateOf("") }

    var pairingPort by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var isPairing by remember { mutableStateOf(false) }
    var pairingSuccess by remember { mutableStateOf(false) }
    var pairingError by remember { mutableStateOf<String?>(null) }
    var isPairingExpanded by remember { mutableStateOf(false) }

    var selectedApk by remember { mutableStateOf<WatchApkInfo?>(apkSourceManager.getCachedApk()) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }

    var grantPermissions by remember { mutableStateOf(true) }
    var reinstallKeepData by remember { mutableStateOf(true) }
    var autoLaunch by remember { mutableStateOf(true) }

    var isScanningLan by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }

    // SAF Document Picker for picking APK from local storage
    val pickApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                adbManager.log("Importing selected APK from phone storage...")
                val result = apkSourceManager.importApkFromUri(uri)
                result.onSuccess { info ->
                    selectedApk = info
                    adbManager.log("Selected APK: ${info.displayName} (${info.sizeFormatted})")
                }.onFailure { err ->
                    adbManager.log("Failed to import APK: ${err.message}")
                }
            }
        }
    }

    // Auto-detect cached or asset APK if available on startup
    LaunchedEffect(Unit) {
        if (selectedApk == null) {
            val cached = apkSourceManager.getCachedApk()
            if (cached != null) {
                selectedApk = cached
                adbManager.log("Detected cached watch APK: ${cached.displayName}")
            } else {
                val assetApk = apkSourceManager.getEmbeddedAssetApk()
                if (assetApk != null) {
                    selectedApk = assetApk
                    adbManager.log("Detected pre-packaged APK in assets: ${assetApk.displayName}")
                }
            }
        }
    }

    LaunchedEffect(connectionState) {
        if ((connectionState == SideloadConnectionState.CONNECTED || connectionState == SideloadConnectionState.SUCCESS) && selectedApk == null) {
            val cached = apkSourceManager.getCachedApk()
            if (cached != null) {
                selectedApk = cached
                adbManager.log("Loaded cached watch APK: ${cached.displayName}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DigiWearPalette.CyberBg)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (onClose != null) {
            OutlinedButton(
                onClick = onClose,
                border = BorderStroke(1.dp, DigiWearPalette.DigiviceCyan),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DigiWearPalette.DigiviceCyan),
                shape = RoundedCornerShape(6.dp)
            ) {
                DigiVectorIcon(type = DigiIconType.ARROW_LEFT, size = 14.dp, tint = DigiWearPalette.DigiviceCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text("BACK TO SETTINGS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        // --- 1. Header & Terminal Info ---
        Card(
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            border = BorderStroke(1.dp, DigiWearPalette.CardBorder),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "WIRELESS ADB // SIDELOAD",
                        color = DigiWearPalette.DigiviceCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Install & Update DigiWear on Watch (No PC Required)",
                        color = Color(0xFF90A4AE),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // RSA Fingerprint Display
                Surface(
                    color = Color(0xFF0F151F),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "PHONE RSA-2048 ADB FINGERPRINT:",
                            color = Color(0xFF78909C),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = adbManager.getPublicKeyFingerprint(),
                            color = Color(0xFF80CBC4),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Help & Setup Guide Button at bottom of card
                OutlinedButton(
                    onClick = { showGuideDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DigiWearPalette.CrestGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DigiWearPalette.CrestGold)
                ) {
                    DigiVectorIcon(type = DigiIconType.HELP, size = 14.dp, tint = DigiWearPalette.CrestGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HOW TO CONNECT (HELP ?)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = DigiWearPalette.CrestGold
                    )
                }
            }
        }

        // --- 2. Step 1: Smartwatch Connection Panel ---
        Card(
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            border = BorderStroke(1.dp, DigiWearPalette.CardBorder),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. TARGET SMARTWATCH",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Connection Badge
                    val (badgeText, badgeBg, badgeFg) = when (connectionState) {
                        SideloadConnectionState.IDLE -> Triple("DISCONNECTED", Color(0xFF263238), Color(0xFF90A4AE))
                        SideloadConnectionState.CONNECTING -> Triple("CONNECTING...", Color(0xFF3E2723), Color(0xFFFFB74D))
                        SideloadConnectionState.AUTHENTICATING -> Triple("CONNECTING...", Color(0xFF0D47A1), Color(0xFF40C4FF))
                        SideloadConnectionState.CONNECTED -> Triple("CONNECTED", Color(0xFF1B5E20), Color(0xFF69F0AE))
                        SideloadConnectionState.TRANSFERRING -> Triple("STREAMING...", Color(0xFF1B5E20), Color(0xFF69F0AE))
                        SideloadConnectionState.INSTALLING -> Triple("INSTALLING...", Color(0xFF1B5E20), Color(0xFF69F0AE))
                        SideloadConnectionState.GRANTING_PERMISSIONS -> Triple("PERMISSIONS...", Color(0xFF1B5E20), Color(0xFF69F0AE))
                        SideloadConnectionState.SUCCESS -> Triple("SUCCESS", Color(0xFF1B5E20), Color(0xFF69F0AE))
                        SideloadConnectionState.ERROR -> Triple("ERROR", Color(0xFFB71C1C), Color(0xFFFF8A80))
                    }

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, badgeFg)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeFg,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (connectionState == SideloadConnectionState.IDLE || connectionState == SideloadConnectionState.ERROR) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFF261D12),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DigiVectorIcon(type = DigiIconType.ATTENTION, tint = Color(0xFFFFB74D), size = 16.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "NO WATCH CONNECTED OVER ADB",
                                    color = Color(0xFFFFB74D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "To sideload DigiWear, enable 'Wireless Debugging' (or 'Debug over Wi-Fi') on your smartwatch in Developer Options, ensure both devices are on the same Wi-Fi / Hotspot, and tap CONNECT below.",
                                color = Color.LightGray,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = watchIp,
                        onValueChange = { input ->
                            val trimmed = input.trim()
                            if (trimmed.contains(":")) {
                                val parts = trimmed.split(":")
                                watchIp = parts[0].trim()
                                val p = parts.getOrNull(1)?.filter { it.isDigit() } ?: ""
                                if (p.isNotEmpty()) watchPort = p
                            } else {
                                watchIp = trimmed
                            }
                        },
                        label = { Text("Watch IP Address", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        placeholder = { Text("192.168.1.50", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DigiWearPalette.DigiviceCyan,
                            unfocusedBorderColor = DigiWearPalette.CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = watchPort,
                        onValueChange = { input ->
                            val trimmed = input.trim()
                            if (trimmed.contains(":")) {
                                val parts = trimmed.split(":")
                                val ip = parts[0].trim()
                                if (ip.isNotEmpty()) watchIp = ip
                                watchPort = parts.getOrNull(1)?.filter { it.isDigit() } ?: ""
                            } else {
                                watchPort = trimmed.filter { it.isDigit() }
                            }
                        },
                        label = { Text("Port", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        placeholder = { Text("e.g. 41235", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.width(105.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DigiWearPalette.DigiviceCyan,
                            unfocusedBorderColor = DigiWearPalette.CardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons: Connect, Auto-Detect LAN, Disconnect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val port = watchPort.toIntOrNull() ?: 0
                            wearableManager.setConfiguredAospWatchIp(watchIp)
                            scope.launch {
                                if (port > 0) {
                                    adbManager.connect(watchIp, port)
                                } else {
                                    val ok = adbManager.autoDiscoverAndConnect(context, watchIp)
                                    if (ok && adbManager.activePort > 0) {
                                        watchPort = adbManager.activePort.toString()
                                    }
                                }
                            }
                        },
                        enabled = watchIp.isNotBlank() && (connectionState == SideloadConnectionState.IDLE || connectionState == SideloadConnectionState.ERROR),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DigiWearPalette.DigiviceCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CONNECT", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isScanningLan = true
                                adbManager.log("Scanning local Wi-Fi / Hotspot for smartwatch...")
                                val discovered = wearableManager.discoverAospWatchOnLan(600)
                                if (!discovered.isNullOrEmpty()) {
                                    watchIp = discovered
                                    adbManager.log("Discovered Watch on LAN: $discovered")
                                } else {
                                    adbManager.log("LAN discovery probe did not find watch. Please enter IP manually.")
                                }
                                isScanningLan = false
                            }
                        },
                        enabled = !isScanningLan,
                        border = BorderStroke(1.dp, Color(0xFF546E7A)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB0BEC5)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.SEARCH_LENS, size = 14.dp, tint = Color(0xFFB0BEC5))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isScanningLan) "SCANNING..." else "AUTO-DETECT", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    if (connectionState == SideloadConnectionState.CONNECTED || connectionState == SideloadConnectionState.SUCCESS) {
                        OutlinedButton(
                            onClick = { adbManager.disconnect() },
                            border = BorderStroke(1.dp, Color(0xFFE57373)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("DISCONNECT", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Dedicated Wear OS 3/4/5 Pairing Card ---
                Surface(
                    color = Color(0xFF0F1722),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (pairingSuccess) Color(0xFF00E676) else if (isPairingExpanded) DigiWearPalette.DigiviceCyan else Color(0xFF2A3A4D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPairingExpanded = !isPairingExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                DigiVectorIcon(
                                    type = DigiIconType.KEYPAD,
                                    tint = if (pairingSuccess) Color(0xFF00E676) else DigiWearPalette.DigiviceCyan,
                                    size = 16.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "PAIR & CONNECT WITH 6-DIGIT CODE (WEAR OS 3/4/5)",
                                        color = if (pairingSuccess) Color(0xFF69F0AE) else Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Galaxy Watch 4+, Pixel Watch 1/2/3, OnePlus Watch 2",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Surface(
                                color = if (pairingSuccess) Color(0xFF1B5E20) else if (isPairingExpanded) Color(0xFF003737) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (pairingSuccess) Color(0xFF69F0AE) else if (isPairingExpanded) DigiWearPalette.DigiviceCyan else Color(0xFF64748B)
                                )
                            ) {
                                Text(
                                    text = if (connectionState == SideloadConnectionState.CONNECTED) "CONNECTED" else if (pairingSuccess) "PAIRED!" else if (isPairingExpanded) "COLLAPSE" else "PAIR DEVICE",
                                    color = if (pairingSuccess) Color(0xFF69F0AE) else if (isPairingExpanded) DigiWearPalette.DigiviceCyan else Color(0xFFB0BEC5),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = isPairingExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Surface(
                                    color = Color(0xFF060B12),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1E2F44)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "On your watch: Settings > Developer options > Wireless debugging > 'Pair new device'.",
                                            color = Color(0xFF80CBC4),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "IMPORTANT: Keep the watch screen awake while pairing (Android kills the pairing port if the screen dims). Enter the IP, Pairing Port, and 6-Digit PIN shown on the watch below:",
                                            color = Color(0xFFB0BEC5),
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = watchIp,
                                        onValueChange = { input ->
                                            val trimmed = input.trim()
                                            if (trimmed.contains(":")) {
                                                val parts = trimmed.split(":")
                                                watchIp = parts[0].trim()
                                                val p = parts.getOrNull(1)?.filter { it.isDigit() } ?: ""
                                                if (p.isNotEmpty()) pairingPort = p
                                            } else {
                                                watchIp = trimmed
                                            }
                                            pairingSuccess = false
                                            pairingError = null
                                        },
                                        label = { Text("Watch IP Address", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                        placeholder = { Text("192.168.0.42", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DigiWearPalette.DigiviceCyan,
                                            unfocusedBorderColor = DigiWearPalette.CardBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = pairingPort,
                                        onValueChange = { input ->
                                            val trimmed = input.trim()
                                            if (trimmed.contains(":")) {
                                                val parts = trimmed.split(":")
                                                val ip = parts[0].trim()
                                                if (ip.isNotEmpty()) watchIp = ip
                                                pairingPort = parts.getOrNull(1)?.filter { it.isDigit() } ?: ""
                                            } else {
                                                pairingPort = trimmed.filter { ch -> ch.isDigit() }
                                            }
                                            pairingSuccess = false
                                            pairingError = null
                                        },
                                        label = { Text("Pairing Port (from watch popup)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                        placeholder = { Text("e.g. 38841", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                        modifier = Modifier.weight(0.48f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DigiWearPalette.DigiviceCyan,
                                            unfocusedBorderColor = DigiWearPalette.CardBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    OutlinedTextField(
                                        value = pairingCode,
                                        onValueChange = {
                                            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                                pairingCode = it
                                                pairingSuccess = false
                                                pairingError = null
                                            }
                                        },
                                        label = { Text("6-Digit PIN", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                        placeholder = { Text("173601", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                        modifier = Modifier.weight(0.52f),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = 2.sp
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DigiWearPalette.DigiviceCyan,
                                            unfocusedBorderColor = DigiWearPalette.CardBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                }

                                if (watchIp.isNotBlank() && pairingPort.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = Color(0xFF0A192F),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DigiVectorIcon(type = DigiIconType.WATCH, size = 12.dp, tint = DigiWearPalette.DigiviceCyan)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Pairing Target: $watchIp:$pairingPort",
                                                color = DigiWearPalette.DigiviceCyan,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (watchIp.isBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚠ Enter Watch IP Address (e.g. 192.168.0.42) before pairing",
                                        color = Color(0xFFFFB74D),
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val pPort = pairingPort.toIntOrNull() ?: 0
                                        isPairing = true
                                        pairingError = null
                                        pairingSuccess = false
                                        scope.launch {
                                            adbManager.log("Initiating Wear OS pairing with $watchIp:$pPort...")
                                            val success = adbManager.pair(watchIp, pPort, pairingCode)
                                            if (success) {
                                                pairingSuccess = true
                                                wearableManager.setConfiguredAospWatchIp(watchIp)
                                                adbManager.log("✓ Pairing approved! DigiWear-Companion is trusted on watch.")

                                                val opPort = watchPort.toIntOrNull() ?: 0
                                                var connected = false
                                                if (opPort > 0 && opPort != pPort) {
                                                    connected = adbManager.connectTlsOnly(watchIp, opPort)
                                                }
                                                if (!connected) {
                                                    connected = adbManager.autoDiscoverAndConnect(context, watchIp, skipPort = pPort)
                                                }
                                                if (connected) {
                                                    val ap = adbManager.activePort
                                                    if (ap > 0) watchPort = ap.toString()
                                                    adbManager.log("✓ Smartwatch paired & connected! Initiating sideload...")

                                                    // Auto-sideload if APK is ready
                                                    var apkToInstall = selectedApk
                                                    if (apkToInstall == null || apkToInstall?.sourceType != ApkSourceType.PHONE_STORAGE) {
                                                        adbManager.log("Downloading latest watch APK from GitHub release_bin...")
                                                        apkSourceManager.downloadReleaseApk().onSuccess { dl ->
                                                            apkToInstall = dl
                                                            adbManager.log("Download complete: ${dl.displayName} ${dl.versionLabel}")
                                                        }
                                                        if (apkToInstall == null) {
                                                            apkToInstall = apkSourceManager.getCachedApk() ?: apkSourceManager.getEmbeddedAssetApk()
                                                        }
                                                        if (apkToInstall != null) selectedApk = apkToInstall
                                                    }
                                                    if (apkToInstall != null) {
                                                        adbManager.sideloadApk(
                                                            apkFile = apkToInstall.file,
                                                            grantPermissions = grantPermissions,
                                                            reinstallKeepData = reinstallKeepData,
                                                            autoLaunch = autoLaunch
                                                        )
                                                    } else {
                                                        adbManager.log("Ready to install! Tap 'CONNECT, DOWNLOAD & SIDELOAD' below.")
                                                    }
                                                } else {
                                                    adbManager.log("✓ Paired successfully! You can now tap 'CONNECT & SIDELOAD' below.")
                                                }
                                            } else {
                                                pairingError = adbManager.lastError.value ?: "Pairing failed"
                                            }
                                            isPairing = false
                                        }
                                    },
                                    enabled = !isPairing && watchIp.isNotBlank() && pairingPort.isNotBlank() && pairingCode.length == 6,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (pairingSuccess) Color(0xFF00E676) else DigiWearPalette.DigiviceCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isPairing) {
                                        Text("PAIRING & SIDELOADING...", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    } else if (connectionState == SideloadConnectionState.INSTALLING) {
                                        Text("INSTALLING TO WATCH...", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    } else if (connectionState == SideloadConnectionState.TRANSFERRING) {
                                        Text("STREAMING APK TO WATCH...", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    } else if (connectionState == SideloadConnectionState.SUCCESS) {
                                        Text("✓ INSTALLED & READY", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    } else if (connectionState == SideloadConnectionState.CONNECTED) {
                                        Text("PAIRED & CONNECTED", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    } else {
                                        Text("PAIR & SIDELOAD TO WATCH", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    }
                                }

                                if (pairingSuccess) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (connectionState == SideloadConnectionState.SUCCESS) {
                                            "✓ Installation complete! DigiWear is now running on your watch."
                                        } else if (connectionState == SideloadConnectionState.CONNECTED) {
                                            "✓ Watch connected! Ready to sideload below."
                                        } else {
                                            "✓ Watch paired! Connecting..."
                                        },
                                        color = Color(0xFF69F0AE),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (pairingError != null && !pairingSuccess) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = Color(0xFF260808),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, Color(0xFF5C1D1D)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "▲ $pairingError",
                                            color = Color(0xFFFF5252),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(6.dp),
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Connected Watch Info Box
                AnimatedVisibility(visible = watchDetails != null) {
                    val details = watchDetails
                    if (details != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFF101923),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF26A69A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        DigiVectorIcon(type = DigiIconType.WATCH, tint = Color(0xFF26A69A), size = 16.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${details.model} (${details.manufacturer})",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (details.batteryPercent.isNotEmpty()) {
                                        Text(
                                            text = details.batteryPercent,
                                            color = Color(0xFF80CBC4),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Android ${details.androidVersion} (API ${details.sdkVersion}) ${details.wearPlatform}",
                                    color = Color(0xFF90A4AE),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "DIGIWEAR: ",
                                        color = Color(0xFF78909C),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (details.isDigiWearInstalled) {
                                        Text(
                                            text = "INSTALLED (${details.installedVersionName})",
                                            color = Color(0xFF00E676),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    } else {
                                        Text(
                                            text = "NOT INSTALLED",
                                            color = Color(0xFFFFB74D),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Step 2: Watch APK Source Selection ---
        Card(
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            border = BorderStroke(1.dp, DigiWearPalette.CardBorder),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "2. WATCH APK SOURCE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickApkLauncher.launch("application/vnd.android.package-archive") },
                        border = BorderStroke(1.dp, DigiWearPalette.DigiviceCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DigiWearPalette.DigiviceCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        DigiVectorIcon(type = DigiIconType.VAULT_SAFE, size = 14.dp, tint = DigiWearPalette.DigiviceCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SELECT APK", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isDownloading = true
                                downloadProgress = 0f
                                downloadStatusText = "Connecting to GitHub Releases..."
                                adbManager.log("Starting download of DigiWear watch release APK...")
                                val dlResult = apkSourceManager.downloadReleaseApk { downloaded, total ->
                                    if (total > 0) {
                                        downloadProgress = downloaded.toFloat() / total.toFloat()
                                        val mbDown = downloaded / (1024.0 * 1024.0)
                                        val mbTotal = total / (1024.0 * 1024.0)
                                        downloadStatusText = String.format("%.1f / %.1f MB (%.0f%%)", mbDown, mbTotal, downloadProgress * 100f)
                                    }
                                }
                                dlResult.onSuccess { info ->
                                    selectedApk = info
                                    downloadStatusText = "Download complete!"
                                    adbManager.log("Download complete: ${info.displayName} (${info.sizeFormatted})")
                                }.onFailure { err ->
                                    downloadStatusText = "Failed: ${err.message}"
                                    adbManager.log("Download failed: ${err.message}")
                                }
                                isDownloading = false
                            }
                        },
                        enabled = !isDownloading,
                        border = BorderStroke(1.dp, DigiWearPalette.CrestGold),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DigiWearPalette.CrestGold),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        DigiVectorIcon(type = DigiIconType.COMM_ANTENNA, size = 14.dp, tint = DigiWearPalette.CrestGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isDownloading) "DOWNLOADING..." else "DOWNLOAD LATEST", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                // Download Progress Indicator
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = DigiWearPalette.CrestGold,
                        trackColor = Color(0xFF263238)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = downloadStatusText,
                        color = DigiWearPalette.CrestGold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Selected APK Info Banner
                val apk = selectedApk
                if (apk != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFF101923),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E676)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DigiVectorIcon(type = DigiIconType.CHECK, tint = Color(0xFF00E676), size = 16.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = apk.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${apk.versionLabel.ifEmpty { "version unknown" }} • ${apk.sizeFormatted} • ${apk.sourceType.label}",
                                        color = Color(0xFF80CBC4),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Step 3: Installation Options ---
        Card(
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            border = BorderStroke(1.dp, DigiWearPalette.CardBorder),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "3. INSTALLATION OPTIONS",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))

                val checkboxColors = CheckboxDefaults.colors(
                    checkedColor = DigiWearPalette.DigiviceCyan,
                    checkmarkColor = Color.Black,
                    uncheckedColor = Color(0xFF546E7A)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = grantPermissions, onCheckedChange = { grantPermissions = it }, colors = checkboxColors)
                    Text("Auto-grant runtime permissions (-g: Step, Sensor, Notifications)", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reinstallKeepData, onCheckedChange = { reinstallKeepData = it }, colors = checkboxColors)
                    Text("Reinstall and keep existing pet data (-r -d)", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoLaunch, onCheckedChange = { autoLaunch = it }, colors = checkboxColors)
                    Text("Launch DigiWear on watch immediately after install", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // --- 5. Step 4: Sideload Action Button ---
        val isConnectedToWatch = connectionState == SideloadConnectionState.CONNECTED || connectionState == SideloadConnectionState.SUCCESS
        val isBusy = connectionState == SideloadConnectionState.TRANSFERRING ||
            connectionState == SideloadConnectionState.INSTALLING ||
            connectionState == SideloadConnectionState.GRANTING_PERMISSIONS
        val canSideload = watchIp.isNotBlank() && !isBusy && !isDownloading

        Button(
            onClick = {
                scope.launch {
                    // Step 1: Connect to watch if not already connected
                    if (!isConnectedToWatch) {
                        val opPort = watchPort.toIntOrNull() ?: 0
                        var connected = false
                        if (opPort > 0) {
                            adbManager.log("Connecting directly to $watchIp:$opPort...")
                            connected = adbManager.connect(watchIp, opPort)
                        }
                        if (!connected) {
                            adbManager.log("Attempting mDNS discovery on $watchIp...")
                            connected = adbManager.autoDiscoverAndConnect(context, watchIp, skipPort = pairingPort.toIntOrNull() ?: 0)
                            if (connected && adbManager.activePort > 0) {
                                watchPort = adbManager.activePort.toString()
                            }
                        }
                        if (!connected && !adbManager.isConnected()) {
                            adbManager.log("Could not establish connection to $watchIp. Please verify the IP and Port displayed on your watch screen.")
                            return@launch
                        }
                    }

                    // Step 2: Download APK if not cached
                    var apkToInstall = selectedApk
                    // Always refresh from GitHub release_bin unless the user explicitly picked a file from phone storage
                    if (apkToInstall == null || apkToInstall?.sourceType != ApkSourceType.PHONE_STORAGE) {
                        isDownloading = true
                        downloadProgress = 0f
                        downloadStatusText = "Downloading latest release APK from GitHub..."
                        adbManager.log("Downloading DigiWear watch release APK from GitHub...")
                        val dlResult = apkSourceManager.downloadReleaseApk { downloaded, total ->
                            if (total > 0) {
                                downloadProgress = downloaded.toFloat() / total.toFloat()
                                val mbDown = downloaded / (1024.0 * 1024.0)
                                val mbTotal = total / (1024.0 * 1024.0)
                                downloadStatusText = String.format(java.util.Locale.US, "%.1f / %.1f MB (%.0f%%)", mbDown, mbTotal, downloadProgress * 100f)
                            }
                        }
                        dlResult.onSuccess { info ->
                            selectedApk = info
                            apkToInstall = info
                            downloadStatusText = "Download complete!"
                            adbManager.log("Download complete: ${info.displayName} (${info.sizeFormatted})")
                        }.onFailure { err ->
                            downloadStatusText = "Download failed: ${err.message}"
                            adbManager.log("Download failed: ${err.message} (using cached APK if available)")
                        }
                        isDownloading = false
                    }

                    // Step 3: Stream and install on watch
                    val finalApk = apkToInstall
                    if (finalApk != null) {
                        val installedName = adbManager.watchDetails.value?.installedVersionName.orEmpty()
                        adbManager.log("Installing APK ${finalApk.versionLabel.ifEmpty { "(version unknown)" }}; watch currently has ${installedName.ifEmpty { "none" }}")
                        adbManager.sideloadApk(
                            apkFile = finalApk.file,
                            grantPermissions = grantPermissions,
                            reinstallKeepData = reinstallKeepData,
                            autoLaunch = autoLaunch
                        )
                    }
                }
            },
            enabled = canSideload,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E676),
                disabledContainerColor = Color(0xFF1E2E28),
                contentColor = Color.Black,
                disabledContentColor = Color(0xFF455A64)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (!isBusy && !isDownloading) {
                    DigiVectorIcon(type = DigiIconType.BOLT, size = 16.dp, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = when {
                        isBusy -> "SIDELOADING IN PROGRESS..."
                        isDownloading -> "DOWNLOADING RELEASE APK..."
                        !isConnectedToWatch -> if (selectedApk == null) "CONNECT, DOWNLOAD & SIDELOAD" else "CONNECT & SIDELOAD TO WATCH"
                        selectedApk == null -> "DOWNLOAD & SIDELOAD DIGIWEAR"
                        else -> "SIDELOAD DIGIWEAR TO WATCH"
                    },
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        if (!isConnectedToWatch) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (watchIp.isBlank()) "▲ Enter Watch IP in Step 1 to enable sideloading" else "▲ Tapping above will automatically connect to $watchIp and install DigiWear",
                color = Color(0xFF80CBC4),
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        if (isBusy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFF00E676),
                trackColor = Color(0xFF1B2C24)
            )
        }

        // --- 6. Live ADB Console Logs Window ---
        var isTerminalExpanded by remember { mutableStateOf(false) }
        val clipboardManager = LocalClipboardManager.current
        var copiedLogs by remember { mutableStateOf(false) }
        LaunchedEffect(copiedLogs) {
            if (copiedLogs) {
                kotlinx.coroutines.delay(2000)
                copiedLogs = false
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF080C12)),
            border = BorderStroke(1.dp, Color(0xFF1E2C3D)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DigiVectorIcon(type = DigiIconType.COMM_LINK, tint = Color(0xFF00E676), size = 14.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE D-TERMINAL LOGS",
                            color = Color(0xFF00E676),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                if (consoleLogs.isNotEmpty()) {
                                    val full = consoleLogs.joinToString("\n")
                                    clipboardManager.setText(AnnotatedString(full))
                                    copiedLogs = true
                                }
                            }
                        ) {
                            Text(
                                text = if (copiedLogs) "✓ COPIED!" else "COPY",
                                color = if (copiedLogs) Color(0xFF00E676) else DigiWearPalette.DigiviceCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(onClick = { isTerminalExpanded = !isTerminalExpanded }) {
                            Text(
                                text = if (isTerminalExpanded) "COLLAPSE" else "EXPAND",
                                color = Color(0xFFB0BEC5),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        TextButton(onClick = { adbManager.clearLogs() }) {
                            Text("CLEAR", color = Color(0xFF78909C), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val listState = rememberLazyListState()
                LaunchedEffect(consoleLogs.size) {
                    if (consoleLogs.isNotEmpty()) {
                        listState.animateScrollToItem(consoleLogs.size - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isTerminalExpanded) 300.dp else 130.dp)
                        .background(Color(0xFF05080D))
                        .padding(8.dp)
                ) {
                    if (consoleLogs.isEmpty()) {
                        Text(
                            text = "Terminal ready. Connect to smartwatch to view real-time ADB transmission logs.",
                            color = Color(0xFF455A64),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        LazyColumn(state = listState) {
                            itemsIndexed(consoleLogs, key = { index, _ -> index }) { _, logEntry ->
                                Text(
                                    text = logEntry,
                                    color = if (logEntry.contains("SUCCESS", true) || logEntry.contains("OK", true)) {
                                        Color(0xFF00E676)
                                    } else if (logEntry.contains("ERROR", true) || logEntry.contains("FAILED", true)) {
                                        Color(0xFFFF5252)
                                    } else if (logEntry.contains("WARNING", true) || logEntry.contains("CHECK", true)) {
                                        Color(0xFFFFD700)
                                    } else {
                                        Color(0xFF80CBC4)
                                    },
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 7. Quick Watch Controls & Diagnostics ---
        if (connectionState == SideloadConnectionState.CONNECTED || connectionState == SideloadConnectionState.SUCCESS) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                border = BorderStroke(1.dp, DigiWearPalette.CardBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "QUICK WATCH DIAGNOSTICS & CONTROLS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { scope.launch { adbManager.refreshWatchStatus() } },
                            border = BorderStroke(1.dp, Color(0xFF546E7A)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB0BEC5)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            DigiVectorIcon(type = DigiIconType.SCAN, size = 12.dp, tint = Color(0xFFB0BEC5))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("STATUS", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = { scope.launch { adbManager.launchOnWatch() } },
                            border = BorderStroke(1.dp, Color(0xFF00E676)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            DigiVectorIcon(type = DigiIconType.BOLT, size = 12.dp, tint = Color(0xFF00E676))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LAUNCH", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = { scope.launch { adbManager.uninstallFromWatch() } },
                            border = BorderStroke(1.dp, Color(0xFFE57373)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            DigiVectorIcon(type = DigiIconType.CLOSE, size = 12.dp, tint = Color(0xFFE57373))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("UNINSTALL", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Bottom spacing so that diagnostics & controls are never clipped by navigation bars
        Spacer(modifier = Modifier.height(72.dp))
    }

    // --- Wireless Debugging Setup Guide Dialog ---
    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            containerColor = DigiWearPalette.CardSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.border(BorderStroke(1.dp, DigiWearPalette.CardBorder), RoundedCornerShape(12.dp)),
            confirmButton = {
                TextButton(onClick = { showGuideDialog = false }) {
                    Text("GOT IT", color = DigiWearPalette.DigiviceCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text("HOW TO ENABLE WIRELESS DEBUGGING", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. WEAR OS 3 / 4 / 5 (Pixel Watch, Galaxy Watch 4/5/6/7, OnePlus, Xiaomi):",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• On your watch: Settings -> System -> About -> Versions -> tap 'Build Number' 7 times rapidly until Developer Options unlock.\n" +
                            "• Settings -> Developer Options -> turn ON 'ADB Debugging' and 'Wireless Debugging'.\n" +
                            "• Connect Watch and Phone to the SAME Wi-Fi network (or Phone Hotspot).\n" +
                            "• STEP 1 (PAIR & CONNECT): Tap 'Wireless Debugging' -> tap 'Pair new device'. Note the 6-digit Wi-Fi pairing code, IP, and pairing port (e.g. 192.168.1.50:38912).\n" +
                            "• In this app, expand 'PAIR & CONNECT', enter the IP, Pairing Port, and 6-Digit PIN, then tap 'PAIR & CONNECT WITH WATCH'.\n" +
                            "• The app automatically pairs, connects to the watch session, and ungreys the sideload button!\n" +
                            "• STEP 2 (SIDELOAD): Tap 'SIDELOAD DIGIWEAR' below to install or update the app on your watch.",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "2. STANDALONE AOSP / LEGACY ANDROID WATCH (Kospet, Lokmat, Lemfo, Zeblaze):",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Open Settings -> About Watch -> tap 'Build Number' 7 times.\n" +
                            "• Go to Developer Options -> turn ON 'USB Debugging' or 'Wireless Debugging'.\n" +
                            "• Connect watch to Wi-Fi (or your Phone's Wi-Fi Hotspot).\n" +
                            "• Leave Port as 5555 (default unencrypted ADB) and tap 'CONNECT'.\n" +
                            "• On the watch screen prompt, tap 'Always Allow'.",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            textContentColor = Color.White,
            titleContentColor = DigiWearPalette.DigiviceCyan
        )
    }
}
