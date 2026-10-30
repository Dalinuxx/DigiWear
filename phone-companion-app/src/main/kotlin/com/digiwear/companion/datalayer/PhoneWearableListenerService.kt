package com.digiwear.companion.datalayer

import com.digiwear.companion.data.PhoneDatabase
import com.digiwear.companion.data.SyncedPetEntity
import com.digiwear.core.model.PetState
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Background listener service on the Android phone companion app.
 * Automatically receives replicated DataItems (e.g. /sync/active_pet_state)
 * and direct responses from the smartwatch, persisting them into Room DB
 * so the companion UI stays synchronized in real time.
 */
class PhoneWearableListenerService : WearableListenerService() {
    private val json = Json { ignoreUnknownKeys = true }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/sync/active_pet_state") {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val jsonStr = dataMapItem.dataMap.getString("pet_state_json")
                if (!jsonStr.isNullOrBlank()) {
                    runBlocking(Dispatchers.IO) {
                        try {
                            val state = json.decodeFromString<PetState>(jsonStr)
                            PhoneDatabase.getInstance(applicationContext).phoneDao().insertSnapshot(SyncedPetEntity.fromDomain(state))
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/sync/response_pet_state") {
            val jsonStr = String(messageEvent.data, Charsets.UTF_8)
            if (jsonStr.isNotBlank()) {
                runBlocking(Dispatchers.IO) {
                    try {
                        val state = json.decodeFromString<PetState>(jsonStr)
                        PhoneDatabase.getInstance(applicationContext).phoneDao().insertSnapshot(SyncedPetEntity.fromDomain(state))
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
