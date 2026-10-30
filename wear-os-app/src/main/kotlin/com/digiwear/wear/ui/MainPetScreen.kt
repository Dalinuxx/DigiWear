package com.digiwear.wear.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.platform.LocalContext
import com.digiwear.wear.sound.BuzzerManager
import kotlin.math.sqrt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.digiwear.core.model.AdventureProgress
import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.CampaignNode
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import com.digiwear.core.model.PetState
import com.digiwear.wear.ambient.LocalAmbientState
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.haptic.HapticManager
import com.digiwear.wear.input.HardwareInputManager
import com.digiwear.wear.input.VPetButton
import com.digiwear.wear.render.SpriteCanvasRenderer
import com.digiwear.core.engine.TrainingEngine
import com.digiwear.wear.render.SpriteFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VPetMenu(val label: String, val iconType: DigiIconType) {
    STATUS("STATUS", DigiIconType.STATUS),
    FEED("FEED", DigiIconType.FEED),
    TRAIN("TRAIN", DigiIconType.TRAIN),
    BATTLE("BATTLE", DigiIconType.BATTLE),
    FLUSH("FLUSH", DigiIconType.FLUSH),
    LIGHT("LIGHT", DigiIconType.LIGHT_ON),
    MEDS("MEDS", DigiIconType.BANDAGE_CROSS),
    ATTENTION("CALL", DigiIconType.ATTENTION)
}

