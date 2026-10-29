package com.digiwear.wear.watchface

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import android.content.ComponentName
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.watchface.ComplicationHelperActivity
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.editor.EditorSession
import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.CustomThemeConfig
import com.digiwear.core.model.LcdPattern
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.haptic.HapticManager
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * On-Wrist Watch Face Editor for DigiWatch.
 *
 * Implements AndroidX Watch Face Editor Activity (androidx.wear.watchface.editor.action.WATCH_FACE_EDITOR).
 * Allows Tamers to configure:
 * 1. Retro LCD Backlight Themes (Olive 1997, Amber, Cyber Cyan, Emerald, Silver GB, Violet, Coral Red, Ice Blue, OLED Black).
 * 2. LCD Matrix Patterns (Dot Matrix, Digital Grid, Scanlines, OLED Solid).
 * 3. 12-Hour vs 24-Hour Time Format.
 * 4. 4 Complication Slots (Top, Bottom, Left, Right) via Wear OS EditorSession complication picker.
 * 5. Live Interactive Preview with instant confirmation and haptic feedback.
 */
class DigiWatchFaceEditorActivity : ComponentActivity() {

    private var editorSession: EditorSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                val session = EditorSession.createOnWatchEditorSession(this@DigiWatchFaceEditorActivity)
                session.commitChangesOnClose = true
                editorSession = session
                Log.d(TAG, "Watch face EditorSession successfully initialized")
            } catch (e: Exception) {
                Log.w(TAG, "Watch face EditorSession unavailable: ${e.message}")
            }
        }

        val repository = PetRepository.getInstance(applicationContext)
        val hapticManager = HapticManager(applicationContext)

        setContent {
            MaterialTheme {
                DigiWatchFaceEditorScreen(
                    repository = repository,
                    hapticManager = hapticManager,
                    onConfigureComplicationSlot = { slotId ->
                        launchComplicationChooser(slotId)
                    },
                    onClose = {
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }

    private fun launchComplicationChooser(slotId: Int) {
        val session = editorSession
        if (session != null) {
            lifecycleScope.launch {
                try {
                    session.openComplicationDataSourceChooser(slotId)
                } catch (e: Exception) {
                    Log.w(TAG, "EditorSession complication chooser failed for slot $slotId: ${e.message}")
                    startComplicationChooserFallback(slotId)
                }
            }
        } else {
            startComplicationChooserFallback(slotId)
        }
    }

    private fun startComplicationChooserFallback(slotId: Int) {
        try {
            val supportedTypes = listOf(
                ComplicationType.SHORT_TEXT,
                ComplicationType.RANGED_VALUE,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            )
            val chooserIntent = ComplicationHelperActivity.createComplicationDataSourceChooserHelperIntent(
                this,
                ComponentName(this, DigiWatchFaceService::class.java),
                slotId,
                supportedTypes,
                null,
                null,
                null,
                null
            )
            startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch complication chooser for slot $slotId", e)
        }
    }

    override fun onDestroy() {
        editorSession?.close()
        editorSession = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "DigiWatchFaceEditor"
    }
}

data class BacklightThemePreset(
    val id: String,
    val name: String,
    val primaryColorHex: Long,
    val isSystemTheme: Boolean = false,
    val backgroundTheme: BackgroundTheme? = null
)

val PRESET_THEMES = listOf(
    BacklightThemePreset("classic_lcd", "Olive 1997", 0xFF8B956DL, isSystemTheme = true, backgroundTheme = BackgroundTheme.CLASSIC_VPET_LCD),
    BacklightThemePreset("amber", "Amber", 0xFFDDAA33L),
    BacklightThemePreset("cyber_cyan", "Cyber Cyan", 0xFF00E5FFL),
    BacklightThemePreset("emerald", "Emerald", 0xFF00C853L),
    BacklightThemePreset("silver_gb", "Silver GB", 0xFFA8B39BL),
    BacklightThemePreset("violet", "Violet", 0xFF9C27B0L),
    BacklightThemePreset("coral_red", "Coral Red", 0xFFFF5252L),
    BacklightThemePreset("ice_blue", "Ice Blue", 0xFF80D8FFL),
    BacklightThemePreset("oled_black", "OLED Black", 0xFF000000L, isSystemTheme = true, backgroundTheme = BackgroundTheme.OLED_MINIMAL)
)

@Composable
fun DigiWatchFaceEditorScreen(
    repository: PetRepository,
    hapticManager: HapticManager,
    onConfigureComplicationSlot: (Int) -> Unit,
    onClose: () -> Unit
) {
    val currentTheme by repository.themeFlow.collectAsState(initial = BackgroundTheme.CLASSIC_VPET_LCD)
    val customThemeConfig by repository.customThemeConfigFlow.collectAsState()
    val is24hFormat by repository.clockFormatFlow.collectAsState()
    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun triggerFeedback(message: String) {
        statusMessage = message
        try {
            hapticManager.playStepMilestone()
        } catch (_: Exception) {}
    }

    val currentPattern = when {
        currentTheme == BackgroundTheme.CUSTOM_MATRIX && customThemeConfig != null -> customThemeConfig!!.pattern
        else -> LcdPattern.DOT_MATRIX
    }

    val activePrimaryHex = when {
        currentTheme == BackgroundTheme.CUSTOM_MATRIX && customThemeConfig != null -> customThemeConfig!!.primaryColorHex
        currentTheme == BackgroundTheme.OLED_MINIMAL -> 0xFF000000L
        else -> currentTheme.primaryColorHex
    }

    val displayConfig = com.digiwear.wear.ui.WatchDisplayConfig.current()

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101216)),
        state = listState,
        contentPadding = displayConfig.contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "DIGI-WATCH EDITOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "CUSTOMIZE WATCH FACE",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // 2. Live Interactive LCD Preview Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(56.dp)
                    .background(Color(activePrimaryHex), RoundedCornerShape(8.dp))
                    .border(1.5.dp, Color(0xFF37474F), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                val previewTime = if (is24hFormat) {
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                } else {
                    LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
                }

                val previewTextColor = if (activePrimaryHex == 0xFF000000L) Color.White else Color(0xFF1A1F16)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = previewTime,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = previewTextColor,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• ${currentPattern.displayName.uppercase()} •",
                        fontSize = 7.5.sp,
                        color = previewTextColor.copy(alpha = 0.75f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        if (statusMessage != null) {
            item {
                Text(
                    text = "✓ $statusMessage",
                    fontSize = 8.sp,
                    color = Color(0xFF81C784),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        // 3. Section: LCD Backlight Theme
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "LCD BACKLIGHT THEME",
                    fontSize = 9.sp,
                    color = Color(0xFF80CBC4),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PRESET_THEMES.forEach { preset ->
                    val isSelected = when {
                        currentTheme == BackgroundTheme.CUSTOM_MATRIX && customThemeConfig != null -> customThemeConfig!!.primaryColorHex == preset.primaryColorHex
                        preset.isSystemTheme && preset.backgroundTheme != null -> currentTheme == preset.backgroundTheme
                        else -> preset.primaryColorHex == activePrimaryHex
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (preset.isSystemTheme && preset.backgroundTheme != null) {
                                    repository.setBackgroundTheme(preset.backgroundTheme)
                                } else {
                                    val gridHex = deriveGridColor(preset.primaryColorHex)
                                    val secHex = CustomThemeConfig.deriveSecondaryColorHex(preset.primaryColorHex, gridHex)
                                    val config = CustomThemeConfig(
                                        primaryColorHex = preset.primaryColorHex,
                                        secondaryColorHex = secHex,
                                        gridColorHex = gridHex,
                                        pattern = currentPattern
                                    )
                                    repository.setCustomThemeConfig(config)
                                    repository.setBackgroundTheme(BackgroundTheme.CUSTOM_MATRIX)
                                }
                                triggerFeedback("Theme: ${preset.name}")
                            }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(preset.primaryColorHex), RoundedCornerShape(6.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color(0xFF37474F),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.name,
                            fontSize = 7.sp,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 4. Section: LCD Matrix Pattern
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LCD MATRIX PATTERN",
                    fontSize = 9.sp,
                    color = Color(0xFF80CBC4),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LcdPattern.entries.forEach { pattern ->
                    val isSelected = pattern == currentPattern
                    Chip(
                        onClick = {
                            val activeHex = activePrimaryHex
                            val gridHex = if (activeHex == 0xFF000000L) 0xFF141414L else deriveGridColor(activeHex)
                            val secHex = if (activeHex == 0xFF000000L) 0xFF0A0A0AL else CustomThemeConfig.deriveSecondaryColorHex(activeHex, gridHex)
                            val newConfig = CustomThemeConfig(
                                primaryColorHex = activeHex,
                                secondaryColorHex = secHex,
                                gridColorHex = gridHex,
                                pattern = pattern
                            )
                            repository.setCustomThemeConfig(newConfig)
                            triggerFeedback("Pattern: ${pattern.displayName}")
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (isSelected) Color(0xFF263238) else Color(0xFF181C24),
                            contentColor = if (isSelected) Color(0xFFFFD54F) else Color.LightGray
                        ),
                        modifier = Modifier.height(32.dp),
                        label = {
                            Text(
                                text = pattern.displayName,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // 5. Section: 12H / 24H Clock Format Toggle
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ToggleChip(
                checked = is24hFormat,
                onCheckedChange = { checked ->
                    repository.setClockFormat24h(checked)
                    triggerFeedback(if (checked) "Clock: 24H Format" else "Clock: 12H Format")
                },
                label = {
                    Text(
                        text = "24-HOUR FORMAT",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                secondaryLabel = {
                    Text(
                        text = if (is24hFormat) "Active (14:30)" else "Inactive (2:30 PM)",
                        fontSize = 8.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                },
                toggleControl = {
                    Switch(checked = is24hFormat)
                },
                colors = ToggleChipDefaults.toggleChipColors(
                    checkedStartBackgroundColor = Color(0xFF1E2838),
                    checkedEndBackgroundColor = Color(0xFF1E2838),
                    uncheckedStartBackgroundColor = Color(0xFF151820),
                    uncheckedEndBackgroundColor = Color(0xFF151820)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // 6. Section: Complication Slots Configuration
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "COMPLICATION SLOTS",
                    fontSize = 9.sp,
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "TAP SLOT TO CONFIGURE PROVIDER",
                    fontSize = 7.5.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        val complicationSlots = listOf(
            Triple(DigiWatchFaceService.SLOT_TOP_ID, "TOP SLOT (100)", "Date / Weather / Empty"),
            Triple(DigiWatchFaceService.SLOT_BOTTOM_ID, "BOTTOM SLOT (101)", "Steps / Goal Progress"),
            Triple(DigiWatchFaceService.SLOT_LEFT_ID, "LEFT SLOT (102)", "Heart Rate / Steps"),
            Triple(DigiWatchFaceService.SLOT_RIGHT_ID, "RIGHT SLOT (103)", "Battery / Custom Choice")
        )

        complicationSlots.forEach { (slotId, title, defaultDesc) ->
            item {
                Chip(
                    onClick = {
                        triggerFeedback("Opening $title picker...")
                        onConfigureComplicationSlot(slotId)
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF181F2A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    label = {
                        Text(
                            text = title,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF90CAF9)
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = defaultDesc,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        // 7. Done / Confirmation Button
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(10.dp))
                Chip(
                    onClick = onClose,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .padding(vertical = 4.dp),
                    label = {
                        Text(
                            text = "APPLY & EXIT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Derives a subtle grid tint based on the primary LCD backlight color.
 */
private fun deriveGridColor(primaryHex: Long): Long {
    val r = (((primaryHex shr 16) and 0xFF) * 0.72f).toLong().coerceIn(0, 255)
    val g = (((primaryHex shr 8) and 0xFF) * 0.72f).toLong().coerceIn(0, 255)
    val b = ((primaryHex and 0xFF) * 0.72f).toLong().coerceIn(0, 255)
    return (0xFF000000L or (r shl 16) or (g shl 8) or b) and 0xFFFFFFFFL
}
