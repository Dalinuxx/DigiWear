package com.digiwear.companion

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digiwear.companion.ui.ExpeditionScreen
import com.digiwear.companion.ui.SettingsScreen
import com.digiwear.companion.ui.DigiIconType
import com.digiwear.companion.ui.DigiVectorIcon
import com.digiwear.companion.data.PhoneDatabase
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.nfc.NfcDuelManager
import com.digiwear.companion.ui.BattleHubScreen
import com.digiwear.companion.ui.DigiVaultScreen
import com.digiwear.companion.ui.DigiWearPalette
import com.digiwear.companion.ui.PetDashboardScreen
import com.digiwear.companion.ui.SpriteStudioScreen
import kotlinx.coroutines.flow.MutableStateFlow

object NfcBattleBridge {
    val incomingCodeFlow = MutableStateFlow<String?>(null)
    val incomingWatchRelayFlow = MutableStateFlow<String?>(null)
    var pendingTagToWrite: Tag? = null
}

class MainActivity : ComponentActivity() {

    private lateinit var db: PhoneDatabase
    private lateinit var wearableManager: PhoneWearableManager
    private val relayListener = com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener { event ->
        if (event.path == "/sync/watch_acom_relay") {
            val digiRom = String(event.data, Charsets.UTF_8).trim()
            if (digiRom.isNotEmpty()) {
                NfcBattleBridge.incomingWatchRelayFlow.value = digiRom
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        @Suppress("DEPRECATION")
        window.statusBarColor = 0xFF0A0E14.toInt()

        db = PhoneDatabase.getInstance(this)
        wearableManager = PhoneWearableManager(this)
        // Load user-edited custom sprites at startup (off the main thread) so every screen and the
        // widget render them, not only after Sprite Studio has been opened.
        val appCtx = applicationContext
        Thread { com.digiwear.companion.render.PixelArtMatrices.loadCustomMatrices(appCtx) }.start()
        handleNfcIntent(intent)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = DigiWearPalette.DigiviceCyan,
                    onPrimary = Color.White,
                    secondary = DigiWearPalette.CrestGold,
                    onSecondary = Color.Black,
                    background = DigiWearPalette.CyberBg,
                    onBackground = Color.White,
                    surface = DigiWearPalette.CardSurface,
                    onSurface = Color.White
                )
            ) {
                MainPhoneApp(db, wearableManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            com.google.android.gms.wearable.Wearable.getMessageClient(this).addListener(relayListener)
        } catch (_: Exception) {}
        if (NfcDuelManager.isNfcEnabled(this)) {
            NfcDuelManager.enableReaderMode(this) { opponentCode, tag ->
                NfcBattleBridge.pendingTagToWrite = tag
                NfcBattleBridge.incomingCodeFlow.value = opponentCode
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            com.google.android.gms.wearable.Wearable.getMessageClient(this).removeListener(relayListener)
        } catch (_: Exception) {}
        NfcDuelManager.disableReaderMode(this)
        NfcBattleBridge.pendingTagToWrite = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val tag = androidx.core.content.IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag::class.java)
        if (tag != null) {
            NfcBattleBridge.pendingTagToWrite = tag
            val code = NfcDuelManager.extractCommCodeFromTag(tag)
            NfcBattleBridge.incomingCodeFlow.value = code
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainPhoneApp(
    db: PhoneDatabase,
    wearableManager: PhoneWearableManager
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color(0xFF00E5FF),
        selectedTextColor = Color(0xFF00E5FF),
        indicatorColor = Color(0xFF142D38),
        unselectedIconColor = Color(0xFF78909C),
        unselectedTextColor = Color(0xFF78909C)
    )

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DigiWearPalette.CyberBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color(0xFF101722))
                    .border(width = 1.dp, color = Color(0xFF1E2C3D))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DigiWear",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )

                IconButton(
                    onClick = { selectedTab = if (selectedTab == 4) 0 else 4 },
                    modifier = Modifier.size(32.dp)
                ) {
                    DigiVectorIcon(
                        type = DigiIconType.SETTINGS_GEAR,
                        size = 20.dp,
                        tint = if (selectedTab == 4) Color(0xFF00E5FF) else Color(0xFF90A4AE)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF101722)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        DigiVectorIcon(
                            type = DigiIconType.VPET_DEVICE,
                            size = 20.dp,
                            tint = if (selectedTab == 0) Color(0xFF00E5FF) else Color(0xFF78909C)
                        )
                    },
                    label = { Text("V-PET", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        DigiVectorIcon(
                            type = DigiIconType.VAULT_SAFE,
                            size = 20.dp,
                            tint = if (selectedTab == 1) Color(0xFF00E5FF) else Color(0xFF78909C)
                        )
                    },
                    label = { Text("VAULT", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        DigiVectorIcon(
                            type = DigiIconType.COMM_ANTENNA,
                            size = 20.dp,
                            tint = if (selectedTab == 2) Color(0xFF00E5FF) else Color(0xFF78909C)
                        )
                    },
                    label = { Text("D-COM", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        DigiVectorIcon(
                            type = DigiIconType.MAP_COMPASS,
                            size = 20.dp,
                            tint = if (selectedTab == 3) Color(0xFF00E5FF) else Color(0xFF78909C)
                        )
                    },
                    label = { Text("EXPEDITION", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                    colors = navItemColors
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> PetDashboardScreen(db, wearableManager, onNavigateToSettings = { selectedTab = 4 })
                1 -> DigiVaultScreen(db, wearableManager)
                2 -> BattleHubScreen(db, wearableManager)
                3 -> ExpeditionScreen(db, wearableManager)
                4 -> SettingsScreen(db, wearableManager)
            }
        }
    }
}

