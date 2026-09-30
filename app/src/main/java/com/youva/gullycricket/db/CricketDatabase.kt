package com.youva.gullycricket.db

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
import com.youva.gullycricket.model.GullyCricketGame
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cricket_game_state")
data class CricketGameStateEntity(
    @PrimaryKey val id: Int = 1,
    val gameStateJson: String
)

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey val matchId: String,
    val timestamp: Long,
    val formattedDate: String,
    val teamAName: String,
    val teamBName: String,
    val totalOvers: Int,
    val teamAScore: Int,
    val teamAWickets: Int,
    val teamAOversStr: String,
    val teamBScore: Int,
    val teamBWickets: Int,
    val teamBOversStr: String,
    val resultSummary: String,
    val isCompleted: Boolean,
    val gameJson: String
)

@Dao
interface CricketGameStateDao {
    @Query("SELECT * FROM cricket_game_state WHERE id = 1 LIMIT 1")
    suspend fun getGameState(): CricketGameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameState(state: CricketGameStateEntity)

    @Query("DELETE FROM cricket_game_state WHERE id = 1")
    suspend fun deleteGameState()
}

@Dao
interface MatchHistoryDao {
    @Query("SELECT matchId, timestamp, formattedDate, teamAName, teamBName, totalOvers, teamAScore, teamAWickets, teamAOversStr, teamBScore, teamBWickets, teamBOversStr, resultSummary, isCompleted, '' AS gameJson FROM match_history ORDER BY timestamp DESC")
    fun getAllMatchesFlow(): Flow<List<MatchHistoryEntity>>

    @Query("SELECT matchId, timestamp, formattedDate, teamAName, teamBName, totalOvers, teamAScore, teamAWickets, teamAOversStr, teamBScore, teamBWickets, teamBOversStr, resultSummary, isCompleted, '' AS gameJson FROM match_history ORDER BY timestamp DESC")
    suspend fun getAllMatches(): List<MatchHistoryEntity>

    @Query("SELECT * FROM match_history WHERE matchId = :id LIMIT 1")
    suspend fun getMatchById(id: String): MatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMatch(match: MatchHistoryEntity)

    @Query("DELETE FROM match_history WHERE matchId = :id")
    suspend fun deleteMatchById(id: String)

    @Query("DELETE FROM match_history")
    suspend fun deleteAllMatches()
}

@Database(
    entities = [CricketGameStateEntity::class, MatchHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cricketDao(): CricketGameStateDao
    abstract fun historyDao(): MatchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gully_cricket_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class CricketRepository(
    private val stateDao: CricketGameStateDao,
    private val historyDao: MatchHistoryDao
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(GullyCricketGame::class.java)

    suspend fun getSavedGame(): GullyCricketGame? {
        return try {
            val entity = try {
                stateDao.getGameState()
            } catch (e: Throwable) {
                e.printStackTrace()
                // If cursor window error or oversized blob exists from earlier versions, purge it to recover
                try { stateDao.deleteGameState() } catch (_: Throwable) {}
                null
            } ?: return null
            adapter.fromJson(entity.gameStateJson)
        } catch (e: Throwable) {
            e.printStackTrace()
            try { stateDao.deleteGameState() } catch (_: Throwable) {}
            null
        }
    }

    suspend fun saveGame(game: GullyCricketGame) {
        try {
            val json = adapter.toJson(game)
            stateDao.saveGameState(CricketGameStateEntity(gameStateJson = json))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSavedGame() {
        try {
            stateDao.deleteGameState()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun getAllHistoryFlow(): Flow<List<MatchHistoryEntity>> = historyDao.getAllMatchesFlow()

    suspend fun getAllMatches(): List<MatchHistoryEntity> {
        return try {
            historyDao.getAllMatches()
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getMatchById(id: String): MatchHistoryEntity? {
        return try {
            historyDao.getMatchById(id)
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveMatchToHistory(match: MatchHistoryEntity) {
        try {
            historyDao.insertOrUpdateMatch(match)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    suspend fun deleteMatchFromHistory(matchId: String) {
        try {
            historyDao.deleteMatchById(matchId)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    suspend fun clearAllHistory() {
        try {
            historyDao.deleteAllMatches()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun deserializeGame(json: String): GullyCricketGame? {
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun serializeGame(game: GullyCricketGame): String {
        return try {
            adapter.toJson(game)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
