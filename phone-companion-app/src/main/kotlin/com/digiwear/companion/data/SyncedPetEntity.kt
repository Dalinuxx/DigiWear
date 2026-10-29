package com.digiwear.companion.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "synced_pet_history")
data class SyncedPetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val speciesId: Int,
    val speciesName: String,
    val currentHp: Int,
    val maxHp: Int,
    val hunger: Int,
    val strength: Int,
    val weight: Int,
    val ageSeconds: Long,
    val careMistakes: Int,
    val effort: Int,
    val overfeedCount: Int,
    val totalBattles: Int,
    val victories: Int,
    val wasteCount: Int = 0,
    val isSleeping: Boolean = false,
    val lightsOff: Boolean = false,
    val isFrozen: Boolean = false,
    val isInjured: Boolean = false,
    val lastProcessedTimestampUtc: Long = 0L,
    val attentionCallActive: Boolean = false,
    val attentionCallTimestampUtc: Long? = null,
    val hungerAccumulatedSeconds: Long = 0L,
    val strengthAccumulatedSeconds: Long = 0L,
    val wasteAccumulatedSeconds: Long = 0L,
    val generation: Int = 1,
    val lifespanDays: Int = 14,
    val isDeceased: Boolean = false,
    val traitedEggBonus: Int = 0,
    val vitalPoints: Int = 0,
    val syncedTimestampUtc: Long = System.currentTimeMillis() / 1000L
) {
    companion object {
        fun fromDomain(
            state: com.digiwear.core.model.PetState,
            speciesName: String = com.digiwear.core.model.PetSpecies.getById(state.speciesId).name,
            id: Long = 0
        ): SyncedPetEntity = SyncedPetEntity(
            id = id,
            speciesId = state.speciesId,
            speciesName = speciesName,
            currentHp = state.currentHp,
            maxHp = state.maxHp,
            hunger = state.hunger,
            strength = state.strength,
            weight = state.weight,
            ageSeconds = state.ageSeconds,
            careMistakes = state.careMistakes,
            effort = state.effort,
            overfeedCount = state.overfeedCount,
            totalBattles = state.totalBattles,
            victories = state.victories,
            wasteCount = state.wasteCount,
            isSleeping = state.isSleeping,
            lightsOff = state.lightsOff,
            isFrozen = state.isFrozen,
            isInjured = state.isInjured,
            lastProcessedTimestampUtc = state.lastProcessedTimestampUtc,
            attentionCallActive = state.attentionCallActive,
            attentionCallTimestampUtc = state.attentionCallTimestampUtc,
            hungerAccumulatedSeconds = state.hungerAccumulatedSeconds,
            strengthAccumulatedSeconds = state.strengthAccumulatedSeconds,
            wasteAccumulatedSeconds = state.wasteAccumulatedSeconds,
            generation = state.generation,
            lifespanDays = state.lifespanDays,
            isDeceased = state.isDeceased,
            traitedEggBonus = state.traitedEggBonus,
            vitalPoints = state.vitalPoints,
            syncedTimestampUtc = System.currentTimeMillis() / 1000L
        )
    }

    fun toDomain(): com.digiwear.core.model.PetState = com.digiwear.core.model.PetState(
        speciesId = speciesId,
        currentHp = currentHp,
        maxHp = maxHp,
        hunger = hunger,
        strength = strength,
        weight = weight,
        ageSeconds = ageSeconds,
        careMistakes = careMistakes,
        effort = effort,
        overfeedCount = overfeedCount,
        totalBattles = totalBattles,
        victories = victories,
        wasteCount = wasteCount,
        isSleeping = isSleeping,
        lightsOff = lightsOff,
        isFrozen = isFrozen,
        isInjured = isInjured,
        lastProcessedTimestampUtc = lastProcessedTimestampUtc,
        attentionCallActive = attentionCallActive,
        attentionCallTimestampUtc = attentionCallTimestampUtc,
        hungerAccumulatedSeconds = hungerAccumulatedSeconds,
        strengthAccumulatedSeconds = strengthAccumulatedSeconds,
        wasteAccumulatedSeconds = wasteAccumulatedSeconds,
        generation = generation,
        lifespanDays = lifespanDays,
        isDeceased = isDeceased,
        traitedEggBonus = traitedEggBonus,
        vitalPoints = vitalPoints
    )
}

