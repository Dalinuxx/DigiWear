package com.digiwear.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.digiwear.core.adventure.AdventureEngine
import com.digiwear.core.battle.DeterministicBattleSolver
import com.digiwear.core.model.AdventureProgress
import com.digiwear.core.model.BattleFighter
import com.digiwear.core.model.BattleMode
import com.digiwear.core.model.BattleResult
import com.digiwear.core.model.BossEncounter
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import com.digiwear.core.model.SagaProgressRecord
import com.digiwear.core.model.SpriteFrame
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.haptic.HapticManager
import com.digiwear.wear.input.HardwareInputManager
import com.digiwear.wear.input.VPetButton
import com.digiwear.wear.render.SpriteCanvasRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun AdventureScreen(
    repository: PetRepository,
    hapticManager: HapticManager,
    buzzerManager: com.digiwear.wear.sound.BuzzerManager = remember { com.digiwear.wear.sound.BuzzerManager() },
    onNavigateWorkout: () -> Unit = {},
    onBack: () -> Unit
) {
    val progress by repository.adventureStateFlow.collectAsState(initial = AdventureProgress())
    val petState by repository.petStateFlow.collectAsState(initial = PetState())
    val hazardAlertsEnabled by repository.hazardAlertsFlow.collectAsState(initial = true)
    val campaignJson by repository.campaignJsonFlow.collectAsState()
    val story = remember(campaignJson) { repository.getActiveCampaign() }
    val playerSpecies = remember(petState.speciesId) { PetSpecies.getById(petState.speciesId) }
    val displayConfig = WatchDisplayConfig.current()
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    val currentNodeIndex = progress.currentNodeIndex.coerceAtMost((story.nodes.size - 1).coerceAtLeast(0))
    val currentNode = story.nodes[currentNodeIndex]
    var eventMessage by remember { mutableStateOf<String?>(null) }
    var wildEncounterMonsterId by remember { mutableStateOf<Int?>(null) }
    var showSagaPicker by remember { mutableStateOf(false) }
    var showSectorPicker by remember { mutableStateOf(false) }

    // Live Adventure Combat Arena State
    var activeBattleResult by remember { mutableStateOf<BattleResult?>(null) }
    var currentTurnIndex by remember { mutableIntStateOf(0) }
    var isBattling by remember { mutableStateOf(false) }
    var qteActive by remember { mutableStateOf(false) }
    var criticalBoostActive by remember { mutableStateOf(false) }
    var turnFxProgress by remember { mutableFloatStateOf(0f) }
    var activeCutInSpeciesId by remember { mutableStateOf<Int?>(null) }
    var activeCutInTitle by remember { mutableStateOf("CRITICAL HIT") }
    var encounterFlashEnemyId by remember { mutableStateOf<Int?>(null) }
    var encounterFlashIsBoss by remember { mutableStateOf(false) }
    var encounterFlashBossTitle by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            HardwareInputManager.activeQteListener = null
        }
    }

    val actLabel = if (currentNode.actTitle.isNotBlank()) {
        currentNode.actTitle
    } else {
        when {
            currentNodeIndex < 10 -> "ACT I • FILE ISLAND"
            currentNodeIndex < 20 -> "ACT II • SERVER CONT."
            currentNodeIndex < 25 -> "ACT III • SPIRAL MTN"
            else -> "ACT IV • GENESIS"
        }
    }
    val stepRatio = (progress.accumulatedStepsCurrentNode.toFloat() / currentNode.requiredSteps.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    androidx.activity.compose.BackHandler(enabled = showSagaPicker || showSectorPicker) {
        buzzerManager.playButtonBeep()
        showSagaPicker = false
        showSectorPicker = false
    }

    val executeAdventureCombat: suspend (Int, Int?, Int?, BossEncounter?) -> Unit = { enemySpeciesId, hpOverride, powerOverride, bossToClear ->
        if (petState.isDeceased) {
            eventMessage = "[RIP] Partner has passed away."
            buzzerManager.playAttentionBeep()
        } else if (petState.isInjured) {
            eventMessage = "[INJURED] Partner is injured! Cure with [MEDS] first."
            buzzerManager.playAttentionBeep()
            hapticManager.playAttentionAlert()
        } else {
        val enemySpecies = PetSpecies.getById(enemySpeciesId)
        val enemyHp = hpOverride ?: enemySpecies.baseHp
        val enemyPower = powerOverride ?: enemySpecies.basePower
        val seedA = Random.nextInt(0xFFFF)
        val seedB = Random.nextInt(0xFFFF)

        val fighterA = BattleFighter(
            id = "player",
            speciesId = petState.speciesId,
            name = playerSpecies.name,
            stage = playerSpecies.stage,
            attribute = playerSpecies.attribute,
            maxHp = petState.maxHp,
            currentHp = petState.maxHp,
            basePower = playerSpecies.basePower,
            victories = petState.victories,
            effort = petState.effort,
            strength = petState.strength,
            weight = petState.weight,
            battleSeed = seedA
        )

        val fighterB = BattleFighter(
            id = "adventure_enemy",
            speciesId = enemySpecies.id,
            name = bossToClear?.bossName?.ifBlank { enemySpecies.name } ?: enemySpecies.name,
            stage = enemySpecies.stage,
            attribute = enemySpecies.attribute,
            maxHp = enemyHp,
            currentHp = enemyHp,
            basePower = enemyPower,
            victories = currentNodeIndex,
            effort = (currentNodeIndex / 6).coerceIn(0, 4),
            battleSeed = seedB
        )

        val baseResult = DeterministicBattleSolver.resolveBattle(
            fighterA = fighterA,
            fighterB = fighterB,
            environmentEffect = currentNode.environmentEffect,
            attackerCritBoost = false,
            battleMode = BattleMode.ADVENTURE
        )
        val critResult = DeterministicBattleSolver.resolveBattle(
            fighterA = fighterA,
            fighterB = fighterB,
            environmentEffect = currentNode.environmentEffect,
            attackerCritBoost = true,
            battleMode = BattleMode.ADVENTURE
        )

        // Dramatic Encounter Flash Sequence
        encounterFlashEnemyId = enemySpeciesId
        encounterFlashIsBoss = (bossToClear != null)
        encounterFlashBossTitle = bossToClear?.bossName
        buzzerManager.playEncounterAlarm()
        hapticManager.playAttentionAlert()
        delay(1050L)
        encounterFlashEnemyId = null

        var chosenResult = baseResult
        activeBattleResult = chosenResult
        isBattling = true
        currentTurnIndex = 0

        var idx = 0
        while (idx < chosenResult.turns.size) {
            currentTurnIndex = idx
            val initialTurn = chosenResult.turns[idx]
            criticalBoostActive = false
            if (initialTurn.attackerIndex == 0) {
                qteActive = true
                HardwareInputManager.activeQteListener = { btn ->
                    if (btn == VPetButton.B || btn == VPetButton.A) {
                        criticalBoostActive = true
                        chosenResult = critResult
                        activeBattleResult = critResult
                        buzzerManager.playQteClickTone()
                        hapticManager.playAttackImpact()
                        true
                    } else false
                }
                delay(420L)
                HardwareInputManager.activeQteListener = null
                qteActive = false
                if (criticalBoostActive) {
                    chosenResult = critResult
                    activeBattleResult = critResult
                }
            }

            val turn = chosenResult.turns[idx.coerceAtMost(chosenResult.turns.lastIndex)]
            // Authentic Color V-Pet Battle Cut-In on Critical Hit / Mega Hit
            val isCrit = turn.attackType == "MEGA_HIT" || (turn.attackerIndex == 0 && criticalBoostActive)
            if (isCrit) {
                val attackerSpeciesId = if (turn.attackerIndex == 0) petState.speciesId else enemySpecies.id
                activeCutInTitle = if (turn.attackType == "MEGA_HIT") "MEGA HIT" else "CRITICAL HIT"
                activeCutInSpeciesId = attackerSpeciesId
                buzzerManager.playCriticalCutInTone()
                hapticManager.playAttackImpact()
                delay(750L)
                activeCutInSpeciesId = null
            }

            buzzerManager.playAttackImpactTone()
            hapticManager.playAttackImpact()
            for (step in 1..8) {
                turnFxProgress = step / 8f
                delay(70L)
            }
            idx++
        }

        val won = chosenResult.winnerIndex == 0
        val (finalState, wasInjured) = repository.recordBattleResult(won)
        isBattling = false
        activeBattleResult = null

        if (bossToClear != null) {
            if (won) {
                buzzerManager.playVictoryFanfare()
                hapticManager.playVictoryFanfare()
                repository.recordBossVictory(bossToClear)
                val eggRewardMsg = bossToClear.rewardEggId?.let { eggId ->
                    val eggSp = PetSpecies.getById(eggId)
                    " [EGG] Unlocked ${eggSp.name} Starter Egg!"
                } ?: ""
                val itemRewardMsg = if (bossToClear.rewardItem.isNotBlank()) {
                    " [REWARD] Obtained ${bossToClear.rewardItem}!"
                } else ""
                val defeatLine = if (bossToClear.defeatDialogue.isNotBlank()) "\n\"${bossToClear.defeatDialogue}\"" else ""
                eventMessage = "[VICTORY] Boss ${enemySpecies.name} Defeated!$defeatLine$eggRewardMsg$itemRewardMsg"
            } else {
                buzzerManager.playDefeatJingle()
                hapticManager.playAttentionAlert()
                val introLine = if (bossToClear.introDialogue.isNotBlank()) "\n\"${bossToClear.introDialogue}\"" else ""
                val injuryNotice = if (wasInjured) "\n[INJURED] INJURED IN COMBAT! Needs Medicine!" else ""
                eventMessage = "[DEFEAT] Boss ${enemySpecies.name} held the gate (HP: ${chosenResult.fighterBFinalHp}).$introLine$injuryNotice"
            }
        } else {
            wildEncounterMonsterId = null
            if (won) {
                buzzerManager.playVictoryFanfare()
                hapticManager.playVictoryFanfare()
                eventMessage = "[VICTORY] Defeated Wild ${enemySpecies.name} (${chosenResult.turns.size} turns)! +1 Win"
            } else {
                buzzerManager.playDefeatJingle()
                hapticManager.playAttackImpact()
                val injuryNotice = if (wasInjured) " [INJURED] Needs Medicine!" else ""
                eventMessage = "Wild ${enemySpecies.name} overwhelmed your partner!$injuryNotice"
            }
        }
    }
}

    LaunchedEffect(currentNode, progress, wildEncounterMonsterId, petState, isBattling, showSagaPicker, showSectorPicker) {
        HardwareInputManager.buttonEvents.collect { button ->
            if (isBattling) return@collect
            when (button) {
                VPetButton.C -> {
                    if (showSagaPicker) {
                        showSagaPicker = false
                    } else if (showSectorPicker) {
                        showSectorPicker = false
                    } else {
                        onBack()
                    }
                }
                VPetButton.B -> {
                    if (showSagaPicker || showSectorPicker) return@collect
                    val activeWild = wildEncounterMonsterId
                    val boss = currentNode.bossEncounter
                    if (activeWild != null) {
                        executeAdventureCombat(activeWild, null, null, null)
                    } else if (boss != null && !progress.clearedFlags.contains(boss.completionFlag)) {
                        executeAdventureCombat(boss.bossMonsterId, boss.bossHp, boss.bossPower, boss)
                    } else {
                        val result = repository.addAdventureSteps(100)
                        val monsterId = result.randomEncounterMonsterId
                        if (result.nodeCleared) {
                            hapticManager.playStepMilestone()
                            eventMessage = "Sector Gate Cleared!"
                        } else if (monsterId != null) {
                            wildEncounterMonsterId = monsterId
                            val encSpecies = PetSpecies.getById(monsterId)
                            if (hazardAlertsEnabled) {
                                hapticManager.playAttackImpact()
                            }
                            eventMessage = "Wild ${encSpecies.name} appeared!"
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Live Face-to-Face Adventure Combat Overlay
    if (activeBattleResult != null && isBattling) {
        val currentTurn = activeBattleResult!!.turns.getOrNull(currentTurnIndex)
        val playerRemHp = if (currentTurn == null) {
            activeBattleResult!!.fighterA.maxHp
        } else if (currentTurn.attackerIndex == 1) {
            currentTurn.defenderRemainingHp
        } else {
            activeBattleResult!!.turns.take(currentTurnIndex).lastOrNull { it.attackerIndex == 1 }?.defenderRemainingHp ?: activeBattleResult!!.fighterA.maxHp
        }
        val oppRemHp = if (currentTurn == null) {
            activeBattleResult!!.fighterB.maxHp
        } else if (currentTurn.attackerIndex == 0) {
            currentTurn.defenderRemainingHp
        } else {
            activeBattleResult!!.turns.take(currentTurnIndex).lastOrNull { it.attackerIndex == 0 }?.defenderRemainingHp ?: activeBattleResult!!.fighterB.maxHp
        }
        val playerHpFrac = (playerRemHp.toFloat() / activeBattleResult!!.fighterA.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
        val oppHpFrac = (oppRemHp.toFloat() / activeBattleResult!!.fighterB.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B10)),
            contentAlignment = Alignment.Center
        ) {
            EnvironmentParticleCanvas(environmentEffect = currentNode.environmentEffect)
            if (currentTurn != null) {
                val attackerSp = if (currentTurn.attackerIndex == 0) playerSpecies else PetSpecies.getById(activeBattleResult!!.fighterB.speciesId)
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(displayConfig.contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DigiVectorIcon(type = DigiIconType.BATTLE, size = 10.dp, tint = Color(0xFF64FFDA))
                    Text(
                        text = "ADVENTURE ARENA",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF64FFDA),
                        fontFamily = FontFamily.Monospace
                    )
                    DigiVectorIcon(type = DigiIconType.BATTLE, size = 10.dp, tint = Color(0xFF64FFDA))
                }
                Text(
                    text = "TURN ${currentTurnIndex + 1}/${activeBattleResult!!.turns.size} • ${currentNode.environmentEffect.uppercase()}",
                    fontSize = 6.5.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Fighter (Player)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(activeBattleResult!!.fighterA.name.take(8), fontSize = 7.5.sp, color = Color(0xFF80D8FF), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        val advPlayerFrame = when {
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
                            speciesId = activeBattleResult!!.fighterA.speciesId,
                            isAmbient = false,
                            overrideFrame = advPlayerFrame,
                            size = displayConfig.battleSpriteSize,
                            flipHorizontal = true
                        )
                        Box(modifier = Modifier.width(48.dp).height(4.dp).background(Color(0xFF263238), RoundedCornerShape(2.dp))) {
                            Box(modifier = Modifier.fillMaxWidth(playerHpFrac).height(4.dp).background(Color(0xFF00E676), RoundedCornerShape(2.dp)))
                        }
                        Text("$playerRemHp HP", fontSize = 6.5.sp, color = Color(0xFFB9F6CA), fontFamily = FontFamily.Monospace)
                    }

                    // Clash Animation Symbol
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentTurn?.attackerIndex == 0) ">>>" else "<<<",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (criticalBoostActive) Color(0xFFFFD54F) else Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        if (criticalBoostActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                DigiVectorIcon(type = DigiIconType.BOLT, size = 8.dp, tint = Color(0xFFFF5252))
                                Text("CRIT", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Right Fighter (Enemy / Boss)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(activeBattleResult!!.fighterB.name.take(8), fontSize = 7.5.sp, color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        val advOppFrame = when {
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
                            speciesId = activeBattleResult!!.fighterB.speciesId,
                            isAmbient = false,
                            overrideFrame = advOppFrame,
                            size = displayConfig.battleSpriteSize,
                            flipHorizontal = false
                        )
                        Box(modifier = Modifier.width(48.dp).height(4.dp).background(Color(0xFF263238), RoundedCornerShape(2.dp))) {
                            Box(modifier = Modifier.fillMaxWidth(oppHpFrac).height(4.dp).background(Color(0xFFFF5252), RoundedCornerShape(2.dp)))
                        }
                        Text("$oppRemHp HP", fontSize = 6.5.sp, color = Color(0xFFFFCDD2), fontFamily = FontFamily.Monospace)
                    }
                }

                currentTurn?.let { turn ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${turn.attackerName.take(8)} [${turn.attackType}] -${turn.damage}HP!",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (turn.isAdvantage) Color(0xFF00E676) else Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    if (qteActive) {
                        Row(
                            modifier = Modifier
                                .padding(top = 2.dp)
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
                            Text("STRIKE [B] / TAP!", fontSize = 7.5.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Monospace)
                            DigiVectorIcon(type = DigiIconType.BOLT, size = 8.dp, tint = Color.White)
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
                    isBoss = encounterFlashIsBoss,
                    bossTitle = encounterFlashBossTitle,
                    onFinished = { encounterFlashEnemyId = null }
                )
            }
        }
        return
    }

    // Saga Switcher Overlay Dialog
    if (showSagaPicker) {
        val builtInSagas = remember { AdventureEngine.getBuiltInCampaigns() }
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080D12)),
            contentPadding = displayConfig.contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "SELECT SAGA",
                    fontSize = displayConfig.headerFontSize,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF26A69A),
                    fontFamily = FontFamily.Monospace
                )
            }
            item {
                Text(
                    text = "Choose a Canonical Adventure Campaign:",
                    fontSize = 7.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            items(builtInSagas, key = { it.campaignId }) { saga ->
                val isActive = saga.campaignId.equals(story.campaignId, ignoreCase = true)
                val sagaRecord = if (isActive) {
                    SagaProgressRecord(
                        campaignId = saga.campaignId,
                        currentNodeIndex = progress.currentNodeIndex,
                        highestUnlockedNodeIndex = progress.highestUnlockedNodeIndex,
                        accumulatedStepsCurrentNode = progress.accumulatedStepsCurrentNode,
                        clearedFlags = progress.clearedFlags,
                        isCompleted = progress.isCompleted
                    )
                } else {
                    progress.sagaStates[saga.campaignId]
                }

                val progressSummary = when {
                    isActive && progress.isCompleted -> "COMPLETED [100%]"
                    sagaRecord?.isCompleted == true -> "COMPLETED [100%]"
                    sagaRecord != null && (sagaRecord.highestUnlockedNodeIndex > 0 || sagaRecord.accumulatedStepsCurrentNode > 0) ->
                        "SEC ${sagaRecord.currentNodeIndex + 1}/${saga.nodes.size} (Max: ${sagaRecord.highestUnlockedNodeIndex + 1}) • IN PROGRESS"
                    else -> "NOT STARTED"
                }

                Spacer(modifier = Modifier.height(3.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .padding(horizontal = 4.dp)
                        .background(
                            if (isActive) Color(0xFF004D40) else Color(0xFF141F28),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isActive) Color(0xFF64FFDA) else Color(0xFF2C3E50),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            scope.launch {
                                repository.switchCampaign(saga.campaignId)
                                showSagaPicker = false
                                hapticManager.playStepMilestone()
                                eventMessage = "Switched to ${saga.title}!"
                            }
                        }
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = saga.title,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color(0xFF64FFDA) else Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        if (isActive) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64FFDA),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = "${saga.nodes.size} Sectors • Rec: ${saga.recommendedStage}",
                        fontSize = 7.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = progressSummary,
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sagaRecord?.isCompleted == true || (isActive && progress.isCompleted)) Color(0xFFFFD54F) else Color(0xFF80CBC4),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { showSagaPicker = false },
                    modifier = Modifier.fillMaxWidth(0.84f).height(26.dp).padding(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F))
                ) {
                    Text("BACK [C]", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        return
    }

    // Sector Selector Overlay Dialog (Stage Replay / Frontline Selection)
    if (showSectorPicker) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080D12)),
            contentPadding = displayConfig.contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "SELECT SECTOR",
                    fontSize = displayConfig.headerFontSize,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF26A69A),
                    fontFamily = FontFamily.Monospace
                )
            }
            item {
                Text(
                    text = "${story.title}: Replay cleared sectors or jump to frontline:",
                    fontSize = 7.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            items(story.nodes, key = { it.nodeIndex }) { node ->
                val idx = story.nodes.indexOf(node)
                val isCurrent = idx == currentNodeIndex
                val isUnlocked = idx <= progress.highestUnlockedNodeIndex
                val isCleared = idx < progress.highestUnlockedNodeIndex || (idx == progress.highestUnlockedNodeIndex && progress.isCompleted)

                val badgeText = when {
                    isCurrent -> "ACTIVE"
                    isCleared -> "CLEARED [REPLAY]"
                    isUnlocked -> "UNLOCKED"
                    else -> "LOCKED"
                }

                val cardBg = when {
                    isCurrent -> Color(0xFF004D40)
                    isUnlocked -> Color(0xFF141F28)
                    else -> Color(0xFF0D1217)
                }

                val borderColor = when {
                    isCurrent -> Color(0xFF64FFDA)
                    isUnlocked -> Color(0xFF26A69A)
                    else -> Color(0xFF1C2833)
                }

                Spacer(modifier = Modifier.height(3.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .padding(horizontal = 4.dp)
                        .background(cardBg, RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable(enabled = isUnlocked) {
                            scope.launch {
                                repository.selectAdventureNode(idx)
                                showSectorPicker = false
                                hapticManager.playStepMilestone()
                                buzzerManager.playButtonBeep()
                                eventMessage = "Deployed to Sector ${idx + 1}: ${node.name}!"
                            }
                        }
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SEC ${idx + 1}: ${node.name}",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) Color.White else Color.DarkGray,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = badgeText,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                isCurrent -> Color(0xFF64FFDA)
                                isCleared -> Color(0xFF80CBC4)
                                isUnlocked -> Color(0xFFFFB74D)
                                else -> Color.Gray
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${node.requiredSteps} steps • ${node.environmentEffect.uppercase()}" +
                            (node.bossEncounter?.let { " • BOSS: ${it.bossName.ifBlank { "Gate Boss" }}" } ?: ""),
                        fontSize = 7.sp,
                        color = if (isUnlocked) Color.LightGray else Color.DarkGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { showSectorPicker = false },
                    modifier = Modifier.fillMaxWidth(0.84f).height(26.dp).padding(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F))
                ) {
                    Text("BACK [C]", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1410))
    ) {
        EnvironmentParticleCanvas(environmentEffect = currentNode.environmentEffect)
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = displayConfig.contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DIGIVICE QUEST",
                    fontSize = displayConfig.headerFontSize,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF26A69A),
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF142820), RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color(0xFF26A69A), RoundedCornerShape(4.dp))
                            .clickable { showSectorPicker = true }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("SECTORS", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80CBC4), fontFamily = FontFamily.Monospace)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00695C), RoundedCornerShape(4.dp))
                            .clickable { showSagaPicker = true }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("SAGA", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.width(2.dp))
                            DigiVectorIcon(type = DigiIconType.ARROW_DOWN, size = 6.dp, tint = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(vertical = 1.dp)
                    .background(Color(0xFF142820), RoundedCornerShape(4.dp))
                    .border(0.5.dp, Color(0xFF26A69A), RoundedCornerShape(4.dp))
                    .clickable { showSectorPicker = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$actLabel • SEC ${currentNodeIndex + 1}/${story.nodes.size}" +
                            if (progress.highestUnlockedNodeIndex > currentNodeIndex) " [REPLAY]" else "",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80CBC4),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    DigiVectorIcon(type = DigiIconType.ARROW_DOWN, size = 6.dp, tint = Color(0xFF80CBC4))
                }
            }
        }

        item {
            Text(
                text = currentNode.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Narrative box
        item {
            Text(
                text = currentNode.narrativeText,
                fontSize = 8.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding)
            )
        }

        // Environmental condition indicator
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                DigiVectorIcon(type = DigiIconType.BOLT, size = 8.dp, tint = Color(0xFFFFB74D))
                Text(
                    text = "ENV: ${currentNode.environmentEffect.uppercase()}",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB74D),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Steps Progress Card with Visual Progress Bar
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(vertical = 4.dp)
                    .background(Color(0xFF12221B), RoundedCornerShape(8.dp))
                    .border(0.75.dp, Color(0x5526A69A), RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                Text(
                    text = "${progress.accumulatedStepsCurrentNode} / ${currentNode.requiredSteps} STEPS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF64FFDA),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color(0xFF08100C), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(stepRatio)
                            .height(6.dp)
                            .background(Color(0xFF00E676), RoundedCornerShape(3.dp))
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Expedition Total: ${progress.totalAdventureSteps} steps",
                    fontSize = 7.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Holographic Sector Radar Scanner
        item {
            AdventureRadarView(
                currentSteps = progress.accumulatedStepsCurrentNode,
                requiredSteps = currentNode.requiredSteps,
                bossName = currentNode.bossEncounter?.bossName,
                environmentEffect = currentNode.environmentEffect,
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .padding(vertical = 2.dp)
            )
        }

        // Start Expedition Workout Action
        item {
            Button(
                onClick = {
                    hapticManager.playStepMilestone()
                    onNavigateWorkout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                    .height(28.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00B0FF)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    DigiVectorIcon(type = DigiIconType.BOLT, size = 9.dp, tint = Color.Black)
                    Text(
                        text = "START EXPEDITION WORKOUT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )
                }
            }
        }

        // Wild Encounter Card (when random encounter occurs)
        wildEncounterMonsterId?.let { wildId ->
            val wildSpecies = PetSpecies.getById(wildId)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                        .background(Color(0xFF1A2536), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.BOLT, size = 9.dp, tint = Color(0xFFFFD54F))
                        Text(
                            text = "WILD ${wildSpecies.name.uppercase()}!",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace
                        )
                        DigiVectorIcon(type = DigiIconType.BOLT, size = 9.dp, tint = Color(0xFFFFD54F))
                    }
                    Text(
                        text = "${wildSpecies.stage.displayName} • ${wildSpecies.attribute.displayName} • HP ${wildSpecies.baseHp}",
                        fontSize = 7.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                    SpriteCanvasRenderer(
                        speciesId = wildId,
                        isAmbient = false,
                        size = displayConfig.battleSpriteSize
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                executeAdventureCombat(wildId, null, null, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEF6C00))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.BATTLE, size = 9.dp, tint = Color.White)
                            Text("ENGAGE WILD [B]", fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Boss Battle Encounter Gate (if steps met and boss present)
        if (currentNode.bossEncounter != null && !progress.clearedFlags.contains(currentNode.bossEncounter!!.completionFlag)) {
            val boss = currentNode.bossEncounter!!
            val bossSpecies = PetSpecies.getById(boss.bossMonsterId)
            val bossDisplayName = boss.bossName.ifBlank { bossSpecies.name }
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                        .background(Color(0xFF2A0F14), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.SKULL, size = 10.dp, tint = Color(0xFFFF5252))
                        Text(
                            text = "BOSS: ${bossDisplayName.uppercase()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF5252),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (boss.introDialogue.isNotBlank()) {
                        Text(
                            text = "\"${boss.introDialogue}\"",
                            fontSize = 7.5.sp,
                            color = Color(0xFFFFCCBC),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = "HP ${boss.bossHp} • PWR ${boss.bossPower} • ${bossSpecies.attribute.displayName}",
                        fontSize = 7.sp,
                        color = Color(0xFFFFCDD2),
                        fontFamily = FontFamily.Monospace
                    )
                    SpriteCanvasRenderer(
                        speciesId = boss.bossMonsterId,
                        isAmbient = false,
                        size = displayConfig.battleSpriteSize
                    )
                    if (boss.rewardItem.isNotBlank() || boss.rewardEggId != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val rewardLabel = buildString {
                            if (boss.rewardEggId != null) append("[EGG] ")
                            if (boss.rewardItem.isNotBlank()) append(boss.rewardItem)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.REWARD, size = 8.dp, tint = Color(0xFFFFD54F))
                            Text(
                                text = "REWARD: $rewardLabel",
                                fontSize = 6.5.sp,
                                color = Color(0xFFFFD54F),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                executeAdventureCombat(boss.bossMonsterId, boss.bossHp, boss.bossPower, boss)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.BATTLE, size = 9.dp, tint = Color.White)
                            Text("CHALLENGE BOSS [B]", fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Campaign Completed Banner
        if (progress.isCompleted) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 4.dp)
                        .background(Color(0xFF263238), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.TROPHY, size = 10.dp, tint = Color(0xFFFFD54F))
                        Text(
                            text = "SAGA CONQUERED!",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace
                        )
                        DigiVectorIcon(type = DigiIconType.TROPHY, size = 10.dp, tint = Color(0xFFFFD54F))
                    }
                    Text(
                        text = "All sectors of '${story.title}' restored!",
                        fontSize = 7.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Expedition Spoils / Unlocked Rewards Summary
        if (progress.unlockedEggs.isNotEmpty() || progress.collectedItems.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                        .background(Color(0xFF142028), RoundedCornerShape(6.dp))
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SPOILS: ${progress.unlockedEggs.size} Eggs • ${progress.collectedItems.size} Crests",
                        fontSize = 6.5.sp,
                        color = Color(0xFF80CBC4),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Step Simulator / Walk Button (+100 and +500)
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = displayConfig.horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = repository.addAdventureSteps(100)
                            val monsterId = result.randomEncounterMonsterId
                            if (result.nodeCleared) {
                                hapticManager.playStepMilestone()
                                eventMessage = "Sector Gate Cleared!"
                            } else if (monsterId != null) {
                                wildEncounterMonsterId = monsterId
                                val encSpecies = PetSpecies.getById(monsterId)
                                if (hazardAlertsEnabled) {
                                    hapticManager.playAttackImpact()
                                }
                                eventMessage = "Wild ${encSpecies.name} Encountered!"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(28.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00897B))
                ) {
                    Text("+100", fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                Button(
                    onClick = {
                        scope.launch {
                            val result = repository.addAdventureSteps(500)
                            val monsterId = result.randomEncounterMonsterId
                            if (result.nodeCleared) {
                                hapticManager.playStepMilestone()
                                eventMessage = "Sector Gate Cleared!"
                            } else if (monsterId != null) {
                                wildEncounterMonsterId = monsterId
                                val encSpecies = PetSpecies.getById(monsterId)
                                if (hazardAlertsEnabled) {
                                    hapticManager.playAttackImpact()
                                }
                                eventMessage = "Wild ${encSpecies.name} Encountered!"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(28.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00695C))
                ) {
                    Text("+500", fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        eventMessage?.let { msg ->
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = msg,
                    fontSize = 8.5.sp,
                    color = Color(0xFF64FFDA),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = displayConfig.horizontalPadding)
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
            isBoss = encounterFlashIsBoss,
            bossTitle = encounterFlashBossTitle,
            onFinished = { encounterFlashEnemyId = null }
        )
    }
}
