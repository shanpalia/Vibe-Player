package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "video_records")
data class VideoRecordEntity(
    @PrimaryKey
    val uri: String,
    val title: String = "",
    val durationMs: Long = 0L,
    val lastPositionMs: Long = 0L,
    val lastPlayedTimestamp: Long = 0L,
    val isFavorite: Boolean = false,
    val audioDelayMs: Long = 0L,
    val subtitleDelayMs: Long = 0L
)

@Entity(tableName = "app_settings")
data class SettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

@Dao
interface VideoRecordDao {
    @Query("SELECT * FROM video_records")
    fun getAllRecords(): Flow<List<VideoRecordEntity>>

    @Query("SELECT * FROM video_records WHERE uri = :uri LIMIT 1")
    fun getRecordByUri(uri: String): Flow<VideoRecordEntity?>

    @Query("SELECT * FROM video_records WHERE uri = :uri LIMIT 1")
    suspend fun getRecordByUriSync(uri: String): VideoRecordEntity?

    @Query("SELECT * FROM video_records WHERE isFavorite = 1")
    fun getFavoriteRecords(): Flow<List<VideoRecordEntity>>

    @Query("SELECT * FROM video_records WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC")
    fun getRecentlyPlayedRecords(): Flow<List<VideoRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: VideoRecordEntity)

    @Query("UPDATE video_records SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun setFavorite(uri: String, isFavorite: Boolean)

    @Query("UPDATE video_records SET audioDelayMs = :audioDelayMs, subtitleDelayMs = :subtitleDelayMs WHERE uri = :uri")
    suspend fun updateDelays(uri: String, audioDelayMs: Long, subtitleDelayMs: Long)

    @Query("DELETE FROM video_records WHERE uri = :uri")
    suspend fun deleteRecord(uri: String)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<SettingEntity>>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingEntity)
}

@Database(
    entities = [VideoRecordEntity::class, SettingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VibePlayerDatabase : RoomDatabase() {
    abstract fun videoRecordDao(): VideoRecordDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: VibePlayerDatabase? = null

        fun getDatabase(context: Context): VibePlayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VibePlayerDatabase::class.java,
                    "vibe_player_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
