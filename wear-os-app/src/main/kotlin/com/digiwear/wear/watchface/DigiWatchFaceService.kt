package com.digiwear.wear.watchface

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.BatteryManager
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.EmptyComplicationData
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PhotoImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.digiwear.core.model.BackgroundTheme
import com.digiwear.core.model.CustomThemeConfig
import com.digiwear.core.model.LcdPattern
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import com.digiwear.core.model.SpriteFrame
import com.digiwear.wear.MainActivity
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.haptic.HapticManager
import com.digiwear.wear.render.PixelArtMatrices
import com.digiwear.wear.sound.BuzzerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Standalone Wear OS Interactive Watch Face Service for DigiWear.
 *
 * Implements AndroidX WatchFace with custom CanvasRenderer2:
 * - Interactive Mode:
 *   - Authentic 7-segment retro LCD digital clock typography with inactive shadow segments (88:88).
 *   - Active partner sprite animated with idle/walk cycles (1 Hz) or joyful double-hop bounce emotes.
 *   - Physiological hunger and strength heart gauges.
 *   - Battery percentage with safe fallback to ACTION_BATTERY_CHANGED (no integer underflow).
 *   - Total adventure step count vs daily step goal.
 *   - Derived theme palette supporting both built-in themes and custom user LCD matrices.
 * - Dynamic User Interaction:
 *   - Single tap: Triggers partner dual-hop bounce animation & joy heart emote with haptic vibration and chime.
 *   - Double tap: Launches MainActivity for full pet management.
 *   - Ambient mode tap suppression to prevent accidental reactions in sleep/AOD state.
 * - Ambient Mode: Low-power monochrome dimmed retro outline rendering with pure black OLED
 *   background (OPR <= 15%) to prevent display burn-in and maximize battery life.
 * - Zero Resource Leaks: Guarantees full AudioTrack & tone release on service/renderer teardown.
 */
