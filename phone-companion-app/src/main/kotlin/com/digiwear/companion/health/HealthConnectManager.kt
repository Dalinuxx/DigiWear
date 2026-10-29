package com.digiwear.companion.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import com.digiwear.companion.data.WorkoutHistoryEntity
import java.time.Instant
import java.time.ZoneOffset

/**
 * Opt-in manager for synchronizing physical workouts to Android Health Connect.
 * Strictly optional: if Health Connect is unavailable on the phone or the user
 * declines permissions, all workout history is retained locally in Room SQLite.
 */
class HealthConnectManager(private val context: Context) {

    private val tag = "HealthConnectManager"

    val isAvailable: Boolean
        get() = try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (_: Exception) {
            false
        }

    private val client: HealthConnectClient?
        get() = if (isAvailable) {
            try {
                HealthConnectClient.getOrCreate(context)
            } catch (e: Exception) {
                Log.w(tag, "HealthConnectClient could not be instantiated: ${e.message}")
                null
            }
        } else {
            null
        }

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
    )

    suspend fun hasPermissions(): Boolean {
        val hc = client ?: return false
        return try {
            val granted = hc.permissionController.getGrantedPermissions()
            granted.containsAll(requiredPermissions)
        } catch (e: Exception) {
            Log.w(tag, "Permission check failed: ${e.message}")
            false
        }
    }

    /**
     * Attempts to write an active workout session into Health Connect.
     * Returns true if successfully written to Health Connect, false otherwise.
     */
    suspend fun syncWorkoutSession(workout: WorkoutHistoryEntity): Boolean {
        val hc = client ?: return false
        if (!hasPermissions()) {
            Log.d(tag, "Health Connect permissions pending; workout logged locally.")
            return false
        }

        return try {
            val startTime = Instant.ofEpochSecond(workout.timestampUtc)
            val endTime = startTime.plusSeconds(workout.durationSeconds.coerceAtLeast(1L))
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime)

            val exerciseType = when (workout.workoutType) {
                "CARDIO_RUN" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
                "POWER_WALK", "EXPEDITION_TREK" -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
                else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            }

            val sessionRecord = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = endTime,
                endZoneOffset = zoneOffset,
                exerciseType = exerciseType,
                title = "DigiWear Training: ${workout.workoutType}",
                notes = "Partner training session. Accelerated evolution by ${workout.evolutionMinutesAccelerated}m.",
                metadata = Metadata()
            )

            val caloriesRecord = TotalCaloriesBurnedRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = endTime,
                endZoneOffset = zoneOffset,
                energy = Energy.calories(workout.calories.coerceAtLeast(1).toDouble()),
                metadata = Metadata()
            )

            hc.insertRecords(listOf(sessionRecord, caloriesRecord))
            Log.i(tag, "Workout session #${workout.sessionId} successfully synced to Health Connect.")
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to insert workout records into Health Connect: ${e.message}")
            false
        }
    }
}
