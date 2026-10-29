package com.digiwear.companion.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digiwear.core.model.ElementalAttribute
import com.digiwear.core.model.PetStage

/**
 * Shared Bandai V-Pet (1997 Brick-Wall Casing / Digimon Color / D-3 / D-Arc / Vital Bracelet)
 * & Cyber Digivice UI components for the Phone Companion App.
 */
object DigiWearPalette {
    val CyberBg = Color(0xFF0B0F17)
    val CardSurface = Color(0xFF171E2B)
    val CardInner = Color(0xFF101620)
    val CardBorder = Color(0xFF28354A)
    val LcdSageBg = Color(0xFF1D2D24)
    val LcdPhosphor = Color(0xFF80CBC4)
    val DigiviceCyan = Color(0xFF4DD0E1)
    val CrestGold = Color(0xFFFFCA28)
    val BrickTerracotta = Color(0xFF4A2C2A)
    val BrickBorder = Color(0xFF795548)
    val ShellSteel = Color(0xFF263238)
}

enum class DigiviceShellStyle(
    val id: String,
    val displayName: String,
    val shortName: String,
    val shellGradient: List<Color>,
    val borderColor: Color,
    val buttonColors: Triple<Color, Color, Color>, // A, B, C buttons
    val accentRivetColor: Color,
    val lcdCageColor: Color,
    val isBrickTextured: Boolean
) {
    ORIGINAL_1997_GREY(
        id = "1997_grey",
        displayName = "1997 Ver. 1 Classic Grey",
        shortName = "1997 GREY",
        shellGradient = listOf(Color(0xFF37474F), Color(0xFF263238), Color(0xFF1C262B)),
        borderColor = Color(0xFF607D8B),
        buttonColors = Triple(Color(0xFFFFCA28), Color(0xFF26A69A), Color(0xFFEF5350)),
        accentRivetColor = Color(0xFF90A4AE),
        lcdCageColor = Color(0xFF0D1412),
        isBrickTextured = true
    ),
    ORIGINAL_1997_BROWN(
        id = "1997_brown",
        displayName = "1997 Ver. 1 Terracotta",
        shortName = "1997 BROWN",
        shellGradient = listOf(Color(0xFF4E342E), Color(0xFF3E2723), Color(0xFF271510)),
        borderColor = Color(0xFF8D6E63),
        buttonColors = Triple(Color(0xFFFFD54F), Color(0xFF81C784), Color(0xFFE57373)),
        accentRivetColor = Color(0xFFD7CCC8),
        lcdCageColor = Color(0xFF16120E),
        isBrickTextured = true
    ),
    DIGIMON_COLOR_CLEAR(
        id = "color_clear",
        displayName = "Digimon Color Smoke Metal",
        shortName = "DM COLOR",
        shellGradient = listOf(Color(0xFF212529), Color(0xFF16191D), Color(0xFF0F1114)),
        borderColor = Color(0xFFFFD700),
        buttonColors = Triple(Color(0xFFFFD700), Color(0xFF00E5FF), Color(0xFFFF4081)),
        accentRivetColor = Color(0xFFFFD700),
        lcdCageColor = Color(0xFF080C0E),
        isBrickTextured = false
    ),
    D3_DIGIVICE_BLUE(
        id = "d3_blue",
        displayName = "D-3 Daisuke Deep Blue",
        shortName = "D-3 BLUE",
        shellGradient = listOf(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF0A2472)),
        borderColor = Color(0xFF80D8FF),
        buttonColors = Triple(Color(0xFF00E5FF), Color(0xFFFFFFFF), Color(0xFFFF5252)),
        accentRivetColor = Color(0xFF82B1FF),
        lcdCageColor = Color(0xFF051124),
        isBrickTextured = false
    ),
    D_ARC_SILVER(
        id = "d_arc_silver",
        displayName = "D-Arc Takato Crimson Alloy",
        shortName = "D-ARC",
        shellGradient = listOf(Color(0xFF78909C), Color(0xFF546E7A), Color(0xFF37474F)),
        borderColor = Color(0xFFFF1744),
        buttonColors = Triple(Color(0xFFFF1744), Color(0xFFB0BEC5), Color(0xFF263238)),
        accentRivetColor = Color(0xFFFF1744),
        lcdCageColor = Color(0xFF121920),
        isBrickTextured = false
    ),
    VITAL_BRACELET_CYBER(
        id = "vital_bracelet",
        displayName = "Vital Bracelet BE Cyber Stealth",
        shortName = "VITAL BE",
        shellGradient = listOf(Color(0xFF121417), Color(0xFF0A0C0E), Color(0xFF050608)),
        borderColor = Color(0xFF00E5FF),
        buttonColors = Triple(Color(0xFF00E5FF), Color(0xFF00E676), Color(0xFFFF1744)),
        accentRivetColor = Color(0xFF00E5FF),
        lcdCageColor = Color(0xFF040608),
        isBrickTextured = false
    );

    companion object {
        fun fromId(id: String?): DigiviceShellStyle =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ORIGINAL_1997_GREY
    }
}

