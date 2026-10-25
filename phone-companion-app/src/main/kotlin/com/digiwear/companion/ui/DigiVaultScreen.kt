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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.digiwear.companion.data.CompanionMemorialEntity
import com.digiwear.companion.data.SyncedPetEntity
import com.digiwear.core.fusion.FusionType
import com.digiwear.core.fusion.JogressEngine
import com.digiwear.core.fusion.JogressRecipe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digiwear.companion.data.PhoneDatabase
import com.digiwear.companion.data.StoredDigimonEntity
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.render.SpriteCanvasRenderer
import com.digiwear.companion.widget.DigiWearAppWidgetProvider
import com.digiwear.core.codec.CommCodeCodec
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import com.digiwear.core.model.PetState
import com.digiwear.core.model.SpriteFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DigiVaultScreen(
    db: PhoneDatabase,
    wearableManager: PhoneWearableManager
) {
    val vaultPets by db.phoneDao().getAllVaultPetsFlow().collectAsState(initial = emptyList())
    val memorials by db.phoneDao().getAllMemorialsFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val buzzerManager = remember { com.digiwear.companion.sound.BuzzerManager(context) }
    var activeCelebrationSpeciesId by remember { mutableStateOf<Int?>(null) }
    var activeCelebrationTitle by remember { mutableStateOf<String?>(null) }

    var activeTab by remember { mutableIntStateOf(0) } // 0: CRYO-VAULT, 1: DIGIDEX, 2: FUSION LAB, 3: MEMORIAL GRAVEYARD
    var searchQuery by remember { mutableStateOf("") }
    var selectedStageFilter by remember { mutableStateOf<PetStage?>(null) }
    var activePetForDeposit by remember { mutableStateOf<PetState?>(null) }
    var nicknameInput by remember { mutableStateOf("") }
    var showDepositDialog by remember { mutableStateOf(false) }
    var commCodeDialogData by remember { mutableStateOf<Pair<String, String>?>(null) }
    var deleteConfirmPet by remember { mutableStateOf<StoredDigimonEntity?>(null) }
    var actionStatusMessage by remember { mutableStateOf<String?>(null) }
    var pendingFusionExecution by remember { mutableStateOf<FusionExecutionCandidate?>(null) }

    // Fusion Lab Filter State
    var fusionSearchQuery by remember { mutableStateOf("") }
    var fusionTypeFilter by remember { mutableStateOf<FusionType?>(null) }
    var onlyShowCanFuseNow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val latest = db.phoneDao().getLatestSnapshot()
        if (latest != null) {
            activePetForDeposit = PetState(
                speciesId = latest.speciesId,
                currentHp = latest.currentHp,
                maxHp = latest.maxHp,
                hunger = latest.hunger,
                strength = latest.strength,
                weight = latest.weight,
                ageSeconds = latest.ageSeconds,
                careMistakes = latest.careMistakes,
                effort = latest.effort,
                overfeedCount = latest.overfeedCount,
                totalBattles = latest.totalBattles,
                victories = latest.victories
            )
        }
    }

    val filteredPets = remember(vaultPets, searchQuery, selectedStageFilter) {
        vaultPets.filter { pet ->
            val matchesSearch = pet.speciesName.contains(searchQuery, ignoreCase = true) ||
                    pet.customNickname.contains(searchQuery, ignoreCase = true)
            val matchesStage = selectedStageFilter == null || pet.stageName == selectedStageFilter?.displayName
            matchesSearch && matchesStage
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DigiWearPalette.CyberBg)
            .padding(16.dp)
    ) {
        // Header
        DigiSectionHeader(
            tag = "DIGIVAULT // CRYO-STASIS & COMPENDIUM",
            title = when (activeTab) {
                0 -> "Preserved Partners (${vaultPets.size})"
                1 -> "500-Species Master DigiDex"
                2 -> "DNA Jogress & DigiXros Lab (${JogressEngine.BASE_RECIPES.size})"
                else -> "Hall of Fame Memorial (${memorials.size})"
            },
            accentColor = when (activeTab) {
                0 -> Color(0xFF26A69A)
                1 -> Color(0xFF00E5FF)
                2 -> Color(0xFFCE93D8)
                else -> Color(0xFF90A4AE)
            },
            trailingContent = {
                if (activeTab == 0 && activePetForDeposit != null) {
                    Button(
                        onClick = { showDepositDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                    ) {
                        DigiVectorIcon(type = DigiIconType.VAULT_SAFE, size = 16.dp, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DEPOSIT ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        )

        actionStatusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = msg,
                fontSize = 11.sp,
                color = Color(0xFF81C784),
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4 Dedicated Sub-Tabs: CRYO-VAULT, DIGIDEX, FUSION LAB, MEMORIAL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 0: CRYO-VAULT
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (activeTab == 0) Color(0xFF00897B) else DigiWearPalette.CardSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        if (activeTab == 0) 1.5.dp else 1.dp,
                        if (activeTab == 0) Color(0xFF64FFDA) else DigiWearPalette.CardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeTab = 0 }
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    DigiVectorIcon(type = DigiIconType.VAULT_SAFE, size = 11.dp, tint = if (activeTab == 0) Color.White else Color(0xFF90A4AE))
                    Text(
                        text = "VAULT (${vaultPets.size})",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (activeTab == 0) Color.White else Color(0xFF90A4AE),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            // Tab 1: DIGIDEX
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (activeTab == 1) Color(0xFF00838F) else DigiWearPalette.CardSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        if (activeTab == 1) 1.5.dp else 1.dp,
                        if (activeTab == 1) Color(0xFF64FFDA) else DigiWearPalette.CardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeTab = 1 }
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    DigiVectorIcon(type = DigiIconType.SEARCH_LENS, size = 11.dp, tint = if (activeTab == 1) Color.White else Color(0xFF80DEEA))
                    Text(
                        text = "DIGIDEX",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (activeTab == 1) Color.White else Color(0xFF80DEEA),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            // Tab 2: FUSION LAB
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (activeTab == 2) Color(0xFF6A1B9A) else DigiWearPalette.CardSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        if (activeTab == 2) 1.5.dp else 1.dp,
                        if (activeTab == 2) Color(0xFF64FFDA) else DigiWearPalette.CardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeTab = 2 }
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    DigiVectorIcon(type = DigiIconType.DNA_HELIX, size = 11.dp, tint = if (activeTab == 2) Color.White else Color(0xFFEA80FC))
                    Text(
                        text = "FUSION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (activeTab == 2) Color.White else Color(0xFFEA80FC),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            // Tab 3: MEMORIAL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (activeTab == 3) Color(0xFF455A64) else DigiWearPalette.CardSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        if (activeTab == 3) 1.5.dp else 1.dp,
                        if (activeTab == 3) Color(0xFF64FFDA) else DigiWearPalette.CardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeTab = 3 }
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    DigiVectorIcon(type = DigiIconType.MEMORIAL_GRAVE, size = 11.dp, tint = if (activeTab == 3) Color.White else Color(0xFF90A4AE))
                    Text(
                        text = "MEMORIAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (activeTab == 3) Color.White else Color(0xFF90A4AE),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (activeTab) {
            0 -> {
                // =========================================================================
                // SUB-TAB 0: CRYO-VAULT
                // =========================================================================
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search vault by species or nickname...", color = Color.Gray, fontSize = 12.sp) },
                    leadingIcon = { DigiVectorIcon(type = DigiIconType.SEARCH_LENS, tint = Color.Gray, size = 16.dp) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip("All", selectedStageFilter == null) { selectedStageFilter = null }
                    PetStage.entries.forEach { stage ->
                        FilterChip(stage.displayName, selectedStageFilter == stage) {
                            selectedStageFilter = if (selectedStageFilter == stage) null else stage
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active partner status preview
                if (activePetForDeposit != null) {
                    val activeSp = PetSpecies.getById(activePetForDeposit!!.speciesId)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121B18)),
                        border = BorderStroke(1.dp, Color(0xFF26A69A))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SpriteCanvasRenderer(speciesId = activeSp.id, frame = SpriteFrame.IDLE_A, size = 36.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("ACTIVE PARTNER: ${activeSp.name.uppercase()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80CBC4), fontFamily = FontFamily.Monospace)
                                    Text("${activeSp.stage.displayName} • ${activePetForDeposit!!.victories}W/${activePetForDeposit!!.totalBattles}B • Effort ${activePetForDeposit!!.effort}/4", fontSize = 9.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                                }
                            }
                            OutlinedButton(
                                onClick = { showDepositDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("DEPOSIT", fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Stored Pets List
                if (filteredPets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NO DIGIMON IN DIGIVAULT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (activePetForDeposit != null)
                                    "Tap 'DEPOSIT ACTIVE' above to store your raised partner in cryo-stasis."
                                else
                                    "Raise a partner on your smartwatch or hatch an egg in the V-Pet dashboard first.",
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredPets, key = { it.id }) { storedPet ->
                            VaultPetCard(
                                pet = storedPet,
                                activePet = activePetForDeposit,
                                onDeployToWatch = {
                                    scope.launch {
                                        val state = storedPet.toDomain().copy(isFrozen = false)
                                        activePetForDeposit = state
                                        db.phoneDao().insertSnapshot(SyncedPetEntity.fromDomain(state))
                                        val ok = wearableManager.sendPetStateToWatch(state)
                                        DigiWearAppWidgetProvider.updateAllWidgets(context)
                                        actionStatusMessage = if (ok) {
                                            "${storedPet.speciesName} deployed to Smartwatch & Dashboard!"
                                        } else {
                                            "${storedPet.speciesName} deployed to Phone Dashboard (Watch offline)"
                                        }
                                    }
                                },
                                onGenerateCommCode = {
                                    val species = PetSpecies.getById(storedPet.speciesId)
                                    val seed = (System.currentTimeMillis() and 0xFFFF).toInt()
                                    val code = CommCodeCodec.encode128(
                                        monsterId = storedPet.speciesId,
                                        stage = species.stage,
                                        attribute = species.attribute,
                                        basePowerHp = species.basePower,
                                        historicalVictories = storedPet.victories,
                                        trainingEffort = storedPet.effort,
                                        battleRngSeed = seed
                                    )
                                    val digiRom = CommCodeCodec.encodeDigiRom(
                                        monsterId = storedPet.speciesId,
                                        stage = species.stage,
                                        attribute = species.attribute,
                                        basePowerHp = species.basePower,
                                        battleRngSeed = seed
                                    )
                                    commCodeDialogData = code to digiRom
                                },
                                onDelete = {
                                    deleteConfirmPet = storedPet
                                },
                                onFuseWithActive = { pet, recipe ->
                                    if (activePetForDeposit != null) {
                                        pendingFusionExecution = FusionExecutionCandidate.ActiveWithVault(
                                            activeState = activePetForDeposit!!,
                                            vaultPet = pet,
                                            recipe = recipe
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            1 -> {
                // =========================================================================
                // SUB-TAB 1: 500-SPECIES MASTER DIGIDEX
                // =========================================================================
                Box(modifier = Modifier.fillMaxSize()) {
                    SpriteStudioScreen(
                        db = db,
                        wearableManager = wearableManager,
                        showHeader = false,
                        contentPadding = PaddingValues(0.dp)
                    )
                }
            }

            2 -> {
                // =========================================================================
                // SUB-TAB 2: FUSION LAB (CANON 85 RECIPES BROWSER & EXECUTION)
                // =========================================================================
                OutlinedTextField(
                    value = fusionSearchQuery,
                    onValueChange = { fusionSearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search 85 recipes by species name...", color = Color.Gray, fontSize = 12.sp) },
                    leadingIcon = { DigiVectorIcon(type = DigiIconType.SEARCH_LENS, tint = Color.Gray, size = 16.dp) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip("All (${JogressEngine.BASE_RECIPES.size})", fusionTypeFilter == null && !onlyShowCanFuseNow) {
                        fusionTypeFilter = null
                        onlyShowCanFuseNow = false
                    }
                    FilterChip("Can Fuse Now", onlyShowCanFuseNow) {
                        onlyShowCanFuseNow = !onlyShowCanFuseNow
                    }
                    FusionType.entries.forEach { fType ->
                        FilterChip(fType.displayName, fusionTypeFilter == fType && !onlyShowCanFuseNow) {
                            onlyShowCanFuseNow = false
                            fusionTypeFilter = if (fusionTypeFilter == fType) null else fType
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Determine owned species IDs
                val ownedActiveId = activePetForDeposit?.speciesId
                val ownedVaultIds = remember(vaultPets) { vaultPets.map { it.speciesId }.toSet() }

                val displayedRecipes = remember(
                    JogressEngine.BASE_RECIPES,
                    fusionSearchQuery,
                    fusionTypeFilter,
                    onlyShowCanFuseNow,
                    ownedVaultIds,
                    ownedActiveId
                ) {
                    JogressEngine.BASE_RECIPES.filter { recipe ->
                        val primarySp = PetSpecies.getById(recipe.primarySpeciesId)
                        val secondarySp = PetSpecies.getById(recipe.secondarySpeciesId)
                        val targetSp = PetSpecies.getById(recipe.targetSpeciesId)

                        val matchesSearch = primarySp.name.contains(fusionSearchQuery, ignoreCase = true) ||
                                secondarySp.name.contains(fusionSearchQuery, ignoreCase = true) ||
                                targetSp.name.contains(fusionSearchQuery, ignoreCase = true)

                        val matchesType = fusionTypeFilter == null || recipe.fusionType == fusionTypeFilter

                        val hasPrimary = (ownedActiveId == recipe.primarySpeciesId) || ownedVaultIds.contains(recipe.primarySpeciesId)
                        val hasSecondary = (ownedActiveId == recipe.secondarySpeciesId) || ownedVaultIds.contains(recipe.secondarySpeciesId)
                        val canFuse = hasPrimary && hasSecondary

                        val matchesCanFuse = !onlyShowCanFuseNow || canFuse

                        matchesSearch && matchesType && matchesCanFuse
                    }
                }

                if (displayedRecipes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DigiVectorIcon(type = DigiIconType.DNA_HELIX, size = 32.dp, tint = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "NO MATCHING RECIPES FOUND",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (onlyShowCanFuseNow)
                                    "No canon fusions match your current active & vault roster.\nRaise or store more species to unlock DNA Jogress!"
                                else
                                    "Try adjusting your search query or filter tags.",
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            displayedRecipes,
                            key = { "${it.primarySpeciesId}_${it.secondarySpeciesId}_${it.targetSpeciesId}" }
                        ) { recipe ->
                            val primarySp = PetSpecies.getById(recipe.primarySpeciesId)
                            val secondarySp = PetSpecies.getById(recipe.secondarySpeciesId)
                            val targetSp = PetSpecies.getById(recipe.targetSpeciesId)

                            val hasPrimaryInActive = ownedActiveId == recipe.primarySpeciesId
                            val hasPrimaryInVault = ownedVaultIds.contains(recipe.primarySpeciesId)
                            val hasSecondaryInActive = ownedActiveId == recipe.secondarySpeciesId
                            val hasSecondaryInVault = ownedVaultIds.contains(recipe.secondarySpeciesId)

                            val canFuseWithActive = (hasPrimaryInActive && hasSecondaryInVault) || (hasSecondaryInActive && hasPrimaryInVault)

                            // Check if player has two distinct vault pets matching the pair
                            val canFuseVaultWithVault = run {
                                val primPet = vaultPets.firstOrNull { it.speciesId == recipe.primarySpeciesId }
                                val secPet = vaultPets.firstOrNull { it.speciesId == recipe.secondarySpeciesId && it.id != primPet?.id }
                                primPet != null && secPet != null
                            }

                            val canExecute = canFuseWithActive || canFuseVaultWithVault

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (canExecute) Color(0xFF191226) else DigiWearPalette.CardSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    if (canExecute) 1.5.dp else 1.dp,
                                    if (canExecute) Color(recipe.fusionType.badgeColorHex) else DigiWearPalette.CardBorder
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            DigiVectorIcon(type = DigiIconType.DNA_HELIX, size = 11.dp, tint = Color(recipe.fusionType.badgeColorHex))
                                            Text(
                                                text = recipe.fusionType.displayName.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(recipe.fusionType.badgeColorHex),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        if (canExecute) {
                                            Text(
                                                text = "CAN FUSE NOW!",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF00E676),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Visualizer Row: Primary + Secondary = Target
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Component 1
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            SpriteCanvasRenderer(speciesId = primarySp.id, frame = SpriteFrame.IDLE_A, size = 36.dp)
                                            Text(primarySp.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(
                                                text = when {
                                                    hasPrimaryInActive -> "Active"
                                                    hasPrimaryInVault -> "In Vault"
                                                    else -> "Missing"
                                                },
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (hasPrimaryInActive || hasPrimaryInVault) Color(0xFF80CBC4) else Color(0xFFEF5350)
                                            )
                                        }

                                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.Gray)

                                        // Component 2
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            SpriteCanvasRenderer(speciesId = secondarySp.id, frame = SpriteFrame.IDLE_A, size = 36.dp)
                                            Text(secondarySp.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(
                                                text = when {
                                                    hasSecondaryInActive -> "Active"
                                                    hasSecondaryInVault -> "In Vault"
                                                    else -> "Missing"
                                                },
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (hasSecondaryInActive || hasSecondaryInVault) Color(0xFF80CBC4) else Color(0xFFEF5350)
                                            )
                                        }

                                        DigiVectorIcon(type = DigiIconType.ARROW_RIGHT, size = 16.dp, tint = DigiWearPalette.CrestGold)

                                        // Target
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            SpriteCanvasRenderer(speciesId = targetSp.id, frame = SpriteFrame.IDLE_A, size = 42.dp)
                                            Text(targetSp.name, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF80D8FF))
                                            Text(targetSp.stage.displayName, fontSize = 8.sp, color = Color.Gray)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = recipe.description,
                                        fontSize = 9.5.sp,
                                        color = Color.LightGray
                                    )

                                    if (canExecute) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                if (canFuseWithActive && activePetForDeposit != null) {
                                                    val vaultPartnerSpId = if (activePetForDeposit!!.speciesId == recipe.primarySpeciesId)
                                                        recipe.secondarySpeciesId else recipe.primarySpeciesId
                                                    val vaultPet = vaultPets.first { it.speciesId == vaultPartnerSpId }
                                                    pendingFusionExecution = FusionExecutionCandidate.ActiveWithVault(
                                                        activeState = activePetForDeposit!!,
                                                        vaultPet = vaultPet,
                                                        recipe = recipe
                                                    )
                                                } else if (canFuseVaultWithVault) {
                                                    val petA = vaultPets.first { it.speciesId == recipe.primarySpeciesId }
                                                    val petB = vaultPets.first { it.speciesId == recipe.secondarySpeciesId && it.id != petA.id }
                                                    pendingFusionExecution = FusionExecutionCandidate.VaultWithVault(
                                                        vaultPetA = petA,
                                                        vaultPetB = petB,
                                                        recipe = recipe
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(32.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(recipe.fusionType.badgeColorHex))
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                DigiVectorIcon(type = DigiIconType.DNA_HELIX, size = 11.dp, tint = Color.Black)
                                                Text("AWAKEN ${targetSp.name.uppercase()}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // =========================================================================
                // SUB-TAB 3: MEMORIAL GRAVEYARD VIEW
                // =========================================================================
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HALL OF FAME // MEMORIAL GRAVEYARD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80CBC4),
                        fontFamily = FontFamily.Monospace
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                val records = wearableManager.requestMemorialsFromWatch()
                                if (!records.isNullOrEmpty()) {
                                    for (r in records) {
                                        db.phoneDao().insertMemorial(CompanionMemorialEntity.fromDomain(r))
                                    }
                                    actionStatusMessage = "Synced ${records.size} memorial record(s) from Smartwatch!"
                                } else {
                                    actionStatusMessage = "No new memorial records on Smartwatch (or watch offline)"
                                }
                            }
                        },
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("SYNC FROM WATCH", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                if (memorials.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DigiVectorIcon(type = DigiIconType.MEMORIAL_GRAVE, size = 36.dp, tint = Color(0xFF78909C))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "NO DEPARTED PARTNERS YET",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Departed partners that complete their canonical lifecycle\nwill be respectfully commemorated in this Hall of Fame.",
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(memorials, key = { it.id }) { m ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B24)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0x5580CBC4))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color(0xFF0A1016), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0x3380CBC4), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SpriteCanvasRenderer(
                                            speciesId = m.speciesId,
                                            frame = SpriteFrame.IDLE_A,
                                            size = 36.dp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${m.speciesName} • GEN ${m.generation}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                DigiVectorIcon(type = DigiIconType.MEMORIAL_GRAVE, size = 10.dp, tint = Color(0xFFFF8A80))
                                                Text(
                                                    text = m.causeOfDeath,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFFFF8A80)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${m.finalStage} • Age: ${m.ageDays}d • Record: ${m.battlesWon}/${m.battlesTotal} (${(m.winRatio * 100).toInt()}%)",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFFB0BEC5),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "\"${m.epitaph}\"",
                                            fontSize = 10.sp,
                                            color = Color(0xFF80CBC4),
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
    }

    // Deposit Active Partner Dialog
    if (showDepositDialog && activePetForDeposit != null) {
        val p = activePetForDeposit!!
        val spec = PetSpecies.getById(p.speciesId)
        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            containerColor = DigiWearPalette.CardSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.border(BorderStroke(1.dp, DigiWearPalette.CardBorder), RoundedCornerShape(12.dp)),
            title = { Text("Deposit Active Partner", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Store ${spec.name} (${spec.stage.displayName}) into cryo-stasis vault?", fontFamily = FontFamily.Monospace, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { nicknameInput = it },
                        label = { Text("Custom Nickname (Optional)", fontFamily = FontFamily.Monospace) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            db.phoneDao().insertVaultPet(
                                StoredDigimonEntity(
                                    speciesId = p.speciesId,
                                    speciesName = spec.name,
                                    customNickname = nicknameInput.trim(),
                                    stageName = spec.stage.displayName,
                                    attributeName = spec.attribute.displayName,
                                    currentHp = p.currentHp,
                                    maxHp = p.maxHp,
                                    hunger = p.hunger,
                                    strength = p.strength,
                                    weight = p.weight,
                                    ageSeconds = p.ageSeconds,
                                    careMistakes = p.careMistakes,
                                    effort = p.effort,
                                    overfeedCount = p.overfeedCount,
                                    totalBattles = p.totalBattles,
                                    victories = p.victories
                                )
                            )
                            db.phoneDao().clearActivePet()
                            wearableManager.sendRemoteCareCommand("DEPOSIT_VAULT")
                            activePetForDeposit = null
                            showDepositDialog = false
                            nicknameInput = ""
                            actionStatusMessage = "${spec.name} safely stored in DigiVault! Active partner cleared."
                        }
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("CONFIRM DEPOSIT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) {
                    Text("CANCEL", color = Color(0xFFB0BEC5), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        )
    }

    // Comm Code & DigiROM Dialog
    commCodeDialogData?.let { (code, digiRom) ->
        AlertDialog(
            onDismissRequest = { commCodeDialogData = null },
            containerColor = DigiWearPalette.CardSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.border(BorderStroke(1.dp, DigiWearPalette.CardBorder), RoundedCornerShape(12.dp)),
            title = { Text("Comm Code & 0nl1ne DigiROM", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = DigiWearPalette.DigiviceCyan) },
            text = {
                Column {
                    Text("128-bit Base32 Comm Code:", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CommCodeCodec.formatWithHyphens(code),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80CBC4),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF14241D), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Digimon 0nl1ne / D-Com / A-Com Packet:", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = digiRom,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF261E14), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            Toast.makeText(context, "Base32 Comm Code copied!", Toast.LENGTH_SHORT).show()
                            commCodeDialogData = null
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.COPY, size = 14.dp, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Base32", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(digiRom))
                            Toast.makeText(context, "DigiROM Packet copied!", Toast.LENGTH_SHORT).show()
                            commCodeDialogData = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB8C00)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.COPY, size = 14.dp, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DigiROM", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { commCodeDialogData = null }) {
                    Text("CLOSE", color = Color(0xFFB0BEC5), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteConfirmPet?.let { petToDelete ->
        AlertDialog(
            onDismissRequest = { deleteConfirmPet = null },
            containerColor = DigiWearPalette.CardSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.border(BorderStroke(1.dp, DigiWearPalette.CardBorder), RoundedCornerShape(12.dp)),
            title = { Text("Release Digimon?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("Are you sure you want to release ${petToDelete.speciesName} from DigiVault? This cannot be undone.", fontFamily = FontFamily.Monospace, color = Color.LightGray, fontSize = 11.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            db.phoneDao().deleteVaultPet(petToDelete.id)
                            deleteConfirmPet = null
                            actionStatusMessage = "${petToDelete.speciesName} released from vault."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("RELEASE", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmPet = null }) {
                    Text("CANCEL", color = Color(0xFFB0BEC5), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        )
    }

    // DNA Digivolution / DigiXros Confirmation Dialog
    pendingFusionExecution?.let { executionCandidate ->
        val recipe = executionCandidate.recipe
        val targetSp = PetSpecies.getById(recipe.targetSpeciesId)
        val (primName, primId, secName, secId) = when (executionCandidate) {
            is FusionExecutionCandidate.ActiveWithVault -> {
                val pSp = PetSpecies.getById(executionCandidate.activeState.speciesId)
                val sSp = PetSpecies.getById(executionCandidate.vaultPet.speciesId)
                Quad(pSp.name, pSp.id, sSp.name, sSp.id)
            }
            is FusionExecutionCandidate.VaultWithVault -> {
                val pSp = PetSpecies.getById(executionCandidate.vaultPetA.speciesId)
                val sSp = PetSpecies.getById(executionCandidate.vaultPetB.speciesId)
                Quad(pSp.name, pSp.id, sSp.name, sSp.id)
            }
        }

        AlertDialog(
            onDismissRequest = { pendingFusionExecution = null },
            containerColor = DigiWearPalette.CardSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.border(BorderStroke(1.dp, DigiWearPalette.CardBorder), RoundedCornerShape(12.dp)),
            title = {
                Text(
                    text = "${recipe.fusionType.displayName} Awakening",
                    fontWeight = FontWeight.Bold,
                    color = Color(recipe.fusionType.badgeColorHex),
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = recipe.description,
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SpriteCanvasRenderer(speciesId = primId, frame = SpriteFrame.IDLE_A, modifier = Modifier.size(46.dp))
                            Text(primName, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB74D))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SpriteCanvasRenderer(speciesId = secId, frame = SpriteFrame.IDLE_A, modifier = Modifier.size(46.dp))
                            Text(secName, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Text("=", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF64B5F6))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedVaultPetSprite(
                                speciesId = targetSp.id,
                                size = 52.dp
                            )
                            Text(targetSp.name, fontSize = 11.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Black)
                            Text(targetSp.stage.displayName, fontSize = 8.sp, color = Color(0xFF80CBC4))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141A24))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("FUSION SYNERGY INHERITANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80D8FF))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Synergy Max HP: ${(targetSp.baseHp * 1.10f).toInt()} (+10% DNA Harmony)", fontSize = 11.sp, color = Color.LightGray)
                            Text("• Digimon 0nl1ne / D-Com: 100% Wire Protocol Compatible", fontSize = 10.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            when (executionCandidate) {
                                is FusionExecutionCandidate.ActiveWithVault -> {
                                    val fusedState = JogressEngine.executeFusion(
                                        primary = executionCandidate.activeState,
                                        secondarySpeciesId = executionCandidate.vaultPet.speciesId,
                                        secondaryVictories = executionCandidate.vaultPet.victories,
                                        secondaryEffort = executionCandidate.vaultPet.effort
                                    )
                                    activePetForDeposit = fusedState
                                    wearableManager.sendPetStateToWatch(fusedState)
                                    db.phoneDao().insertSnapshot(SyncedPetEntity.fromDomain(fusedState))
                                    db.phoneDao().deleteVaultPet(executionCandidate.vaultPet.id)
                                    DigiWearAppWidgetProvider.updateAllWidgets(context)
                                }
                                is FusionExecutionCandidate.VaultWithVault -> {
                                    val primaryState = PetState(
                                        speciesId = executionCandidate.vaultPetA.speciesId,
                                        currentHp = executionCandidate.vaultPetA.currentHp,
                                        maxHp = executionCandidate.vaultPetA.maxHp,
                                        hunger = executionCandidate.vaultPetA.hunger,
                                        strength = executionCandidate.vaultPetA.strength,
                                        weight = executionCandidate.vaultPetA.weight,
                                        ageSeconds = executionCandidate.vaultPetA.ageSeconds,
                                        careMistakes = executionCandidate.vaultPetA.careMistakes,
                                        effort = executionCandidate.vaultPetA.effort,
                                        overfeedCount = executionCandidate.vaultPetA.overfeedCount,
                                        totalBattles = executionCandidate.vaultPetA.totalBattles,
                                        victories = executionCandidate.vaultPetA.victories
                                    )
                                    val fusedState = JogressEngine.executeFusion(
                                        primary = primaryState,
                                        secondarySpeciesId = executionCandidate.vaultPetB.speciesId,
                                        secondaryVictories = executionCandidate.vaultPetB.victories,
                                        secondaryEffort = executionCandidate.vaultPetB.effort
                                    )
                                    // Insert resulting fused monster into Vault
                                    db.phoneDao().insertVaultPet(
                                        StoredDigimonEntity(
                                            speciesId = fusedState.speciesId,
                                            speciesName = targetSp.name,
                                            customNickname = "Awakened ${targetSp.name}",
                                            stageName = targetSp.stage.displayName,
                                            attributeName = targetSp.attribute.displayName,
                                            currentHp = fusedState.currentHp,
                                            maxHp = fusedState.maxHp,
                                            hunger = fusedState.hunger,
                                            strength = fusedState.strength,
                                            weight = fusedState.weight,
                                            ageSeconds = fusedState.ageSeconds,
                                            careMistakes = fusedState.careMistakes,
                                            effort = fusedState.effort,
                                            overfeedCount = fusedState.overfeedCount,
                                            totalBattles = fusedState.totalBattles,
                                            victories = fusedState.victories
                                        )
                                    )
                                    // Remove both consumed vault partners
                                    db.phoneDao().deleteVaultPet(executionCandidate.vaultPetA.id)
                                    db.phoneDao().deleteVaultPet(executionCandidate.vaultPetB.id)
                                }
                            }

                            pendingFusionExecution = null
                            activeCelebrationSpeciesId = targetSp.id
                            activeCelebrationTitle = "DNA JOGRESS AWAKENING"
                            buzzerManager.playVictoryFanfare()
                            actionStatusMessage = "Awakened ${targetSp.name} via ${recipe.fusionType.displayName}!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("AWAKEN ${targetSp.name.uppercase()}!", fontWeight = FontWeight.Black, color = Color.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFusionExecution = null }) {
                    Text("CANCEL", color = Color(0xFFB0BEC5), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        )
    }

    activeCelebrationSpeciesId?.let { celebrateId ->
        PhoneCutInOverlay(
            speciesId = celebrateId,
            title = activeCelebrationTitle ?: "DNA JOGRESS AWAKENING",
            autoDismissMs = 2200L,
            onDismiss = {
                activeCelebrationSpeciesId = null
                activeCelebrationTitle = null
            }
        )
    }
}

private sealed interface FusionExecutionCandidate {
    val recipe: JogressRecipe
    data class ActiveWithVault(
        val activeState: PetState,
        val vaultPet: StoredDigimonEntity,
        override val recipe: JogressRecipe
    ) : FusionExecutionCandidate

    data class VaultWithVault(
        val vaultPetA: StoredDigimonEntity,
        val vaultPetB: StoredDigimonEntity,
        override val recipe: JogressRecipe
    ) : FusionExecutionCandidate
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun AnimatedVaultPetSprite(
    speciesId: Int,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    var frameToggle by remember { mutableStateOf(false) }
    LaunchedEffect(speciesId) {
        while (true) {
            delay(500L)
            frameToggle = !frameToggle
        }
    }
    SpriteCanvasRenderer(
        speciesId = speciesId,
        frame = if (frameToggle) SpriteFrame.IDLE_A else SpriteFrame.IDLE_B,
        size = size,
        modifier = modifier
    )
}

@Composable
private fun VaultPetCard(
    pet: StoredDigimonEntity,
    activePet: PetState?,
    onDeployToWatch: () -> Unit,
    onGenerateCommCode: () -> Unit,
    onDelete: () -> Unit,
    onFuseWithActive: (StoredDigimonEntity, JogressRecipe) -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        .format(Date(pet.storedTimestampUtc * 1000L))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recessed LCD Cryo-Viewport
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color(0xFF16241E), RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0x6680CBC4), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedVaultPetSprite(
                        speciesId = pet.speciesId,
                        size = 54.dp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Metadata Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (pet.customNickname.isNotEmpty()) "${pet.customNickname} (#${pet.speciesId} ${pet.speciesName})" else "#${pet.speciesId} ${pet.speciesName}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StageBadge(pet.stageName)
                        AttributeBadge(pet.attributeName)
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("H:", fontSize = 10.sp, color = Color(0xFFFF8A80), fontFamily = FontFamily.Monospace)
                            VPetMeterBar(count = pet.hunger, max = 4, isHearts = true, iconSize = 8.dp, activeColor = Color(0xFFFF8A80))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("S:", fontSize = 10.sp, color = Color(0xFFFFD54F), fontFamily = FontFamily.Monospace)
                            VPetMeterBar(count = pet.strength, max = 4, isHearts = false, iconSize = 8.dp, activeColor = Color(0xFFFFD54F))
                        }
                        Text(
                            text = "EFF:${pet.effort}/4",
                            fontSize = 10.sp,
                            color = Color(0xFF80CBC4),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Wt: ${pet.weight}g • Battles: ${pet.victories}W/${pet.totalBattles} • Stored: $dateStr",
                        fontSize = 9.sp,
                        color = Color(0xFF90A4AE),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            val fusionRecipe = activePet?.let {
                JogressEngine.findFusionRecipe(it.speciesId, pet.speciesId)
            }

            if (fusionRecipe != null) {
                val targetSp = PetSpecies.getById(fusionRecipe.targetSpeciesId)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF152230), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(fusionRecipe.fusionType.badgeColorHex), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SpriteCanvasRenderer(
                            speciesId = targetSp.id,
                            frame = SpriteFrame.IDLE_A,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                DigiVectorIcon(type = DigiIconType.DNA_HELIX, size = 10.dp, tint = Color(fusionRecipe.fusionType.badgeColorHex))
                                Text(
                                    text = "${fusionRecipe.fusionType.displayName.uppercase()} READY!",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(fusionRecipe.fusionType.badgeColorHex),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Fuses into ${targetSp.name} (${targetSp.stage.displayName})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Button(
                        onClick = { onFuseWithActive(pet, fusionRecipe) },
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(fusionRecipe.fusionType.badgeColorHex))
                    ) {
                        Text("DNA FUSE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                ) {
                    DigiVectorIcon(type = DigiIconType.CLOSE, size = 14.dp, tint = Color(0xFFEF5350))
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onGenerateCommCode,
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DigiWearPalette.CrestGold)
                ) {
                    DigiVectorIcon(type = DigiIconType.BOLT, size = 14.dp, tint = DigiWearPalette.CrestGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("D-COM CODE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onDeployToWatch,
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                ) {
                    DigiVectorIcon(type = DigiIconType.BEAM_ANTENNA, size = 14.dp, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DEPLOY TO WATCH", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun FilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) Color(0xFF00897B) else DigiWearPalette.CardSurface,
                RoundedCornerShape(8.dp)
            )
            .border(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) Color(0xFF64FFDA) else DigiWearPalette.CardBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.LightGray,
            fontFamily = FontFamily.Monospace
        )
    }
}
