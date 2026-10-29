package com.digiwear.companion.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digiwear.companion.data.CustomCampaignEntity
import com.digiwear.companion.data.PhoneDatabase
import com.digiwear.companion.data.CompanionMemorialEntity
import com.digiwear.companion.data.StoredDigimonEntity
import com.digiwear.companion.data.SyncedPetEntity
import com.digiwear.companion.datalayer.PhoneWearableManager
import com.digiwear.companion.render.SpriteCanvasRenderer
import com.digiwear.companion.widget.DigiWearAppWidgetProvider
import com.digiwear.core.model.PetSpecies
import com.digiwear.core.model.PetState
import com.digiwear.core.model.SpriteFrame
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class FullCompanionArchive(
    val activePet: PetState = PetState(),
    val vaultPets: List<StoredDigimonEntity> = emptyList(),
    val memorials: List<CompanionMemorialEntity> = emptyList(),
    val customCampaigns: List<CustomCampaignEntity> = emptyList(),
    val exportedAtUtc: Long = System.currentTimeMillis() / 1000L
)

@Composable
fun BackupRestoreScreen(
    db: PhoneDatabase,
    wearableManager: PhoneWearableManager
) {
    val snapshots by db.phoneDao().getAllSnapshotsFlow().collectAsState(initial = emptyList())
    val vaultPets by db.phoneDao().getAllVaultPetsFlow().collectAsState(initial = emptyList())
    val memorials by db.phoneDao().getAllMemorialsFlow().collectAsState(initial = emptyList())
    val customCampaigns by db.phoneDao().getAllCampaignsFlow().collectAsState(initial = emptyList())
    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var jsonExportString by remember { mutableStateOf(json.encodeToString(PetState())) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snapshots, vaultPets) {
        val latest = snapshots.firstOrNull() ?: db.phoneDao().getLatestSnapshot()
        if (latest != null) {
            val loadedState = latest.toDomain()
            jsonExportString = json.encodeToString(loadedState)
        } else if (vaultPets.isNotEmpty()) {
            jsonExportString = json.encodeToString(vaultPets.first().toDomain())
        }
    }

    val persistStateSnapshot: suspend (PetState) -> Unit = { state ->
        db.phoneDao().insertSnapshot(SyncedPetEntity.fromDomain(state))
        DigiWearAppWidgetProvider.updateAllWidgets(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DigiWearPalette.CyberBg)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DigiSectionHeader(
            tag = "SYSTEM ARCHIVE // BACKUP & RECOVERY",
            title = "State Persistence & JSON",
            subtitle = "Room database snapshot history, Vault archive & unified JSON serialization",
            accentColor = Color(0xFFCE93D8)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Synced Snapshots History
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "▸ SYNCED WATCH STATE HISTORY (${snapshots.size} SNAPSHOTS • ${vaultPets.size} VAULT)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE1BEE7),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (snapshots.isEmpty() && vaultPets.isEmpty()) {
                    Text(
                        text = "No saved snapshots yet. Sync with watch on dashboard to create backups.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                } else if (snapshots.isNotEmpty()) {
                    snapshots.take(5).forEach { snap ->
                        val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(snap.syncedTimestampUtc * 1000L))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF222E3C), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SpriteCanvasRenderer(
                                    speciesId = snap.speciesId,
                                    frame = SpriteFrame.IDLE_A,
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "#${snap.speciesId} ${snap.speciesName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Wt: ${snap.weight}g • CM: ${snap.careMistakes} • Wins: ${snap.victories}/${snap.totalBattles}",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = dateStr,
                                fontSize = 10.sp,
                                color = Color(0xFFCE93D8),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    vaultPets.take(5).forEach { vPet ->
                        val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(vPet.storedTimestampUtc * 1000L))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(DigiWearPalette.CardInner, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF222E3C), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SpriteCanvasRenderer(
                                    speciesId = vPet.speciesId,
                                    frame = SpriteFrame.IDLE_A,
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "#${vPet.speciesId} ${vPet.speciesName} [VAULT]",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Wt: ${vPet.weight}g • CM: ${vPet.careMistakes} • Wins: ${vPet.victories}/${vPet.totalBattles}",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = dateStr,
                                fontSize = 10.sp,
                                color = Color(0xFF80CBC4),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // JSON Backup Contract
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DigiWearPalette.CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DigiWearPalette.CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "▸ UNIFIED JSON STATE CONTRACT",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCE93D8),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = {
                                val latest = snapshots.firstOrNull()
                                val activePet = latest?.toDomain() ?: vaultPets.firstOrNull()?.toDomain() ?: PetState()
                                val archive = FullCompanionArchive(
                                    activePet = activePet,
                                    vaultPets = vaultPets.map { it.copy(id = 0) },
                                    memorials = memorials.map { it.copy(id = 0) },
                                    customCampaigns = customCampaigns
                                )
                                jsonExportString = json.encodeToString(archive)
                                statusMessage = "Generated Full Companion Archive (${vaultPets.size} Vault pets, ${memorials.size} Memorials, ${customCampaigns.size} Campaigns)!"
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("FULL ARCHIVE", color = Color.White, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(jsonExportString))
                                Toast.makeText(context, "JSON Backup copied!", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            DigiVectorIcon(type = DigiIconType.COPY, size = 12.dp, tint = DigiWearPalette.DigiviceCyan)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("COPY", color = Color.White, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = jsonExportString,
                    onValueChange = { jsonExportString = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    if (jsonExportString.contains("\"vaultPets\"") || jsonExportString.contains("\"activePet\"")) {
                                        val archive = json.decodeFromString<FullCompanionArchive>(jsonExportString)
                                        persistStateSnapshot(archive.activePet)
                                        archive.vaultPets.forEach { db.phoneDao().insertVaultPet(it.copy(id = 0)) }
                                        archive.memorials.forEach { db.phoneDao().insertMemorial(it.copy(id = 0)) }
                                        archive.customCampaigns.forEach { db.phoneDao().insertOrUpdateCampaign(it) }
                                        val ok = wearableManager.sendPetStateToWatch(archive.activePet)
                                        statusMessage = if (ok) {
                                            "Restored Full Archive (${archive.vaultPets.size} Vault, ${archive.memorials.size} Memorials) + Synced Watch!"
                                        } else {
                                            "Restored Full Archive locally (${archive.vaultPets.size} Vault, ${archive.memorials.size} Memorials)!"
                                        }
                                    } else {
                                        val state = json.decodeFromString<PetState>(jsonExportString)
                                        persistStateSnapshot(state)
                                        val ok = wearableManager.sendPetStateToWatch(state)
                                        statusMessage = if (ok) "Restored state to Phone & Smartwatch!" else "Saved locally to Phone & Widget (Watch offline)"
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "Invalid JSON: ${e.message}"
                                }
                            }
                        }
                    ) {
                        Text("RESTORE BACKUP", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            val botamon = PetSpecies.getById(101)
                            val eggState = PetState(
                                speciesId = 101,
                                currentHp = botamon.baseHp,
                                maxHp = botamon.baseHp,
                                weight = botamon.baselineWeight,
                                ageSeconds = 0L
                            )
                            jsonExportString = json.encodeToString(eggState)
                            scope.launch {
                                persistStateSnapshot(eggState)
                                wearableManager.sendPetStateToWatch(eggState)
                                statusMessage = "New Digi-Egg (Botamon) hatched & saved!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA), contentColor = Color.White)
                    ) {
                        Text("HATCH NEW EGG", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        statusMessage?.let {
            Text(
                text = "▸ $it",
                fontSize = 11.sp,
                color = Color(0xFF00E676),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
