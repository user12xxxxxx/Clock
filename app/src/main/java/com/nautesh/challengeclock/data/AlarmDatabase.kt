package com.nautesh.challengeclock.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAll(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms")
    suspend fun getAll(): List<Alarm>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun get(id: Long): Alarm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(alarm: Alarm): Long

    @Delete
    suspend fun delete(alarm: Alarm)

    @Query("UPDATE alarms SET snoozedUntil = :at, snoozeCount = :count WHERE id = :id")
    suspend fun setSnooze(id: Long, at: Long, count: Int)
}

@Dao
interface CountdownDao {
    @Query("SELECT * FROM timers ORDER BY id DESC")
    fun observeAll(): Flow<List<Countdown>>

    @Query("SELECT * FROM timers")
    suspend fun getAll(): List<Countdown>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun get(id: Long): Countdown?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(timer: Countdown): Long

    @Delete
    suspend fun delete(timer: Countdown)
}

// ponytail: exportSchema off; turn on (and keep the JSON in git) before the first public release
@Database(entities = [Alarm::class, Countdown::class], version = 5, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao
    abstract fun timers(): CountdownDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `timers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`label` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, `remainingMs` INTEGER NOT NULL, " +
                        "`endAt` INTEGER NOT NULL, `running` INTEGER NOT NULL)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN sound TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeLimit INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozedUntil INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timers ADD COLUMN endElapsed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE timers ADD COLUMN boot INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
