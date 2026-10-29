package com.digiwear.wear.tile

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.digiwear.wear.complication.DigiWearComplicationService
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.haptic.HapticManager
import com.digiwear.wear.receiver.NotificationQuickCareReceiver
import com.digiwear.wear.sound.BuzzerManager
import com.digiwear.wear.worker.PetDecayWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lightweight, transparent Activity that executes instant one-tap quick care actions
 * triggered directly from Wear OS ProtoLayout Tile action chips:
 * - [ FEED ]: Feeds meal (+1 hunger, +1g weight) or vitamin (+1 strength, +2g weight).
 * - [ CLEAN / FLUSH ]: Flushes waste and resets poop hazards.
 * - [ LIGHTS ]: Toggles sleep / waking state lights.
 *
 * Runs non-blocking on Dispatchers.IO, updates Room SQLite, triggers wrist haptic feedback,
 * immediately invalidates the Tile and complications, reconciles attention notifications,
 * guarantees zero AudioTrack leaks by safely awaiting and releasing buzzer hardware,
 * and self-dismisses via finishAndRemoveTask() with zero window animation flicker.
 */
class TileQuickCareActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableActivityTransitions()

        val action = intent?.getStringExtra(EXTRA_CARE_ACTION)
            ?: intent?.action
            ?: run {
                dismissImmediately()
                return
            }

        scope.launch {
            val repo = PetRepository.getInstance(applicationContext)
            val haptic = HapticManager(applicationContext)
            val buzzer = BuzzerManager().apply {
                isEnabled = repo.soundEnabledFlow.value
                soundpack = repo.soundpackFlow.value
            }

            try {
                val current = repo.getPetState()
                if (current.isDeceased) {
                    return@launch
                }

                val updatedState = when (action) {
                    ACTION_FEED, NotificationQuickCareReceiver.ACTION_QUICK_FEED -> {
                        val state = if (current.hunger < 4) {
                            repo.feedMeal()
                        } else {
                            repo.feedProtein()
                        }
                        buzzer.playButtonBeepSuspend()
                        state
                    }
                    ACTION_CLEAN, NotificationQuickCareReceiver.ACTION_QUICK_CLEAN -> {
                        val state = repo.cleanWaste()
                        buzzer.playFlushToneSuspend()
                        state
                    }
                    ACTION_LIGHTS, NotificationQuickCareReceiver.ACTION_QUICK_LIGHTS -> {
                        val state = repo.toggleLights()
                        buzzer.playButtonBeepSuspend()
                        state
                    }
                    ACTION_MEDS, NotificationQuickCareReceiver.ACTION_QUICK_MEDS -> {
                        val state = repo.cureInjury()
                        buzzer.playButtonBeepSuspend()
                        state
                    }
                    else -> return@launch
                }

                // 1. Wrist haptic feedback
                try {
                    haptic.playStepMilestone()
                } catch (_: Exception) {}

                // 2. Immediate Tile & Complication timeline invalidation
                DigiWearTileService.requestUpdate(applicationContext)
                DigiWearComplicationService.requestUpdate(applicationContext)

                // 3. Reconcile / dismiss attention notification if pet is no longer in distress
                if (NotificationQuickCareReceiver.shouldDismissNotification(updatedState)) {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.cancel(NotificationQuickCareReceiver.NOTIFICATION_ID_ATTENTION)
                    val prefs = getSharedPreferences("digiwear_wear_prefs", Context.MODE_PRIVATE)
                    prefs.edit().remove(PetDecayWorker.KEY_LAST_NOTIFIED_CALL_UTC).apply()
                }
            } catch (_: Exception) {
            } finally {
                buzzer.release()
                withContext(Dispatchers.Main) {
                    dismissImmediately()
                }
            }
        }
    }

    private fun dismissImmediately() {
        disableActivityTransitions()
        finishAndRemoveTask()
    }

    private fun disableActivityTransitions() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        const val EXTRA_CARE_ACTION = "com.digiwear.wear.extra.CARE_ACTION"
        const val ACTION_FEED = "com.digiwear.wear.action.TILE_FEED"
        const val ACTION_CLEAN = "com.digiwear.wear.action.TILE_CLEAN"
        const val ACTION_LIGHTS = "com.digiwear.wear.action.TILE_LIGHTS"
        const val ACTION_MEDS = "com.digiwear.wear.action.TILE_MEDS"
    }
}
