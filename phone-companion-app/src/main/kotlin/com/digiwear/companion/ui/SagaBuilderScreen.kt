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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.digiwear.companion.data.CustomCampaignEntity
import com.digiwear.companion.data.PhoneDatabase
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.render.SpriteCanvasRenderer
import com.digiwear.core.adventure.AdventureEngine
import com.digiwear.core.model.BossEncounter
import com.digiwear.core.model.CampaignNode
import com.digiwear.core.model.CampaignStory
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.SpriteFrame
import kotlinx.coroutines.launch

/**
 * Visual In-App Saga & Campaign Architect for designing custom adventure sectors,
 * selecting environmental hazards, configuring 550-monster boss encounters,
 * exporting/importing JSON schemas, and beaming directly to smartwatches over Wi-Fi/Bluetooth.
 */
@Composable
fun SagaBuilderScreen(
    initialStory: CampaignStory? = null,
    db: PhoneDatabase,
    wearableManager: PhoneWearableManager,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var campaignId by remember { mutableStateOf(initialStory?.campaignId ?: "custom_saga_${System.currentTimeMillis() % 10000}") }
    var title by remember { mutableStateOf(initialStory?.title ?: "Dark Area Incursion") }
    var description by remember { mutableStateOf(initialStory?.description ?: "Custom tamer campaign created with DigiWear Saga Architect.") }
    var recommendedStage by remember { mutableStateOf(initialStory?.recommendedStage ?: "Adult") }

    val nodes = remember {
        mutableStateListOf<CampaignNode>().apply {
            if (initialStory != null && initialStory.nodes.isNotEmpty()) {
                addAll(initialStory.nodes)
            } else {
                add(
                    CampaignNode(
                        nodeId = 1,
                        name = "Digital Boundary Gate",
                        requiredSteps = 1000,
                        environmentEffect = "clear",
                        encounterRate = 0.25f,
                        encounterPool = listOf(201, 202, 203),
                        bossEncounter = BossEncounter(
                            bossMonsterId = 301,
                            bossHp = 120,
                            bossPower = 45,
                            bossName = "Greymon",
                            introDialogue = "None shall cross the firewall!",
                            defeatDialogue = "You have proven worthy...",
                            rewardItem = "Firewall Key"
                        ),
                        narrativeText = "A mysterious gateway stands before the digital horizon."
                    )
                )
            }
        }
    }

    var isRawJsonMode by remember { mutableStateOf(false) }
    var rawJsonText by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isDeploying by remember { mutableStateOf(false) }

    // Boss picker modal state
    var pickingBossForNodeIndex by remember { mutableStateOf<Int?>(null) }

    fun buildCurrentStory(): CampaignStory {
        return CampaignStory(
            campaignId = campaignId.trim().ifEmpty { "custom_saga" },
            title = title.trim().ifEmpty { "Custom Saga" },
            nodes = nodes.mapIndexed { idx, n -> n.copy(nodeId = idx + 1) },
            description = description.trim(),
            recommendedStage = recommendedStage.trim()
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DigiWearPalette.CyberBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFF546E7A))
                ) {
                    Text("← CLOSE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                }

                Text(
                    text = "SAGA ARCHITECT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = DigiWearPalette.DigiviceCyan
                )

                // Toggle Visual vs Raw JSON
                OutlinedButton(
                    onClick = {
                        if (!isRawJsonMode) {
                            rawJsonText = AdventureEngine.serializeCampaign(buildCurrentStory())
                        } else {
                            try {
                                val parsed = AdventureEngine.parseCampaign(rawJsonText)
                                campaignId = parsed.campaignId
                                title = parsed.title
                                description = parsed.description
                                recommendedStage = parsed.recommendedStage
                                nodes.clear()
                                nodes.addAll(parsed.nodes)
                                parseError = null
                            } catch (e: Exception) {
                                parseError = "Invalid JSON: ${e.message}"
                                return@OutlinedButton
                            }
                        }
                        isRawJsonMode = !isRawJsonMode
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, if (isRawJsonMode) Color(0xFFFFD54F) else Color(0xFF00E5FF))
                ) {
                    Text(
                        text = if (isRawJsonMode) "VISUAL" else "RAW JSON",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isRawJsonMode) Color(0xFFFFD54F) else Color(0xFF00E5FF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isRawJsonMode) {
                // Raw JSON Editor Mode
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = rawJsonText,
                        onValueChange = {
                            rawJsonText = it
                            try {
                                AdventureEngine.parseCampaign(it)
                                parseError = null
                            } catch (e: Exception) {
                                parseError = e.message
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    )

                    parseError?.let {
                        Text(
                            text = "⚠ $it",
                            fontSize = 10.sp,
                            color = Color(0xFFFF5252),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                // Visual Editor Mode
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Campaign Metadata Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E2E3E))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("CAMPAIGN METADATA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Saga Title", fontSize = 9.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = campaignId,
                                    onValueChange = { campaignId = it },
                                    label = { Text("Campaign ID", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                )
                                OutlinedTextField(
                                    value = recommendedStage,
                                    onValueChange = { recommendedStage = it },
                                    label = { Text("Stage Gate", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sector Nodes Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECTOR NODES (${nodes.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFD54F)
                        )

                        Button(
                            onClick = {
                                val nextId = nodes.size + 1
                                nodes.add(
                                    CampaignNode(
                                        nodeId = nextId,
                                        name = "Sector $nextId",
                                        requiredSteps = 1500,
                                        environmentEffect = "clear",
                                        bossEncounter = BossEncounter(
                                            bossMonsterId = 301,
                                            bossHp = 150,
                                            bossPower = 50,
                                            bossName = "Boss Monster"
                                        ),
                                        narrativeText = "Adventure node $nextId."
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+ ADD NODE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Render each sector node card
                    nodes.forEachIndexed { index, node ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1622)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF263238))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${index + 1} SECTOR",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF),
                                        fontFamily = FontFamily.Monospace
                                    )

                                    if (nodes.size > 1) {
                                        Text(
                                            text = "[REMOVE ✕]",
                                            fontSize = 9.sp,
                                            color = Color(0xFFFF5252),
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.clickable {
                                                nodes.removeAt(index)
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = node.name,
                                        onValueChange = { newName ->
                                            nodes[index] = node.copy(name = newName)
                                        },
                                        label = { Text("Sector Name", fontSize = 8.sp) },
                                        modifier = Modifier.weight(2f),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                    )

                                    OutlinedTextField(
                                        value = node.requiredSteps.toString(),
                                        onValueChange = { newStepsStr ->
                                            val stepsVal = newStepsStr.filter { it.isDigit() }.toIntOrNull() ?: 100
                                            nodes[index] = node.copy(requiredSteps = stepsVal)
                                        },
                                        label = { Text("Steps", fontSize = 8.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF00E676))
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Environmental Hazards Selector
                                Text("ENVIRONMENT EFFECT:", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                val hazards = listOf("clear", "thunderstorm", "magma", "miasma", "gravity", "void")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    hazards.forEach { h ->
                                        val isSel = node.environmentEffect.equals(h, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .background(if (isSel) Color(0xFF004D40) else Color(0xFF141C24), RoundedCornerShape(6.dp))
                                                .border(1.dp, if (isSel) Color(0xFF00E5FF) else Color(0xFF263238), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    nodes[index] = node.copy(environmentEffect = h)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = h.uppercase(),
                                                fontSize = 7.5.sp,
                                                color = if (isSel) Color.White else Color.Gray,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Sector Boss Encounter
                                val boss = node.bossEncounter
                                val bossSpecies = PetSpecies.getById(boss?.bossMonsterId ?: 301)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF141C24), RoundedCornerShape(6.dp))
                                        .clickable { pickingBossForNodeIndex = index }
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SpriteCanvasRenderer(
                                        speciesId = bossSpecies.id,
                                        frame = SpriteFrame.IDLE_A,
                                        isAmbient = false,
                                        size = 32.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "BOSS: ${bossSpecies.name.uppercase()}",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFB300),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${bossSpecies.stage.displayName} • ${bossSpecies.attribute.displayName} • HP: ${boss?.bossHp ?: 100}",
                                            fontSize = 7.5.sp,
                                            color = Color.LightGray,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text("[CHANGE]", fontSize = 8.sp, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Save locally
                Button(
                    onClick = {
                        val story = if (isRawJsonMode) {
                            try {
                                AdventureEngine.parseCampaign(rawJsonText)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        } else {
                            buildCurrentStory()
                        }
                        val json = AdventureEngine.serializeCampaign(story)
                        scope.launch {
                            db.phoneDao().insertOrUpdateCampaign(
                                CustomCampaignEntity(
                                    campaignId = story.campaignId,
                                    title = story.title,
                                    jsonContent = json
                                )
                            )
                            Toast.makeText(context, "Saved saga '${story.title}' to DigiVault!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                ) {
                    Text("💾 SAVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                }

                // Beam to watch
                Button(
                    onClick = {
                        val story = if (isRawJsonMode) {
                            try {
                                AdventureEngine.parseCampaign(rawJsonText)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        } else {
                            buildCurrentStory()
                        }
                        val json = AdventureEngine.serializeCampaign(story)
                        isDeploying = true
                        scope.launch {
                            // Also save locally
                            db.phoneDao().insertOrUpdateCampaign(
                                CustomCampaignEntity(
                                    campaignId = story.campaignId,
                                    title = story.title,
                                    jsonContent = json
                                )
                            )
                            val sent = wearableManager.sendCampaignToWatch(json)
                            isDeploying = false
                            if (sent) {
                                Toast.makeText(context, "⚡ Saga '${story.title}' beamed to Watch!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Saved locally (Watch currently offline)", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1.5f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DigiWearPalette.CrestGold)
                ) {
                    Text(
                        text = if (isDeploying) "BEAMING..." else "⚡ BEAM TO WATCH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )
                }

                // Copy JSON
                OutlinedButton(
                    onClick = {
                        val story = buildCurrentStory()
                        val json = AdventureEngine.serializeCampaign(story)
                        clipboardManager.setText(AnnotatedString(json))
                        Toast.makeText(context, "Saga JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF546E7A))
                ) {
                    Text("📋", fontSize = 12.sp)
                }
            }
        }
    }

    // Boss Species Picker Dialog
    pickingBossForNodeIndex?.let { nodeIdx ->
        Dialog(
            onDismissRequest = { pickingBossForNodeIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD000000))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1722)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SELECT APEX BOSS MONSTER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                            Text("[CLOSE ✕]", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.clickable { pickingBossForNodeIndex = null })
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Scrollable grid of species
                        val sortedAdultUp = remember {
                            PetSpecies.SORTED_SPECIES.filter { it.stage.ordinal >= 3 }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            sortedAdultUp.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    pair.forEach { sp ->
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(Color(0xFF16202C), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val currentNode = nodes[nodeIdx]
                                                    nodes[nodeIdx] = currentNode.copy(
                                                        bossEncounter = BossEncounter(
                                                            bossMonsterId = sp.id,
                                                            bossHp = sp.baseHp * 2,
                                                            bossPower = sp.basePower,
                                                            bossName = sp.name,
                                                            introDialogue = "Beware the power of ${sp.name}!",
                                                            defeatDialogue = "Impossible... my digital core..."
                                                        )
                                                    )
                                                    pickingBossForNodeIndex = null
                                                }
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            SpriteCanvasRenderer(
                                                speciesId = sp.id,
                                                frame = SpriteFrame.IDLE_A,
                                                isAmbient = false,
                                                size = 28.dp
                                            )
                                            Column {
                                                Text(
                                                    text = sp.name,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${sp.stage.displayName} • ${sp.attribute.displayName}",
                                                    fontSize = 7.sp,
                                                    color = Color.LightGray,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
