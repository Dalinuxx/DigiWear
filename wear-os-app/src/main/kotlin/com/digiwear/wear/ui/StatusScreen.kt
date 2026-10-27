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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import com.digiwear.core.engine.EvolutionEngine
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import com.digiwear.core.model.SpriteFrame
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.input.HardwareInputManager
import com.digiwear.wear.input.VPetButton
import com.digiwear.wear.render.SpriteCanvasRenderer

@Composable
fun StatusScreen(
    repository: PetRepository,
    onNavigateDigiDex: () -> Unit = {},
    onBack: () -> Unit
) {
    val petState by repository.petStateFlow.collectAsState(initial = PetState())
    val backupPet by repository.backupPetStateFlow.collectAsState(initial = null)
    val memorials by repository.memorialsFlow.collectAsState(initial = emptyList())
    val discoveredIds by repository.discoveredSpeciesFlow.collectAsState(initial = emptySet())
    val maskSpoilers by repository.maskUndiscoveredSpeciesFlow.collectAsState(initial = true)
    val displayConfig = WatchDisplayConfig.current()
    val species = remember(petState.speciesId) { PetSpecies.getById(petState.speciesId) }
    val nextEvo = remember(petState) { EvolutionEngine.evaluateEvolution(petState) }
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()
    var fusionFeedback by remember { mutableStateOf<String?>(null) }
    var showCutInPortrait by remember { mutableStateOf(false) }
    var showMemorials by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = showCutInPortrait || showMemorials) {
        if (showCutInPortrait) showCutInPortrait = false
        else if (showMemorials) showMemorials = false
    }

    LaunchedEffect(Unit) {
        HardwareInputManager.buttonEvents.collect { button ->
            if (button == VPetButton.C || button == VPetButton.B) {
                when {
                    showCutInPortrait -> showCutInPortrait = false
                    showMemorials -> showMemorials = false
                    else -> onBack()
                }
            }
        }
    }

    val ageHours = petState.ageSeconds / 3600
    val ageDays = ageHours / 24
    val remHours = ageHours % 24

    val winPct = if (petState.totalBattles > 0) {
        (petState.victories * 100) / petState.totalBattles
    } else 0

    val attrColor = when (species.attribute.displayName) {
        "Vaccine" -> Color(0xFF00E5FF)
        "Data" -> Color(0xFFFFCA28)
        "Virus" -> Color(0xFFFF5252)
        else -> Color(0xFF69F0AE)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D131A)),
            state = listState,
            contentPadding = displayConfig.contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "V-PET ANALYZER",
                    fontSize = displayConfig.headerFontSize,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E676),
                    fontFamily = FontFamily.Monospace
                )
            }

            if (petState.isDeceased) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = displayConfig.horizontalPadding, vertical = 3.dp)
                            .background(Color(0xFF381216), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.TOMBSTONE, size = 10.dp, tint = Color(0xFFFF5252))
                            Text(
                                text = "REST IN PEACE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFF5252),
                                fontFamily = FontFamily.Monospace
                            )
                            DigiVectorIcon(type = DigiIconType.TOMBSTONE, size = 10.dp, tint = Color(0xFFFF5252))
                        }
                        Text(
                            text = "Partner of Generation ${petState.generation} has completed its digital journey. A Traited Digitama awaits!",
                            fontSize = 6.5.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.hatchTraitedEgg()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00C853)),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                DigiVectorIcon(type = DigiIconType.EGG, size = 10.dp, tint = Color.White)
                                Text(
                                    text = "HATCH TRAITED EGG (GEN ${petState.generation + 1})",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Framed Partner Header Card with Static LCD Preview
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                        .background(Color(0xFF16202B), RoundedCornerShape(8.dp))
                        .border(0.75.dp, Color(0x5580CBC4), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF15241E), RoundedCornerShape(6.dp))
                            .border(0.75.dp, Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                            .clickable { showCutInPortrait = true },
                        contentAlignment = Alignment.Center
                    ) {
                        SpriteCanvasRenderer(
                            speciesId = petState.speciesId,
                            isAmbient = false,
                            overrideFrame = SpriteFrame.IDLE_A,
                            size = 38.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = species.name.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0x3380CBC4), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = species.stage.displayName.uppercase(),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF80CBC4),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF263238), RoundedCornerShape(3.dp))
                                    .border(0.5.dp, Color(0xFFFFD54F), RoundedCornerShape(3.dp))
                                    .clickable { showCutInPortrait = true }
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                DigiVectorIcon(type = DigiIconType.BOLT, size = 8.dp, tint = Color(0xFFFFD54F))
                                Text(
                                    text = "CUT-IN",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD54F),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(attrColor.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = species.attribute.displayName.uppercase(),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = attrColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Biological Gauges Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                        .background(Color(0xFF141C24), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    StatusMeterRow("HUNGER", petState.hunger, isHearts = true, Color(0xFFFF5252))
                    StatusMeterRow("STRENGTH", petState.strength, isHearts = false, Color(0xFFFFD700))
                    StatusMeterRow("EFFORT", petState.effort, isHearts = false, Color(0xFFCE93D8))
                    StatusRow("HP / POWER", "${petState.currentHp}/${petState.maxHp} - ${species.basePower}P", Color(0xFF80D8FF))
                }
            }

            // Vitals & Care History Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                        .background(Color(0xFF141C24), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    StatusRow("GENERATION", "Gen ${petState.generation} (+${petState.traitedEggBonus} Trait)", Color(0xFFFFD54F))
                    StatusRow("AGE / WT", "${ageDays}d ${remHours}h - ${petState.weight}g", Color.White)
                    StatusRow("MISTAKES", "${petState.careMistakes} Care / ${petState.overfeedCount} Overfeed", Color(0xFFFFCC80))
                    StatusRow("RECORD", "${petState.victories}W / ${petState.totalBattles}B ($winPct%)", Color(0xFF69F0AE))
                    StatusRow(
                        "NEXT EVO",
                        nextEvo?.name?.uppercase() ?: if (species.evolutionRules.isEmpty()) "APEX STAGE" else "TRAINING...",
                        if (nextEvo != null) Color(0xFF00E676) else Color.Gray
                    )
                }
            }

            // Evolution Targets & Lineage Guide Card
            item {
                val lineagePath = remember(species.id) { EvolutionEngine.findEggLineagePath(species.id).map { PetSpecies.getById(it) } }
                val jogressRecipes = remember(species.id) { com.digiwear.core.fusion.JogressEngine.getCompatibleRecipesFor(species.id) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                        .background(Color(0xFF161D28), RoundedCornerShape(8.dp))
                        .border(0.5.dp, Color(0x44FFD54F), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "EVO GUIDE & LINEAGE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace
                    )
                    if (lineagePath.isNotEmpty()) {
                        Text(
                            text = lineagePath.joinToString(" -> ") { if (!maskSpoilers || discoveredIds.contains(it.id)) it.name else "???" },
                            fontSize = 7.sp,
                            color = Color(0xFF80CBC4),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    if (species.evolutionRules.isEmpty()) {
                        Text(
                            text = "Apex Stage Reached!",
                            fontSize = 7.sp,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        species.evolutionRules.forEach { rule ->
                            val target = PetSpecies.getById(rule.targetSpeciesId)
                            val isReady = nextEvo?.id == target.id
                            val isRevealed = !maskSpoilers || discoveredIds.contains(target.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.5.dp)
                                    .background(
                                        if (isReady) Color(0xFF142E24) else Color(0xFF0F151D),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpriteCanvasRenderer(
                                    speciesId = target.id,
                                    isAmbient = false,
                                    overrideFrame = SpriteFrame.IDLE_A,
                                    isSilhouette = !isRevealed,
                                    size = 20.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "-> ${if (isRevealed) target.name else "???"} ${if (isReady) "[READY]" else ""}",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isReady) Color(0xFF00E676) else Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = EvolutionEngine.describeRule(rule),
                                        fontSize = 6.5.sp,
                                        color = Color(0xFFB0BEC5),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                    if (jogressRecipes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.BOLT, size = 9.dp, tint = Color(0xFFEA80FC))
                            Text(
                                text = "DNA JOGRESS / DIGIXROS READY (${jogressRecipes.size})",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEA80FC),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        fusionFeedback?.let { msg ->
                            Text(
                                text = msg,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        jogressRecipes.forEach { recipe ->
                            val partnerId = if (recipe.primarySpeciesId == species.id) recipe.secondarySpeciesId else recipe.primarySpeciesId
                            val partner = PetSpecies.getById(partnerId)
                            val target = PetSpecies.getById(recipe.targetSpeciesId)
                            val partnerRevealed = !maskSpoilers || discoveredIds.contains(partner.id)
                            val targetRevealed = !maskSpoilers || discoveredIds.contains(target.id)
                            val partnerName = if (partnerRevealed) partner.name else "???"
                            val targetName = if (targetRevealed) target.name else "???"
                            val isSelfModeChange = recipe.primarySpeciesId == recipe.secondarySpeciesId
                            val hasBackupPartner = backupPet?.speciesId == partner.id
                            val canExecuteNow = if (isSelfModeChange) {
                                petState.effort >= 4 && petState.victories >= 15
                            } else {
                                hasBackupPartner && com.digiwear.core.fusion.JogressEngine.findFusionRecipe(
                                    petState.speciesId,
                                    backupPet!!.speciesId
                                ) != null
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .background(Color(0xFF1F1630), RoundedCornerShape(6.dp))
                                    .border(0.5.dp, Color(recipe.fusionType.badgeColorHex), RoundedCornerShape(6.dp))
                                    .padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        SpriteCanvasRenderer(
                                            speciesId = partner.id,
                                            isAmbient = false,
                                            overrideFrame = SpriteFrame.IDLE_A,
                                            isSilhouette = !partnerRevealed,
                                            size = 18.dp
                                        )
                                        Text(" -> ", fontSize = 7.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Black)
                                        SpriteCanvasRenderer(
                                            speciesId = target.id,
                                            isAmbient = false,
                                            overrideFrame = SpriteFrame.IDLE_A,
                                            isSilhouette = !targetRevealed,
                                            size = 20.dp
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Column {
                                            Text(
                                                text = if (isSelfModeChange) "MODE: ${targetName.take(10)}" else "+${partnerName.take(7)} -> ${targetName.take(8)}",
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
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                if (isSelfModeChange && canExecuteNow) {
                                                    val fused = com.digiwear.core.fusion.JogressEngine.executeFusion(
                                                        primary = petState,
                                                        secondarySpeciesId = partner.id,
                                                        secondaryVictories = petState.victories,
                                                        secondaryEffort = petState.effort
                                                    )
                                                    repository.savePetState(fused)
                                                    fusionFeedback = "Awakened ${target.name}!"
                                                } else if (hasBackupPartner) {
                                                    val fused = repository.performDirectJogress()
                                                    fusionFeedback = if (fused != null) {
                                                        "Awakened ${target.name}!"
                                                    } else {
                                                        "Needs 4 Effort or 15 Wins!"
                                                    }
                                                } else {
                                                    fusionFeedback = if (isSelfModeChange) {
                                                        "Needs 4 Effort & 15 Wins!"
                                                    } else {
                                                        "Need ${partner.name} in Cryo!"
                                                    }
                                                }
                                            }
                                        },
                                        enabled = canExecuteNow,
                                        modifier = Modifier.height(22.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = if (canExecuteNow) Color(recipe.fusionType.badgeColorHex) else Color(0xFF37474F)
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            DigiVectorIcon(type = DigiIconType.BOLT, size = 8.dp, tint = if (canExecuteNow) Color.Black else Color.LightGray)
                                            Text(
                                                text = if (canExecuteNow) "FUSE" else "LOCKED",
                                                fontSize = 6.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (canExecuteNow) Color.Black else Color.LightGray,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Show upcoming Jogress/DigiXros in immediate evolution branch
                        val nextBranchJogress = remember(species.id) {
                            species.evolutionRules.flatMap { r ->
                                com.digiwear.core.fusion.JogressEngine.getCompatibleRecipesFor(r.targetSpeciesId)
                                    .map { recipe -> PetSpecies.getById(r.targetSpeciesId) to recipe }
                            }.take(2)
                        }
                        if (nextBranchJogress.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                DigiVectorIcon(type = DigiIconType.BOLT, size = 8.dp, tint = Color(0xFFEA80FC))
                                Text(
                                    text = "NEXT STAGE JOGRESS / XROS UNLOCKS:",
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEA80FC),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            nextBranchJogress.forEach { (branchSp, recipe) ->
                                val targetSp = PetSpecies.getById(recipe.targetSpeciesId)
                                Text(
                                    text = "- Evolve to ${branchSp.name.take(8)} -> ${targetSp.name.take(10)} (${recipe.fusionType.displayName.take(8)})",
                                    fontSize = 6.sp,
                                    color = Color(0xFFCE93D8),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(3.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding)
                        .background(Color(0xFF131924), RoundedCornerShape(8.dp))
                        .border(0.5.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CRYO-STASIS (SLOT 2)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (backupPet != null) "OCCUPIED" else "EMPTY",
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (backupPet != null) Color(0xFF00E676) else Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    if (backupPet == null) {
                        Text(
                            text = "Store secondary partner in cryo-stasis to preserve state or perform on-watch Jogress.",
                            fontSize = 6.5.sp,
                            color = Color(0xFFB0BEC5),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.depositActiveToBackup()
                                    fusionFeedback = "Partner stored in Slot 2 stasis!"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E293B))
                        ) {
                            Text("DEPOSIT TO SLOT 2", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        val backupSp = remember(backupPet!!.speciesId) { PetSpecies.getById(backupPet!!.speciesId) }
                        val canJogress = remember(petState.speciesId, backupPet!!.speciesId) {
                            com.digiwear.core.fusion.JogressEngine.findFusionRecipe(petState.speciesId, backupPet!!.speciesId) != null
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpriteCanvasRenderer(
                                speciesId = backupSp.id,
                                isAmbient = false,
                                overrideFrame = SpriteFrame.IDLE_A,
                                size = 22.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = backupSp.name,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${backupSp.stage.displayName} - HP ${backupPet!!.currentHp}",
                                    fontSize = 6.5.sp,
                                    color = Color(0xFF90A4AE),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        repository.swapActiveAndBackup()
                                        fusionFeedback = "Swapped to Slot 2 partner!"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(22.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00E5FF))
                            ) {
                                Text("SWAP", fontSize = 6.5.sp, fontWeight = FontWeight.Black, color = Color.Black, fontFamily = FontFamily.Monospace)
                            }

                            if (canJogress) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val evolved = repository.performDirectJogress()
                                            if (evolved != null) {
                                                fusionFeedback = "Jogress Awakened ${PetSpecies.getById(evolved.speciesId).name}!"
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(22.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEA80FC))
                                ) {
                                    Text("JOGRESS", fontSize = 6.5.sp, fontWeight = FontWeight.Black, color = Color.Black, fontFamily = FontFamily.Monospace)
                                }
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        repository.saveBackupPetState(null)
                                        fusionFeedback = "Released backup partner."
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(22.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F))
                            ) {
                                Text("CLEAR", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // DigiDex Field Guide Button
            item {
                Spacer(modifier = Modifier.height(3.dp))
                Button(
                    onClick = onNavigateDigiDex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = displayConfig.horizontalPadding),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF0A2B35))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.SCAN, size = 10.dp, tint = Color(0xFF00E5FF))
                        Text(
                            text = "DIGIDEX FIELD GUIDE (550)",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(3.dp))
                Button(
                    onClick = { showMemorials = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = displayConfig.horizontalPadding),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.TOMBSTONE, size = 10.dp, tint = Color(0xFF80D8FF))
                        Text(
                            text = "MEMORIAL GRAVEYARD (${memorials.size})",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF80D8FF)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .padding(horizontal = displayConfig.horizontalPadding),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF263238))
                ) {
                    Text("BACK [C]", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        if (showMemorials) {
            val memListState = rememberScalingLazyListState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF03080E).copy(alpha = 0.96f)),
                contentAlignment = Alignment.Center
            ) {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = memListState,
                    contentPadding = displayConfig.contentPadding,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            text = "DIGI-MEMORIAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Departed Partners - Hall of Fame",
                            fontSize = 7.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (memorials.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                DigiVectorIcon(type = DigiIconType.TOMBSTONE, size = 28.dp, tint = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No Departed Partners Yet",
                                    fontSize = 8.sp,
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Current Gen ${petState.generation} lives on!",
                                    fontSize = 7.sp,
                                    color = Color(0xFF81C784),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        items(memorials, key = { it.id }) { m ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp)
                                    .background(Color(0xFF101922), RoundedCornerShape(6.dp))
                                    .border(0.5.dp, Color(0x5580D8FF), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SpriteCanvasRenderer(
                                            speciesId = m.speciesId,
                                            isAmbient = false,
                                            overrideFrame = SpriteFrame.IDLE_A,
                                            size = 18.dp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${m.speciesName.take(10)} [GEN ${m.generation}]",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    DigiVectorIcon(type = DigiIconType.TOMBSTONE, size = 12.dp, tint = Color(0xFF80D8FF))
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${m.finalStage} - Age: ${m.ageDays}d - ${m.battlesWon}/${m.battlesTotal} Wins (${(m.winRatio * 100).toInt()}%)",
                                    fontSize = 6.5.sp,
                                    color = Color(0xFFB0BEC5),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "\"${m.epitaph}\"",
                                    fontSize = 6.sp,
                                    color = Color(0xFF80CBC4),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { showMemorials = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .padding(horizontal = displayConfig.horizontalPadding),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF263238))
                        ) {
                            Text("BACK [C]", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        if (showCutInPortrait) {
            CutInOverlay(
                speciesId = petState.speciesId,
                title = "OFFICIAL ALBUM CUT-IN",
                autoDismissMs = null,
                onDismiss = { showCutInPortrait = false }
            )
        }
    }
}

@Composable
private fun StatusMeterRow(
    label: String,
    filledCount: Int,
    isHearts: Boolean,
    activeColor: Color,
    horizontalPadding: Dp = 0.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 8.sp, color = Color(0xFF90A4AE), fontFamily = FontFamily.Monospace)
        VPetMeterBar(
            filledCount = filledCount,
            maxCount = 4,
            isHearts = isHearts,
            activeColor = activeColor,
            emptyColor = activeColor.copy(alpha = 0.35f),
            iconSize = 8.5.dp
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, valueColor: Color = Color.White, horizontalPadding: Dp = 0.dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 8.sp, color = Color(0xFF90A4AE), fontFamily = FontFamily.Monospace)
        Text(text = value, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = valueColor, fontFamily = FontFamily.Monospace)
    }
}