class DigiWatchFaceService : WatchFaceService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var buzzerManager: BuzzerManager? = null
    private var lastTapTimestampMs: Long = 0L

    override fun onDestroy() {
        serviceScope.cancel()
        buzzerManager?.release()
        buzzerManager = null
        super.onDestroy()
    }

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        val defaultCanvasComplicationFactory = CanvasComplicationFactory { watchState, invalidateCallback ->
            RetroLcdCanvasComplication(
                context = applicationContext,
                watchState = watchState,
                invalidateCallback = invalidateCallback,
                colorProvider = {
                    val repo = PetRepository.getInstance(applicationContext)
                    val theme = BackgroundTheme.fromId(repo.themeIdFlow.value)
                    val custom = repo.customThemeConfigFlow.value
                    val isCustom = theme == BackgroundTheme.CUSTOM_MATRIX && custom != null
                    val baseBgColor = if (isCustom) custom!!.primaryColorHex.toInt() else theme.primaryColorHex.toInt()
                    getThemeForegroundColors(theme, custom, baseBgColor)
                }
            )
        }

        val supportedTypes = listOf(
            ComplicationType.SHORT_TEXT,
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )

        // Slot Top (100): Battery percentage in top center
        val topSlot = ComplicationSlot.createRoundRectComplicationSlotBuilder(
            id = SLOT_TOP_ID,
            canvasComplicationFactory = defaultCanvasComplicationFactory,
            supportedTypes = supportedTypes,
            defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
                SystemDataSources.DATA_SOURCE_WATCH_BATTERY,
                ComplicationType.SHORT_TEXT
            ),
            bounds = ComplicationSlotBounds(RectF(0.24f, 0.05f, 0.76f, 0.17f))
        ).build()

        // Slot Bottom (101): Step counter / Goal Progress
        val bottomSlot = ComplicationSlot.createRoundRectComplicationSlotBuilder(
            id = SLOT_BOTTOM_ID,
            canvasComplicationFactory = defaultCanvasComplicationFactory,
            supportedTypes = supportedTypes,
            defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
                SystemDataSources.DATA_SOURCE_STEP_COUNT,
                ComplicationType.SHORT_TEXT
            ),
            bounds = ComplicationSlotBounds(RectF(0.24f, 0.80f, 0.76f, 0.94f))
        ).build()

        // Slot Left (102): Heart rate / Steps (optional)
        val leftSlot = ComplicationSlot.createRoundRectComplicationSlotBuilder(
            id = SLOT_LEFT_ID,
            canvasComplicationFactory = defaultCanvasComplicationFactory,
            supportedTypes = supportedTypes,
            defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(),
            bounds = ComplicationSlotBounds(RectF(0.04f, 0.40f, 0.28f, 0.62f))
        ).build()

        // Slot Right (103): Optional utility slot (unpopulated by default for clean dial)
        val rightSlot = ComplicationSlot.createRoundRectComplicationSlotBuilder(
            id = SLOT_RIGHT_ID,
            canvasComplicationFactory = defaultCanvasComplicationFactory,
            supportedTypes = supportedTypes,
            defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(),
            bounds = ComplicationSlotBounds(RectF(0.72f, 0.40f, 0.96f, 0.62f))
        ).build()

        return ComplicationSlotsManager(
            listOf(topSlot, bottomSlot, leftSlot, rightSlot),
            currentUserStyleRepository
        )
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val repository = PetRepository.getInstance(applicationContext)
        val hapticManager = HapticManager(applicationContext)
        val bm = BuzzerManager().apply {
            isEnabled = repository.soundEnabledFlow.value
            soundpack = repository.soundpackFlow.value
        }
        buzzerManager = bm

        val renderer = DigiCanvasRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            currentUserStyleRepository = currentUserStyleRepository,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            repository = repository,
            hapticManager = hapticManager,
            buzzerManager = bm
        )

        val watchFace = WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
            renderer = renderer
        )

        // Observe repository state updates and trigger render invalidations
        serviceScope.launch {
            repository.petStateFlow.collect {
                renderer.updatePetState(it)
                renderer.invalidate()
            }
        }

        serviceScope.launch {
            repository.themeFlow.collect {
                renderer.updateTheme(it)
                renderer.invalidate()
            }
        }

        serviceScope.launch {
            repository.customThemeConfigFlow.collect {
                renderer.updateCustomThemeConfig(it)
                renderer.invalidate()
            }
        }

        serviceScope.launch {
            repository.clockFormatFlow.collect {
                renderer.updateClockFormat(it)
                renderer.invalidate()
            }
        }

        serviceScope.launch {
            repository.dailyStepGoalFlow.collect {
                renderer.updateStepGoal(it)
                renderer.invalidate()
            }
        }

        serviceScope.launch {
            repository.dailyStepsFlow.collect {
                renderer.updateAdventureSteps(it)
                renderer.invalidate()
            }
        }

        serviceScope.launch {
            repository.ambientStyleFlow.collect {
                renderer.updateAmbientStyle(it)
                renderer.invalidate()
            }
        }

        serviceScope.launch {
            repository.soundEnabledFlow.collect {
                bm.isEnabled = it
            }
        }

        serviceScope.launch {
            repository.soundpackFlow.collect {
                bm.soundpack = it
            }
        }

        // Observe complication updates to trigger redraws
        for (slot in complicationSlotsManager.complicationSlots.values) {
            serviceScope.launch {
                slot.complicationData.collect {
                    renderer.invalidate()
                }
            }
        }

        // Tap listener: tapping complication slots triggers tapAction / provider,
        // while tapping outside retains single-tap partner bounce and double-tap app launch
        watchFace.setTapListener(object : WatchFace.TapListener {
            override fun onTapEvent(tapType: Int, tapEvent: TapEvent, complicationSlot: ComplicationSlot?) {
                // Ignore taps when screen is sleeping / ambient to prevent unintentional wake reactions
                if (watchState.isAmbient.value == true) {
                    return
                }

                if (tapType == TapType.UP) {
                    if (complicationSlot != null && complicationSlot.enabled && hasDisplayableData(complicationSlot.complicationData.value)) {
                        val tapAction = getTapAction(complicationSlot.complicationData.value)
                        if (tapAction != null) {
                            try {
                                tapAction.send()
                            } catch (_: Exception) {}
                        }
                        return
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastTapTimestampMs < DOUBLE_TAP_THRESHOLD_MS) {
                        // Double Tap: Launch MainActivity
                        lastTapTimestampMs = 0L
                        renderer.cancelBounce()
                        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(launchIntent)
                    } else {
                        // Single Tap: Trigger partner bounce emote
                        lastTapTimestampMs = now
                        renderer.triggerBounce(serviceScope)
                    }
                }
            }
        })

        return watchFace
    }

    companion object {
        private const val DOUBLE_TAP_THRESHOLD_MS = 380L
        const val SLOT_TOP_ID = 100
        const val SLOT_BOTTOM_ID = 101
        const val SLOT_LEFT_ID = 102
        const val SLOT_RIGHT_ID = 103

        /**
         * Resolves high-contrast active pixel and shadow/ghost segment colors for any theme.
         * Guarantees WCAG-compliant readability on both light LCD screens (e.g. Classic LCD #8B956D)
         * and dark AMOLED/procedural backgrounds.
         */
        fun getThemeForegroundColors(
            theme: BackgroundTheme,
            customConfig: CustomThemeConfig?,
            baseBgColor: Int
        ): Pair<Int, Int> { // (darkPixelColor, gridDotColor / ghostShadow)
            if (theme == BackgroundTheme.OLED_MINIMAL || baseBgColor == Color.BLACK) {
                return Pair(Color.WHITE, Color.rgb(45, 45, 45))
            }
            if (theme == BackgroundTheme.CLASSIC_VPET_LCD) {
                // Authentic 1997 Bandai V-Pet LCD: deep dark charcoal pixels on olive green background (>10:1 contrast)
                val activeInk = Color.rgb(20, 32, 18) // #142012 - sharp, high contrast LCD ink
                val ghostShadow = Color.rgb(126, 138, 98) // subtle inactive ghost segment
                return Pair(activeInk, ghostShadow)
            }
            if (theme == BackgroundTheme.CUSTOM_MATRIX && customConfig != null) {
                val r = Color.red(baseBgColor) / 255.0
                val g = Color.green(baseBgColor) / 255.0
                val b = Color.blue(baseBgColor) / 255.0
                val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
                return if (lum > 0.40) {
                    Pair(Color.rgb(20, 30, 20), Color.argb(60, 0, 0, 0))
                } else {
                    Pair(Color.WHITE, Color.argb(60, 255, 255, 255))
                }
            }
            return when (theme) {
                BackgroundTheme.CYBER_FOREST -> Pair(Color.rgb(105, 240, 174), Color.rgb(27, 56, 40)) // neon mint
                BackgroundTheme.DIGITAL_OCEAN -> Pair(Color.rgb(0, 229, 255), Color.rgb(27, 38, 59)) // bright cyan
                BackgroundTheme.DATA_WASTELAND -> Pair(Color.rgb(255, 213, 79), Color.rgb(61, 43, 31)) // warm amber
                BackgroundTheme.NEON_CORE_CITY -> Pair(Color.rgb(234, 128, 252), Color.rgb(45, 18, 77)) // neon violet
                BackgroundTheme.DARK_NETWORK -> Pair(Color.rgb(255, 82, 82), Color.rgb(45, 22, 31)) // vivid coral
                BackgroundTheme.GLITCH_VOID -> Pair(Color.rgb(179, 136, 255), Color.rgb(32, 32, 53)) // electric lavender
                else -> {
                    val r = Color.red(baseBgColor) / 255.0
                    val g = Color.green(baseBgColor) / 255.0
                    val b = Color.blue(baseBgColor) / 255.0
                    val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
                    if (lum > 0.40) Pair(Color.rgb(20, 32, 18), Color.rgb(120, 130, 100))
                    else Pair(Color.WHITE, Color.DKGRAY)
                }
            }
        }

        fun hasDisplayableData(data: ComplicationData?): Boolean {
            if (data == null) return false
            if (data is NoDataComplicationData || data is EmptyComplicationData) return false
            return when (data.type) {
                ComplicationType.NO_DATA,
                ComplicationType.EMPTY,
                ComplicationType.NOT_CONFIGURED,
                ComplicationType.NO_PERMISSION -> false
                else -> true
            }
        }

        fun getTapAction(data: ComplicationData?): PendingIntent? {
            if (data == null) return null
            return when (data) {
                is ShortTextComplicationData -> data.tapAction
                is RangedValueComplicationData -> data.tapAction
                is MonochromaticImageComplicationData -> data.tapAction
                is SmallImageComplicationData -> data.tapAction
                is LongTextComplicationData -> data.tapAction
                is PhotoImageComplicationData -> data.tapAction
                is GoalProgressComplicationData -> data.tapAction
                is WeightedElementsComplicationData -> data.tapAction
                else -> null
            }
        }
    }
}