@Serializable
@Entity(
    tableName = "companion_memorial_graveyard",
    indices = [Index(value = ["timestampUtc"])]
)
data class CompanionMemorialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val speciesId: Int,
    val speciesName: String,
    val customNickname: String = "",
    val generation: Int = 1,
    val finalStage: String = "",
    val ageDays: Int = 0,
    val battlesWon: Int = 0,
    val battlesTotal: Int = 0,
    val causeOfDeath: String = "OLD_AGE",
    val epitaph: String = "",
    val timestampUtc: Long = System.currentTimeMillis() / 1000L,
    val traitedEggBonusGiven: Int = 1
) {
    val winRatio: Float
        get() = if (battlesTotal > 0) battlesWon.toFloat() / battlesTotal else 0f

    fun toDomain(): com.digiwear.core.model.MemorialRecord = com.digiwear.core.model.MemorialRecord(
        id = id,
        speciesId = speciesId,
        speciesName = speciesName,
        customNickname = customNickname,
        generation = generation,
        finalStage = finalStage,
        ageDays = ageDays,
        battlesWon = battlesWon,
        battlesTotal = battlesTotal,
        causeOfDeath = causeOfDeath,
        epitaph = epitaph,
        timestampUtc = timestampUtc,
        traitedEggBonusGiven = traitedEggBonusGiven
    )

    companion object {
        fun fromDomain(record: com.digiwear.core.model.MemorialRecord): CompanionMemorialEntity = CompanionMemorialEntity(
            id = record.id,
            speciesId = record.speciesId,
            speciesName = record.speciesName,
            customNickname = record.customNickname,
            generation = record.generation,
            finalStage = record.finalStage,
            ageDays = record.ageDays,
            battlesWon = record.battlesWon,
            battlesTotal = record.battlesTotal,
            causeOfDeath = record.causeOfDeath,
            epitaph = record.epitaph,
            timestampUtc = record.timestampUtc,
            traitedEggBonusGiven = record.traitedEggBonusGiven
        )
    }
}

@Serializable
@Entity(tableName = "custom_campaigns")
data class CustomCampaignEntity(
    @PrimaryKey
    val campaignId: String,
    val title: String,
    val jsonContent: String,
    val updatedAtUtc: Long = System.currentTimeMillis() / 1000L
)

@Serializable
@Entity(
    tableName = "digi_vault",
    indices = [Index(value = ["storedTimestampUtc"])]
)
data class StoredDigimonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val speciesId: Int,
    val speciesName: String,
    val customNickname: String = "",
    val stageName: String,
    val attributeName: String,
    val currentHp: Int,
    val maxHp: Int,
    val hunger: Int,
    val strength: Int,
    val weight: Int,
    val ageSeconds: Long,
    val careMistakes: Int,
    val effort: Int,
    val overfeedCount: Int,
    val totalBattles: Int,
    val victories: Int,
    val wasteCount: Int = 0,
    val isInjured: Boolean = false,
    val generation: Int = 1,
    val lifespanDays: Int = 14,
    val traitedEggBonus: Int = 0,
    val vitalPoints: Int = 0,
    val storedTimestampUtc: Long = System.currentTimeMillis() / 1000L,
    val notes: String = ""
) {
    companion object {
        fun fromDomain(
            state: com.digiwear.core.model.PetState,
            species: com.digiwear.core.model.PetSpecies,
            nickname: String = "",
            id: Long = 0,
            notes: String = ""
        ): StoredDigimonEntity = StoredDigimonEntity(
            id = id,
            speciesId = state.speciesId,
            speciesName = species.name,
            customNickname = nickname,
            stageName = species.stage.displayName,
            attributeName = species.attribute.displayName,
            currentHp = state.currentHp,
            maxHp = state.maxHp,
            hunger = state.hunger,
            strength = state.strength,
            weight = state.weight,
            ageSeconds = state.ageSeconds,
            careMistakes = state.careMistakes,
            effort = state.effort,
            overfeedCount = state.overfeedCount,
            totalBattles = state.totalBattles,
            victories = state.victories,
            wasteCount = state.wasteCount,
            isInjured = state.isInjured,
            generation = state.generation,
            lifespanDays = state.lifespanDays,
            traitedEggBonus = state.traitedEggBonus,
            vitalPoints = state.vitalPoints,
            storedTimestampUtc = System.currentTimeMillis() / 1000L,
            notes = notes
        )
    }

    fun toDomain(): com.digiwear.core.model.PetState = com.digiwear.core.model.PetState(
        speciesId = speciesId,
        currentHp = currentHp,
        maxHp = maxHp,
        hunger = hunger,
        strength = strength,
        weight = weight,
        ageSeconds = ageSeconds,
        careMistakes = careMistakes,
        effort = effort,
        overfeedCount = overfeedCount,
        totalBattles = totalBattles,
        victories = victories,
        wasteCount = wasteCount,
        isSleeping = false,
        lightsOff = false,
        isFrozen = true, // Vaulted partners are in Cryo-Stasis stasis
        isInjured = isInjured,
        lastProcessedTimestampUtc = storedTimestampUtc,
        generation = generation,
        lifespanDays = lifespanDays,
        traitedEggBonus = traitedEggBonus,
        vitalPoints = vitalPoints
    )
}

@Serializable
@Entity(
    tableName = "workout_history",
    indices = [Index(value = ["timestampUtc"])]
)
data class WorkoutHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    val workoutType: String,
    val timestampUtc: Long = System.currentTimeMillis() / 1000L,
    val durationSeconds: Long,
    val avgBpm: Int,
    val peakBpm: Int,
    val calories: Int,
    val steps: Int,
    val evolutionMinutesAccelerated: Long = 0L,
    val syncedToHealthConnect: Boolean = false
)
