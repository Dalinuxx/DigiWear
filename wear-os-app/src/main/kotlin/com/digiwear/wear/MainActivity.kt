package com.digiwear.wear

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import com.digiwear.wear.nfc.NfcDuelManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.digiwear.core.model.PetState
import com.digiwear.wear.ambient.AmbientController
import com.digiwear.wear.ambient.LocalAmbientState
import com.digiwear.wear.data.PetRepository
import com.digiwear.wear.haptic.HapticManager
import com.digiwear.wear.sensor.StepTrackerManager
import com.digiwear.wear.ui.AdventureScreen
import com.digiwear.wear.ui.AmbientPetScreen
import com.digiwear.wear.ui.BattleScreen
import com.digiwear.wear.ui.MainPetScreen
import com.digiwear.wear.ui.SettingsScreen
import com.digiwear.wear.ui.StatusScreen
import com.digiwear.wear.ui.WorkoutScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import android.view.KeyEvent
import android.view.MotionEvent
import com.digiwear.wear.input.HardwareInputManager
import com.digiwear.wear.input.VPetButton

enum class WearScreen {
    MAIN,
    BATTLE,
    ADVENTURE,
    STATUS,
    SETTINGS,
    WORKOUT,
    DIGIDEX
}


class MainActivity : ComponentActivity() {

    private lateinit var repository: PetRepository
    private lateinit var hapticManager: HapticManager
    private lateinit var buzzerManager: com.digiwear.wear.sound.BuzzerManager
    private lateinit var stepTracker: StepTrackerManager
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val currentScreenState = androidx.compose.runtime.mutableStateOf(WearScreen.MAIN)
    private val navigationBackStack = mutableListOf<WearScreen>()

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchDownTimeMs = 0L
    private var isVerticalDragging = false

    fun navigateTo(screen: WearScreen) {
        if (currentScreenState.value != screen) {
            navigationBackStack.add(currentScreenState.value)
            currentScreenState.value = screen
        }
    }

    fun popScreen(): Boolean {
        if (navigationBackStack.isNotEmpty()) {
            val prev = navigationBackStack.removeAt(navigationBackStack.size - 1)
            currentScreenState.value = prev
            buzzerManager.playButtonBeep()
            return true
        } else if (currentScreenState.value != WearScreen.MAIN) {
            currentScreenState.value = WearScreen.MAIN
            buzzerManager.playButtonBeep()
            return true
        }
        return false
    }

