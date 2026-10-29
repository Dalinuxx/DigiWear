package com.digiwear.wear.tile

import android.content.Context
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
import com.digiwear.core.model.PetSpecies
import com.digiwear.wear.MainActivity
import com.digiwear.wear.data.PetRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Glanceable and Interactive Wear OS Tile for instant wrist access to active partner status,
 * hunger/strength hearts, adventure steps, and 1-tap quick care actions:
 * - [ FEED ]: Feeds meal or vitamin straight from the tile.
 * - [ CLEAN ]: Flushes waste and resets poop hazards.
 * - [ LIGHTS ]: Toggles sleep schedule / waking state lights.
 */
class DigiWearTileService : TileService() {

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
                val species = PetSpecies.getById(pet.speciesId)
                val campaign = repo.getActiveCampaign()
                val progress = repo.getAdventureProgress()
                val activeNode = storyNode(campaign, progress.currentNodeIndex)

                val layout = buildTileLayout(
                    context = applicationContext,
                    speciesName = species.name,
                    stageName = species.stage.displayName,
                    hunger = pet.hunger,
                    strength = pet.strength,
                    weight = pet.weight,
                    isSleeping = pet.isSleeping || pet.isFrozen,
                    activeSector = if (pet.isFrozen) "CRYO-STASIS" else (activeNode?.name ?: "File Island"),
                    currentSteps = progress.accumulatedStepsCurrentNode,
                    targetSteps = activeNode?.requiredSteps ?: 1000
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

    private fun storyNode(campaign: CampaignStory, index: Int) =
        campaign.nodes.getOrNull(index.coerceIn(0, (campaign.nodes.size - 1).coerceAtLeast(0)))

    private fun buildTileLayout(
        context: Context,
        speciesName: String,
        stageName: String,
        hunger: Int,
        strength: Int,
        weight: Int,
        isSleeping: Boolean,
        activeSector: String,
        currentSteps: Int,
        targetSteps: Int
    ): LayoutElementBuilders.Layout {
        // App launch action for general navigation
        val launchAppIntent = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(context.packageName)
                    .setClassName(MainActivity::class.java.name)
                    .build()
            )
            .build()

        val openAppModifier = ModifiersBuilders.Modifiers.Builder()
            .setClickable(
                ModifiersBuilders.Clickable.Builder()
                    .setOnClick(launchAppIntent)
                    .setId("open_app_header")
                    .build()
            )
            .build()

        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        // 1. Top Header (Clickable to open full app)
        val headerBox = LayoutElementBuilders.Box.Builder()
            .setModifiers(openAppModifier)
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText("DIGIWEAR V-PET")
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(sp(10f))
                                    .setColor(argb(0xFF00E5FF.toInt()))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText("$speciesName • $stageName")
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(sp(13f))
                                    .setColor(argb(0xFFFFFFFF.toInt()))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
        column.addContent(headerBox)

        // Spacer
        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(2f)).build())

        // 2. Physiological Meters (LCD Block Bar Style)
        val hVal = hunger.coerceIn(0, 4)
        val sVal = strength.coerceIn(0, 4)
        val hungerBar = "HGR [" + "#".repeat(hVal) + "-".repeat(4 - hVal) + "]"
        val strengthBar = "STR [" + "#".repeat(sVal) + "-".repeat(4 - sVal) + "]"

        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("$hungerBar  $strengthBar")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(10.5f))
                        .setColor(argb(0xFFFFD54F.toInt()))
                        .build()
                )
                .build()
        )

        // Weight & Sleep status
        val statusLine = if (isSleeping) "Weight: ${weight}g [ZZZ]" else "Weight: ${weight}g • $activeSector"
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(statusLine)
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(9f))
                        .setColor(argb(0xFFB0BEC5.toInt()))
                        .build()
                )
                .build()
        )

        // Spacer before Quick Care Action Buttons
        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())

        // 3. Interactive Quick Care Buttons Row: [ FEED ] [ CLEAN ] [ LIGHTS ]
        val buttonRow = LayoutElementBuilders.Row.Builder()
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)

        buttonRow.addContent(
            buildActionButton(
                context = context,
                label = "FEED",
                actionExtra = TileQuickCareActivity.ACTION_FEED,
                bgColor = 0xFF1B5E20.toInt(),
                textColor = 0xFFE8F5E9.toInt(),
                clickableId = "tile_action_feed"
            )
        )

        buttonRow.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(4f)).build())

        buttonRow.addContent(
            buildActionButton(
                context = context,
                label = "CLEAN",
                actionExtra = TileQuickCareActivity.ACTION_CLEAN,
                bgColor = 0xFF006064.toInt(),
                textColor = 0xFFE0F7FA.toInt(),
                clickableId = "tile_action_clean"
            )
        )

        buttonRow.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(4f)).build())

        buttonRow.addContent(
            buildActionButton(
                context = context,
                label = "LIGHTS",
                actionExtra = TileQuickCareActivity.ACTION_LIGHTS,
                bgColor = 0xFF4A148C.toInt(),
                textColor = 0xFFEDE7F6.toInt(),
                clickableId = "tile_action_lights"
            )
        )

        column.addContent(buttonRow.build())

        // Spacer
        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())

        // 4. Adventure Progress & Open Cue
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("Adv: $currentSteps / $targetSteps steps")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(10f))
                        .setColor(argb(0xFF00E676.toInt()))
                        .build()
                )
                .build()
        )

        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("TAP TITLE TO OPEN")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(7.5f))
                        .setColor(argb(0xFF80D8FF.toInt()))
                        .build()
                )
                .build()
        )

        return LayoutElementBuilders.Layout.Builder().setRoot(column.build()).build()
    }

    private fun buildActionButton(
        context: Context,
        label: String,
        actionExtra: String,
        bgColor: Int,
        textColor: Int,
        clickableId: String
    ): LayoutElementBuilders.LayoutElement {
        val launchIntent = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(context.packageName)
                    .setClassName(TileQuickCareActivity::class.java.name)
                    .addKeyToExtraMapping(
                        TileQuickCareActivity.EXTRA_CARE_ACTION,
                        ActionBuilders.AndroidStringExtra.Builder().setValue(actionExtra).build()
                    )
                    .build()
            )
            .build()

        val buttonModifier = ModifiersBuilders.Modifiers.Builder()
            .setClickable(
                ModifiersBuilders.Clickable.Builder()
                    .setOnClick(launchIntent)
                    .setId(clickableId)
                    .build()
            )
            .setBackground(
                ModifiersBuilders.Background.Builder()
                    .setColor(argb(bgColor))
                    .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(12f)).build())
                    .build()
            )
            .setPadding(
                ModifiersBuilders.Padding.Builder()
                    .setStart(dp(6f))
                    .setEnd(dp(6f))
                    .setTop(dp(4f))
                    .setBottom(dp(4f))
                    .build()
            )
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(dp(52f))
            .setHeight(dp(26f))
            .setModifiers(buttonModifier)
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(label)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(10f))
                            .setColor(argb(textColor))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    companion object {
        private const val RESOURCES_VERSION = "2"

        fun requestUpdate(context: Context) {
            try {
                TileService.getUpdater(context).requestUpdate(DigiWearTileService::class.java)
            } catch (_: Exception) {
            }
        }
    }
}