@Composable
fun MainPetScreen(
    repository: PetRepository,
    hapticManager: HapticManager,
    buzzerManager: BuzzerManager = remember { BuzzerManager() },
    onNavigateBattle: () -> Unit,
    onNavigateAdventure: () -> Unit,
    onNavigateStatus: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateWorkout: (com.digiwear.core.workout.WorkoutType) -> Unit = {}
) {
    val ambientState = LocalAmbientState.current
    val petState by repository.petStateFlow.collectAsState(initial = PetState())
    val currentTheme by repository.themeFlow.collectAsState(initial = BackgroundTheme.CLASSIC_VPET_LCD)
    val customThemeConfig by repository.customThemeConfigFlow.collectAsState(initial = null)
    val wallpaperVersion by repository.wallpaperVersionFlow.collectAsState(initial = 0L)
    val buttonPlacement by repository.buttonPlacementFlow.collectAsState(initial = "RIGHT")
    val isPendulumShakeMode by repository.pendulumShakeModeFlow.collectAsState(initial = false)
    val isSoundEnabled by repository.soundEnabledFlow.collectAsState(initial = false)
    val contrastLevel by repository.contrastLevelFlow.collectAsState(initial = "MEDIUM")
    val ambientStyle by repository.ambientStyleFlow.collectAsState(initial = "MINIMAL_DOT")
    val is24hFormat by repository.clockFormatFlow.collectAsState(initial = false)
    val adventureOverlayEnabled by repository.adventureOverlayFlow.collectAsState(initial = true)
    val adventureProgress by repository.adventureStateFlow.collectAsState(initial = AdventureProgress())
    val campaignJson by repository.campaignJsonFlow.collectAsState()
    val activeCampaign = remember(campaignJson) { repository.getActiveCampaign() }
    val totalNodes = activeCampaign.nodes.size.coerceAtLeast(1)
    val activeNodeIndex = adventureProgress.currentNodeIndex.coerceIn(0, totalNodes - 1)
    val activeNode = activeCampaign.nodes.getOrNull(activeNodeIndex)
    val hazardEffect = activeNode?.environmentEffect?.takeIf { !it.equals("clear", ignoreCase = true) }
    val isBossReady = (adventureProgress.accumulatedStepsCurrentNode >= (activeNode?.requiredSteps ?: 1000)) && !petState.isFrozen
    val displayConfig = WatchDisplayConfig.current()
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSoundEnabled) {
        buzzerManager.isEnabled = isSoundEnabled
    }

    val hasActiveEgg by repository.hasActiveEggFlow.collectAsState(initial = repository.hasActiveEggFlow.value)
    var showEggPicker by remember { mutableStateOf(false) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var overrideSpriteFrame by remember { mutableStateOf<SpriteFrame?>(null) }
    var selectedMenuIndex by remember { mutableStateOf<Int?>(null) }

    // Training Minigame Quick Time Event state
    var isTrainingActive by remember { mutableStateOf(false) }
    var showTrainingPicker by remember { mutableStateOf(false) }
    var showLightPicker by remember { mutableStateOf(false) }
    var flushAnimationProgress by remember { mutableFloatStateOf(-1f) }
    var medsAnimationProgress by remember { mutableFloatStateOf(-1f) }
    var evolvingFromSpeciesId by remember { mutableStateOf<Int?>(null) }

    // Evolution celebration is driven directly from the persisted-state flow so the placeholder
    // PetState() used as the Compose initial value can never be mistaken for a previous species.
    // The first emission only seeds the baseline, so re-entering this screen never replays it.
    LaunchedEffect(Unit) {
        var lastSeenSpeciesId: Int? = null
        repository.petStateFlow.collect { state ->
            val previousId = lastSeenSpeciesId
            if (previousId != null && previousId != state.speciesId) {
                val prev = PetSpecies.getById(previousId)
                val curr = PetSpecies.getById(state.speciesId)
                // Only a forward stage advance is an evolution; hatching an egg (Baby I) or swapping partners is not.
                if (curr.stage != PetStage.BABY_I && curr.stage.stageIndex > prev.stage.stageIndex) {
                    evolvingFromSpeciesId = previousId
                }
            }
            lastSeenSpeciesId = state.speciesId
        }
    }

    // Periodic time-delta refresh while interactive (strictly paused during Ambient Mode)
    LaunchedEffect(ambientState.isAmbient) {
        if (ambientState.isAmbient) return@LaunchedEffect
        repository.processDecay()
        while (true) {
            delay(30_000L) // Refresh every 30s while interactive
            repository.processDecay()
        }
    }

    // Play haptic & buzzer alert if attention call active and lights are on
    LaunchedEffect(petState.attentionCallActive, petState.lightsOff) {
        if (petState.attentionCallActive && !petState.lightsOff) {
            hapticManager.playAttentionAlert()
            buzzerManager.playAttentionBeep()
        }
    }

    val executeMenuAction: (Int) -> Unit = { index ->
        val item = VPetMenu.entries[index]
        if (petState.isDeceased && item != VPetMenu.STATUS && item != VPetMenu.ATTENTION) {
            actionFeedback = "Tap Tombstone to Hatch"
            buzzerManager.playAttentionBeep()
            scope.launch {
                delay(1200L)
                actionFeedback = null
            }
        } else when (item) {
            VPetMenu.FEED -> {
                scope.launch {
                    if (petState.isSleeping || petState.isFrozen) {
                        overrideSpriteFrame = SpriteFrame.REFUSAL
                        actionFeedback = if (petState.isFrozen) "Frozen in Cryo!" else "Sleeping!"
                        buzzerManager.playAttentionBeep()
                        delay(800L)
                        overrideSpriteFrame = null
                        actionFeedback = null
                    } else {
                        if (petState.hunger < 4) {
                            repository.feedMeal()
                            actionFeedback = "Fed Meat (+1g)"
                        } else {
                            repository.feedProtein()
                            actionFeedback = "Fed Vitamin (+2g)"
                        }
                        hapticManager.playStepMilestone()
                        overrideSpriteFrame = SpriteFrame.FEED_START
                        delay(320L)
                        overrideSpriteFrame = SpriteFrame.JOY_EAT
                        delay(320L)
                        overrideSpriteFrame = SpriteFrame.FEED_START
                        delay(320L)
                        overrideSpriteFrame = SpriteFrame.JOY_EAT
                        delay(550L)
                        overrideSpriteFrame = null
                        actionFeedback = null
                    }
                }
            }
            VPetMenu.LIGHT -> {
                showLightPicker = true
            }
            VPetMenu.BATTLE -> {
                if (petState.isInjured) {
                    actionFeedback = "Injured! Needs Meds"
                    buzzerManager.playAttentionBeep()
                    hapticManager.playAttentionAlert()
                    scope.launch {
                        delay(1200L)
                        actionFeedback = null
                    }
                } else {
                    onNavigateBattle()
                }
            }
            VPetMenu.FLUSH -> {
                scope.launch {
                    hapticManager.playStepMilestone()
                    buzzerManager.playFlushTone()
                    for (i in 0..10) {
                        flushAnimationProgress = i / 10f
                        delay(60L)
                    }
                    repository.cleanWaste()
                    actionFeedback = "Cleaned!"
                    flushAnimationProgress = -1f
                    delay(800L)
                    actionFeedback = null
                }
            }
            VPetMenu.STATUS -> {
                onNavigateStatus()
            }
            VPetMenu.TRAIN -> {
                showTrainingPicker = true
            }
            VPetMenu.MEDS -> {
                scope.launch {
                    if (petState.isInjured) {
                        hapticManager.playStepMilestone()
                        buzzerManager.playHealTone()
                        for (i in 0..10) {
                            medsAnimationProgress = i / 10f
                            delay(70L)
                        }
                        repository.cureInjury()
                        actionFeedback = "Cured!"
                        medsAnimationProgress = -1f
                    } else {
                        actionFeedback = "Not Sick"
                    }
                    delay(800L)
                    actionFeedback = null
                }
            }

            VPetMenu.ATTENTION -> {
                if (petState.attentionCallActive && !petState.lightsOff) {
                    val needReason = when {
                        petState.hunger == 0 -> {
                            selectedMenuIndex = 1 // Auto-select FEED
                            "Hungry! • Feed Partner"
                        }
                        petState.isInjured -> {
                            selectedMenuIndex = 6 // Auto-select MEDS
                            "Injured! • Give Meds"
                        }
                        petState.wasteCount >= 4 -> {
                            selectedMenuIndex = 4 // Auto-select FLUSH
                            "Dirty! • Flush Waste"
                        }
                        petState.isSleeping && !petState.lightsOff -> {
                            scope.launch {
                                repository.toggleLights()
                                hapticManager.playStepMilestone()
                            }
                            "Toggled Lights OFF"
                        }
                        petState.strength == 0 -> {
                            selectedMenuIndex = 2 // Auto-select TRAIN
                            "Needs Training!"
                        }
                        else -> "Attending Call!"
                    }
                    actionFeedback = needReason
                    hapticManager.playStepMilestone()
                    scope.launch {
                        delay(1200L)
                        actionFeedback = null
                    }
                } else {
                    actionFeedback = "Partner Satisfied • No Call"
                    hapticManager.playStepMilestone()
                    scope.launch {
                        delay(1000L)
                        actionFeedback = null
                    }
                }
            }
        }
    }

    androidx.activity.compose.BackHandler(
        enabled = isTrainingActive || showTrainingPicker || showLightPicker || evolvingFromSpeciesId != null || (hasActiveEgg && showEggPicker)
    ) {
        buzzerManager.playButtonBeep()
        when {
            showEggPicker -> showEggPicker = false
            evolvingFromSpeciesId != null -> evolvingFromSpeciesId = null
            showTrainingPicker -> showTrainingPicker = false
            showLightPicker -> showLightPicker = false
            isTrainingActive -> isTrainingActive = false
        }
    }

    // Listen to physical smartwatch hardware button presses & rotary crown input
    LaunchedEffect(isTrainingActive, showTrainingPicker, showLightPicker, selectedMenuIndex) {
        HardwareInputManager.buttonEvents.collect { button ->
            if (showTrainingPicker) {
                when (button) {
                    VPetButton.B -> {
                        showTrainingPicker = false
                        isTrainingActive = true
                    }
                    VPetButton.A -> {
                        showTrainingPicker = false
                        onNavigateWorkout(com.digiwear.core.workout.WorkoutType.FREE_TRAINING)
                    }
                    VPetButton.C -> showTrainingPicker = false
                    else -> {}
                }
                return@collect
            }
            if (showLightPicker) {
                when (button) {
                    VPetButton.B -> {
                        showLightPicker = false
                        scope.launch {
                            repository.toggleLights()
                            hapticManager.playStepMilestone()
                            actionFeedback = if (petState.lightsOff) "Lights ON" else "Lights OFF"
                            delay(900L)
                            actionFeedback = null
                        }
                    }
                    VPetButton.A -> {
                        showLightPicker = false
                        scope.launch {
                            repository.toggleColdSleep()
                            hapticManager.playStepMilestone()
                            actionFeedback = if (!petState.isFrozen) "Cold Sleep Active!" else "Awakened!"
                            delay(1000L)
                            actionFeedback = null
                        }
                    }
                    VPetButton.C -> showLightPicker = false
                    else -> {}
                }
                return@collect
            }
            if (isTrainingActive) {
                // If in training, button C can cancel
                if (button == VPetButton.C) {
                    isTrainingActive = false
                }
                return@collect
            }

            when (button) {
                VPetButton.A -> {
                    // Cycle forward through the 7 interactive care tools (0..6), skipping passive Call Indicator (7)
                    selectedMenuIndex = if (selectedMenuIndex == null) 0 else (selectedMenuIndex!! + 1) % 7
                    hapticManager.playStepMilestone()
                }
                VPetButton.PREV -> {
                    // Cycle backwards through 7 tools (0..6)
                    selectedMenuIndex = if (selectedMenuIndex == null) 6 else (selectedMenuIndex!! + 6) % 7
                    hapticManager.playStepMilestone()
                }
                VPetButton.B -> {
                    // Confirm / Execute currently highlighted menu icon
                    if (selectedMenuIndex != null) {
                        val idx = selectedMenuIndex!!
                        selectedMenuIndex = null
                        executeMenuAction(idx)
                    } else {
                        // Quick poke / interaction
                        scope.launch {
                            overrideSpriteFrame = SpriteFrame.JOY_EAT
                            hapticManager.playStepMilestone()
                            delay(1000L)
                            overrideSpriteFrame = null
                        }
                    }
                }
                VPetButton.C -> {
                    // Cancel selection or toggle lights
                    if (selectedMenuIndex != null) {
                        selectedMenuIndex = null
                    } else {
                        scope.launch {
                            repository.toggleLights()
                            hapticManager.playStepMilestone()
                            actionFeedback = if (petState.lightsOff) "Lights ON" else "Lights OFF"
                            delay(1000L)
                            actionFeedback = null
                        }
                    }
                }
            }
        }
    }

    if (ambientState.isAmbient) {
        AmbientPetScreen(
            petState = petState,
            displayConfig = displayConfig,
            ambientStyle = ambientStyle,
            is24hFormat = is24hFormat
        )
        return
    }

    val species = remember(petState.speciesId) { PetSpecies.getById(petState.speciesId) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic procedural background
        BackgroundCanvas(
            theme = currentTheme,
            isAmbient = false,
            lightsOff = petState.lightsOff,
            contrastLevel = contrastLevel,
            ambientStyle = ambientStyle,
            customThemeConfig = customThemeConfig,
            wallpaperVersion = wallpaperVersion
        )

        // Atmospheric Hazardous Biome Particles (Glitch Fog, Blizzard, Inferno Embers, Cosmic Void)
        if (adventureOverlayEnabled && hazardEffect != null) {
            EnvironmentParticleCanvas(
                environmentEffect = hazardEffect,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isTrainingActive) {
            // ========================================================
            // Authentic Bandai 5-Round Sparring Drill Training Minigame
            // (Supports both Bandai Ver. 2/20th QTE & Pendulum Accelerometer Shake)
            // ========================================================
            TrainingMinigameView(
                petState = petState,
                isPendulumShakeMode = isPendulumShakeMode,
                displayConfig = displayConfig,
                hapticManager = hapticManager,
                buzzerManager = buzzerManager,
                theme = currentTheme,
                customConfig = customThemeConfig,
                onFinished = { won, message ->
                    scope.launch {
                        repository.train(won = won)
                        if (won) {
                            overrideSpriteFrame = SpriteFrame.JOY_EAT
                            actionFeedback = message
                        } else {
                            overrideSpriteFrame = SpriteFrame.REFUSAL
                            actionFeedback = message
                        }
                        isTrainingActive = false
                        delay(1200L)
                        overrideSpriteFrame = null
                        actionFeedback = null
                    }
                },
                onCancel = { isTrainingActive = false }
            )
        } else {
            // ========================================================
            // Authentic V-Pet Display Frame (Always active; supports RIGHT, LEFT, and OFF/Touch modes)
            // ========================================================
            ThreeButtonPetView(
                species = species,
                petState = petState,
                displayConfig = displayConfig,
                buttonPlacement = buttonPlacement,
                buzzerManager = buzzerManager,
                selectedMenuIndex = selectedMenuIndex,
                overrideSpriteFrame = overrideSpriteFrame,
                actionFeedback = actionFeedback,
                is24h = is24hFormat,
                onSelectIcon = { index ->
                    if (index >= 7) return@ThreeButtonPetView
                    if (selectedMenuIndex == index) {
                        selectedMenuIndex = null
                        executeMenuAction(index)
                    } else {
                        selectedMenuIndex = index
                        hapticManager.playStepMilestone()
                    }
                },
                onPressA = {
                    selectedMenuIndex = if (selectedMenuIndex == null) 0 else (selectedMenuIndex!! + 1) % 7
                    hapticManager.playStepMilestone()
                },
                onPressB = {
                    if (selectedMenuIndex != null) {
                        val idx = selectedMenuIndex!!
                        selectedMenuIndex = null
                        executeMenuAction(idx)
                    } else {
                        selectedMenuIndex = 0
                        hapticManager.playStepMilestone()
                    }
                },
                onPressC = {
                    if (selectedMenuIndex != null) {
                        selectedMenuIndex = null
                    } else {
                        scope.launch {
                            repository.toggleLights()
                            hapticManager.playStepMilestone()
                            actionFeedback = if (petState.lightsOff) "Lights ON" else "Lights OFF"
                            delay(1000L)
                            actionFeedback = null
                        }
                    }
                },
                onNavigateSettings = onNavigateSettings,
                onNavigateStatus = onNavigateStatus,
                onNavigateAdventure = onNavigateAdventure,
                onHatchTraitedEgg = {
                    showEggPicker = true
                },
                adventureOverlayEnabled = adventureOverlayEnabled,
                adventureProgress = adventureProgress,
                activeNode = activeNode,
                isBossReady = isBossReady
            )
        }

        // Sweeping Flush Water Wave Animation
        if (flushAnimationProgress in 0f..1f) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val sweepX = size.width * flushAnimationProgress
                drawRect(
                    color = Color(0x7700E5FF),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(sweepX, size.height)
                )
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = androidx.compose.ui.geometry.Offset(sweepX, 0f),
                    end = androidx.compose.ui.geometry.Offset(sweepX, size.height),
                    strokeWidth = 4.dp.toPx()
                )
            }
        }

        // Medical Pill Capsule & Healing Sparkle Animation
        if (medsAnimationProgress in 0f..1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MedsPillOverlayIcon(size = 30.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+ + + MED-PILL CURED + + +",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E676)
                    )
                }
            }
        }

        // Training Mode Selector Modal
        if (showTrainingPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF5050810))
                    .clickable { showTrainingPicker = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .background(Color(0xFF111822), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SELECT TRAINING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E5FF)
                    )

                    Button(
                        onClick = {
                            showTrainingPicker = false
                            isTrainingActive = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DigiVectorIcon(type = DigiIconType.TRAIN, size = 11.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("MINI-GAME", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Button(
                        onClick = {
                            showTrainingPicker = false
                            onNavigateWorkout(com.digiwear.core.workout.WorkoutType.FREE_TRAINING)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DigiVectorIcon(type = DigiIconType.BOLT, size = 11.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FREE WORKOUT", fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Button(
                        onClick = { showTrainingPicker = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F)),
                        shape = RoundedCornerShape(11.dp)
                    ) {
                        Text("CANCEL", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = Color.LightGray)
                    }
                }
            }
        }

        if (showLightPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF5050810))
                    .clickable { showLightPicker = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .background(Color(0xFF111822), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "LIGHT & COLD SLEEP",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFD54F)
                    )

                    Button(
                        onClick = {
                            showLightPicker = false
                            scope.launch {
                                repository.toggleLights()
                                hapticManager.playStepMilestone()
                                buzzerManager.playButtonBeep()
                                actionFeedback = if (petState.lightsOff) "Lights ON" else "Lights OFF"
                                delay(900L)
                                actionFeedback = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (petState.lightsOff) "LIGHTS ON" else "LIGHTS OFF",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            showLightPicker = false
                            scope.launch {
                                repository.toggleColdSleep()
                                hapticManager.playStepMilestone()
                                buzzerManager.playHealTone()
                                actionFeedback = if (!petState.isFrozen) "Cold Sleep Active!" else "Awakened!"
                                delay(1000L)
                                actionFeedback = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00838F)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (petState.isFrozen) "WAKE UP" else "COLD SLEEP (REST)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = { showLightPicker = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F)),
                        shape = RoundedCornerShape(11.dp)
                    ) {
                        Text("CANCEL", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = Color.LightGray)
                    }
                }
            }
        }

        // Evolution Celebration Overlay
        evolvingFromSpeciesId?.let { prevId ->
            EvolutionCelebrationOverlay(
                previousSpeciesId = prevId,
                newSpeciesId = petState.speciesId,
                buzzerManager = buzzerManager,
                hapticManager = hapticManager,
                onDismiss = { evolvingFromSpeciesId = null }
            )
        }

        // Digi-Egg Selection Screen (auto-prompted when no active egg exists or on hatch request)
        if (!hasActiveEgg || showEggPicker) {
            EggSelectionOverlay(
                onSelectEgg = { eggId ->
                    scope.launch {
                        repository.hatchNewEgg(eggId)
                        buzzerManager.playVictoryFanfare()
                        hapticManager.playVictoryFanfare()
                        showEggPicker = false
                        evolvingFromSpeciesId = 0
                    }
                },
                canDismiss = hasActiveEgg,
                onDismiss = { showEggPicker = false }
            )
        }
    }
}