    fun handleBack(): Boolean {
        // Check if active QTE is listening in battle/training before general back
        val qte = HardwareInputManager.activeQteListener
        if (qte != null && qte.invoke(VPetButton.C)) {
            buzzerManager.playButtonBeep()
            hapticManager.playStepMilestone()
            return true
        }

        // Deliver VPetButton.C to active screen:
        // - In screens with active modals/dialogs, dismisses the modal without popping screen.
        // - In screens without modals, triggers onBack() which calls popScreen().
        // - On MAIN, cancels menu selection or toggles lights off/on.
        HardwareInputManager.onButtonDown(VPetButton.C)
        buzzerManager.playButtonBeep()
        return true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = ev.rawX
                touchDownY = ev.rawY
                touchDownTimeMs = System.currentTimeMillis()
                isVerticalDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - touchDownX
                val dy = kotlin.math.abs(ev.rawY - touchDownY)
                if (dy > 30f && dy > kotlin.math.abs(dx)) {
                    isVerticalDragging = true
                }
            }
            MotionEvent.ACTION_UP -> {
                val dx = ev.rawX - touchDownX
                val dy = kotlin.math.abs(ev.rawY - touchDownY)
                val duration = System.currentTimeMillis() - touchDownTimeMs
                val screenW = resources.displayMetrics.widthPixels
                // Rightward back swipe: dx >= 65px, dy < dx * 0.75, fast swipe (<750ms), started in left/center 75% of screen
                if (!isVerticalDragging && dx > 65f && dy < dx * 0.75f && duration in 35..750 && touchDownX < (screenW * 0.75f)) {
                    val handled = handleBack()
                    if (handled) {
                        return true
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                touchDownTimeMs = 0L
                isVerticalDragging = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseTargetScreen(intent)

        repository = PetRepository.getInstance(applicationContext)
        hapticManager = HapticManager(this)
        buzzerManager = com.digiwear.wear.sound.BuzzerManager()
        buzzerManager.isEnabled = repository.soundEnabledFlow.value
        buzzerManager.soundpack = repository.soundpackFlow.value
        hapticManager.buzzerManager = buzzerManager
        stepTracker = StepTrackerManager(this, repository, hapticManager, activityScope)

        activityScope.launch {
            repository.soundEnabledFlow.collect { buzzerManager.isEnabled = it }
        }
        activityScope.launch {
            repository.soundpackFlow.collect { buzzerManager.soundpack = it }
        }

        // Start AOSP Wi-Fi / Hotspot LAN Sync Bridge & UDP Discovery Responder
        com.digiwear.wear.datalayer.AospLanSyncServer.start(applicationContext)

        // Request ACTIVITY_RECOGNITION / BODY_SENSORS / POST_NOTIFICATIONS on AOSP & Android 10+ watches
        requestWatchRuntimePermissionsIfNeeded()

        setContent {
            AmbientController(
                activity = this,
                onEnterAmbient = {
                    stepTracker.onEnterAmbient()
                    com.digiwear.wear.datalayer.AospLanSyncServer.pauseIfUnplugged(applicationContext)
                },
                onExitAmbient = {
                    stepTracker.onExitAmbient()
                    com.digiwear.wear.datalayer.AospLanSyncServer.resumeIfNeeded(applicationContext)
                },
                onAmbientTick = {
                    activityScope.launch(Dispatchers.IO) {
                        repository.processDecay()
                    }
                }
            ) {
                WearNavigationHost(
                    repository = repository,
                    hapticManager = hapticManager,
                    buzzerManager = buzzerManager,
                    currentScreen = currentScreenState.value,
                    onNavigate = { screen -> navigateTo(screen) },
                    onBack = { popScreen() },
                    onBackGesture = { handleBack() }
                )
            }
        }
    }

    private fun requestWatchRuntimePermissionsIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val perms = mutableListOf<String>()
            if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                perms.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (checkSelfPermission(android.Manifest.permission.BODY_SENSORS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                perms.add(android.Manifest.permission.BODY_SENSORS)
            }
            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            if (perms.isNotEmpty()) {
                try {
                    requestPermissions(perms.toTypedArray(), 1001)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onStart() {
        super.onStart()
        com.digiwear.wear.datalayer.AospLanSyncServer.resumeIfNeeded(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        stepTracker.startListening()
        activityScope.launch(Dispatchers.IO) {
            repository.processDecay()
        }
        if (NfcDuelManager.isNfcEnabled(this)) {
            NfcDuelManager.enableReaderMode(this) { opponentCode ->
                repository.setBeamedCommCode(opponentCode)
                hapticManager.playStepMilestone()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stepTracker.stopListening()
        NfcDuelManager.disableReaderMode(this)
    }

    override fun onStop() {
        super.onStop()
        com.digiwear.wear.datalayer.AospLanSyncServer.pauseIfUnplugged(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        parseTargetScreen(intent)
        val tag = androidx.core.content.IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag::class.java)
        if (tag != null) {
            val opponentCode = NfcDuelManager.extractCommCodeFromTag(tag)
            repository.setBeamedCommCode(opponentCode)
            hapticManager.playStepMilestone()
        }
    }

    private fun parseTargetScreen(intent: Intent?) {
        val target = when (intent?.getStringExtra("target_screen")) {
            "ADVENTURE" -> WearScreen.ADVENTURE
            "WORKOUT" -> WearScreen.WORKOUT
            "BATTLE" -> WearScreen.BATTLE
            "STATUS" -> WearScreen.STATUS
            "SETTINGS" -> WearScreen.SETTINGS
            "DIGIDEX" -> WearScreen.DIGIDEX
            else -> null
        }
        if (target != null && target != currentScreenState.value) {
            navigationBackStack.clear()
            navigationBackStack.add(WearScreen.MAIN)
            currentScreenState.value = target
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.digiwear.wear.datalayer.AospLanSyncServer.stop()
        buzzerManager.release()
        activityScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val button = when (event.keyCode) {
                KeyEvent.KEYCODE_STEM_1,
                KeyEvent.KEYCODE_STEM_PRIMARY,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_PAGE_UP,
                KeyEvent.KEYCODE_NAVIGATE_NEXT,
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_F1 -> VPetButton.A

                KeyEvent.KEYCODE_STEM_2,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_SPACE,
                KeyEvent.KEYCODE_CAMERA,
                KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_BUTTON_B,
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.KEYCODE_F2 -> VPetButton.B

                KeyEvent.KEYCODE_STEM_3,
                KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_SYSRQ,
                KeyEvent.KEYCODE_BUTTON_C,
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.KEYCODE_F3 -> VPetButton.C

                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_PAGE_DOWN,
                KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
                KeyEvent.KEYCODE_BUTTON_X,
                KeyEvent.KEYCODE_BUTTON_Y -> VPetButton.PREV

                KeyEvent.KEYCODE_BACK -> {
                    val handled = handleBack()
                    if (handled) return true
                    null
                }

                else -> null
            }

            if (button != null) {
                val consumed = HardwareInputManager.onButtonDown(button)
                if (consumed) {
                    buzzerManager.playButtonBeep()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_SCROLL) {
            val delta = -event.getAxisValue(MotionEvent.AXIS_SCROLL)
            if (delta > 0.05f) {
                HardwareInputManager.onButtonDown(VPetButton.A)
                buzzerManager.playButtonBeep()
                return true
            } else if (delta < -0.05f) {
                HardwareInputManager.onButtonDown(VPetButton.PREV)
                buzzerManager.playButtonBeep()
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }
}

@Composable
fun WearNavigationHost(
    repository: PetRepository,
    hapticManager: HapticManager,
    buzzerManager: com.digiwear.wear.sound.BuzzerManager = remember { com.digiwear.wear.sound.BuzzerManager() },
    currentScreen: WearScreen = WearScreen.MAIN,
    onNavigate: (WearScreen) -> Unit = {},
    onBack: () -> Unit = {},
    onBackGesture: () -> Unit = onBack
) {
    val ambientState = LocalAmbientState.current

    // Intercept system back gesture / back button
    androidx.activity.compose.BackHandler(enabled = true) {
        onBackGesture()
    }

    LaunchedEffect(Unit) {
        repository.vibrationFlow.collect { enabled ->
            hapticManager.isEnabled = enabled
        }
    }

    LaunchedEffect(Unit) {
        repository.soundEnabledFlow.collect { enabled ->
            buzzerManager.isEnabled = enabled
        }
    }
    androidx.compose.runtime.DisposableEffect(buzzerManager) {
        onDispose {
            buzzerManager.release()
        }
    }

    // Guarantee low-power monochrome display (OPR <= 15%) in Ambient Mode regardless of active sub-screen
    if (ambientState.isAmbient && currentScreen != WearScreen.MAIN) {
        val petState by repository.petStateFlow.collectAsState(initial = PetState())
        val ambientStyle by repository.ambientStyleFlow.collectAsState(initial = "MINIMAL_DOT")
        val is24hFormat by repository.clockFormatFlow.collectAsState(initial = false)
        AmbientPetScreen(
            petState = petState,
            ambientStyle = ambientStyle,
            is24hFormat = is24hFormat
        )
        return
    }

    var activeWorkoutType by remember { mutableStateOf(com.digiwear.core.workout.WorkoutType.FREE_TRAINING) }

    when (currentScreen) {
        WearScreen.MAIN -> MainPetScreen(
            repository = repository,
            hapticManager = hapticManager,
            buzzerManager = buzzerManager,
            onNavigateBattle = { onNavigate(WearScreen.BATTLE) },
            onNavigateAdventure = { onNavigate(WearScreen.ADVENTURE) },
            onNavigateStatus = { onNavigate(WearScreen.STATUS) },
            onNavigateSettings = { onNavigate(WearScreen.SETTINGS) },
            onNavigateWorkout = { type ->
                activeWorkoutType = type
                onNavigate(WearScreen.WORKOUT)
            }
        )
        WearScreen.BATTLE -> BattleScreen(
            repository = repository,
            hapticManager = hapticManager,
            buzzerManager = buzzerManager,
            onBack = { onBack() }
        )
        WearScreen.ADVENTURE -> AdventureScreen(
            repository = repository,
            hapticManager = hapticManager,
            buzzerManager = buzzerManager,
            onNavigateWorkout = {
                activeWorkoutType = com.digiwear.core.workout.WorkoutType.EXPEDITION_TREK
                onNavigate(WearScreen.WORKOUT)
            },
            onBack = { onBack() }
        )
        WearScreen.STATUS -> StatusScreen(
            repository = repository,
            onNavigateDigiDex = { onNavigate(WearScreen.DIGIDEX) },
            onBack = { onBack() }
        )
        WearScreen.DIGIDEX -> com.digiwear.wear.ui.WatchDigiDexScreen(
            repository = repository,
            onBack = { onBack() }
        )
        WearScreen.SETTINGS -> SettingsScreen(
            repository = repository,
            hapticManager = hapticManager,
            onBack = { onBack() }
        )
        WearScreen.WORKOUT -> WorkoutScreen(
            repository = repository,
            hapticManager = hapticManager,
            buzzerManager = buzzerManager,
            workoutType = activeWorkoutType,
            onFinish = { onBack() }
        )
    }
}
