package com.digiwear.wear.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS adventure_state (
                id INTEGER PRIMARY KEY NOT NULL,
                campaignId TEXT NOT NULL,
                currentNodeIndex INTEGER NOT NULL,
                accumulatedStepsCurrentNode INTEGER NOT NULL,
                totalAdventureSteps INTEGER NOT NULL,
                clearedFlagsCsv TEXT NOT NULL,
                isCompleted INTEGER NOT NULL,
                unlockedEggsCsv TEXT NOT NULL,
                collectedItemsCsv TEXT NOT NULL,
                unlockedCampaignsCsv TEXT NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS memorial_graveyard (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                speciesId INTEGER NOT NULL,
                speciesName TEXT NOT NULL,
                customNickname TEXT NOT NULL,
                generation INTEGER NOT NULL,
                finalStage TEXT NOT NULL,
                ageDays INTEGER NOT NULL,
                battlesWon INTEGER NOT NULL,
                battlesTotal INTEGER NOT NULL,
                causeOfDeath TEXT NOT NULL,
                epitaph TEXT NOT NULL,
                timestampUtc INTEGER NOT NULL,
                traitedEggBonusGiven INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE adventure_state ADD COLUMN highestUnlockedNodeIndex INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE adventure_state ADD COLUMN sagaStatesJson TEXT NOT NULL DEFAULT '{}'")
    }
}

@Dao
interface PetDao {
    @Query("SELECT * FROM pet_state WHERE id = 1 LIMIT 1")
    fun getPetStateFlow(): Flow<PetEntity?>

    @Query("SELECT * FROM pet_state WHERE id = 1 LIMIT 1")
    suspend fun getPetState(): PetEntity?

    @Query("SELECT * FROM pet_state WHERE id = 2 LIMIT 1")
    fun getBackupPetStateFlow(): Flow<PetEntity?>

    @Query("SELECT * FROM pet_state WHERE id = 2 LIMIT 1")
    suspend fun getBackupPetState(): PetEntity?

    @Query("DELETE FROM pet_state WHERE id = 1")
    suspend fun deleteActivePet()

    @Query("DELETE FROM pet_state WHERE id = 2")
    suspend fun deleteBackupPet()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePet(pet: PetEntity)

    @Query("SELECT * FROM adventure_state WHERE id = 1 LIMIT 1")
    fun getAdventureStateFlow(): Flow<AdventureEntity?>

    @Query("SELECT * FROM adventure_state WHERE id = 1 LIMIT 1")
    suspend fun getAdventureState(): AdventureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAdventure(adventure: AdventureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemorial(memorial: MemorialEntity): Long

    @Query("SELECT * FROM memorial_graveyard ORDER BY timestampUtc DESC")
    fun getAllMemorialsFlow(): Flow<List<MemorialEntity>>

    @Query("SELECT * FROM memorial_graveyard ORDER BY timestampUtc DESC")
    suspend fun getAllMemorials(): List<MemorialEntity>
}

@Database(entities = [PetEntity::class, AdventureEntity::class, MemorialEntity::class], version = 4, exportSchema = false)
abstract class DigiWearDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
}