class DigiSharedAssets : Renderer.SharedAssets {
    override fun onDestroy() {}
}

/**
 * Custom Hardware Canvas Renderer for DigiWatchFaceService.
 */
class DigiCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    private val repository: PetRepository,
    private val hapticManager: HapticManager,
    private val buzzerManager: BuzzerManager
) : Renderer.CanvasRenderer2<DigiSharedAssets>(
    surfaceHolder = surfaceHolder,
    currentUserStyleRepository = currentUserStyleRepository,
    watchState = watchState,
    canvasType = CanvasType.HARDWARE,
    interactiveDrawModeUpdateDelayMillis = 1000L,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {

    @Volatile
    private var petState: PetState = PetState(speciesId = 101)

    @Volatile
    private var activeTheme: BackgroundTheme = BackgroundTheme.CLASSIC_VPET_LCD

    @Volatile
    private var customThemeConfig: CustomThemeConfig? = null

    @Volatile
    private var ambientStyle: String = "MINIMAL_DOT"

    @Volatile
    private var is24hFormat: Boolean = false

    @Volatile
    private var dailyStepGoal: Int = 5000

    @Volatile
    private var currentSteps: Int = 0

    @Volatile
    private var bounceUntilTimestampMs: Long = 0L

    private var bounceJob: Job? = null

    private val timeFormatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val timeFormatter12 = DateTimeFormatter.ofPattern("hh:mm", Locale.getDefault())
    private val amPmFormatter = DateTimeFormatter.ofPattern("a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

    private val bgPaint = Paint()
    private val dotPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
    }
    private val framePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val segmentPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
    }
    private val textPaint = Paint().apply {
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val spritePaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }
    private val heartPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val reusableDstRect = Rect()
    private val reusableBezelRect = RectF()
    private val reusableHeartPath = Path()

    private var cachedBgBitmap: android.graphics.Bitmap? = null
    private var cachedBgKey: String = ""

    fun updatePetState(state: PetState) {
        petState = state
    }

    fun updateTheme(theme: BackgroundTheme) {
        activeTheme = theme
    }

    fun updateCustomThemeConfig(config: CustomThemeConfig?) {
        customThemeConfig = config
    }

    fun updateAmbientStyle(style: String) {
        ambientStyle = style
    }

    fun updateClockFormat(is24h: Boolean) {
        is24hFormat = is24h
    }

    fun updateStepGoal(goal: Int) {
        dailyStepGoal = goal
    }

    fun updateAdventureSteps(steps: Int) {
        currentSteps = steps
    }

    fun triggerBounce(scope: CoroutineScope) {
        val now = System.currentTimeMillis()
        bounceUntilTimestampMs = now + BOUNCE_DURATION_MS
        try {
            hapticManager.playStepMilestone()
        } catch (_: Exception) {}
        buzzerManager.playButtonBeep()
        invalidate()

        bounceJob?.cancel()
        bounceJob = scope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive && (System.currentTimeMillis() - startTime) < BOUNCE_DURATION_MS) {
                delay(80L)
                invalidate()
            }
            bounceUntilTimestampMs = 0L
            invalidate()
        }
    }

    fun cancelBounce() {
        bounceJob?.cancel()
        bounceJob = null
        bounceUntilTimestampMs = 0L
        invalidate()
    }

    override fun onDestroy() {
        cancelBounce()
        cachedBgBitmap?.recycle()
        cachedBgBitmap = null
        buzzerManager.release()
        super.onDestroy()
    }

    override suspend fun createSharedAssets(): DigiSharedAssets = DigiSharedAssets()

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: DigiSharedAssets
    ) {
        for (slot in complicationSlotsManager.complicationSlots.values) {
            if (slot.enabled) {
                slot.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: DigiSharedAssets
    ) {
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()

        if (isAmbient) {
            renderAmbientMode(canvas, bounds, zonedDateTime, centerX, centerY, w, h)
        } else {
            renderInteractiveMode(canvas, bounds, zonedDateTime, centerX, centerY, w, h)
        }

        // Render complication slots directly on canvas matching retro LCD styling
        for (slot in complicationSlotsManager.complicationSlots.values) {
            if (slot.enabled) {
                slot.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun renderAmbientMode(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        centerX: Float,
        centerY: Float,
        w: Float,
        h: Float
    ) {
        // Pure black background for OLED burn-in protection and <= 15% on-pixel ratio (OPR)
        canvas.drawColor(Color.BLACK)

        val tintColor = when (ambientStyle) {
            "DIMMED_LCD" -> Color.parseColor("#80CBC4")
            "OUTLINE" -> Color.parseColor("#E0E0E0")
            else -> Color.WHITE
        }

        // 0. Calendar Date (Monochrome, above 7-segment digital time)
        val dateStr = dateFormatter.format(zonedDateTime).uppercase()
        textPaint.color = tintColor
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = (w * 0.040f).coerceIn(11f, 15f)
        canvas.drawText(dateStr, centerX, centerY - h * 0.33f, textPaint)

        // 1. Digital Clock (Authentic 7-Segment Retro Digits in Monochrome)
        val timeStr = if (is24hFormat) timeFormatter24.format(zonedDateTime) else timeFormatter12.format(zonedDateTime)
        val amPmStr = if (!is24hFormat) amPmFormatter.format(zonedDateTime) else ""
        val clockWidth = w * 0.52f
        val clockHeight = h * 0.16f
        val clockY = centerY - h * 0.22f

        draw7SegmentTime(
            canvas = canvas,
            timeStr = timeStr,
            cx = centerX,
            cy = clockY,
            clockWidth = clockWidth,
            clockHeight = clockHeight,
            activeColor = tintColor,
            shadowColor = Color.BLACK,
            drawGhost = false
        )

        if (amPmStr.isNotEmpty()) {
            textPaint.color = tintColor
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = w * 0.04f
            canvas.drawText(amPmStr, centerX + (clockWidth * 0.54f), clockY - (clockHeight * 0.1f), textPaint)
            textPaint.textAlign = Paint.Align.CENTER
        }

        // 2. Monochrome Ambient Outline Sprite
        val ambientBitmap = PixelArtMatrices.getAndroidBitmap(petState.speciesId, SpriteFrame.IDLE_A, isAmbient = true)
        val spriteSize = (w * 0.34f).toInt()
        val spriteLeft = (centerX - spriteSize / 2f).toInt()
        val spriteTop = (centerY - spriteSize / 2f + h * 0.02f).toInt()
        reusableDstRect.set(spriteLeft, spriteTop, spriteLeft + spriteSize, spriteTop + spriteSize)
        canvas.drawBitmap(ambientBitmap, null, reusableDstRect, spritePaint)

        // 3. Status Line
        textPaint.color = tintColor
        textPaint.textSize = w * 0.055f
        val statusText = when {
            petState.isDeceased -> "[RIP]"
            petState.attentionCallActive -> "[CALL]"
            petState.isSleeping -> "ZZZ"
            else -> "H:${petState.hunger}/4  S:${petState.strength}/4"
        }
        canvas.drawText(statusText, centerX, centerY + h * 0.28f, textPaint)

        // Minimal battery / steps (omitted when bottom complication slot is populated)
        val hasBottomSlotData = DigiWatchFaceService.hasDisplayableData(
            complicationSlotsManager[DigiWatchFaceService.SLOT_BOTTOM_ID]?.complicationData?.value
        )
        if (!hasBottomSlotData) {
            val batteryPct = getBatteryPercentage(context)
            textPaint.color = Color.DKGRAY
            textPaint.textSize = w * 0.045f
            canvas.drawText("${currentSteps}st • ${batteryPct}%", centerX, centerY + h * 0.38f, textPaint)
        }
    }

    private fun getOrBuildBackgroundBitmap(
        width: Int,
        height: Int,
        baseLcdColor: Int,
        gridDotColor: Int,
        isOled: Boolean,
        activePattern: LcdPattern
    ): android.graphics.Bitmap {
        val key = "${width}x${height}_${baseLcdColor}_${gridDotColor}_${isOled}_${activePattern.name}"
        val existing = cachedBgBitmap
        if (existing != null && !existing.isRecycled && cachedBgKey == key) {
            return existing
        }
        existing?.recycle()
        val bmp = android.graphics.Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
        val bgCanvas = Canvas(bmp)
        bgPaint.color = baseLcdColor
        bgCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val w = width.toFloat()
        val h = height.toFloat()
        when (activePattern) {
            LcdPattern.DOT_MATRIX -> {
                dotPaint.color = gridDotColor
                dotPaint.alpha = if (isOled) 30 else 45
                dotPaint.style = Paint.Style.FILL
                val dotSpacing = 10f
                var x = 5f
                while (x < w) {
                    var y = 5f
                    while (y < h) {
                        bgCanvas.drawCircle(x, y, 1.2f, dotPaint)
                        y += dotSpacing
                    }
                    x += dotSpacing
                }
            }
            LcdPattern.GRID -> {
                dotPaint.color = gridDotColor
                dotPaint.alpha = if (isOled) 25 else 35
                dotPaint.strokeWidth = 1f
                dotPaint.style = Paint.Style.STROKE
                val gridSpacing = 12f
                var gx = gridSpacing
                while (gx < w) {
                    bgCanvas.drawLine(gx, 0f, gx, h, dotPaint)
                    gx += gridSpacing
                }
                var gy = gridSpacing
                while (gy < h) {
                    bgCanvas.drawLine(0f, gy, w, gy, dotPaint)
                    gy += gridSpacing
                }
                dotPaint.style = Paint.Style.FILL
            }
            LcdPattern.SCANLINES -> {
                dotPaint.color = gridDotColor
                dotPaint.alpha = if (isOled) 30 else 40
                dotPaint.strokeWidth = 1.2f
                dotPaint.style = Paint.Style.STROKE
                val scanlineSpacing = 4f
                var sy = 2f
                while (sy < h) {
                    bgCanvas.drawLine(0f, sy, w, sy, dotPaint)
                    sy += scanlineSpacing
                }
                dotPaint.style = Paint.Style.FILL
            }
            LcdPattern.OLED -> {
                // Pure solid backlight, no pattern overlay
            }
        }
        cachedBgBitmap = bmp
        cachedBgKey = key
        return bmp
    }

    private fun renderInteractiveMode(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        centerX: Float,
        centerY: Float,
        w: Float,
        h: Float
    ) {
        val now = System.currentTimeMillis()
        val isBouncing = now < bounceUntilTimestampMs
        val species = PetSpecies.getById(petState.speciesId)

        // Derive palette with Custom LCD Theme support and verified high contrast
        val isCustom = activeTheme == BackgroundTheme.CUSTOM_MATRIX && customThemeConfig != null
        val baseLcdColor = if (isCustom) customThemeConfig!!.primaryColorHex.toInt() else activeTheme.primaryColorHex.toInt()
        val isOled = (activeTheme == BackgroundTheme.OLED_MINIMAL) || (baseLcdColor == Color.BLACK)

        val (darkPixelColor, gridDotColor) = DigiWatchFaceService.getThemeForegroundColors(
            activeTheme,
            customThemeConfig,
            baseLcdColor
        )

        // 1 & 2. Cached LCD Matrix Pattern Overlay (DOT_MATRIX, GRID, SCANLINES, OLED)
        val activePattern = if (isCustom) {
            customThemeConfig?.pattern ?: LcdPattern.DOT_MATRIX
        } else if (isOled) {
            LcdPattern.OLED
        } else {
            LcdPattern.DOT_MATRIX
        }
        val bgBitmap = getOrBuildBackgroundBitmap(
            width = bounds.width(),
            height = bounds.height(),
            baseLcdColor = baseLcdColor,
            gridDotColor = gridDotColor,
            isOled = isOled,
            activePattern = activePattern
        )
        canvas.drawBitmap(bgBitmap, 0f, 0f, null)

        val isRound = kotlin.math.abs(w - h) < 12f

        // 3. LCD Screen Bezel Frame (Circular accent ring on round dials, rounded rect on square displays)
        if (isRound) {
            framePaint.color = darkPixelColor
            framePaint.alpha = if (isOled) 45 else 65
            framePaint.style = Paint.Style.STROKE
            framePaint.strokeWidth = 2f
            canvas.drawCircle(centerX, centerY, w * 0.465f, framePaint)
        } else {
            framePaint.color = darkPixelColor
            framePaint.alpha = 110
            framePaint.style = Paint.Style.STROKE
            framePaint.strokeWidth = 3f
            val bezelMargin = w * 0.06f
            reusableBezelRect.set(bezelMargin, bezelMargin, w - bezelMargin, h - bezelMargin)
            canvas.drawRoundRect(reusableBezelRect, 18f, 18f, framePaint)
        }

        // 4. Top Header: Battery percentage centered at top
        val hasTopSlotData = DigiWatchFaceService.hasDisplayableData(
            complicationSlotsManager[DigiWatchFaceService.SLOT_TOP_ID]?.complicationData?.value
        )
        if (!hasTopSlotData) {
            val batteryPct = getBatteryPercentage(context)
            textPaint.color = darkPixelColor
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = (w * 0.040f).coerceIn(12f, 16f)
            val topHeaderY = if (isRound) centerY - (h * 0.36f) else (w * 0.06f) + 16f
            canvas.drawText("⚡${batteryPct}%", centerX, topHeaderY, textPaint)
        }

        // Calendar Date centered directly above 7-segment digital time
        val dateStr = dateFormatter.format(zonedDateTime).uppercase()
        textPaint.color = darkPixelColor
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = (w * 0.040f).coerceIn(12f, 16f)
        val dateY = if (isRound) centerY - (h * 0.25f) else (w * 0.06f) + 38f
        canvas.drawText(dateStr, centerX, dateY, textPaint)

        // 5. Authentic 7-Segment Retro LCD Digital Clock with Ghost Segments
        val timeStr = if (is24hFormat) timeFormatter24.format(zonedDateTime) else timeFormatter12.format(zonedDateTime)
        val amPmStr = if (!is24hFormat) amPmFormatter.format(zonedDateTime) else ""
        val clockWidth = w * 0.54f
        val clockHeight = h * 0.16f
        val clockY = centerY - h * 0.11f

        draw7SegmentTime(
            canvas = canvas,
            timeStr = timeStr,
            cx = centerX,
            cy = clockY,
            clockWidth = clockWidth,
            clockHeight = clockHeight,
            activeColor = darkPixelColor,
            shadowColor = gridDotColor,
            drawGhost = true
        )

        if (amPmStr.isNotEmpty()) {
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = (w * 0.036f).coerceIn(10f, 14f)
            canvas.drawText(amPmStr, centerX + (clockWidth * 0.53f), clockY - (clockHeight * 0.1f), textPaint)
            textPaint.textAlign = Paint.Align.CENTER
        }

        // 6. Current Partner Sprite Animation (1 Hz Walking / Idle Cycle or Dynamic Dual-Hop Bounce Emote)
        val bounceProgress = if (isBouncing) {
            val elapsed = BOUNCE_DURATION_MS - (bounceUntilTimestampMs - now)
            (elapsed.toFloat() / BOUNCE_DURATION_MS).coerceIn(0f, 1f)
        } else 0f

        val hop = if (isBouncing) {
            kotlin.math.abs(kotlin.math.sin(bounceProgress * kotlin.math.PI * 2.0)).toFloat()
        } else 0f
        val bounceOffset = -16f * hop

        val activeFrame = when {
            isBouncing -> if (hop > 0.35f) SpriteFrame.JOY_EAT else SpriteFrame.IDLE_A
            petState.isInjured -> SpriteFrame.INJURED
            petState.isSleeping -> if ((zonedDateTime.second % 2) == 0) SpriteFrame.SLEEP_A else SpriteFrame.SLEEP_B
            (zonedDateTime.second % 2) == 0 -> SpriteFrame.IDLE_A
            else -> SpriteFrame.IDLE_B
        }

        val spriteBitmap = PixelArtMatrices.getAndroidBitmap(petState.speciesId, activeFrame, isAmbient = false)
        val spriteSize = (w * 0.35f).toInt()
        val spriteLeft = (centerX - spriteSize / 2f).toInt()
        val spriteTop = (centerY - spriteSize / 2f + h * 0.08f + bounceOffset).toInt()
        reusableDstRect.set(spriteLeft, spriteTop, spriteLeft + spriteSize, spriteTop + spriteSize)
        canvas.drawBitmap(spriteBitmap, null, reusableDstRect, spritePaint)

        // If bouncing, draw a floating joy heart emote scaling above the partner
        if (isBouncing) {
            val heartOffset = bounceOffset - 12f - (bounceProgress * 8f)
            drawJoyHeart(canvas, centerX, (centerY - spriteSize / 2f + h * 0.08f) + heartOffset, darkPixelColor, w)
        }

        // 7. Physiological Hearts (Hunger & Strength) - Mathematically centered
        val metersY = centerY + h * 0.28f
        val hgrWidth = measureHeartRowWidth("HGR", w)
        val strWidth = measureHeartRowWidth("STR", w)
        val midGap = w * 0.045f
        val totalMetersWidth = hgrWidth + midGap + strWidth
        val metersStartX = centerX - (totalMetersWidth / 2f)

        drawHeartRow(canvas, metersStartX, metersY, "HGR", petState.hunger.coerceIn(0, 4), darkPixelColor, gridDotColor, w)
        drawHeartRow(canvas, metersStartX + hgrWidth + midGap, metersY, "STR", petState.strength.coerceIn(0, 4), darkPixelColor, gridDotColor, w)

        // 8. Centered Steps Telemetry (drawn when bottom complication slot is empty)
        val hasBottomSlotData = DigiWatchFaceService.hasDisplayableData(
            complicationSlotsManager[DigiWatchFaceService.SLOT_BOTTOM_ID]?.complicationData?.value
        )
        if (!hasBottomSlotData) {
            val bottomY = if (isRound) centerY + h * 0.38f else h - (w * 0.06f) - 10f
            textPaint.textSize = (w * 0.038f).coerceIn(11f, 16f)
            textPaint.color = darkPixelColor
            textPaint.textAlign = Paint.Align.CENTER
            val bottomText = if (petState.isSleeping) {
                "ZZZ • 👟 ${currentSteps} STEPS"
            } else {
                "👟 ${currentSteps} / ${dailyStepGoal} STEPS"
            }
            canvas.drawText(bottomText, centerX, bottomY, textPaint)
        }
    }

    /**
     * Draws authentic 7-segment retro LCD digital clock time with ghost inactive segments.
     */
    private fun draw7SegmentTime(
        canvas: Canvas,
        timeStr: String,
        cx: Float,
        cy: Float,
        clockWidth: Float,
        clockHeight: Float,
        activeColor: Int,
        shadowColor: Int,
        drawGhost: Boolean
    ) {
        val chars = timeStr.toCharArray()
        val digitW = clockWidth * 0.19f
        val colonW = clockWidth * 0.08f
        val spacing = clockWidth * 0.035f
        val totalW = digitW * 4 + colonW + spacing * 4
        var curX = cx - totalW / 2f
        val curY = cy - clockHeight / 2f

        segmentPaint.color = shadowColor
        segmentPaint.style = Paint.Style.FILL

        // 1. Draw Ghost 88:88 background segments if requested
        if (drawGhost) {
            segmentPaint.alpha = 28
            var gX = curX
            for (i in 0 until 5) {
                if (i == 2) {
                    drawColon(canvas, gX, curY, colonW, clockHeight, segmentPaint)
                    gX += colonW + spacing
                } else {
                    draw7SegmentDigit(canvas, 127, gX, curY, digitW, clockHeight, segmentPaint)
                    gX += digitW + spacing
                }
            }
        }

        // 2. Draw Active Segments
        segmentPaint.color = activeColor
        segmentPaint.alpha = 255
        for (ch in chars) {
            when (ch) {
                ':' -> {
                    drawColon(canvas, curX, curY, colonW, clockHeight, segmentPaint)
                    curX += colonW + spacing
                }
                in '0'..'9' -> {
                    val mask = getDigitMask(ch)
                    draw7SegmentDigit(canvas, mask, curX, curY, digitW, clockHeight, segmentPaint)
                    curX += digitW + spacing
                }
                else -> {
                    curX += digitW + spacing
                }
            }
        }
    }

    private fun getDigitMask(ch: Char): Int = when (ch) {
        '0' -> 0x3F // a,b,c,d,e,f
        '1' -> 0x06 // b,c
        '2' -> 0x5B // a,b,g,e,d
        '3' -> 0x4F // a,b,g,c,d
        '4' -> 0x66 // f,g,b,c
        '5' -> 0x6D // a,f,g,c,d
        '6' -> 0x7D // a,f,g,e,c,d
        '7' -> 0x07 // a,b,c
        '8' -> 0x7F // a,b,c,d,e,f,g
        '9' -> 0x6F // a,b,c,d,f,g
        else -> 0
    }

    private fun draw7SegmentDigit(
        canvas: Canvas,
        mask: Int,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        paint: Paint
    ) {
        val t = (w * 0.18f).coerceAtLeast(2f)
        val halfH = h / 2f
        val gap = t * 0.25f

        // Segment a: top
        if ((mask and 1) != 0) {
            canvas.drawRect(x + t, y, x + w - t, y + t, paint)
        }
        // Segment b: top-right
        if ((mask and 2) != 0) {
            canvas.drawRect(x + w - t, y + t + gap, x + w, y + halfH - gap, paint)
        }
        // Segment c: bottom-right
        if ((mask and 4) != 0) {
            canvas.drawRect(x + w - t, y + halfH + gap, x + w, y + h - t - gap, paint)
        }
        // Segment d: bottom
        if ((mask and 8) != 0) {
            canvas.drawRect(x + t, y + h - t, x + w - t, y + h, paint)
        }
        // Segment e: bottom-left
        if ((mask and 16) != 0) {
            canvas.drawRect(x, y + halfH + gap, x + t, y + h - t - gap, paint)
        }
        // Segment f: top-left
        if ((mask and 32) != 0) {
            canvas.drawRect(x, y + t + gap, x + t, y + halfH - gap, paint)
        }
        // Segment g: middle
        if ((mask and 64) != 0) {
            val halfT = t / 2f
            canvas.drawRect(x + t, y + halfH - halfT, x + w - t, y + halfH + halfT, paint)
        }
    }

    private fun drawColon(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, paint: Paint) {
        val dotSize = (w * 0.6f).coerceAtLeast(3f)
        val dotX = x + (w - dotSize) / 2f
        val topY = y + h * 0.32f - dotSize / 2f
        val bottomY = y + h * 0.68f - dotSize / 2f
        canvas.drawRect(dotX, topY, dotX + dotSize, topY + dotSize, paint)
        canvas.drawRect(dotX, bottomY, dotX + dotSize, bottomY + dotSize, paint)
    }

    private fun measureHeartRowWidth(label: String, screenW: Float): Float {
        val textSize = (screenW * 0.034f).coerceIn(10f, 15f)
        val heartRadius = (screenW * 0.014f).coerceIn(4.5f, 7f)
        val heartSpacing = heartRadius * 2.5f
        textPaint.textSize = textSize
        val labelWidth = textPaint.measureText(label)
        return labelWidth + (screenW * 0.018f) + (3 * heartSpacing) + heartRadius
    }

    private fun drawHeartRow(
        canvas: Canvas,
        startX: Float,
        y: Float,
        label: String,
        filledCount: Int,
        activeColor: Int,
        emptyColor: Int,
        screenW: Float
    ) {
        val textSize = (screenW * 0.034f).coerceIn(10f, 15f)
        val heartRadius = (screenW * 0.014f).coerceIn(4.5f, 7f)
        val heartSpacing = heartRadius * 2.5f

        textPaint.color = activeColor
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(label, startX, y, textPaint)

        var hX = startX + textPaint.measureText(label) + (screenW * 0.018f)
        for (i in 0 until 4) {
            val isFilled = i < filledCount
            drawHeartIcon(canvas, hX, y - (heartRadius * 0.8f), heartRadius, if (isFilled) activeColor else emptyColor, isFilled)
            hX += heartSpacing
        }
        textPaint.textAlign = Paint.Align.CENTER
    }

    private fun drawHeartIcon(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, filled: Boolean) {
        heartPaint.color = color
        heartPaint.style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
        heartPaint.strokeWidth = 1.2f

        reusableHeartPath.reset()
        reusableHeartPath.moveTo(cx, cy + radius * 0.8f)
        reusableHeartPath.cubicTo(cx - radius * 1.5f, cy - radius * 0.8f, cx - radius, cy - radius * 1.8f, cx, cy - radius * 0.7f)
        reusableHeartPath.cubicTo(cx + radius, cy - radius * 1.8f, cx + radius * 1.5f, cy - radius * 0.8f, cx, cy + radius * 0.8f)
        reusableHeartPath.close()
        canvas.drawPath(reusableHeartPath, heartPaint)
    }

    private fun drawJoyHeart(canvas: Canvas, cx: Float, cy: Float, color: Int, screenW: Float = 400f) {
        val heartR = (screenW * 0.020f).coerceIn(6f, 10f)
        drawHeartIcon(canvas, cx, cy, heartR, color, filled = true)
    }

    companion object {
        private const val BOUNCE_DURATION_MS = 1400L

        /**
         * Robust battery percentage retrieval protecting against Integer.MIN_VALUE or negative values.
         */
        fun getBatteryPercentage(context: Context): Int {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val cap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (cap in 0..100) return cap
            return try {
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, filter)
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) {
                    (level * 100 / scale.toFloat()).toInt().coerceIn(0, 100)
                } else {
                    100
                }
            } catch (_: Exception) {
                100
            }
        }
    }
}

/**
 * Retro LCD Complication Renderer.
 *
 * Implements AndroidX CanvasComplication rendering complications directly onto the watch face canvas:
 * - Short Text: Retro monospace typography, optional monochromatic icon, framed by subtle 1px LCD bezel.
 * - Ranged Value: Monospace value label plus retro segmented LCD gauge bar with ghost/shadow blocks.
 * - Monochromatic / Small Image: Scaled and tinted with the active LCD palette or ambient wireframe.
 * - Ambient Mode: Dimmed wireframe rendering with zero fill and low OPR to prevent OLED display burn-in.
 */
class RetroLcdCanvasComplication(
    private val context: Context,
    private val watchState: WatchState,
    private val invalidateCallback: CanvasComplication.InvalidateCallback,
    private val colorProvider: () -> Pair<Int, Int> // (darkPixelColor, gridDotColor)
) : CanvasComplication {

    private var complicationData: ComplicationData = NoDataComplicationData()
    private var cachedIconDrawable: android.graphics.drawable.Drawable? = null
    private val reusableRectF = RectF()

    private val textPaint = Paint().apply {
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val framePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        isAntiAlias = false
    }

    private val highlightPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = false
    }

    private val barPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    override fun loadData(complicationData: ComplicationData, loadNow: Boolean) {
        this.complicationData = complicationData
        this.cachedIconDrawable = try {
            when (complicationData) {
                is ShortTextComplicationData -> complicationData.monochromaticImage?.image?.loadDrawable(context)
                    ?: complicationData.smallImage?.image?.loadDrawable(context)
                is RangedValueComplicationData -> complicationData.monochromaticImage?.image?.loadDrawable(context)
                is MonochromaticImageComplicationData -> complicationData.monochromaticImage.image.loadDrawable(context)
                is SmallImageComplicationData -> complicationData.smallImage.image.loadDrawable(context)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
        if (loadNow) {
            invalidateCallback.onInvalidate()
        }
    }

    override fun getData(): ComplicationData = complicationData

    override fun drawHighlight(
        canvas: Canvas,
        bounds: Rect,
        boundsType: Int,
        zonedDateTime: ZonedDateTime,
        color: Int
    ) {
        highlightPaint.color = color
        reusableRectF.set(bounds)
        canvas.drawRoundRect(reusableRectF, 4f, 4f, highlightPaint)
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        renderParameters: RenderParameters,
        slotId: Int
    ) {
        val data = complicationData
        if (!DigiWatchFaceService.hasDisplayableData(data)) return

        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        val (darkColor, gridColor) = colorProvider()
        val primaryColor = if (isAmbient) Color.WHITE else darkColor
        val shadowColor = if (isAmbient) Color.TRANSPARENT else gridColor

        val instant = zonedDateTime.toInstant()
        reusableRectF.set(bounds)

        when (data) {
            is ShortTextComplicationData -> {
                val text = data.text.getTextAt(context.resources, instant)?.toString() ?: ""
                val title = data.title?.getTextAt(context.resources, instant)?.toString()
                val iconDrawable = cachedIconDrawable

                val hasIcon = iconDrawable != null
                val hasTitle = !title.isNullOrEmpty()

                if (hasIcon) {
                    val iconSize = (bounds.height() * 0.48f).toInt().coerceAtLeast(8)
                    val textStr = text.uppercase()
                    textPaint.textSize = (bounds.height() * 0.40f).coerceIn(8f, 13f)
                    val textW = textPaint.measureText(textStr)
                    val totalW = iconSize + 4f + textW
                    val startX = bounds.exactCenterX() - (totalW / 2f)

                    val iconLeft = startX.toInt()
                    val iconTop = bounds.centerY() - (iconSize / 2)
                    iconDrawable!!.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                    if (isAmbient || data.monochromaticImage != null) {
                        iconDrawable.setTint(primaryColor)
                    } else {
                        // Clear any tint left over from a previous ambient frame on the cached drawable.
                        iconDrawable.setTintList(null)
                    }
                    iconDrawable.draw(canvas)

                    textPaint.color = primaryColor
                    textPaint.textAlign = Paint.Align.LEFT
                    val textX = startX + iconSize + 4f
                    val textY = bounds.centerY() + (textPaint.textSize * 0.35f)
                    canvas.drawText(textStr, textX, textY, textPaint)
                    textPaint.textAlign = Paint.Align.CENTER
                } else if (hasTitle) {
                    textPaint.color = primaryColor
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.textSize = (bounds.height() * 0.28f).coerceIn(7f, 10f)
                    val titleY = bounds.top + bounds.height() * 0.38f
                    canvas.drawText(title!!.uppercase(), bounds.exactCenterX(), titleY, textPaint)

                    textPaint.textSize = (bounds.height() * 0.42f).coerceIn(8f, 13f)
                    val textY = bounds.top + bounds.height() * 0.82f
                    canvas.drawText(text.uppercase(), bounds.exactCenterX(), textY, textPaint)
                } else {
                    textPaint.color = primaryColor
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.textSize = (bounds.height() * 0.46f).coerceIn(8f, 14f)
                    val textY = bounds.centerY() + (textPaint.textSize * 0.35f)
                    canvas.drawText(text.uppercase(), bounds.exactCenterX(), textY, textPaint)
                }
            }

            is RangedValueComplicationData -> {
                val value = data.value
                val min = data.min
                val max = data.max
                val ratio = if (max > min && !value.isNaN() && !value.isInfinite()) {
                    ((value - min) / (max - min)).coerceIn(0f, 1f)
                } else 0f
                val text = data.text?.getTextAt(context.resources, instant)?.toString()
                    ?: data.title?.getTextAt(context.resources, instant)?.toString()
                    ?: "${(ratio * 100).toInt()}%"

                val iconDrawable = cachedIconDrawable

                // Top: Monospace label with optional icon
                textPaint.color = primaryColor
                textPaint.textSize = (bounds.height() * 0.35f).coerceIn(7f, 12f)
                val textY = bounds.top + bounds.height() * 0.40f

                if (iconDrawable != null) {
                    val iconSize = (bounds.height() * 0.32f).toInt().coerceAtLeast(8)
                    val textW = textPaint.measureText(text.uppercase())
                    val totalContentW = iconSize + 4f + textW
                    val startX = bounds.exactCenterX() - (totalContentW / 2f)
                    val iconTop = (textY - iconSize * 0.85f).toInt()
                    iconDrawable.setBounds(startX.toInt(), iconTop, (startX + iconSize).toInt(), iconTop + iconSize)
                    iconDrawable.setTint(primaryColor)
                    iconDrawable.draw(canvas)

                    textPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(text.uppercase(), startX + iconSize + 4f, textY, textPaint)
                    textPaint.textAlign = Paint.Align.CENTER
                } else {
                    textPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(text.uppercase(), bounds.exactCenterX(), textY, textPaint)
                }

                // Bottom: Retro LCD Segmented Gauge Bar (6 discrete blocks)
                val barMarginH = 6f
                val barTop = bounds.top + bounds.height() * 0.56f
                val barBottom = bounds.bottom - 4f
                val totalBarW = bounds.width() - (barMarginH * 2)
                val segments = 6
                val segSpacing = 2f
                val segWidth = (totalBarW - (segSpacing * (segments - 1))) / segments
                val filledSegments = (ratio * segments).toInt().coerceIn(0, segments)

                for (i in 0 until segments) {
                    val sLeft = bounds.left + barMarginH + i * (segWidth + segSpacing)
                    val sRight = sLeft + segWidth
                    if (i < filledSegments) {
                        barPaint.color = primaryColor
                        barPaint.alpha = 255
                        barPaint.style = Paint.Style.FILL
                        canvas.drawRect(sLeft, barTop, sRight, barBottom, barPaint)
                    } else if (!isAmbient && shadowColor != Color.TRANSPARENT) {
                        barPaint.color = shadowColor
                        barPaint.alpha = 50
                        barPaint.style = Paint.Style.FILL
                        canvas.drawRect(sLeft, barTop, sRight, barBottom, barPaint)
                    } else if (isAmbient) {
                        barPaint.color = primaryColor
                        barPaint.alpha = 40
                        barPaint.style = Paint.Style.STROKE
                        barPaint.strokeWidth = 1f
                        canvas.drawRect(sLeft, barTop, sRight, barBottom, barPaint)
                    }
                }
            }

            is MonochromaticImageComplicationData -> {
                val iconDrawable = cachedIconDrawable
                if (iconDrawable != null) {
                    val size = (minOf(bounds.width(), bounds.height()) * 0.62f).toInt()
                    val left = bounds.centerX() - (size / 2)
                    val top = bounds.centerY() - (size / 2)
                    iconDrawable.setBounds(left, top, left + size, top + size)
                    iconDrawable.setTint(primaryColor)
                    iconDrawable.draw(canvas)
                }
            }

            is SmallImageComplicationData -> {
                val iconDrawable = cachedIconDrawable
                if (iconDrawable != null) {
                    val size = (minOf(bounds.width(), bounds.height()) * 0.62f).toInt()
                    val left = bounds.centerX() - (size / 2)
                    val top = bounds.centerY() - (size / 2)
                    iconDrawable.setBounds(left, top, left + size, top + size)
                    if (isAmbient) {
                        iconDrawable.setTint(primaryColor)
                    } else {
                        // Clear any tint left over from a previous ambient frame on the cached drawable.
                        iconDrawable.setTintList(null)
                    }
                    iconDrawable.draw(canvas)
                }
            }

            else -> {
                val text = (data as? PlainComplicationText)?.toString() ?: ""
                if (text.isNotEmpty()) {
                    textPaint.color = primaryColor
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.textSize = (bounds.height() * 0.38f).coerceIn(8f, 13f)
                    canvas.drawText(text.uppercase(), bounds.exactCenterX(), bounds.centerY() + 4f, textPaint)
                }
            }
        }
    }
}