private val TOP_VPET_ICONS = listOf(
    (DigiIconType.STATUS_SCALE to "STAT"),
    (DigiIconType.FEED_MEAT to "FEED"),
    (DigiIconType.TRAIN_GLOVE to "TRN"),
    (DigiIconType.BATTLE_SWORDS to "BTL")
)
private val BOTTOM_VPET_ICONS = listOf(
    (DigiIconType.CLEAN_FLUSH to "FLSH"),
    (DigiIconType.SLEEP_LIGHT to "LGT"),
    (DigiIconType.BANDAGE_CROSS to "MED"),
    (DigiIconType.CALL_ALERT to "CALL")
)

/**
 * Authentic Bandai V-Pet & Digivice Shell Viewport.
 * Supports authentic 1997 Brick-Wall, Digimon Color, D-3, D-Arc, and Vital Bracelet casings.
 * Includes top & bottom 4-icon LCD rails, dot-matrix screen grid, corner rivets,
 * and optional interactive [A] [B] [C] shell buttons.
 */
@Composable
fun VPetBrickShellFrame(
    modifier: Modifier = Modifier,
    shellStyle: DigiviceShellStyle = DigiviceShellStyle.ORIGINAL_1997_GREY,
    screenBgColor: Color = DigiWearPalette.LcdSageBg,
    selectedIconIndex: Int = 0,
    statusLabel: String = "DM-COLOR // VER. 1-5",
    onButtonA: (() -> Unit)? = null,
    onButtonB: (() -> Unit)? = null,
    onButtonC: (() -> Unit)? = null,
    screenHeight: Dp = 152.dp,
    content: @Composable BoxScope.() -> Unit
) {
    // Outer Shell Container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(colors = shellStyle.shellGradient))
            .border(2.dp, shellStyle.borderColor, RoundedCornerShape(20.dp))
            .drawWithCache {
                val brickColor = Color(0x22000000)
                val highlightColor = Color(0x14FFFFFF)
                val isBrick = shellStyle.isBrickTextured
                onDrawBehind {
                    if (isBrick) {
                        // Subtle brick-wall mortar lines across the shell casing
                        val rowH = 18.dp.toPx()
                        val colW = 36.dp.toPx()
                        var row = 0
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = brickColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.5f
                            )
                            drawLine(
                                color = highlightColor,
                                start = Offset(0f, y + 1.5f),
                                end = Offset(size.width, y + 1.5f),
                                strokeWidth = 1f
                            )
                            val xOffset = if (row % 2 == 0) 0f else colW / 2f
                            var x = xOffset
                            while (x < size.width) {
                                drawLine(
                                    color = brickColor,
                                    start = Offset(x, y),
                                    end = Offset(x, (y + rowH).coerceAtMost(size.height)),
                                    strokeWidth = 1.5f
                                )
                                x += colW
                            }
                            y += rowH
                            row++
                        }
                    } else {
                        // High-tech cybernetic circuit trace lines in shell corners
                        val traceColor = shellStyle.borderColor.copy(alpha = 0.25f)
                        drawLine(traceColor, Offset(16f, 16f), Offset(60f, 16f), 1.5f)
                        drawLine(traceColor, Offset(16f, 16f), Offset(16f, 60f), 1.5f)
                        drawLine(traceColor, Offset(size.width - 16f, 16f), Offset(size.width - 60f, 16f), 1.5f)
                        drawLine(traceColor, Offset(size.width - 16f, 16f), Offset(size.width - 16f, 60f), 1.5f)
                    }
                }
            }
            .padding(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Top Casing Nameplate with Corner Rivets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RivetDot(color = shellStyle.accentRivetColor)
                Text(
                    text = "[ ${shellStyle.shortName} // $statusLabel ]",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = DigiWearPalette.CrestGold,
                    fontFamily = FontFamily.Monospace
                )
                RivetDot(color = shellStyle.accentRivetColor)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inner Recessed LCD Bezel + Optional [A][B][C] Physical Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Recessed LCD Cage
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shellStyle.lcdCageColor)
                        .border(2.dp, shellStyle.borderColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(6.dp)
                ) {
                    // Top 4 V-Pet LCD Care Icons Rail
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0A100E), RoundedCornerShape(6.dp))
                            .padding(vertical = 3.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TOP_VPET_ICONS.forEachIndexed { idx, (iconType, lbl) ->
                            val isSel = selectedIconIndex == idx
                            LcdIconCell(iconType = iconType, label = lbl, isSelected = isSel)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Dot-Matrix LCD Center Viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(screenBgColor)
                            .border(1.dp, Color(0x6680CBC4), RoundedCornerShape(8.dp))
                            .drawWithCache {
                                val gridColor = Color(0x16000000)
                                val frameColor = Color(0x3380CBC4)
                                onDrawBehind {
                                    val step = 6.dp.toPx()
                                    var x = 0f
                                    while (x < size.width) {
                                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                                        x += step
                                    }
                                    var y = 0f
                                    while (y < size.height) {
                                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                                        y += step
                                    }
                                    val inset = 5.dp.toPx()
                                    drawRect(
                                        color = frameColor,
                                        topLeft = Offset(inset, inset),
                                        size = Size(size.width - inset * 2, size.height - inset * 2),
                                        style = Stroke(width = 1f)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                        content = content
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom 4 V-Pet LCD Care Icons Rail
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0A100E), RoundedCornerShape(6.dp))
                            .padding(vertical = 3.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BOTTOM_VPET_ICONS.forEachIndexed { idx, (iconType, lbl) ->
                            val isSel = selectedIconIndex == (idx + 4)
                            LcdIconCell(iconType = iconType, label = lbl, isSelected = isSel)
                        }
                    }
                }

                // Right-Side Tactile [A] [B] [C] Bandai Buttons (when callbacks provided)
                if (onButtonA != null && onButtonB != null && onButtonC != null) {
                    val (btnAColor, btnBColor, btnCColor) = shellStyle.buttonColors
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ShellRoundButton(label = "A", sub = "SEL", color = btnAColor, onClick = onButtonA)
                        ShellRoundButton(label = "B", sub = "OK", color = btnBColor, onClick = onButtonB)
                        ShellRoundButton(label = "C", sub = "ESC", color = btnCColor, onClick = onButtonC)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Casing Serial Engraving
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RivetDot(color = shellStyle.accentRivetColor)
                Text(
                    text = "D-COM / 0NL1NE LINK READY • 64x64 PX",
                    fontSize = 8.sp,
                    color = Color(0xFF90A4AE),
                    fontFamily = FontFamily.Monospace
                )
                RivetDot(color = shellStyle.accentRivetColor)
            }
        }
    }
}

