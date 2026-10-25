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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digiwear.companion.NfcBattleBridge
import com.digiwear.companion.data.PhoneDatabase
import com.digiwear.companion.data.SyncedPetEntity
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.nfc.NfcDuelManager
import com.digiwear.companion.render.SpriteCanvasRenderer
import com.digiwear.companion.widget.DigiWearAppWidgetProvider
import com.digiwear.core.battle.DeterministicBattleSolver
import com.digiwear.core.battle.WildScanEncounter
import com.digiwear.core.battle.WildScanEncounterGenerator
import com.digiwear.core.codec.CommCodeCodec
import com.digiwear.core.codec.CommCodePayload
import com.digiwear.core.fusion.JogressEngine
import com.digiwear.core.model.BattleFighter
import com.digiwear.core.model.BattleMode
import com.digiwear.core.model.BattleResult
import com.digiwear.core.model.ElementalAttribute
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import com.digiwear.core.model.PetState
import com.digiwear.core.model.SpriteFrame
import com.digiwear.core.qrcode.QrMatrixGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val ONLINE_LOBBY_TAMERS = listOf(
    Triple("Tamer_Taichi [A-Com]", 401, 0x4101),    // MetalGreymon
    Triple("Tamer_Yamato [D-Com]", 404, 0x4502),    // WereGarurumon
    Triple("Tamer_Ruki [WiFiCom]", 425, 0x7711),    // Taomon
    Triple("Tamer_Takato [0nl1ne]", 515, 0x8822),   // Dukemon
    Triple("RoyalKnight_X [WiFiCom]", 523, 0x99AA), // Alphamon
    Triple("DarkMaster_Z [A-Com]", 509, 0xDEAD)     // Piedmon
)

private val COLOSSEUM_ENVIRONMENTS = listOf(
    "Normal",
    "Thunderstorm",
    "Magma Heat",
    "Tidal Current",
    "Miasma Fog",
    "Zero Gravity",
    "Apocalymon Void",
    "X-Program Fog",
    "Cocytus Blizzard",
    "Dark Area Sin Gates",
    "GRB Wave Cataclysm",
    "Quantum Singularity"
)

private data class AvailableFighter(
    val speciesId: Int,
    val victories: Int,
    val effort: Int,
    val label: String
)

