package com.digiwear.companion.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digiwear.companion.data.PhoneDatabase
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.render.PortraitAssetCache
import com.digiwear.companion.render.SpriteCanvasRenderer
import com.digiwear.companion.sound.BuzzerManager
import com.digiwear.core.battle.DeterministicBattleSolver
import com.digiwear.core.battle.SignatureAttackRegistry
import com.digiwear.core.colosseum.BattleTowerEngine
import com.digiwear.core.model.BattleFighter
import com.digiwear.core.model.BattleResult
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import com.digiwear.core.model.SpriteFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 100-Floor Battle Tower / Colosseum Gauntlet.
 * Ascend 10 difficulty tiers against canonical adversaries, milestone floor bosses,
 * and environmental hazards with real-time turn combat and milestone rewards.
 */
@Composable
fun BattleTowerScreen(
    db: PhoneDatabase,
    wearableManager: PhoneWearableManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("digiwear_battle_tower", Context.MODE_PRIVATE) }
    var highestUnlockedFloor by remember { mutableIntStateOf(prefs.getInt("highest_floor", 1)) }
    var currentFloorNumber by remember { mutableIntStateOf(highestUnlockedFloor.coerceIn(1, 100)) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val buzzerManager = remember { BuzzerManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            buzzerManager.release()
        }
    }

    // Active partner from Room (fall back to first stored Vault partner before default Greymon)
    val latestSnapshot by db.phoneDao().getAllSnapshotsFlow().collectAsState(initial = emptyList())
    val vaultPets by db.phoneDao().getAllVaultPetsFlow().collectAsState(initial = emptyList())
    val activePartner = latestSnapshot.firstOrNull()
    val fallbackVaultPartner = vaultPets.firstOrNull()
    val effectiveSpeciesId = activePartner?.speciesId ?: fallbackVaultPartner?.speciesId ?: 301
    val playerSpecies = remember(effectiveSpeciesId) {
        PetSpecies.getById(effectiveSpeciesId)
    }

    val floor = remember(currentFloorNumber) {
        BattleTowerEngine.getFloor(currentFloorNumber)
    }

    val opponentPortrait: ImageBitmap? = remember(floor.opponentSpeciesId) {
        PortraitAssetCache.getPortrait(context, floor.opponentSpeciesId)
    }

    // Battle simulation state
    val playerMaxHp = activePartner?.maxHp ?: fallbackVaultPartner?.maxHp ?: playerSpecies.baseHp
    var isBattling by remember { mutableStateOf(false) }
    var battleResult by remember { mutableStateOf<BattleResult?>(null) }
    var currentTurnIndex by remember { mutableIntStateOf(0) }
    var playerDisplayHp by remember(playerMaxHp, currentFloorNumber) { mutableIntStateOf(playerMaxHp) }
    var opponentDisplayHp by remember(floor.opponentHp, currentFloorNumber) { mutableIntStateOf(floor.opponentHp) }
    var currentCombatActionText by remember { mutableStateOf("READY TO ENGAGE") }
    var battleFinishedMsg by remember { mutableStateOf<String?>(null) }

    val CHECKPOINTS = listOf(1, 11, 21, 31, 41, 51, 61, 71, 81, 91)

    fun startBattle() {
        val playerFighter = BattleFighter(
            id = "player_tamer",
            speciesId = playerSpecies.id,
            name = playerSpecies.name,
            stage = playerSpecies.stage,
            attribute = playerSpecies.attribute,
            maxHp = playerMaxHp,
            currentHp = playerMaxHp,
            basePower = playerSpecies.basePower,
            victories = activePartner?.victories ?: fallbackVaultPartner?.victories ?: 15,
            effort = activePartner?.effort ?: fallbackVaultPartner?.effort ?: 4,
            strength = activePartner?.strength ?: fallbackVaultPartner?.strength ?: 4,
            battleSeed = System.currentTimeMillis().toInt()
        )

        val opponentFighter = BattleTowerEngine.createOpponentFighter(currentFloorNumber)
        val result = DeterministicBattleSolver.resolveBattle(
            playerFighter,
            opponentFighter,
            environmentEffect = floor.environmentEffect
        )

        isBattling = true
        battleResult = result
        currentTurnIndex = 0
        battleFinishedMsg = null
        playerDisplayHp = playerFighter.maxHp
        opponentDisplayHp = opponentFighter.maxHp

        scope.launch {
            buzzerManager.playEncounterAlarm()
            delay(600L)

            var curPlayerHp = playerFighter.maxHp
            var curOpponentHp = opponentFighter.maxHp

            for (turn in result.turns) {
                currentCombatActionText = "${turn.moveName}!"
                if (turn.isCritical) {
                    buzzerManager.playCriticalCutInTone()
                } else {
                    buzzerManager.playAttackImpactTone()
                }

                if (turn.attackerIndex == 0) {
                    curOpponentHp = max(0, curOpponentHp - turn.damage)
                    opponentDisplayHp = curOpponentHp
                } else {
                    curPlayerHp = max(0, curPlayerHp - turn.damage)
                    playerDisplayHp = curPlayerHp
                }
                delay(700L)
            }

            val won = result.winnerIndex == 0
            db.phoneDao().recordEncounter(floor.opponentSpeciesId, isDefeated = won)

            if (activePartner != null) {
                val nowSec = System.currentTimeMillis() / 1000L
                // Use the live watch pet when reachable so stale phone data never overwrites it.
                val liveWatchState = wearableManager.requestPetStateFromWatch()
                    ?.takeIf { it.speciesId == activePartner.speciesId }
                val baseState = liveWatchState
                    ?: (db.phoneDao().getLatestSnapshot()?.takeIf { it.speciesId == activePartner.speciesId } ?: activePartner).toDomain()
                val (baseUpdated, _) = com.digiwear.core.engine.PetSimulationEngine.processBattleOutcome(
                    state = baseState,
                    won = won,
                    combatInjuriesEnabled = wearableManager.isCombatInjuriesEnabled()
                )
                // Permanent boss rewards (max HP, traited-egg bonus) are granted only on a floor's
                // first clear, so replaying boss floors can't farm them indefinitely.
                val isFirstClear = currentFloorNumber >= highestUnlockedFloor
                val bonusVp = if (won && floor.isBossFloor) 150 else if (won) 25 else 0
                val bonusMaxHp = if (won && floor.isBossFloor && isFirstClear) 5 else 0
                val bonusTraitedEgg = when {
                    !won || !isFirstClear -> 0
                    floor.floorNumber == 100 -> 2
                    floor.floorNumber % 20 == 0 -> 1
                    else -> 0
                }
                val bonusEffort = if (won && floor.isBossFloor) 1 else 0
                val bonusStrength = if (won && floor.isBossFloor) 1 else 0
                val newMaxHp = baseUpdated.maxHp + bonusMaxHp
                val withBonus = baseUpdated.copy(
                    vitalPoints = (baseUpdated.vitalPoints + bonusVp).coerceIn(0, 9999),
                    maxHp = newMaxHp,
                    currentHp = (baseUpdated.currentHp + bonusMaxHp).coerceAtMost(newMaxHp),
                    traitedEggBonus = (baseUpdated.traitedEggBonus + bonusTraitedEgg).coerceAtMost(5),
                    effort = (baseUpdated.effort + bonusEffort).coerceAtMost(4),
                    strength = (baseUpdated.strength + bonusStrength).coerceAtMost(4)
                )
                val evolved = com.digiwear.core.engine.EvolutionEngine.evaluateEvolution(withBonus)
                val finalPet = if (evolved != null) {
                    com.digiwear.core.engine.EvolutionEngine.evolve(withBonus, evolved, nowSec)
                } else withBonus
                db.phoneDao().insertSnapshot(com.digiwear.companion.data.SyncedPetEntity.fromDomain(finalPet))
                com.digiwear.companion.widget.DigiWearAppWidgetProvider.updateAllWidgets(context)
                if (liveWatchState != null) {
                    wearableManager.sendPetStateToWatch(finalPet)
                }
            }

            if (won) {
                buzzerManager.playVictoryFanfare()
                battleFinishedMsg = "VICTORY! Floor $currentFloorNumber Cleared: ${floor.rewardDescription}"

                // Unlock next floor
                if (currentFloorNumber >= highestUnlockedFloor && highestUnlockedFloor < 100) {
                    highestUnlockedFloor = currentFloorNumber + 1
                    prefs.edit().putInt("highest_floor", highestUnlockedFloor).apply()
                }
            } else {
                buzzerManager.playDefeatJingle()
                battleFinishedMsg = "DEFEAT! Re-train your partner and challenge Floor $currentFloorNumber again."
            }
            isBattling = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "100-FLOOR BATTLE TOWER",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Highest Floor Unlocked: $highestUnlockedFloor / 100",
                        fontSize = 9.sp,
                        color = Color(0xFF80CBC4),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238), contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("BACK", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Checkpoint Jump Row
            Text(
                text = "MILESTONE CHECKPOINTS:",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB0BEC5),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CHECKPOINTS.forEach { cp ->
                    val isUnlocked = highestUnlockedFloor >= cp
                    val isSelected = currentFloorNumber == cp
                    Box(
                        modifier = Modifier
                            .background(
                                when {
                                    isSelected -> Color(0xFF00E5FF)
                                    isUnlocked -> Color(0xFF142436)
                                    else -> Color(0xFF0C131D)
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF) else if (isUnlocked) Color(0xFF37474F) else Color(0xFF1E2836),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable(enabled = isUnlocked && !isBattling) {
                                currentFloorNumber = cp
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "F$cp",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected -> Color.Black
                                isUnlocked -> Color.White
                                else -> Color(0xFF546E7A)
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Floor Arena Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1624)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (floor.isBossFloor) Color(0xFFFFD54F) else Color(0xFF1E3A5F)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Floor Title & Boss Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FLOOR $currentFloorNumber / 100",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF),
                            fontFamily = FontFamily.Monospace
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (floor.isBossFloor) Color(0xFF3E2723) else Color(0xFF132238),
                            border = BorderStroke(1.dp, if (floor.isBossFloor) Color(0xFFFFD54F) else Color(0xFF64B5F6))
                        ) {
                            Text(
                                text = if (floor.isBossFloor) "BOSS MILESTONE" else "TIER ${floor.tierNumber}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (floor.isBossFloor) Color(0xFFFFD54F) else Color(0xFF64B5F6),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = floor.tierName.uppercase(),
                        fontSize = 10.sp,
                        color = Color(0xFF80CBC4),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opponent Portrait & Stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090E17), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF19283E), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(Color(0xFF050810), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (opponentPortrait != null) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawImage(
                                        image = opponentPortrait,
                                        dstOffset = IntOffset.Zero,
                                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                        filterQuality = FilterQuality.Low
                                    )
                                }
                            } else {
                                SpriteCanvasRenderer(
                                    speciesId = floor.opponentSpeciesId,
                                    frame = SpriteFrame.IDLE_A,
                                    size = 64.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = floor.opponentName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${floor.opponentStage.displayName} • ${floor.opponentAttribute.displayName}",
                                fontSize = 9.sp,
                                color = Color(0xFF80CBC4),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "HP: $opponentDisplayHp / ${floor.opponentHp}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252),
                                fontFamily = FontFamily.Monospace
                            )
                            LinearProgressIndicator(
                                progress = { (opponentDisplayHp.toFloat() / floor.opponentHp).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp),
                                color = Color(0xFFFF5252),
                                trackColor = Color(0xFF261820)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "BASE PWR: ${floor.opponentPower}",
                                fontSize = 8.sp,
                                color = Color(0xFFFFD54F),
                                fontFamily = FontFamily.Monospace
                            )
                            if (floor.environmentEffect != "none") {
                                Text(
                                    text = "HAZARD: ${floor.environmentEffect.uppercase()}",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEA80FC),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Player Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090E17), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF19283E), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpriteCanvasRenderer(
                            speciesId = playerSpecies.id,
                            frame = SpriteFrame.IDLE_A,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YOUR PARTNER: ${playerSpecies.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "HP: $playerDisplayHp / ${activePartner?.maxHp ?: playerSpecies.baseHp}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676),
                                fontFamily = FontFamily.Monospace
                            )
                            LinearProgressIndicator(
                                progress = { (playerDisplayHp.toFloat() / (activePartner?.maxHp ?: playerSpecies.baseHp)).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp),
                                color = Color(0xFF00E676),
                                trackColor = Color(0xFF132A1C)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Combat Announcer
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF050810),
                        border = BorderStroke(1.dp, Color(0xFF263238)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentCombatActionText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
                        )
                    }

                    battleFinishedMsg?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (msg.startsWith("VICTORY")) Color(0xFF00E676) else Color(0xFFFF5252),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { startBattle() },
                            enabled = !isBattling,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isBattling) "COMBAT IN PROGRESS..." else "CHALLENGE FLOOR $currentFloorNumber",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (currentFloorNumber < highestUnlockedFloor) {
                            Button(
                                onClick = {
                                    currentFloorNumber++
                                    battleFinishedMsg = null
                                    currentCombatActionText = "READY TO ENGAGE"
                                },
                                enabled = !isBattling,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("NEXT FLOOR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}
