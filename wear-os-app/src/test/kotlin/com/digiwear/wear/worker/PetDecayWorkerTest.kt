package com.digiwear.wear.worker

import android.content.SharedPreferences
import com.digiwear.core.engine.EvolutionAlertEngine
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test verifying PetDecayWorker evolution detection, constants, and SharedPreferences idempotency suppression.
 * Uses a pure in-memory SharedPreferences implementation requiring zero Robolectric overhead.
 */
class PetDecayWorkerTest {

    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
    }

    @Test
    fun testWorkerEvolutionDetectionAndIdempotentPrefWrite() {
        val initialSpeciesId = 201
        val finalSpeciesId = 301
        val prefKey = EvolutionAlertEngine.KEY_LAST_NOTIFIED_SPECIES_ID

        // 1. Initial state: pref is unset (-1)
        assertEquals(-1, fakePrefs.getInt(prefKey, -1))

        // 2. Evolution detected
        val lastNotified = fakePrefs.getInt(prefKey, -1)
        val shouldNotify = EvolutionAlertEngine.shouldNotifyEvolution(
            lastNotifiedSpeciesId = lastNotified,
            initialSpeciesId = initialSpeciesId,
            finalSpeciesId = finalSpeciesId
        )
        assertTrue("First evolution should notify", shouldNotify)

        // 3. Worker writes to SharedPreferences
        fakePrefs.edit().putInt(prefKey, finalSpeciesId).apply()
        assertEquals(301, fakePrefs.getInt(prefKey, -1))

        // 4. Subsequent Worker cycle 15 min later: initial=301, final=301
        val shouldNotifyCycle2 = EvolutionAlertEngine.shouldNotifyEvolution(
            lastNotifiedSpeciesId = fakePrefs.getInt(prefKey, -1),
            initialSpeciesId = 301,
            finalSpeciesId = 301
        )
        assertFalse("Subsequent cycle must be suppressed", shouldNotifyCycle2)

        // 5. Worker retry: initial=201, final=301, but pref is already 301
        val shouldNotifyRetry = EvolutionAlertEngine.shouldNotifyEvolution(
            lastNotifiedSpeciesId = fakePrefs.getInt(prefKey, -1),
            initialSpeciesId = initialSpeciesId,
            finalSpeciesId = finalSpeciesId
        )
        assertFalse("Retry with existing pref must be suppressed", shouldNotifyRetry)
    }

    @Test
    fun testWorkerConstantsIntegrity() {
        assertEquals("pet_attention_channel", PetDecayWorker.CHANNEL_ID_ATTENTION)
        assertEquals("pet_evolution_channel", PetDecayWorker.CHANNEL_ID_EVOLUTION)
        assertEquals(1001, PetDecayWorker.NOTIFICATION_ID_ATTENTION)
        assertEquals(1002, PetDecayWorker.NOTIFICATION_ID_EVOLUTION)
        assertEquals("last_notified_call_utc", PetDecayWorker.KEY_LAST_NOTIFIED_CALL_UTC)
        assertEquals("last_notified_species_id", PetDecayWorker.KEY_LAST_NOTIFIED_SPECIES_ID)
        assertEquals("EXTRA_EVOLVED_SPECIES_ID", PetDecayWorker.EXTRA_EVOLVED_SPECIES_ID)

        // Cross-module constant alignment
        assertEquals(EvolutionAlertEngine.CHANNEL_ID_EVOLUTION, PetDecayWorker.CHANNEL_ID_EVOLUTION)
        assertEquals(EvolutionAlertEngine.NOTIFICATION_ID_EVOLUTION, PetDecayWorker.NOTIFICATION_ID_EVOLUTION)
        assertEquals(EvolutionAlertEngine.KEY_LAST_NOTIFIED_SPECIES_ID, PetDecayWorker.KEY_LAST_NOTIFIED_SPECIES_ID)
    }

    @Test
    fun testDistressEvaluationModes() {
        val healthyState = PetState(hunger = 4, strength = 4, wasteCount = 0, isInjured = false, attentionCallActive = false)
        assertFalse("Healthy pet must not trigger distress", PetDecayWorker.evaluateDistress(healthyState))

        val starvingState = healthyState.copy(hunger = 0)
        assertTrue("Starving pet must trigger distress", PetDecayWorker.evaluateDistress(starvingState))

        val dirtyState = healthyState.copy(wasteCount = 4)
        assertTrue("4 waste units must trigger distress", PetDecayWorker.evaluateDistress(dirtyState))

        val injuredState = healthyState.copy(isInjured = true)
        assertTrue("Injured pet must trigger distress", PetDecayWorker.evaluateDistress(injuredState))

        val attentionState = healthyState.copy(attentionCallActive = true)
        assertTrue("Active attention call must trigger distress", PetDecayWorker.evaluateDistress(attentionState))

        val lightsOffDistress = starvingState.copy(lightsOff = true)
        assertFalse("Lights off must suppress distress alert", PetDecayWorker.evaluateDistress(lightsOffDistress))
    }

    @Test
    fun testDynamicDistressMessageFormatting() {
        val agumon = "Agumon"

        // Single causes
        val starving = PetState(hunger = 0)
        assertEquals("Agumon is starving!", PetDecayWorker.formatDistressMessage(agumon, starving))

        val dirty = PetState(wasteCount = 4)
        assertEquals("Agumon is surrounded by poop!", PetDecayWorker.formatDistressMessage(agumon, dirty))

        val injured = PetState(isInjured = true)
        assertEquals("Agumon is injured!", PetDecayWorker.formatDistressMessage(agumon, injured))

        // Combined causes
        val starvingAndDirty = PetState(hunger = 0, wasteCount = 4)
        assertEquals("Agumon is starving and surrounded by poop!", PetDecayWorker.formatDistressMessage(agumon, starvingAndDirty))

        val allDistress = PetState(hunger = 0, wasteCount = 4, isInjured = true)
        assertEquals("Agumon is starving, surrounded by poop, and injured!", PetDecayWorker.formatDistressMessage(agumon, allDistress))

        // Fallback
        val activeCallOnly = PetState(attentionCallActive = true)
        assertEquals("Agumon requires immediate attention!", PetDecayWorker.formatDistressMessage(agumon, activeCallOnly))
    }

    @Test
    fun testQuickCareActionConstants() {
        assertEquals("com.digiwear.wear.action.QUICK_FEED", PetDecayWorker.ACTION_QUICK_FEED)
        assertEquals("com.digiwear.wear.action.QUICK_CLEAN", PetDecayWorker.ACTION_QUICK_CLEAN)
        assertEquals(101, PetDecayWorker.REQUEST_CODE_FEED)
        assertEquals(102, PetDecayWorker.REQUEST_CODE_CLEAN)
    }

    /**
     * Lightweight in-memory SharedPreferences implementation for fast JVM unit tests.
     */
    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = map
        override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (map[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val backingMap: MutableMap<String, Any>) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { temp[key ?: ""] = values; return this }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { temp[key ?: ""] = value; return this }
            override fun remove(key: String?): SharedPreferences.Editor { temp[key ?: ""] = null; return this }
            override fun clear(): SharedPreferences.Editor { clearRequested = true; return this }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clearRequested) backingMap.clear()
                for ((k, v) in temp) {
                    if (v == null) backingMap.remove(k) else backingMap[k] = v
                }
            }
        }
    }
}