/**
 * Authentic Bandai LCD style 3-Button layout with recessed top & bottom 4-icon LCD rails,
 * framed pixel-art viewport, and tactile right-side [A] [B] [C] molded circular peg buttons.
 */
@Composable
private fun MoldedCircularButton(
    label: String,
    subLabel: String,
    size: Dp,
    rimColor: Color,
    baseColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "btnScale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(animatedScale)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(baseColor, Color(0xFF06090D)),
                    radius = 55f
                ),
                shape = CircleShape
            )
            .border(
                width = 1.75.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        rimColor,
                        rimColor.copy(alpha = 0.45f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = if (size.value >= 36f) 11.5.sp else 9.5.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                fontFamily = FontFamily.Monospace,
                lineHeight = if (size.value >= 36f) 11.5.sp else 9.5.sp
            )
            Text(
                text = subLabel,
                fontSize = if (size.value >= 36f) 6.sp else 5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = rimColor.copy(alpha = 0.9f),
                fontFamily = FontFamily.Monospace,
                lineHeight = if (size.value >= 36f) 6.sp else 5.sp
            )
        }
    }
}

@Composable
private fun BandaiButtonRail(
    displayConfig: WatchDisplayConfig,
    onPressA: () -> Unit,
    onPressB: () -> Unit,
    onPressC: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(displayConfig.rightRailWidth)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Button A: Select / Cycle
        MoldedCircularButton(
            label = "A",
            subLabel = "SEL",
            size = displayConfig.shellButtonSize,
            rimColor = Color(0xFF80D8FF),
            baseColor = Color(0xFF162530),
            textColor = Color.White,
            onClick = onPressA
        )

        Spacer(modifier = Modifier.height(displayConfig.shellButtonSpacing))

        // Button B: Confirm / Execute / Poke
        MoldedCircularButton(
            label = "B",
            subLabel = "OK",
            size = displayConfig.shellButtonSize,
            rimColor = Color(0xFF69F0AE),
            baseColor = Color(0xFF142B1A),
            textColor = Color(0xFFB9F6CA),
            onClick = onPressB
        )

        Spacer(modifier = Modifier.height(displayConfig.shellButtonSpacing))

        // Button C: Cancel / Esc / Light
        MoldedCircularButton(
            label = "C",
            subLabel = "ESC",
            size = displayConfig.shellButtonSize,
            rimColor = Color(0xFFEA80FC),
            baseColor = Color(0xFF281432),
            textColor = Color(0xFFF3E5F5),
            onClick = onPressC
        )
    }
}

