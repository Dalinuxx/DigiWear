package com.digiwear.wear.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import com.digiwear.core.dex.DigiDexEngine
import com.digiwear.core.model.ElementalAttribute
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetStage
import com.digiwear.core.model.SpriteFrame
import com.digiwear.wear.input.HardwareInputManager
import com.digiwear.wear.input.VPetButton
import com.digiwear.wear.render.SpriteCanvasRenderer

/**
 * Wear OS 550-Species DigiDex Field Guide.
 * Optimized for circular smartwatch displays with rotary crown scrolling,
 * stage filters, 16x16 pixel sprites, and canonical discovery hints.
 */
@Composable
fun WatchDigiDexScreen(
    repository: com.digiwear.wear.data.PetRepository = com.digiwear.wear.data.PetRepository.getInstance(androidx.compose.ui.platform.LocalContext.current),
    onBack: () -> Unit
) {
    val displayConfig = WatchDisplayConfig.current()
    val listState = rememberScalingLazyListState()
    val discoveredIds by repository.discoveredSpeciesFlow.collectAsState(initial = emptySet())
    val maskSpoilers by repository.maskUndiscoveredSpeciesFlow.collectAsState(initial = true)
    var selectedStageFilter by remember { mutableStateOf<PetStage?>(null) }
    var selectedSpeciesId by remember { mutableStateOf<Int?>(null) }

    val filteredList = remember(selectedStageFilter) {
        if (selectedStageFilter == null) {
            PetSpecies.SORTED_SPECIES
        } else {
            PetSpecies.SORTED_SPECIES.filter { it.stage == selectedStageFilter }
        }
    }

    BackHandler {
        if (selectedSpeciesId != null) {
            selectedSpeciesId = null
        } else {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        HardwareInputManager.buttonEvents.collect { button ->
            when (button) {
                VPetButton.C -> {
                    if (selectedSpeciesId != null) selectedSpeciesId = null else onBack()
                }
                VPetButton.A -> {
                    // Step down
                }
                VPetButton.B -> {
                    if (selectedSpeciesId != null) selectedSpeciesId = null
                }
                else -> {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050810))
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = displayConfig.contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DIGIDEX 550",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${discoveredIds.size}/${PetSpecies.SORTED_SPECIES.size} Discovered",
                        fontSize = 7.5.sp,
                        color = Color(0xFF80CBC4),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick stage filter chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf(null, PetStage.CHILD, PetStage.ADULT, PetStage.PERFECT, PetStage.ULTIMATE).forEach { stage ->
                        val isSelected = selectedStageFilter == stage
                        val label = when (stage) {
                            null -> "ALL"
                            PetStage.CHILD -> "CHLD"
                            PetStage.ADULT -> "ADLT"
                            PetStage.PERFECT -> "PERF"
                            PetStage.ULTIMATE -> "ULTM"
                            else -> stage.displayName.take(4).uppercase()
                        }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .background(
                                    if (isSelected) Color(0xFF006064) else Color(0xFF141C28),
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFF00E5FF) else Color(0xFF263238),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { selectedStageFilter = stage }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFFB0BEC5),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Species items
            items(filteredList, key = { it.id }) { sp ->
                val isDiscovered = discoveredIds.contains(sp.id)
                val isRevealed = !maskSpoilers || isDiscovered
                val attrColor = when (sp.attribute) {
                    ElementalAttribute.VACCINE -> Color(0xFF00E5FF)
                    ElementalAttribute.DATA -> Color(0xFFFFCA28)
                    ElementalAttribute.VIRUS -> Color(0xFFFF5252)
                    ElementalAttribute.FREE -> Color(0xFF69F0AE)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = displayConfig.horizontalPadding, vertical = 1.5.dp)
                        .background(Color(0xFF101722), RoundedCornerShape(6.dp))
                        .border(0.5.dp, if (isRevealed) Color(0x3300E5FF) else Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                        .clickable { selectedSpeciesId = sp.id }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpriteCanvasRenderer(
                        speciesId = sp.id,
                        isAmbient = false,
                        overrideFrame = SpriteFrame.IDLE_A,
                        isSilhouette = !isRevealed,
                        size = 22.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRevealed) "#${sp.id} ${sp.name}" else "#${sp.id} ???",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRevealed) Color.White else Color(0xFF78909C),
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sp.stage.displayName.uppercase(),
                                fontSize = 6.sp,
                                color = Color(0xFF90A4AE),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRevealed) sp.attribute.displayName.uppercase() else "???",
                                fontSize = 6.sp,
                                color = if (isRevealed) attrColor else Color(0xFF546E7A),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .padding(horizontal = displayConfig.horizontalPadding),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF263238))
                ) {
                    Text(
                        text = "BACK [C]",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Detail Modal Overlay
        selectedSpeciesId?.let { id ->
            val sp = PetSpecies.getById(id)
            val isDiscovered = discoveredIds.contains(id)
            val isRevealed = !maskSpoilers || isDiscovered
            val hint = DigiDexEngine.getDiscoveryHint(id)
            val attrColor = when (sp.attribute) {
                ElementalAttribute.VACCINE -> Color(0xFF00E5FF)
                ElementalAttribute.DATA -> Color(0xFFFFCA28)
                ElementalAttribute.VIRUS -> Color(0xFFFF5252)
                ElementalAttribute.FREE -> Color(0xFF69F0AE)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF050810))
                    .clickable { selectedSpeciesId = null }
                    .padding(displayConfig.horizontalPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .background(Color(0xFF141D2B), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${sp.id}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = sp.stage.displayName.uppercase(),
                            fontSize = 7.5.sp,
                            color = Color(0xFFB0BEC5),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    SpriteCanvasRenderer(
                        speciesId = sp.id,
                        isAmbient = false,
                        overrideFrame = SpriteFrame.IDLE_A,
                        isSilhouette = !isRevealed,
                        size = 32.dp
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isRevealed) sp.name else "???",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRevealed) sp.attribute.displayName else "???",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRevealed) attrColor else Color(0xFF78909C),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isRevealed) "HP ${sp.baseHp}  PWR ${sp.basePower}" else "HP ???  PWR ???",
                            fontSize = 7.sp,
                            color = Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = hint,
                        fontSize = 6.5.sp,
                        color = Color(0xFFCFD8DC),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { selectedSpeciesId = null },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF263238)),
                        modifier = Modifier.height(22.dp)
                    ) {
                        Text(
                            text = "BACK [C]",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
