package com.digiwear.companion.ui

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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.digiwear.companion.data.CustomCampaignEntity
import com.digiwear.companion.data.PhoneDatabase
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.render.SpriteCanvasRenderer
import com.digiwear.core.adventure.AdventureEngine
import com.digiwear.core.model.CampaignStory
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.SpriteFrame
import kotlinx.coroutines.launch

/**
 * Clean, uncluttered Digital World Expedition hub.
 * Focuses on active adventure saga tracking, quick saga presets, and one-tap watch deployment.
 * Detailed 114-sector roadmaps and the story architect/JSON editor are accessed via dedicated buttons and modals.
 */
@Composable
fun ExpeditionScreen(
    db: PhoneDatabase,
    wearableManager: PhoneWearableManager
) {
    var rawJson by remember { mutableStateOf(AdventureEngine.DEFAULT_CAMPAIGN_JSON.trim()) }
    var parsedStory by remember { mutableStateOf<CampaignStory?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var deployStatus by remember { mutableStateOf<String?>(null) }

    var showSectorDetailsModal by remember { mutableStateOf(false) }
    var showEditorModal by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val builtInSagas = remember { AdventureEngine.getBuiltInCampaigns() }
    val customCampaigns by db.phoneDao().getAllCampaignsFlow().collectAsState(initial = emptyList())

    // Parse on start or change
    LaunchedEffect(rawJson) {
        try {
            parsedStory = AdventureEngine.parseCampaign(rawJson)
            parseError = null
        } catch (e: Exception) {
            parseError = "JSON Schema Error: ${e.message}"
            parsedStory = null
        }
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
                tag = "EXPEDITION // DIGITAL WORLD CAMPAIGN",
                title = parsedStory?.title ?: "Adventure Saga",
                subtitle = "Step exploration, wild encounters & boss trials",
                accentColor = Color(0xFF26A69A)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Deployment Status Banner
            deployStatus?.let { status ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142E28)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E676))
                ) {
                    Text(
                        text = "▸ $status",
                        fontSize = 11.sp,
                        color = Color(0xFF00E676),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // --- 1. ACTIVE SAGA SUMMARY CARD ---
            parsedStory?.let { story ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF26A69A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE ADVENTURE SAGA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF80CBC4),
                                fontFamily = FontFamily.Monospace
                            )
                            StageBadge(story.recommendedStage)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = story.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = story.description,
                            fontSize = 10.5.sp,
                            color = Color(0xFFB0BEC5),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats metrics bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${story.nodes.size}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00E5FF),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("SECTORS", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                            Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color(0xFF37474F)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val totalSteps = story.nodes.sumOf { it.requiredSteps }
                                Text(
                                    text = if (totalSteps >= 1000) "${totalSteps / 1000}K" else "$totalSteps",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = DigiWearPalette.CrestGold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("TOTAL STEPS", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                            Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color(0xFF37474F)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${story.nodes.count { it.bossEncounter != null }}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF8A80),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("BOSS TRIALS", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Direct Deploy Button
                        Button(
                            onClick = {
                                scope.launch {
                                    val ok = wearableManager.sendCampaignToWatch(rawJson)
                                    deployStatus = if (ok) {
                                        "Saga '${story.title}' deployed to Smartwatch!"
                                    } else {
                                        "Saga '${story.title}' saved locally on phone."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.BEAM_ANTENNA, size = 16.dp, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DEPLOY SAGA TO SMARTWATCH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 2. CANONICAL SAGAS PRESET SELECTOR CAROUSEL ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "▸ 5 GRAND CANONICAL ADVENTURE SAGAS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80CBC4),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Swipe to explore authentic story arcs with 500-species wild rosters:",
                        fontSize = 9.5.sp,
                        color = Color(0xFF90A4AE)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        builtInSagas.forEach { saga ->
                            val isCurrent = parsedStory?.campaignId.equals(saga.campaignId, ignoreCase = true)
                            Card(
                                modifier = Modifier
                                    .width(230.dp)
                                    .height(180.dp)
                                    .clickable {
                                        rawJson = AdventureEngine.serializeCampaign(saga)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) Color(0xFF004D40) else DigiWearPalette.CardInner
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    if (isCurrent) 1.5.dp else 1.dp,
                                    if (isCurrent) Color(0xFF64FFDA) else Color(0xFF263238)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = saga.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color(0xFF64FFDA) else Color.White,
                                            maxLines = 2,
                                            lineHeight = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${saga.nodes.size} Sectors • ${saga.recommendedStage}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF80CBC4),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = saga.description,
                                            fontSize = 8.5.sp,
                                            color = Color(0xFFB0BEC5),
                                            maxLines = 2,
                                            lineHeight = 12.sp,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            rawJson = AdventureEngine.serializeCampaign(saga)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCurrent) Color(0xFF00897B) else Color(0xFF263238),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(
                                            text = if (isCurrent) "SELECTED [OK]" else "SELECT SAGA",
                                            color = Color.White,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Saved Custom Campaigns Carousel (if any exist)
            if (customCampaigns.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "▸ SAVED CUSTOM CAMPAIGNS (${customCampaigns.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
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
                            customCampaigns.forEach { c ->
                                val isCurrent = parsedStory?.campaignId.equals(c.campaignId, ignoreCase = true)
                                Card(
                                    modifier = Modifier
                                        .width(230.dp)
                                        .height(130.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) Color(0xFF332A15) else DigiWearPalette.CardInner
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        if (isCurrent) 1.5.dp else 1.dp,
                                        if (isCurrent) Color(0xFFFFD54F) else Color(0xFF263238)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = c.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) Color(0xFFFFE082) else Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "ID: ${c.campaignId}",
                                                fontSize = 9.sp,
                                                color = Color(0xFFB0BEC5),
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = { rawJson = c.jsonContent },
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isCurrent) Color(0xFFFFA000) else Color(0xFF37474F),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text(
                                                    text = if (isCurrent) "SELECTED" else "LOAD",
                                                    color = Color.White,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        db.phoneDao().deleteCampaign(c.campaignId)
                                                    }
                                                },
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                                border = BorderStroke(1.dp, Color(0x66FF5252))
                                            ) {
                                                DigiVectorIcon(type = DigiIconType.CLOSE, size = 14.dp, tint = Color(0xFFFF5252))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 3. DEDICATED BUTTONS FOR QUEST DETAILS & STORY ARCHITECT ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "▸ EXPLORATION ARCHIVES & STORY TOOLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80CBC4),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Access complete 114-sector roadmap details or open the campaign architect:",
                        fontSize = 9.5.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Button A: Quest & Sector Details
                    Button(
                        onClick = { showSectorDetailsModal = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DigiVectorIcon(type = DigiIconType.MAP_COMPASS, size = 15.dp, tint = Color.White)
                            Text(
                                text = "VIEW QUEST & SECTOR DETAILS (${parsedStory?.nodes?.size ?: 0} SECTORS)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button B: Story Architect & JSON Schema Editor
                    OutlinedButton(
                        onClick = { showEditorModal = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80CBC4)),
                        border = BorderStroke(1.dp, Color(0xFF00897B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DigiVectorIcon(type = DigiIconType.ARCHITECT_PEN, size = 14.dp, tint = Color(0xFF80CBC4))
                            Text(
                                text = "STORY ARCHITECT & JSON EDITOR",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF80CBC4)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // =========================================================================
    // MODAL 1: FULL SECTOR-BY-SECTOR QUEST DETAILS ROADMAP
    // =========================================================================
    if (showSectorDetailsModal) {
        Dialog(
            onDismissRequest = { showSectorDetailsModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DigiWearPalette.CyberBg)
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Modal Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF101923))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DigiVectorIcon(type = DigiIconType.MAP_COMPASS, size = 14.dp, tint = Color(0xFF80CBC4))
                                Text(
                                    text = "ROADMAP // ${parsedStory?.title?.uppercase() ?: "QUEST"}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${parsedStory?.nodes?.size ?: 0} Sectors • Rec: ${parsedStory?.recommendedStage ?: "ANY"} • ${parsedStory?.nodes?.sumOf { it.requiredSteps } ?: 0} Steps",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { showSectorDetailsModal = false },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF546E7A)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 72.dp, minHeight = 32.dp)
                        ) {
                            Text("CLOSE", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace, maxLines = 1)
                        }
                    }

                    // Sector Nodes List
                    val sectorScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(sectorScrollState)
                    ) {
                        parsedStory?.nodes?.forEachIndexed { index, node ->
                            val actLabel = node.actTitle.ifBlank { "SECTOR ${index + 1}" }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardInner),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF222E3C))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = actLabel,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF26A69A),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "Sector ${index + 1}: ${node.name}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF263238), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${node.requiredSteps} STEPS",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DigiWearPalette.CrestGold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Text(
                                        text = "\"${node.narrativeText}\"",
                                        fontSize = 11.sp,
                                        color = Color(0xFFB0BEC5),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    Text(
                                        text = "Env: ${node.environmentEffect} • Encounter Rate: ${(node.encounterRate * 100).toInt()}%",
                                        fontSize = 9.sp,
                                        color = Color(0xFF78909C),
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // Encounter Pool Mini Sprites
                                    if (node.encounterPool.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "WILD:",
                                                fontSize = 8.sp,
                                                color = Color.Gray,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            node.encounterPool.take(6).forEach { encId ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(Color(0xFF161F28), RoundedCornerShape(4.dp))
                                                        .padding(2.dp)
                                                ) {
                                                    SpriteCanvasRenderer(
                                                        speciesId = encId,
                                                        frame = SpriteFrame.IDLE_A,
                                                        size = 20.dp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    node.bossEncounter?.let { boss ->
                                        val bossSpecies = PetSpecies.getById(boss.bossMonsterId)
                                        val bossDisplayName = boss.bossName.ifBlank { bossSpecies.name }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF2C1518), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                SpriteCanvasRenderer(
                                                    speciesId = bossSpecies.id,
                                                    frame = SpriteFrame.IDLE_A,
                                                    size = 32.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                        DigiVectorIcon(type = DigiIconType.SKULL_INJURY, size = 11.dp, tint = Color(0xFFFF8A80))
                                                        Text(
                                                            text = "BOSS: ${bossDisplayName.uppercase()}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFFFF8A80),
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                    if (boss.introDialogue.isNotBlank()) {
                                                        Text(
                                                            text = "\"${boss.introDialogue}\"",
                                                            fontSize = 8.5.sp,
                                                            color = Color(0xFFFFCCBC)
                                                        )
                                                    }
                                                    Text(
                                                        text = "HP: ${boss.bossHp} • PWR: ${boss.bossPower} • ${boss.completionFlag}",
                                                        fontSize = 8.5.sp,
                                                        color = Color(0xFFCFD8DC),
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    if (boss.rewardItem.isNotBlank() || boss.rewardEggId != null) {
                                                        val rewardStr = buildString {
                                                            if (boss.rewardEggId != null) append("Unlocks Egg #${boss.rewardEggId} • ")
                                                            if (boss.rewardItem.isNotBlank()) append(boss.rewardItem)
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            DigiVectorIcon(type = DigiIconType.TROPHY_CROWN, size = 10.dp, tint = Color(0xFFFFD54F))
                                                            Text(
                                                                text = rewardStr,
                                                                fontSize = 8.sp,
                                                                color = Color(0xFFFFD54F),
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            StageBadge(bossSpecies.stage.displayName)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // =========================================================================
    // MODAL 2: STORY ARCHITECT & JSON SCHEMA EDITOR
    // =========================================================================
    if (showEditorModal) {
        Dialog(
            onDismissRequest = { showEditorModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DigiWearPalette.CyberBg)
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Modal Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF101923))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.ARCHITECT_PEN, size = 14.dp, tint = Color(0xFF80CBC4))
                            Text(
                                text = "STORY ARCHITECT // JSON EDITOR",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { showEditorModal = false },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF546E7A)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 72.dp, minHeight = 32.dp)
                        ) {
                            Text("CLOSE", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace, maxLines = 1)
                        }
                    }

                    val editorScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(editorScrollState)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "RAW JSON STORY SCHEMA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF80CBC4),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Edit campaign JSON directly or paste custom campaigns below:",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = rawJson,
                                    onValueChange = { rawJson = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFFE0E0E0)
                                    )
                                )

                                parseError?.let {
                                    Text(
                                        text = it,
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF5252),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            rawJson = AdventureEngine.DEFAULT_CAMPAIGN_JSON.trim()
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF546E7A)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("RESET DEFAULT", fontSize = 10.sp, color = Color(0xFFB0BEC5), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                parsedStory?.let { story ->
                                                    db.phoneDao().insertOrUpdateCampaign(
                                                        CustomCampaignEntity(
                                                            campaignId = story.campaignId,
                                                            title = story.title,
                                                            jsonContent = rawJson
                                                        )
                                                    )
                                                    val ok = wearableManager.sendCampaignToWatch(rawJson)
                                                    deployStatus = if (ok) {
                                                        "Campaign '${story.title}' deployed to Smartwatch!"
                                                    } else {
                                                        "Campaign saved locally (Smartwatch offline)"
                                                    }
                                                    showEditorModal = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                                    ) {
                                        DigiVectorIcon(type = DigiIconType.BEAM_ANTENNA, size = 15.dp, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SAVE & DEPLOY", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
