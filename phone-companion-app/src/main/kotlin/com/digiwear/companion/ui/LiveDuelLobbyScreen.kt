package com.digiwear.companion.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.multiplayer.LiveDuelLobbyManager
import com.digiwear.companion.multiplayer.LobbyState
import com.digiwear.companion.render.SpriteCanvasRenderer
import com.digiwear.core.model.BattleFighter
import com.digiwear.core.model.BattleMode
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.SpriteFrame
import kotlinx.coroutines.launch

@Composable
fun LiveDuelLobbyScreen(
    wearableManager: PhoneWearableManager,
    activeFighter: BattleFighter?,
    onLaunchArena: (opponentFighter: BattleFighter, roomCode: String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val lobbyManager = remember { LiveDuelLobbyManager(scope) }
    val lobbyState by lobbyManager.lobbyState.collectAsState()
    val currentRoomId by lobbyManager.currentRoomId.collectAsState()
    val isHost by lobbyManager.isHost.collectAsState()
    val opponentFighter by lobbyManager.opponentFighter.collectAsState()
    val opponentTamerTag by lobbyManager.opponentTamerTag.collectAsState()
    val statusMessage by lobbyManager.statusMessage.collectAsState()
    val errorMessage by lobbyManager.errorMessage.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }
    var configuredUrl by remember { mutableStateOf(wearableManager.getRelayServerUrl()) }
    var configuredKey by remember { mutableStateOf(wearableManager.getRelayServerKey()) }
    var joinRoomCodeInput by remember { mutableStateOf("") }
    var myTamerTag by remember { mutableStateOf("Tamer_${activeFighter?.speciesId ?: 101}") }

    // Clean up websocket on leaving screen
    DisposableEffect(Unit) {
        onDispose {
            lobbyManager.disconnect()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Header: Live Relay Status & Server Config Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val statusDotColor = when (lobbyState) {
                        LobbyState.CONNECTED, LobbyState.WAITING_FOR_OPPONENT, LobbyState.OPPONENT_FOUND, LobbyState.DUEL_READY -> Color(0xFF00E676)
                        LobbyState.CONNECTING, LobbyState.SECURITY_HANDSHAKE -> Color(0xFFFFD54F)
                        LobbyState.ERROR -> Color(0xFFFF5252)
                        LobbyState.DISCONNECTED -> Color.Gray
                    }
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(statusDotColor, CircleShape)
                    )
                    Text(
                        text = "LIVE RELAY: ${lobbyState.name}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusDotColor,
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedButton(
                    onClick = { showConfigDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DigiWearPalette.DigiviceCyan),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DigiVectorIcon(type = DigiIconType.SETTINGS_GEAR, size = 11.dp, tint = DigiWearPalette.DigiviceCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SERVER KEY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = DigiWearPalette.DigiviceCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Host URL: $configuredUrl",
                fontSize = 8.5.sp,
                color = Color(0xFF78909C),
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = "▸ $statusMessage",
                fontSize = 9.5.sp,
                color = if (errorMessage != null) Color(0xFFFF8A80) else Color(0xFF80D8FF),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (activeFighter == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141C24), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No partner selected. Incubate or sync a partner above to enter Live Duels.",
                        fontSize = 9.5.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (lobbyState == LobbyState.DISCONNECTED || lobbyState == LobbyState.ERROR) {
                // Connect Action
                Button(
                    onClick = {
                        lobbyManager.connect(configuredUrl, configuredKey)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DigiWearPalette.DigiviceCyan)
                ) {
                    DigiVectorIcon(type = DigiIconType.BEAM_ANTENNA, size = 13.dp, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONNECT TO LIVE RELAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )
                }
            } else if (currentRoomId == null) {
                // In Lobby: Create or Join Room
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            lobbyManager.createRoom(activeFighter, myTamerTag)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text(
                            text = "HOST ROOM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            lobbyManager.disconnect()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF546E7A))
                    ) {
                        Text(
                            text = "DISCONNECT",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Join Existing Room
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = joinRoomCodeInput,
                        onValueChange = { if (it.length <= 6) joinRoomCodeInput = it.uppercase() },
                        placeholder = { Text("6-CHAR CODE", fontSize = 10.sp, color = Color.DarkGray) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DigiWearPalette.DigiviceCyan,
                            unfocusedBorderColor = Color(0xFF37474F)
                        )
                    )

                    Button(
                        onClick = {
                            if (joinRoomCodeInput.isNotBlank()) {
                                lobbyManager.joinRoom(joinRoomCodeInput, activeFighter, myTamerTag)
                            }
                        },
                        enabled = joinRoomCodeInput.length >= 4,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DigiWearPalette.DigiviceCyan)
                    ) {
                        Text(
                            text = "JOIN",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }
                }
            } else {
                // Room Active State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF101722), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF1E2C3D), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isHost) "DUEL ROOM CODE (HOST)" else "JOINED ROOM CODE",
                        fontSize = 8.5.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currentRoomId ?: "------",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            color = DigiWearPalette.CrestGold,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedButton(
                            onClick = {
                                currentRoomId?.let {
                                    clipboard.setText(AnnotatedString(it))
                                    Toast.makeText(context, "Room code $it copied!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("COPY", fontSize = 9.sp, color = DigiWearPalette.CrestGold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (opponentFighter == null) {
                        Text(
                            text = "Awaiting challenger connection... Share code above with opponent.",
                            fontSize = 9.5.sp,
                            color = Color(0xFFFFD54F),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        val opp = opponentFighter!!
                        val oppSpecies = runCatching { PetSpecies.getById(opp.speciesId) }.getOrNull()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F251A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF00E676), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpriteCanvasRenderer(
                                        speciesId = opp.speciesId,
                                        frame = SpriteFrame.IDLE_A,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Column {
                                        Text(
                                            text = opponentTamerTag ?: "Challenger",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "#${opp.speciesId} ${opp.name} • ${opp.stage.displayName}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF80D8FF),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "HP: ${opp.maxHp} | POWER: ${opp.basePower} | WINS: ${opp.victories}",
                                            fontSize = 8.5.sp,
                                            color = Color.LightGray,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF00E676), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("VERIFIED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                onLaunchArena(opp, currentRoomId ?: "")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                        ) {
                            DigiVectorIcon(type = DigiIconType.BATTLE, size = 13.dp, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ENTER LIVE DUEL ARENA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { lobbyManager.leaveRoom() }) {
                        Text("LEAVE ROOM", fontSize = 9.sp, color = Color(0xFFFF8A80), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }

    // Server Configuration & Security Key Dialog
    if (showConfigDialog) {
        var tempUrl by remember { mutableStateOf(configuredUrl) }
        var tempKey by remember { mutableStateOf(configuredKey) }
        var testResult by remember { mutableStateOf<String?>(null) }
        var isTesting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Text(
                    text = "LIVE RELAY SECURITY CONFIG",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = DigiWearPalette.DigiviceCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your private relay host and password are stored securely on your phone and NEVER committed or leaked to GitHub.",
                        fontSize = 9.sp,
                        color = Color.LightGray,
                        lineHeight = 13.sp
                    )

                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Relay WebSocket URL", fontSize = 10.sp) },
                        placeholder = { Text("wss://relay.yourdomain.com:8080", fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("Pre-Shared Server Key (DIGIWEAR_SERVER_KEY)", fontSize = 10.sp) },
                        placeholder = { Text("Leave blank if server auth disabled", fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    testResult?.let {
                        Text(
                            text = it,
                            fontSize = 9.sp,
                            color = if (it.startsWith("OK")) Color(0xFF00E676) else Color(0xFFFF8A80),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            isTesting = true
                            testResult = "Testing connection to $tempUrl..."
                            lobbyManager.connect(tempUrl, tempKey) {
                                testResult = "OK: Connected and authenticated successfully!"
                                isTesting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                    ) {
                        Text("TEST HANDSHAKE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = DigiWearPalette.DigiviceCyan)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        wearableManager.setRelayServerUrl(tempUrl)
                        wearableManager.setRelayServerKey(tempKey)
                        configuredUrl = tempUrl
                        configuredKey = tempKey
                        showConfigDialog = false
                        Toast.makeText(context, "Saved Relay Server configuration!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DigiWearPalette.DigiviceCyan)
                ) {
                    Text("SAVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("CANCEL", fontSize = 10.sp, color = Color.Gray)
                }
            },
            containerColor = Color(0xFF141C24)
        )
    }
}