@Composable
fun BattleHubScreen(
    db: PhoneDatabase,
    wearableManager: PhoneWearableManager
) {
    val vaultPets by db.phoneDao().getAllVaultPetsFlow().collectAsState(initial = emptyList())
    var activeSnapshot by remember { mutableStateOf<SyncedPetEntity?>(null) }

    var ownMonsterId by remember { mutableStateOf<Int?>(null) }
    var ownVictories by remember { mutableIntStateOf(0) }
    var ownEffort by remember { mutableIntStateOf(0) }
    var ownSeed by remember { mutableIntStateOf(0x1337) }
    var generatedCode by remember { mutableStateOf("") }
    var generatedDigiRom by remember { mutableStateOf("") }
    var inputCode by remember { mutableStateOf("") }
    var decodedOpponent by remember { mutableStateOf<CommCodePayload?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var battleResult by remember { mutableStateOf<BattleResult?>(null) }
    var beamStatus by remember { mutableStateOf<String?>(null) }

    var showQrMatrix by remember { mutableStateOf(false) }
    var showFighterPicker by remember { mutableStateOf(false) }
    var showPayloadSpecs by remember { mutableStateOf(false) }
    var isCameraScannerOpen by remember { mutableStateOf(false) }
    var scannerStatusError by remember { mutableStateOf<String?>(null) }

    // 4-Mode Battle Command Center State
    var selectedBattleMode by remember { mutableStateOf(BattleMode.NFC_TAP_DUEL) }
    var barcodeScanInput by remember { mutableStateOf("4902425617220") }
    var activeWildEncounter by remember { mutableStateOf<WildScanEncounter?>(null) }
    var selectedEnvironment by remember { mutableStateOf("Normal") }
    var showBattleTower by remember { mutableStateOf(false) }

    // Live Animated Face-to-Face Battle Arena State
    var battleJob by remember { mutableStateOf<Job?>(null) }
    var isAnimatingBattle by remember { mutableStateOf(false) }
    var currentTurnIndex by remember { mutableIntStateOf(0) }
    var qteActive by remember { mutableStateOf(false) }
    var critBoostTriggered by remember { mutableStateOf(false) }
    var turnFxProgress by remember { mutableFloatStateOf(0f) }
    var activeCutInSpeciesId by remember { mutableStateOf<Int?>(null) }
    var activeCutInTitle by remember { mutableStateOf("CRITICAL HIT") }
    var encounterFlashEnemyId by remember { mutableStateOf<Int?>(null) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val buzzerManager = remember { com.digiwear.companion.sound.BuzzerManager(context) }
    var retroSoundEnabled by remember { mutableStateOf(buzzerManager.isEnabled) }

    DisposableEffect(Unit) {
        onDispose {
            battleJob?.cancel()
            buzzerManager.release()
        }
    }

    LaunchedEffect(Unit) {
        val latest = db.phoneDao().getLatestSnapshot()
        if (latest != null) {
            activeSnapshot = latest
            ownMonsterId = latest.speciesId
            ownVictories = latest.victories
            ownEffort = latest.effort
        }
    }

    // Fallback to vault pet if activeSnapshot is absent but vault has monsters
    LaunchedEffect(vaultPets, activeSnapshot) {
        if (ownMonsterId == null) {
            if (activeSnapshot != null) {
                ownMonsterId = activeSnapshot!!.speciesId
                ownVictories = activeSnapshot!!.victories
                ownEffort = activeSnapshot!!.effort
            } else if (vaultPets.isNotEmpty()) {
                val first = vaultPets.first()
                ownMonsterId = first.speciesId
                ownVictories = first.victories
                ownEffort = first.effort
            }
        }
    }

    val ownSpecies = remember(ownMonsterId) {
        ownMonsterId?.let { PetSpecies.getById(it) }
    }

    // Handle NFC Tag or Watch-to-Phone NFC Tap
    LaunchedEffect(ownSpecies) {
        NfcBattleBridge.incomingCodeFlow.collect { scannedCode ->
            if (!scannedCode.isNullOrBlank()) {
                if (selectedBattleMode == BattleMode.WILD_SCAN && ownSpecies != null) {
                    val enc = WildScanEncounterGenerator.generateFromScan(
                        rawScanData = scannedCode,
                        scanSource = "NFC_TAG",
                        playerStage = ownSpecies.stage
                    )
                    activeWildEncounter = enc
                    inputCode = enc.commCode128
                    selectedEnvironment = enc.environmentHazard
                    beamStatus = "[SCAN] Wild ${enc.species.name} (${enc.habitatZone}) spawned from NFC Tag!"
                } else if (selectedBattleMode == BattleMode.ACOM_DCOM_WIFICOM) {
                    try {
                        val payload = CommCodeCodec.decodeAuto(scannedCode)
                        ownMonsterId = payload.monsterId
                        ownVictories = payload.historicalVictories
                        ownEffort = payload.trainingEffort
                        ownSeed = payload.battleRngSeed
                        val sp = PetSpecies.getById(payload.monsterId)
                        beamStatus = "[RELAY] NFC Watch Relay: Loaded ${sp.name} (${scannedCode.take(14)}) into A-Com Transceiver!"
                    } catch (_: Exception) {
                        inputCode = scannedCode
                        beamStatus = "[RELAY] Scanned A-Com / DigiROM Packet via NFC!"
                    }
                } else {
                    inputCode = scannedCode
                    validationError = null
                    decodedOpponent = try { CommCodeCodec.decodeAuto(scannedCode) } catch (_: Exception) { null }
                    beamStatus = "[NFC] Opponent scanned via NFC Tap-to-Duel!"
                }
                NfcBattleBridge.incomingCodeFlow.value = null
            }
        }
    }

    // Handle Watch DataLayer A-Com Relay ("RELAY DIGIROM TO PHONE" button on Watch)
    LaunchedEffect(Unit) {
        NfcBattleBridge.incomingWatchRelayFlow.collect { watchDigiRom ->
            if (!watchDigiRom.isNullOrBlank()) {
                selectedBattleMode = BattleMode.ACOM_DCOM_WIFICOM
                try {
                    val payload = CommCodeCodec.decodeAuto(watchDigiRom)
                    ownMonsterId = payload.monsterId
                    ownSeed = payload.battleRngSeed
                    val sp = PetSpecies.getById(payload.monsterId)
                    beamStatus = "[WATCH] A-Com Relay Received: ${sp.name} ($watchDigiRom) ready for Online Battle!"
                } catch (_: Exception) {
                    beamStatus = "[WATCH] A-Com Relay Received: $watchDigiRom"
                }
                NfcBattleBridge.incomingWatchRelayFlow.value = null
            }
        }
    }

    // Generate own code on load / change
    LaunchedEffect(ownMonsterId, ownVictories, ownEffort, ownSeed) {
        if (ownMonsterId != null && ownSpecies != null) {
            generatedCode = CommCodeCodec.encode128(
                monsterId = ownMonsterId!!,
                stage = ownSpecies.stage,
                attribute = ownSpecies.attribute,
                basePowerHp = ownSpecies.basePower,
                historicalVictories = ownVictories,
                trainingEffort = ownEffort,
                battleRngSeed = ownSeed
            )
            generatedDigiRom = CommCodeCodec.encodeDigiRom(
                monsterId = ownMonsterId!!,
                stage = ownSpecies.stage,
                attribute = ownSpecies.attribute,
                basePowerHp = ownSpecies.basePower,
                battleRngSeed = ownSeed
            )
        } else {
            generatedCode = ""
            generatedDigiRom = ""
        }
    }

    val ownQrMatrix = remember(generatedCode) {
        if (generatedCode.isNotBlank()) {
            try {
                QrMatrixGenerator.generateQrMatrix(generatedCode)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val launchAnimatedBattle: (String, BattleMode, String) -> Unit = launchAnimatedBattle@{ codeToFight, mode, envHazard ->
        if (isAnimatingBattle || ownMonsterId == null || ownSpecies == null) return@launchAnimatedBattle
        battleJob?.cancel()
        try {
            val payload = CommCodeCodec.decodeAuto(codeToFight)
            decodedOpponent = payload
            validationError = null

            val ownSp = ownSpecies
            val oppSp = PetSpecies.getById(payload.monsterId)

            val fA = BattleFighter(
                id = "player",
                speciesId = ownMonsterId!!,
                name = ownSp.name,
                stage = ownSp.stage,
                attribute = ownSp.attribute,
                maxHp = ownSp.baseHp,
                currentHp = ownSp.baseHp,
                basePower = ownSp.basePower,
                victories = ownVictories,
                effort = ownEffort,
                battleSeed = ownSeed
            )
            val fB = BattleFighter(
                id = "opponent",
                speciesId = payload.monsterId,
                name = oppSp.name,
                stage = payload.stage,
                attribute = payload.attribute,
                maxHp = oppSp.baseHp,
                currentHp = oppSp.baseHp,
                basePower = payload.basePowerHp.coerceAtLeast(oppSp.basePower),
                victories = payload.historicalVictories,
                effort = payload.trainingEffort,
                battleSeed = payload.battleRngSeed
            )

            val normalRes = DeterministicBattleSolver.resolveBattle(
                fighterA = fA,
                fighterB = fB,
                environmentEffect = envHazard,
                attackerCritBoost = false,
                battleMode = mode
            )
            val critRes = DeterministicBattleSolver.resolveBattle(
                fighterA = fA,
                fighterB = fB,
                environmentEffect = envHazard,
                attackerCritBoost = true,
                battleMode = mode
            )

            var activeRes = normalRes
            battleResult = activeRes
            isAnimatingBattle = true
            currentTurnIndex = 0
            critBoostTriggered = false

            battleJob = scope.launch {
                try {
                    encounterFlashEnemyId = fB.speciesId
                    buzzerManager.playAttentionBeep()
                    delay(1050L)
                    encounterFlashEnemyId = null

                    var idx = 0
                    while (idx < activeRes.turns.size) {
                        currentTurnIndex = idx
                        val initialTurn = activeRes.turns[idx]
                        if (initialTurn.attackerIndex == 0) {
                            qteActive = true
                            delay(420L)
                            qteActive = false
                            if (critBoostTriggered) {
                                activeRes = critRes
                                battleResult = critRes
                            }
                        }

                        val turn = activeRes.turns.getOrNull(idx) ?: break

                        val isCrit = turn.attackType == "MEGA_HIT" || (turn.attackerIndex == 0 && critBoostTriggered)
                        if (isCrit) {
                            val attackerId = if (turn.attackerIndex == 0) fA.speciesId else fB.speciesId
                            activeCutInTitle = if (turn.attackType == "MEGA_HIT") "MEGA HIT" else "CRITICAL HIT"
                            activeCutInSpeciesId = attackerId
                            delay(850L)
                            activeCutInSpeciesId = null
                        } else {
                            buzzerManager.playAttackImpactTone()
                        }

                        for (step in 1..8) {
                            turnFxProgress = step / 8f
                            delay(65L)
                        }
                        idx++
                    }
                    if (activeRes.winnerIndex == 0) {
                        buzzerManager.playVictoryFanfare()
                    } else {
                        buzzerManager.playDefeatJingle()
                    }
                } finally {
                    isAnimatingBattle = false
                    encounterFlashEnemyId = null
                    activeCutInSpeciesId = null
                    qteActive = false
                }
            }
        } catch (e: Exception) {
            validationError = "Validation Failed: ${e.message}"
            isAnimatingBattle = false
        }
    }

    if (showBattleTower) {
        BattleTowerScreen(
            db = db,
            wearableManager = wearableManager,
            onBack = { showBattleTower = false }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DigiWearPalette.CyberBg)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DigiSectionHeader(
                tag = "D-COM // 4-MODE BATTLE COMMAND CENTER",
                title = selectedBattleMode.displayName,
                subtitle = "Canonical V-Pet battle formulas, Face-to-Face LCD Arena & Watch Relay",
                accentColor = Color(0xFFEF5350)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(if (retroSoundEnabled) Color(0xFF1B3B2B) else Color(0xFF263238), RoundedCornerShape(8.dp))
                        .border(1.dp, if (retroSoundEnabled) Color(0xFF00E676) else Color(0xFF546E7A), RoundedCornerShape(8.dp))
                        .clickable {
                            buzzerManager.isEnabled = !buzzerManager.isEnabled
                            retroSoundEnabled = buzzerManager.isEnabled
                            if (retroSoundEnabled) buzzerManager.playButtonBeep()
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        DigiVectorIcon(
                            type = if (retroSoundEnabled) DigiIconType.SOUND_ON else DigiIconType.SOUND_OFF,
                            size = 11.dp,
                            tint = if (retroSoundEnabled) Color(0xFF00E676) else Color(0xFFB0BEC5)
                        )
                        Text(
                            text = if (retroSoundEnabled) "RETRO BUZZER: ON" else "RETRO BUZZER: MUTED",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (retroSoundEnabled) Color(0xFF00E676) else Color(0xFFB0BEC5),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Distinct Battle Mode Selector Tabs
            val battleModes = listOf(
                Triple(BattleMode.NFC_TAP_DUEL, DigiIconType.NFC_WAVE, "NFC DUEL"),
                Triple(BattleMode.WILD_SCAN, DigiIconType.RADAR_SCAN, "WILD SCAN"),
                Triple(BattleMode.ACOM_DCOM_WIFICOM, DigiIconType.COMM_ANTENNA, "D-COM / WIFI"),
                Triple(BattleMode.ADVENTURE, DigiIconType.TROPHY_CROWN, "COLOSSEUM")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                battleModes.forEach { opt ->
                    val mode = opt.first
                    val iconType = opt.second
                    val label = opt.third
                    val active = selectedBattleMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (active) Color(0xFF3E1F29) else DigiWearPalette.CardSurface,
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                if (active) 1.5.dp else 1.dp,
                                if (active) Color(0xFFFF5252) else DigiWearPalette.CardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedBattleMode = mode }
                            .padding(vertical = 9.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DigiVectorIcon(
                                type = iconType,
                                size = 10.dp,
                                tint = if (active) Color(0xFFFF8A80) else Color(0xFFB0BEC5)
                            )
                            Text(
                                text = label,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (active) Color(0xFFFF8A80) else Color(0xFFB0BEC5),
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // =========================================================================
            // FIGHTER RESOLUTION: ACTIVE PARTNER OR VAULT PARTNER (ZERO PRE-LOADED CHEATS)
            // =========================================================================
            // FIGHTER RESOLUTION: ACTIVE PARTNER OR VAULT PARTNER (ZERO PRE-LOADED CHEATS)
            // =========================================================================
            val availableFighters = remember(activeSnapshot, vaultPets) {
                val list = mutableListOf<AvailableFighter>()
                activeSnapshot?.let {
                    list.add(AvailableFighter(it.speciesId, it.victories, it.effort, "ACTIVE"))
                }
                vaultPets.forEach { vp ->
                    if (list.none { it.speciesId == vp.speciesId && it.label == "ACTIVE" }) {
                        list.add(AvailableFighter(vp.speciesId, vp.victories, vp.effort, vp.customNickname.ifEmpty { "VAULT" }))
                    }
                }
                list
            }

            if (availableFighters.isEmpty() || ownMonsterId == null || ownSpecies == null) {
                // Empty state: User has not raised or synced any Digimon yet
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B141A)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF5350))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DigiVectorIcon(type = DigiIconType.TROPHY_CROWN, size = 36.dp, tint = Color(0xFFEF5350))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NO BATTLE PARTNER DETECTED",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF8A80),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The D-Com Transceiver requires an active partner to battle.\nSync your partner from your Smartwatch or hatch an egg in the V-Pet dashboard first.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val watchState = wearableManager.requestPetStateFromWatch()
                                    if (watchState != null) {
                                        ownMonsterId = watchState.speciesId
                                        ownVictories = watchState.victories
                                        ownEffort = watchState.effort
                                        val sp = PetSpecies.getById(watchState.speciesId)
                                        beamStatus = "[SYNC] Pulled live ${sp.name} from Smartwatch!"
                                    } else {
                                        beamStatus = "Watch offline â€” ensure DigiWatch is running on your smartwatch."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                DigiVectorIcon(type = DigiIconType.RADAR_SCAN, size = 12.dp, tint = Color.White)
                                Text("SYNC FROM SMARTWATCH", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                val ownFighterPreview = remember(ownMonsterId, ownVictories, ownEffort, ownSeed) {
                    BattleFighter(
                        id = "player",
                        speciesId = ownMonsterId!!,
                        name = ownSpecies.name,
                        stage = ownSpecies.stage,
                        attribute = ownSpecies.attribute,
                        maxHp = ownSpecies.baseHp,
                        currentHp = ownSpecies.baseHp,
                        basePower = ownSpecies.basePower,
                        victories = ownVictories,
                        effort = ownEffort,
                        battleSeed = ownSeed
                    )
                }
                val ownEffPower = remember(ownFighterPreview) {
                    DeterministicBattleSolver.computeEffectivePower(ownFighterPreview)
                }

                // =========================================================================
                // ACTIVE FIGHTER HEADER CARD (ALWAYS VISIBLE & COMPACT)
                // =========================================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                SpriteCanvasRenderer(
                                    speciesId = ownSpecies.id,
                                    frame = SpriteFrame.IDLE_A,
                                    size = 40.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = ownSpecies.name.uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        StageBadge(ownSpecies.stage.displayName)
                                        AttributeBadge(ownSpecies.attribute.displayName)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "BASE ${ownSpecies.basePower}P (EFF: ${ownEffPower}P) â€¢ ${ownSpecies.baseHp} HP â€¢ ${ownVictories}W â€¢ EFFORT ${ownEffort}/4",
                                        fontSize = 9.sp,
                                        color = Color(0xFF80CBC4),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            if (availableFighters.size > 1) {
                                OutlinedButton(
                                    onClick = { showFighterPicker = !showFighterPicker },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(
                                        text = if (showFighterPicker) "DONE" else "SWITCH",
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (showFighterPicker && availableFighters.size > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "SELECT BATTLE FIGHTER (${availableFighters.size} AVAILABLE):",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF90A4AE),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableFighters.forEach { fighter ->
                                    val sp = PetSpecies.getById(fighter.speciesId)
                                    val isSelected = ownMonsterId == fighter.speciesId
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) Color(0xFF3E1F29) else DigiWearPalette.CardInner,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                if (isSelected) 1.5.dp else 1.dp,
                                                if (isSelected) Color(0xFFFF5252) else Color(0xFF263238),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                ownMonsterId = fighter.speciesId
                                                ownVictories = fighter.victories
                                                ownEffort = fighter.effort
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SpriteCanvasRenderer(
                                            speciesId = sp.id,
                                            frame = SpriteFrame.IDLE_A,
                                            size = 24.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "${sp.name} [${fighter.label}]",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFFCFD8DC)
                                            )
                                            Text(
                                                text = "${sp.stage.displayName} â€¢ ${sp.basePower}P â€¢ ${fighter.victories}W",
                                                fontSize = 8.sp,
                                                color = if (isSelected) Color(0xFFFF8A80) else Color(0xFF90A4AE),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                beamStatus?.let {
                    Text(
                        text = "â–¸ $it",
                        fontSize = 10.5.sp,
                        color = Color(0xFF00E676),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // =========================================================================
                // LIVE ANIMATED FACE-TO-FACE LCD VS ARENA & RESULT (SHOWN DURING/AFTER DUEL)
                // =========================================================================
                if (decodedOpponent != null && (isAnimatingBattle || battleResult != null)) {
                    val opp = decodedOpponent!!
                    val oppSp = PetSpecies.getById(opp.monsterId)
                    val ownSp = ownSpecies

                    val activeTurn = if (isAnimatingBattle && battleResult != null) {
                        battleResult!!.turns.getOrNull(currentTurnIndex)
                    } else {
                        battleResult?.turns?.lastOrNull()
                    }

                    val playerRemHp = when {
                        battleResult == null -> ownSp.baseHp
                        !isAnimatingBattle -> battleResult!!.fighterAFinalHp
                        activeTurn == null -> battleResult!!.fighterA.maxHp
                        activeTurn.attackerIndex == 1 -> activeTurn.defenderRemainingHp
                        else -> battleResult!!.turns.take(currentTurnIndex).lastOrNull { it.attackerIndex == 1 }?.defenderRemainingHp ?: battleResult!!.fighterA.maxHp
                    }

                    val oppRemHp = when {
                        battleResult == null -> oppSp.baseHp
                        !isAnimatingBattle -> battleResult!!.fighterBFinalHp
                        activeTurn == null -> battleResult!!.fighterB.maxHp
                        activeTurn.attackerIndex == 0 -> activeTurn.defenderRemainingHp
                        else -> battleResult!!.turns.take(currentTurnIndex).lastOrNull { it.attackerIndex == 0 }?.defenderRemainingHp ?: battleResult!!.fighterB.maxHp
                    }

                    val playerHpFrac = (playerRemHp.toFloat() / ownSp.baseHp.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val oppHpFrac = (oppRemHp.toFloat() / oppSp.baseHp.coerceAtLeast(1)).coerceIn(0f, 1f)

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF15221D), RoundedCornerShape(14.dp))
                            .border(1.5.dp, Color(0xFF4CAF50), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        EnvironmentParticleCanvas(environmentEffect = selectedEnvironment)
                        if (isAnimatingBattle && activeTurn != null) {
                            val attackerSp = if (activeTurn.attackerIndex == 0) ownSp else oppSp
                            BattleClashFxCanvas(
                                attackerIndex = activeTurn.attackerIndex,
                                attribute = attackerSp.attribute,
                                attackType = activeTurn.attackType,
                                isCritical = activeTurn.isCritical,
                                moveName = activeTurn.moveName,
                                damage = activeTurn.damage,
                                progress = turnFxProgress
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${selectedBattleMode.displayName.uppercase()} â€¢ ENV: ${selectedEnvironment.uppercase()}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = DigiWearPalette.CrestGold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Player Fighter
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val playerArenaFrame = when {
                                        !isAnimatingBattle && battleResult != null ->
                                            if (battleResult!!.winnerIndex == 0) SpriteFrame.JOY_EAT else SpriteFrame.INJURED
                                        activeTurn == null -> SpriteFrame.IDLE_A
                                        activeTurn.attackerIndex == 0 -> when {
                                            qteActive || turnFxProgress < 0.25f -> SpriteFrame.ATTACK_CHARGE
                                            turnFxProgress < 0.65f -> SpriteFrame.ATTACK_RELEASE
                                            else -> SpriteFrame.IDLE_B
                                        }
                                        turnFxProgress >= 0.65f -> SpriteFrame.INJURED
                                        else -> SpriteFrame.IDLE_A
                                    }
                                    SpriteCanvasRenderer(
                                        speciesId = ownSp.id,
                                        frame = playerArenaFrame,
                                        size = 64.dp,
                                        flipHorizontal = true
                                    )
                                    Text(ownSp.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80CBC4))
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(84.dp)
                                            .height(6.dp)
                                            .background(Color(0xFF263238), RoundedCornerShape(3.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(playerHpFrac)
                                                .height(6.dp)
                                                .background(Color(0xFF00E676), RoundedCornerShape(3.dp))
                                        )
                                    }
                                    Text("$playerRemHp / ${ownSp.baseHp} HP â€¢ ${ownEffPower}P", fontSize = 9.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                                }

                                // Center Clash
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF3E1C1C), RoundedCornerShape(6.dp))
                                            .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                activeTurn?.attackType == "MEGA_HIT" -> "[ MEGA HIT ]"
                                                activeTurn?.attackerIndex == 0 -> "[ ===> ]"
                                                activeTurn?.attackerIndex == 1 -> "[ <=== ]"
                                                else -> "VS"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFFD54F),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    activeTurn?.let { turn ->
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "R${turn.round} â€¢ HIT ${turn.hitRatePercent}%",
                                            fontSize = 8.5.sp,
                                            color = Color(0xFF90CAF9),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Opponent Fighter
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val oppArenaFrame = when {
                                        !isAnimatingBattle && battleResult != null ->
                                            if (battleResult!!.winnerIndex == 1) SpriteFrame.JOY_EAT else SpriteFrame.INJURED
                                        activeTurn == null -> SpriteFrame.IDLE_A
                                        activeTurn.attackerIndex == 1 -> when {
                                            turnFxProgress < 0.25f -> SpriteFrame.ATTACK_CHARGE
                                            turnFxProgress < 0.65f -> SpriteFrame.ATTACK_RELEASE
                                            else -> SpriteFrame.IDLE_B
                                        }
                                        turnFxProgress >= 0.65f -> SpriteFrame.INJURED
                                        else -> SpriteFrame.IDLE_A
                                    }
                                    SpriteCanvasRenderer(
                                        speciesId = oppSp.id,
                                        frame = oppArenaFrame,
                                        size = 64.dp,
                                        flipHorizontal = false
                                    )
                                    Text(oppSp.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80))
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(84.dp)
                                            .height(6.dp)
                                            .background(Color(0xFF263238), RoundedCornerShape(3.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(oppHpFrac)
                                                .height(6.dp)
                                                .background(Color(0xFFFF5252), RoundedCornerShape(3.dp))
                                        )
                                    }
                                    Text("$oppRemHp / ${oppSp.baseHp} HP â€¢ ${opp.basePowerHp}P", fontSize = 9.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                                }
                            }

                            if (isAnimatingBattle && activeTurn != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${activeTurn.attackerName} [${activeTurn.attackType}] -> -${activeTurn.damage} HP!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTurn.isAdvantage) Color(0xFF00E676) else Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (qteActive) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            critBoostTriggered = true
                                            qteActive = false
                                            buzzerManager.playButtonBeep()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            DigiVectorIcon(type = DigiIconType.LIGHTNING_BOLT, size = 11.dp, tint = Color.White)
                                            Text("TAP TO MEGA-BOOST (+25% CRIT)!", fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                            DigiVectorIcon(type = DigiIconType.LIGHTNING_BOLT, size = 11.dp, tint = Color.White)
                                        }
                                    }
                                }
                            }

                            if (!isAnimatingBattle && battleResult != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        battleResult = null
                                        decodedOpponent = null
                                        validationError = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("CLOSE ARENA / READY NEXT DUEL", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Battle Simulation Log Results
                    battleResult?.let { result ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (result.winnerIndex == 0) Color(0xFF00E676) else Color(0xFFEF5350))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "â–¸ COMBAT RESOLUTION LOG (${result.battleMode.displayName.uppercase()})",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DigiWearPalette.CrestGold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "WINNER: ${result.winnerName ?: "DRAW"} (${result.totalRounds} ROUNDS)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (result.winnerIndex == 0) Color(0xFF00E676) else Color(0xFFFF5252),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = result.formulaSummary,
                                    fontSize = 9.sp,
                                    color = Color(0xFF80CBC4),
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                for (turn in result.turns) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .background(DigiWearPalette.CardInner, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "R${turn.round} [${turn.attackType} â€¢ ${turn.hitRatePercent}%]: ${turn.attackerName} -> ${turn.defenderName}",
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "-${turn.damage} HP ${if (turn.isAdvantage) "[1.2x]" else ""} (${turn.defenderRemainingHp}HP)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (turn.isAdvantage) Color(0xFF00E676) else Color(0xFFFF8A80),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                validationError?.let {
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = Color(0xFFFF5252),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =========================================================================
                // MODE-SPECIFIC SUB-PANELS (NFC, WILD SCAN, D-COM/WIFI, COLOSSEUM)
                // =========================================================================
                when (selectedBattleMode) {
                    BattleMode.NFC_TAP_DUEL -> {
                        // -----------------------------------------------------------------
                        // 1. NFC TAP-TO-DUEL
                        // -----------------------------------------------------------------
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f).padding(end = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF00E676), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "NFC TAP-TO-DUEL TRANSCEIVER",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E676),
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    val isNfcReady = NfcDuelManager.isNfcEnabled(context)
                                    Surface(
                                        color = if (isNfcReady) Color(0xFF1B5E20) else Color(0xFF3E2723),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, if (isNfcReady) Color(0xFF00E676) else Color.Red)
                                    ) {
                                        Text(
                                            text = if (isNfcReady) "NFC READY" else "NFC OFF",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNfcReady) Color(0xFF00E676) else Color.Red,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Touch two phones back-to-back, tap your smartwatch to the back of this phone, or tap a physical NFC Battle Card to duel wirelessly!",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val tag = NfcBattleBridge.pendingTagToWrite
                                            if (tag != null) {
                                                val ok = NfcDuelManager.writeDuelCodeToTag(tag, generatedDigiRom)
                                                beamStatus = if (ok) "[OK] Wrote ${ownSpecies.name} DigiROM to physical NFC Tag!" else "Tag write failed (Check tag write-lock)"
                                            } else {
                                                beamStatus = "Hold physical NFC tag to back of phone, then click Write"
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        DigiVectorIcon(type = DigiIconType.NFC_WAVE, size = 12.dp, tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WRITE TO TAG", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val ok = wearableManager.sendCommCodeToWatch(generatedCode)
                                                beamStatus = if (ok) "Dispatched Comm Code to Watch!" else "Watch not reachable (Offline Mode)"
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                                    ) {
                                        DigiVectorIcon(type = DigiIconType.BEAM_ANTENNA, size = 12.dp, tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("BEAM TO WATCH", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Scanned Opponent via NFC Tap
                                if (decodedOpponent != null && !isAnimatingBattle && battleResult == null) {
                                    val opp = decodedOpponent!!
                                    val oppSp = PetSpecies.getById(opp.monsterId)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF13261E), RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0xFF00E676), RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            SpriteCanvasRenderer(speciesId = oppSp.id, frame = SpriteFrame.IDLE_A, size = 40.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "SCANNED: ${oppSp.name}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF69F0AE)
                                                )
                                                Text(
                                                    text = "${opp.stage.displayName} â€¢ ${opp.attribute.displayName} â€¢ ${opp.basePowerHp} PWR",
                                                    fontSize = 9.sp,
                                                    color = Color.LightGray,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                launchAnimatedBattle(inputCode, BattleMode.NFC_TAP_DUEL, selectedEnvironment)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                        ) {
                                            Text("START DUEL", fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    BattleMode.WILD_SCAN -> {
                        // -----------------------------------------------------------------
                        // 2. WILD BARCODE / QR SCANNER & RADAR
                        // -----------------------------------------------------------------
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF69F0AE).copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "â–¸ WILD BARCODE / QR SCANNER & RADAR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF69F0AE),
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Scan any product barcode (UPC/EAN-13), QR code, or screen code to encounter wild Digimon scaled to your partner's stage!",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = barcodeScanInput,
                                        onValueChange = { barcodeScanInput = it },
                                        label = { Text("UPC / EAN-13 Barcode or NFC UID", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            val enc = WildScanEncounterGenerator.generateFromScan(
                                                rawScanData = barcodeScanInput,
                                                scanSource = "BARCODE",
                                                playerStage = ownSpecies.stage
                                            )
                                            activeWildEncounter = enc
                                            inputCode = enc.commCode128
                                            selectedEnvironment = enc.environmentHazard
                                            decodedOpponent = CommCodeCodec.decodeAuto(enc.commCode128)
                                            beamStatus = "[SCAN] Decoded Barcode -> Wild ${enc.species.name} in ${enc.habitatZone}!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Text("SCAN", fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    }
                                    Button(
                                        onClick = {
                                            scannerStatusError = null
                                            isCameraScannerOpen = true
                                            buzzerManager.playButtonBeep()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                                    ) {
                                        DigiVectorIcon(type = DigiIconType.CAMERA, size = 14.dp, tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("CAM", fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("PRESET BANDAI BARCODES & ARTIFACTS:", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    WildScanEncounterGenerator.PRESET_BARCODES.forEach { (barcode, label) ->
                                        OutlinedButton(
                                            onClick = {
                                                barcodeScanInput = barcode
                                                val enc = WildScanEncounterGenerator.generateFromScan(
                                                    rawScanData = barcode,
                                                    scanSource = "BARCODE",
                                                    playerStage = ownSpecies.stage
                                                )
                                                activeWildEncounter = enc
                                                inputCode = enc.commCode128
                                                selectedEnvironment = enc.environmentHazard
                                                decodedOpponent = CommCodeCodec.decodeAuto(enc.commCode128)
                                            }
                                        ) {
                                            Text(label, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }

                                activeWildEncounter?.let { enc ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF13261E), RoundedCornerShape(10.dp))
                                            .border(1.dp, if (enc.isRareAnomaly) DigiWearPalette.CrestGold else Color(0xFF00E676), RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            SpriteCanvasRenderer(speciesId = enc.species.id, frame = SpriteFrame.IDLE_A, size = 44.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "${if (enc.isRareAnomaly) "[RARE BOSS] " else "WILD: "}${enc.species.name}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (enc.isRareAnomaly) DigiWearPalette.CrestGold else Color(0xFF69F0AE)
                                                )
                                                Text(
                                                    text = "${enc.habitatZone} â€¢ ENV: ${enc.environmentHazard}",
                                                    fontSize = 9.5.sp,
                                                    color = Color.LightGray,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "HP ${enc.fighter.maxHp} â€¢ PWR ${enc.fighter.basePower} â€¢ ${enc.species.attribute.displayName}",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF80CBC4),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Button(
                                                onClick = {
                                                    launchAnimatedBattle(enc.commCode128, BattleMode.WILD_SCAN, enc.environmentHazard)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                            ) {
                                                Text("FIGHT WILD", fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        val ok = wearableManager.sendCommCodeToWatch(enc.commCode128)
                                                        beamStatus = if (ok) "Beamed Wild ${enc.species.name} to Watch!" else "Watch offline"
                                                    }
                                                }
                                            ) {
                                                Text("TO WATCH", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    BattleMode.ACOM_DCOM_WIFICOM -> {
                        // -----------------------------------------------------------------
                        // 3. D-COM / A-COM / WIFICOM COMM CENTER
                        // -----------------------------------------------------------------
                        // Card 1: Your Player Comm Code & DigiROM
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, DigiWearPalette.DigiviceCyan.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "â–¸ YOUR COMM CODE & DIGIROM PACKET",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DigiWearPalette.DigiviceCyan,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    OutlinedButton(
                                        onClick = { showQrMatrix = !showQrMatrix },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            DigiVectorIcon(
                                                type = if (showQrMatrix) DigiIconType.COMM_LINK else DigiIconType.QR_MATRIX,
                                                size = 10.dp,
                                                tint = if (showQrMatrix) DigiWearPalette.DigiviceCyan else Color(0xFF00E676)
                                            )
                                            Text(
                                                text = if (showQrMatrix) "TEXT CODE" else "QR MATRIX",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (showQrMatrix) DigiWearPalette.DigiviceCyan else Color(0xFF00E676)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                if (showQrMatrix) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DigiWearPalette.CardInner, RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0xFF263238), RoundedCornerShape(10.dp))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(170.dp)
                                                    .background(Color.White, RoundedCornerShape(8.dp))
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (ownQrMatrix != null) {
                                                    QrCodeMatrixView(
                                                        matrix = ownQrMatrix,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Text("Generating QR Matrix...", fontSize = 10.sp, color = Color.DarkGray)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "POINT COMPANION CAMERA SCANNER TO DUEL",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = DigiWearPalette.CrestGold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = CommCodeCodec.formatWithHyphens(generatedCode),
                                                fontSize = 8.sp,
                                                color = Color(0xFF90A4AE),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DigiWearPalette.CardInner, RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0xFF263238), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "128-BIT BASE32 COMM CODE:",
                                                fontSize = 8.5.sp,
                                                color = Color(0xFF78909C),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = CommCodeCodec.formatWithHyphens(generatedCode),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00E676),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "A-COM / D-COM / WIFICOM DIGIROM:",
                                                fontSize = 8.5.sp,
                                                color = Color(0xFF78909C),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = generatedDigiRom,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DigiWearPalette.CrestGold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(generatedCode))
                                            Toast.makeText(context, "128-Bit Base32 Code copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        border = BorderStroke(1.dp, DigiWearPalette.DigiviceCyan),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        DigiVectorIcon(type = DigiIconType.COPY, size = 12.dp, tint = DigiWearPalette.DigiviceCyan)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Base32", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(generatedDigiRom))
                                            Toast.makeText(context, "0nl1ne DigiROM copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        border = BorderStroke(1.dp, DigiWearPalette.DigiviceCyan),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        DigiVectorIcon(type = DigiIconType.COPY, size = 12.dp, tint = DigiWearPalette.DigiviceCyan)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("DigiROM", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            ownSeed = Random.nextInt(0xFFFF)
                                            val hex = Integer.toHexString(ownSeed).uppercase()
                                            Toast.makeText(context, "Battle PRNG Seed: 0x$hex", Toast.LENGTH_SHORT).show()
                                        },
                                        border = BorderStroke(1.dp, DigiWearPalette.DigiviceCyan),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        DigiVectorIcon(type = DigiIconType.BOLT, size = 12.dp, tint = DigiWearPalette.DigiviceCyan)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Seed", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                val watchState = wearableManager.requestPetStateFromWatch()
                                                if (watchState != null) {
                                                    ownMonsterId = watchState.speciesId
                                                    ownVictories = watchState.victories
                                                    ownEffort = watchState.effort
                                                    val sp = PetSpecies.getById(watchState.speciesId)
                                                    beamStatus = "[SYNC] Pulled live ${sp.name} from Smartwatch!"
                                                } else {
                                                    beamStatus = "Watch offline â€” tap Watch to Phone via NFC or use local fighter"
                                                }
                                            }
                                        },
                                        border = BorderStroke(1.dp, DigiWearPalette.DigiviceCyan),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("PULL FROM WATCH", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val ok = wearableManager.sendCommCodeToWatch(generatedCode)
                                                beamStatus = if (ok) "Dispatched Comm Code to Watch!" else "Watch not reachable"
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                                    ) {
                                        DigiVectorIcon(type = DigiIconType.BEAM_ANTENNA, size = 12.dp, tint = Color.White)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("BEAM TO WATCH", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Card 2: Opponent Transceiver & Duel Launch
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF8A80).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "â–¸ OPPONENT D-COM TRANSCEIVER & DUEL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8A80),
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        scannerStatusError = null
                                        isCameraScannerOpen = true
                                        buzzerManager.playButtonBeep()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                                ) {
                                    DigiVectorIcon(type = DigiIconType.CAMERA, size = 15.dp, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SCAN CAMERA (TEXT CODES OR QR)", fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = inputCode,
                                    onValueChange = {
                                        inputCode = it
                                        validationError = null
                                        decodedOpponent = try {
                                            CommCodeCodec.decodeAuto(it)
                                        } catch (_: Exception) {
                                            null
                                        }
                                    },
                                    label = { Text("Paste 26-Char Base32 or V1-XXXX-XXXX-XXXX DigiROM", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("SPARRING HOLO-TARGETS:", fontSize = 8.5.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val devimonCode = CommCodeCodec.encode128(
                                                monsterId = 303, // Devimon (Virus)
                                                stage = PetStage.ADULT,
                                                attribute = ElementalAttribute.VIRUS,
                                                basePowerHp = 210,
                                                historicalVictories = 9,
                                                trainingEffort = 3,
                                                battleRngSeed = 0xBEEF
                                            )
                                            inputCode = CommCodeCodec.formatWithHyphens(devimonCode)
                                            decodedOpponent = try { CommCodeCodec.decodeAuto(devimonCode) } catch (_: Exception) { null }
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("Devimon", fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val metalGaruruCode = CommCodeCodec.encode128(
                                                monsterId = 503, // MetalGarurumon (Data)
                                                stage = PetStage.ULTIMATE,
                                                attribute = ElementalAttribute.DATA,
                                                basePowerHp = 280,
                                                historicalVictories = 18,
                                                trainingEffort = 4,
                                                battleRngSeed = 0xCAFE
                                            )
                                            inputCode = CommCodeCodec.formatWithHyphens(metalGaruruCode)
                                            decodedOpponent = try { CommCodeCodec.decodeAuto(metalGaruruCode) } catch (_: Exception) { null }
                                        },
                                        modifier = Modifier.weight(1.3f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("MetalGarurumon", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                                    }

                                    Button(
                                        onClick = {
                                            launchAnimatedBattle(inputCode, BattleMode.ACOM_DCOM_WIFICOM, selectedEnvironment)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                                    ) {
                                        DigiVectorIcon(type = DigiIconType.BATTLE, size = 12.dp, tint = Color.White)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("DUEL!", fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, maxLines = 1)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Card 3: Lobby Spar Tamers
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, DigiWearPalette.CrestGold.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "â–¸ D-COM & 0NL1NE LOBBY SPAR TAMERS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DigiWearPalette.CrestGold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ONLINE_LOBBY_TAMERS.forEach { (tamerName, monId, seed) ->
                                    val tamerSp = PetSpecies.getById(monId)
                                    val tamerRom = CommCodeCodec.encodeDigiRom(
                                        monsterId = monId,
                                        stage = tamerSp.stage,
                                        attribute = tamerSp.attribute,
                                        basePowerHp = tamerSp.basePower,
                                        battleRngSeed = seed
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .background(DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            SpriteCanvasRenderer(speciesId = monId, frame = SpriteFrame.IDLE_A, size = 32.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(tamerName, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(
                                                    text = "${tamerSp.name} â€¢ $tamerRom",
                                                    fontSize = 8.5.sp,
                                                    color = DigiWearPalette.CrestGold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        val ok = wearableManager.sendCommCodeToWatch(tamerRom)
                                                        beamStatus = if (ok) "Relayed $tamerName to Watch!" else "Watch offline"
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("WATCH", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            Button(
                                                onClick = {
                                                    inputCode = tamerRom
                                                    launchAnimatedBattle(tamerRom, BattleMode.ACOM_DCOM_WIFICOM, "Normal")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("DUEL", fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Card 4: Collapsible 128-bit payload specs
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "â–¸ 128-BIT PAYLOAD ARCHITECTURE (SPEC)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFCC80),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    OutlinedButton(
                                        onClick = { showPayloadSpecs = !showPayloadSpecs },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text(
                                            text = if (showPayloadSpecs) "HIDE" else "EXPAND",
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                if (showPayloadSpecs) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    BitFieldRow("000 - 007", "Version (8-bit)", "0x01")
                                    BitFieldRow("008 - 023", "Monster ID (16-bit)", "$ownMonsterId (${ownSpecies.name})")
                                    BitFieldRow("024 - 027", "Stage Index (4-bit)", "${ownSpecies.stage.stageIndex}")
                                    BitFieldRow("028 - 031", "Attribute (4-bit)", ownSpecies.attribute.displayName)
                                    BitFieldRow("032 - 047", "Base Power (16-bit)", "${ownSpecies.basePower} PWR")
                                    BitFieldRow("080 - 095", "PRNG Seed (16-bit)", "0x${Integer.toHexString(ownSeed).uppercase()}")
                                    BitFieldRow("112 - 127", "HMAC-16 Checksum", "Tamper-Proof")
                                }
                            }
                        }
                    }

                    BattleMode.ADVENTURE -> {
                        // -----------------------------------------------------------------
                        // 4. COLOSSEUM & 100-FLOOR BATTLE TOWER
                        // -----------------------------------------------------------------
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF26A69A).copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "â–¸ 100-FLOOR BATTLE TOWER GAUNTLET",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64FFDA),
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        buzzerManager.playButtonBeep()
                                        showBattleTower = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        DigiVectorIcon(type = DigiIconType.TROPHY_CROWN, size = 14.dp, tint = Color.Black)
                                        Text(
                                            text = "ENTER 100-FLOOR BATTLE TOWER",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "SELECT DIGITAL WORLD SECTOR HAZARD:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF80CBC4),
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    COLOSSEUM_ENVIRONMENTS.forEach { env ->
                                        val active = selectedEnvironment == env
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (active) Color(0xFF00695C) else DigiWearPalette.CardInner,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (active) Color(0xFF64FFDA) else Color(0xFF263238),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedEnvironment = env }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                DigiVectorIcon(
                                                    type = DigiIconType.LIGHTNING_BOLT,
                                                    size = 10.dp,
                                                    tint = if (active) Color(0xFF64FFDA) else Color.LightGray
                                                )
                                                Text(
                                                    text = env,
                                                    fontSize = 9.sp,
                                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (active) Color.White else Color.LightGray,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "COLOSSEUM TOURNAMENT ROSTER ($selectedEnvironment):",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF80D8FF),
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ONLINE_LOBBY_TAMERS.forEach { (tamerName, monId, seed) ->
                                    val tamerSp = PetSpecies.getById(monId)
                                    val tamerRom = CommCodeCodec.encodeDigiRom(
                                        monsterId = monId,
                                        stage = tamerSp.stage,
                                        attribute = tamerSp.attribute,
                                        basePowerHp = tamerSp.basePower,
                                        battleRngSeed = seed
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .background(DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            SpriteCanvasRenderer(speciesId = monId, frame = SpriteFrame.IDLE_A, size = 32.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(tamerName, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(
                                                    text = "${tamerSp.name} â€¢ ${tamerSp.stage.displayName} â€¢ ${tamerSp.attribute.displayName}",
                                                    fontSize = 8.5.sp,
                                                    color = Color(0xFF80CBC4),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                inputCode = tamerRom
                                                launchAnimatedBattle(tamerRom, BattleMode.ADVENTURE, selectedEnvironment)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("DUEL", fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // High-Impact Battle Cut-In Overlay
        activeCutInSpeciesId?.let { cutInId ->
            PhoneCutInOverlay(
                speciesId = cutInId,
                title = activeCutInTitle,
                autoDismissMs = null,
                onDismiss = { activeCutInSpeciesId = null }
            )
        }

        // Authentic Battle Encounter Flash Overlay
        encounterFlashEnemyId?.let { enemyId ->
            PhoneEncounterFlashOverlay(
                enemySpeciesId = enemyId,
                isBoss = false,
                onFinished = { encounterFlashEnemyId = null }
            )
        }

        // Real-Time Optical CameraX / MLKit QR Battle Scanner Overlay
        if (isCameraScannerOpen && ownSpecies != null) {
            CameraBattleScanner(
                statusMessage = scannerStatusError,
                onDismiss = {
                    isCameraScannerOpen = false
                    scannerStatusError = null
                },
                onCodeScanned = { scannedCode ->
                    if (!isAnimatingBattle) {
                        if (selectedBattleMode == BattleMode.WILD_SCAN) {
                            try {
                                val enc = WildScanEncounterGenerator.generateFromScan(
                                    rawScanData = scannedCode,
                                    scanSource = "CAMERA_SCAN",
                                    playerStage = ownSpecies.stage
                                )
                                buzzerManager.playAttentionBeep()
                                activeWildEncounter = enc
                                inputCode = enc.commCode128
                                selectedEnvironment = enc.environmentHazard
                                decodedOpponent = CommCodeCodec.decodeAuto(enc.commCode128)
                                beamStatus = "[SCAN] Camera Scanned Wild ${enc.species.name} (${enc.habitatZone})!"
                                isCameraScannerOpen = false
                                scannerStatusError = null
                                launchAnimatedBattle(enc.commCode128, BattleMode.WILD_SCAN, enc.environmentHazard)
                            } catch (e: Exception) {
                                scannerStatusError = "SCAN ERROR: ${e.message}"
                            }
                        } else {
                            try {
                                val payload = CommCodeCodec.decodeAuto(scannedCode)
                                buzzerManager.playAttentionBeep()
                                inputCode = CommCodeCodec.formatWithHyphens(scannedCode)
                                decodedOpponent = payload
                                validationError = null
                                scannerStatusError = null
                                isCameraScannerOpen = false
                                val oppSpecies = PetSpecies.getById(payload.monsterId)
                                beamStatus = "[QR] Verified: Opponent ${oppSpecies.name} locked into Arena!"
                                launchAnimatedBattle(scannedCode, selectedBattleMode, selectedEnvironment)
                            } catch (e: Exception) {
                                scannerStatusError = "HMAC-16 VERIFY FAILED: ${e.message ?: "Invalid checksum"}"
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun BitFieldRow(bits: String, name: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Bits $bits", modifier = Modifier.weight(0.32f), fontSize = 9.5.sp, color = Color.Gray, fontFamily = FontFamily.Monospace, maxLines = 1)
        Text(text = name, modifier = Modifier.weight(0.40f), fontSize = 9.5.sp, color = Color.LightGray, maxLines = 1)
        Text(text = value, modifier = Modifier.weight(0.28f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}
