package com.digiwear.companion

import androidx.sqlite.db.SupportSQLiteDatabase
import com.digiwear.companion.data.MIGRATION_1_2
import com.digiwear.companion.data.MIGRATION_2_3
import com.digiwear.companion.data.MIGRATION_3_4
import com.digiwear.companion.data.MIGRATION_4_5
import com.digiwear.companion.data.MIGRATION_5_6
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class PhoneDatabaseMigrationTest {

    private fun createRecordingDb(executedSql: MutableList<String>): SupportSQLiteDatabase {
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase
    }

    @Test
    fun testMigrationVersionSequencing() {
        assertEquals("MIGRATION_1_2 must transition v1 to v2", 1, MIGRATION_1_2.startVersion)
        assertEquals("MIGRATION_1_2 must transition v1 to v2", 2, MIGRATION_1_2.endVersion)

        assertEquals("MIGRATION_2_3 must transition v2 to v3", 2, MIGRATION_2_3.startVersion)
        assertEquals("MIGRATION_2_3 must transition v2 to v3", 3, MIGRATION_2_3.endVersion)

        assertEquals("MIGRATION_3_4 must transition v3 to v4", 3, MIGRATION_3_4.startVersion)
        assertEquals("MIGRATION_3_4 must transition v3 to v4", 4, MIGRATION_3_4.endVersion)

        assertEquals("MIGRATION_4_5 must transition v4 to v5", 4, MIGRATION_4_5.startVersion)
        assertEquals("MIGRATION_4_5 must transition v4 to v5", 5, MIGRATION_4_5.endVersion)

        assertEquals("MIGRATION_5_6 must transition v5 to v6", 5, MIGRATION_5_6.startVersion)
        assertEquals("MIGRATION_5_6 must transition v5 to v6", 6, MIGRATION_5_6.endVersion)
    }

    @Test
    fun testMigration1To2CreatesCustomCampaignsAndDigiVault() {
        val sqlList = mutableListOf<String>()
        val db = createRecordingDb(sqlList)

        MIGRATION_1_2.migrate(db)

        assertEquals(2, sqlList.size)
        assertTrue("Must create custom_campaigns table", sqlList[0].contains("custom_campaigns"))
        assertTrue("custom_campaigns must have campaignId primary key", sqlList[0].contains("campaignId TEXT NOT NULL PRIMARY KEY"))

        assertTrue("Must create digi_vault table", sqlList[1].contains("digi_vault"))
        assertTrue("digi_vault must have autoincrement id", sqlList[1].contains("id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue("digi_vault must contain storedTimestampUtc", sqlList[1].contains("storedTimestampUtc INTEGER NOT NULL"))
    }

    @Test
    fun testMigration2To3CreatesCompanionMemorialGraveyard() {
        val sqlList = mutableListOf<String>()
        val db = createRecordingDb(sqlList)

        MIGRATION_2_3.migrate(db)

        assertEquals(1, sqlList.size)
        assertTrue("Must create companion_memorial_graveyard", sqlList[0].contains("companion_memorial_graveyard"))
        assertTrue("companion_memorial_graveyard must have autoincrement id", sqlList[0].contains("id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue("companion_memorial_graveyard must contain timestampUtc", sqlList[0].contains("timestampUtc INTEGER NOT NULL"))
    }

    @Test
    fun testMigration3To4CreatesDiscoveredSpecies() {
        val sqlList = mutableListOf<String>()
        val db = createRecordingDb(sqlList)

        MIGRATION_3_4.migrate(db)

        assertEquals(1, sqlList.size)
        assertTrue("Must create discovered_species table", sqlList[0].contains("discovered_species"))
        assertTrue("discovered_species must have speciesId primary key", sqlList[0].contains("speciesId INTEGER NOT NULL PRIMARY KEY"))
        assertTrue("discovered_species must have timesRaised", sqlList[0].contains("timesRaised INTEGER NOT NULL"))
        assertTrue("discovered_species must have timesDefeated", sqlList[0].contains("timesDefeated INTEGER NOT NULL"))
    }

    @Test
    fun testMigration4To5CreatesExpectedIndicesMatchingRoomSchema() {
        val sqlList = mutableListOf<String>()
        val db = createRecordingDb(sqlList)

        MIGRATION_4_5.migrate(db)

        assertEquals(2, sqlList.size)
        assertTrue(
            "Must create index_digi_vault_storedTimestampUtc",
            sqlList[0].contains("index_digi_vault_storedTimestampUtc") && sqlList[0].contains("digi_vault(storedTimestampUtc)")
        )
        assertTrue(
            "Must create index_companion_memorial_graveyard_timestampUtc",
            sqlList[1].contains("index_companion_memorial_graveyard_timestampUtc") && sqlList[1].contains("companion_memorial_graveyard(timestampUtc)")
        )
    }

    @Test
    fun testMigration5To6CreatesWorkoutHistoryTableAndIndex() {
        val sqlList = mutableListOf<String>()
        val db = createRecordingDb(sqlList)

        MIGRATION_5_6.migrate(db)

        assertEquals(2, sqlList.size)
        assertTrue("Must create workout_history table", sqlList[0].contains("workout_history"))
        assertTrue("workout_history must have sessionId primary key", sqlList[0].contains("sessionId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue("workout_history must have workoutType", sqlList[0].contains("workoutType TEXT NOT NULL"))
        assertTrue("workout_history must have evolutionMinutesAccelerated", sqlList[0].contains("evolutionMinutesAccelerated INTEGER NOT NULL DEFAULT 0"))
        assertTrue("workout_history must have syncedToHealthConnect", sqlList[0].contains("syncedToHealthConnect INTEGER NOT NULL DEFAULT 0"))
        assertTrue(
            "Must create index_workout_history_timestampUtc",
            sqlList[1].contains("index_workout_history_timestampUtc") && sqlList[1].contains("workout_history(timestampUtc)")
        )
    }
}
