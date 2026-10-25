package com.digiwear.wear.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import com.digiwear.core.model.SpriteFrame
import com.digiwear.wear.MainActivity
import com.digiwear.wear.complication.DigiWearComplicationService
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.receiver.NotificationQuickCareReceiver
import com.digiwear.wear.render.PixelArtMatrices
import com.digiwear.wear.sound.BuzzerManager
import com.digiwear.wear.tile.DigiWearTileService

class PetDecayWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = PetRepository.getInstance(context)
        if (!repo.hasActiveEggFlow.value) {
            return Result.success()
        }
        val now = System.currentTimeMillis() / 1000L
        val initialPetState = repo.getPetState()
        val initialSpeciesId = initialPetState.speciesId
        val finalState = repo.processDecay(now)
        val finalSpeciesId = finalState.speciesId
        val prefs = context.getSharedPreferences("digiwear_wear_prefs", Context.MODE_PRIVATE)

        // Surface Invalidation: Update complications and tiles with latest state
        DigiWearComplicationService.requestUpdate(context)
        DigiWearTileService.requestUpdate(context)

        // 1. Digital Evolution Detection & Notification
        if (initialSpeciesId > 0 && finalSpeciesId != initialSpeciesId) {
            val lastNotifiedSpecies = prefs.getInt(KEY_LAST_NOTIFIED_SPECIES_ID, -1)
            if (lastNotifiedSpecies != finalSpeciesId) {
                prefs.edit().putInt(KEY_LAST_NOTIFIED_SPECIES_ID, finalSpeciesId).apply()
                if (prefs.getBoolean("vibration_enabled", true)) {
                    triggerEvolutionVibrationAlert()
                }
                if (prefs.getBoolean("sound_enabled", false)) {
                    val sp = com.digiwear.core.model.SoundpackProfile.fromId(prefs.getString("soundpack", null))
                    val buzzer = BuzzerManager().apply {
                        isEnabled = true
                        soundpack = sp
                    }
                    try {
                        buzzer.playEvolutionAlert()
                    } catch (_: Exception) {
                    } finally {
                        buzzer.release()
                    }
                }
                postEvolutionNotification(finalSpeciesId)
            }
        }

        // 2. Attention Call Distress Signal & Rich Notification Actions
        val shouldDistressCall = evaluateDistress(finalState)
        if (shouldDistressCall) {
            val callTimestamp = finalState.attentionCallTimestampUtc ?: 0L
            val lastNotifiedCall = prefs.getLong(KEY_LAST_NOTIFIED_CALL_UTC, -1L)
            if (callTimestamp != lastNotifiedCall) {
                prefs.edit().putLong(KEY_LAST_NOTIFIED_CALL_UTC, callTimestamp).apply()
                if (prefs.getBoolean("vibration_enabled", true)) {
                    triggerVibrationAlert()
                }
                if (prefs.getBoolean("sound_enabled", false)) {
                    val sp = com.digiwear.core.model.SoundpackProfile.fromId(prefs.getString("soundpack", null))
                    val buzzer = BuzzerManager().apply {
                        isEnabled = true
                        soundpack = sp
                    }
                    try {
                        buzzer.playAttentionBeepSuspend()
                    } catch (_: Exception) {
                    } finally {
                        buzzer.release()
                    }
                }
                postAttentionCallNotification(finalState)
            }
        } else if (prefs.getLong(KEY_LAST_NOTIFIED_CALL_UTC, -1L) != -1L) {
            prefs.edit().remove(KEY_LAST_NOTIFIED_CALL_UTC).apply()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID_ATTENTION)
        }

        return Result.success()
    }

    private fun triggerEvolutionVibrationAlert() {
        val pattern = longArrayOf(0, 80, 60, 80, 60, 80, 60, 80, 120, 250, 100, 450)
        val effect = VibrationEffect.createWaveform(pattern, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(effect)
        }
    }

    private fun triggerVibrationAlert() {
        val pattern = longArrayOf(0, 200, 100, 200, 100, 400)
        val effect = VibrationEffect.createWaveform(pattern, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(effect)
        }
    }

    private fun postEvolutionNotification(finalSpeciesId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(CHANNEL_ID_EVOLUTION) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID_EVOLUTION,
                "Digital Evolution Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your virtual pet partner undergoes digital monster evolution"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 80, 60, 80, 60, 80, 60, 80, 120, 250, 100, 450)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val species = PetSpecies.getById(finalSpeciesId)
        val rawBitmap = PixelArtMatrices.getAndroidBitmap(finalSpeciesId, SpriteFrame.IDLE_A)
        val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, 192, 192, false)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_EVOLVED_SPECIES_ID, finalSpeciesId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_EVOLUTION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EVOLUTION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setLargeIcon(scaledBitmap)
            .setContentTitle("DIGITAL EVOLUTION!")
            .setContentText("${species.name} (${species.stage.displayName})")
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(scaledBitmap)
                    .setBigContentTitle("DIGITAL EVOLUTION!")
                    .setSummaryText("${species.name} (${species.stage.displayName})")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(NOTIFICATION_ID_EVOLUTION, notification)
        }
    }

    fun postAttentionCallNotification(finalState: PetState) {
        val channelId = CHANNEL_ID_ATTENTION
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                "Pet Attention Call",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when pet requires food, training, or care"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 400)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val species = PetSpecies.getById(finalState.speciesId)
        val distressMessage = formatDistressMessage(species.name, finalState)

        // 1. Content Intent: Launches MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action: [ FEED MEAT ]
        val feedIntent = Intent(context, NotificationQuickCareReceiver::class.java).apply {
            action = ACTION_QUICK_FEED
        }
        val feedPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_FEED,
            feedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val feedAction = NotificationCompat.Action.Builder(
            com.digiwear.wear.R.drawable.ic_vector_meat,
            "[ FEED MEAT ]",
            feedPendingIntent
        ).build()

        // 3. Action: [ FLUSH WASTE ]
        val cleanIntent = Intent(context, NotificationQuickCareReceiver::class.java).apply {
            action = ACTION_QUICK_CLEAN
        }
        val cleanPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CLEAN,
            cleanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cleanAction = NotificationCompat.Action.Builder(
            com.digiwear.wear.R.drawable.ic_vector_flush,
            "[ FLUSH WASTE ]",
            cleanPendingIntent
        ).build()

        // 4. WearableExtender with Quick Actions
        val wearableExtender = NotificationCompat.WearableExtender()
            .addAction(feedAction)
            .addAction(cleanAction)

        // 5. Pixel Sprite Large Icon
        val largeIcon = try {
            val frame = if (finalState.isInjured) SpriteFrame.INJURED else SpriteFrame.IDLE_A
            val rawBitmap = PixelArtMatrices.getAndroidBitmap(finalState.speciesId, frame)
            Bitmap.createScaledBitmap(rawBitmap, 192, 192, false)
        } catch (_: Throwable) {
            null
        }

        // 6. Build Notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("${species.name} Call Signal!")
            .setContentText(distressMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(distressMessage)
                    .setBigContentTitle("${species.name} Call Signal!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .addAction(feedAction)
            .addAction(cleanAction)
            .extend(wearableExtender)

        if (largeIcon != null) {
            notificationBuilder.setLargeIcon(largeIcon)
        }

        val notification = notificationBuilder.build()

        // 7. Permission Check & Notify
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(NOTIFICATION_ID_ATTENTION, notification)
        }
    }

    companion object {
        const val CHANNEL_ID_ATTENTION = "pet_attention_channel"
        const val CHANNEL_ID_EVOLUTION = "pet_evolution_channel"
        const val NOTIFICATION_ID_ATTENTION = 1001
        const val NOTIFICATION_ID_EVOLUTION = 1002
        const val KEY_LAST_NOTIFIED_CALL_UTC = "last_notified_call_utc"
        const val KEY_LAST_NOTIFIED_SPECIES_ID = "last_notified_species_id"
        const val EXTRA_EVOLVED_SPECIES_ID = "EXTRA_EVOLVED_SPECIES_ID"

        const val ACTION_QUICK_FEED = "com.digiwear.wear.action.QUICK_FEED"
        const val ACTION_QUICK_CLEAN = "com.digiwear.wear.action.QUICK_CLEAN"
        const val REQUEST_CODE_FEED = 101
        const val REQUEST_CODE_CLEAN = 102
        const val REQUEST_CODE_QUICK_FEED = 101
        const val REQUEST_CODE_QUICK_CLEAN = 102

        /**
         * Evaluates whether the pet state requires an attention call distress signal.
         */
        fun evaluateDistress(state: PetState): Boolean {
            if (state.isDeceased || state.isFrozen) return false
            return state.attentionCallActive ||
                    state.hunger == 0 ||
                    state.wasteCount >= 4 ||
                    state.isInjured
        }

        /**
         * Dynamically formats the distress alert message according to specific physiological needs.
         */
        fun formatDistressMessage(speciesName: String, state: PetState): String {
            val distressReasons = mutableListOf<String>()
            if (state.hunger == 0) {
                distressReasons.add("starving")
            }
            if (state.wasteCount >= 4) {
                distressReasons.add("surrounded by poop")
            }
            if (state.isInjured) {
                distressReasons.add("injured")
            }

            return when {
                distressReasons.isEmpty() -> "$speciesName requires immediate attention!"
                distressReasons.size == 1 -> "$speciesName is ${distressReasons[0]}!"
                distressReasons.size == 2 -> "$speciesName is ${distressReasons[0]} and ${distressReasons[1]}!"
                else -> "$speciesName is ${distressReasons.dropLast(1).joinToString(", ")}, and ${distressReasons.last()}!"
            }
        }
    }
}