@Composable
private fun LcdIconCell(
    index: Int,
    item: VPetMenu,
    isSelected: Boolean,
    isCallActive: Boolean,
    width: Dp,
    height: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    isCircular: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val bgColor = when {
        isSelected -> Color(0xFF00B0FF)
        isCallActive -> Color(0xFFD50000)
        else -> Color(0x33091016)
    }
    val borderColor = when {
        isSelected -> Color(0xFFFFD54F)
        isCallActive -> Color(0xFFFFD54F)
        else -> Color(0x4480CBC4)
    }
    val borderWidth = if (isSelected || isCallActive) 1.5.dp else 0.75.dp
    val cellShape = if (isCircular) CircleShape else RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(bgColor, cellShape)
            .border(borderWidth, borderColor, cellShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val iconSize = (minOf(width, height) * 0.62f).coerceAtLeast(13.dp)
        DigiVectorIcon(
            type = item.iconType,
            size = iconSize,
            tint = if (isCallActive) Color(0xFFFFEB3B) else Color.Unspecified
        )
    }
}

/** Places [content] centred on the dial centre, shifted by ([x], [y]). */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.DialSlot(
    x: Dp,
    y: Dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(x = x, y = y),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Round "V-Pet Dial" layout shared by every round watch size.
 *
 * Everything is derived from the display diameter [D] (never fixed dp), so Galaxy Watch, Pixel Watch,
 * and AOSP round watches all get the same composition:
 * - the pet always rests on the exact screen centre (regardless of button placement),
 * - the 8 menu icons sit on arcs near the bezel (4 upper, 4 lower) and can never clip,
 * - time, name and telemetry sit on chords that are always inside the circle,
 * - the A/B/C rail (LEFT or RIGHT) follows the bezel and only acts as the pet's walking boundary.
 */
@Composable
private fun RoundDialPetView(
    species: PetSpecies,
    petState: PetState,
    displayConfig: WatchDisplayConfig,
    buttonPlacement: String,
    buzzerManager: BuzzerManager,
    selectedMenuIndex: Int?,
    overrideSpriteFrame: SpriteFrame?,
    actionFeedback: String?,
    is24h: Boolean,
    onSelectIcon: (Int) -> Unit,
    onPressA: () -> Unit,
    onPressB: () -> Unit,
    onPressC: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateStatus: () -> Unit,
    onNavigateAdventure: () -> Unit,
    onHatchTraitedEgg: () -> Unit,
    adventureOverlayEnabled: Boolean = true,
    adventureProgress: AdventureProgress = AdventureProgress(),
    activeNode: CampaignNode? = null,
    isBossReady: Boolean = false
) {
    val attrColor = when (species.attribute.displayName) {
        "Vaccine" -> Color(0xFF00E5FF)
        "Data" -> Color(0xFFFFCA28)
        "Virus" -> Color(0xFFFF5252)
        else -> Color(0xFF69F0AE)
    }

    val diameter = minOf(displayConfig.screenWidthDp, displayConfig.screenHeightDp).toFloat()
    val d = diameter.dp
    val r = (diameter / 2f).dp
    val fontBase = (diameter * 0.036f).coerceAtLeast(7f)

    // Live clock with 12h AM/PM support
    val timeFormatter = remember(is24h) {
        SimpleDateFormat(if (is24h) "HH:mm" else "h:mm", Locale.getDefault())
    }
    val amPmFormatter = remember(is24h) {
        if (is24h) null else SimpleDateFormat("a", Locale.getDefault())
    }
    var clockTimeText by remember { mutableStateOf("") }
    var clockAmPmText by remember { mutableStateOf("") }
    LaunchedEffect(timeFormatter, amPmFormatter) {
        while (true) {
            val now = Date()
            clockTimeText = timeFormatter.format(now)
            clockAmPmText = amPmFormatter?.format(now)?.uppercase(Locale.getDefault()) ?: ""
            delay(15_000L)
        }
    }

    // Sprite geometry & walking boundaries hugging bezel edge
    val spriteSize = (diameter * 0.30f).dp
    val spriteHalf = spriteSize * 0.40f
    val railBtn = (diameter * 0.125f).dp
    val railOffsetY = railBtn * 1.30f
    val railCenterEdge = 0.97f * diameter / 2f
    // Horizontal centre of a rail button at vertical offset yDp, hugging the bezel
    fun railX(yDp: Float): Float {
        val inside = railCenterEdge * railCenterEdge - yDp * yDp
        return sqrt(inside.coerceAtLeast(0f)) - railBtn.value / 2f
    }
    val railInnerEdge = (railX(railOffsetY.value) - railBtn.value / 2f).dp
    val gap = (diameter * 0.02f).dp
    val fullSide = r * 0.95f - spriteHalf

    val isRailEnabled = buttonPlacement == "RIGHT" || buttonPlacement == "LEFT"
    val railOnRight = buttonPlacement == "RIGHT"

    // Touch-friendly circular menu buttons and utility chips hugging bezel with zero clipping
    val iconDiameter = (diameter * 0.125f).dp.coerceIn(24.dp, 29.dp)
    val chipSize = (diameter * 0.120f).dp.coerceIn(24.dp, 29.dp)
    val chipHalf = chipSize * 0.5f
    val chipR = r * 0.865f
    val chipOffsetY = r * 0.18f
    val chipX = chipR * (if (railOnRight) -1f else 1f) * kotlin.math.cos(kotlin.math.asin((chipOffsetY / chipR).toDouble())).toFloat()
    val chipInnerEdge = (kotlin.math.abs(chipX.value) - chipHalf.value).dp
    val roamLimitChips = (chipInnerEdge - spriteHalf - gap).coerceAtLeast(0.dp)
    val roamLimitRail = (railInnerEdge - spriteHalf - gap).coerceAtLeast(0.dp)

    val roamLeft: Dp
    val roamRight: Dp
    when (buttonPlacement) {
        "RIGHT" -> { roamLeft = roamLimitChips; roamRight = roamLimitRail }
        "LEFT" -> { roamLeft = roamLimitRail; roamRight = roamLimitChips }
        else -> {
            val roamSide = (chipR.value - chipHalf.value - spriteHalf.value - gap.value).coerceAtLeast(0f).dp
            roamLeft = roamSide
            roamRight = roamSide
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ---- Live Clock (top centre, comfortably under top menu icons) ----
        DialSlot(0.dp, -(r * 0.63f)) {
            Box(
                modifier = Modifier
                    .background(Color(0x7A030A10), RoundedCornerShape(10.dp))
                    .border(0.75.dp, Color(0x3380CBC4), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = clockTimeText,
                        fontSize = (diameter * 0.082f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE0F2F1),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    if (clockAmPmText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = clockAmPmText,
                            fontSize = (diameter * 0.040f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF80CBC4),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }

        // ---- Quest & Settings utility chips along side boundary ----
        if (isRailEnabled) {
            DialSlot(chipX, -chipOffsetY) {
                Box(
                    modifier = Modifier
                        .size(chipSize)
                        .background(Color(0x66004D40), CircleShape)
                        .border(0.75.dp, Color(0xFF4DB6AC), CircleShape)
                        .clickable { onNavigateAdventure() },
                    contentAlignment = Alignment.Center
                ) {
                    DigiVectorIcon(type = DigiIconType.QUEST, size = chipSize * 0.60f)
                }
            }
            DialSlot(chipX, chipOffsetY) {
                Box(
                    modifier = Modifier
                        .size(chipSize)
                        .background(Color(0x66263238), CircleShape)
                        .border(0.75.dp, Color(0xFF90A4AE), CircleShape)
                        .clickable { onNavigateSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    DigiVectorIcon(type = DigiIconType.SETTINGS, size = chipSize * 0.60f)
                }
            }
        } else {
            // Bilateral symmetry: Quest on Left (West), Settings on Right (East)
            DialSlot(-chipR, 0.dp) {
                Box(
                    modifier = Modifier
                        .size(chipSize)
                        .background(Color(0x66004D40), CircleShape)
                        .border(0.75.dp, Color(0xFF4DB6AC), CircleShape)
                        .clickable { onNavigateAdventure() },
                    contentAlignment = Alignment.Center
                ) {
                    DigiVectorIcon(type = DigiIconType.QUEST, size = chipSize * 0.60f)
                }
            }
            DialSlot(chipR, 0.dp) {
                Box(
                    modifier = Modifier
                        .size(chipSize)
                        .background(Color(0x66263238), CircleShape)
                        .border(0.75.dp, Color(0xFF90A4AE), CircleShape)
                        .clickable { onNavigateSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    DigiVectorIcon(type = DigiIconType.SETTINGS, size = chipSize * 0.60f)
                }
            }
        }

        // ---- Menu care icons on the outside circle (0.865f * r) hugging face edge with zero bezel clipping ----
        val ringRadius = 0.865f
        val topAngles = floatArrayOf(-48f, -16f, 16f, 48f)
        val bottomAngles = floatArrayOf(-48f, -16f, 16f, 48f)
        for (k in 0..3) {
            val radTop = Math.toRadians(topAngles[k].toDouble())
            val xt = r * (ringRadius * kotlin.math.sin(radTop).toFloat())
            val yt = r * (ringRadius * kotlin.math.cos(radTop).toFloat())
            DialSlot(xt, -yt) {
                LcdIconCell(
                    index = k,
                    item = VPetMenu.entries[k],
                    isSelected = selectedMenuIndex == k,
                    isCallActive = false,
                    width = iconDiameter,
                    height = iconDiameter,
                    fontSize = displayConfig.lcdIconFontSize,
                    isCircular = true,
                    onClick = { onSelectIcon(k) }
                )
            }

            val radBottom = Math.toRadians(bottomAngles[k].toDouble())
            val xb = r * (ringRadius * kotlin.math.sin(radBottom).toFloat())
            val yb = r * (ringRadius * kotlin.math.cos(radBottom).toFloat())
            val idx = 4 + k
            val isAttentionLamp = (idx == 7)
            DialSlot(xb, yb) {
                LcdIconCell(
                    index = idx,
                    item = VPetMenu.entries[idx],
                    isSelected = if (isAttentionLamp) false else (selectedMenuIndex == idx),
                    isCallActive = (isAttentionLamp && petState.attentionCallActive && !petState.lightsOff),
                    width = iconDiameter,
                    height = iconDiameter,
                    fontSize = displayConfig.lcdIconFontSize,
                    isCircular = true,
                    onClick = if (isAttentionLamp) null else { { onSelectIcon(idx) } }
                )
            }
        }

        // ---- Name / attribute / weight (upper chord, condensed under clock) ----
        DialSlot(0.dp, -(r * 0.47f)) {
            Box(
                modifier = Modifier
                    .background(Color(0x66050D12), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = species.name.take(if (diameter < 220f) 8 else 10),
                        fontSize = fontBase.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFB9F6CA),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .background(attrColor.copy(alpha = 0.22f), RoundedCornerShape(3.dp))
                            .border(0.5.dp, attrColor, RoundedCornerShape(3.dp))
                            .padding(horizontal = 2.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = species.attribute.displayName.take(3).uppercase(),
                            fontSize = (fontBase - 2f).coerceAtLeast(5.5f).sp,
                            fontWeight = FontWeight.Black,
                            color = attrColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${petState.weight}g",
                        fontSize = (fontBase - 1f).coerceAtLeast(6.5f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0E0E0),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }

        // ---- Authentic V-Pet Waste on the floor plane (Bandai faithful: placed on right floor, mon walks over it) ----
        if (petState.wasteCount > 0) {
            DialSlot(r * 0.22f, r * 0.18f) {
                VPetWasteRow(
                    wasteCount = petState.wasteCount,
                    iconSize = (diameter * 0.095f).dp.coerceIn(20.dp, 28.dp)
                )
            }
        }

        // ---- Pet (central stage with generous roaming headroom, walks right over waste) ----
        DialSlot(0.dp, -(r * 0.05f)) {
            Box(
                modifier = Modifier
                    .size(spriteSize)
                    .clickable { onNavigateStatus() },
                contentAlignment = Alignment.Center
            ) {
                SpriteCanvasRenderer(
                    speciesId = petState.speciesId,
                    isAmbient = false,
                    overrideFrame = overrideSpriteFrame,
                    isSleeping = petState.isSleeping,
                    isInjured = petState.isInjured,
                    enableIdleRoaming = true,
                    size = spriteSize,
                    roamAmplitude = 2.2f,
                    roamLimitLeft = roamLeft,
                    roamLimitRight = roamRight
                )
            }
        }

        // ---- Prompt / feedback / RIP (comfortably below waste and pet) ----
        DialSlot(0.dp, r * 0.38f) {
            if (petState.isDeceased) {
                Box(
                    modifier = Modifier
                        .background(Color(0xD9381216), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(4.dp))
                        .clickable { onHatchTraitedEgg() }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DigiVectorIcon(type = DigiIconType.TOMBSTONE, size = 9.dp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "TAP TO HATCH EGG",
                            fontSize = (fontBase - 1f).coerceAtLeast(6.5f).sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF8A80),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val promptText = if (selectedMenuIndex != null) {
                    "[ ${VPetMenu.entries[selectedMenuIndex].label} ]"
                } else actionFeedback ?: when {
                    petState.attentionCallActive && !petState.lightsOff -> "! CALL SIGNAL !"
                    petState.isSleeping -> "ZZZ... SLEEPING"
                    else -> null
                }

                if (promptText != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (petState.attentionCallActive && !petState.lightsOff && selectedMenuIndex == null) Color(0xCCD50000) else Color(0x66091016),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = promptText,
                            fontSize = (fontBase - 0.5f).coerceAtLeast(6.5f).sp,
                            color = if (selectedMenuIndex != null) Color(0xFFFFD54F) else Color(0xFFB9F6CA),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (adventureOverlayEnabled && activeNode != null && !petState.isFrozen) {
                    // Adventure Quest Status / Boss Encounter Pill (1-tap to engage)
                    if (isBossReady) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xD9B71C1C), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFFFFD600), RoundedCornerShape(4.dp))
                                .clickable { onNavigateAdventure() }
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "⚔️ S${adventureProgress.currentNodeIndex + 1} BOSS READY",
                                fontSize = (fontBase - 0.5f).coerceAtLeast(6.5f).sp,
                                color = Color(0xFFFFD600),
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(Color(0x66004D40), RoundedCornerShape(4.dp))
                                .border(0.75.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .clickable { onNavigateAdventure() }
                                .padding(horizontal = 5.dp, vertical = 0.5.dp)
                        ) {
                            Text(
                                text = "🗺️ S${adventureProgress.currentNodeIndex + 1}: ${adventureProgress.accumulatedStepsCurrentNode}/${activeNode.requiredSteps}",
                                fontSize = (fontBase - 0.5f).coerceAtLeast(6.5f).sp,
                                color = Color(0xFFE0F2F1),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // ---- Hunger & Strength (lower chord, perfectly centered with ample bezel margin) ----
        DialSlot(0.dp, r * 0.58f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0x55000000), RoundedCornerShape(3.dp))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "H:",
                            fontSize = (fontBase - 0.5f).coerceAtLeast(6.5f).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252),
                            fontFamily = FontFamily.Monospace
                        )
                        VPetMeterBar(
                            count = petState.hunger,
                            isHeart = true,
                            iconSize = fontBase.coerceAtLeast(7f).dp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color(0x55000000), RoundedCornerShape(3.dp))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "S:",
                            fontSize = (fontBase - 0.5f).coerceAtLeast(6.5f).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            fontFamily = FontFamily.Monospace
                        )
                        VPetMeterBar(
                            count = petState.strength,
                            isHeart = false,
                            iconSize = fontBase.coerceAtLeast(7f).dp
                        )
                    }
                }
            }
        }

        // ---- A / B / C rail following the bezel (LEFT or RIGHT); OFF hides it ----
        if (buttonPlacement == "RIGHT" || buttonPlacement == "LEFT") {
            val sign = if (buttonPlacement == "RIGHT") 1f else -1f
            val specs = listOf(
                Triple("A", "SEL", -railOffsetY.value),
                Triple("B", "OK", 0f),
                Triple("C", "ESC", railOffsetY.value)
            )
            for ((label, sub, yOff) in specs) {
                val x = (railX(yOff) * sign).dp
                DialSlot(x, yOff.dp) {
                    when (label) {
                        "A" -> MoldedCircularButton(
                            label = "A", subLabel = "SEL", size = railBtn,
                            rimColor = Color(0xFF80D8FF), baseColor = Color(0xFF162530), textColor = Color.White,
                            onClick = { buzzerManager.playButtonBeep(); onPressA() }
                        )
                        "B" -> MoldedCircularButton(
                            label = "B", subLabel = "OK", size = railBtn,
                            rimColor = Color(0xFF69F0AE), baseColor = Color(0xFF142B1A), textColor = Color(0xFFB9F6CA),
                            onClick = { buzzerManager.playButtonBeep(); onPressB() }
                        )
                        else -> MoldedCircularButton(
                            label = "C", subLabel = "ESC", size = railBtn,
                            rimColor = Color(0xFFEA80FC), baseColor = Color(0xFF281432), textColor = Color(0xFFF3E5F5),
                            onClick = { buzzerManager.playButtonBeep(); onPressC() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreeButtonPetView(
    species: PetSpecies,
    petState: PetState,
    displayConfig: WatchDisplayConfig,
    buttonPlacement: String,
    buzzerManager: BuzzerManager,
    selectedMenuIndex: Int?,
    overrideSpriteFrame: SpriteFrame?,
    actionFeedback: String?,
    onSelectIcon: (Int) -> Unit,
    onPressA: () -> Unit,
    onPressB: () -> Unit,
    onPressC: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateStatus: () -> Unit,
    onNavigateAdventure: () -> Unit,
    onHatchTraitedEgg: () -> Unit = {},
    is24h: Boolean = false,
    adventureOverlayEnabled: Boolean = true,
    adventureProgress: AdventureProgress = AdventureProgress(),
    activeNode: CampaignNode? = null,
    isBossReady: Boolean = false
) {
    if (displayConfig.isRound) {
        RoundDialPetView(
            species = species,
            petState = petState,
            displayConfig = displayConfig,
            buttonPlacement = buttonPlacement,
            buzzerManager = buzzerManager,
            selectedMenuIndex = selectedMenuIndex,
            overrideSpriteFrame = overrideSpriteFrame,
            actionFeedback = actionFeedback,
            is24h = is24h,
            onSelectIcon = onSelectIcon,
            onPressA = onPressA,
            onPressB = onPressB,
            onPressC = onPressC,
            onNavigateSettings = onNavigateSettings,
            onNavigateStatus = onNavigateStatus,
            onNavigateAdventure = onNavigateAdventure,
            onHatchTraitedEgg = onHatchTraitedEgg,
            adventureOverlayEnabled = adventureOverlayEnabled,
            adventureProgress = adventureProgress,
            activeNode = activeNode,
            isBossReady = isBossReady
        )
        return
    }

    val attrColor = when (species.attribute.displayName) {
        "Vaccine" -> Color(0xFF00E5FF)
        "Data" -> Color(0xFFFFCA28)
        "Virus" -> Color(0xFFFF5252)
        else -> Color(0xFF69F0AE)
    }

    val buttonRail: @Composable () -> Unit = {
        BandaiButtonRail(
            displayConfig = displayConfig,
            onPressA = {
                buzzerManager.playButtonBeep()
                onPressA()
            },
            onPressB = {
                buzzerManager.playButtonBeep()
                onPressB()
            },
            onPressC = {
                buzzerManager.playButtonBeep()
                onPressC()
            }
        )
    }

    val topRail: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .background(Color(0x77091016), RoundedCornerShape(6.dp))
                .border(0.75.dp, Color(0x4480CBC4), RoundedCornerShape(6.dp))
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0..3) {
                val item = VPetMenu.entries[i]
                LcdIconCell(
                    index = i,
                    item = item,
                    isSelected = selectedMenuIndex == i,
                    isCallActive = false,
                    width = displayConfig.lcdIconWidth,
                    height = displayConfig.lcdIconHeight,
                    fontSize = displayConfig.lcdIconFontSize,
                    onClick = { onSelectIcon(i) }
                )
            }
        }
    }

    val bottomRail: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .background(Color(0x77091016), RoundedCornerShape(6.dp))
                .border(0.75.dp, Color(0x4480CBC4), RoundedCornerShape(6.dp))
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 4..7) {
                val item = VPetMenu.entries[i]
                val isAttentionLamp = (i == 7)
                val isCall = (isAttentionLamp && petState.attentionCallActive && !petState.lightsOff)
                LcdIconCell(
                    index = i,
                    item = item,
                    isSelected = if (isAttentionLamp) false else (selectedMenuIndex == i),
                    isCallActive = isCall,
                    width = displayConfig.lcdIconWidth,
                    height = displayConfig.lcdIconHeight,
                    fontSize = displayConfig.lcdIconFontSize,
                    onClick = if (isAttentionLamp) null else { { onSelectIcon(i) } }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(displayConfig.contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Recessed V-Pet LCD Rail
        topRail()

        // Middle Content Row with Center Viewport and Side Button Rail
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional Left Button Rail (Southpaw mode)
            if (buttonPlacement == "LEFT") {
                buttonRail()
            }

            // Central Recessed V-Pet LCD Console Viewport
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header telemetry: Name, Attribute badge, Weight, Quest & Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = species.name.take(if (displayConfig.screenWidthDp < 240) 8 else 11),
                                fontSize = displayConfig.telemetryFontSize,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB9F6CA),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .background(attrColor.copy(alpha = 0.22f), RoundedCornerShape(3.dp))
                                    .border(0.5.dp, attrColor, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 2.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = species.attribute.displayName.take(3).uppercase(),
                                    fontSize = (displayConfig.telemetryFontSize.value - 2f).coerceAtLeast(5.5f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = attrColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${petState.weight}g",
                                fontSize = (displayConfig.telemetryFontSize.value - 1f).coerceAtLeast(6.5f).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE0E0E0),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(if (displayConfig.screenWidthDp < 240) 16.dp else 19.dp)
                                    .background(Color(0x66004D40), CircleShape)
                                    .border(0.5.dp, Color(0xFF4DB6AC), CircleShape)
                                    .clickable { onNavigateAdventure() },
                                contentAlignment = Alignment.Center
                            ) {
                                DigiVectorIcon(
                                    type = DigiIconType.QUEST,
                                    size = if (displayConfig.screenWidthDp < 240) 10.dp else 12.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(if (displayConfig.screenWidthDp < 240) 16.dp else 19.dp)
                                    .background(Color(0x66263238), CircleShape)
                                    .border(0.5.dp, Color(0xFF90A4AE), CircleShape)
                                    .clickable { onNavigateSettings() },
                                contentAlignment = Alignment.Center
                            ) {
                                DigiVectorIcon(
                                    type = DigiIconType.SETTINGS,
                                    size = if (displayConfig.screenWidthDp < 240) 10.dp else 12.dp
                                )
                            }
                        }
                    }

                    // Framed Hunger & Strength LCD Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0x55000000), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "H:",
                                    fontSize = (displayConfig.telemetryFontSize.value - 0.5f).coerceAtLeast(6.5f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252),
                                    fontFamily = FontFamily.Monospace
                                )
                                VPetMeterBar(
                                    count = petState.hunger,
                                    isHeart = true,
                                    iconSize = (displayConfig.telemetryFontSize.value).coerceAtLeast(7f).dp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0x55000000), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "S:",
                                    fontSize = (displayConfig.telemetryFontSize.value - 0.5f).coerceAtLeast(6.5f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700),
                                    fontFamily = FontFamily.Monospace
                                )
                                VPetMeterBar(
                                    count = petState.strength,
                                    isHeart = false,
                                    iconSize = (displayConfig.telemetryFontSize.value).coerceAtLeast(7f).dp
                                )
                            }
                        }
                    }

                    if (petState.attentionCallActive && !petState.lightsOff) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xCCD50000), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 0.5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DigiVectorIcon(type = DigiIconType.ATTENTION, size = 8.dp, tint = Color.White)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "CALL SIGNAL",
                                    fontSize = (displayConfig.telemetryFontSize.value - 1.5f).coerceAtLeast(6f).sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Center Sprite Viewport (Takes available space, clickable for status) - NO BORDER OUTLINE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Authentic V-Pet floor waste (drawn before sprite so mon walks over it)
                        if (petState.wasteCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 12.dp, bottom = 4.dp)
                            ) {
                                VPetWasteRow(
                                    wasteCount = petState.wasteCount,
                                    iconSize = (displayConfig.telemetryFontSize.value * 2.2f).coerceIn(18f, 26f).dp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(displayConfig.mainSpriteSize)
                                .clickable { onNavigateStatus() },
                            contentAlignment = Alignment.Center
                        ) {
                            SpriteCanvasRenderer(
                                speciesId = petState.speciesId,
                                isAmbient = false,
                                overrideFrame = overrideSpriteFrame,
                                isSleeping = petState.isSleeping,
                                isInjured = petState.isInjured,
                                enableIdleRoaming = true,
                                size = displayConfig.mainSpriteSize
                            )
                        }
                    }

                    // Active Menu Prompt or Action Feedback Pill
                    if (petState.isDeceased) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xD9381216), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(4.dp))
                                .clickable { onHatchTraitedEgg() }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DigiVectorIcon(type = DigiIconType.TOMBSTONE, size = 9.dp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "REST IN PEACE • TAP TO HATCH EGG (GEN ${petState.generation + 1})",
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF8A80),
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val promptText = if (selectedMenuIndex != null) {
                            "[ ${VPetMenu.entries[selectedMenuIndex].label} ]"
                        } else actionFeedback ?: if (petState.isSleeping) "ZZZ... SLEEPING" else null

                        if (promptText != null) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0x66091016), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = promptText,
                                    fontSize = (displayConfig.telemetryFontSize.value - 0.5f).coerceAtLeast(6.5f).sp,
                                    color = if (selectedMenuIndex != null) Color(0xFFFFD54F) else Color(0xFFB9F6CA),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (adventureOverlayEnabled && activeNode != null && !petState.isFrozen) {
                            if (isBossReady) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xD9B71C1C), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFFFFD600), RoundedCornerShape(4.dp))
                                        .clickable { onNavigateAdventure() }
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "⚔️ S${adventureProgress.currentNodeIndex + 1} BOSS READY",
                                        fontSize = (displayConfig.telemetryFontSize.value - 0.5f).coerceAtLeast(6.5f).sp,
                                        color = Color(0xFFFFD600),
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x66004D40), RoundedCornerShape(4.dp))
                                        .border(0.75.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .clickable { onNavigateAdventure() }
                                        .padding(horizontal = 4.dp, vertical = 0.5.dp)
                                ) {
                                    Text(
                                        text = "🗺️ S${adventureProgress.currentNodeIndex + 1}: ${adventureProgress.accumulatedStepsCurrentNode}/${activeNode.requiredSteps}",
                                        fontSize = (displayConfig.telemetryFontSize.value - 0.5f).coerceAtLeast(6.5f).sp,
                                        color = Color(0xFFE0F2F1),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Button Rail (Authentic Bandai default)
                if (buttonPlacement == "RIGHT") {
                    buttonRail()
                }
            }

            // Bottom Recessed V-Pet LCD Rail (Centered on bottom circle chord)
            bottomRail()
        }
    }

