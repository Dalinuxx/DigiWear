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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digiwear.companion.data.PhoneDatabase
import com.digiwear.companion.data.StoredDigimonEntity
import com.digiwear.companion.data.SyncedPetEntity
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.render.SpriteCanvasRenderer
import com.digiwear.companion.widget.DigiWearAppWidgetProvider
import com.digiwear.core.engine.EvolutionEngine
import com.digiwear.core.engine.PetSimulationEngine
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import com.digiwear.core.model.SpriteFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetDashboardScreen(
    db: PhoneDatabase,
    wearableManager: PhoneWearableManager,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToVault: (() -> Unit)? = null
) {
    // 100% Clean Fresh Start: Starts null until loaded from DB or synced from watch.
    var petState by remember { mutableStateOf<PetState?>(null) }
    var syncStatus by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var vaultDepositSuccess by remember { mutableStateOf<String?>(null) }
    var showStarterEggPicker by remember { mutableStateOf(false) }

    // Interactive V-Pet Brick Shell State
    var selectedShellIcon by remember { mutableIntStateOf(0) }
    var shellPreviewStance by remember { mutableIntStateOf(0) } // 0=IDLE, 1=WALK, 2=HAPPY, 3=ATTACK
    var currentShellStyle by remember { mutableStateOf(DigiviceShellStyle.ORIGINAL_1997_GREY) }
    var showPartnerCutIn by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val buzzerManager = remember { com.digiwear.companion.sound.BuzzerManager(context) }
    androidx.compose.runtime.DisposableEffect(buzzerManager) {
        onDispose { buzzerManager.release() }
    }

    val persistPhoneSnapshot: suspend (PetState) -> Unit = { updated ->
        db.phoneDao().insertSnapshot(SyncedPetEntity.fromDomain(updated))
        DigiWearAppWidgetProvider.updateAllWidgets(context)
    }

    val snapshots by db.phoneDao().getAllSnapshotsFlow().collectAsState(initial = emptyList())
    LaunchedEffect(snapshots) {
        val latest = snapshots.firstOrNull()
        if (latest != null) {
            petState = latest.toDomain()
        }
    }

    // Load latest legitimate snapshot or sync from watch on start
    LaunchedEffect(Unit) {
        val latest = db.phoneDao().getLatestSnapshot()
        if (latest != null) {
            petState = latest.toDomain()
        } else {
            // Attempt auto-sync from watch if connected
            val liveWatchState = wearableManager.requestPetStateFromWatch()
            if (liveWatchState != null) {
                petState = liveWatchState
                persistPhoneSnapshot(liveWatchState)
            }
        }
    }

    val stanceNames = listOf("IDLE", "WALK", "HAPPY", "ATTACK")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DigiWearPalette.CyberBg)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val currentPet = petState

            if (currentPet == null) {
                // --- ONBOARDING STATE: ZERO PRE-LOADED MONSTERS ---
                DigiSectionHeader(
                    tag = "ACTIVE PARTNER MONITOR",
                    title = "No Partner Synced",
                    subtitle = "Pair your smartwatch or incubate a fresh starter egg below",
                    accentColor = DigiWearPalette.DigiviceCyan
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFF101722), CircleShape)
                                .border(1.dp, Color(0xFF1E2C3D), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            DigiVectorIcon(type = DigiIconType.EGG, size = 36.dp, tint = DigiWearPalette.CrestGold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "WELCOME, TAMER!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your companion app is currently empty. Sync with your smartwatch to bridge your active partner, or incubate a fresh starter egg:",
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    val liveWatchState = wearableManager.requestPetStateFromWatch()
                                    if (liveWatchState != null) {
                                        petState = liveWatchState
                                        persistPhoneSnapshot(liveWatchState)
                                        syncStatus = "Synced live partner from Smartwatch!"
                                    } else {
                                        syncStatus = "No active partner received. Ensure DigiWear is running on your smartwatch."
                                    }
                                    isSyncing = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DigiWearPalette.DigiviceCyan)
                        ) {
                            DigiVectorIcon(type = DigiIconType.BEAM_ANTENNA, size = 15.dp, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncing) "CONNECTING TO WATCH..." else "SYNC FROM SMARTWATCH",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showStarterEggPicker = !showStarterEggPicker },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DigiWearPalette.CrestGold)
                        ) {
                            DigiVectorIcon(type = DigiIconType.EGG, size = 15.dp, tint = DigiWearPalette.CrestGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showStarterEggPicker) "HIDE STARTER DIGI-EGGS" else "CHOOSE STARTER DIGI-EGG (STANDALONE)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = DigiWearPalette.CrestGold
                            )
                        }

                        if (showStarterEggPicker) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Select one of 16 Canonical Starter Lineages (Starts at Age 0H, 0 Battles):",
                                fontSize = 9.5.sp,
                                color = Color(0xFFFFD54F),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EvolutionEngine.STARTER_EGG_IDS.forEach { eggId ->
                                    val eggSp = PetSpecies.getById(eggId)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .background(DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF455A64), RoundedCornerShape(8.dp))
                                            .clickable {
                                                val freshPartner = PetState(
                                                    speciesId = eggId,
                                                    currentHp = eggSp.baseHp,
                                                    maxHp = eggSp.baseHp,
                                                    hunger = 4,
                                                    strength = 4,
                                                    weight = eggSp.baselineWeight,
                                                    ageSeconds = 0L,
                                                    careMistakes = 0,
                                                    effort = 0,
                                                    victories = 0,
                                                    totalBattles = 0
                                                )
                                                petState = freshPartner
                                                scope.launch {
                                                    persistPhoneSnapshot(freshPartner)
                                                    wearableManager.sendPetStateToWatch(freshPartner)
                                                }
                                                showStarterEggPicker = false
                                                syncStatus = "Hatched fresh ${eggSp.name}!"
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        SpriteCanvasRenderer(speciesId = eggId, frame = SpriteFrame.IDLE_A, size = 36.dp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(eggSp.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // --- ACTIVE PARTNER DASHBOARD ---
                val currentSpecies = PetSpecies.getById(currentPet.speciesId)

                DigiSectionHeader(
                    tag = "ACTIVE PARTNER MONITOR",
                    title = "#${currentSpecies.id} ${currentSpecies.name}",
                    accentColor = DigiWearPalette.DigiviceCyan,
                    trailingContent = {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    val liveWatchState = wearableManager.requestPetStateFromWatch()
                                    if (liveWatchState != null) {
                                        petState = liveWatchState
                                        persistPhoneSnapshot(liveWatchState)
                                        syncStatus = "Synced live ${currentSpecies.name} from Smartwatch!"
                                    } else {
                                        syncStatus = "Connected watch node responded, but returned no state."
                                    }
                                    isSyncing = false
                                }
                            },
                            border = BorderStroke(1.dp, DigiWearPalette.DigiviceCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.SCAN, size = 13.dp, tint = DigiWearPalette.DigiviceCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSyncing) "SYNCING..." else "SYNC WATCH",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DigiWearPalette.DigiviceCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stage, Attribute & Weight Pills Row (FlowRow to prevent clipping)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StageBadge(currentSpecies.stage.displayName)
                    AttributeBadge(currentSpecies.attribute.displayName)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF172230), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF37474F), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "WT: ${currentPet.weight}G • AGE: ${currentPet.ageSeconds / 3600}H",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB0BEC5),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                syncStatus?.let {
                    Text(
                        text = "▸ $it",
                        fontSize = 10.sp,
                        color = Color(0xFF00E676),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Digivice Shell Edition Selector Chips (2x3 non-clipping grid)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DigiviceShellStyle.entries.chunked(3).forEach { rowStyles ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowStyles.forEach { style ->
                                val isSel = currentShellStyle == style
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSel) style.borderColor.copy(alpha = 0.25f) else Color(0xFF141C24),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSel) style.borderColor else Color(0xFF263238),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { currentShellStyle = style }
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = style.shortName,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSel) FontWeight.Black else FontWeight.Normal,
                                        color = if (isSel) style.borderColor else Color.Gray,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Authentic Bandai V-Pet Brick Shell Viewport
                VPetBrickShellFrame(
                    shellStyle = currentShellStyle,
                    screenBgColor = Color(0xFF8B956D),
                    selectedIconIndex = selectedShellIcon,
                    statusLabel = "STANCE: ${stanceNames[shellPreviewStance]}",
                    onButtonA = { selectedShellIcon = (selectedShellIcon + 1) % 8 },
                    onButtonB = { shellPreviewStance = (shellPreviewStance + 1) % 4 },
                    onButtonC = {
                        selectedShellIcon = 0
                        shellPreviewStance = 0
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clickable { showPartnerCutIn = true },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedPetSprite(
                            speciesId = currentPet.speciesId,
                            stance = shellPreviewStance,
                            size = 112.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Vault Deposit Action
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val entry = StoredDigimonEntity.fromDomain(
                                state = currentPet,
                                species = currentSpecies,
                                notes = "Deposited from Dashboard"
                            )
                            db.phoneDao().insertVaultPet(entry)
                            db.phoneDao().clearActivePet()
                            wearableManager.sendRemoteCareCommand("DEPOSIT_VAULT")
                            petState = null
                            vaultDepositSuccess = "Stored ${currentSpecies.name} in DigiVault! Active partner cleared."
                            delay(3000L)
                            vaultDepositSuccess = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (vaultDepositSuccess != null) Color(0xFF00E676) else DigiWearPalette.DigiviceCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DigiWearPalette.DigiviceCyan)
                ) {
                    DigiVectorIcon(type = DigiIconType.VAULT_SAFE, size = 16.dp, tint = if (vaultDepositSuccess != null) Color(0xFF00E676) else DigiWearPalette.DigiviceCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = vaultDepositSuccess ?: "FREEZE & DEPOSIT PARTNER TO DIGIVAULT",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (vaultDepositSuccess != null) Color(0xFF00E676) else DigiWearPalette.DigiviceCyan
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Biological Telemetry Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "▸ BIOLOGICAL TELEMETRY & CARE METRICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DigiWearPalette.DigiviceCyan,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        StatGaugeRow(
                            label = "HUNGER",
                            current = currentPet.hunger,
                            max = 4,
                            barColor = Color(0xFFFF5252),
                            heartIcon = DigiIconType.HEART_FULL
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        StatGaugeRow(
                            label = "STRENGTH",
                            current = currentPet.strength,
                            max = 4,
                            barColor = Color(0xFFFFD54F),
                            heartIcon = DigiIconType.HEART_FULL
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        StatGaugeRow(
                            label = "EFFORT",
                            current = currentPet.effort,
                            max = 4,
                            barColor = Color(0xFFCE93D8),
                            heartIcon = DigiIconType.STAR_FULL
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TelemetryTile(
                                modifier = Modifier.weight(1f),
                                label = "MISTAKES",
                                value = "${currentPet.careMistakes}",
                                valueColor = if (currentPet.careMistakes == 0) Color(0xFF00E676) else Color(0xFFFF8A80)
                            )
                            TelemetryTile(
                                modifier = Modifier.weight(1f),
                                label = "OVERFEED",
                                value = "${currentPet.overfeedCount}",
                                valueColor = Color.White
                            )
                            val winPct = if (currentPet.totalBattles > 0) (currentPet.victories * 100) / currentPet.totalBattles else 0
                            TelemetryTile(
                                modifier = Modifier.weight(1f),
                                label = "WIN RATIO",
                                value = "$winPct% (${currentPet.victories}W)",
                                valueColor = Color(0xFF81C784)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Remote V-Pet Care Deck
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "▸ REMOTE V-PET CARE DECK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val executeCareAction: (String, (PetState) -> PetState, String) -> Unit = { remoteCmd, localTransform, label ->
                            scope.launch {
                                val watchUpdated = wearableManager.sendRemoteCareCommand(remoteCmd)
                                val nextState = watchUpdated ?: localTransform(currentPet)
                                val evolved = EvolutionEngine.evaluateEvolution(nextState)
                                val finalState = if (evolved != null) {
                                    EvolutionEngine.evolve(nextState, evolved, System.currentTimeMillis() / 1000L)
                                } else nextState
                                petState = finalState
                                persistPhoneSnapshot(finalState)
                                if (watchUpdated == null || evolved != null) {
                                    wearableManager.sendPetStateToWatch(finalState)
                                }
                                syncStatus = if (evolved != null) {
                                    buzzerManager.playVictoryFanfare()
                                    "[DIGIVOLUTION] Partner evolved into ${evolved.name}!"
                                } else {
                                    "$label applied (${if (watchUpdated != null) "Watch + Phone" else "Phone Local"})"
                                }
                            }
                        }

                        // Care Buttons Row 1: MEAT, VITAMIN, TRAIN (Compact padding, no hyphen breaks)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    buzzerManager.playButtonBeep()
                                    executeCareAction("FEED_MEAL", { PetSimulationEngine.feedMeal(it, System.currentTimeMillis() / 1000L) }, "Fed Meat")
                                    shellPreviewStance = 2
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    DigiVectorIcon(type = DigiIconType.FEED_MEAT, size = 11.dp, tint = Color(0xFFFFB74D))
                                    Text("MEAT", color = Color.White, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    buzzerManager.playButtonBeep()
                                    executeCareAction("FEED_PROTEIN", { PetSimulationEngine.feedProtein(it, System.currentTimeMillis() / 1000L) }, "Fed Vitamin")
                                    shellPreviewStance = 2
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    DigiVectorIcon(type = DigiIconType.PROTEIN_TABLET, size = 11.dp, tint = Color(0xFF4FC3F7))
                                    Text("VITAMIN", color = Color.White, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    buzzerManager.playAttackImpactTone()
                                    executeCareAction("TRAIN_WIN", { PetSimulationEngine.train(it, true, System.currentTimeMillis() / 1000L) }, "Sparring Win")
                                    shellPreviewStance = 3
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    DigiVectorIcon(type = DigiIconType.TRAIN_GLOVE, size = 11.dp, tint = Color(0xFFFF8A80))
                                    Text("TRAIN", color = Color.White, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Care Buttons Row 2: CLEAN, LIGHTS, BANDAGE
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    buzzerManager.playFlushTone()
                                    executeCareAction("CLEAN_WASTE", { PetSimulationEngine.cleanWaste(it, System.currentTimeMillis() / 1000L) }, "Flushed Waste")
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    DigiVectorIcon(type = DigiIconType.CLEAN_FLUSH, size = 11.dp, tint = Color(0xFF80DEEA))
                                    Text("CLEAN", color = Color.White, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    executeCareAction("TOGGLE_LIGHTS", { PetSimulationEngine.toggleLights(it, System.currentTimeMillis() / 1000L) }, "Toggled Lights")
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    DigiVectorIcon(type = DigiIconType.SLEEP_LIGHT, size = 11.dp, tint = Color(0xFFFFE082))
                                    Text("LIGHTS", color = Color.White, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    buzzerManager.playHealTone()
                                    executeCareAction("CURE_INJURY", { PetSimulationEngine.cureInjury(it, System.currentTimeMillis() / 1000L) }, "Cured Injury")
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    DigiVectorIcon(type = DigiIconType.BANDAGE_CROSS, size = 11.dp, tint = Color(0xFFFFB74D))
                                    Text("BANDAGE", color = Color.White, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Cold Sleep Action
                        OutlinedButton(
                            onClick = {
                                buzzerManager.playHealTone()
                                executeCareAction(
                                    "TOGGLE_COLD_SLEEP",
                                    { PetSimulationEngine.toggleColdSleep(it, System.currentTimeMillis() / 1000L) },
                                    if (!currentPet.isFrozen) "Cold Sleep Active" else "Awakened"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (currentPet.isFrozen) Color(0x3300E5FF) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (currentPet.isFrozen) Color(0xFF00E5FF) else Color(0xFF546E7A))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                DigiVectorIcon(
                                    type = DigiIconType.COLD_SLEEP,
                                    size = 14.dp,
                                    tint = if (currentPet.isFrozen) Color(0xFF00E5FF) else Color(0xFF80D8FF)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentPet.isFrozen) "WAKE FROM COLD SLEEP" else "COLD SLEEP / REST MODE (PAUSE DECAY)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentPet.isFrozen) Color(0xFF00E5FF) else Color(0xFF80D8FF),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPartnerCutIn && petState != null) {
        val sp = PetSpecies.getById(petState!!.speciesId)
        PhoneCutInOverlay(
            speciesId = petState!!.speciesId,
            title = sp.name.uppercase(),
            autoDismissMs = 2500L,
            onDismiss = { showPartnerCutIn = false }
        )
    }
}

@Composable
private fun StatGaugeRow(
    label: String,
    current: Int,
    max: Int,
    barColor: Color,
    heartIcon: DigiIconType
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DigiVectorIcon(type = heartIcon, size = 12.dp, tint = barColor)
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.LightGray
            )
        }
        VPetMeterBar(
            count = current,
            max = max,
            isHearts = heartIcon == DigiIconType.HEART_FULL,
            iconSize = 12.dp,
            activeColor = barColor
        )
    }
}

@Composable
private fun TelemetryTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        modifier = modifier
            .background(DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF222E3C), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 8.5.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = valueColor, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun AnimatedPetSprite(
    speciesId: Int,
    stance: Int,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    var frameToggle by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500L)
            frameToggle = !frameToggle
        }
    }
    val frame = when (stance) {
        1 -> if (frameToggle) SpriteFrame.WALK_A else SpriteFrame.WALK_B
        2 -> if (frameToggle) SpriteFrame.fromIndex(4) else SpriteFrame.fromIndex(5)
        3 -> if (frameToggle) SpriteFrame.ATTACK_CHARGE else SpriteFrame.ATTACK_RELEASE
        else -> if (frameToggle) SpriteFrame.IDLE_A else SpriteFrame.IDLE_B
    }
    SpriteCanvasRenderer(
        speciesId = speciesId,
        frame = frame,
        size = size,
        modifier = modifier
    )
}
