package com.digiwear.wear.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.haptic.HapticManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StepTrackerManager(
    private val context: Context,
    private val repository: PetRepository,
    private val hapticManager: HapticManager,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val offBodySensor = sensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)
    private val stationarySensor = sensorManager.getDefaultSensor(Sensor.TYPE_STATIONARY_DETECT)
    private val prefs = context.getSharedPreferences("digiwear_step_prefs", Context.MODE_PRIVATE)

    @Volatile
    private var pendingSteps: Int = 0
    private var lastFlushTimestampMs: Long = 0L
    private var lastAccelStepTimestampMs: Long = 0L

    @Volatile
    private var lastHardwareStepCount: Int = -1

    init {
        lastHardwareStepCount = prefs.getInt("last_hardware_step_count", -1)
    }

    @Volatile
    private var isAmbient: Boolean = false

    @Volatile
    private var isOffBody: Boolean = false

    fun startListening() {
        try {
            registerStepSensors(isAmbientMode = false)

            // Off-body & Stationary sensors to prevent false steps on nightstands / chargers
            offBodySensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            stationarySensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (e: Exception) {
            android.util.Log.w("StepTracker", "Sensors could not be registered (permissions may be pending): ${e.message}")
        }
    }

    private fun registerStepSensors(isAmbientMode: Boolean) {
        val maxReportLatencyUs = if (isAmbientMode) 60_000_000 else 10_000_000 // 60s FIFO batching in ambient
        try {
            when {
                stepCounterSensor != null -> {
                    sensorManager.registerListener(
                        this,
                        stepCounterSensor,
                        SensorManager.SENSOR_DELAY_NORMAL,
                        maxReportLatencyUs
                    )
                }
                stepDetectorSensor != null -> {
                    sensorManager.registerListener(
                        this,
                        stepDetectorSensor,
                        SensorManager.SENSOR_DELAY_NORMAL,
                        maxReportLatencyUs
                    )
                }
                accelerometerSensor != null -> {
                    // Only poll accelerometer when NOT in ambient and NOT off-body
                    if (!isAmbientMode && !isOffBody) {
                        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("StepTracker", "Step sensor registration error: ${e.message}")
        }
    }

    fun stopListening() {
        flushPendingSteps()
        if (lastHardwareStepCount >= 0) {
            prefs.edit().putInt("last_hardware_step_count", lastHardwareStepCount).apply()
        }
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {}
    }

    /**
     * Reconfigures hardware sensor hub to batch up to 60s in FIFO when entering Ambient Mode,
     * and suspends high-frequency CPU accelerometer polling so the Application Processor can Doze.
     */
    fun onEnterAmbient() {
        isAmbient = true
        flushPendingSteps()

        try {
            // Switch hardware step counter / detector to 60s FIFO batch latency
            if (stepCounterSensor != null) {
                sensorManager.unregisterListener(this, stepCounterSensor)
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL, 60_000_000)
            } else if (stepDetectorSensor != null) {
                sensorManager.unregisterListener(this, stepDetectorSensor)
                sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL, 60_000_000)
            }
        } catch (e: Exception) {
            android.util.Log.w("StepTracker", "Ambient sensor reconfiguration error: ${e.message}")
        }

        // Always pause accelerometer in ambient mode
        if (accelerometerSensor != null) {
            try {
                sensorManager.unregisterListener(this, accelerometerSensor)
            } catch (_: Exception) {}
        }
    }

    /**
     * Restores low-latency reporting (10s batching) upon exiting Ambient Mode for responsive UI.
     */
    fun onExitAmbient() {
        isAmbient = false

        try {
            if (stepCounterSensor != null) {
                sensorManager.unregisterListener(this, stepCounterSensor)
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL, 10_000_000)
            } else if (stepDetectorSensor != null) {
                sensorManager.unregisterListener(this, stepDetectorSensor)
                sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL, 10_000_000)
            }

            if (stepCounterSensor == null && accelerometerSensor != null && !isOffBody) {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (e: Exception) {
            android.util.Log.w("StepTracker", "Exit ambient sensor registration error: ${e.message}")
        }
    }

    fun flushPendingSteps() {
        if (lastHardwareStepCount >= 0) {
            prefs.edit().putInt("last_hardware_step_count", lastHardwareStepCount).apply()
        }
        val stepsToFlush = synchronized(this) {
            val s = pendingSteps
            pendingSteps = 0
            s
        }
        if (stepsToFlush > 0) {
            lastFlushTimestampMs = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                val result = repository.addAdventureSteps(stepsToFlush)
                if (result.nodeCleared || result.bossEncounter != null) {
                    hapticManager.playStepMilestone()
                }
            }
        }
    }

    private fun recordStepsDelta(deltaSteps: Int) {
        if (deltaSteps <= 0 || isOffBody) return
        val now = System.currentTimeMillis()
        val stepThreshold = if (isAmbient) 50 else 10
        val maxWaitMs = if (isAmbient) 60_000L else 5_000L

        val shouldFlush = synchronized(this) {
            pendingSteps += deltaSteps
            pendingSteps >= stepThreshold || (now - lastFlushTimestampMs >= maxWaitMs)
        }
        if (shouldFlush) {
            flushPendingSteps()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> {
                // 0.0 = off body, 1.0 = on body
                val onBodyState = event.values[0] == 1.0f
                isOffBody = !onBodyState
                if (isOffBody) {
                    flushPendingSteps()
                    if (accelerometerSensor != null) {
                        sensorManager.unregisterListener(this, accelerometerSensor)
                    }
                } else if (!isAmbient && accelerometerSensor != null && stepCounterSensor == null) {
                    sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
                }
            }
            Sensor.TYPE_STATIONARY_DETECT -> {
                // Device placed stationary on surface
                if (event.values[0] == 1.0f) {
                    flushPendingSteps()
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                if (isOffBody) return
                val rawSteps = event.values[0].toInt()
                val lastSavedSteps = lastHardwareStepCount

                // First boot or hardware counter reset after device reboot
                if (lastSavedSteps < 0 || rawSteps < lastSavedSteps) {
                    lastHardwareStepCount = rawSteps
                    return
                }

                val deltaSteps = (rawSteps - lastSavedSteps).coerceAtMost(50_000)
                if (deltaSteps > 0) {
                    lastHardwareStepCount = rawSteps
                    recordStepsDelta(deltaSteps)
                }
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (isOffBody) return
                val steps = event.values.firstOrNull()?.toInt()?.coerceAtLeast(1) ?: 1
                recordStepsDelta(steps)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (isOffBody || isAmbient) return
                // Lightweight gait peak detector (~1.24g threshold with 330ms step cadence debounce)
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val mag = kotlin.math.sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
                val now = System.currentTimeMillis()
                if (mag in 12.15f..24.5f && (now - lastAccelStepTimestampMs) >= 330L) {
                    lastAccelStepTimestampMs = now
                    recordStepsDelta(1)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        fun getActivePedometerMode(context: Context): String {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return "MANUAL_STEP_DECK"
            return when {
                sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null -> "HW STEP COUNTER"
                sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null -> "HW STEP DETECTOR"
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null -> "AOSP ACCEL GAIT"
                else -> "MANUAL STEP DECK"
            }
        }
    }
}