@Composable
private fun RivetDot(color: Color = Color(0xFF546E7A)) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .background(Color(0xFF151C20), CircleShape)
            .border(1.dp, color, CircleShape)
    )
}

@Composable
private fun LcdIconCell(iconType: DigiIconType, label: String, isSelected: Boolean) {
    Row(
        modifier = Modifier
            .background(
                if (isSelected) Color(0x33FFD54F) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFFFFD54F) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DigiVectorIcon(
            type = iconType,
            size = 11.dp,
            tint = if (isSelected) Color(0xFFFFD54F) else Color(0xFF90A4AE)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFFFFD54F) else Color(0xFF607D8B),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ShellRoundButton(
    label: String,
    sub: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF151D24))
                .border(2.dp, color, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = sub,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF90A4AE),
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Cyber-Digivice Section Header with glowing accent bar and optional right-side badge.
 */
@Composable
fun DigiSectionHeader(
    tag: String,
    title: String,
    subtitle: String? = null,
    accentColor: Color = DigiWearPalette.DigiviceCyan,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(34.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "▸ $tag",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE)
                    )
                }
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

/**
 * Digimon Stage Pill Badge.
 */
@Composable
fun StageBadge(stageName: String) {
    Surface(
        color = Color(0xFF102428),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, Color(0xFF26A69A))
    ) {
        Text(
            text = stageName.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF80CBC4),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

/**
 * Digimon Elemental Attribute Pill Badge (Vaccine / Data / Virus / Free).
 */
@Composable
fun AttributeBadge(attributeName: String) {
    val (bg, border, textCol) = when {
        attributeName.contains("Vaccine", ignoreCase = true) ->
            Triple(Color(0xFF0D2538), Color(0xFF29B6F6), Color(0xFF81D4FA))
        attributeName.contains("Data", ignoreCase = true) ->
            Triple(Color(0xFF2E220D), Color(0xFFFFB300), Color(0xFFFFD54F))
        attributeName.contains("Virus", ignoreCase = true) ->
            Triple(Color(0xFF2A1228), Color(0xFFEC407A), Color(0xFFF48FB1))
        else ->
            Triple(Color(0xFF14281D), Color(0xFF66BB6A), Color(0xFFA5D6A7))
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = attributeName.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textCol,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

/**
 * 4-Segment LCD Block Gauge Row for Hunger, Strength, and Effort.
 */
@Composable
fun SegmentedGaugeRow(
    label: String,
    filled: Int,
    max: Int = 4,
    activeColor: Color,
    symbolFilled: String = "HEART",
    symbolEmpty: String = "EMPTY"
) {
    val safeFilled = filled.coerceIn(0, max)
    val isHearts = !symbolFilled.contains("STAR", ignoreCase = true)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFB0BEC5),
            fontFamily = FontFamily.Monospace
        )

        VPetMeterBar(
            count = safeFilled,
            max = max,
            isHearts = isHearts,
            iconSize = 12.dp,
            activeColor = activeColor
        )
    }
}
