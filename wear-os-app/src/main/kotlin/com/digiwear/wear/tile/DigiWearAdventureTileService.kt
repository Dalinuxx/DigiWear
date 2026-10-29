package com.digiwear.wear.tile

import android.content.Context
import android.content.Intent
import androidx.concurrent.futures.ResolvableFuture
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.digiwear.core.model.CampaignStory
import com.digiwear.wear.MainActivity
import com.digiwear.wear.data.PetRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Dedicated Wear OS Tile providing glanceable wrist radar progress:
 * - Current Sector node & Environmental hazard radar alert.
 * - Live step distance remaining to the sector apex boss.
 * - 1-tap shortcut to launch directly into Adventure Screen / Boss Combat.
 */
class DigiWearAdventureTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val future = ResolvableFuture.create<TileBuilders.Tile>()
        scope.launch {
            try {
                val repo = PetRepository.getInstance(applicationContext)
                val pet = repo.getPetState()
                val campaign = repo.getActiveCampaign()
                val progress = repo.getAdventureProgress()
                val totalNodes = campaign.nodes.size.coerceAtLeast(1)
                val nodeIdx = progress.currentNodeIndex.coerceIn(0, totalNodes - 1)
                val activeNode = campaign.nodes.getOrNull(nodeIdx)

                val sectorTitle = if (pet.isFrozen) "CRYO-STASIS" else (activeNode?.name ?: "Digital World")
                val campaignTitle = campaign.title.take(18)
                val currentSteps = progress.accumulatedStepsCurrentNode
                val targetSteps = (activeNode?.requiredSteps ?: 1000).coerceAtLeast(1)
                val hazard = activeNode?.environmentEffect?.takeIf { !it.equals("clear", ignoreCase = true) }?.uppercase()

                val layout = buildAdventureTileLayout(
                    context = applicationContext,
                    campaignTitle = campaignTitle,
                    sectorIndex = nodeIdx + 1,
                    totalSectors = totalNodes,
                    sectorName = sectorTitle,
                    currentSteps = currentSteps,
                    targetSteps = targetSteps,
                    hazardName = hazard,
                    isFrozen = pet.isFrozen
                )

                val tile = TileBuilders.Tile.Builder()
                    .setResourcesVersion(RESOURCES_VERSION)
                    .setTileTimeline(
                        TimelineBuilders.Timeline.Builder()
                            .addTimelineEntry(
                                TimelineBuilders.TimelineEntry.Builder()
                                    .setLayout(layout)
                                    .build()
                            )
                            .build()
                    )
                    .build()

                future.set(tile)
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val future = ResolvableFuture.create<ResourceBuilders.Resources>()
        val res = ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
        future.set(res)
        return future
    }

    private fun buildAdventureTileLayout(
        context: Context,
        campaignTitle: String,
        sectorIndex: Int,
        totalSectors: Int,
        sectorName: String,
        currentSteps: Int,
        targetSteps: Int,
        hazardName: String?,
        isFrozen: Boolean
    ): LayoutElementBuilders.Layout {
        // App launch action to go straight to Adventure Screen
        val launchAdventureIntent = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(context.packageName)
                    .setClassName(MainActivity::class.java.name)
                    .build()
            )
            .build()

        val openAdventureModifier = ModifiersBuilders.Modifiers.Builder()
            .setClickable(
                ModifiersBuilders.Clickable.Builder()
                    .setOnClick(launchAdventureIntent)
                    .setId("open_adventure_tile")
                    .build()
            )
            .build()

        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(openAdventureModifier)

        // 1. Top Header: Cyber cyan tracker title
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("RADAR // SAGA EXPEDITION")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(10f))
                        .setColor(argb(0xFF00E5FF.toInt()))
                        .build()
                )
                .build()
        )

        // 2. Saga title & sector index
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(if (isFrozen) "// CRYO-STASIS" else "$campaignTitle • S$sectorIndex/$totalSectors")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(11f))
                        .setColor(argb(0xFFB0BEC5.toInt()))
                        .build()
                )
                .build()
        )

        column.addContent(
            LayoutElementBuilders.Spacer.Builder()
                .setHeight(dp(4f))
                .build()
        )

        // 3. Sector Name (Bold high contrast)
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(sectorName)
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(14f))
                        .setColor(argb(0xFFFFFFFF.toInt()))
                        .build()
                )
                .build()
        )

        // 4. Hazard alert or Radar status
        val isBossReady = currentSteps >= targetSteps && !isFrozen
        val statusText = when {
            isFrozen -> "STASIS SLEEP"
            isBossReady -> "⚡ BOSS ENCOUNTER READY ⚡"
            hazardName != null -> "HAZARD: $hazardName"
            else -> "SECTOR SCANNING"
        }
        val statusColor = when {
            isBossReady -> 0xFFFFD600.toInt()
            hazardName != null -> 0xFFFF7043.toInt()
            else -> 0xFF4DB6AC.toInt()
        }

        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(statusText)
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(10f))
                        .setColor(argb(statusColor))
                        .build()
                )
                .build()
        )

        column.addContent(
            LayoutElementBuilders.Spacer.Builder()
                .setHeight(dp(6f))
                .build()
        )

        // 5. Step numbers: Current / Target
        val stepPercent = if (targetSteps > 0) ((currentSteps.toFloat() / targetSteps.toFloat()) * 100).toInt().coerceAtMost(100) else 0
        val stepDisplay = if (isFrozen) "-- / --" else "$currentSteps / $targetSteps"

        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(stepDisplay)
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(15f))
                        .setColor(argb(if (isBossReady) 0xFFFFD600.toInt() else 0xFFE0F2F1.toInt()))
                        .build()
                )
                .build()
        )

        column.addContent(
            LayoutElementBuilders.Spacer.Builder()
                .setHeight(dp(4f))
                .build()
        )

        // 6. Action button: "⚔️ FIGHT BOSS" or "▶ ADVENTURE"
        val btnLabel = when {
            isBossReady -> "⚔️ FIGHT BOSS"
            isFrozen -> "WAKE PARTNER"
            else -> "▶ EXPEDITION ($stepPercent%)"
        }
        val btnBg = when {
            isBossReady -> 0xFFD50000.toInt()
            isFrozen -> 0xFF37474F.toInt()
            else -> 0xFF00695C.toInt()
        }
        val btnText = if (isBossReady) 0xFFFFEB3B.toInt() else 0xFFFFFFFF.toInt()

        val buttonBox = LayoutElementBuilders.Box.Builder()
            .setWidth(dp(132f))
            .setHeight(dp(26f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(btnBg))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(13f)).build())
                            .build()
                    )
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(btnLabel)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(10f))
                            .setColor(argb(btnText))
                            .build()
                    )
                    .build()
            )
            .build()

        column.addContent(buttonBox)

        return LayoutElementBuilders.Layout.Builder()
            .setRoot(column.build())
            .build()
    }

    companion object {
        private const val RESOURCES_VERSION = "1"

        fun requestUpdate(context: Context) {
            try {
                TileService.getUpdater(context).requestUpdate(DigiWearAdventureTileService::class.java)
            } catch (_: Exception) {
            }
        }
    }
}
