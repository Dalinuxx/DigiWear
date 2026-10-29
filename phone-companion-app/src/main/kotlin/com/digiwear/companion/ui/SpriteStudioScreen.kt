package com.digiwear.companion.ui

import com.digiwear.companion.BuildConfig

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
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
import com.digiwear.core.battle.SignatureAttackRegistry
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.render.SpriteCanvasRenderer
import com.digiwear.companion.widget.DigiWearAppWidgetProvider
import com.digiwear.core.dex.DigiDexEngine
import com.digiwear.core.fusion.JogressEngine
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import com.digiwear.core.model.PetState
import com.digiwear.core.model.SpriteFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FrameMeta(val index: Int, val row: Int, val col: Int, val name: String, val category: String)

val SPRITE_GRID_FRAMES = listOf(
    // Row 0
    FrameMeta(0, 0, 0, "Idle Stance A", "Row 0 (Idle/Walk)"),
    FrameMeta(1, 0, 1, "Idle Stance B", "Row 0 (Idle/Walk)"),
    FrameMeta(2, 0, 2, "Walking Motion A", "Row 0 (Idle/Walk)"),
    // Row 1
    FrameMeta(3, 1, 0, "Walking Motion B", "Row 1 (Feed/Joy)"),
    FrameMeta(4, 1, 1, "Feeding Start", "Row 1 (Feed/Joy)"),
    FrameMeta(5, 1, 2, "Chewing / Joy", "Row 1 (Feed/Joy)"),
    // Row 2
    FrameMeta(6, 2, 0, "Sleeping Frame A", "Row 2 (Sleep/Refuse)"),
    FrameMeta(7, 2, 1, "Sleeping Frame B", "Row 2 (Sleep/Refuse)"),
    FrameMeta(8, 2, 2, "Refusal / Displeasure", "Row 2 (Sleep/Refuse)"),
    // Row 3
    FrameMeta(9, 3, 0, "Attack Charge", "Row 3 (Combat/Status)"),
    FrameMeta(10, 3, 1, "Attack Release", "Row 3 (Combat/Status)"),
    FrameMeta(11, 3, 2, "Injury / Illness", "Row 3 (Combat/Status)")
)

private val PREVIEW_MODES = listOf("IDLE", "WALK", "FEED", "SLEEP", "ATTACK", "STATUS")

