package com.digiwear.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.SpriteFrame
import com.digiwear.wear.render.SpriteCanvasRenderer

data class StarterEggUiModel(
    val speciesId: Int,
    val lineageTag: String,
    val eggTint: Color,
    val cardBg: Color
)

/**
 * Dedicated Wear OS Egg Selection Screen.
 * Automatically presented when no active partner/egg exists, or when
 * a partner is deposited into the DigiVault.
 */
@Composable
fun EggSelectionOverlay(
    onSelectEgg: (Int) -> Unit,
    canDismiss: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    val starterEggs = remember {
        listOf(
            StarterEggUiModel(101, "V1 • ORIGINAL 1997", Color(0xFF90A4AE), Color(0xFF1E2830)),
            StarterEggUiModel(103, "V2 • VER.2 ROSTER", Color(0xFFFF8A80), Color(0xFF2E1919)),
            StarterEggUiModel(105, "V3 • VER.3 OCEAN", Color(0xFF80D8FF), Color(0xFF102838)),
            StarterEggUiModel(107, "V4 • VER.4 JUNGLE", Color(0xFFA5D6A7), Color(0xFF152A18)),
            StarterEggUiModel(109, "V5 • VER.5 DARK", Color(0xFFC5E1A5), Color(0xFF222B15)),
            StarterEggUiModel(111, "PEN • NATURE SPIRITS", Color(0xFFFFCC80), Color(0xFF332210)),
            StarterEggUiModel(113, "VIR • DEEP DIABORO", Color(0xFFCE93D8), Color(0xFF2C1636)),
            StarterEggUiModel(130, "WND • WIND GUARDIANS", Color(0xFFF48FB1), Color(0xFF331825)),
            StarterEggUiModel(131, "DEP • DEEP SAVERS", Color(0xFF80CBC4), Color(0xFF122C28)),
            StarterEggUiModel(132, "MTL • METAL EMPIRE", Color(0xFF90CAF9), Color(0xFF142436)),
            StarterEggUiModel(1101, "Z • NIGHTMARE ROSTER", Color(0xFFBCAAA4), Color(0xFF2E221E)),
            StarterEggUiModel(1104, "D3 • DRAGON ROSTER", Color(0xFF80DEEA), Color(0xFF102D30)),
            StarterEggUiModel(1106, "DRK • ALGOR ROSTER", Color(0xFFB39DDB), Color(0xFF201736)),
            StarterEggUiModel(1108, "VTL • VITAL BRACELET", Color(0xFFFFAB91), Color(0xFF381C15)),
            StarterEggUiModel(1111, "CYB • CYBER SYSTEM", Color(0xFFA5D6A7), Color(0xFF182E1A)),
            StarterEggUiModel(1113, "COL • COLOR V-PET", Color(0xFFF06292), Color(0xFF351220))
        )
    }

    val listState = rememberScalingLazyListState()
    val displayConfig = WatchDisplayConfig.current()
    val horizPadding = if (displayConfig.isRound) 16.dp else 8.dp
    val topPadding = if (displayConfig.isRound) 28.dp else 12.dp
    val bottomPadding = if (displayConfig.isRound) 28.dp else 12.dp
    val cardWidthFraction = if (displayConfig.isRound) 0.86f else 0.94f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080D14)),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            autoCentering = AutoCenteringParams(itemIndex = 1),
            scalingParams = ScalingLazyColumnDefaults.scalingParams(
                edgeScale = 0.78f,
                edgeAlpha = 0.6f
            ),
            contentPadding = PaddingValues(
                top = topPadding,
                bottom = bottomPadding,
                start = horizPadding,
                end = horizPadding
            ),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DigiVectorIcon(type = DigiIconType.EGG, size = 12.dp, tint = Color(0xFFFFD54F))
                        Text(
                            text = "CHOOSE DIGI-EGG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFD54F)
                        )
                        DigiVectorIcon(type = DigiIconType.EGG, size = 12.dp, tint = Color(0xFFFFD54F))
                    }
                    Text(
                        text = "SELECT AN EGG TO INCUBATE",
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF80D8FF),
                        textAlign = TextAlign.Center
                    )
                }
            }

            items(starterEggs, key = { it.speciesId }) { egg ->
                val species = remember(egg.speciesId) { PetSpecies.getById(egg.speciesId) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(cardWidthFraction)
                        .background(egg.cardBg, RoundedCornerShape(10.dp))
                        .border(1.dp, egg.eggTint.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { onSelectEgg(egg.speciesId) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Egg Icon & Hatch Preview Sprite
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0x33000000), CircleShape)
                                .border(0.5.dp, egg.eggTint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            DigiVectorIcon(
                                type = DigiIconType.EGG,
                                size = 16.dp,
                                tint = egg.eggTint
                            )
                        }

                        Column {
                            Text(
                                text = species.name.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Text(
                                text = egg.lineageTag,
                                fontSize = 6.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = egg.eggTint
                            )
                        }
                    }

                    // Right: Hatch Button
                    Box(
                        modifier = Modifier
                            .background(egg.eggTint, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HATCH",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }
                }
            }

            if (canDismiss) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(24.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CANCEL", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                }
            }
        }
    }
}
