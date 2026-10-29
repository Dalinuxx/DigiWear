package com.digiwear.wear.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.SoundpackProfile
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.haptic.HapticManager
import com.digiwear.wear.input.HardwareInputManager
import com.digiwear.wear.input.VPetButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: PetRepository,
    hapticManager: HapticManager,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val displayConfig = WatchDisplayConfig.current()
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    val currentTheme by repository.themeFlow.collectAsState(initial = BackgroundTheme.CLASSIC_VPET_LCD)
    val customThemeConfig by repository.customThemeConfigFlow.collectAsState(initial = null)
    val isVibrationEnabled by repository.vibrationFlow.collectAsState(initial = true)
    val isThreeButtonMode by repository.threeButtonModeFlow.collectAsState(initial = true)
    val buttonPlacement by repository.buttonPlacementFlow.collectAsState(initial = "RIGHT")
    val isPendulumShakeMode by repository.pendulumShakeModeFlow.collectAsState(initial = false)
    val isSoundEnabled by repository.soundEnabledFlow.collectAsState(initial = false)
    val currentSoundpack by repository.soundpackFlow.collectAsState(initial = SoundpackProfile.ORIGINAL_1997)
    val nfcBridgeMode by repository.nfcBridgeModeFlow.collectAsState(initial = "NFC_DUEL")
    val displayShapeOverride by repository.displayShapeOverrideFlow.collectAsState(initial = "AUTO")
    val dailyStepGoal by repository.dailyStepGoalFlow.collectAsState(initial = 5000)
    val contrastLevel by repository.contrastLevelFlow.collectAsState(initial = "MEDIUM")
    val elementContrast by repository.elementContrastFlow.collectAsState(initial = "AUTO")
    val sleepSchedule by repository.sleepScheduleFlow.collectAsState(initial = "21:00_07:00")
    val customSleepSched by repository.customSleepScheduleFlow.collectAsState(initial = null)
    val ambientStyle by repository.ambientStyleFlow.collectAsState(initial = "MINIMAL_DOT")
    val hazardAlertsEnabled by repository.hazardAlertsFlow.collectAsState(initial = true)
    val clockFormat24h by repository.clockFormatFlow.collectAsState(initial = false)
    val isLifespanCanonical by repository.lifespanCanonicalEnabledFlow.collectAsState(initial = false)
    val combatInjuriesEnabled by repository.combatInjuriesEnabledFlow.collectAsState(initial = true)
    val maskUndiscoveredSpecies by repository.maskUndiscoveredSpeciesFlow.collectAsState(initial = true)

    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var showEggSelector by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = showEggSelector) {
        showEggSelector = false
    }

    LaunchedEffect(Unit) {
        HardwareInputManager.buttonEvents.collect { button ->
            if (button == VPetButton.C) {
                if (showEggSelector) {
                    showEggSelector = false
                } else {
                    onBack()
                }
            }
        }
    }

    val themes = BackgroundTheme.entries
    val contrastOptions = listOf(
        "LOW" to "Soft Matrix",
        "MEDIUM" to "Standard (Def)",
        "HIGH" to "Deep Crisp"
    )
    val elementContrastOptions = listOf(
        "AUTO" to "Auto-Adapt",
        "INVERT" to "Light / White",
        "RETRO" to "Dark Ink"
    )
    val stepGoalOptions = listOf(3000, 5000, 7500, 10000)
    val sleepScheduleOptions = listOf(
        "20:00_07:00" to "20:00–07:00 (Early)",
        "21:00_07:00" to "21:00–07:00 (Standard)",
        "22:00_08:00" to "22:00–08:00 (Night Owl)",
        "23:00_08:00" to "23:00–08:00 (Late Night)"
    )
    val ambientStyleOptions = listOf(
        "MINIMAL_DOT" to "Minimal Dot",
        "OUTLINE" to "Outline Wire",
        "DIMMED_LCD" to "Dimmed Retro"
    )
    val nfcModes = listOf(
        "NFC_DUEL" to "Tap-to-Duel (P2P)",
        "WILD_SCAN" to "Wild Tag Scanner",
        "ACOM_RELAY" to "Phone A-Com Relay"
    )
    val shapeModes = listOf("AUTO", "ROUND", "SQUARE")

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12141A)),
        state = listState,
        contentPadding = displayConfig.contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "DIGIWEAR SETTINGS",
                fontSize = displayConfig.headerFontSize,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64B5F6),
                fontFamily = FontFamily.Monospace
            )
        }

        // ==========================================
        // 0. Watch Face Replacement
        // ==========================================
        item {
            Button(
                onClick = {
                    hapticManager.playStepMilestone()
                    try {
                        val intent = android.content.Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(
                                android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                android.content.ComponentName(context, com.digiwear.wear.watchface.DigiWatchFaceService::class.java)
                            )
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val chooserIntent = android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(chooserIntent)
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                    .height(30.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00695C)),
                shape = RoundedCornerShape(15.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DigiVectorIcon(type = DigiIconType.SETTINGS, size = 10.dp, tint = Color(0xFFB9F6CA))
                    Text(
                        text = "SET AS ACTIVE WATCH FACE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }

        // ==========================================
        // 1. Shell & LCD Theme Selector
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SHELL & LCD THEME",
                fontSize = 8.5.sp,
                color = Color.LightGray,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val currentIdx = themes.indexOf(currentTheme)
                        val prevIdx = if (currentIdx > 0) currentIdx - 1 else themes.size - 1
                        repository.setTheme(themes[prevIdx])
                        hapticManager.playStepMilestone()
                    },
                    modifier = Modifier.size(32.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF263238))
                ) {
                    DigiVectorIcon(type = DigiIconType.ARROW_LEFT, size = 12.dp, tint = Color.White)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    val swatchColor = when {
                        currentTheme == BackgroundTheme.CUSTOM_MATRIX && customThemeConfig != null -> Color(customThemeConfig!!.primaryColorHex)
                        currentTheme == BackgroundTheme.CUSTOM_WALLPAPER -> Color(0xFF00E5FF)
                        else -> Color(currentTheme.primaryColorHex)
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp, 12.dp)
                            .background(swatchColor, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = currentTheme.displayName,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        val currentIdx = themes.indexOf(currentTheme)
                        val nextIdx = (currentIdx + 1) % themes.size
                        repository.setTheme(themes[nextIdx])
                        hapticManager.playStepMilestone()
                    },
                    modifier = Modifier.size(32.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF263238))
                ) {
                    DigiVectorIcon(type = DigiIconType.ARROW_RIGHT, size = 12.dp, tint = Color.White)
                }
            }
        }

        // ==========================================
        // 2. LCD Contrast Level
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            val currentContrast = contrastOptions.find { it.first == contrastLevel } ?: contrastOptions[1]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LCD CONTRAST LEVEL",
                        fontSize = 9.5.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentContrast.second,
                        fontSize = 8.sp,
                        color = Color(0xFFFFD54F)
                    )
                }
                Button(
                    onClick = {
                        val idx = contrastOptions.indexOfFirst { it.first == contrastLevel }.coerceAtLeast(0)
                        val next = contrastOptions[(idx + 1) % contrastOptions.size].first
                        repository.setContrastLevel(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier
                        .size(width = 54.dp, height = 30.dp)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F))
                ) {
                    Text(
                        text = contrastLevel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ==========================================
        // 2b. Invert / Element Contrast Mode (for Dark Wallpapers)
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            val currentElemContrast = elementContrastOptions.find { it.first == elementContrast } ?: elementContrastOptions[0]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ELEMENT CONTRAST",
                        fontSize = 9.5.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentElemContrast.second,
                        fontSize = 8.sp,
                        color = Color(0xFF69F0AE)
                    )
                }
                Button(
                    onClick = {
                        val idx = elementContrastOptions.indexOfFirst { it.first == elementContrast }.coerceAtLeast(0)
                        val next = elementContrastOptions[(idx + 1) % elementContrastOptions.size].first
                        repository.setElementContrast(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier
                        .size(width = 54.dp, height = 30.dp)
                        .border(1.5.dp, Color(0xFF69F0AE), CircleShape),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A2F))
                ) {
                    Text(
                        text = elementContrast,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ==========================================
        // 3. Haptic Feedback Toggle
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HAPTIC FEEDBACK",
                    fontSize = 9.5.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Button(
                    onClick = {
                        val next = !isVibrationEnabled
                        repository.setVibrationEnabled(next)
                        hapticManager.isEnabled = next
                        if (next) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier.size(width = 54.dp, height = 30.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isVibrationEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                ) {
                    Text(
                        text = if (isVibrationEnabled) "ON" else "OFF",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 4. Chiptune Buzzer Sound FX Toggle
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CHIPTUNE BUZZER",
                        fontSize = 9.5.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "8-Bit Piezo Beeps",
                        fontSize = 7.5.sp,
                        color = Color.LightGray
                    )
                }
                Button(
                    onClick = {
                        val next = !isSoundEnabled
                        repository.setSoundEnabled(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier.size(width = 54.dp, height = 30.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isSoundEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                ) {
                    Text(
                        text = if (isSoundEnabled) "ON" else "OFF",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 4b. Authentic Soundpack Profile Selector
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            val soundpacks = SoundpackProfile.entries
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SOUNDPACK PROFILE",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentSoundpack.displayName,
                        fontSize = 7.5.sp,
                        color = Color(0xFFFFD54F)
                    )
                }
                Button(
                    onClick = {
                        val idx = soundpacks.indexOf(currentSoundpack).coerceAtLeast(0)
                        val next = soundpacks[(idx + 1) % soundpacks.size]
                        repository.setSoundpack(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                        if (isSoundEnabled) {
                            hapticManager.buzzerManager?.let { bm ->
                                bm.soundpack = next
                                bm.playButtonBeep()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(width = 62.dp, height = 30.dp)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00695C))
                ) {
                    Text(
                        text = when (currentSoundpack) {
                            SoundpackProfile.ORIGINAL_1997 -> "1997"
                            SoundpackProfile.DIGIVICE_1999 -> "1999"
                            SoundpackProfile.CHIPTUNE_RETRO -> "CHIP"
                        },
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ==========================================
        // 5. Button Rail Position & Controls
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                    Text(
                        text = "ON-SCREEN BUTTONS",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = when (buttonPlacement) {
                            "RIGHT" -> "Right (A/B/C)"
                            "LEFT" -> "Left (A/B/C)"
                            else -> "Off (Touch & HW Buttons)"
                        },
                        fontSize = 7.5.sp,
                        color = Color.LightGray
                    )
                }
                Button(
                    onClick = {
                        val next = when (buttonPlacement) {
                            "RIGHT" -> "LEFT"
                            "LEFT" -> "OFF"
                            else -> "RIGHT"
                        }
                        repository.setButtonPlacement(next)
                        repository.setThreeButtonMode(next != "OFF")
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier.size(width = 58.dp, height = 30.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = when (buttonPlacement) {
                            "RIGHT" -> Color(0xFF00897B)
                            "LEFT" -> Color(0xFF1E88E5)
                            else -> Color(0xFF546E7A)
                        }
                    )
                ) {
                    Text(
                        text = buttonPlacement,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 6. Training Method Toggle (QTE vs Pendulum Shake)
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                    Text(
                        text = "TRAINING METHOD",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isPendulumShakeMode) "Pendulum: Shake wrist 5-8x" else "Timing QTE: Tap hit bar",
                        fontSize = 7.sp,
                        color = if (isPendulumShakeMode) Color(0xFFFFD54F) else Color(0xFF80D8FF)
                    )
                }
                Button(
                    onClick = {
                        val next = !isPendulumShakeMode
                        repository.setPendulumShakeMode(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier.size(width = 62.dp, height = 30.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isPendulumShakeMode) Color(0xFF6A1B9A) else Color(0xFF00695C)
                    )
                ) {
                    Text(
                        text = if (isPendulumShakeMode) "SHAKE" else "QTE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 7. Daily Step Target
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DAILY STEP TARGET",
                        fontSize = 9.5.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$dailyStepGoal Steps Goal",
                        fontSize = 8.sp,
                        color = Color(0xFF80D8FF)
                    )
                }
                Button(
                    onClick = {
                        val idx = stepGoalOptions.indexOf(dailyStepGoal).coerceAtLeast(0)
                        val nextGoal = stepGoalOptions[(idx + 1) % stepGoalOptions.size]
                        repository.setDailyStepGoal(nextGoal)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier
                        .size(width = 58.dp, height = 30.dp)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00838F))
                ) {
                    Text(
                        text = "${dailyStepGoal / 1000}K",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ==========================================
        // 8. Sleep Schedule
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            val presetSleep = sleepScheduleOptions.find { it.first == sleepSchedule }
            val currentSleepLabel = presetSleep?.second ?: run {
                if (sleepSchedule.contains("_")) {
                    val parts = sleepSchedule.split("_")
                    "Custom (${parts.getOrElse(0) { "" }}–${parts.getOrElse(1) { "" }})"
                } else {
                    "Custom ($sleepSchedule)"
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SLEEP SCHEDULE",
                        fontSize = 9.5.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentSleepLabel,
                        fontSize = 7.5.sp,
                        color = Color(0xFFB39DDB)
                    )
                }
                Button(
                    onClick = {
                        val allOptions = if (customSleepSched != null && !sleepScheduleOptions.any { it.first == customSleepSched }) {
                            sleepScheduleOptions.map { it.first } + customSleepSched!!
                        } else {
                            sleepScheduleOptions.map { it.first }
                        }
                        val idx = allOptions.indexOf(sleepSchedule)
                        val next = if (idx >= 0) {
                            allOptions[(idx + 1) % allOptions.size]
                        } else {
                            allOptions[0]
                        }
                        repository.setSleepSchedule(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier
                        .size(width = 58.dp, height = 30.dp)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4527A0))
                ) {
                    Text(
                        text = if (presetSleep != null) sleepSchedule.substringBefore("_") else "CUSTOM",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ==========================================
        // 9. Always-On Ambient Display Style
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            val currentAmb = ambientStyleOptions.find { it.first == ambientStyle } ?: ambientStyleOptions[0]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ALWAYS-ON AMBIENT",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentAmb.second,
                        fontSize = 7.5.sp,
                        color = Color(0xFF81C784)
                    )
                }
                Button(
                    onClick = {
                        val idx = ambientStyleOptions.indexOfFirst { it.first == ambientStyle }.coerceAtLeast(0)
                        val next = ambientStyleOptions[(idx + 1) % ambientStyleOptions.size].first
                        repository.setAmbientStyle(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier
                        .size(width = 58.dp, height = 30.dp)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                ) {
                    Text(
                        text = when (ambientStyle) {
                            "OUTLINE" -> "OUTL"
                            "DIMMED_LCD" -> "LCD"
                            else -> "DOT"
                        },
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ==========================================
        // 10. Clock Time Format (12H vs 24H)
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CLOCK TIME FORMAT",
                        fontSize = 9.5.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (clockFormat24h) "24-Hour (13:00)" else "12-Hour (1:00 PM)",
                        fontSize = 7.5.sp,
                        color = Color.LightGray
                    )
                }
                Button(
                    onClick = {
                        val next = !clockFormat24h
                        repository.setClockFormat24h(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier.size(width = 54.dp, height = 30.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F))
                ) {
                    Text(
                        text = if (clockFormat24h) "24H" else "12H",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 11. Environmental Hazard Notifications
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HAZARD NOTIFICATIONS",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "X-Program Fog & Miasma Alerts",
                        fontSize = 7.5.sp,
                        color = Color.LightGray
                    )
                }
                Button(
                    onClick = {
                        val next = !hazardAlertsEnabled
                        repository.setHazardAlertsEnabled(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier.size(width = 54.dp, height = 30.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (hazardAlertsEnabled) Color(0xFF00897B) else Color(0xFF78909C)
                    )
                ) {
                    Text(
                        text = if (hazardAlertsEnabled) "ON" else "OFF",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 12. Lifespan & Canonical Death / Traited Digitama
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LIFESPAN & DEATH",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isLifespanCanonical) "Canonical 1997 (Traited Egg on Death)" else "Immortal (Modern No-Perish)",
                        fontSize = 7.5.sp,
                        color = if (isLifespanCanonical) Color(0xFFFF8A80) else Color(0xFF80D8FF)
                    )
                }
                Button(
                    onClick = {
                        val next = !isLifespanCanonical
                        repository.setLifespanCanonicalEnabled(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier.size(width = 58.dp, height = 30.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isLifespanCanonical) Color(0xFFC62828) else Color(0xFF00695C)
                    )
                ) {
                    Text(
                        text = if (isLifespanCanonical) "CANON" else "IMMRT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 13. NFC Tap Bridge Mode Selector
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            val currentPair = nfcModes.find { it.first == nfcBridgeMode } ?: nfcModes.first()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NFC TAP BRIDGE MODE",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentPair.second,
                        fontSize = 7.5.sp,
                        color = Color(0xFF80D8FF)
                    )
                }
                Button(
                    onClick = {
                        val idx = nfcModes.indexOfFirst { it.first == nfcBridgeMode }.coerceAtLeast(0)
                        val nextMode = nfcModes[(idx + 1) % nfcModes.size].first
                        repository.setNfcBridgeMode(nextMode)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier
                        .size(width = 58.dp, height = 30.dp)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF0277BD))
                ) {
                    Text(
                        text = when (nfcBridgeMode) {
                            "WILD_SCAN" -> "SCAN"
                            "ACOM_RELAY" -> "A-COM"
                            else -> "DUEL"
                        },
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ==========================================
        // 14. Combat Battle Injuries (Canonical 25% Defeat Risk)
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BATTLE INJURIES",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (combatInjuriesEnabled) "Canonical 25% Defeat Risk (Needs Meds)" else "Disabled (Safe Sparring)",
                        fontSize = 7.5.sp,
                        color = if (combatInjuriesEnabled) Color(0xFFFF8A80) else Color(0xFF80D8FF)
                    )
                }
                Button(
                    onClick = {
                        val next = !combatInjuriesEnabled
                        repository.setCombatInjuriesEnabled(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier.size(width = 58.dp, height = 30.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (combatInjuriesEnabled) Color(0xFFC62828) else Color(0xFF00695C)
                    )
                ) {
                    Text(
                        text = if (combatInjuriesEnabled) "CANON" else "SAFE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 14b. Mask Undiscovered Digimon (DigiDex & Evo Lines)
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MASK UNDISCOVERED",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (maskUndiscoveredSpecies) "Question marks & silhouettes" else "Full reveal (no mystery)",
                        fontSize = 7.5.sp,
                        color = if (maskUndiscoveredSpecies) Color(0xFF80D8FF) else Color(0xFFFFB74D)
                    )
                }
                Button(
                    onClick = {
                        val next = !maskUndiscoveredSpecies
                        repository.setMaskUndiscoveredSpecies(next)
                        if (isVibrationEnabled) hapticManager.playStepMilestone()
                    },
                    modifier = Modifier.size(width = 58.dp, height = 30.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (maskUndiscoveredSpecies) Color(0xFF00695C) else Color(0xFFE65100)
                    )
                ) {
                    Text(
                        text = if (maskUndiscoveredSpecies) "MASK" else "REVEAL",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 15. Hatch New Starter Digi-Egg (Full 16 Lineages)
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(10.dp))
            if (!showEggSelector) {
                Button(
                    onClick = { showEggSelector = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(horizontal = displayConfig.horizontalPadding),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF5E35B1))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.EGG, size = 10.dp, tint = Color.White)
                        Text("HATCH STARTER DIGI-EGG (16)", fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                val starterEggs = listOf(
                    Triple(101, "V1•BOTA", Color(0xFF37474F)),
                    Triple(103, "V2•PUNI", Color(0xFFC62828)),
                    Triple(105, "V3•POYO", Color(0xFF0277BD)),
                    Triple(107, "V4•YURA", Color(0xFF2E7D32)),
                    Triple(109, "V5•ZURU", Color(0xFF33691E)),
                    Triple(111, "PEN•MOKU", Color(0xFFE65100)),
                    Triple(113, "VIR•KURA", Color(0xFF4A148C)),
                    Triple(130, "WND•NYOK", Color(0xFFAD1457)),
                    Triple(131, "DEP•PICH", Color(0xFF0097A7)),
                    Triple(132, "MTL•PUKA", Color(0xFF1565C0)),
                    Triple(135, "Z•COTSU", Color(0xFF4E342E)),
                    Triple(136, "D3•FUFU", Color(0xFF00838F)),
                    Triple(137, "DRK•ALGO", Color(0xFF311B92)),
                    Triple(138, "VTL•DOKI", Color(0xFFC2185B)),
                    Triple(139, "CYB•CHOR", Color(0xFF2E7D32)),
                    Triple(140, "COL•BUBB", Color(0xFFE91E63))
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding)
                        .background(Color(0xFF1D1730), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SELECT FROM 16 STARTER EGGS:",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCE93D8),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    starterEggs.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            pair.forEach { (eggId, label, bgColor) ->
                                val sp = PetSpecies.getById(eggId)
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.hatchNewEgg(eggId)
                                            hapticManager.playVictoryFanfare()
                                            feedbackMessage = "Hatched ${sp.name}!"
                                            showEggSelector = false
                                            delay(1500L)
                                            feedbackMessage = null
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(26.dp).padding(horizontal = 2.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = bgColor)
                                ) {
                                    Text(label, fontSize = 7.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.clickable { showEggSelector = false }
                    ) {
                        DigiVectorIcon(type = DigiIconType.CLOSE, size = 9.dp, tint = Color.LightGray)
                        Text(
                            text = "CANCEL",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        feedbackMessage?.let { msg ->
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg,
                    fontSize = 9.sp,
                    color = Color(0xFF69F0AE),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ==========================================
        // 14. Platform & Hardware Status (AOSP + Wear OS + LAN Sync)
        // ==========================================
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            val pedometerMode = remember { com.digiwear.wear.sensor.StepTrackerManager.getActivePedometerMode(context) }
            val nfcAvailable = remember { com.digiwear.wear.nfc.NfcDuelManager.isNfcAvailable(context) }
            val lanIp = remember { com.digiwear.wear.datalayer.AospLanSyncServer.getLocalIpAddress() }
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding)
                    .background(Color(0xFF1A222D), RoundedCornerShape(8.dp))
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DigiVectorIcon(type = DigiIconType.WATCH, size = 9.dp, tint = Color(0xFF80D8FF))
                    Text(
                        text = "PLATFORM & HARDWARE STATUS",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF80D8FF),
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${displayConfig.runtimePlatformLabel} (${displayConfig.screenWidthDp}x${displayConfig.screenHeightDp}dp)",
                    fontSize = 7.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "PEDOMETER: $pedometerMode",
                    fontSize = 6.5.sp,
                    color = Color(0xFF69F0AE),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "NFC RADIO: ${if (nfcAvailable) "HCE / NDEF READY" else "DIGIROM CODE MODE"}",
                    fontSize = 6.5.sp,
                    color = Color(0xFFFFD54F),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "AOSP LAN BRIDGE: $lanIp:${com.digiwear.wear.datalayer.AospLanSyncServer.TCP_PORT}",
                    fontSize = 6.5.sp,
                    color = Color(0xFFEA80FC),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            val idx = shapeModes.indexOf(displayShapeOverride).coerceAtLeast(0)
                            val nextShape = shapeModes[(idx + 1) % shapeModes.size]
                            repository.setDisplayShapeOverride(nextShape)
                            if (isVibrationEnabled) hapticManager.playStepMilestone()
                        },
                        modifier = Modifier.weight(1f).height(24.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3949AB))
                    ) {
                        Text("SHAPE: $displayShapeOverride", fontSize = 6.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                repository.addAdventureSteps(100)
                                hapticManager.playStepMilestone()
                                feedbackMessage = "+100 Steps Added!"
                                delay(1500L)
                                feedbackMessage = null
                            }
                        },
                        modifier = Modifier.weight(1f).height(24.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00695C))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.STEP, size = 8.dp, tint = Color.White)
                            Text("+100 STEPS", fontSize = 6.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 15. Back Button
        // ==========================================
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(horizontal = displayConfig.horizontalPadding),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF263238))
            ) {
                Text("BACK [C]", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
