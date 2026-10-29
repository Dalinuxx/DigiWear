package com.digiwear.companion.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.digiwear.companion.BuildConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneDao {
    @Query("SELECT * FROM synced_pet_history ORDER BY id DESC LIMIT 50")
    fun getAllSnapshotsFlow(): Flow<List<SyncedPetEntity>>

    @Query("SELECT * FROM synced_pet_history ORDER BY id DESC LIMIT 1")
    suspend fun getLatestSnapshot(): SyncedPetEntity?

    @Query("DELETE FROM synced_pet_history")
    suspend fun clearActivePet()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshotInternal(snapshot: SyncedPetEntity)

    @Query("DELETE FROM synced_pet_history WHERE id NOT IN (SELECT id FROM synced_pet_history ORDER BY id DESC LIMIT 50)")
    suspend fun pruneOldSnapshots()

    @Transaction
    suspend fun insertSnapshot(snapshot: SyncedPetEntity) {
        insertSnapshotInternal(snapshot)
        pruneOldSnapshots()
        recordEncounter(snapshot.speciesId, isRaised = true)
    }

    @Query("SELECT * FROM custom_campaigns")
    fun getAllCampaignsFlow(): Flow<List<CustomCampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCampaign(campaign: CustomCampaignEntity)

    @Query("DELETE FROM custom_campaigns WHERE campaignId = :campaignId")
    suspend fun deleteCampaign(campaignId: String)

    @Query("SELECT * FROM digi_vault ORDER BY storedTimestampUtc DESC")
    fun getAllVaultPetsFlow(): Flow<List<StoredDigimonEntity>>

    @Query("SELECT * FROM digi_vault ORDER BY storedTimestampUtc DESC")
    suspend fun getAllVaultPets(): List<StoredDigimonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultPetInternal(pet: StoredDigimonEntity): Long

    @Transaction
    suspend fun insertVaultPet(pet: StoredDigimonEntity): Long {
        val id = insertVaultPetInternal(pet)
        recordEncounter(pet.speciesId, isRaised = true)
        return id
    }

    @Query("DELETE FROM digi_vault WHERE id = :id")
    suspend fun deleteVaultPet(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemorial(memorial: CompanionMemorialEntity): Long

    @Query("SELECT * FROM companion_memorial_graveyard ORDER BY timestampUtc DESC")
    fun getAllMemorialsFlow(): Flow<List<CompanionMemorialEntity>>

    @Query("SELECT * FROM companion_memorial_graveyard ORDER BY timestampUtc DESC")
    suspend fun getAllMemorials(): List<CompanionMemorialEntity>

    // 550-Species DigiDex Discovery Tracking
    @Query("SELECT * FROM discovered_species")
    fun getAllDiscoveredFlow(): Flow<List<DiscoveredSpeciesEntity>>

    @Query("SELECT * FROM discovered_species")
    suspend fun getAllDiscovered(): List<DiscoveredSpeciesEntity>

    @Query("SELECT * FROM discovered_species WHERE speciesId = :speciesId LIMIT 1")
    suspend fun getDiscovered(speciesId: Int): DiscoveredSpeciesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordDiscovery(entity: DiscoveredSpeciesEntity)

    suspend fun recordEncounter(speciesId: Int, isRaised: Boolean = false, isDefeated: Boolean = false) {
        val existing = getDiscovered(speciesId)
        val now = System.currentTimeMillis() / 1000L
        if (existing == null) {
            recordDiscovery(
                DiscoveredSpeciesEntity(
                    speciesId = speciesId,
                    discoveredTimestampUtc = now,
                    timesRaised = if (isRaised) 1 else 0,
                    timesDefeated = if (isDefeated) 1 else 0
                )
            )
        } else {
            recordDiscovery(
                existing.copy(
                    timesRaised = existing.timesRaised + (if (isRaised) 1 else 0),
                    timesDefeated = existing.timesDefeated + (if (isDefeated) 1 else 0)
                )
            )
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS custom_campaigns (
                campaignId TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                jsonContent TEXT NOT NULL,
                updatedAtUtc INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS digi_vault (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                speciesId INTEGER NOT NULL,
                speciesName TEXT NOT NULL,
                customNickname TEXT NOT NULL,
                stageName TEXT NOT NULL,
                attributeName TEXT NOT NULL,
                currentHp INTEGER NOT NULL,
                maxHp INTEGER NOT NULL,
                hunger INTEGER NOT NULL,
                strength INTEGER NOT NULL,
                weight INTEGER NOT NULL,
                ageSeconds INTEGER NOT NULL,
                careMistakes INTEGER NOT NULL,
                effort INTEGER NOT NULL,
                overfeedCount INTEGER NOT NULL,
                totalBattles INTEGER NOT NULL,
                victories INTEGER NOT NULL,
                wasteCount INTEGER NOT NULL,
                isInjured INTEGER NOT NULL,
                generation INTEGER NOT NULL,
                lifespanDays INTEGER NOT NULL,
                traitedEggBonus INTEGER NOT NULL,
                vitalPoints INTEGER NOT NULL,
                storedTimestampUtc INTEGER NOT NULL,
                notes TEXT NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS companion_memorial_graveyard (
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
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS discovered_species (
                speciesId INTEGER NOT NULL PRIMARY KEY,
                discoveredTimestampUtc INTEGER NOT NULL,
                timesRaised INTEGER NOT NULL,
                timesDefeated INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_digi_vault_storedTimestampUtc ON digi_vault(storedTimestampUtc)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_companion_memorial_graveyard_timestampUtc ON companion_memorial_graveyard(timestampUtc)")
    }
}

@Database(
    entities = [
        SyncedPetEntity::class,
        CustomCampaignEntity::class,
        StoredDigimonEntity::class,
        CompanionMemorialEntity::class,
        DiscoveredSpeciesEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class PhoneDatabase : RoomDatabase() {
    abstract fun phoneDao(): PhoneDao

    companion object {
        @Volatile
        private var instance: PhoneDatabase? = null

        fun getInstance(context: Context): PhoneDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PhoneDatabase::class.java,
                    "digiwear_companion.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .apply {
                        if (BuildConfig.DEBUG) {
                            fallbackToDestructiveMigration()
                        }
                    }
                    .build().also { instance = it }
            }
        }
    }
}
