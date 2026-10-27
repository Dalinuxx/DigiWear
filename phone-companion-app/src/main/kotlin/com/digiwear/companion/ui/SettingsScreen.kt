package com.digiwear.companion.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import com.digiwear.companion.BuildConfig
import com.digiwear.companion.data.PhoneDatabase
import com.digiwear.companion.data.SyncedPetEntity
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.render.SpriteCanvasRenderer
import com.digiwear.companion.render.WallpaperProcessor
import com.digiwear.core.engine.PetSimulationEngine
import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.CustomThemeConfig
import com.digiwear.core.model.LcdPattern
import com.digiwear.core.model.SoundpackProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Dedicated Settings and Diagnostics screen for DigiWear.
 * Purposefully isolates all watch hardware synchronization, AOSP LAN bridges,
 * Wireless ADB Sideloading, and full database JSON backups into a central control room.
 */
@Composable
fun SettingsScreen(
    db: PhoneDatabase,
    wearableManager: PhoneWearableManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val buzzerManager = remember { com.digiwear.companion.sound.BuzzerManager(context) }

    androidx.compose.runtime.DisposableEffect(buzzerManager) {
        onDispose {
            buzzerManager.release()
        }
    }

    // Connected Smartwatch Detection State
    var connectedWatchNodeCount by remember { mutableIntStateOf(0) }
    var hasCheckedConnection by remember { mutableStateOf(false) }
    var showSideloadDialog by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    // Watch Settings State (100% parity with Watch SettingsScreen)
    var selectedTheme by remember { mutableStateOf(BackgroundTheme.CLASSIC_VPET_LCD) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(false) }
    var soundpack by remember { mutableStateOf(buzzerManager.soundpack) }
    var threeButtonMode by remember { mutableStateOf(true) }
    var buttonPlacement by remember { mutableStateOf("RIGHT") }
    var pendulumShakeMode by remember { mutableStateOf(false) }
    var nfcBridgeMode by remember { mutableStateOf("NFC_DUEL") }
    var displayShapeOverride by remember { mutableStateOf("AUTO") }
    var dailyStepGoal by remember { mutableIntStateOf(5000) }
    var contrastLevel by remember { mutableStateOf("MEDIUM") }
    var sleepSchedule by remember { mutableStateOf("21:00_07:00") }
    var ambientStyle by remember { mutableStateOf("MINIMAL_DOT") }
    var hazardAlertsEnabled by remember { mutableStateOf(true) }
    var clockFormat24h by remember { mutableStateOf(false) }
    var lifespanCanonicalEnabled by remember { mutableStateOf(true) }
    var combatInjuriesEnabled by remember { mutableStateOf(true) }
    var maskUndiscoveredSpecies by remember { mutableStateOf(wearableManager.getMaskUndiscoveredSpecies()) }
    var aospWatchIp by remember { mutableStateOf("") }
    var isAutoDetectingAosp by remember { mutableStateOf(false) }

    // Custom Theme & Wallpaper Builder State
    var customPrimaryHex by remember { mutableStateOf(0xFF8B956DL) }
    var customGridHex by remember { mutableStateOf(0xFF9CA67EL) }
    var customPattern by remember { mutableStateOf(LcdPattern.DOT_MATRIX) }
    var customWallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var customThemeTab by remember { mutableIntStateOf(0) } // 0 = Matrix Builder, 1 = Photo Wallpaper

    // Custom Sleep Schedule Picker State
    var showSleepSchedulePicker by remember { mutableStateOf(false) }
    var customBedHour by remember { mutableIntStateOf(22) }
    var customBedMinute by remember { mutableIntStateOf(30) }
    var customWakeHour by remember { mutableIntStateOf(7) }
    var customWakeMinute by remember { mutableIntStateOf(0) }

    // Photo Wallpaper Gallery Picker Launcher
    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val processed = WallpaperProcessor.processWallpaper(context, uri)
                if (processed != null) {
                    WallpaperProcessor.saveToInternalStorage(context, processed)
                    customWallpaperBitmap = processed
                    selectedTheme = BackgroundTheme.CUSTOM_WALLPAPER
                    customThemeTab = 1

                    val pngBytes = WallpaperProcessor.compressToPngBytes(processed)
                    val sentAsset = wearableManager.sendWallpaperToWatch(pngBytes)
                    val sentSettings = wearableManager.sendSettingsToWatch(
                        themeId = CustomThemeConfig.WALLPAPER_THEME_ID,
                        vibrationEnabled = vibrationEnabled,
                        threeButtonMode = threeButtonMode,
                        buttonPlacement = buttonPlacement,
                        pendulumShakeMode = pendulumShakeMode,
                        soundEnabled = soundEnabled,
                        nfcBridgeMode = nfcBridgeMode,
                        displayShapeOverride = displayShapeOverride,
                        dailyStepGoal = dailyStepGoal,
                        contrastLevel = contrastLevel,
                        sleepSchedule = sleepSchedule,
                        ambientStyle = ambientStyle,
                        hazardAlertsEnabled = hazardAlertsEnabled,
                        clockFormat24h = clockFormat24h,
                        lifespanCanonicalEnabled = lifespanCanonicalEnabled,
                        combatInjuriesEnabled = combatInjuriesEnabled,
                        soundpack = soundpack.id,
                        maskUndiscoveredSpecies = maskUndiscoveredSpecies
                    )
                    syncStatus = if (sentAsset || sentSettings) "Wallpaper imported & beamed to Smartwatch!" else "Wallpaper saved locally on Phone."
                } else {
                    Toast.makeText(context, "Failed to decode wallpaper image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Backup & Restore State
    var showBackupRestoreModal by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    fun applyWatchSettings(watchSettings: Map<String, String>) {
        watchSettings["theme"]?.let { themeId ->
            val parsed = CustomThemeConfig.parse(themeId)
            if (parsed != null) {
                selectedTheme = BackgroundTheme.CUSTOM_MATRIX
                customPrimaryHex = parsed.primaryColorHex
                customGridHex = parsed.gridColorHex
                customPattern = parsed.pattern
            } else if (themeId.equals(CustomThemeConfig.WALLPAPER_THEME_ID, ignoreCase = true)) {
                selectedTheme = BackgroundTheme.CUSTOM_WALLPAPER
                customThemeTab = 1
            } else {
                selectedTheme = BackgroundTheme.fromId(themeId)
            }
        }
        watchSettings["vibration"]?.let { vibrationEnabled = it.toBoolean() }
        watchSettings["vpet_3button_mode"]?.let { threeButtonMode = it.toBoolean() }
        watchSettings["vpet_button_placement"]?.let { buttonPlacement = it }
        watchSettings["pendulum_shake_mode"]?.let { pendulumShakeMode = it.toBoolean() }
        watchSettings["sound_enabled"]?.let { soundEnabled = it.toBoolean(); buzzerManager.isEnabled = it.toBoolean() }
        watchSettings["soundpack"]?.let {
            val sp = SoundpackProfile.fromId(it)
            soundpack = sp
            buzzerManager.soundpack = sp
        }
        watchSettings["nfc_bridge_mode"]?.let { nfcBridgeMode = it }
        watchSettings["display_shape_override"]?.let { displayShapeOverride = it }
        watchSettings["daily_step_goal"]?.toIntOrNull()?.let { dailyStepGoal = it }
        watchSettings["lcd_contrast_level"]?.let { contrastLevel = it }
        watchSettings["sleep_schedule"]?.let { sched ->
            sleepSchedule = sched
            val parts = sched.split("_")
            val bedParts = parts.getOrNull(0)?.split(":")
            val wakeParts = parts.getOrNull(1)?.split(":")
            customBedHour = bedParts?.getOrNull(0)?.toIntOrNull() ?: customBedHour
            customBedMinute = bedParts?.getOrNull(1)?.toIntOrNull() ?: customBedMinute
            customWakeHour = wakeParts?.getOrNull(0)?.toIntOrNull() ?: customWakeHour
            customWakeMinute = wakeParts?.getOrNull(1)?.toIntOrNull() ?: customWakeMinute
        }
        watchSettings["ambient_display_style"]?.let { ambientStyle = it }
        watchSettings["hazard_alerts_enabled"]?.let { hazardAlertsEnabled = it.toBoolean() }
        watchSettings["clock_format_24h"]?.let { clockFormat24h = it.toBoolean() }
        watchSettings["lifespan_canonical_enabled"]?.let { lifespanCanonicalEnabled = it.toBoolean() }
        watchSettings["combat_injuries_enabled"]?.let { combatInjuriesEnabled = it.toBoolean() }
        watchSettings["mask_undiscovered_species"]?.let {
            val masked = it.toBoolean()
            maskUndiscoveredSpecies = masked
            wearableManager.setMaskUndiscoveredSpecies(masked)
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        customWallpaperBitmap = WallpaperProcessor.loadFromInternalStorage(context)
        val nodes = wearableManager.getConnectedWatchNodes()
        connectedWatchNodeCount = nodes.size
        hasCheckedConnection = true
        val watchSettings = wearableManager.requestSettingsFromWatch() ?: wearableManager.getCachedSettings()
        if (watchSettings != null) {
            applyWatchSettings(watchSettings)
        }
        aospWatchIp = wearableManager.getConfiguredAospWatchIp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DigiWearPalette.CyberBg)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DigiSectionHeader(
            tag = "SETTINGS // CONTROL ROOM",
            title = "Hardware Sync & Bridges",
            subtitle = "Wear OS & AOSP configuration, wireless sideloading & backups",
            accentColor = DigiWearPalette.CrestGold
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (BuildConfig.DEBUG) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14241E)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF00E676))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DigiVectorIcon(type = DigiIconType.SETTINGS_GEAR, size = 14.dp, tint = Color(0xFF00E676))
                    Column {
                        Text(
                            text = "DEBUG BUILD ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E676),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Developer Sprite Studio & 12-frame matrix enabled in DigiDex tab.",
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // --- 1. SMARTWATCH CONNECTION STATUS & DISCOVERY ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                if (connectedWatchNodeCount > 0) Color(0xFF00E676) else Color(0xFFFFB74D)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (connectedWatchNodeCount > 0) Color(0xFF00E676) else Color(0xFFFFB74D),
                                    CircleShape
                                )
                        )
                        Column {
                            Text(
                                text = if (connectedWatchNodeCount > 0) "SMARTWATCH LINKED" else "NO WATCH DETECTED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (connectedWatchNodeCount > 0) Color(0xFF00E676) else Color(0xFFFFB74D),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (connectedWatchNodeCount > 0)
                                    "$connectedWatchNodeCount Wear OS node(s) connected via DataLayer"
                                else
                                    "Watch app is not connected or Bluetooth is off",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                val nodes = wearableManager.getConnectedWatchNodes()
                                connectedWatchNodeCount = nodes.size
                                val watchSettings = wearableManager.requestSettingsFromWatch()
                                if (watchSettings != null) {
                                    applyWatchSettings(watchSettings)
                                }
                                syncStatus = "Refreshed: ${nodes.size} watch node(s) found"
                                isSyncing = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DigiWearPalette.DigiviceCyan)
                    ) {
                        DigiVectorIcon(type = DigiIconType.SCAN, size = 13.dp, tint = DigiWearPalette.DigiviceCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSyncing) "CHECKING..." else "REFRESH", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = DigiWearPalette.DigiviceCyan)
                    }
                }

                if (hasCheckedConnection && connectedWatchNodeCount == 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF261D12), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DigiVectorIcon(type = DigiIconType.WATCH, size = 14.dp, tint = Color(0xFFFFB74D))
                                Text(
                                    text = "PAIRING SUGGESTION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB74D),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Ensure your Wear OS or AOSP smartwatch is powered on and Bluetooth is active.\n" +
                                        "2. On Wear OS: Pair via the Galaxy Wearable or Pixel Watch app.\n" +
                                        "3. On Standalone AOSP watches: Connect phone and watch to the same Wi-Fi network and use the LAN bridge below.\n" +
                                        "4. Need to install the app on your watch? Tap 'WIRELESS ADB SIDELOAD' below!",
                                fontSize = 9.sp,
                                color = Color(0xFFFFE082),
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. WIRELESS ADB SIDELOAD WATCH APP (PRIMARY ACTION) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF142436)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00E5FF))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DigiVectorIcon(type = DigiIconType.BOLT, size = 16.dp, tint = Color(0xFF00E5FF))
                    Text(
                        text = "WIRELESS ADB SIDELOAD TO WATCH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Install or update DigiWear on your Wear OS or AOSP smartwatch straight from your phone over Wi-Fi — zero PC, USB cables, or terminal commands needed!",
                    fontSize = 9.5.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showSideloadDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    DigiVectorIcon(type = DigiIconType.BOLT, size = 15.dp, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OPEN WIRELESS ADB WATCH SIDELOADER",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. AOSP STANDARD WATCH WI-FI / HOTSPOT BRIDGE ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "▸ AOSP / FULL ANDROID WATCH WI-FI BRIDGE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF80CBC4),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Direct LAN socket bridge for standalone AOSP watches lacking Google Play Services (Kospet, Lokmat, Zeblaze, etc.)",
                    fontSize = 9.5.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = aospWatchIp,
                        onValueChange = { aospWatchIp = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("192.168.1.50", fontSize = 10.sp, color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DigiWearPalette.DigiviceCyan,
                            unfocusedBorderColor = Color(0xFF37474F),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        )
                    )

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isAutoDetectingAosp = true
                                val detectedIp = wearableManager.discoverAospWatchOnLan()
                                if (detectedIp != null) {
                                    aospWatchIp = detectedIp
                                    wearableManager.setConfiguredAospWatchIp(detectedIp)
                                    syncStatus = "Discovered AOSP Watch at $detectedIp!"
                                } else {
                                    syncStatus = "No AOSP Watch broadcast detected on LAN."
                                }
                                isAutoDetectingAosp = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF26A69A))
                    ) {
                        Text(
                            text = if (isAutoDetectingAosp) "SCAN..." else "AUTO-DETECT",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF26A69A),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            wearableManager.setConfiguredAospWatchIp(aospWatchIp)
                            syncStatus = "Saved AOSP Watch IP: $aospWatchIp"
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DigiWearPalette.DigiviceCyan)
                    ) {
                        Text("LINK", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. SMARTWATCH HARDWARE TOGGLES & DISPLAY ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "▸ SMARTWATCH HARDWARE CONTROLS & DISPLAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DigiWearPalette.DigiviceCyan,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))

                // LCD Theme Selector
                Text(text = "LCD BACKLIGHT THEME:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F), fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BackgroundTheme.entries.forEach { theme ->
                        val isSelected = selectedTheme == theme
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color(0xFF004D40) else DigiWearPalette.CardInner,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFF64FFDA) else Color(0xFF263238),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTheme = theme }
                                .padding(horizontal = 9.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val swatchColor = when (theme) {
                                    BackgroundTheme.CUSTOM_MATRIX -> Color(customPrimaryHex)
                                    BackgroundTheme.CUSTOM_WALLPAPER -> Color(0xFF00E5FF)
                                    else -> Color(theme.primaryColorHex)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(swatchColor, CircleShape)
                                        .border(0.75.dp, if (isSelected) Color(0xFF64FFDA) else Color(0xFF546E7A), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = theme.displayName,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFFB0BEC5),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- CUSTOM LCD THEME & WALLPAPER BUILDER CARD ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141E24)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CUSTOM LCD & WALLPAPER",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Matrix builder & photo wallpaper importer",
                                    fontSize = 8.5.sp,
                                    color = Color(0xFF90A4AE)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Mode tabs: [ MATRIX BUILDER ] | [ PHOTO WALLPAPER ]
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (customThemeTab == 0) Color(0xFF00695C) else DigiWearPalette.CardInner, RoundedCornerShape(6.dp))
                                    .border(if (customThemeTab == 0) 1.5.dp else 1.dp, if (customThemeTab == 0) Color(0xFF64FFDA) else Color(0xFF37474F), RoundedCornerShape(6.dp))
                                    .clickable { customThemeTab = 0 }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "MATRIX BUILDER",
                                    fontSize = 9.sp,
                                    fontWeight = if (customThemeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (customThemeTab == 0) Color.White else Color(0xFFB0BEC5),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (customThemeTab == 1) Color(0xFF283593) else DigiWearPalette.CardInner, RoundedCornerShape(6.dp))
                                    .border(if (customThemeTab == 1) 1.5.dp else 1.dp, if (customThemeTab == 1) Color(0xFF8C9EFF) else Color(0xFF37474F), RoundedCornerShape(6.dp))
                                    .clickable { customThemeTab = 1 }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "PHOTO WALLPAPER",
                                    fontSize = 9.sp,
                                    fontWeight = if (customThemeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (customThemeTab == 1) Color.White else Color(0xFFB0BEC5),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (customThemeTab == 0) {
                            // === 1. MATRIX BUILDER ===
                            Text(text = "BACKLIGHT COLOR:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F), fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    0xFF8B956DL to "Olive 1997",
                                    0xFFDDAA33L to "Amber",
                                    0xFF00E5FFL to "Cyber Cyan",
                                    0xFF00C853L to "Emerald",
                                    0xFFA8B39BL to "Silver GB",
                                    0xFF9C27B0L to "Violet",
                                    0xFFFF5252L to "Coral Red",
                                    0xFF80D8FFL to "Ice Blue",
                                    0xFF000000L to "OLED Black"
                                ).forEach { (colorHex, name) ->
                                    val isSel = customPrimaryHex == colorHex
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { customPrimaryHex = colorHex }
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Color(colorHex), RoundedCornerShape(6.dp))
                                                .border(if (isSel) 2.dp else 1.dp, if (isSel) Color.White else Color(0xFF37474F), RoundedCornerShape(6.dp))
                                        )
                                        Text(text = name, fontSize = 7.sp, color = if (isSel) Color.White else Color.Gray, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(text = "GRID / MATRIX COLOR:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80CBC4), fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    0xFF7A845CL to "Olive Shadow",
                                    0xFF37474FL to "Matrix Shadow",
                                    0xFF00E5FFL to "Neon Cyan",
                                    0xFF1A1A1AL to "Charcoal",
                                    0xFF6C227EL to "Synth Twilight",
                                    0xFFFFD54FL to "Gold Accent",
                                    0xFF000000L to "Black"
                                ).forEach { (colorHex, name) ->
                                    val isSel = customGridHex == colorHex
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { customGridHex = colorHex }
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Color(colorHex), RoundedCornerShape(6.dp))
                                                .border(if (isSel) 2.dp else 1.dp, if (isSel) Color.White else Color(0xFF37474F), RoundedCornerShape(6.dp))
                                        )
                                        Text(text = name, fontSize = 7.sp, color = if (isSel) Color.White else Color.Gray, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(text = "LCD PATTERN:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB39DDB), fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                LcdPattern.entries.forEach { pattern ->
                                    val isSel = customPattern == pattern
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSel) Color(0xFF4527A0) else DigiWearPalette.CardInner, RoundedCornerShape(6.dp))
                                            .border(if (isSel) 1.5.dp else 1.dp, if (isSel) Color(0xFFB39DDB) else Color(0xFF37474F), RoundedCornerShape(6.dp))
                                            .clickable { customPattern = pattern }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pattern.displayName,
                                            fontSize = 8.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else Color(0xFFB0BEC5),
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .border(1.5.dp, Color(0xFF64FFDA), RoundedCornerShape(12.dp))
                                        .drawWithCache {
                                            val prim = Color(customPrimaryHex)
                                            val sec = Color(CustomThemeConfig.deriveSecondaryColorHex(customPrimaryHex, customGridHex)).copy(alpha = 0.35f)
                                            val grid25 = Color(customGridHex).copy(alpha = 0.25f)
                                            val grid20 = Color(customGridHex).copy(alpha = 0.20f)
                                            val bezelH = Color(customGridHex).copy(alpha = 0.45f)

                                            val pts = if (customPattern == LcdPattern.DOT_MATRIX) {
                                                val list = ArrayList<Offset>(500)
                                                var x = 3.5f
                                                while (x < size.width) {
                                                    var y = 3.5f
                                                    while (y < size.height) {
                                                        list.add(Offset(x, y))
                                                        y += 7f
                                                    }
                                                    x += 7f
                                                }
                                                list
                                            } else emptyList()

                                            onDrawBehind {
                                                drawRect(color = prim)
                                                when (customPattern) {
                                                    LcdPattern.DOT_MATRIX -> {
                                                        drawPoints(points = pts, pointMode = PointMode.Points, color = grid25, strokeWidth = 1.5f)
                                                        drawRoundRect(color = bezelH, topLeft = Offset(3f, 3f), size = Size(size.width - 6f, size.height - 6f), cornerRadius = CornerRadius(10f, 10f), style = Stroke(width = 2f))
                                                    }
                                                    LcdPattern.GRID -> {
                                                        var x = 0f
                                                        while (x < size.width) {
                                                            drawLine(color = grid20, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                                                            x += 18f
                                                        }
                                                        var y = 0f
                                                        while (y < size.height) {
                                                            drawLine(color = grid20, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                                                            y += 18f
                                                        }
                                                    }
                                                    LcdPattern.SCANLINES -> {
                                                        var y = 0f
                                                        while (y < size.height) {
                                                            drawLine(color = sec, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                                                            y += 10f
                                                        }
                                                    }
                                                    LcdPattern.OLED -> {
                                                        drawRect(color = prim)
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    SpriteCanvasRenderer(speciesId = 1, size = 64.dp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val sec = CustomThemeConfig.deriveSecondaryColorHex(customPrimaryHex, customGridHex)
                                    val config = CustomThemeConfig(customPrimaryHex, sec, customGridHex, customPattern)
                                    selectedTheme = BackgroundTheme.CUSTOM_MATRIX
                                    scope.launch {
                                        val sent = wearableManager.sendSettingsToWatch(
                                            themeId = config.toThemeId(),
                                            vibrationEnabled = vibrationEnabled,
                                            threeButtonMode = threeButtonMode,
                                            buttonPlacement = buttonPlacement,
                                            pendulumShakeMode = pendulumShakeMode,
                                            soundEnabled = soundEnabled,
                                            nfcBridgeMode = nfcBridgeMode,
                                            displayShapeOverride = displayShapeOverride,
                                            dailyStepGoal = dailyStepGoal,
                                            contrastLevel = contrastLevel,
                                            sleepSchedule = sleepSchedule,
                                            ambientStyle = ambientStyle,
                                            hazardAlertsEnabled = hazardAlertsEnabled,
                                            clockFormat24h = clockFormat24h,
                                            lifespanCanonicalEnabled = lifespanCanonicalEnabled,
                                            combatInjuriesEnabled = combatInjuriesEnabled,
                                            soundpack = soundpack.id,
                                            maskUndiscoveredSpecies = maskUndiscoveredSpecies
                                        )
                                        syncStatus = if (sent) "Custom matrix theme beamed to Smartwatch!" else "Custom theme saved locally."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                            ) {
                                DigiVectorIcon(type = DigiIconType.BEAM_ANTENNA, size = 14.dp, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("APPLY & BEAM CUSTOM THEME", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }
                        } else {
                            // === 2. PHOTO WALLPAPER ===
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .border(1.5.dp, Color(0xFF8C9EFF), RoundedCornerShape(12.dp))
                                        .drawWithCache {
                                            val bmp = customWallpaperBitmap?.asImageBitmap()
                                            onDrawBehind {
                                                if (bmp != null) {
                                                    drawImage(image = bmp, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                                                    drawRoundRect(
                                                        color = Color(0x40000000),
                                                        topLeft = Offset(3f, 3f),
                                                        size = Size(size.width - 6f, size.height - 6f),
                                                        cornerRadius = CornerRadius(10f, 10f),
                                                        style = Stroke(width = 2f)
                                                    )
                                                } else {
                                                    drawRect(color = Color(0xFF1E2830))
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (customWallpaperBitmap != null) {
                                        SpriteCanvasRenderer(speciesId = 1, size = 64.dp)
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                            DigiVectorIcon(type = DigiIconType.CAMERA, size = 24.dp, tint = Color(0xFF80D8FF))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "NO PHOTO\nIMPORTED",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.LightGray,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { wallpaperLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE))
                                ) {
                                    Text("CHOOSE PHOTO", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                                }

                                if (customWallpaperBitmap != null) {
                                    Button(
                                        onClick = {
                                            selectedTheme = BackgroundTheme.CUSTOM_WALLPAPER
                                            scope.launch {
                                                val bytes = WallpaperProcessor.compressToPngBytes(customWallpaperBitmap!!)
                                                val sentAsset = wearableManager.sendWallpaperToWatch(bytes)
                                                val sentSettings = wearableManager.sendSettingsToWatch(
                                                    themeId = CustomThemeConfig.WALLPAPER_THEME_ID,
                                                    vibrationEnabled = vibrationEnabled,
                                                    threeButtonMode = threeButtonMode,
                                                    buttonPlacement = buttonPlacement,
                                                    pendulumShakeMode = pendulumShakeMode,
                                                    soundEnabled = soundEnabled,
                                                    nfcBridgeMode = nfcBridgeMode,
                                                    displayShapeOverride = displayShapeOverride,
                                                    dailyStepGoal = dailyStepGoal,
                                                    contrastLevel = contrastLevel,
                                                    sleepSchedule = sleepSchedule,
                                                    ambientStyle = ambientStyle,
                                                    hazardAlertsEnabled = hazardAlertsEnabled,
                                                    clockFormat24h = clockFormat24h,
                                                    lifespanCanonicalEnabled = lifespanCanonicalEnabled,
                                                    combatInjuriesEnabled = combatInjuriesEnabled,
                                                    soundpack = soundpack.id,
                                                    maskUndiscoveredSpecies = maskUndiscoveredSpecies
                                                )
                                                syncStatus = if (sentAsset || sentSettings) "Wallpaper beamed to Smartwatch!" else "Wallpaper saved locally."
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                                    ) {
                                        Text("BEAM TO WATCH", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Settings Switch Rows (With weight(1f) to guarantee clean label line wrapping)
                SettingSwitchRow(
                    title = "HAPTIC FEEDBACK",
                    subtitle = "Tactile wrist vibration for steps, sparring hits, and evolution calls",
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it }
                )

                SettingSwitchRow(
                    title = "CHIPTUNE PIEZO BUZZER",
                    subtitle = "Retro 8-bit square-wave audio for alerts, training prompts, and care beeps",
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        buzzerManager.isEnabled = it
                    }
                )

                // Authentic Soundpack Profile Selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "AUTHENTIC SOUNDPACK PROFILE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = soundpack.description,
                        fontSize = 9.sp,
                        color = DigiWearPalette.CrestGold,
                        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SoundpackProfile.entries.forEach { profile ->
                            val isSelected = soundpack == profile
                            OutlinedButton(
                                onClick = {
                                    soundpack = profile
                                    buzzerManager.soundpack = profile
                                    if (soundEnabled) buzzerManager.playButtonBeep()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) DigiWearPalette.DigiviceCyan else Color(0xFF37474F)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) Color(0xFF00363A) else Color(0xFF1A262C)
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = when (profile) {
                                            SoundpackProfile.ORIGINAL_1997 -> "1997 PIEZO"
                                            SoundpackProfile.DIGIVICE_1999 -> "1999 CHIMES"
                                            SoundpackProfile.CHIPTUNE_RETRO -> "8-BIT SYNTH"
                                        },
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) DigiWearPalette.DigiviceCyan else Color.White
                                    )
                                    Text(
                                        text = profile.subtitle,
                                        fontSize = 7.sp,
                                        color = if (isSelected) Color(0xFF80DEEA) else Color(0xFF90A4AE)
                                    )
                                }
                            }
                        }
                    }
                }

                SettingSwitchRow(
                    title = "3-BUTTON LCD CONTROLS",
                    subtitle = if (threeButtonMode) {
                        "Virtual [A] [B] [C] side buttons enabled on watch face (1997 classic brick layout)"
                    } else {
                        "Full-screen direct touch gestures enabled (Modern tap-anywhere layout)"
                    },
                    checked = threeButtonMode,
                    onCheckedChange = { threeButtonMode = it }
                )

                // Dedicated Mode Selector for Training Method
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "TRAINING METHOD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (pendulumShakeMode) {
                            "Pendulum Shake: Shake watch 5 to 8 times using wrist accelerometer (Digimon Pendulum)."
                        } else {
                            "Timing QTE: Tap action button when cursor hits the sweet spot zone (Digital Monster)."
                        },
                        fontSize = 9.sp,
                        color = if (pendulumShakeMode) Color(0xFFFFD54F) else Color(0xFF80D8FF),
                        lineHeight = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (!pendulumShakeMode) Color(0xFF004D40) else DigiWearPalette.CardInner,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    if (!pendulumShakeMode) 1.5.dp else 1.dp,
                                    if (!pendulumShakeMode) Color(0xFF64FFDA) else Color(0xFF37474F),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { pendulumShakeMode = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TIMING QTE",
                                fontSize = 9.5.sp,
                                fontWeight = if (!pendulumShakeMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!pendulumShakeMode) Color.White else Color(0xFFB0BEC5),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (pendulumShakeMode) Color(0xFF6A1B9A) else DigiWearPalette.CardInner,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    if (pendulumShakeMode) 1.5.dp else 1.dp,
                                    if (pendulumShakeMode) Color(0xFFE040FB) else Color(0xFF37474F),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { pendulumShakeMode = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PENDULUM SHAKE",
                                fontSize = 9.5.sp,
                                fontWeight = if (pendulumShakeMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (pendulumShakeMode) Color.White else Color(0xFFB0BEC5),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Button Rail Placement
                Text(text = "BUTTON RAIL PLACEMENT:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80CBC4), fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "RIGHT" to "RIGHT (OG)",
                        "LEFT" to "LEFT",
                        "OFF" to "OFF (TOUCH)"
                    ).forEach { (placeId, label) ->
                        val active = if (placeId == "OFF") !threeButtonMode || buttonPlacement == "OFF" else (threeButtonMode && buttonPlacement == placeId)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (active) Color(0xFF00695C) else DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                .border(if (active) 1.5.dp else 1.dp, if (active) Color(0xFF64FFDA) else Color(0xFF37474F), RoundedCornerShape(8.dp))
                                .clickable {
                                    buttonPlacement = placeId
                                    threeButtonMode = (placeId != "OFF")
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) Color.White else Color(0xFFB0BEC5),
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SettingSwitchRow(
                    title = "24-HOUR CLOCK FORMAT",
                    subtitle = if (clockFormat24h) "24H military format on watch face (e.g. 15:45)" else "12H standard format with AM/PM (e.g. 3:45 PM)",
                    checked = clockFormat24h,
                    onCheckedChange = { clockFormat24h = it }
                )

                SettingSwitchRow(
                    title = "HAZARD ALERTS",
                    subtitle = "Warn when active sector has adverse conditions (glitch fog, radiation, blizzard)",
                    checked = hazardAlertsEnabled,
                    onCheckedChange = { hazardAlertsEnabled = it }
                )

                SettingSwitchRow(
                    title = "CANONICAL 1997 LIFESPAN",
                    subtitle = "Faithful 1997 death and traited egg inheritance vs modern immortal",
                    checked = lifespanCanonicalEnabled,
                    onCheckedChange = { lifespanCanonicalEnabled = it }
                )

                SettingSwitchRow(
                    title = "BATTLE INJURIES",
                    subtitle = "25% defeat injury risk requiring first aid vs safe sparring",
                    checked = combatInjuriesEnabled,
                    onCheckedChange = { combatInjuriesEnabled = it }
                )

                SettingSwitchRow(
                    title = "MASK UNDISCOVERED DIGIMON",
                    subtitle = "Show question marks & silhouettes for undiscovered species in DigiDex and evolution lines. Turn off to reveal all.",
                    checked = maskUndiscoveredSpecies,
                    onCheckedChange = {
                        maskUndiscoveredSpecies = it
                        wearableManager.setMaskUndiscoveredSpecies(it)
                        // Auto-beam (like the sleep picker / theme builder) so a later settings reload
                        // from the watch or cache can't silently revert this toggle.
                        scope.launch {
                            val activeThemeId = when (selectedTheme) {
                                BackgroundTheme.CUSTOM_MATRIX -> {
                                    val sec = CustomThemeConfig.deriveSecondaryColorHex(customPrimaryHex, customGridHex)
                                    CustomThemeConfig(customPrimaryHex, sec, customGridHex, customPattern).toThemeId()
                                }
                                BackgroundTheme.CUSTOM_WALLPAPER -> CustomThemeConfig.WALLPAPER_THEME_ID
                                else -> selectedTheme.id
                            }
                            wearableManager.sendSettingsToWatch(
                                themeId = activeThemeId,
                                vibrationEnabled = vibrationEnabled,
                                threeButtonMode = threeButtonMode,
                                buttonPlacement = buttonPlacement,
                                pendulumShakeMode = pendulumShakeMode,
                                soundEnabled = soundEnabled,
                                nfcBridgeMode = nfcBridgeMode,
                                displayShapeOverride = displayShapeOverride,
                                dailyStepGoal = dailyStepGoal,
                                contrastLevel = contrastLevel,
                                sleepSchedule = sleepSchedule,
                                ambientStyle = ambientStyle,
                                hazardAlertsEnabled = hazardAlertsEnabled,
                                clockFormat24h = clockFormat24h,
                                lifespanCanonicalEnabled = lifespanCanonicalEnabled,
                                combatInjuriesEnabled = combatInjuriesEnabled,
                                soundpack = soundpack.id,
                                maskUndiscoveredSpecies = it
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sleep Schedule
                Text(text = "SLEEP SCHEDULE:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB39DDB), fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                val sleepPresets = listOf(
                    "20:00_07:00" to "20:00\n07:00",
                    "21:00_07:00" to "21:00\n07:00",
                    "22:00_08:00" to "22:00\n08:00",
                    "23:00_08:00" to "23:00\n08:00"
                )
                val isCustomSleepActive = sleepPresets.none { it.first == sleepSchedule }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    sleepPresets.forEach { (schedId, label) ->
                        val active = sleepSchedule == schedId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (active) Color(0xFF4527A0) else DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                .border(if (active) 1.5.dp else 1.dp, if (active) Color(0xFFB39DDB) else Color(0xFF37474F), RoundedCornerShape(8.dp))
                                .clickable { sleepSchedule = schedId }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 8.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) Color.White else Color(0xFFB0BEC5),
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                lineHeight = 10.sp
                            )
                        }
                    }

                    // Custom Sleep Schedule Chip
                    val customChipText = if (isCustomSleepActive && sleepSchedule.contains("_")) {
                        val parts = sleepSchedule.split("_")
                        "CUSTOM\n${parts[0]}\n${parts.getOrElse(1) { "" }}"
                    } else {
                        "CUSTOM\nSET"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.15f)
                            .background(if (isCustomSleepActive) Color(0xFF512DA8) else DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                            .border(if (isCustomSleepActive) 1.5.dp else 1.dp, if (isCustomSleepActive) Color(0xFFD1C4E9) else Color(0xFF37474F), RoundedCornerShape(8.dp))
                            .clickable {
                                if (isCustomSleepActive && sleepSchedule.contains("_")) {
                                    val parts = sleepSchedule.split("_")
                                    val bedParts = parts.getOrNull(0)?.split(":")
                                    val wakeParts = parts.getOrNull(1)?.split(":")
                                    customBedHour = bedParts?.getOrNull(0)?.toIntOrNull() ?: customBedHour
                                    customBedMinute = bedParts?.getOrNull(1)?.toIntOrNull() ?: customBedMinute
                                    customWakeHour = wakeParts?.getOrNull(0)?.toIntOrNull() ?: customWakeHour
                                    customWakeMinute = wakeParts?.getOrNull(1)?.toIntOrNull() ?: customWakeMinute
                                }
                                showSleepSchedulePicker = true
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customChipText,
                            fontSize = 7.5.sp,
                            fontWeight = if (isCustomSleepActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCustomSleepActive) Color.White else Color(0xFFB0BEC5),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            lineHeight = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ambient Display Style
                Text(text = "AMBIENT DISPLAY STYLE:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784), fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "MINIMAL_DOT" to "MINIMAL DOT",
                        "OUTLINE" to "OUTLINE WIRE",
                        "DIMMED_LCD" to "DIMMED RETRO"
                    ).forEach { (styleId, label) ->
                        val active = ambientStyle == styleId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (active) Color(0xFF2E7D32) else DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                .border(if (active) 1.5.dp else 1.dp, if (active) Color(0xFF81C784) else Color(0xFF37474F), RoundedCornerShape(8.dp))
                                .clickable { ambientStyle = styleId }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 8.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) Color.White else Color(0xFFB0BEC5),
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // LCD Contrast Level
                Text(text = "LCD CONTRAST LEVEL:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D), fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("LOW" to "Soft Matrix", "MEDIUM" to "Standard (Def)", "HIGH" to "Deep Crisp").forEach { (levelId, label) ->
                        val active = contrastLevel == levelId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (active) Color(0xFFE65100) else DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                .border(if (active) 1.5.dp else 1.dp, if (active) Color(0xFFFFB74D) else Color(0xFF37474F), RoundedCornerShape(8.dp))
                                .clickable { contrastLevel = levelId }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) Color.White else Color(0xFFB0BEC5),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Daily Step Target
                Text(text = "DAILY STEP TARGET:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80D8FF), fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3000, 5000, 7500, 10000).forEach { goal ->
                        val active = dailyStepGoal == goal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (active) Color(0xFF00838F) else DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                .border(if (active) 1.5.dp else 1.dp, if (active) Color(0xFF4DD0E1) else Color(0xFF37474F), RoundedCornerShape(8.dp))
                                .clickable { dailyStepGoal = goal }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${goal / 1000}K",
                                fontSize = 9.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) Color.White else Color(0xFFB0BEC5),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val activeThemeId = when (selectedTheme) {
                                BackgroundTheme.CUSTOM_MATRIX -> {
                                    val sec = CustomThemeConfig.deriveSecondaryColorHex(customPrimaryHex, customGridHex)
                                    CustomThemeConfig(customPrimaryHex, sec, customGridHex, customPattern).toThemeId()
                                }
                                BackgroundTheme.CUSTOM_WALLPAPER -> CustomThemeConfig.WALLPAPER_THEME_ID
                                else -> selectedTheme.id
                            }
                            val sent = wearableManager.sendSettingsToWatch(
                                themeId = activeThemeId,
                                vibrationEnabled = vibrationEnabled,
                                threeButtonMode = threeButtonMode,
                                buttonPlacement = buttonPlacement,
                                pendulumShakeMode = pendulumShakeMode,
                                soundEnabled = soundEnabled,
                                nfcBridgeMode = nfcBridgeMode,
                                displayShapeOverride = displayShapeOverride,
                                dailyStepGoal = dailyStepGoal,
                                contrastLevel = contrastLevel,
                                sleepSchedule = sleepSchedule,
                                ambientStyle = ambientStyle,
                                hazardAlertsEnabled = hazardAlertsEnabled,
                                clockFormat24h = clockFormat24h,
                                lifespanCanonicalEnabled = lifespanCanonicalEnabled,
                                combatInjuriesEnabled = combatInjuriesEnabled,
                                soundpack = soundpack.id,
                                maskUndiscoveredSpecies = maskUndiscoveredSpecies
                            )
                            syncStatus = if (sent) "Successfully beamed settings to Smartwatch!" else "Settings saved locally on Phone."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DigiWearPalette.DigiviceCyan)
                ) {
                    DigiVectorIcon(type = DigiIconType.BEAM_ANTENNA, size = 15.dp, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("BEAM SETTINGS TO SMARTWATCH", fontSize = 10.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 5. DATA, STORAGE & BACKUP MANAGEMENT ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFCE93D8))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "▸ DATA, STORAGE & DATABASE BACKUPS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCE93D8),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Export complete partner vaults, DigiDex discoveries, and adventure quest state to JSON",
                    fontSize = 9.5.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showBackupRestoreModal = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCE93D8))
                    ) {
                        DigiVectorIcon(type = DigiIconType.VAULT_SAFE, size = 14.dp, tint = Color(0xFFCE93D8))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BACKUP / JSON", fontSize = 10.sp, color = Color(0xFFCE93D8), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF5252))
                    ) {
                        DigiVectorIcon(type = DigiIconType.CLOSE, size = 14.dp, tint = Color(0xFFFF5252))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WIPE DATA", fontSize = 10.sp, color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        syncStatus?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "▸ $it", fontSize = 10.sp, color = Color(0xFF00E676), fontFamily = FontFamily.Monospace)
        }
    }

    // Wireless ADB Sideload Dialog
    if (showSideloadDialog) {
        Dialog(
            onDismissRequest = { showSideloadDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            WatchSideloadScreen(
                wearableManager = wearableManager,
                onClose = { showSideloadDialog = false }
            )
        }
    }

    // Backup & Restore Full Screen Modal
    if (showBackupRestoreModal) {
        Dialog(
            onDismissRequest = { showBackupRestoreModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DigiWearPalette.CyberBg)
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DATABASE BACKUP & RESTORE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedButton(
                            onClick = { showBackupRestoreModal = false },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF546E7A))
                        ) {
                            Text("CLOSE", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        BackupRestoreScreen(db, wearableManager)
                    }
                }
            }
        }
    }

    // Wipe / Clean Data Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = DigiWearPalette.CardSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.border(BorderStroke(1.dp, DigiWearPalette.CardBorder), RoundedCornerShape(12.dp)),
            title = {
                Text(
                    text = "Reset Companion Database?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Text(
                    text = "This will clear all local companion snapshots, cryo-vault partners, and discovered species. Smartwatch data is not affected.",
                    fontSize = 11.sp,
                    color = Color(0xFFB0BEC5),
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            // Clear local database on IO thread
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                db.clearAllTables()
                            }
                            showResetConfirmDialog = false
                            syncStatus = "Companion database reset to clean state."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("RESET DATA", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("CANCEL", color = Color(0xFFB0BEC5), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        )
    }

    // --- Custom Sleep Schedule Time Picker Dialog ---
    if (showSleepSchedulePicker) {
        AlertDialog(
            onDismissRequest = { showSleepSchedulePicker = false },
            containerColor = DigiWearPalette.CardSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.border(BorderStroke(1.dp, Color(0xFFB39DDB)), RoundedCornerShape(12.dp)),
            title = {
                Text(
                    text = "CUSTOM SLEEP SCHEDULE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Configure exact bed and wake hours. Your virtual pet rests and halts discipline decay during this window.",
                        fontSize = 9.5.sp,
                        color = Color(0xFFB0BEC5),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Bedtime Row
                    Text(
                        text = "BEDTIME (SLEEP START):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { customBedHour = (customBedHour + 23) % 24 },
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                        ) {
                            Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "%02d", customBedHour),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { customBedHour = (customBedHour + 1) % 24 },
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                        ) {
                            Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Text(":", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = { customBedMinute = (customBedMinute + 55) % 60 },
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                        ) {
                            Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "%02d", customBedMinute),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { customBedMinute = (customBedMinute + 5) % 60 },
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                        ) {
                            Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Wake-up Time Row
                    Text(
                        text = "WAKE-UP TIME (PET WAKES UP):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80D8FF),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { customWakeHour = (customWakeHour + 23) % 24 },
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                        ) {
                            Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "%02d", customWakeHour),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { customWakeHour = (customWakeHour + 1) % 24 },
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                        ) {
                            Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Text(":", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = { customWakeMinute = (customWakeMinute + 55) % 60 },
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                        ) {
                            Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "%02d", customWakeMinute),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { customWakeMinute = (customWakeMinute + 5) % 60 },
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                        ) {
                            Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val sleepDurationMinutes = PetSimulationEngine.calculateSleepDurationMinutes(customBedHour, customBedMinute, customWakeHour, customWakeMinute)
                    val isDurationValid = PetSimulationEngine.isValidSleepDuration(customBedHour, customBedMinute, customWakeHour, customWakeMinute)
                    val durationHours = sleepDurationMinutes / 60
                    val durationRemainingMinutes = sleepDurationMinutes % 60

                    // Duration & Minimum Sleep Validation Card (OG V-Pet Canon)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isDurationValid) Color(0xFF00E676) else Color(0xFFFF5252),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDurationValid) Color(0xFF1B2E24) else Color(0xFF2E1B1B)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DURATION: ${durationHours}h ${durationRemainingMinutes}m",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDurationValid) Color(0xFF00E676) else Color(0xFFFF5252),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isDurationValid) "VALID (≥ 6H)" else "MIN 6H REQUIRED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDurationValid) Color(0xFFB9F6CA) else Color(0xFFFF8A80),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (!isDurationValid) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Authentic virtual pets require at least 6 hours of daily rest to prevent abuse and maintain health.",
                                    fontSize = 8.5.sp,
                                    color = Color(0xFFFFCDD2),
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        customWakeHour = (customBedHour + 8) % 24
                                        customWakeMinute = customBedMinute
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF64FFDA)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64FFDA))
                                ) {
                                    Text(
                                        text = "AUTO-ALIGN TO 8H SLEEP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val isDurationValid = PetSimulationEngine.isValidSleepDuration(customBedHour, customBedMinute, customWakeHour, customWakeMinute)
                Button(
                    onClick = {
                        if (!isDurationValid) return@Button
                        val formatted = String.format(Locale.US, "%02d:%02d_%02d:%02d", customBedHour, customBedMinute, customWakeHour, customWakeMinute)
                        sleepSchedule = formatted
                        showSleepSchedulePicker = false
                        scope.launch {
                            val activeThemeId = when (selectedTheme) {
                                BackgroundTheme.CUSTOM_MATRIX -> {
                                    val sec = CustomThemeConfig.deriveSecondaryColorHex(customPrimaryHex, customGridHex)
                                    CustomThemeConfig(customPrimaryHex, sec, customGridHex, customPattern).toThemeId()
                                }
                                BackgroundTheme.CUSTOM_WALLPAPER -> CustomThemeConfig.WALLPAPER_THEME_ID
                                else -> selectedTheme.id
                            }
                            val sent = wearableManager.sendSettingsToWatch(
                                themeId = activeThemeId,
                                vibrationEnabled = vibrationEnabled,
                                threeButtonMode = threeButtonMode,
                                buttonPlacement = buttonPlacement,
                                pendulumShakeMode = pendulumShakeMode,
                                soundEnabled = soundEnabled,
                                nfcBridgeMode = nfcBridgeMode,
                                displayShapeOverride = displayShapeOverride,
                                dailyStepGoal = dailyStepGoal,
                                contrastLevel = contrastLevel,
                                sleepSchedule = formatted,
                                ambientStyle = ambientStyle,
                                hazardAlertsEnabled = hazardAlertsEnabled,
                                clockFormat24h = clockFormat24h,
                                lifespanCanonicalEnabled = lifespanCanonicalEnabled,
                                combatInjuriesEnabled = combatInjuriesEnabled,
                                soundpack = soundpack.id,
                                maskUndiscoveredSpecies = maskUndiscoveredSpecies
                            )
                            syncStatus = if (sent) "Custom sleep schedule $formatted beamed to Smartwatch!" else "Saved schedule locally."
                        }
                    },
                    enabled = isDurationValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7E57C2),
                        disabledContainerColor = Color(0xFF37474F)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isDurationValid) "APPLY & BEAM" else "MIN 6H REQUIRED",
                        color = if (isDurationValid) Color.White else Color(0xFF90A4AE),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSleepSchedulePicker = false }) {
                    Text("CANCEL", color = Color(0xFFB0BEC5), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = Color.Gray,
                lineHeight = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00897B),
                checkedBorderColor = Color(0xFF64FFDA),
                uncheckedThumbColor = Color(0xFF90A4AE),
                uncheckedTrackColor = Color(0xFF1B222D),
                uncheckedBorderColor = Color(0xFF37474F)
            )
        )
    }
}
