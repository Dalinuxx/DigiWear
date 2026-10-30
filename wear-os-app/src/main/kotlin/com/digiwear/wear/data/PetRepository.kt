package com.digiwear.wear.data

import android.content.Context
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.digiwear.core.adventure.AdventureEngine
import com.digiwear.core.adventure.AdventureStepResult
import com.digiwear.core.engine.EvolutionEngine
import com.digiwear.core.engine.PetSimulationEngine
import com.digiwear.core.model.AdventureProgress
import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.BossEncounter
import com.digiwear.core.model.CampaignStory
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import com.digiwear.wear.complication.DigiWearComplicationService
import com.digiwear.wear.tile.DigiWearTileService
import com.digiwear.wear.worker.PetDecayWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

import com.digiwear.core.model.CustomThemeConfig
import com.digiwear.core.model.SoundpackProfile

class PetRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val stateMutex = Mutex()

    /** Single lock shared by every writer of `custom_wallpaper.png` (DataLayer + LAN server). */
    val wallpaperWriteMutex = Mutex()

    /**
     * Validates [bytes] decode as an image (bounds only, 1..2048 px per side) and atomically
     * replaces `custom_wallpaper.png` via a `.tmp` file + rename. Callers should hold [wallpaperWriteMutex].
     * @return true if the wallpaper was written.
     */
    fun writeWallpaperAtomically(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0 || opts.outWidth > 2048 || opts.outHeight > 2048) return false
        val target = java.io.File(appContext.filesDir, "custom_wallpaper.png")
        val tmp = java.io.File(appContext.filesDir, "custom_wallpaper.png.tmp")
        return try {
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(target)) {
                target.delete()
                if (!tmp.renameTo(target)) {
                    tmp.delete()
                    return false
                }
            }
            true
        } catch (_: Exception) {
            tmp.delete()
            false
        }
    }

    private val db = Room.databaseBuilder(
        appContext,
        DigiWearDatabase::class.java,
        "digiwear_wear.db"
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .apply {
            if (com.digiwear.wear.BuildConfig.DEBUG) {
                fallbackToDestructiveMigration()
            }
        }
        .build()

    private val dao = db.petDao()

    private val prefs = appContext.getSharedPreferences("digiwear_wear_prefs", Context.MODE_PRIVATE)

    private val rawStoredTheme = prefs.getString("selected_theme", BackgroundTheme.CLASSIC_VPET_LCD.id) ?: BackgroundTheme.CLASSIC_VPET_LCD.id

    private val _themeIdFlow = MutableStateFlow(rawStoredTheme)
    val themeIdFlow: StateFlow<String> = _themeIdFlow.asStateFlow()

    private val _themeFlow = MutableStateFlow(
        BackgroundTheme.fromId(rawStoredTheme)
    )
    val themeFlow: Flow<BackgroundTheme> = _themeFlow.asStateFlow()

    private val _customThemeConfigFlow = MutableStateFlow(
        CustomThemeConfig.parse(prefs.getString("custom_theme_config", null))
            ?: CustomThemeConfig.parse(rawStoredTheme)
    )
    val customThemeConfigFlow: StateFlow<CustomThemeConfig?> = _customThemeConfigFlow.asStateFlow()

    private val _wallpaperVersionFlow = MutableStateFlow(0L)
    val wallpaperVersionFlow: StateFlow<Long> = _wallpaperVersionFlow.asStateFlow()

    fun notifyWallpaperUpdated() {
        _wallpaperVersionFlow.value++
    }

    private val _hasActiveEggFlow = MutableStateFlow(
        prefs.getBoolean("has_active_egg", false)
    )
    val hasActiveEggFlow: StateFlow<Boolean> = _hasActiveEggFlow.asStateFlow()

    fun setHasActiveEgg(active: Boolean) {
        prefs.edit().putBoolean("has_active_egg", active).apply()
        _hasActiveEggFlow.value = active
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val activeEntity = dao.getPetState()
            val exists = activeEntity != null
            prefs.edit().putBoolean("has_active_egg", exists).apply()
            _hasActiveEggFlow.value = exists

            val initialDiscovered = mutableListOf<Int>()
            activeEntity?.toDomain()?.let { active ->
                initialDiscovered.add(active.speciesId)
                initialDiscovered.addAll(com.digiwear.core.engine.EvolutionEngine.findEggLineagePath(active.speciesId))
            }
            dao.getBackupPetState()?.toDomain()?.let { backup ->
                initialDiscovered.add(backup.speciesId)
                initialDiscovered.addAll(com.digiwear.core.engine.EvolutionEngine.findEggLineagePath(backup.speciesId))
            }
            dao.getAllMemorials().forEach { mem ->
                initialDiscovered.add(mem.speciesId)
            }
            recordDiscoveredSpecies(*initialDiscovered.toIntArray())
        }
    }

    private val _vibrationFlow = MutableStateFlow(
        prefs.getBoolean("vibration_enabled", true)
    )
    val vibrationFlow: Flow<Boolean> = _vibrationFlow.asStateFlow()

    private val _threeButtonModeFlow = MutableStateFlow(
        prefs.getBoolean("vpet_3button_mode", true)
    )
    val threeButtonModeFlow: Flow<Boolean> = _threeButtonModeFlow.asStateFlow()

    private val _buttonPlacementFlow = MutableStateFlow(
        prefs.getString("vpet_button_placement", "RIGHT") ?: "RIGHT"
    )
    val buttonPlacementFlow: StateFlow<String> = _buttonPlacementFlow.asStateFlow()

    private val _pendulumShakeModeFlow = MutableStateFlow(
        prefs.getBoolean("pendulum_shake_mode", false)
    )
    val pendulumShakeModeFlow: StateFlow<Boolean> = _pendulumShakeModeFlow.asStateFlow()

    private val _soundEnabledFlow = MutableStateFlow(
        prefs.getBoolean("sound_enabled", false)
    )
    val soundEnabledFlow: StateFlow<Boolean> = _soundEnabledFlow.asStateFlow()

    private val _soundpackFlow = MutableStateFlow(
        SoundpackProfile.fromId(prefs.getString("soundpack", SoundpackProfile.ORIGINAL_1997.id))
    )
    val soundpackFlow: StateFlow<SoundpackProfile> = _soundpackFlow.asStateFlow()

    private val _nfcBridgeModeFlow = MutableStateFlow(
        prefs.getString("nfc_bridge_mode", "NFC_DUEL") ?: "NFC_DUEL"
    )
    val nfcBridgeModeFlow: StateFlow<String> = _nfcBridgeModeFlow.asStateFlow()

    private val _displayShapeOverrideFlow = MutableStateFlow(
        prefs.getString("display_shape_override", "AUTO") ?: "AUTO"
    )
    val displayShapeOverrideFlow: StateFlow<String> = _displayShapeOverrideFlow.asStateFlow()

    private val _dailyStepGoalFlow = MutableStateFlow(
        prefs.getInt("daily_step_goal", 5000)
    )
    val dailyStepGoalFlow: StateFlow<Int> = _dailyStepGoalFlow.asStateFlow()

    private fun readDailyStepsFromPrefs(): Int {
        val today = java.time.LocalDate.now().toString()
        val storedDate = prefs.getString("daily_steps_date", null)
        return if (storedDate == today) {
            prefs.getInt("daily_steps_count", 0).coerceAtLeast(0)
        } else {
            0
        }
    }

    private val _dailyStepsFlow = MutableStateFlow(readDailyStepsFromPrefs())
    val dailyStepsFlow: StateFlow<Int> = _dailyStepsFlow.asStateFlow()

    fun getDailySteps(): Int {
        val current = readDailyStepsFromPrefs()
        if (_dailyStepsFlow.value != current) {
            _dailyStepsFlow.value = current
        }
        return current
    }

    private fun incrementDailySteps(delta: Int) {
        if (delta <= 0) return
        val today = java.time.LocalDate.now().toString()
        val storedDate = prefs.getString("daily_steps_date", null)
        val base = if (storedDate == today) prefs.getInt("daily_steps_count", 0).coerceAtLeast(0) else 0
        val updated = base + delta
        prefs.edit()
            .putString("daily_steps_date", today)
            .putInt("daily_steps_count", updated)
            .apply()
        _dailyStepsFlow.value = updated
    }

    private val _discoveredSpeciesFlow = MutableStateFlow<Set<Int>>(
        prefs.getString("discovered_species_csv", null)
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet()
            ?: setOf(101)
    )
    val discoveredSpeciesFlow: StateFlow<Set<Int>> = _discoveredSpeciesFlow.asStateFlow()

    fun recordDiscoveredSpecies(vararg speciesIds: Int) {
        val validIds = speciesIds.filter { it > 1 && PetSpecies.SPECIES_REGISTRY.containsKey(it) }
        if (validIds.isEmpty()) return
        val current = _discoveredSpeciesFlow.value
        val updated = current + validIds
        if (updated.size != current.size) {
            _discoveredSpeciesFlow.value = updated
            prefs.edit().putString("discovered_species_csv", updated.joinToString(",")).apply()
        }
    }

    private val _contrastLevelFlow = MutableStateFlow(
        prefs.getString("lcd_contrast_level", "MEDIUM") ?: "MEDIUM"
    )
    val contrastLevelFlow: StateFlow<String> = _contrastLevelFlow.asStateFlow()

    private val _elementContrastFlow = MutableStateFlow(
        prefs.getString("element_contrast_mode", "AUTO") ?: "AUTO"
    )
    val elementContrastFlow: StateFlow<String> = _elementContrastFlow.asStateFlow()

    private val rawStoredSleep = prefs.getString("sleep_schedule", "21:00_07:00") ?: "21:00_07:00"
    private val _sleepScheduleFlow = MutableStateFlow(rawStoredSleep)
    val sleepScheduleFlow: StateFlow<String> = _sleepScheduleFlow.asStateFlow()

    private val _customSleepScheduleFlow = MutableStateFlow<String?>(
        prefs.getString("custom_sleep_schedule", null)
            ?: if (rawStoredSleep.contains("_") && !isPresetSleepSchedule(rawStoredSleep)) rawStoredSleep else null
    )
    val customSleepScheduleFlow: StateFlow<String?> = _customSleepScheduleFlow.asStateFlow()

    private val _ambientStyleFlow = MutableStateFlow(
        prefs.getString("ambient_display_style", "MINIMAL_DOT") ?: "MINIMAL_DOT"
    )
    val ambientStyleFlow: StateFlow<String> = _ambientStyleFlow.asStateFlow()

    private val _hazardAlertsFlow = MutableStateFlow(
        prefs.getBoolean("hazard_alerts_enabled", true)
    )
    val hazardAlertsFlow: StateFlow<Boolean> = _hazardAlertsFlow.asStateFlow()

    private val _adventureOverlayFlow = MutableStateFlow(
        prefs.getBoolean("adventure_overlay_enabled", true)
    )
    val adventureOverlayFlow: StateFlow<Boolean> = _adventureOverlayFlow.asStateFlow()

    private val _clockFormatFlow = MutableStateFlow(
        prefs.getBoolean("clock_format_24h", false)
    )
    val clockFormatFlow: StateFlow<Boolean> = _clockFormatFlow.asStateFlow()

    private val _lifespanCanonicalEnabledFlow = MutableStateFlow(
        prefs.getBoolean("lifespan_canonical_enabled", false)
    )
    val lifespanCanonicalEnabledFlow: StateFlow<Boolean> = _lifespanCanonicalEnabledFlow.asStateFlow()

    private val _combatInjuriesEnabledFlow = MutableStateFlow(
        prefs.getBoolean("combat_injuries_enabled", true)
    )
    val combatInjuriesEnabledFlow: StateFlow<Boolean> = _combatInjuriesEnabledFlow.asStateFlow()

    private val _maskUndiscoveredSpeciesFlow = MutableStateFlow(
        prefs.getBoolean("mask_undiscovered_species", true)
    )
    val maskUndiscoveredSpeciesFlow: StateFlow<Boolean> = _maskUndiscoveredSpeciesFlow.asStateFlow()

    private val _beamedCommCodeFlow = MutableStateFlow<String?>(null)
    val beamedCommCodeFlow: StateFlow<String?> = _beamedCommCodeFlow.asStateFlow()

    private val _campaignJsonFlow = MutableStateFlow(
        prefs.getString("custom_campaign_json", null) ?: AdventureEngine.DEFAULT_CAMPAIGN_JSON
    )
    val campaignJsonFlow: StateFlow<String> = _campaignJsonFlow.asStateFlow()

    @Volatile
    private var cachedCampaign: Pair<String, CampaignStory>? = null

    fun setTheme(theme: BackgroundTheme) {
        if (theme == BackgroundTheme.CUSTOM_MATRIX && _customThemeConfigFlow.value != null) {
            setThemeId(_customThemeConfigFlow.value!!.toThemeId())
        } else {
            setThemeId(theme.id)
        }
    }

    fun setBackgroundTheme(theme: BackgroundTheme) {
        setTheme(theme)
    }

    fun setCustomThemeConfig(config: CustomThemeConfig) {
        setThemeId(config.toThemeId())
    }

    fun setThemeId(themeId: String) {
        val effectiveThemeId = if (themeId == BackgroundTheme.CUSTOM_MATRIX.id && _customThemeConfigFlow.value != null) {
            _customThemeConfigFlow.value!!.toThemeId()
        } else {
            themeId
        }
        prefs.edit().putString("selected_theme", effectiveThemeId).apply()
        _themeIdFlow.value = effectiveThemeId
        _themeFlow.value = BackgroundTheme.fromId(effectiveThemeId)
        val parsed = CustomThemeConfig.parse(effectiveThemeId)
        if (parsed != null) {
            prefs.edit().putString("custom_theme_config", effectiveThemeId).apply()
            _customThemeConfigFlow.value = parsed
        } else if (effectiveThemeId == "custom_matrix" && _customThemeConfigFlow.value == null) {
            val defaultConfig = CustomThemeConfig()
            _customThemeConfigFlow.value = defaultConfig
            prefs.edit().putString("custom_theme_config", defaultConfig.toThemeId()).apply()
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _vibrationFlow.value = enabled
    }

    fun setThreeButtonMode(enabled: Boolean) {
        prefs.edit().putBoolean("vpet_3button_mode", enabled).apply()
        _threeButtonModeFlow.value = enabled
    }

    fun setButtonPlacement(placement: String) {
        prefs.edit().putString("vpet_button_placement", placement).apply()
        _buttonPlacementFlow.value = placement
    }

    fun setPendulumShakeMode(enabled: Boolean) {
        prefs.edit().putBoolean("pendulum_shake_mode", enabled).apply()
        _pendulumShakeModeFlow.value = enabled
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        _soundEnabledFlow.value = enabled
    }

    fun setNfcBridgeMode(mode: String) {
        prefs.edit().putString("nfc_bridge_mode", mode).apply()
        _nfcBridgeModeFlow.value = mode
    }

    fun setDisplayShapeOverride(mode: String) {
        prefs.edit().putString("display_shape_override", mode).apply()
        _displayShapeOverrideFlow.value = mode
    }

    fun setDailyStepGoal(goal: Int) {
        prefs.edit().putInt("daily_step_goal", goal).apply()
        _dailyStepGoalFlow.value = goal
    }

    fun setContrastLevel(level: String) {
        prefs.edit().putString("lcd_contrast_level", level).apply()
        _contrastLevelFlow.value = level
    }

    fun setElementContrast(mode: String) {
        prefs.edit().putString("element_contrast_mode", mode).apply()
        _elementContrastFlow.value = mode
    }

    fun setSleepSchedule(schedule: String) {
        val sanitized = if (PetSimulationEngine.calculateSleepDurationMinutes(schedule) != null) {
            if (PetSimulationEngine.isValidSleepDuration(schedule)) schedule else "21:00_07:00"
        } else {
            schedule
        }
        prefs.edit().putString("sleep_schedule", sanitized).apply()
        _sleepScheduleFlow.value = sanitized
        if (sanitized.contains("_") && !isPresetSleepSchedule(sanitized)) {
            prefs.edit().putString("custom_sleep_schedule", sanitized).apply()
            _customSleepScheduleFlow.value = sanitized
        }
    }

    fun setAmbientStyle(style: String) {
        prefs.edit().putString("ambient_display_style", style).apply()
        _ambientStyleFlow.value = style
    }

    fun setHazardAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("hazard_alerts_enabled", enabled).apply()
        _hazardAlertsFlow.value = enabled
    }

    fun setAdventureOverlay(enabled: Boolean) {
        prefs.edit().putBoolean("adventure_overlay_enabled", enabled).apply()
        _adventureOverlayFlow.value = enabled
        notifySurfaceUpdates()
    }

    fun setClockFormat24h(is24h: Boolean) {
        prefs.edit().putBoolean("clock_format_24h", is24h).apply()
        _clockFormatFlow.value = is24h
    }

    fun setLifespanCanonicalEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("lifespan_canonical_enabled", enabled).apply()
        _lifespanCanonicalEnabledFlow.value = enabled
    }

    fun setCombatInjuriesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("combat_injuries_enabled", enabled).apply()
        _combatInjuriesEnabledFlow.value = enabled
    }

    fun setMaskUndiscoveredSpecies(masked: Boolean) {
        prefs.edit().putBoolean("mask_undiscovered_species", masked).apply()
        _maskUndiscoveredSpeciesFlow.value = masked
    }

    fun setSoundpack(soundpack: SoundpackProfile) {
        prefs.edit().putString("soundpack", soundpack.id).apply()
        _soundpackFlow.value = soundpack
    }

    fun getSettingsMap(): Map<String, String> = mapOf(
        "theme" to if (_themeFlow.value == BackgroundTheme.CUSTOM_MATRIX && _customThemeConfigFlow.value != null) {
            _customThemeConfigFlow.value!!.toThemeId()
        } else {
            _themeIdFlow.value
        },
        "vibration" to _vibrationFlow.value.toString(),
        "vpet_3button_mode" to _threeButtonModeFlow.value.toString(),
        "vpet_button_placement" to _buttonPlacementFlow.value,
        "pendulum_shake_mode" to _pendulumShakeModeFlow.value.toString(),
        "sound_enabled" to _soundEnabledFlow.value.toString(),
        "nfc_bridge_mode" to _nfcBridgeModeFlow.value,
        "display_shape_override" to _displayShapeOverrideFlow.value,
        "daily_step_goal" to _dailyStepGoalFlow.value.toString(),
        "lcd_contrast_level" to _contrastLevelFlow.value,
        "element_contrast_mode" to _elementContrastFlow.value,
        "sleep_schedule" to _sleepScheduleFlow.value,
        "ambient_display_style" to _ambientStyleFlow.value,
        "hazard_alerts_enabled" to _hazardAlertsFlow.value.toString(),
        "adventure_overlay_enabled" to _adventureOverlayFlow.value.toString(),
        "clock_format_24h" to _clockFormatFlow.value.toString(),
        "lifespan_canonical_enabled" to _lifespanCanonicalEnabledFlow.value.toString(),
        "combat_injuries_enabled" to _combatInjuriesEnabledFlow.value.toString(),
        "mask_undiscovered_species" to _maskUndiscoveredSpeciesFlow.value.toString(),
        "soundpack" to _soundpackFlow.value.id
    )

    fun setBeamedCommCode(code: String?) {
        _beamedCommCodeFlow.value = code
    }

    fun saveCustomCampaignJson(jsonStr: String) {
        val validJson = try {
            val parsed = AdventureEngine.parseCampaign(jsonStr)
            if (parsed.nodes.isNotEmpty()) {
                cachedCampaign = jsonStr to parsed
                jsonStr
            } else AdventureEngine.DEFAULT_CAMPAIGN_JSON
        } catch (e: Exception) {
            AdventureEngine.DEFAULT_CAMPAIGN_JSON
        }
        prefs.edit().putString("custom_campaign_json", validJson).apply()
        _campaignJsonFlow.value = validJson
    }

    fun getActiveCampaign(): CampaignStory {
        val rawJson = _campaignJsonFlow.value
        val existing = cachedCampaign
        if (existing != null && existing.first == rawJson) {
            return existing.second
        }
        val parsed = try {
            val c = AdventureEngine.parseCampaign(rawJson)
            if (c.nodes.isNotEmpty()) c else AdventureEngine.parseCampaign(AdventureEngine.DEFAULT_CAMPAIGN_JSON)
        } catch (e: Exception) {
            AdventureEngine.parseCampaign(AdventureEngine.DEFAULT_CAMPAIGN_JSON)
        }
        cachedCampaign = rawJson to parsed
        return parsed
    }

    val petStateFlow: Flow<PetState> = dao.getPetStateFlow().map { entity ->
        val domain = entity?.toDomain() ?: PetState(speciesId = 101)
        if (domain.speciesId <= 1 || !PetSpecies.SPECIES_REGISTRY.containsKey(domain.speciesId)) {
            domain.copy(speciesId = 101)
        } else {
            domain
        }
    }.distinctUntilChanged()

    val adventureStateFlow: Flow<AdventureProgress> = dao.getAdventureStateFlow().map { entity ->
        if (entity != null) {
            val flags = if (entity.clearedFlagsCsv.isBlank()) emptySet() else entity.clearedFlagsCsv.split(",").toSet()
            val eggs = if (entity.unlockedEggsCsv.isBlank()) emptySet() else entity.unlockedEggsCsv.split(",").mapNotNull { it.toIntOrNull() }.toSet()
            val items = if (entity.collectedItemsCsv.isBlank()) emptySet() else entity.collectedItemsCsv.split(",").filter { it.isNotBlank() }.toSet()
            val campaigns = if (entity.unlockedCampaignsCsv.isBlank()) emptySet() else entity.unlockedCampaignsCsv.split(",").filter { it.isNotBlank() }.toSet()
            AdventureProgress(
                campaignId = entity.campaignId,
                currentNodeIndex = entity.currentNodeIndex,
                highestUnlockedNodeIndex = entity.highestUnlockedNodeIndex,
                accumulatedStepsCurrentNode = entity.accumulatedStepsCurrentNode,
                totalAdventureSteps = entity.totalAdventureSteps,
                clearedFlags = flags,
                isCompleted = entity.isCompleted,
                unlockedEggs = eggs,
                collectedItems = items,
                unlockedCampaigns = campaigns,
                sagaStates = AdventureEngine.parseSagaStates(entity.sagaStatesJson)
            )
        } else {
            AdventureProgress()
        }
    }.distinctUntilChanged()

    suspend fun getPetState(): PetState = withContext(Dispatchers.IO) {
        val domain = dao.getPetState()?.toDomain() ?: PetState(speciesId = 101)
        if (domain.speciesId <= 1 || !PetSpecies.SPECIES_REGISTRY.containsKey(domain.speciesId)) {
            domain.copy(speciesId = 101)
        } else {
            domain
        }
    }

    suspend fun getAdventureProgress(): AdventureProgress = withContext(Dispatchers.IO) {
        val currentEntity = dao.getAdventureState()
        if (currentEntity != null) {
            val flags = if (currentEntity.clearedFlagsCsv.isBlank()) emptySet() else currentEntity.clearedFlagsCsv.split(",").toSet()
            val eggs = if (currentEntity.unlockedEggsCsv.isBlank()) emptySet() else currentEntity.unlockedEggsCsv.split(",").mapNotNull { it.toIntOrNull() }.toSet()
            val items = if (currentEntity.collectedItemsCsv.isBlank()) emptySet() else currentEntity.collectedItemsCsv.split(",").filter { it.isNotBlank() }.toSet()
            val campaigns = if (currentEntity.unlockedCampaignsCsv.isBlank()) emptySet() else currentEntity.unlockedCampaignsCsv.split(",").filter { it.isNotBlank() }.toSet()
            AdventureProgress(
                campaignId = currentEntity.campaignId,
                currentNodeIndex = currentEntity.currentNodeIndex,
                highestUnlockedNodeIndex = currentEntity.highestUnlockedNodeIndex,
                accumulatedStepsCurrentNode = currentEntity.accumulatedStepsCurrentNode,
                totalAdventureSteps = currentEntity.totalAdventureSteps,
                clearedFlags = flags,
                isCompleted = currentEntity.isCompleted,
                unlockedEggs = eggs,
                collectedItems = items,
                unlockedCampaigns = campaigns,
                sagaStates = AdventureEngine.parseSagaStates(currentEntity.sagaStatesJson)
            )
        } else {
            AdventureProgress()
        }
    }

    private fun notifySurfaceUpdates() {
        DigiWearComplicationService.requestUpdate(appContext)
        DigiWearTileService.requestUpdate(appContext)
        com.digiwear.wear.tile.DigiWearAdventureTileService.requestUpdate(appContext)
    }

    suspend fun savePetState(state: PetState) = withContext(Dispatchers.IO) {
        dao.insertOrUpdatePet(PetEntity.fromDomain(state))
        setHasActiveEgg(true)
        val discovered = (com.digiwear.core.engine.EvolutionEngine.findEggLineagePath(state.speciesId) + state.speciesId).toIntArray()
        recordDiscoveredSpecies(*discovered)
        notifySurfaceUpdates()
    }

    suspend fun clearActivePet() = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            dao.deleteActivePet()
            setHasActiveEgg(false)
            notifySurfaceUpdates()
        }
    }

    val backupPetStateFlow: Flow<PetState?> = dao.getBackupPetStateFlow().map { entity ->
        entity?.toDomain()
    }.distinctUntilChanged()

    suspend fun getBackupPetState(): PetState? = withContext(Dispatchers.IO) {
        dao.getBackupPetState()?.toDomain()
    }

    suspend fun saveBackupPetState(state: PetState?) = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            if (state == null) {
                dao.deleteBackupPet()
            } else {
                dao.insertOrUpdatePet(PetEntity.fromDomain(state.copy(isFrozen = true), id = 2))
                val discovered = (com.digiwear.core.engine.EvolutionEngine.findEggLineagePath(state.speciesId) + state.speciesId).toIntArray()
                recordDiscoveredSpecies(*discovered)
            }
        }
    }

    suspend fun depositActiveToBackup(): Boolean = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val active = dao.getPetState() ?: return@withContext false
            val now = System.currentTimeMillis() / 1000L
            val backup = active.copy(id = 2, isFrozen = true, lastProcessedTimestampUtc = now)
            dao.insertOrUpdatePet(backup)
            val freshEgg = PetState(speciesId = 101, lastProcessedTimestampUtc = now)
            dao.insertOrUpdatePet(PetEntity.fromDomain(freshEgg, id = 1))
            notifySurfaceUpdates()
            true
        }
    }

    suspend fun swapActiveAndBackup(): Boolean = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val active = dao.getPetState() ?: return@withContext false
            val backup = dao.getBackupPetState() ?: return@withContext false

            val now = System.currentTimeMillis() / 1000L
            val newBackup = active.copy(id = 2, isFrozen = true, lastProcessedTimestampUtc = now)
            val newActive = backup.copy(id = 1, isFrozen = false, lastProcessedTimestampUtc = now)

            dao.insertOrUpdatePet(newActive)
            dao.insertOrUpdatePet(newBackup)
            notifySurfaceUpdates()
            true
        }
    }

    suspend fun performDirectJogress(): PetState? = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val active = dao.getPetState()?.toDomain() ?: return@withContext null
            val backup = dao.getBackupPetState()?.toDomain() ?: return@withContext null

            val now = System.currentTimeMillis() / 1000L
            // JogressEngine.executeFusion throws for deceased pets or missing recipes; guard instead of crashing.
            if (active.isDeceased) return@withContext null
            if (com.digiwear.core.fusion.JogressEngine.findFusionRecipe(active.speciesId, backup.speciesId) == null) return@withContext null
            val fused = runCatching {
                com.digiwear.core.fusion.JogressEngine.executeFusion(
                    primary = active,
                    secondarySpeciesId = backup.speciesId,
                    secondaryVictories = backup.victories,
                    secondaryEffort = backup.effort,
                    currentTimestampUtc = now
                )
            }.getOrNull() ?: return@withContext null
            if (fused.speciesId == active.speciesId) return@withContext null

            val evolved = fused.copy(
                isFrozen = false,
                traitedEggBonus = active.traitedEggBonus + backup.traitedEggBonus + 1
            )

            dao.insertOrUpdatePet(PetEntity.fromDomain(evolved, id = 1))
            dao.deleteBackupPet()
            val discovered = (com.digiwear.core.engine.EvolutionEngine.findEggLineagePath(evolved.speciesId) + evolved.speciesId).toIntArray()
            recordDiscoveredSpecies(*discovered)
            notifySurfaceUpdates()
            evolved
        }
    }

    suspend fun performRemoteJogress(
        secondarySpeciesId: Int,
        secondaryVictories: Int = 15,
        secondaryEffort: Int = 4
    ): PetState? = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            if (dao.getPetState() == null) return@withContext null
            val now = System.currentTimeMillis() / 1000L
            // Apply pending decay first (same as feedMeal) so we never fuse a stale/now-deceased pet.
            val active = processDecayLocked(now)
            if (active.isDeceased) return@withContext null
            val resolvedPartnerId = if (com.digiwear.core.fusion.JogressEngine.findFusionRecipe(active.speciesId, secondarySpeciesId) != null) {
                secondarySpeciesId
            } else {
                val byTarget = com.digiwear.core.fusion.JogressEngine.getCompatibleRecipesFor(active.speciesId)
                    .firstOrNull { it.targetSpeciesId == secondarySpeciesId }
                if (byTarget != null) {
                    if (byTarget.primarySpeciesId == active.speciesId) byTarget.secondarySpeciesId else byTarget.primarySpeciesId
                } else {
                    return@withContext null
                }
            }
            if (com.digiwear.core.fusion.JogressEngine.findFusionRecipe(active.speciesId, resolvedPartnerId) == null) return@withContext null
            val fused = runCatching {
                com.digiwear.core.fusion.JogressEngine.executeFusion(
                    primary = active,
                    secondarySpeciesId = resolvedPartnerId,
                    secondaryVictories = secondaryVictories,
                    secondaryEffort = secondaryEffort,
                    currentTimestampUtc = now
                )
            }.getOrNull() ?: return@withContext null
            if (fused.speciesId == active.speciesId) return@withContext null

            val evolved = fused.copy(isFrozen = false)
            dao.insertOrUpdatePet(PetEntity.fromDomain(evolved, id = 1))
            val discovered = (com.digiwear.core.engine.EvolutionEngine.findEggLineagePath(evolved.speciesId) + listOf(resolvedPartnerId, evolved.speciesId)).toIntArray()
            recordDiscoveredSpecies(*discovered)
            notifySurfaceUpdates()
            evolved
        }
    }

    val memorialsFlow: Flow<List<com.digiwear.core.model.MemorialRecord>> = dao.getAllMemorialsFlow().map { list ->
        list.map { it.toDomain() }
    }.distinctUntilChanged()

    suspend fun getMemorials(): List<com.digiwear.core.model.MemorialRecord> = withContext(Dispatchers.IO) {
        dao.getAllMemorials().map { it.toDomain() }
    }

    suspend fun recordMemorial(record: com.digiwear.core.model.MemorialRecord): Long = withContext(Dispatchers.IO) {
        recordDiscoveredSpecies(record.speciesId)
        dao.insertMemorial(MemorialEntity.fromDomain(record))
    }

    /**
     * Resets current state to hatch a new Digi-Egg.
     */
    suspend fun hatchNewEgg(starterSpeciesId: Int = 101): PetState = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val species = PetSpecies.getById(starterSpeciesId)
            val fresh = PetState(
                speciesId = starterSpeciesId,
                currentHp = species.baseHp,
                maxHp = species.baseHp,
                hunger = 4,
                strength = 4,
                weight = species.baselineWeight,
                ageSeconds = 0L,
                careMistakes = 0,
                effort = 0,
                overfeedCount = 0,
                totalBattles = 0,
                victories = 0,
                wasteCount = 0,
                isSleeping = false,
                lightsOff = false,
                isInjured = false,
                lastProcessedTimestampUtc = System.currentTimeMillis() / 1000L,
                attentionCallActive = false,
                attentionCallTimestampUtc = null
            )
            savePetState(fresh)
            fresh
        }
    }

    /**
     * Hatches a Generation N+1 Traited Digitama inheriting stat and lifespan bonuses from the departed partner.
     */
    suspend fun hatchTraitedEgg(starterSpeciesId: Int = 101): PetState = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val current = getPetState()
            val memorial = PetSimulationEngine.createMemorialRecord(current)
            dao.insertMemorial(MemorialEntity.fromDomain(memorial))
            val nextPet = PetSimulationEngine.createTraitedEggState(current, starterSpeciesId)
            savePetState(nextPet)
            nextPet
        }
    }

    private suspend fun processDecayLocked(currentUtc: Long): PetState {
        val entity = dao.getPetState()
        if (entity == null || !_hasActiveEggFlow.value) {
            return entity?.toDomain() ?: PetState(speciesId = 101)
        }
        val current = entity.toDomain()
        val isCanonical = _lifespanCanonicalEnabledFlow.value
        val localOffset = ZoneId.systemDefault().rules.getOffset(Instant.ofEpochSecond(currentUtc))
        val sleepMode = _sleepScheduleFlow.value
        val updated = PetSimulationEngine.processDelta(
            currentState = current,
            currentTimestampUtc = currentUtc,
            timeZoneOffset = localOffset,
            sleepScheduleMode = sleepMode,
            isCanonicalLifespanEnabled = isCanonical
        )
        if (updated.isDeceased && !current.isDeceased) {
            val memorial = PetSimulationEngine.createMemorialRecord(updated)
            dao.insertMemorial(MemorialEntity.fromDomain(memorial))
        }
        val evolved = if (!updated.isDeceased) EvolutionEngine.evaluateEvolution(updated) else null
        val finalState = if (evolved != null) {
            EvolutionEngine.evolve(updated, evolved, currentUtc)
        } else {
            updated
        }

        val biologicalStateChanged =
            finalState.speciesId != current.speciesId ||
            finalState.hunger != current.hunger ||
            finalState.strength != current.strength ||
            finalState.wasteCount != current.wasteCount ||
            finalState.careMistakes != current.careMistakes ||
            finalState.isSleeping != current.isSleeping ||
            finalState.lightsOff != current.lightsOff ||
            finalState.isInjured != current.isInjured ||
            finalState.isDeceased != current.isDeceased ||
            finalState.attentionCallActive != current.attentionCallActive ||
            finalState.attentionCallTimestampUtc != current.attentionCallTimestampUtc

        val checkpointDue = current.lastProcessedTimestampUtc <= 0L ||
            (currentUtc - current.lastProcessedTimestampUtc) >= 60L

        if (biologicalStateChanged || checkpointDue) {
            savePetState(finalState)
        }
        return finalState
    }

    /**
     * Processes time-delta decay based on UTC timestamps.
     * Coalesces SQLite writes so flash storage is only written when physiological metrics change
     * or at least 60 seconds have elapsed since the last persisted checkpoint.
     */
    suspend fun processDecay(currentUtc: Long = System.currentTimeMillis() / 1000L): PetState = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            processDecayLocked(currentUtc)
        }
    }

    suspend fun feedMeal(currentUtc: Long = System.currentTimeMillis() / 1000L): PetState = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val current = processDecayLocked(currentUtc)
            val updated = PetSimulationEngine.feedMeal(current, currentUtc)
            savePetState(updated)
            updated
        }
    }

    suspend fun feedProtein(currentUtc: Long = System.currentTimeMillis() / 1000L): PetState = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val current = processDecayLocked(currentUtc)
            val updated = PetSimulationEngine.feedProtein(current, currentUtc)
            savePetState(updated)
            updated
        }
    }

    suspend fun cleanWaste(currentUtc: Long = System.currentTimeMillis() / 1000L): PetState = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val current = processDecayLocked(currentUtc)
            val updated = PetSimulationEngine.cleanWaste(current, currentUtc)
            savePetState(updated)
            updated
        }
    }

    suspend fun toggleColdSleep(currentUtc: Long = System.currentTimeMillis() / 1000L): PetState = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val current = processDecayLocked(currentUtc)
            val updated = PetSimulationEngine.toggleColdSleep(current, currentUtc)
            savePetState(updated)
            updated
        }
    }

    suspend fun toggleLights(currentUtc: Long = System.currentTimeMillis() / 1000L): PetState = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val current = processDecayLocked(currentUtc)
            val updated = PetSimulationEngine.toggleLights(current, currentUtc)
            savePetState(updated)
            if (updated.lightsOff) {
                try {
                    val nm = appContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                    nm?.cancel(1001) // NOTIFICATION_ID_ATTENTION
                    prefs.edit().remove("last_notified_call_utc").apply()
                } catch (_: Exception) {}
            }
            updated
        }
    }

    suspend fun train(won: Boolean, currentUtc: Long = System.currentTimeMillis() / 1000L): PetState = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val current = processDecayLocked(currentUtc)
            val updated = PetSimulationEngine.train(current, won, currentUtc)
            savePetState(updated)
            updated
        }
    }

    suspend fun cureInjury(currentUtc: Long = System.currentTimeMillis() / 1000L): PetState = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val current = processDecayLocked(currentUtc)
            val updated = PetSimulationEngine.cureInjury(current, currentUtc)
            savePetState(updated)
            updated
        }
    }

    suspend fun recordBattleResult(won: Boolean, currentUtc: Long = System.currentTimeMillis() / 1000L): Pair<PetState, Boolean> = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val current = processDecayLocked(currentUtc)
            val (updated, wasInjured) = PetSimulationEngine.processBattleOutcome(
                state = current,
                won = won,
                combatInjuriesEnabled = _combatInjuriesEnabledFlow.value
            )
            val evolved = EvolutionEngine.evaluateEvolution(updated)
            val finalState = if (evolved != null) {
                EvolutionEngine.evolve(updated, evolved, currentUtc)
            } else {
                updated
            }
            savePetState(finalState)
            Pair(finalState, wasInjured)
        }
    }

    suspend fun addAdventureSteps(steps: Int): AdventureStepResult = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            incrementDailySteps(steps)
            val story = getActiveCampaign()
            val progress = getAdventureProgress()
            val result = AdventureEngine.processSteps(story, progress, steps)
            val updated = result.updatedProgress
            dao.insertOrUpdateAdventure(
                AdventureEntity(
                    id = 1,
                    campaignId = updated.campaignId,
                    currentNodeIndex = updated.currentNodeIndex,
                    highestUnlockedNodeIndex = updated.highestUnlockedNodeIndex,
                    accumulatedStepsCurrentNode = updated.accumulatedStepsCurrentNode,
                    totalAdventureSteps = updated.totalAdventureSteps,
                    clearedFlagsCsv = updated.clearedFlags.joinToString(","),
                    isCompleted = updated.isCompleted,
                    unlockedEggsCsv = updated.unlockedEggs.joinToString(","),
                    collectedItemsCsv = updated.collectedItems.joinToString(","),
                    unlockedCampaignsCsv = updated.unlockedCampaigns.joinToString(","),
                    sagaStatesJson = AdventureEngine.serializeSagaStates(updated.sagaStates)
                )
            )
            notifySurfaceUpdates()
            result
        }
    }

    suspend fun recordBossVictory(boss: BossEncounter): AdventureProgress = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            recordDiscoveredSpecies(boss.bossMonsterId)
            val story = getActiveCampaign()
            val progress = getAdventureProgress()
            val updated = AdventureEngine.recordBossVictory(story, progress, boss)
            dao.insertOrUpdateAdventure(
                AdventureEntity(
                    id = 1,
                    campaignId = updated.campaignId,
                    currentNodeIndex = updated.currentNodeIndex,
                    highestUnlockedNodeIndex = updated.highestUnlockedNodeIndex,
                    accumulatedStepsCurrentNode = updated.accumulatedStepsCurrentNode,
                    totalAdventureSteps = updated.totalAdventureSteps,
                    clearedFlagsCsv = updated.clearedFlags.joinToString(","),
                    isCompleted = updated.isCompleted,
                    unlockedEggsCsv = updated.unlockedEggs.joinToString(","),
                    collectedItemsCsv = updated.collectedItems.joinToString(","),
                    unlockedCampaignsCsv = updated.unlockedCampaigns.joinToString(","),
                    sagaStatesJson = AdventureEngine.serializeSagaStates(updated.sagaStates)
                )
            )
            notifySurfaceUpdates()
            updated
        }
    }

    /**
     * Switches the active campaign on the watch with seamless progress preservation.
     * Snapshots the previous campaign's progress into [sagaStates] and restores
     * the target campaign's progress (or starts at node 0 if first time).
     */
    suspend fun switchCampaign(campaignId: String) = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val builtIn = AdventureEngine.getCampaignById(campaignId)
            val targetStory = builtIn ?: getActiveCampaign()
            val json = if (builtIn != null) {
                AdventureEngine.serializeCampaign(builtIn)
            } else {
                AdventureEngine.DEFAULT_CAMPAIGN_JSON
            }
            saveCustomCampaignJson(json)
            val currentProgress = getAdventureProgress()
            val updated = AdventureEngine.switchCampaign(currentProgress, targetStory)
            dao.insertOrUpdateAdventure(
                AdventureEntity(
                    id = 1,
                    campaignId = updated.campaignId,
                    currentNodeIndex = updated.currentNodeIndex,
                    highestUnlockedNodeIndex = updated.highestUnlockedNodeIndex,
                    accumulatedStepsCurrentNode = updated.accumulatedStepsCurrentNode,
                    totalAdventureSteps = updated.totalAdventureSteps,
                    clearedFlagsCsv = updated.clearedFlags.joinToString(","),
                    isCompleted = updated.isCompleted,
                    unlockedEggsCsv = updated.unlockedEggs.joinToString(","),
                    collectedItemsCsv = updated.collectedItems.joinToString(","),
                    unlockedCampaignsCsv = updated.unlockedCampaigns.joinToString(","),
                    sagaStatesJson = AdventureEngine.serializeSagaStates(updated.sagaStates)
                )
            )
            notifySurfaceUpdates()
        }
    }

    /**
     * Selects an unlocked sector ([targetIndex] between 0 and [highestUnlockedNodeIndex])
     * allowing the player to replay earlier stages or return to their active frontline.
     */
    suspend fun selectAdventureNode(nodeIndex: Int): AdventureProgress = withContext(Dispatchers.IO) {
        stateMutex.withLock {
            val currentProgress = getAdventureProgress()
            val story = getActiveCampaign()
            val updated = AdventureEngine.selectNode(currentProgress, nodeIndex, story)
            dao.insertOrUpdateAdventure(
                AdventureEntity(
                    id = 1,
                    campaignId = updated.campaignId,
                    currentNodeIndex = updated.currentNodeIndex,
                    highestUnlockedNodeIndex = updated.highestUnlockedNodeIndex,
                    accumulatedStepsCurrentNode = updated.accumulatedStepsCurrentNode,
                    totalAdventureSteps = updated.totalAdventureSteps,
                    clearedFlagsCsv = updated.clearedFlags.joinToString(","),
                    isCompleted = updated.isCompleted,
                    unlockedEggsCsv = updated.unlockedEggs.joinToString(","),
                    collectedItemsCsv = updated.collectedItems.joinToString(","),
                    unlockedCampaignsCsv = updated.unlockedCampaigns.joinToString(","),
                    sagaStatesJson = AdventureEngine.serializeSagaStates(updated.sagaStates)
                )
            )
            notifySurfaceUpdates()
            updated
        }
    }

    /**
     * Enqueues periodic WorkManager worker (every 15 minutes) for low-power background decay calculations.
     */
    fun scheduleBackgroundWorkers() {
        val workRequest = PeriodicWorkRequestBuilder<PetDecayWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.NONE)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            "DigiWearPetDecayWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        @Volatile
        private var instance: PetRepository? = null

        fun getInstance(context: Context): PetRepository {
            return instance ?: synchronized(this) {
                instance ?: PetRepository(context.applicationContext).also {
                    instance = it
                    CoroutineScope(Dispatchers.IO).launch {
                        it.scheduleBackgroundWorkers()
                    }
                }
            }
        }

        fun isPresetSleepSchedule(sched: String?): Boolean =
            sched == "20:00_07:00" || sched == "21:00_07:00" || sched == "22:00_08:00" || sched == "23:00_08:00"
    }
}
