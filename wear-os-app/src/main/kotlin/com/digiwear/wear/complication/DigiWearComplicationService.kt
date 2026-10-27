package com.digiwear.wear.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.digiwear.core.model.PetSpecies
import com.digiwear.wear.MainActivity
import com.digiwear.wear.data.PetRepository

/**
 * Watch Face Complication provider allowing users to display active Digimon metrics,
 * hunger hearts, and step progression on any third-party Wear OS watch face.
 */
class DigiWearComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                plainText("HGR 4/4"),
                plainText("DigiWear Hunger")
            ).setTitle(plainText("Agumon")).build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 2500f,
                min = 0f,
                max = 5000f,
                contentDescription = plainText("Steps")
            ).setText(plainText("2.5k")).build()

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val repo = PetRepository.getInstance(applicationContext)
        val pet = repo.getPetState()
        val species = PetSpecies.getById(pet.speciesId)
        val progress = repo.getAdventureProgress()

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    plainText("HGR ${pet.hunger}/4"),
                    plainText("Hunger")
                )
                    .setTitle(plainText(species.name))
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.RANGED_VALUE -> {
                val target = repo.dailyStepGoalFlow.value.toFloat().coerceAtLeast(1000f)
                val current = repo.getDailySteps().toFloat().coerceIn(0f, target)
                val stepLabel = if (current < 1000f) {
                    "${current.toInt()}"
                } else {
                    String.format(java.util.Locale.US, "%.1fk", current / 1000f)
                }
                RangedValueComplicationData.Builder(
                    value = current,
                    min = 0f,
                    max = target,
                    contentDescription = plainText("Daily Steps")
                )
                    .setText(plainText(stepLabel))
                    .setTitle(plainText("STEPS"))
                    .setTapAction(pendingIntent)
                    .build()
            }

            else -> ShortTextComplicationData.Builder(
                plainText(species.name),
                plainText("Digimon")
            ).setTapAction(pendingIntent).build()
        }
    }

    private fun plainText(text: String): PlainComplicationText =
        PlainComplicationText.Builder(text).build()

    companion object {
        fun requestUpdate(context: Context) {
            try {
                ComplicationDataSourceUpdateRequester.create(
                    context,
                    ComponentName(context, DigiWearComplicationService::class.java)
                ).requestUpdateAll()
            } catch (_: Exception) {
            }
        }
    }
}