@Composable
fun AmbientPetScreen(
    petState: PetState,
    displayConfig: WatchDisplayConfig = WatchDisplayConfig.current(),
    ambientStyle: String = "MINIMAL_DOT",
    is24hFormat: Boolean = false
) {
    val ambientState = LocalAmbientState.current
    val sdf = remember(is24hFormat) {
        SimpleDateFormat(if (is24hFormat) "HH:mm" else "hh:mm a", Locale.getDefault())
    }
    // Driven purely by the 1-minute hardware/ambient tick — zero background coroutine wakeups
    val currentTime = remember(ambientState.ambientTick, is24hFormat) { sdf.format(Date()) }

    // 4-quadrant ±2.dp pixel-shift cycle when OLED burn-in protection is active
    val shiftStep = (ambientState.ambientTick % 4L).toInt()
    val shiftX = if (ambientState.burnInProtection) {
        if (shiftStep == 0 || shiftStep == 3) (-2).dp else 2.dp
    } else 0.dp
    val shiftY = if (ambientState.burnInProtection) {
        if (shiftStep < 2) (-2).dp else 2.dp
    } else 0.dp

    val tintColor = when (ambientStyle) {
        "DIMMED_LCD" -> Color(0xFF80CBC4)
        "OUTLINE" -> Color(0xFFE0E0E0)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Minimum 10px boundary enforcement for OLED burn-in protection
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.offset(x = shiftX, y = shiftY),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTime,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = tintColor,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Monochrome Outline Sprite (OPR <= 15%)
            SpriteCanvasRenderer(
                speciesId = petState.speciesId,
                isAmbient = true,
                isSleeping = petState.isSleeping,
                isInjured = petState.isInjured,
                size = when (ambientStyle) {
                    "MINIMAL_DOT" -> if (displayConfig.isRound) 64.dp else 72.dp
                    else -> if (displayConfig.isRound) 76.dp else 88.dp
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            val statusLabel = when {
                petState.isDeceased -> "[RIP]"
                petState.isSleeping || petState.lightsOff -> "ZZZ"
                petState.attentionCallActive -> "[CALL]"
                ambientStyle == "DIMMED_LCD" -> "H:${petState.hunger}/4 S:${petState.strength}/4"
                else -> "OK"
            }

            Text(
                text = statusLabel,
                fontSize = 10.sp,
                color = tintColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
