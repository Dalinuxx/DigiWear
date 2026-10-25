package com.digiwear.wear.datalayer

import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.CustomThemeConfig
import com.digiwear.core.model.PetState
import com.digiwear.wear.complication.DigiWearComplicationService
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.tile.DigiWearTileService
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class WearDataSyncService : WearableListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val repo = PetRepository.getInstance(applicationContext)

        when (messageEvent.path) {
            "/sync/request_pet_state" -> {
                scope.launch {
                    val state = repo.getPetState()
                    val payload = json.encodeToString(state).toByteArray(Charsets.UTF_8)
                    Wearable.getMessageClient(applicationContext)
                        .sendMessage(messageEvent.sourceNodeId, "/sync/response_pet_state", payload)
                }
            }
            "/sync/update_pet_state" -> {
                scope.launch {
                    try {
                        val str = String(messageEvent.data, Charsets.UTF_8)
                        val incomingState = json.decodeFromString<PetState>(str)
                        if (com.digiwear.core.model.PetSpecies.SPECIES_REGISTRY.containsKey(incomingState.speciesId)) {
                            repo.savePetState(incomingState)
                        }
                    } catch (_: Exception) {}
                }
            }
            "/sync/request_settings" -> {
                scope.launch {
                    val settingsPayload = json.encodeToString(repo.getSettingsMap()).toByteArray(Charsets.UTF_8)
                    Wearable.getMessageClient(applicationContext)
                        .sendMessage(messageEvent.sourceNodeId, "/sync/response_settings", settingsPayload)
                }
            }
            "/sync/request_memorials" -> {
                scope.launch {
                    val list = repo.getMemorials()
                    val payload = json.encodeToString(list).toByteArray(Charsets.UTF_8)
                    Wearable.getMessageClient(applicationContext)
                        .sendMessage(messageEvent.sourceNodeId, "/sync/response_memorials", payload)
                }
            }
            "/sync/update_settings" -> {
                scope.launch {
                    try {
                        val str = String(messageEvent.data, Charsets.UTF_8)
                        val map = json.decodeFromString<Map<String, String>>(str)
                        map["theme"]?.let { themeId ->
                            repo.setThemeId(themeId)
                        }
                        map["vibration"]?.let { vibStr ->
                            repo.setVibrationEnabled(vibStr.toBoolean())
                        }
                        map["vpet_3button_mode"]?.let { btnStr ->
                            repo.setThreeButtonMode(btnStr.toBoolean())
                        }
                        map["vpet_button_placement"]?.let { placement ->
                            repo.setButtonPlacement(placement)
                        }
                        map["pendulum_shake_mode"]?.let { shakeStr ->
                            repo.setPendulumShakeMode(shakeStr.toBoolean())
                        }
                        map["sound_enabled"]?.let { soundStr ->
                            repo.setSoundEnabled(soundStr.toBoolean())
                        }
                        map["nfc_bridge_mode"]?.let { nfcMode ->
                            repo.setNfcBridgeMode(nfcMode)
                        }
                        map["display_shape_override"]?.let { shapeMode ->
                            repo.setDisplayShapeOverride(shapeMode)
                        }
                        map["daily_step_goal"]?.toIntOrNull()?.let { goal ->
                            repo.setDailyStepGoal(goal)
                        }
                        map["lcd_contrast_level"]?.let { level ->
                            repo.setContrastLevel(level)
                        }
                        map["sleep_schedule"]?.let { schedule ->
                            repo.setSleepSchedule(schedule)
                        }
                        map["ambient_display_style"]?.let { style ->
                            repo.setAmbientStyle(style)
                        }
                        map["hazard_alerts_enabled"]?.let { alertStr ->
                            repo.setHazardAlertsEnabled(alertStr.toBoolean())
                        }
                        map["clock_format_24h"]?.let { clockStr ->
                            repo.setClockFormat24h(clockStr.toBoolean())
                        }
                        map["lifespan_canonical_enabled"]?.let { lifeStr ->
                            repo.setLifespanCanonicalEnabled(lifeStr.toBoolean())
                        }
                        map["combat_injuries_enabled"]?.let { injStr ->
                            repo.setCombatInjuriesEnabled(injStr.toBoolean())
                        }
                        map["soundpack"]?.let { spStr ->
                            repo.setSoundpack(com.digiwear.core.model.SoundpackProfile.fromId(spStr))
                        }
                    } catch (_: Exception) {}
                }
            }
            "/sync/remote_care_action" -> {
                scope.launch {
                    try {
                        val action = String(messageEvent.data, Charsets.UTF_8).trim()
                        val updatedState = when {
                            action == "FEED_MEAL" || action == "FEED" -> repo.feedMeal()
                            action == "FEED_PROTEIN" -> repo.feedProtein()
                            action == "CLEAN_WASTE" || action == "CLEAN" -> repo.cleanWaste()
                            action == "TOGGLE_LIGHTS" -> repo.toggleLights()
                            action == "TOGGLE_COLD_SLEEP" -> repo.toggleColdSleep()
                            action == "DEPOSIT_VAULT" || action == "FREEZE_AND_DEPOSIT" -> {
                                repo.clearActivePet()
                                repo.getPetState()
                            }
                            action == "CURE_INJURY" -> repo.cureInjury()
                            action == "TRAIN_WIN" -> repo.train(won = true)
                            action.startsWith("HATCH_EGG:") -> {
                                val eggId = action.substringAfter("HATCH_EGG:").toIntOrNull()
                                    ?.takeIf { com.digiwear.core.model.PetSpecies.SPECIES_REGISTRY.containsKey(it) } ?: 101
                                repo.hatchNewEgg(eggId)
                            }
                            else -> repo.getPetState()
                        }
                        if (action == "FEED_MEAL" || action == "FEED" || action == "CLEAN_WASTE" || action == "CLEAN" || action == "TOGGLE_LIGHTS") {
                            DigiWearComplicationService.requestUpdate(applicationContext)
                            DigiWearTileService.requestUpdate(applicationContext)
                        }
                        val payload = json.encodeToString(updatedState).toByteArray(Charsets.UTF_8)
                        Wearable.getMessageClient(applicationContext)
                            .sendMessage(messageEvent.sourceNodeId, "/sync/response_pet_state", payload)
                    } catch (_: Exception) {}
                }
            }
            "/sync/battle_code" -> {
                val code = String(messageEvent.data, Charsets.UTF_8).trim()
                if (code.isNotEmpty()) {
                    repo.setBeamedCommCode(code)
                }
            }
            "/sync/campaign_data" -> {
                val campaignJson = String(messageEvent.data, Charsets.UTF_8).trim()
                if (campaignJson.isNotEmpty()) {
                    repo.saveCustomCampaignJson(campaignJson)
                    scope.launch {
                        try {
                            val parsed = com.digiwear.core.adventure.AdventureEngine.parseCampaign(campaignJson)
                            repo.switchCampaign(parsed.campaignId)
                        } catch (_: Exception) {}
                    }
                }
            }
            "/sync/upload_wallpaper" -> {
                scope.launch {
                    try {
                        val bytes = messageEvent.data
                        if (bytes.isNotEmpty()) {
                            val file = File(applicationContext.filesDir, "custom_wallpaper.png")
                            file.writeBytes(bytes)
                            repo.setThemeId(CustomThemeConfig.WALLPAPER_THEME_ID)
                            repo.notifyWallpaperUpdated()
                        }
                    } catch (_: Exception) {}
                }
            }
            else -> super.onMessageReceived(messageEvent)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val repo = PetRepository.getInstance(applicationContext)
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/custom_wallpaper") {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val asset = dataMapItem.dataMap.getAsset("wallpaper_asset")
                if (asset != null) {
                    scope.launch {
                        try {
                            val fd = Tasks.await(Wearable.getDataClient(applicationContext).getFdForAsset(asset))
                            fd?.inputStream?.use { input ->
                                val file = File(applicationContext.filesDir, "custom_wallpaper.png")
                                file.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                                repo.setThemeId(CustomThemeConfig.WALLPAPER_THEME_ID)
                                repo.notifyWallpaperUpdated()
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