@Composable
fun SpriteStudioScreen(
    db: PhoneDatabase,
    wearableManager: PhoneWearableManager,
    showHeader: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    var selectedSpeciesId by remember { mutableIntStateOf(201) } // Agumon
    var selectedFrameIndex by remember { mutableIntStateOf(0) }
    var previewMode by remember { mutableStateOf("IDLE") } // IDLE, WALK, FEED, SLEEP, ATTACK, STATUS
    var searchQuery by remember { mutableStateOf("") }
    var selectedStageFilter by remember { mutableStateOf<PetStage?>(null) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var showCutInPortrait by remember { mutableStateOf(false) }
    var discoveryFilterMode by remember { mutableStateOf("ALL") } // ALL, DISCOVERED, UNDISCOVERED
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        com.digiwear.companion.render.PixelArtMatrices.loadCustomMatrices(context)
    }

    val discoveredEntities by db.phoneDao().getAllDiscoveredFlow().collectAsState(initial = emptyList())
    val discoveredMap = remember(discoveredEntities) { discoveredEntities.associateBy { it.speciesId } }
    val summary = remember(discoveredMap) { DigiDexEngine.calculateSummary(discoveredMap.keys) }
    val maskSpoilers = remember { wearableManager.getMaskUndiscoveredSpecies() }

    val selectedSpecies = remember(selectedSpeciesId) { PetSpecies.getById(selectedSpeciesId) }
    val isCurrentDiscovered = discoveredMap.containsKey(selectedSpeciesId)
    val isCurrentRevealed = !maskSpoilers || isCurrentDiscovered

    val filteredRoster = remember(searchQuery, selectedStageFilter, discoveryFilterMode, discoveredMap, maskSpoilers) {
        PetSpecies.SORTED_SPECIES.filter { sp ->
            val isDiscovered = discoveredMap.containsKey(sp.id)
            val matchesDiscovery = when (discoveryFilterMode) {
                "DISCOVERED" -> isDiscovered
                "UNDISCOVERED" -> !isDiscovered
                else -> true
            }
            // While masked, only match names of revealed species so searching can't leak hidden names.
            val nameSearchable = !maskSpoilers || isDiscovered
            val matchesSearch = (nameSearchable && sp.name.contains(searchQuery, ignoreCase = true)) ||
                    sp.id.toString().contains(searchQuery.trim())
            val matchesStage = selectedStageFilter == null || sp.stage == selectedStageFilter
            matchesDiscovery && matchesSearch && matchesStage
        }
    }

    val preEvolutions = remember(selectedSpeciesId) {
        com.digiwear.core.engine.EvolutionEngine.getPreEvolutions(selectedSpeciesId)
    }

    val relatedFusions = remember(selectedSpeciesId) {
        JogressEngine.getRecipesForSpecies(selectedSpeciesId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DigiWearPalette.CyberBg)
                .padding(contentPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showHeader) {
                DigiSectionHeader(
                    tag = if (BuildConfig.DEBUG) "DIGIDEX // FIELD GUIDE & SPRITE STUDIO [DEBUG]" else "DIGIDEX // ENCYCLOPEDIC COMPENDIUM",
                    title = "${PetSpecies.SPECIES_REGISTRY.size}-Species Canonical DigiDex",
                    subtitle = if (BuildConfig.DEBUG) "Discovery Progress, Biometrics, Lineage Trees & Debug Sprite Sheet Matrix" else "Discovery Progress, Biometrics, Lineage Trees & Reference Book Lore",
                    accentColor = DigiWearPalette.CrestGold
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 550-Species DigiDex Discovery Progress Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1724)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DigiVectorIcon(type = DigiIconType.RADAR_SCAN, size = 14.dp, tint = Color(0xFF00E5FF))
                            Text(
                                text = "DIGIDEX DISCOVERY PROGRESS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "${summary.completionPercentage.toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (summary.completionPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF1E2B3D)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${summary.discoveredCount} / ${summary.totalSpecies} Discovered",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${summary.totalSpecies - summary.discoveredCount} Remaining",
                            fontSize = 9.sp,
                            color = Color(0xFF90A4AE),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // Discovery Filter Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "All (${summary.totalSpecies})",
                            "DISCOVERED" to "Discovered (${summary.discoveredCount})",
                            "UNDISCOVERED" to "Undiscovered (${summary.totalSpecies - summary.discoveredCount})"
                        ).forEach { (mode, label) ->
                            val active = discoveryFilterMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (active) Color(0xFF00E5FF) else Color(0xFF16202E),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { discoveryFilterMode = mode }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.Black else Color(0xFFB0BEC5),
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search ${PetSpecies.SPECIES_REGISTRY.size} Digimon by name or ID...", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = { DigiVectorIcon(type = DigiIconType.SEARCH_LENS, tint = Color.Gray, size = 16.dp) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stage Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StageFilterChip("All", selectedStageFilter == null) {
                    selectedStageFilter = null
                }
                PetStage.entries.forEach { stage ->
                    StageFilterChip(stage.displayName, selectedStageFilter == stage) {
                        selectedStageFilter = if (selectedStageFilter == stage) null else stage
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filtered Species Selector Row with Miniature Sprites (Virtualized LazyRow for 550 species)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRoster, key = { it.id }) { sp ->
                    val selected = selectedSpeciesId == sp.id
                    val isDiscovered = discoveredMap.containsKey(sp.id)
                    val isRevealed = !maskSpoilers || isDiscovered
                    Row(
                        modifier = Modifier
                            .background(
                                when {
                                    selected -> Color(0xFF2E220D)
                                    isRevealed -> DigiWearPalette.CardSurface
                                    else -> Color(0xFF0B1017)
                                },
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                when {
                                    selected -> DigiWearPalette.CrestGold
                                    isRevealed -> DigiWearPalette.CardBorder
                                    else -> Color(0xFF1E2836)
                                },
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedSpeciesId = sp.id }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpriteCanvasRenderer(
                            speciesId = sp.id,
                            frame = SpriteFrame.IDLE_A,
                            isSilhouette = !isRevealed,
                            size = 18.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRevealed) "#${sp.id} ${sp.name}" else "#${sp.id} ???",
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                selected -> DigiWearPalette.CrestGold
                                isRevealed -> Color.White
                                else -> Color(0xFF546E7A)
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

        Spacer(modifier = Modifier.height(14.dp))

        // Real-time Animation Preview Stage + Species Metadata inside VPetBrickShellFrame
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCurrentRevealed) "#${selectedSpecies.id} ${selectedSpecies.name}" else "#${selectedSpecies.id} ???",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isCurrentRevealed) Color.White else Color(0xFFFFD54F)
                        )
                        Text(
                            text = if (isCurrentRevealed) {
                                "HP ${selectedSpecies.baseHp} • PWR ${selectedSpecies.basePower} • WT ${selectedSpecies.baselineWeight}G"
                            } else {
                                "UNDISCOVERED SPECIES ENTRY"
                            },
                            fontSize = 10.sp,
                            color = Color(0xFF80CBC4),
                            fontFamily = FontFamily.Monospace
                        )
                        if (isCurrentRevealed) {
                            val ent = discoveredMap[selectedSpeciesId]
                            Text(
                                text = "Times Raised: ${ent?.timesRaised ?: 0} • Defeated: ${ent?.timesDefeated ?: 0}",
                                fontSize = 8.5.sp,
                                color = Color(0xFFFFD54F),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StageBadge(selectedSpecies.stage.displayName)
                        AttributeBadge(if (isCurrentRevealed) selectedSpecies.attribute.displayName else "???")
                    }
                }

                if (!isCurrentRevealed) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF131D2A),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = "DISCOVERY HINT:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = DigiDexEngine.getDiscoveryHint(selectedSpeciesId),
                                fontSize = 9.5.sp,
                                color = Color(0xFFCFD8DC),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                val signatureAttack = remember(selectedSpeciesId) { SignatureAttackRegistry.getAttackName(selectedSpeciesId) }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF131D2A),
                    border = BorderStroke(1.dp, Color(0xFF2E4057)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DigiVectorIcon(type = DigiIconType.LIGHTNING_BOLT, size = 10.dp, tint = Color(0xFFFFB74D))
                            Text("SPECIAL MOVE:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D), fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (isCurrentRevealed) signatureAttack else "???",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        val attrAdvantage = if (!isCurrentRevealed) {
                            "???"
                        } else {
                            when (selectedSpecies.attribute.displayName.uppercase()) {
                                "VACCINE" -> "STRONG VS VIRUS"
                                "DATA" -> "STRONG VS VACCINE"
                                "VIRUS" -> "STRONG VS DATA"
                                else -> "NEUTRAL MATCHUP"
                            }
                        }
                        Text(attrAdvantage, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80CBC4), fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                StudioSpritePreviewShell(
                    selectedSpeciesId = selectedSpeciesId,
                    isDiscovered = isCurrentRevealed,
                    previewMode = previewMode,
                    onPreviewModeChange = { previewMode = it },
                    selectedFrameIndex = selectedFrameIndex,
                    onFrameIndexChange = { selectedFrameIndex = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 6-Mode Animation Controls (scrollable row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PREVIEW_MODES.forEach { mode ->
                        val active = previewMode == mode
                        Button(
                            onClick = { previewMode = mode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) Color(0xFF00897B) else Color(0xFF263238)
                            )
                        ) {
                            Text(mode, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { if (isCurrentRevealed) showCutInPortrait = true },
                    enabled = isCurrentRevealed,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E2A38),
                        disabledContainerColor = Color(0xFF141B24)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isCurrentRevealed) Color(0xFFFFD54F) else Color(0xFF37474F))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DigiVectorIcon(type = DigiIconType.LIGHTNING_BOLT, size = 12.dp, tint = if (isCurrentRevealed) Color(0xFFFFD54F) else Color.Gray)
                        Text(
                            text = if (isCurrentRevealed) "VIEW OFFICIAL ALBUM CUT-IN PORTRAIT" else "CUT-IN PORTRAIT LOCKED (DISCOVER FIRST)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentRevealed) Color(0xFFFFD54F) else Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Evolution & DNA Jogress Encyclopedia Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "▸ EVOLUTION, STARTER EGG LINEAGE & JOGRESS GUIDE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DigiWearPalette.CrestGold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Starter Egg Lineage Path
                val eggLineage = remember(selectedSpeciesId) {
                    com.digiwear.core.engine.EvolutionEngine.findEggLineagePath(selectedSpeciesId).map { PetSpecies.getById(it) }
                }
                if (eggLineage.isNotEmpty()) {
                    Text(
                        text = "STARTER EGG LINEAGE PATH (CLICK TO JUMP):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        eggLineage.forEachIndexed { idx, stepSp ->
                            val isCurrent = stepSp.id == selectedSpeciesId
                            val stepDiscovered = discoveredMap.containsKey(stepSp.id)
                            val stepRevealed = !maskSpoilers || stepDiscovered
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (isCurrent) Color(0xFF14363D) else DigiWearPalette.CardInner,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isCurrent) Color(0xFF00E5FF) else Color(0xFF263238),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedSpeciesId = stepSp.id }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpriteCanvasRenderer(
                                    speciesId = stepSp.id,
                                    frame = SpriteFrame.IDLE_A,
                                    isSilhouette = !stepRevealed,
                                    size = 22.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                if (idx == 0) {
                                    DigiVectorIcon(type = DigiIconType.EGG, size = 11.dp, tint = Color(0xFFFFD54F))
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                val displayName = if (stepRevealed) stepSp.name else "???"
                                Text(
                                    text = "${if (idx > 0) "-> " else ""}$displayName (${stepSp.stage.displayName})",
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) Color(0xFF80D8FF) else Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Pre-evolutions
                if (preEvolutions.isNotEmpty()) {
                    Text("EVOLVES FROM (${preEvolutions.size} SOURCES):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9), fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        preEvolutions.forEach { (pre, rule) ->
                            val preDiscovered = discoveredMap.containsKey(pre.id)
                            val preRevealed = !maskSpoilers || preDiscovered
                            Row(
                                modifier = Modifier
                                    .background(DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
                                    .clickable { selectedSpeciesId = pre.id }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpriteCanvasRenderer(
                                    speciesId = pre.id,
                                    frame = SpriteFrame.IDLE_A,
                                    isSilhouette = !preRevealed,
                                    size = 24.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        DigiVectorIcon(type = DigiIconType.ARROW_LEFT, size = 10.dp, tint = Color.White)
                                        Text("${if (preRevealed) pre.name else "???"} (${pre.stage.displayName})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Text(
                                        text = com.digiwear.core.engine.EvolutionEngine.describeRule(rule),
                                        fontSize = 8.5.sp,
                                        color = Color(0xFFB0BEC5),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Next evolutions
                Text("EVOLVES INTO:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784), fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                if (selectedSpecies.evolutionRules.isEmpty()) {
                    Text("Apex Stage — No standard evolution branches (check DNA Jogress below).", fontSize = 10.sp, color = Color.Gray)
                } else {
                    selectedSpecies.evolutionRules.forEach { rule ->
                        val target = PetSpecies.getById(rule.targetSpeciesId)
                        val targetDiscovered = discoveredMap.containsKey(target.id)
                        val targetRevealed = !maskSpoilers || targetDiscovered
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF222E3C), RoundedCornerShape(8.dp))
                            .clickable { selectedSpeciesId = target.id }
                            .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpriteCanvasRenderer(
                                    speciesId = target.id,
                                    frame = SpriteFrame.IDLE_A,
                                    isSilhouette = !targetRevealed,
                                    size = 34.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "-> #${target.id} ${if (targetRevealed) target.name else "???"} (${target.stage.displayName} • ${if (targetRevealed) target.attribute.displayName else "???"})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = com.digiwear.core.engine.EvolutionEngine.describeRule(rule),
                                        fontSize = 9.sp,
                                        color = Color(0xFF80CBC4),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("VIEW ", fontSize = 9.sp, color = DigiWearPalette.DigiviceCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                DigiVectorIcon(type = DigiIconType.ARROW_RIGHT, size = 9.dp, tint = DigiWearPalette.DigiviceCyan)
                            }
                        }
                    }
                }

                // DNA Digivolution / DigiXros Recipes
                if (relatedFusions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("DNA DIGIVOLUTION / DIGIXROS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DigiWearPalette.CrestGold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    relatedFusions.forEach { recipe ->
                        val spA = PetSpecies.getById(recipe.primarySpeciesId)
                        val spB = PetSpecies.getById(recipe.secondarySpeciesId)
                        val target = PetSpecies.getById(recipe.targetSpeciesId)
                        val aDisc = discoveredMap.containsKey(spA.id)
                        val bDisc = discoveredMap.containsKey(spB.id)
                        val tDisc = discoveredMap.containsKey(target.id)
                        val aRevealed = !maskSpoilers || aDisc
                        val bRevealed = !maskSpoilers || bDisc
                        val tRevealed = !maskSpoilers || tDisc
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(Color(0xFF152230), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(recipe.fusionType.badgeColorHex), RoundedCornerShape(8.dp))
                                .clickable { selectedSpeciesId = target.id }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    DigiVectorIcon(type = DigiIconType.DNA_HELIX, size = 11.dp, tint = Color(recipe.fusionType.badgeColorHex))
                                    Text(
                                        text = "${if (aRevealed) spA.name else "???"} + ${if (bRevealed) spB.name else "???"} = ${if (tRevealed) target.name else "???"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(recipe.fusionType.badgeColorHex)
                                    )
                                }
                                Text(text = if (tRevealed) recipe.description else "Discover fusion partners to unlock lore.", fontSize = 9.sp, color = Color.LightGray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            SpriteCanvasRenderer(
                                speciesId = target.id,
                                frame = SpriteFrame.IDLE_A,
                                isSilhouette = !tRevealed,
                                size = 36.dp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive 16x16 Custom Pixel Art Matrix Override Card
        CustomSpriteMatrixEditorCard(
            speciesId = selectedSpeciesId,
            frame = SpriteFrame.fromIndex(selectedFrameIndex),
            isDiscovered = isCurrentRevealed,
            context = context
        )

        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(16.dp))

            // 3x4 Grid Layout Inspector (12 full-color frames) - DEBUG ONLY
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101722)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DigiVectorIcon(type = DigiIconType.RADAR_SCAN, size = 14.dp, tint = Color(0xFF00E5FF))
                            Text(
                                text = "SPRITE STUDIO // SHEET MATRIX",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF004D40)
                        ) {
                            Text(
                                text = "DEBUG ONLY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64FFDA),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "12 raw dot-matrix frames [3 columns x 4 rows] for sprite sheet alignment & animation QA:",
                        fontSize = 9.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )

                    for (row in 0..3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0..2) {
                                val frameIndex = row * 3 + col
                                val frameMeta = SPRITE_GRID_FRAMES[frameIndex]
                                val isSelected = selectedFrameIndex == frameIndex

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(3.dp)
                                        .clickable {
                                            selectedFrameIndex = frameIndex
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFF16324A) else DigiWearPalette.CardSurface
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) DigiWearPalette.DigiviceCyan else DigiWearPalette.CardBorder),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(5.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "[$row,$col] #${frameIndex}",
                                            fontSize = 7.5.sp,
                                            color = Color.Gray,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        SpriteCanvasRenderer(
                                            speciesId = selectedSpeciesId,
                                            frame = SpriteFrame.fromIndex(frameIndex),
                                            size = 44.dp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = frameMeta.name,
                                            fontSize = 7.5.sp,
                                            maxLines = 1,
                                            color = Color.LightGray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Functional Action Buttons: Morph Active Partner or Deposit to Vault (DEBUG DEV TOOLS)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val morphedState = PetState(
                                        speciesId = selectedSpecies.id,
                                        currentHp = selectedSpecies.baseHp,
                                        maxHp = selectedSpecies.baseHp,
                                        hunger = 4,
                                        strength = 4,
                                        weight = selectedSpecies.baselineWeight,
                                        effort = 4,
                                        victories = 10,
                                        totalBattles = 12,
                                        lastProcessedTimestampUtc = System.currentTimeMillis() / 1000L
                                    )
                                    db.phoneDao().insertSnapshot(SyncedPetEntity.fromDomain(morphedState))
                                    val sentToWatch = wearableManager.sendPetStateToWatch(morphedState)
                                    DigiWearAppWidgetProvider.updateAllWidgets(context)
                                    syncMessage = if (sentToWatch) {
                                        "[DEBUG] Morphed active partner to ${selectedSpecies.name} & synced to Watch!"
                                    } else {
                                        "[DEBUG] Set ${selectedSpecies.name} on Phone Dashboard & Widget (Watch offline)"
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                        ) {
                            DigiVectorIcon(type = DigiIconType.BEAM_ANTENNA, size = 14.dp, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("MORPH PARTNER", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val vaultState = PetState(
                                        speciesId = selectedSpecies.id,
                                        currentHp = selectedSpecies.baseHp,
                                        maxHp = selectedSpecies.baseHp,
                                        hunger = 4,
                                        strength = 4,
                                        weight = selectedSpecies.baselineWeight,
                                        ageSeconds = 3600L,
                                        careMistakes = 0,
                                        effort = 4,
                                        overfeedCount = 0,
                                        totalBattles = 10,
                                        victories = 8,
                                        lastProcessedTimestampUtc = System.currentTimeMillis() / 1000L
                                    )
                                    db.phoneDao().insertVaultPet(
                                        StoredDigimonEntity.fromDomain(
                                            state = vaultState,
                                            species = selectedSpecies,
                                            notes = "Synthesized via DigiDex Sprite Studio (Debug)"
                                        )
                                    )
                                    syncMessage = "[DEBUG] Added ${selectedSpecies.name} to DigiVault Archive!"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF))
                        ) {
                            DigiVectorIcon(type = DigiIconType.VAULT_SAFE, size = 14.dp, tint = Color(0xFF00E5FF))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ADD TO VAULT", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF00E5FF))
                        }
                    }

                    syncMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "▸ $it", fontSize = 10.sp, color = Color(0xFF00E676), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }

    if (showCutInPortrait) {
        PhoneCutInOverlay(
            speciesId = selectedSpeciesId,
            title = "OFFICIAL ALBUM CUT-IN",
            autoDismissMs = null,
            onDismiss = { showCutInPortrait = false }
        )
    }
}
}

@Composable
private fun StageFilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
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
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 8.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.LightGray,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StudioSpritePreviewShell(
    selectedSpeciesId: Int,
    isDiscovered: Boolean,
    previewMode: String,
    onPreviewModeChange: (String) -> Unit,
    selectedFrameIndex: Int,
    onFrameIndexChange: (Int) -> Unit
) {
    var animFrame by remember { mutableIntStateOf(selectedFrameIndex) }

    LaunchedEffect(selectedFrameIndex) {
        animFrame = selectedFrameIndex
    }

    LaunchedEffect(previewMode) {
        while (true) {
            when (previewMode) {
                "IDLE" -> {
                    animFrame = 0
                    delay(500L)
                    animFrame = 1
                    delay(500L)
                }
                "WALK" -> {
                    animFrame = 2
                    delay(350L)
                    animFrame = 3
                    delay(350L)
                }
                "FEED" -> {
                    animFrame = 4
                    delay(400L)
                    animFrame = 5
                    delay(600L)
                }
                "SLEEP" -> {
                    animFrame = 6
                    delay(600L)
                    animFrame = 7
                    delay(600L)
                }
                "ATTACK" -> {
                    animFrame = 9
                    delay(500L)
                    animFrame = 10
                    delay(500L)
                }
                "STATUS" -> {
                    animFrame = 8
                    delay(650L)
                    animFrame = 11
                    delay(650L)
                }
            }
        }
    }

    VPetBrickShellFrame(
        selectedIconIndex = PREVIEW_MODES.indexOf(previewMode).coerceAtLeast(0),
        statusLabel = "FRAME $animFrame • $previewMode",
        screenHeight = 140.dp,
        onButtonA = {
            val nextIdx = (PREVIEW_MODES.indexOf(previewMode) + 1) % PREVIEW_MODES.size
            onPreviewModeChange(PREVIEW_MODES[nextIdx])
        },
        onButtonB = {
            val nextFrame = (animFrame + 1) % 12
            animFrame = nextFrame
            onFrameIndexChange(nextFrame)
        },
        onButtonC = {
            onPreviewModeChange("IDLE")
            animFrame = 0
            onFrameIndexChange(0)
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                SpriteCanvasRenderer(
                    speciesId = selectedSpeciesId,
                    frame = SpriteFrame.fromIndex(animFrame),
                    isSilhouette = !isDiscovered,
                    size = 106.dp
                )
                if (!isDiscovered) {
                    Text(
                        text = "???",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Text(
                text = if (isDiscovered) SPRITE_GRID_FRAMES[animFrame].name.uppercase() else "UNDISCOVERED SPECIMEN",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF81C784),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun CustomSpriteMatrixEditorCard(
    speciesId: Int,
    frame: SpriteFrame,
    isDiscovered: Boolean,
    context: android.content.Context
) {
    var expanded by remember { mutableStateOf(false) }
    var activeBrush by remember { mutableStateOf('#') }
    var editableRows by remember(speciesId, frame) {
        val currentMatrix = com.digiwear.companion.render.PixelArtMatrices.getCustomMatrix(speciesId, frame)
            ?: com.digiwear.core.render.SpriteMatrixRegistry.getMatrix(speciesId, frame)
        mutableStateOf(currentMatrix.toList())
    }
    var statusNote by remember(speciesId, frame) { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "▸ 16x16 PIXEL ART MATRIX STUDIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DigiWearPalette.DigiviceCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Customize & persist 16x16 dot-matrix frame overrides locally",
                        fontSize = 8.5.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
                OutlinedButton(
                    onClick = { if (isDiscovered) expanded = !expanded },
                    enabled = isDiscovered,
                    border = BorderStroke(1.dp, if (isDiscovered) DigiWearPalette.DigiviceCyan else Color.DarkGray)
                ) {
                    Text(
                        text = if (!isDiscovered) "LOCKED" else if (expanded) "HIDE GRID" else "EDIT 16x16",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDiscovered) DigiWearPalette.DigiviceCyan else Color.Gray
                    )
                }
            }

            if (expanded && isDiscovered) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val brushes = listOf('#' to "PRIMARY", 'S' to "SECOND", 'E' to "EYE", 'B' to "OUTLINE", '.' to "ERASE")
                    brushes.forEach { (ch, label) ->
                        val selected = activeBrush == ch
                        Box(
                            modifier = Modifier
                                .background(if (selected) Color(0xFF00897B) else Color(0xFF1B2430), RoundedCornerShape(6.dp))
                                .border(1.dp, if (selected) Color(0xFF64FFDA) else Color(0xFF37474F), RoundedCornerShape(6.dp))
                                .clickable { activeBrush = ch }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(label, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(Color(0xFF0B1017), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    for (r in 0 until 16) {
                        val rowStr = editableRows.getOrElse(r) { "................" }.padEnd(16, '.')
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            for (c in 0 until 16) {
                                val pixelChar = rowStr[c]
                                val cellColor = when (pixelChar) {
                                    '#' -> Color(0xFF00E5FF)
                                    'S' -> Color(0xFFFFB74D)
                                    'T' -> Color(0xFFBA68C8)
                                    'E' -> Color(0xFFFF5252)
                                    'W' -> Color.White
                                    'B', 'K' -> Color(0xFF546E7A)
                                    else -> Color(0xFF141C26)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(cellColor)
                                        .clickable {
                                            val updated = editableRows.toMutableList()
                                            val currentRow = updated.getOrElse(r) { "................" }.padEnd(16, '.').toCharArray()
                                            currentRow[c] = if (currentRow[c] == activeBrush) '.' else activeBrush
                                            updated[r] = String(currentRow)
                                            editableRows = updated
                                        }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            com.digiwear.companion.render.PixelArtMatrices.setCustomMatrix(
                                context = context,
                                speciesId = speciesId,
                                frame = frame,
                                matrixRows = editableRows.toTypedArray()
                            )
                            statusNote = "Saved custom 16x16 matrix for #$speciesId (${frame.name})!"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                    ) {
                        Text("SAVE OVERRIDE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    OutlinedButton(
                        onClick = {
                            com.digiwear.companion.render.PixelArtMatrices.clearCustomMatrix(context, speciesId, frame)
                            editableRows = com.digiwear.core.render.SpriteMatrixRegistry.getMatrix(speciesId, frame).toList()
                            statusNote = "Restored canonical Bandai sprite matrix for #$speciesId."
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFFFF8A80))
                    ) {
                        Text("RESET DEFAULT", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80), fontFamily = FontFamily.Monospace)
                    }
                }

                statusNote?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, fontSize = 9.sp, color = Color(0xFF00E676), fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

