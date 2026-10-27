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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Text
import androidx.compose.ui.platform.LocalContext
import com.digiwear.core.battle.DeterministicBattleSolver
import com.digiwear.core.battle.WildScanEncounter
import com.digiwear.core.battle.WildScanEncounterGenerator
import com.digiwear.core.codec.CommCodeCodec
import com.digiwear.core.qrcode.QrMatrixGenerator
import com.digiwear.core.fusion.JogressEngine
import com.digiwear.core.model.BattleFighter
import com.digiwear.core.model.BattleMode
import com.digiwear.core.model.BattleResult
import com.digiwear.core.model.ElementalAttribute
import com.digiwear.core.model.PetSpecies
import com.digiwear.wear.input.HardwareInputManager
import com.digiwear.wear.input.VPetButton
import com.digiwear.core.model.PetStage
import com.digiwear.core.model.PetState
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.haptic.HapticManager
import com.digiwear.wear.render.SpriteCanvasRenderer
import com.digiwear.wear.render.SpriteFrame
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

@Composable
fun BattleScreen(
    repository: PetRepository,
    hapticManager: HapticManager,
    buzzerManager: com.digiwear.wear.sound.BuzzerManager = remember { com.digiwear.wear.sound.BuzzerManager() },
    onBack: () -> Unit
) {
    val petState by repository.petStateFlow.collectAsState(initial = PetState())
    val backupPet by repository.backupPetStateFlow.collectAsState(initial = null)
    val isSoundEnabled by repository.soundEnabledFlow.collectAsState(initial = false)
    val nfcBridgeMode by repository.nfcBridgeModeFlow.collectAsState(initial = "NFC_DUEL")
    val displayConfig = WatchDisplayConfig.current()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val species = remember(petState.speciesId) { PetSpecies.getById(petState.speciesId) }

    LaunchedEffect(isSoundEnabled) {
        buzzerManager.isEnabled = isSoundEnabled
    }

    // Generate own 128-bit Comm Code and 0nl1ne DigiROM packet
    val ownSeed = remember { Random.nextInt(0xFFFF) }
    val ownCode = remember(petState) {
        CommCodeCodec.encode128(
            monsterId = petState.speciesId,
            stage = species.stage,
            attribute = species.attribute,
            basePowerHp = species.basePower,
            historicalVictories = petState.victories,
            trainingEffort = petState.effort,
            battleRngSeed = ownSeed
        )
    }
    val ownDigiRom = remember(petState) {
        CommCodeCodec.encodeDigiRom(
            monsterId = petState.speciesId,
            stage = species.stage,
            attribute = species.attribute,
            basePowerHp = species.basePower,
            battleRngSeed = ownSeed
        )
    }

    val ownQrMatrix = remember(ownCode) {
        try {
            QrMatrixGenerator.generateQrMatrix(ownCode)
        } catch (_: Exception) {
            emptyArray()
        }
    }
    var showQrMatrix by remember { mutableStateOf(false) }

    val beamedCode by repository.beamedCommCodeFlow.collectAsState()
    var selectedBattleMode by remember {
        mutableStateOf(
            when (nfcBridgeMode) {
                "WILD_SCAN" -> BattleMode.WILD_SCAN
                "ACOM_RELAY" -> BattleMode.ACOM_DCOM_WIFICOM
                else -> BattleMode.NFC_TAP_DUEL
            }
        )
    }
    var wildEncounter by remember { mutableStateOf<WildScanEncounter?>(null) }
    var opponentCodeInput by remember { mutableStateOf("") }
    var battleResult by remember { mutableStateOf<BattleResult?>(null) }
    var activeEnvEffect by remember { mutableStateOf("none") }
    var currentTurnIndex by remember { mutableIntStateOf(0) }
    var isBattling by remember { mutableStateOf(false) }
    var battleStatusText by remember { mutableStateOf<String?>(null) }
    var showManualKeypad by remember { mutableStateOf(false) }
    var keypadPageIndex by remember { mutableIntStateOf(0) }

    val resultListState = rememberScalingLazyListState()
    val hubListState = rememberScalingLazyListState()

    var qteActive by remember { mutableStateOf(false) }
    var turnFxProgress by remember { mutableFloatStateOf(0f) }
    var criticalBoostActive by remember { mutableStateOf(false) }
    var activeCutInSpeciesId by remember { mutableStateOf<Int?>(null) }
    var activeCutInTitle by remember { mutableStateOf("CRITICAL HIT") }
    var encounterFlashEnemyId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(beamedCode) {
        val incoming = beamedCode
        if (!incoming.isNullOrBlank()) {
            if (selectedBattleMode == BattleMode.WILD_SCAN) {
                val enc = WildScanEncounterGenerator.generateFromScan(
                    rawScanData = incoming,
                    scanSource = "NFC_TAG",
                    playerStage = species.stage
                )
                wildEncounter = enc
                opponentCodeInput = enc.commCode128
                battleStatusText = "[SCAN] Wild ${enc.species.name} (${enc.habitatZone}) Scanned!"
            } else {
                opponentCodeInput = incoming
                battleStatusText = if (incoming.startsWith("V1-")) "[NFC] Beamed: DigiROM Loaded!" else "[NFC] Beamed: Comm Code Loaded!"
            }
            hapticManager.playStepMilestone()
            repository.setBeamedCommCode(null)
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            HardwareInputManager.activeQteListener = null
        }
    }

    androidx.activity.compose.BackHandler(
        enabled = showQrMatrix || showManualKeypad || (battleResult != null && !isBattling)
    ) {
        buzzerManager.playButtonBeep()
        when {
            showQrMatrix -> showQrMatrix = false
            showManualKeypad -> showManualKeypad = false
            battleResult != null && !isBattling -> battleResult = null
        }
    }

    LaunchedEffect(Unit) {
        HardwareInputManager.buttonEvents.collect { button ->
            when (button) {
                VPetButton.C -> {
                    when {
                        showQrMatrix -> showQrMatrix = false
                        showManualKeypad -> showManualKeypad = false
                        battleResult != null && !isBattling -> battleResult = null
                        !isBattling -> onBack()
                    }
                }
                VPetButton.B -> {
                    if (battleResult != null && !isBattling) {
                        battleResult = null
                    }
                }
                else -> {}
            }
        }
    }

    fun startBattleWithCode(code: String, mode: BattleMode, envEffect: String) {
        if (petState.isDeceased) {
            battleStatusText = "[RIP] Partner has passed away."
            buzzerManager.playAttentionBeep()
            return
        }
        if (petState.isInjured) {
            battleStatusText = "[INJURED] Cure with [MEDS] first!"
            buzzerManager.playAttentionBeep()
            hapticManager.playAttentionAlert()
            return
        }
        try {
            activeEnvEffect = envEffect
            val opponentPayload = CommCodeCodec.decodeAuto(code)
            val oppSpecies = PetSpecies.getById(opponentPayload.monsterId)
            repository.recordDiscoveredSpecies(oppSpecies.id)

            val fighterA = BattleFighter(
                id = "player",
                speciesId = petState.speciesId,
                name = species.name,
                stage = species.stage,
                attribute = species.attribute,
                maxHp = petState.maxHp,
                currentHp = petState.maxHp,
                basePower = species.basePower,
                victories = petState.victories,
                effort = petState.effort,
                strength = petState.strength,
                weight = petState.weight,
                battleSeed = ownSeed
            )

            val fighterB = BattleFighter(
                id = "opponent",
                speciesId = opponentPayload.monsterId,
                name = oppSpecies.name,
                stage = opponentPayload.stage,
                attribute = opponentPayload.attribute,
                maxHp = oppSpecies.baseHp,
                currentHp = oppSpecies.baseHp,
                basePower = opponentPayload.basePowerHp.coerceAtLeast(oppSpecies.basePower),
                victories = opponentPayload.historicalVictories,
                effort = opponentPayload.trainingEffort,
                battleSeed = opponentPayload.battleRngSeed
            )

            val baseResult = DeterministicBattleSolver.resolveBattle(
                fighterA = fighterA,
                fighterB = fighterB,
                environmentEffect = envEffect,
                attackerCritBoost = false,
                battleMode = mode
            )
            val critResult = DeterministicBattleSolver.resolveBattle(
                fighterA = fighterA,
                fighterB = fighterB,
                environmentEffect = envEffect,
                attackerCritBoost = true,
                battleMode = mode
            )
            var activeResult = baseResult
            battleResult = activeResult
            isBattling = true
            battleStatusText = null

            scope.launch {
                // Dramatic Encounter Flash Sequence
                encounterFlashEnemyId = oppSpecies.id
                buzzerManager.playEncounterAlarm()
                hapticManager.playAttentionAlert()
                delay(1050L)
                encounterFlashEnemyId = null

                var i = 0
                while (i < activeResult.turns.size) {
                    currentTurnIndex = i
                    val initialTurn = activeResult.turns[i]
                    criticalBoostActive = false

                    if (initialTurn.attackerIndex == 0) {
                        // Player attack turn: launch Quick Time Event!
                        qteActive = true
                        HardwareInputManager.activeQteListener = { button ->
                            if (button == VPetButton.B || button == VPetButton.A) {
                                criticalBoostActive = true
                                activeResult = critResult
                                battleResult = critResult
                                buzzerManager.playQteClickTone()
                                hapticManager.playAttackImpact()
                                true
                            } else false
                        }
                        delay(500L) // QTE window
                        HardwareInputManager.activeQteListener = null
                        qteActive = false
                        if (criticalBoostActive) {
                            activeResult = critResult
                            battleResult = critResult
                        }
                    }

                    val turn = activeResult.turns[i.coerceAtMost(activeResult.turns.lastIndex)]
                    // Authentic Color V-Pet Cut-In on Critical Hit / Mega Hit
                    val isCrit = turn.attackType == "MEGA_HIT" || (turn.attackerIndex == 0 && criticalBoostActive)
                    if (isCrit) {
                        val attackerId = if (turn.attackerIndex == 0) petState.speciesId else oppSpecies.id
                        activeCutInTitle = if (turn.attackType == "MEGA_HIT") "MEGA HIT" else "CRITICAL HIT"
                        activeCutInSpeciesId = attackerId
                        buzzerManager.playCriticalCutInTone()
                        hapticManager.playAttackImpact()
                        delay(750L)
                        activeCutInSpeciesId = null
                    }

                    buzzerManager.playAttackImpactTone()
                    hapticManager.playAttackImpact()
                    for (step in 1..8) {
                        turnFxProgress = step / 8f
                        delay(75L)
                    }
                    i++
                }

                val won = activeResult.winnerIndex == 0
                val (finalState, wasInjured) = repository.recordBattleResult(won)
                if (won) {
                    buzzerManager.playVictoryFanfare()
                    hapticManager.playVictoryFanfare()
                } else {
                    buzzerManager.playDefeatJingle()
                    if (wasInjured) {
                        hapticManager.playAttentionAlert()
                        battleStatusText = "[INJURED] INJURED IN BATTLE! Treat with medicine!"
                    }
                }
                isBattling = false
            }
        } catch (e: Exception) {
            battleStatusText = "Code Error: ${e.message?.take(30)}"
            hapticManager.playAttentionAlert()
        }
    }

    // ========================================================
    // 1. Live Turn-by-Turn Combat Animation View (Face-to-Face)
    // ========================================================
    if (battleResult != null && isBattling) {
        val currentTurn = battleResult!!.turns.getOrNull(currentTurnIndex)
        val playerRemHp = if (currentTurn == null) {
            battleResult!!.fighterA.maxHp
        } else if (currentTurn.attackerIndex == 1) {
            currentTurn.defenderRemainingHp
        } else {
            battleResult!!.turns.take(currentTurnIndex).lastOrNull { it.attackerIndex == 1 }?.defenderRemainingHp ?: battleResult!!.fighterA.maxHp
        }
        val oppRemHp = if (currentTurn == null) {
            battleResult!!.fighterB.maxHp
        } else if (currentTurn.attackerIndex == 0) {
            currentTurn.defenderRemainingHp
        } else {
            battleResult!!.turns.take(currentTurnIndex).lastOrNull { it.attackerIndex == 0 }?.defenderRemainingHp ?: battleResult!!.fighterB.maxHp
        }
        val playerHpFrac = (playerRemHp.toFloat() / battleResult!!.fighterA.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
        val oppHpFrac = (oppRemHp.toFloat() / battleResult!!.fighterB.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A1420)),
            contentAlignment = Alignment.Center
        ) {
            EnvironmentParticleCanvas(environmentEffect = activeEnvEffect)
            if (currentTurn != null) {
                val attackerSp = if (currentTurn.attackerIndex == 0) species else PetSpecies.getById(battleResult!!.fighterB.speciesId)
                BattleClashFxCanvas(
                    attackerIndex = currentTurn.attackerIndex,
                    attribute = attackerSp.attribute,
                    attackType = currentTurn.attackType,
                    isCritical = currentTurn.isCritical,
                    moveName = currentTurn.moveName,
                    damage = currentTurn.damage,
                    progress = turnFxProgress
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = displayConfig.horizontalPadding)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF162436), RoundedCornerShape(4.dp))
                        .border(0.5.dp, Color(0xFFFFD166), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${battleResult!!.battleMode.displayName.take(11).uppercase()} • R${currentTurn?.round ?: 1}/10",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD166),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Player Monster (flipped horizontally to face Opponent on right)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = species.name.take(9),
                            fontSize = 8.sp,
                            color = Color(0xFF80D8FF),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        val playerFrame = when {
                            currentTurn == null -> SpriteFrame.IDLE_A
                            currentTurn.attackerIndex == 0 -> when {
                                qteActive || turnFxProgress < 0.25f -> SpriteFrame.ATTACK_CHARGE
                                turnFxProgress < 0.65f -> SpriteFrame.ATTACK_RELEASE
                                else -> SpriteFrame.IDLE_B
                            }
                            turnFxProgress >= 0.65f -> SpriteFrame.INJURED
                            else -> SpriteFrame.IDLE_A
                        }
                        SpriteCanvasRenderer(
                            speciesId = petState.speciesId,
                            isAmbient = false,
                            overrideFrame = playerFrame,
                            size = displayConfig.battleSpriteSize,
                            flipHorizontal = true
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(5.dp)
                                .background(Color(0xFF263238), RoundedCornerShape(2.5.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(playerHpFrac)
                                    .height(5.dp)
                                    .background(Color(0xFF00E676), RoundedCornerShape(2.5.dp))
                            )
                        }
                        Text("$playerRemHp HP", fontSize = 7.sp, color = Color(0xFFB9F6CA), fontFamily = FontFamily.Monospace)
                    }

                    // Animated Projectile Clash Indicator
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when {
                                currentTurn?.attackType == "MEGA_HIT" -> "[MEGA]"
                                currentTurn?.attackerIndex == 0 -> ">>>"
                                currentTurn?.attackerIndex == 1 -> "<<<"
                                else -> "VS"
                            },
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (currentTurn?.attackType == "MEGA_HIT") Color(0xFFFFD54F) else Color(0xFFFF5252),
                            fontFamily = FontFamily.Monospace
                        )
                        currentTurn?.let { t ->
                            Text(
                                text = "HIT ${t.hitRatePercent}%",
                                fontSize = 6.5.sp,
                                color = Color(0xFF90CAF9),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Opponent Monster (faces left toward Player)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = battleResult!!.fighterB.name.take(9),
                            fontSize = 8.sp,
                            color = Color(0xFFFF8A80),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        val oppFrame = when {
                            currentTurn == null -> SpriteFrame.IDLE_A
                            currentTurn.attackerIndex == 1 -> when {
                                turnFxProgress < 0.25f -> SpriteFrame.ATTACK_CHARGE
                                turnFxProgress < 0.65f -> SpriteFrame.ATTACK_RELEASE
                                else -> SpriteFrame.IDLE_B
                            }
                            turnFxProgress >= 0.65f -> SpriteFrame.INJURED
                            else -> SpriteFrame.IDLE_A
                        }
                        SpriteCanvasRenderer(
                            speciesId = battleResult!!.fighterB.speciesId,
                            isAmbient = false,
                            overrideFrame = oppFrame,
                            size = displayConfig.battleSpriteSize,
                            flipHorizontal = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(5.dp)
                                .background(Color(0xFF263238), RoundedCornerShape(2.5.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(oppHpFrac)
                                    .height(5.dp)
                                    .background(Color(0xFFFF5252), RoundedCornerShape(2.5.dp))
                            )
                        }
                        Text("$oppRemHp HP", fontSize = 7.sp, color = Color(0xFFFFCDD2), fontFamily = FontFamily.Monospace)
                    }
                }

                currentTurn?.let { turn ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${turn.attackerName.take(8)} [${turn.attackType}] -> -${turn.damage} HP!",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (turn.isAdvantage) Color(0xFF00E676) else Color.White,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    if (turn.isAdvantage) {
                        Text("1.2x ATTRIBUTE ADVANTAGE! • PWR ${turn.effectivePower}", fontSize = 6.5.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    if (qteActive) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFFF1744), RoundedCornerShape(4.dp))
                                .border(0.75.dp, Color.White, RoundedCornerShape(4.dp))
                                .clickable {
                                    criticalBoostActive = true
                                    buzzerManager.playQteClickTone()
                                    hapticManager.playAttackImpact()
                                    qteActive = false
                                }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.BOLT, size = 8.dp, tint = Color.White)
                            Text("STRIKE! [B] / TAP!", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Monospace)
                            DigiVectorIcon(type = DigiIconType.BOLT, size = 8.dp, tint = Color.White)
                        }
                    } else if (criticalBoostActive) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.STAR_FULL, size = 8.dp, tint = Color(0xFFFFD54F))
                            Text("MEGA HIT BOOST!", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD54F), fontFamily = FontFamily.Monospace)
                            DigiVectorIcon(type = DigiIconType.STAR_FULL, size = 8.dp, tint = Color(0xFFFFD54F))
                        }
                    }
                }
            }

            activeCutInSpeciesId?.let { cutInId ->
                CutInOverlay(
                    speciesId = cutInId,
                    title = activeCutInTitle,
                    autoDismissMs = null,
                    onDismiss = { activeCutInSpeciesId = null }
                )
            }

            encounterFlashEnemyId?.let { enemyId ->
                EncounterFlashOverlay(
                    enemySpeciesId = enemyId,
                    isBoss = false,
                    onFinished = { encounterFlashEnemyId = null }
                )
            }
        }
        return
    }

    // ========================================================
    // 2. Battle Result Summary View
    // ========================================================
    if (battleResult != null && !isBattling) {
        val won = battleResult!!.winnerIndex == 0
        val isDraw = battleResult!!.isDraw
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F131A)),
            state = resultListState,
            contentPadding = displayConfig.contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val resultIcon = when {
                        won -> DigiIconType.TROPHY
                        isDraw -> DigiIconType.BATTLE
                        else -> DigiIconType.SKULL
                    }
                    val resultColor = if (won) Color(0xFF00E676) else Color(0xFFFF5252)
                    DigiVectorIcon(type = resultIcon, size = 12.dp, tint = resultColor)
                    Text(
                        text = if (won) "VICTORY!" else if (isDraw) "DRAW" else "DEFEAT...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = resultColor,
                        fontFamily = FontFamily.Monospace
                    )
                    DigiVectorIcon(type = resultIcon, size = 12.dp, tint = resultColor)
                }
            }
            item {
                Text(
                    text = battleResult!!.battleMode.displayName.uppercase(),
                    fontSize = 7.5.sp,
                    color = Color(0xFFFFD54F),
                    fontFamily = FontFamily.Monospace
                )
            }
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${species.name} vs ${battleResult!!.fighterB.name}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Player HP: ${battleResult!!.fighterAFinalHp} | Opp HP: ${battleResult!!.fighterBFinalHp}",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF80CBC4)
                )
            }
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = battleResult!!.formulaSummary,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFB0BEC5),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = displayConfig.horizontalPadding)
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { battleResult = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .padding(horizontal = displayConfig.horizontalPadding),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF0288D1))
                ) {
                    Text("CONTINUE [B / C]", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        return
    }

    // ========================================================
    // 3. 4-Mode Battle Hub & Keypad Entry Screen
    // ========================================================
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10151E)),
        state = hubListState,
        contentPadding = displayConfig.contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (petState.isInjured) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                        .background(Color(0xFF381216), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    DigiVectorIcon(type = DigiIconType.SKULL, size = 10.dp, tint = Color(0xFFFF5252))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PARTNER IS INJURED!\nCannot battle until cured with [MEDS].",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF5252),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BATTLE TERMINAL",
                    fontSize = displayConfig.headerFontSize,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF9800),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                // 4-Mode Selector Pills (2x2 compact grid for Round/Rect watch)
                val modes = listOf(
                    Triple(BattleMode.NFC_TAP_DUEL, "NFC DUEL", DigiIconType.NFC),
                    Triple(BattleMode.WILD_SCAN, "WILD SCAN", DigiIconType.SCAN),
                    Triple(BattleMode.ACOM_DCOM_WIFICOM, "A-COM/WIFI", DigiIconType.GLOBE),
                    Triple(BattleMode.ADVENTURE, "COLOSSEUM", DigiIconType.COLOSSEUM)
                )
                modes.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (displayConfig.isRound) 26.dp else displayConfig.horizontalPadding,
                                vertical = 1.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        pair.forEach { (mode, label, iconType) ->
                            val active = selectedBattleMode == mode
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (active) Color(0xFF00695C) else Color(0xFF1A222D),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        0.75.dp,
                                        if (active) Color(0xFF00E676) else Color(0xFF37474F),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        selectedBattleMode = mode
                                        hapticManager.playStepMilestone()
                                    }
                                    .padding(vertical = 3.dp, horizontal = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                DigiVectorIcon(
                                    type = iconType,
                                    size = 8.dp,
                                    tint = if (active) Color.White else Color(0xFF90A4AE)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = label,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else Color(0xFF90A4AE),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mode-Specific Action Panel (Wild Scan or A-Com Phone Relay)
        if (selectedBattleMode == BattleMode.WILD_SCAN) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                        .background(Color(0xFF15251E), RoundedCornerShape(8.dp))
                        .border(0.75.dp, Color(0xFF00E676), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WILD NFC / BARCODE RADAR",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF69F0AE),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Tap any NFC tag or scan a barcode preset:",
                        fontSize = 7.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        WildScanEncounterGenerator.PRESET_BARCODES.take(3).forEach { (code, label) ->
                            Button(
                                onClick = {
                                    val enc = WildScanEncounterGenerator.generateFromScan(
                                        rawScanData = code,
                                        scanSource = "BARCODE",
                                        playerStage = species.stage
                                    )
                                    wildEncounter = enc
                                    opponentCodeInput = enc.commCode128
                                    hapticManager.playStepMilestone()
                                },
                                modifier = Modifier.weight(1f).height(24.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                            ) {
                                Text(label.take(6), fontSize = 6.5.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    wildEncounter?.let { enc ->
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${if (enc.isRareAnomaly) "[BOSS] " else ""}${enc.species.name} (${enc.habitatZone})",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace
                        )
                        Button(
                            onClick = {
                                startBattleWithCode(enc.commCode128, BattleMode.WILD_SCAN, enc.environmentHazard)
                            },
                            modifier = Modifier.fillMaxWidth().height(26.dp).padding(top = 2.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE65100))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                DigiVectorIcon(type = DigiIconType.BATTLE, size = 8.dp, tint = Color.White)
                                Text("BATTLE WILD ${enc.species.name.take(7)}", fontSize = 7.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        } else if (selectedBattleMode == BattleMode.ACOM_DCOM_WIFICOM) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                        .background(Color(0xFF1A1E2E), RoundedCornerShape(8.dp))
                        .border(0.75.dp, Color(0xFFFFD54F), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "A-COM / D-COM / WIFICOM RELAY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Tap Watch to Phone (NFC) or Beam DigiROM to Phone Companion App for Online Play:",
                        fontSize = 6.5.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val sent = withContext(Dispatchers.IO) {
                                    try {
                                        val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                                        var ok = false
                                        for (n in nodes) {
                                            Tasks.await(
                                                Wearable.getMessageClient(context).sendMessage(
                                                    n.id,
                                                    "/sync/watch_acom_relay",
                                                    ownDigiRom.toByteArray(Charsets.UTF_8)
                                                )
                                            )
                                            ok = true
                                        }
                                        ok
                                    } catch (_: Exception) {
                                        false
                                    }
                                }
                                clipboardManager.setText(AnnotatedString(ownDigiRom))
                                battleStatusText = if (sent) "[RELAY] Beamed $ownDigiRom to Phone A-Com!" else "[COPY] Copied $ownDigiRom (Tap Phone NFC)"
                                hapticManager.playStepMilestone()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(26.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1565C0))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.GLOBE, size = 8.dp, tint = Color.White)
                            Text("RELAY DIGIROM TO PHONE", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Own Comm Code Card + QR Matrix Toggle
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                    .background(Color(0xFF18202C), RoundedCornerShape(8.dp))
                    .border(0.75.dp, Color(0x5580D8FF), RoundedCornerShape(8.dp))
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showQrMatrix) {
                    Text(
                        text = "COMM CODE QR MATRIX",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF80D8FF),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(if (displayConfig.screenWidthDp >= 300) 150.dp else 140.dp)
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeMatrixView(
                            matrix = ownQrMatrix,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Scan with Companion Phone",
                        fontSize = 6.5.sp,
                        color = Color(0xFF90A4AE),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Button(
                        onClick = {
                            showQrMatrix = false
                            hapticManager.playStepMilestone()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F))
                    ) {
                        Text("SHOW TEXT CODE", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    Text("YOUR 128-BIT / DIGIROM:", fontSize = 7.sp, color = Color(0xFF90A4AE), fontFamily = FontFamily.Monospace)
                    Text(
                        text = CommCodeCodec.formatWithHyphens(ownCode).take(19) + "...",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80D8FF),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "0NL1NE: $ownDigiRom",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(ownCode))
                                battleStatusText = "Code Copied!"
                                hapticManager.playStepMilestone()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00838F))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                DigiVectorIcon(type = DigiIconType.COPY, size = 8.dp, tint = Color.White)
                                Text("COPY", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Button(
                            onClick = {
                                showQrMatrix = true
                                hapticManager.playStepMilestone()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                DigiVectorIcon(type = DigiIconType.QR, size = 8.dp, tint = Color.White)
                                Text("SHOW QR MATRIX", fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // Opponent Input Section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding)
                    .background(Color(0xFF20242E), RoundedCornerShape(6.dp))
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("OPPONENT CODE:", fontSize = 8.sp, color = Color.Gray)
                Text(
                    text = if (opponentCodeInput.isEmpty()) "[ENTER CODE]" else opponentCodeInput,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (opponentCodeInput.isEmpty()) Color.DarkGray else Color(0xFFFFD54F),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Paste from clipboard button
                    Button(
                        onClick = {
                            val clip = clipboardManager.getText()?.text?.trim()
                            if (!clip.isNullOrBlank()) {
                                opponentCodeInput = clip
                                battleStatusText = "Pasted!"
                            }
                        },
                        modifier = Modifier.height(26.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.COPY, size = 8.dp, tint = Color.White)
                            Text("PASTE", fontSize = 8.sp)
                        }
                    }

                    // Keypad toggle
                    Button(
                        onClick = { showManualKeypad = !showManualKeypad },
                        modifier = Modifier.height(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (showManualKeypad) Color(0xFFE65100) else Color(0xFF00695C)
                        )
                    ) {
                        if (showManualKeypad) {
                            Text("HIDE", fontSize = 8.sp)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                DigiVectorIcon(type = DigiIconType.KEYPAD, size = 8.dp, tint = Color.White)
                                Text("KEYPAD", fontSize = 8.sp)
                            }
                        }
                    }

                    // Backspace
                    Button(
                        onClick = {
                            if (opponentCodeInput.isNotEmpty()) {
                                opponentCodeInput = opponentCodeInput.dropLast(1)
                            }
                        },
                        modifier = Modifier.height(26.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828))
                    ) {
                        DigiVectorIcon(type = DigiIconType.BACKSPACE, size = 10.dp, tint = Color.White)
                    }
                }
            }
        }

        // Manual Crockford Base32 Keypad on Watch
        if (showManualKeypad) {
            val keyPages = listOf(
                listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                listOf("A", "B", "C", "D", "E", "F", "G", "H"),
                listOf("J", "K", "M", "N", "P", "Q", "R", "S"),
                listOf("T", "V", "W", "X", "Y", "Z")
            )
            val currentKeys = keyPages[keypadPageIndex]

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = displayConfig.horizontalPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SET ${keypadPageIndex + 1}/4",
                        fontSize = 8.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                    Button(
                        onClick = {
                            keypadPageIndex = (keypadPageIndex + 1) % keyPages.size
                            hapticManager.playStepMilestone()
                        },
                        modifier = Modifier.height(22.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF455A64))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("NEXT ", fontSize = 7.sp)
                            DigiVectorIcon(type = DigiIconType.ARROW_RIGHT, size = 7.dp, tint = Color.White)
                        }
                    }
                }
            }

            // Keypad Character Grid (2 rows)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = displayConfig.horizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val half = (currentKeys.size + 1) / 2
                    val row1 = currentKeys.take(half)
                    val row2 = currentKeys.drop(half)

                    Row(horizontalArrangement = Arrangement.Center) {
                        for (k in row1) {
                            Button(
                                onClick = {
                                    opponentCodeInput += k
                                    hapticManager.playStepMilestone()
                                },
                                modifier = Modifier.size(28.dp).padding(1.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E3846))
                            ) {
                                Text(k, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.Center) {
                        for (k in row2) {
                            Button(
                                onClick = {
                                    opponentCodeInput += k
                                    hapticManager.playStepMilestone()
                                },
                                modifier = Modifier.size(28.dp).padding(1.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E3846))
                            ) {
                                Text(k, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Start Battle or Jogress Button (if code present)
        if (opponentCodeInput.isNotEmpty()) {
            val fusionRecipe = try {
                val oppPayload = CommCodeCodec.decodeAuto(opponentCodeInput)
                JogressEngine.findFusionRecipe(petState.speciesId, oppPayload.monsterId)
            } catch (e: Exception) {
                null
            }

            if (fusionRecipe != null) {
                val targetSp = PetSpecies.getById(fusionRecipe.targetSpeciesId)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val oppPayload = CommCodeCodec.decodeAuto(opponentCodeInput)
                                    val fusedState = JogressEngine.executeFusion(
                                        primary = petState,
                                        secondarySpeciesId = oppPayload.monsterId,
                                        secondaryVictories = oppPayload.historicalVictories,
                                        secondaryEffort = oppPayload.trainingEffort
                                    )
                                    repository.savePetState(fusedState)
                                    hapticManager.playVictoryFanfare()
                                    battleStatusText = "Awakened ${targetSp.name} via ${fusionRecipe.fusionType.displayName}!"
                                    delay(2000L)
                                    onBack()
                                } catch (e: Exception) {
                                    battleStatusText = "Fusion Failed: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = displayConfig.horizontalPadding),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(fusionRecipe.fusionType.badgeColorHex))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.BOLT, size = 8.dp, tint = Color.Black)
                            Text("DNA DIGIVOLVE -> ${targetSp.name}", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            DigiVectorIcon(type = DigiIconType.BOLT, size = 8.dp, tint = Color.Black)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val env = if (selectedBattleMode == BattleMode.WILD_SCAN) {
                            wildEncounter?.environmentHazard ?: "Normal"
                        } else "Normal"
                        startBattleWithCode(opponentCodeInput, selectedBattleMode, env)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53935))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.BATTLE, size = 10.dp, tint = Color.White)
                        Text("START BATTLE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        DigiVectorIcon(type = DigiIconType.BATTLE, size = 10.dp, tint = Color.White)
                    }
                }
            }
        }

        // DNA Jogress / DigiXros / Mode Change Quick Matrix
        val compatibleFusions = JogressEngine.getCompatibleRecipesFor(petState.speciesId)
        if (compatibleFusions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding)
                        .background(Color(0xFF1F1630), RoundedCornerShape(8.dp))
                        .border(0.75.dp, Color(0xFFEA80FC), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.BOLT, size = 8.dp, tint = Color(0xFFEA80FC))
                        Text(
                            text = "DNA JOGRESS / DIGIXROS (${compatibleFusions.size})",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEA80FC),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    compatibleFusions.forEach { recipe ->
                        val partnerId = if (recipe.primarySpeciesId == petState.speciesId) recipe.secondarySpeciesId else recipe.primarySpeciesId
                        val partnerSp = PetSpecies.getById(partnerId)
                        val targetSp = PetSpecies.getById(recipe.targetSpeciesId)
                        val isSelfModeChange = recipe.primarySpeciesId == recipe.secondarySpeciesId
                        val decodedPartner = remember(opponentCodeInput) {
                            if (opponentCodeInput.isBlank()) null
                            else try { CommCodeCodec.decodeAuto(opponentCodeInput) } catch (_: Exception) { null }
                        }
                        val hasBackupPartner = backupPet?.speciesId == partnerSp.id
                        val hasCodePartner = decodedPartner?.monsterId == partnerSp.id
                        val canExecuteFusion = when {
                            isSelfModeChange -> petState.effort >= 4 && petState.victories >= 15
                            hasBackupPartner -> JogressEngine.findFusionRecipe(petState.speciesId, backupPet!!.speciesId) != null
                            hasCodePartner -> JogressEngine.findFusionRecipe(petState.speciesId, decodedPartner!!.monsterId) != null
                            else -> false
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .background(Color(0xFF141020), RoundedCornerShape(6.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "+${partnerSp.name.take(8)} -> ${targetSp.name.take(9)}",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = recipe.fusionType.displayName,
                                    fontSize = 6.sp,
                                    color = Color(recipe.fusionType.badgeColorHex),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            when {
                                                isSelfModeChange && canExecuteFusion -> {
                                                    val fusedState = JogressEngine.executeFusion(
                                                        primary = petState,
                                                        secondarySpeciesId = partnerSp.id,
                                                        secondaryVictories = petState.victories,
                                                        secondaryEffort = petState.effort
                                                    )
                                                    repository.savePetState(fusedState)
                                                    hapticManager.playVictoryFanfare()
                                                    battleStatusText = "Awakened ${targetSp.name}!"
                                                }
                                                hasBackupPartner && canExecuteFusion -> {
                                                    val fused = repository.performDirectJogress()
                                                    if (fused != null) {
                                                        hapticManager.playVictoryFanfare()
                                                        battleStatusText = "Awakened ${targetSp.name}!"
                                                    } else {
                                                        battleStatusText = "Needs 4 Effort or 15 Wins!"
                                                    }
                                                }
                                                hasCodePartner && canExecuteFusion -> {
                                                    val fusedState = JogressEngine.executeFusion(
                                                        primary = petState,
                                                        secondarySpeciesId = decodedPartner!!.monsterId,
                                                        secondaryVictories = decodedPartner.historicalVictories,
                                                        secondaryEffort = decodedPartner.trainingEffort
                                                    )
                                                    if (fusedState.speciesId != petState.speciesId) {
                                                        repository.savePetState(fusedState)
                                                        hapticManager.playVictoryFanfare()
                                                        battleStatusText = "Awakened ${targetSp.name}!"
                                                    } else {
                                                        battleStatusText = "Needs 4 Effort or 15 Wins!"
                                                    }
                                                }
                                                else -> {
                                                    battleStatusText = "Need ${partnerSp.name} in Cryo or Comm Code!"
                                                }
                                            }
                                        }
                                    },
                                    enabled = canExecuteFusion,
                                    modifier = Modifier.height(22.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (canExecuteFusion) Color(recipe.fusionType.badgeColorHex) else Color(0xFF37474F)
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        DigiVectorIcon(type = DigiIconType.BOLT, size = 7.dp, tint = if (canExecuteFusion) Color.Black else Color.LightGray)
                                        Text(
                                            text = if (canExecuteFusion) "FUSE" else "LOCKED",
                                            fontSize = 6.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (canExecuteFusion) Color.Black else Color.LightGray,
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

        // Quick Challenge Bots (authentic Base32 codes)
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("COLOSSEUM / A-COM BOTS:", fontSize = 8.sp, color = Color.Gray)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Devimon Bot
                Button(
                    onClick = {
                        val botCode = CommCodeCodec.encode128(
                            monsterId = 303,
                            stage = PetStage.ADULT,
                            attribute = ElementalAttribute.VIRUS,
                            basePowerHp = 220,
                            historicalVictories = 8,
                            trainingEffort = 3,
                            battleRngSeed = 0x55AA
                        )
                        startBattleWithCode(botCode, selectedBattleMode, "Normal")
                    },
                    modifier = Modifier.weight(1f).padding(2.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF8E24AA))
                ) {
                    Text("Devimon", fontSize = 8.sp)
                }

                // Garurumon Bot
                Button(
                    onClick = {
                        val botCode = CommCodeCodec.encode128(
                            monsterId = 306,
                            stage = PetStage.ADULT,
                            attribute = ElementalAttribute.DATA,
                            basePowerHp = 230,
                            historicalVictories = 10,
                            trainingEffort = 3,
                            battleRngSeed = 0x1234
                        )
                        startBattleWithCode(botCode, selectedBattleMode, "Normal")
                    },
                    modifier = Modifier.weight(1f).padding(2.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E88E5))
                ) {
                    Text("Garuru", fontSize = 8.sp)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // MetalGreymon Bot
                Button(
                    onClick = {
                        val botCode = CommCodeCodec.encode128(
                            monsterId = 401,
                            stage = PetStage.PERFECT,
                            attribute = ElementalAttribute.VACCINE,
                            basePowerHp = 380,
                            historicalVictories = 22,
                            trainingEffort = 4,
                            battleRngSeed = 0x9876
                        )
                        startBattleWithCode(botCode, selectedBattleMode, "Normal")
                    },
                    modifier = Modifier.weight(1f).padding(2.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFB8C00))
                ) {
                    Text("M-Grey", fontSize = 8.sp)
                }

                // Machinedramon Bot (Ultimate)
                Button(
                    onClick = {
                        val botCode = CommCodeCodec.encode128(
                            monsterId = 502,
                            stage = PetStage.ULTIMATE,
                            attribute = ElementalAttribute.VIRUS,
                            basePowerHp = 580,
                            historicalVictories = 35,
                            trainingEffort = 4,
                            battleRngSeed = 0xBEEF
                        )
                        startBattleWithCode(botCode, selectedBattleMode, "Normal")
                    },
                    modifier = Modifier.weight(1f).padding(2.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
                ) {
                    Text("Machine", fontSize = 8.sp)
                }
            }
        }

        // Status or Error text
        battleStatusText?.let {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    fontSize = 9.sp,
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F))
            ) {
                Text("BACK [C]", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }

    // High-Impact Battle Cut-In Overlay
    activeCutInSpeciesId?.let { cutInId ->
        CutInOverlay(
            speciesId = cutInId,
            title = activeCutInTitle,
            autoDismissMs = null,
            onDismiss = { activeCutInSpeciesId = null }
        )
    }

    // Authentic Battle Encounter Flash Overlay
    encounterFlashEnemyId?.let { enemyId ->
        EncounterFlashOverlay(
            enemySpeciesId = enemyId,
            isBoss = false,
            onFinished = { encounterFlashEnemyId = null }
        )
    }
}
