package com.digiwear.wear.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.digiwear.core.engine.TrainingEngine
import com.digiwear.core.engine.TrainingResult
import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.CustomThemeConfig
import com.digiwear.core.model.ElementalAttribute
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import com.digiwear.core.model.SpriteFrame
import com.digiwear.wear.haptic.HapticManager
import com.digiwear.wear.input.HardwareInputManager
import com.digiwear.wear.input.VPetButton
import com.digiwear.wear.render.SpriteCanvasRenderer
import com.digiwear.wear.sound.BuzzerManager
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private enum class TrainingPhase {
    COUNTDOWN,
    INPUT,
    BARRAGE,
    RESOLVED
}

/**
 * Authentic Bandai Digital Monster Virtual Pet Training Minigame.
 * Faithfully recreates the iconic 5-round battle sparring drill with animated energy projectiles,
 * projectile trails, impact shockwaves, flying debris, and dynamically crumbling brick wall.
 * Supports both Bandai Ver. 2/20th Timing QTE and Digimon Pendulum Accelerometer Shake.
 * Seamlessly adapts theme aesthetics to match any background selected by the user.
 */
@Composable
fun TrainingMinigameView(
    petState: PetState,
    isPendulumShakeMode: Boolean,
    displayConfig: WatchDisplayConfig,
    hapticManager: HapticManager,
    buzzerManager: BuzzerManager,
    theme: BackgroundTheme = BackgroundTheme.CLASSIC_VPET_LCD,
    customConfig: CustomThemeConfig? = null,
    onFinished: (won: Boolean, message: String) -> Unit,
    onCancel: () -> Unit
) {
    val isLcdTheme = theme == BackgroundTheme.CLASSIC_VPET_LCD
    val overlayColor = when (theme) {
        BackgroundTheme.CLASSIC_VPET_LCD -> Color(0xAA6E7A52)
        BackgroundTheme.OLED_MINIMAL -> Color(0xEE000000)
        BackgroundTheme.CUSTOM_MATRIX -> Color(customConfig?.secondaryColorHex ?: 0xFF14241BL).copy(alpha = 0.85f)
        else -> Color(theme.secondaryColorHex).copy(alpha = 0.85f)
    }
    val dialTrackColor = when (theme) {
        BackgroundTheme.CLASSIC_VPET_LCD -> Color(0x441B2C1A)
        BackgroundTheme.OLED_MINIMAL -> Color(0x44FFFFFF)
        else -> Color(0x3300E5FF)
    }
    val context = LocalContext.current
    val species = remember(petState.speciesId) { PetSpecies.getById(petState.speciesId) }
    val optimalShakeRange = remember(species.attribute) { TrainingEngine.getOptimalShakeRange(species.attribute) }

    var phase by remember { mutableStateOf(TrainingPhase.COUNTDOWN) }
    var countdownValue by remember { mutableIntStateOf(3) }

    // QTE Power Gauge State
    var punchRegistered by remember { mutableStateOf(false) }
    var punchGauge by remember { mutableFloatStateOf(0f) }

    // Pendulum Shake State
    var shakeCount by remember { mutableIntStateOf(0) }
    var shakeTimeRemainingMs by remember { mutableIntStateOf(3500) }

    // Evaluated Result
    var trainingResult by remember { mutableStateOf<TrainingResult?>(null) }

    // Barrage Animation State (5 Rounds)
    var currentBarrageRound by remember { mutableIntStateOf(1) } // 1..5
    var roundProgress by remember { mutableFloatStateOf(0f) } // 0f..1f
    var hitsLandedSoFar by remember { mutableIntStateOf(0) }
    var roundResults by remember { mutableStateOf(BooleanArray(5)) } // true = hit, false = miss
    var impactShakeOffset by remember { mutableStateOf(Offset.Zero) }

    // Gauge oscillation (60fps draw-phase only)
    val animatedGaugeState = if (phase == TrainingPhase.INPUT && !isPendulumShakeMode) {
        val infiniteTransition = rememberInfiniteTransition(label = "trainingGauge")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 620, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "gaugeNeedle"
        )
    } else null

    // Palette & Colors based on theme & attribute
    val (projPrimary, projSecondary) = remember(species.attribute) {
        when (species.attribute) {
            ElementalAttribute.VACCINE -> Pair(Color(0xFFFFD54F), Color(0xFFFF7043)) // Solar Flame
            ElementalAttribute.DATA -> Pair(Color(0xFF00E676), Color(0xFF00E5FF))    // Cyber Matrix
            ElementalAttribute.VIRUS -> Pair(Color(0xFFFF1744), Color(0xFFD500F9))   // Dark Chaos
            else -> Pair(Color(0xFF80D8FF), Color(0xFFB388FF))                       // Astral Starlight
        }
    }

    // --- PHASE 1: COUNTDOWN LOGIC ---
    LaunchedEffect(Unit) {
        while (countdownValue > 1) {
            buzzerManager.playButtonBeep()
            hapticManager.playStepMilestone()
            delay(480L)
            countdownValue--
        }
        buzzerManager.playAttackImpactTone()
        hapticManager.playAttackImpact()
        delay(380L)
        phase = TrainingPhase.INPUT
    }

    // --- PHASE 2: INPUT DISPATCHERS ---
    val executeBarrage: (TrainingResult) -> Unit = { evalResult ->
        trainingResult = evalResult
        phase = TrainingPhase.BARRAGE
    }

    // QTE Trigger Handler
    val handleQtePunch: (Float) -> Unit = { gaugeVal ->
        if (!punchRegistered) {
            punchRegistered = true
            punchGauge = gaugeVal
            val res = TrainingEngine.evaluateQtePunch(gaugeVal)
            executeBarrage(res)
        }
    }

    // Pendulum Shake Timer
    LaunchedEffect(phase) {
        if (phase == TrainingPhase.INPUT && isPendulumShakeMode) {
            val stepMs = 50L
            while (shakeTimeRemainingMs > 0 && phase == TrainingPhase.INPUT) {
                delay(stepMs)
                shakeTimeRemainingMs = (shakeTimeRemainingMs - stepMs.toInt()).coerceAtLeast(0)
            }
            if (phase == TrainingPhase.INPUT) {
                val res = TrainingEngine.evaluateShakeTraining(species.attribute, shakeCount)
                executeBarrage(res)
            }
        }
    }

    // Hardware Accelerometer for Shake Mode
    DisposableEffect(phase) {
        if (phase == TrainingPhase.INPUT && isPendulumShakeMode) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            var lastShakeTimestamp = 0L

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null || phase != TrainingPhase.INPUT) return
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

                    val now = System.currentTimeMillis()
                    if (gForce > 1.85f && (now - lastShakeTimestamp) > 190L) {
                        lastShakeTimestamp = now
                        shakeCount++
                        buzzerManager.playQteClickTone()
                        hapticManager.playStepMilestone()
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager?.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        } else {
            onDispose {}
        }
    }

    // Hardware Stem Button Listener for Input and Skip
    DisposableEffect(phase) {
        HardwareInputManager.activeQteListener = { button ->
            when (phase) {
                TrainingPhase.INPUT -> {
                    if (!isPendulumShakeMode && (button == VPetButton.B || button == VPetButton.A)) {
                        handleQtePunch(animatedGaugeState?.value ?: 0f)
                        true
                    } else if (isPendulumShakeMode && (button == VPetButton.B || button == VPetButton.A)) {
                        shakeCount++
                        buzzerManager.playQteClickTone()
                        hapticManager.playStepMilestone()
                        true
                    } else if (button == VPetButton.C) {
                        onCancel()
                        true
                    } else false
                }
                TrainingPhase.BARRAGE -> {
                    if (button == VPetButton.C) {
                        // Instant Skip to Resolved (authentic Bandai Ver. 20th C-skip)
                        trainingResult?.let { res ->
                            hitsLandedSoFar = res.hits
                            val allRounds = BooleanArray(5) { i -> i < res.hits }
                            roundResults = allRounds
                            phase = TrainingPhase.RESOLVED
                        }
                        true
                    } else false
                }
                TrainingPhase.RESOLVED -> {
                    if (button == VPetButton.C || button == VPetButton.B || button == VPetButton.A) {
                        trainingResult?.let { onFinished(it.won, it.feedbackMessage) }
                        true
                    } else false
                }
                else -> false
            }
        }
        onDispose {
            HardwareInputManager.activeQteListener = null
        }
    }

    // --- PHASE 3: BARRAGE COMBAT ANIMATION COROUTINE (5 ROUNDS) ---
    LaunchedEffect(phase) {
        if (phase == TrainingPhase.BARRAGE && trainingResult != null) {
            val totalHits = trainingResult!!.hits // 0..5
            val roundOutcomes = BooleanArray(5) { roundIdx -> roundIdx < totalHits }

            for (r in 1..5) {
                currentBarrageRound = r
                val isThisRoundHit = roundOutcomes[r - 1]

                // Round progression: 340ms total per round
                val steps = 14
                val stepDuration = 24L
                for (s in 0..steps) {
                    roundProgress = s.toFloat() / steps.toFloat()
                    delay(stepDuration)
                }

                // Impact moment at round conclusion
                if (isThisRoundHit) {
                    hitsLandedSoFar++
                    roundResults = roundResults.clone().apply { this[r - 1] = true }
                    impactShakeOffset = Offset(
                        x = Random.nextInt(-4, 5).toFloat(),
                        y = Random.nextInt(-3, 4).toFloat()
                    )
                    buzzerManager.playAttackImpactTone()
                    hapticManager.playAttackImpact()
                } else {
                    roundResults = roundResults.clone().apply { this[r - 1] = false }
                    impactShakeOffset = Offset.Zero
                    buzzerManager.playButtonBeep()
                }

                delay(90L)
                impactShakeOffset = Offset.Zero
            }

            // Final Impact / Smashed flourish
            if (trainingResult!!.isMegaHit) {
                buzzerManager.playCriticalCutInTone()
                hapticManager.playAttackImpact()
                impactShakeOffset = Offset(0f, 3f)
                delay(150L)
                impactShakeOffset = Offset.Zero
            }

            delay(200L)
            phase = TrainingPhase.RESOLVED

            // Play final fanfare or defeat alert
            if (trainingResult!!.won) {
                buzzerManager.playVictoryFanfare()
                hapticManager.playVictoryFanfare()
            } else {
                buzzerManager.playAttentionBeep()
                hapticManager.playAttentionAlert()
            }

            // Auto-finish after 2.4 seconds if user doesn't tap
            delay(2400L)
            if (phase == TrainingPhase.RESOLVED) {
                trainingResult?.let { onFinished(it.won, it.feedbackMessage) }
            }
        }
    }

    // UI CONTAINER
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayColor)
            .clickable {
                when (phase) {
                    TrainingPhase.INPUT -> {
                        if (!isPendulumShakeMode) {
                            handleQtePunch(animatedGaugeState?.value ?: 0f)
                        } else {
                            shakeCount++
                            buzzerManager.playQteClickTone()
                            hapticManager.playStepMilestone()
                        }
                    }
                    TrainingPhase.BARRAGE -> {
                        // Instant Skip to Resolved
                        trainingResult?.let { res ->
                            hitsLandedSoFar = res.hits
                            roundResults = BooleanArray(5) { i -> i < res.hits }
                            phase = TrainingPhase.RESOLVED
                        }
                    }
                    TrainingPhase.RESOLVED -> {
                        trainingResult?.let { onFinished(it.won, it.feedbackMessage) }
                    }
                    else -> {}
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Subtle circular boundary track for round displays
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width / 2f) * 0.96f
            drawCircle(
                color = dialTrackColor,
                radius = radius,
                center = center,
                style = Stroke(width = 1.5f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = displayConfig.horizontalPadding, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ========================================================
            // 1. TOP HEADER & 5-ROUND ATTEMPT TALLY (Safe Arc)
            // ========================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isPendulumShakeMode) "PENDULUM SHAKE" else "TRAINING • SMASH 5",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isLcdTheme) Color(0xFF142012) else Color(0xFFFFD54F)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 5-Attempt Hit Tally Badges (● = Hit, ✕ = Miss, ○ = Upcoming)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 5) {
                        val isPast = when (phase) {
                            TrainingPhase.RESOLVED -> true
                            TrainingPhase.BARRAGE -> i < currentBarrageRound
                            else -> false
                        }
                        val isHit = roundResults[i]
                        val isCurrent = phase == TrainingPhase.BARRAGE && i == currentBarrageRound - 1

                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .background(
                                    when {
                                        isPast && isHit -> if (isLcdTheme) Color(0xFF142012) else Color(0xFF00E676)
                                        isPast && !isHit -> if (isLcdTheme) Color(0x55142012) else Color(0xFFFF5252)
                                        isCurrent -> if (isLcdTheme) Color(0xFF384628) else Color(0xFFFFD54F)
                                        else -> if (isLcdTheme) Color(0x22142012) else Color(0xFF263238)
                                    },
                                    CircleShape
                                )
                                .border(
                                    0.75.dp,
                                    if (isCurrent) Color.White else (if (isLcdTheme) Color(0xFF142012) else Color(0x66FFFFFF)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    isPast && isHit -> "●"
                                    isPast && !isHit -> "✕"
                                    isCurrent -> "▶"
                                    else -> "${i + 1}"
                                },
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    isPast && isHit -> Color.Black
                                    isPast && !isHit -> Color.White
                                    isCurrent -> Color.Black
                                    else -> if (isLcdTheme) Color(0xFF142012) else Color.LightGray
                                },
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // ========================================================
            // 2. CENTER BATTLE ARENA (Partner vs Brick Wall)
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp),
                contentAlignment = Alignment.Center
            ) {
                // Determine active animation frame for Digimon
                val activeSpriteFrame = when (phase) {
                    TrainingPhase.COUNTDOWN -> SpriteFrame.IDLE_A
                    TrainingPhase.INPUT -> {
                        if (isPendulumShakeMode) {
                            if (shakeCount % 2 == 0) SpriteFrame.ATTACK_CHARGE else SpriteFrame.IDLE_B
                        } else SpriteFrame.IDLE_A
                    }
                    TrainingPhase.BARRAGE -> {
                        when {
                            roundProgress < 0.25f -> SpriteFrame.ATTACK_CHARGE
                            roundProgress < 0.70f -> SpriteFrame.ATTACK_RELEASE
                            else -> SpriteFrame.IDLE_B
                        }
                    }
                    TrainingPhase.RESOLVED -> {
                        if (trainingResult?.won == true) SpriteFrame.JOY_EAT else SpriteFrame.INJURED
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT: Partner Digimon (flipped horizontally to face right towards wall)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .offset(
                                y = if (phase == TrainingPhase.RESOLVED && trainingResult?.won == true) {
                                    // Bouncing victory celebration hop
                                    ((-kotlin.math.abs(sin(System.currentTimeMillis() / 150.0)) * 6.0)).dp
                                } else 0.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        SpriteCanvasRenderer(
                            speciesId = petState.speciesId,
                            isAmbient = false,
                            overrideFrame = activeSpriteFrame,
                            size = 44.dp,
                            flipHorizontal = true
                        )
                    }

                    // RIGHT: Retro Brick Wall Target
                    RetroBrickWallCanvas(
                        hitsLanded = hitsLandedSoFar,
                        shakeOffset = impactShakeOffset,
                        isMegaHit = trainingResult?.isMegaHit == true && phase == TrainingPhase.RESOLVED,
                        theme = theme,
                        customConfig = customConfig,
                        modifier = Modifier
                            .width(30.dp)
                            .height(64.dp)
                    )
                }

                // CENTER: Dynamic In-Flight Projectile & Impact Clash FX
                if (phase == TrainingPhase.BARRAGE) {
                    val isCurrentHit = currentBarrageRound <= (trainingResult?.hits ?: 0)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        val startX = w * 0.28f
                        val endX = w * 0.76f
                        val centerY = h * 0.52f

                        if (roundProgress < 0.70f) {
                            // In-flight projectile phase
                            val flightProgress = (roundProgress / 0.70f).coerceIn(0f, 1f)
                            val currentX = startX + (endX - startX) * flightProgress
                            val waveY = if (isCurrentHit) {
                                centerY + sin(flightProgress * 12.56f) * 4f
                            } else {
                                // Curved trajectory heading off-target
                                centerY - flightProgress * 22f
                            }

                            val coreRadius = if (trainingResult?.isMegaHit == true && currentBarrageRound == 5) 9f else 5.5f

                            // Particle trails
                            for (p in 1..4) {
                                val trailOffset = -p * 8f
                                val trailAlpha = (1f - p * 0.22f).coerceAtLeast(0.1f)
                                drawCircle(
                                    color = projSecondary.copy(alpha = trailAlpha * 0.8f),
                                    radius = (coreRadius * (1f - p * 0.18f)).coerceAtLeast(1.5f),
                                    center = Offset(currentX + trailOffset, waveY)
                                )
                            }

                            // Glowing Core
                            drawCircle(color = projPrimary, radius = coreRadius, center = Offset(currentX, waveY))
                            drawCircle(color = Color.White, radius = coreRadius * 0.5f, center = Offset(currentX, waveY))
                        } else {
                            // Impact moment on brick wall (0.70f .. 1.0f)
                            val impactPct = ((roundProgress - 0.70f) / 0.30f).coerceIn(0f, 1f)
                            if (isCurrentHit) {
                                val maxRadius = if (trainingResult?.isMegaHit == true && currentBarrageRound == 5) 28f else 18f
                                val curRadius = maxRadius * impactPct
                                val alpha = (1f - impactPct).coerceIn(0f, 1f)

                                // Expanding Shockwave Ring
                                drawCircle(
                                    color = projPrimary.copy(alpha = alpha),
                                    radius = curRadius,
                                    center = Offset(endX, centerY),
                                    style = Stroke(width = 2.5f * (1f - impactPct).coerceAtLeast(0.5f))
                                )

                                // 8-Point Starburst Sparks
                                val sparkDist = curRadius * 1.3f
                                for (angle in 0 until 360 step 45) {
                                    val rad = Math.toRadians(angle.toDouble())
                                    val sx = endX + (cos(rad) * sparkDist).toFloat()
                                    val sy = centerY + (sin(rad) * sparkDist).toFloat()
                                    val sparkLen = 4f * (1f - impactPct)
                                    drawLine(
                                        color = projSecondary.copy(alpha = alpha),
                                        start = Offset(
                                            endX + (cos(rad) * (sparkDist - sparkLen)).toFloat(),
                                            centerY + (sin(rad) * (sparkDist - sparkLen)).toFloat()
                                        ),
                                        end = Offset(sx, sy),
                                        strokeWidth = 2f
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ========================================================
            // 3. BOTTOM CONTROL / PROMPT DECK (Safe Arc - fitted for round watch displays)
            // ========================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (phase) {
                    TrainingPhase.COUNTDOWN -> {
                        Text(
                            text = "GET READY... $countdownValue",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLcdTheme) Color(0xFF142012) else Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isPendulumShakeMode) "Prepare to shake wrist!" else "Time your power punch!",
                            fontSize = 7.sp,
                            color = if (isLcdTheme) Color(0xFF283620) else Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    TrainingPhase.INPUT -> {
                        if (!isPendulumShakeMode) {
                            // --- RETRO OSCILLATING POWER GAUGE (Bandai Ver. 2/20th) ---
                            Row(
                                modifier = Modifier.fillMaxWidth(0.72f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PUNCH POWER",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLcdTheme) Color(0xFF142012) else Color(0xFF80D8FF),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "SWEET SPOT 75-100%",
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLcdTheme) Color(0xFF142012) else Color(0xFF00E676),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Dynamic Oscillating Meter Bar (0 layout passes)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(11.dp)
                                    .background(if (isLcdTheme) Color(0x44142012) else Color(0xFF263238), RoundedCornerShape(5.dp))
                                    .border(1.dp, if (isLcdTheme) Color(0xFF142012) else Color.White, RoundedCornerShape(5.dp))
                            ) {
                                // Sweet spot highlight marker (75% to 100%)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.25f)
                                        .fillMaxHeight()
                                        .align(Alignment.CenterEnd)
                                        .background(
                                            if (isLcdTheme) Color(0x66142012) else Color(0x6600E676),
                                            RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)
                                        )
                                )

                                // Sweeping Indicator Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawWithContent {
                                            val gauge = animatedGaugeState?.value ?: 0f
                                            val barColor = when {
                                                gauge >= 0.75f -> if (isLcdTheme) Color(0xFF142012) else Color(0xFF00E676)
                                                gauge >= 0.40f -> if (isLcdTheme) Color(0xFF2E462C) else Color(0xFFFFD54F)
                                                else -> if (isLcdTheme) Color(0xFF556644) else Color(0xFFFF5252)
                                            }
                                            val r = 5.dp.toPx()
                                            drawRoundRect(
                                                color = barColor,
                                                size = Size(size.width * gauge.coerceIn(0f, 1f), size.height),
                                                cornerRadius = CornerRadius(r, r)
                                            )
                                        }
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "PRESS [B] / STEM OR TAP",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLcdTheme) Color(0xFF142012) else Color(0xFFFFD54F),
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            // --- PENDULUM ACCELEROMETER SHAKE METER ---
                            Row(
                                modifier = Modifier.fillMaxWidth(0.72f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SHAKES: [ $shakeCount ]",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (shakeCount in optimalShakeRange) (if (isLcdTheme) Color(0xFF142012) else Color(0xFF00E676)) else (if (isLcdTheme) Color(0xFF283620) else Color(0xFFFFD54F)),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "TARGET: ${optimalShakeRange.first}-${optimalShakeRange.last}",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLcdTheme) Color(0xFF142012) else Color(0xFF80D8FF),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Shake Countdown Timer Bar (3500ms)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(7.dp)
                                    .background(if (isLcdTheme) Color(0x44142012) else Color(0xFF263238), RoundedCornerShape(3.5.dp))
                                    .border(0.5.dp, if (isLcdTheme) Color(0xFF142012) else Color.White, RoundedCornerShape(3.5.dp))
                            ) {
                                val progress = (shakeTimeRemainingMs / 3500f).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .background(
                                            if (shakeCount in optimalShakeRange) (if (isLcdTheme) Color(0xFF142012) else Color(0xFF00E676)) else (if (isLcdTheme) Color(0xFF2E462C) else Color(0xFFFFD54F)),
                                            RoundedCornerShape(3.5.dp)
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SHAKE WATCH OR TAP [B]",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLcdTheme) Color(0xFF142012) else Color(0xFFFFD54F),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    TrainingPhase.BARRAGE -> {
                        Text(
                            text = "ATTACKING! ROUND $currentBarrageRound / 5",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLcdTheme) Color(0xFF142012) else Color(0xFF00E5FF),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "TAP OR [C] TO FAST-FORWARD",
                            fontSize = 7.sp,
                            color = if (isLcdTheme) Color(0xFF283620) else Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    TrainingPhase.RESOLVED -> {
                        val won = trainingResult?.won == true
                        val isMega = trainingResult?.isMegaHit == true
                        val hits = trainingResult?.hits ?: 0

                        Box(
                            modifier = Modifier
                                .background(
                                    when {
                                        isMega -> if (isLcdTheme) Color(0xFF142012) else Color(0xFF2E7D32)
                                        won -> if (isLcdTheme) Color(0xFF283620) else Color(0xFF1B5E20)
                                        else -> if (isLcdTheme) Color(0x66142012) else Color(0xFFB71C1C)
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isMega) Color(0xFFFFD54F) else (if (won) Color(0xFF00E676) else Color(0xFFFF5252)),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when {
                                    isMega -> "[ 5/5 MEGA HIT! ] +1 EFFORT"
                                    won -> "[ $hits/5 SMASHED! ] +1 EFFORT"
                                    else -> "[ $hits/5 MISS! ] TRY AGAIN"
                                },
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isMega) Color(0xFFFFD54F) else Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (won) "Strength +1 • Weight -1g • +25 VP" else "Tap [B] or [C] to exit",
                            fontSize = 7.sp,
                            color = if (isLcdTheme) Color(0xFF142012) else Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
