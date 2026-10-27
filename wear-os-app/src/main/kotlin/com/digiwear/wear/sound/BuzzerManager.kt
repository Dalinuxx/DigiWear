package com.digiwear.wear.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import com.digiwear.core.model.SoundpackProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Authentic Soundpack Audio Engine for Wear OS.
 * Supports multiple soundpack profiles:
 * - [SoundpackProfile.ORIGINAL_1997]: Faithful 2.048 kHz square-wave piezo beeps and death siren.
 * - [SoundpackProfile.DIGIVICE_1999]: Classic tri-tone anime/digivice chimes.
 * - [SoundpackProfile.CHIPTUNE_RETRO]: Polyphonic 8-bit procedural synthesizer.
 *
 * Produces pure 1-bit / 8-bit square-wave PCM audio using an AudioTrack pipeline.
 * Gracefully falls back to ToneGenerator if AudioTrack hardware buffers are unavailable.
 * Muted by default to respect silent environments.
 */
class BuzzerManager {

    @Volatile
    var isEnabled: Boolean = false

    @Volatile
    var soundpack: SoundpackProfile = SoundpackProfile.ORIGINAL_1997

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var _toneGenerator: ToneGenerator? = null

    /** Once true, this manager never (re)creates AudioTrack / ToneGenerator; all playback is a no-op. */
    @Volatile
    private var released = false

    private val toneGenerator: ToneGenerator?
        @Synchronized get() {
            if (released) return null
            var current = _toneGenerator
            if (current == null) {
                current = try {
                    ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                } catch (_: Exception) {
                    null
                }
                _toneGenerator = current
            }
            return current
        }

    @Volatile
    private var _synth: PureSquareWaveSynth? = null

    /** Inert synth (no AudioTrack) handed out after [release] so callers fall through to no-ops. */
    private val releasedSynth by lazy { PureSquareWaveSynth(createTrack = false) }

    private val synth: PureSquareWaveSynth
        @Synchronized get() {
            if (released) return releasedSynth
            var current = _synth
            if (current == null) {
                current = PureSquareWaveSynth()
                _synth = current
            }
            return current
        }

    /**
     * Attention call sequence (pip-pip-pip-pip or anime chime).
     */
    fun playAttentionBeep() {
        if (!isEnabled) return
        scope.launch {
            playAttentionBeepSuspend()
        }
    }

