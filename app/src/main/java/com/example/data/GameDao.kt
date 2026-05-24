package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 10")
    fun getTopTenHighScores(): Flow<List<HighScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighScore(score: HighScoreEntity)

    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    fun getPlayerStats(): Flow<PlayerStatsEntity?>

    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    suspend fun getPlayerStatsDirect(): PlayerStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerStats(stats: PlayerStatsEntity)
}
