package com.digiwear.wear.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.SpriteFrame
import com.digiwear.core.workout.HeartRateZone
import com.digiwear.core.workout.WorkoutEngine
import com.digiwear.core.workout.WorkoutSummary
import com.digiwear.core.workout.WorkoutType
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.haptic.HapticManager
import com.digiwear.wear.render.SpriteCanvasRenderer
import com.digiwear.wear.sensor.HeartRateTracker
import com.digiwear.wear.sound.BuzzerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun WorkoutScreen(
    repository: PetRepository,
    hapticManager: HapticManager,
    buzzerManager: BuzzerManager,
    workoutType: WorkoutType = WorkoutType.FREE_TRAINING,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val petState by repository.petStateFlow.collectAsState(initial = com.digiwear.core.model.PetState())
    val progress by repository.adventureStateFlow.collectAsState(initial = com.digiwear.core.model.AdventureProgress())
    val campaign = remember { repository.getActiveCampaign() }
    val species = remember(petState.speciesId) { PetSpecies.getById(petState.speciesId) }

    val tracker = remember { HeartRateTracker(context, scope) }
    val currentBpm by tracker.currentBpm.collectAsState()

    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var sessionSteps by remember { mutableIntStateOf(0) }
    var hardwareStepsInSession by remember { mutableIntStateOf(0) }
    var lastObservedAdventureSteps by remember { mutableIntStateOf(-1) }
    var isRunning by remember { mutableStateOf(true) }
    var summary by remember { mutableStateOf<WorkoutSummary?>(null) }

    LaunchedEffect(progress.totalAdventureSteps, isRunning) {
        if (!isRunning) return@LaunchedEffect
        val total = progress.totalAdventureSteps
        if (lastObservedAdventureSteps < 0) {
            lastObservedAdventureSteps = total
        } else if (total > lastObservedAdventureSteps) {
            val delta = total - lastObservedAdventureSteps
            sessionSteps += delta
            hardwareStepsInSession += delta
            lastObservedAdventureSteps = total
        }
    }

    fun finishWorkoutSession() {
        if (!isRunning && summary != null) {
            hapticManager.playStepMilestone()
            onFinish()
            return
        }
        isRunning = false
        tracker.stopListening()
        hapticManager.playStepMilestone()
        buzzerManager.playVictoryFanfare()

        val samples = tracker.getSamples()
        val (updatedState, resSummary) = WorkoutEngine.finishSession(
            currentState = petState,
            type = workoutType,
            durationSeconds = elapsedSeconds,
            rawSteps = sessionSteps,
            bpmSamples = samples
        )
        val evolvedSpecies = com.digiwear.core.engine.EvolutionEngine.evaluateEvolution(updatedState)
        val finalState = if (evolvedSpecies != null) {
            com.digiwear.core.engine.EvolutionEngine.evolve(updatedState, evolvedSpecies, System.currentTimeMillis() / 1000L)
        } else {
            updatedState
        }

        val additionalAdventureSteps = (resSummary.effectiveAdventureSteps - hardwareStepsInSession).coerceAtLeast(0)
        scope.launch {
            repository.savePetState(finalState)
            if (workoutType == WorkoutType.EXPEDITION_TREK && additionalAdventureSteps > 0) {
                repository.addAdventureSteps(additionalAdventureSteps)
            }
        }
        summary = resSummary
    }

    LaunchedEffect(Unit) {
        com.digiwear.wear.input.HardwareInputManager.buttonEvents.collect { button ->
            if (button == com.digiwear.wear.input.VPetButton.B || button == com.digiwear.wear.input.VPetButton.C) {
                finishWorkoutSession()
            }
        }
    }

    // Heartbeat pulse animation
    val pulseTransition = rememberInfiniteTransition(label = "HeartPulse")
    val heartScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (currentBpm > 120) 1.35f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (currentBpm > 0) (60_000 / currentBpm.coerceAtLeast(60)).coerceIn(300, 1000) else 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Active training stance toggle (2Hz)
    var isStanceB by remember { mutableStateOf(false) }
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(500L)
            isStanceB = !isStanceB
        }
    }

    // Workout timer & heart-rate cadence fallback when hardware step sensor is idle/unavailable
    LaunchedEffect(isRunning) {
        tracker.startListening()
        while (isRunning) {
            val stepsBeforeTick = sessionSteps
            delay(1000L)
            elapsedSeconds++
            if (sessionSteps == stepsBeforeTick && currentBpm > 0) {
                val cadence = when {
                    currentBpm >= 140 -> 3
                    currentBpm >= 100 -> 2
                    else -> 1
                }
                sessionSteps += cadence
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tracker.stopListening()
        }
    }

    val hrZone = remember(currentBpm) { HeartRateZone.fromBpm(currentBpm) }
    val zoneColor = Color(hrZone.hexColor)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B0E)),
        contentAlignment = Alignment.Center
    ) {
        if (summary == null) {
            val displayConfig = WatchDisplayConfig.current()
            val horizInset = if (displayConfig.isRound) 30.dp else 8.dp
            val topInset = if (displayConfig.isRound) 26.dp else 6.dp

            // Active Workout HUD
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizInset, vertical = topInset),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: Type & Zone Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (displayConfig.isRound) 24.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = workoutType.displayName.uppercase(Locale.US),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )

                    Box(
                        modifier = Modifier
                            .background(zoneColor.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .border(1.dp, zoneColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = hrZone.label,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = zoneColor
                        )
                    }
                }

                // Center: Animated Partner Sprite & Heart Rate Gauge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exercising Sprite
                    Box(contentAlignment = Alignment.TopEnd) {
                        SpriteCanvasRenderer(
                            speciesId = petState.speciesId,
                            isAmbient = false,
                            overrideFrame = if (workoutType == WorkoutType.SHADOW_BOXING) {
                                if (isStanceB) SpriteFrame.ATTACK_RELEASE else SpriteFrame.ATTACK_CHARGE
                            } else {
                                if (isStanceB) SpriteFrame.WALK_B else SpriteFrame.WALK_A
                            },

                            size = 72.dp
                        )
                        DigiVectorIcon(
                            type = DigiIconType.SWEAT,
                            size = 13.dp,
                            tint = Color(0xFF40C4FF),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // BPM Gauge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DigiVectorIcon(
                            type = DigiIconType.HEART_FULL,
                            size = 18.dp,
                            tint = zoneColor,
                            modifier = Modifier.scale(heartScale)
                        )
                        Text(
                            text = if (currentBpm > 0) "$currentBpm" else "--",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = "BPM",
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    }
                }

                // Middle: Telemetry Bar (Duration, Calories, Steps)
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                val timeStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)
                val estCalories = WorkoutEngine.computeEstimatedCalories(currentBpm, elapsedSeconds)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111822), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TIME", fontSize = 6.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                        Text(timeStr, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CAL", fontSize = 6.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                        Text("$estCalories", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300), fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("STEPS", fontSize = 6.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                        Text("$sessionSteps", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676), fontFamily = FontFamily.Monospace)
                    }
                }

                // If Adventure-linked: Sector Goal Progress
                if (workoutType == WorkoutType.EXPEDITION_TREK) {
                    val activeNodeIndex = progress.currentNodeIndex.coerceIn(0, (campaign.nodes.size - 1).coerceAtLeast(0))
                    val activeNode = campaign.nodes.getOrNull(activeNodeIndex)
                    val target = activeNode?.requiredSteps ?: 1000
                    val currentSteps = progress.accumulatedStepsCurrentNode
                    val progressRatio = (currentSteps.toFloat() / target.toFloat()).coerceIn(0f, 1f)


                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "EXPEDITION: ${activeNode?.name ?: "Sector"}",
                            fontSize = 6.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF80D8FF),
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(3.dp)
                                .background(Color(0xFF223040), RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressRatio)
                                    .height(3.dp)
                                    .background(Color(0xFF00E5FF), RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }

                // Bottom: Finish Workout Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (displayConfig.isRound) 0.76f else 0.86f)
                        .height(28.dp)
                        .background(Color(0xFFD50000), RoundedCornerShape(14.dp))
                        .clickable { finishWorkoutSession() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[STOP] FINISH WORKOUT",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Summary Completion Screen
            val res = summary!!
            val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)

            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                state = listState,
                autoCentering = null,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = "WORKOUT COMPLETE!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E676),
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    SpriteCanvasRenderer(
                        speciesId = petState.speciesId,
                        isAmbient = false,
                        overrideFrame = SpriteFrame.IDLE_A,
                        size = 44.dp
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111822), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DURATION", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Text("${res.durationSeconds / 60}m ${res.durationSeconds % 60}s", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("AVG HR", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (res.averageBpm > 0) "${res.averageBpm} BPM" else "-- BPM",
                                fontSize = 8.sp,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CALORIES", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Text("${res.estimatedCalories} kcal", fontSize = 8.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                         Row(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Text("EFFORT", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                             Row(
                                 verticalAlignment = Alignment.CenterVertically,
                                 horizontalArrangement = Arrangement.spacedBy(3.dp)
                             ) {
                                 VPetMeterBar(
                                     filledCount = petState.effort.coerceIn(0, 4),
                                     maxCount = 4,
                                     isHearts = false,
                                     activeColor = Color(0xFFFFD54F),
                                     emptyColor = Color(0x55FFD54F),
                                     iconSize = 8.dp
                                 )
                                 Text("(+${res.effortGained})", fontSize = 8.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                             }
                         }
                        if (res.weightGramsBurned > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("WEIGHT BURN", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text("-${res.weightGramsBurned}g", fontSize = 8.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        if (res.evolutionMinutesAccelerated > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("EVO BOOST", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text("+${res.evolutionMinutesAccelerated}m Growth", fontSize = 8.sp, color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        if (workoutType == WorkoutType.EXPEDITION_TREK) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ADV STEPS", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text("+${res.effectiveAdventureSteps}", fontSize = 8.sp, color = Color(0xFF80D8FF), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            hapticManager.playStepMilestone()
                            onFinish()
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(30.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text(
                            text = "CONFIRM & EXIT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