    /**
     * Suspending attention call sequence so background workers can await completion before release().
     */
    suspend fun playAttentionBeepSuspend() {
        if (!isEnabled) return
        withContext(Dispatchers.Default) {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        for (i in 0 until 4) {
                            synth.playTone(2048f, 65)
                            synth.playSilence(55)
                        }
                    } else {
                        val gen = toneGenerator ?: return@withContext
                        for (i in 0 until 4) {
                            try {
                                gen.startTone(ToneGenerator.TONE_PROP_BEEP, 65)
                            } catch (_: Exception) {}
                            delay(120L)
                        }
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        // Tri-tone chime: C6 (1046Hz), E6 (1318Hz), G6 (1568Hz)
                        synth.playTone(1046.5f, 80)
                        synth.playTone(1318.5f, 80)
                        synth.playTone(1567.9f, 140)
                    } else {
                        val gen = toneGenerator ?: return@withContext
                        try {
                            gen.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                            delay(100L)
                            gen.startTone(ToneGenerator.TONE_PROP_BEEP2, 80)
                            delay(100L)
                            gen.startTone(ToneGenerator.TONE_PROP_PROMPT, 140)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        for (i in 0 until 4) {
                            synth.playTone(2400f, 70)
                            synth.playSilence(60)
                        }
                    } else {
                        val gen = toneGenerator ?: return@withContext
                        for (i in 0 until 4) {
                            try {
                                gen.startTone(ToneGenerator.TONE_PROP_BEEP, 75)
                            } catch (_: Exception) {}
                            delay(130L)
                        }
                    }
                }
            }
        }
    }

    /**
     * Tactile stem button click chirp.
     */
    fun playButtonBeep() {
        if (!isEnabled) return
        scope.launch {
            playButtonBeepSuspend()
        }
    }

    /**
     * Suspending tactile stem button click chirp.
     */
    suspend fun playButtonBeepSuspend() {
        if (!isEnabled) return
        withContext(Dispatchers.Default) {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        synth.playTone(2048f, 18)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 18)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        synth.playTone(3136f, 22)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 20)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        synth.playTone(3200f, 18)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 20)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /**
     * Punch / attack impact tone.
     */
    fun playAttackImpactTone() {
        if (!isEnabled) return
        scope.launch {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        synth.playTone(2048f, 30)
                        synth.playSilence(15)
                        synth.playTone(1024f, 45)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 75)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        synth.playTone(1760f, 40)
                        synth.playTone(2637f, 70)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        synth.playTone(1400f, 40)
                        synth.playTone(800f, 60)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /**
     * Encounter alarm siren / chime for wild scans and boss clashes.
     */
    fun playEncounterAlarm() {
        if (!isEnabled) return
        scope.launch {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        for (i in 0 until 3) {
                            synth.playTone(2048f, 75)
                            synth.playTone(1536f, 75)
                        }
                    } else {
                        val gen = toneGenerator ?: return@launch
                        try {
                            for (i in 0 until 3) {
                                gen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 120)
                                delay(140L)
                            }
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        val chime = listOf(784f to 60, 1046.5f to 60, 1318.5f to 60, 1567.9f to 100)
                        for (cycle in 0 until 2) {
                            for ((f, d) in chime) {
                                synth.playTone(f, d)
                            }
                            synth.playSilence(40)
                        }
                    } else {
                        val gen = toneGenerator ?: return@launch
                        try {
                            gen.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 250)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        for (i in 0 until 3) {
                            synth.playTone(3000f, 80)
                            synth.playTone(2200f, 80)
                        }
                    } else {
                        val gen = toneGenerator ?: return@launch
                        try {
                            for (i in 0 until 3) {
                                gen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 120)
                                delay(140L)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /**
     * Critical Hit cut-in tone.
     */
    fun playCriticalCutInTone() {
        if (!isEnabled) return
        scope.launch {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        synth.playTone(2048f, 90)
                        synth.playSilence(25)
                        synth.playTone(2048f, 160)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 120)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        synth.playTone(2637f, 90)
                        synth.playSilence(20)
                        synth.playTone(3951f, 160)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 180)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        synth.playTone(3600f, 100)
                        synth.playSilence(20)
                        synth.playTone(2800f, 150)
                    } else {
                        val gen = toneGenerator ?: return@launch
                        try {
                            gen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 120)
                            delay(140L)
                            gen.startTone(ToneGenerator.TONE_PROP_PROMPT, 180)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /**
     * Tactile QTE reflex confirmation chirp.
     */
    fun playQteClickTone() {
        if (!isEnabled) return
        scope.launch {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        synth.playTone(2048f, 25)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 30)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        synth.playTone(2793f, 30)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 35)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        synth.playTone(2800f, 35)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 40)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /**
     * Digital evolution code glitch frequency.
     */
    fun playEvolutionGlitchTone() {
        if (!isEnabled) return
        scope.launch {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        for (i in 0 until 4) {
                            synth.playTone(2048f, 20)
                            synth.playTone(1024f, 20)
                        }
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK, 80)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        val notes = listOf(1318f, 1568f, 2093f, 2637f)
                        for (n in notes) {
                            synth.playTone(n, 25)
                        }
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK, 80)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        for (i in 0 until 4) {
                            synth.playTone(1800f, 20)
                            synth.playTone(3200f, 20)
                        }
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK, 80)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /**
     * Sweeping water flush wave tone.
     */
    fun playFlushTone() {
        if (!isEnabled) return
        scope.launch {
            playFlushToneSuspend()
        }
    }

    /**
     * Suspending sweeping water flush wave tone.
     */
    suspend fun playFlushToneSuspend() {
        if (!isEnabled) return
        withContext(Dispatchers.Default) {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        for (i in 0 until 5) {
                            synth.playTone(2048f, 25)
                            synth.playSilence(15)
                        }
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL, 150)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        synth.playTone(2093f, 50)
                        synth.playTone(1760f, 50)
                        synth.playTone(1396f, 70)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL, 160)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        synth.playTone(1600f, 50)
                        synth.playTone(1200f, 50)
                        synth.playTone(900f, 60)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL, 160)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /**
     * Medical recovery chime.
     */
    fun playHealTone() {
        if (!isEnabled) return
        scope.launch {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        synth.playTone(2048f, 60)
                        synth.playSilence(40)
                        synth.playTone(2048f, 120)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        synth.playTone(1318f, 70)
                        synth.playTone(1760f, 70)
                        synth.playTone(2637f, 150)
                    } else {
                        val gen = toneGenerator ?: return@launch
                        try {
                            gen.startTone(ToneGenerator.TONE_PROP_BEEP2, 90)
                            delay(100L)
                            gen.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        synth.playTone(2093f, 80)
                        synth.playSilence(20)
                        synth.playTone(3136f, 140)
                    } else {
                        val gen = toneGenerator ?: return@launch
                        try {
                            gen.startTone(ToneGenerator.TONE_PROP_BEEP2, 90)
                            delay(100L)
                            gen.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /**
     * Victory fanfare.
     */
    fun playVictoryFanfare() {
        if (!isEnabled) return
        scope.launch {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        val pulses = listOf(60 to 40, 60 to 40, 60 to 60, 220 to 50)
                        for ((t, s) in pulses) {
                            synth.playTone(2048f, t)
                            synth.playSilence(s)
                        }
                    } else {
                        val gen = toneGenerator ?: return@launch
                        try {
                            gen.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 250)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        val notes = listOf(1046.5f to 80, 1318.5f to 80, 1567.9f to 100, 2093f to 250)
                        for ((f, d) in notes) {
                            synth.playTone(f, d)
                            synth.playSilence(20)
                        }
                    } else {
                        val gen = toneGenerator ?: return@launch
                        try {
                            gen.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 250)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        val notes = listOf(2093f to 80, 2637f to 80, 3136f to 100, 4186f to 250)
                        for ((f, d) in notes) {
                            synth.playTone(f, d)
                            synth.playSilence(25)
                        }
                    } else {
                        val gen = toneGenerator ?: return@launch
                        val tones = listOf(
                            ToneGenerator.TONE_PROP_BEEP to 80L,
                            ToneGenerator.TONE_PROP_BEEP2 to 80L,
                            ToneGenerator.TONE_PROP_PROMPT to 120L,
                            ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE to 250L
                        )
                        for ((tone, duration) in tones) {
                            try {
                                gen.startTone(tone, duration.toInt())
                            } catch (_: Exception) {}
                            delay(duration + 30L)
                        }
                    }
                }
            }
        }
    }

    /**
     * Defeat jingle.
     */
    fun playDefeatJingle() {
        if (!isEnabled) return
        scope.launch {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        synth.playTone(2048f, 120)
                        synth.playSilence(60)
                        synth.playTone(1024f, 250)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 220)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        val notes = listOf(1567.9f to 90, 1318.5f to 90, 1046.5f to 110, 784f to 220)
                        for ((f, d) in notes) {
                            synth.playTone(f, d)
                            synth.playSilence(20)
                        }
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 220)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        val notes = listOf(3136f to 90, 2637f to 90, 2093f to 100, 1568f to 200)
                        for ((f, d) in notes) {
                            synth.playTone(f, d)
                            synth.playSilence(25)
                        }
                    } else {
                        val gen = toneGenerator ?: return@launch
                        val tones = listOf(
                            ToneGenerator.TONE_PROP_PROMPT to 90L,
                            ToneGenerator.TONE_PROP_BEEP2 to 90L,
                            ToneGenerator.TONE_PROP_BEEP to 110L,
                            ToneGenerator.TONE_CDMA_ABBR_ALERT to 220L
                        )
                        for ((tone, duration) in tones) {
                            try {
                                gen.startTone(tone, duration.toInt())
                            } catch (_: Exception) {}
                            delay(duration + 25L)
                        }
                    }
                }
            }
        }
    }

    /**
     * Somber death siren when partner departs.
     */
    fun playDeathSiren() {
        if (!isEnabled) return
        scope.launch {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        synth.playTone(2048f, 200)
                        synth.playSilence(100)
                        synth.playTone(1024f, 600)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        synth.playTone(1318f, 150)
                        synth.playTone(1174f, 150)
                        synth.playTone(987f, 400)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 400)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        synth.playTone(880f, 150)
                        synth.playTone(660f, 150)
                        synth.playTone(440f, 450)
                    } else {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 450)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /**
     * Evolution Alarm sequence upon digital monster mutation.
     */
    suspend fun playEvolutionAlert() {
        if (!isEnabled) return
        withContext(Dispatchers.Default) {
            when (soundpack) {
                SoundpackProfile.ORIGINAL_1997 -> {
                    if (synth.isAvailable) {
                        // Phase 1: 6-beep 2048 Hz burst
                        for (i in 0 until 6) {
                            synth.playTone(2048f, 50)
                            synth.playSilence(40)
                        }
                        // Phase 2: High/low piezo warble
                        for (i in 0 until 4) {
                            synth.playTone(2048f, 25)
                            synth.playTone(1024f, 25)
                        }
                        synth.playSilence(40)
                        // Phase 3: Rhythmic 2048 Hz celebration
                        val fanfare = listOf(70 to 30, 70 to 30, 120 to 40, 250 to 50)
                        for ((d, s) in fanfare) {
                            synth.playTone(2048f, d)
                            synth.playSilence(s)
                        }
                    } else {
                        val gen = toneGenerator ?: return@withContext
                        try {
                            for (i in 0 until 6) {
                                gen.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
                                delay(90L)
                            }
                            gen.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 250)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.DIGIVICE_1999 -> {
                    if (synth.isAvailable) {
                        // Phase 1: Tri-tone call
                        synth.playTone(1046.5f, 70)
                        synth.playTone(1318.5f, 70)
                        synth.playTone(1567.9f, 90)
                        synth.playSilence(40)
                        // Phase 2: Rising anime energy arpeggio
                        val scale = listOf(1046.5f, 1174.7f, 1318.5f, 1567.9f, 1760.0f, 2093.0f)
                        for (f in scale) {
                            synth.playTone(f, 35)
                        }
                        synth.playSilence(40)
                        // Phase 3: Soaring chime fanfare
                        val fanfare = listOf(2093.0f to 90, 2637.0f to 90, 3136.0f to 220)
                        for ((f, d) in fanfare) {
                            synth.playTone(f, d)
                            synth.playSilence(25)
                        }
                    } else {
                        val gen = toneGenerator ?: return@withContext
                        try {
                            gen.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
                            delay(90L)
                            gen.startTone(ToneGenerator.TONE_PROP_PROMPT, 120)
                            delay(140L)
                            gen.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 250)
                        } catch (_: Exception) {}
                    }
                }
                SoundpackProfile.CHIPTUNE_RETRO -> {
                    if (synth.isAvailable) {
                        for (i in 0 until 6) {
                            synth.playTone(2400f, 60)
                            synth.playSilence(50)
                        }
                        for (i in 0 until 4) {
                            synth.playTone(1800f, 20)
                            synth.playTone(3200f, 20)
                        }
                        synth.playSilence(40)
                        val fanfare = listOf(2093f to 80, 2637f to 80, 3136f to 120, 4186f to 250)
                        for ((f, d) in fanfare) {
                            synth.playTone(f, d)
                            synth.playSilence(30)
                        }
                    } else {
                        val gen = toneGenerator ?: return@withContext
                        try {
                            for (i in 0 until 6) {
                                try {
                                    gen.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
                                } catch (_: Exception) {}
                                delay(110L)
                            }
                            delay(40L)
                            try {
                                gen.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK, 120)
                            } catch (_: Exception) {}
                            delay(160L)
                            val fanfare = listOf(
                                ToneGenerator.TONE_PROP_BEEP to 80L,
                                ToneGenerator.TONE_PROP_BEEP2 to 80L,
                                ToneGenerator.TONE_PROP_PROMPT to 120L,
                                ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE to 250L
                            )
                            for ((tone, duration) in fanfare) {
                                try {
                                    gen.startTone(tone, duration.toInt())
                                } catch (_: Exception) {}
                                delay(duration + 30L)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    fun playEvolutionAlertAsync() {
        if (!isEnabled) return
        scope.launch {
            playEvolutionAlert()
        }
    }

    @Synchronized
    fun release() {
        released = true
        scope.coroutineContext[kotlinx.coroutines.Job]?.children?.forEach { it.cancel() }
        _synth?.release()
        _synth = null
        try {
            _toneGenerator?.release()
        } catch (_: Exception) {}
        _toneGenerator = null
    }

    /**
     * Pure square-wave PCM audio synthesizer generating in-memory chiptune frequencies via AudioTrack.
     */
    private class PureSquareWaveSynth(createTrack: Boolean = true) {
        private val sampleRate = 22050
        private var track: AudioTrack? = null

        init {
            if (createTrack) try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf.coerceAtLeast(4096))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                audioTrack.play()
                track = audioTrack
            } catch (_: Exception) {
                track = null
            }
        }

        val isAvailable: Boolean
            get() = track?.playState == AudioTrack.PLAYSTATE_PLAYING

        @Synchronized
        fun playTone(freqHz: Float, durationMs: Int, volume: Short = 16000) {
            val t = track ?: return
            val totalSamples = (sampleRate * (durationMs / 1000f)).toInt()
            if (totalSamples <= 0) return
            val phaseStep = freqHz / sampleRate
            var phase = 0f
            val buffer = ShortArray(totalSamples)
            for (i in 0 until totalSamples) {
                buffer[i] = if (phase < 0.5f) volume else (-volume).toShort()
                phase += phaseStep
                if (phase >= 1f) phase -= 1f
            }
            try {
                t.write(buffer, 0, buffer.size)
            } catch (_: Exception) {}
        }

        @Synchronized
        fun playSilence(durationMs: Int) {
            val t = track ?: return
            val totalSamples = (sampleRate * (durationMs / 1000f)).toInt()
            if (totalSamples <= 0) return
            val buffer = ShortArray(totalSamples)
            try {
                t.write(buffer, 0, buffer.size)
            } catch (_: Exception) {}
        }

        @Synchronized
        fun release() {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {}
            track = null
        }
    }
}
